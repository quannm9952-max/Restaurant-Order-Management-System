package com.restaurant;

import com.restaurant.entity.enums.PaymentStatus;

import com.restaurant.entity.enums.OrderStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.restaurant.dto.report.ReportModels.*;
import com.restaurant.service.HistoryService;
import com.restaurant.service.ReportService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@WithMockUser(username = "TEST_MANAGER", roles = "MANAGER")
class ReportingIntegrationTest {
  private static final DateRange RANGE = new DateRange(
      LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 4));
  @Autowired
  DataSource dataSource;
  @Autowired
  JdbcTemplate jdbc;
  @Autowired
  MockMvc mvc;
  @Autowired
  ReportService reports;
  @Autowired
  HistoryService monitoring;

  @BeforeAll
  void loadSharedSchemaAndReportingData() throws Exception {
    // Same nine-table schema and seed as MySQL; only MySQL-specific storage
    // syntax is removed for
    // H2.
    String schema = Files
        .readString(Path.of("..", "database", "schema.sql"),
            StandardCharsets.UTF_8)
        .replace(" ENGINE=InnoDB", "").replace(" STORED", "");
    var populator = new ResourceDatabasePopulator();
    populator.addScript(
        new ByteArrayResource(schema.getBytes(StandardCharsets.UTF_8)));
    populator.addScript(new ByteArrayResource(readSeed()));
    populator.execute(dataSource);
  }

  @Test
  void revenueMatchesTheReconciliationFixtureWithoutMultiplyingOrderLines() {
    var report = reports.report(RANGE, Grouping.DAY);
    assertThat(report.summary().totalRevenue())
        .isEqualByComparingTo("18920000.00");
    assertThat(report.summary().successful()).isEqualTo(24);
    assertThat(report.summary().excluded()).isEqualTo(4);
    assertThat(report.summary().pending()).isEqualTo(2);
    assertThat(report.summary().failed()).isEqualTo(2);
    assertThat(report.chartCeiling()).isEqualByComparingTo("4000000");
    assertThat(report.points()).extracting(ChartPoint::amount)
        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .containsExactly(new BigDecimal("1960000"), new BigDecimal("2480000"),
            new BigDecimal("3120000"), new BigDecimal("2840000"),
            new BigDecimal("3380000"), new BigDecimal("3300000"),
            new BigDecimal("1840000"));
    assertThat(report.recentPayments()).extracting(PaymentRow::paymentId)
        .containsExactly(2046, 2044, 2043);
  }

  @Test
  void weeklyReportIncludesOnlySelectedDaysEvenForPartialWeeks() {
    var report = reports.report(RANGE, Grouping.WEEK);
    assertThat(report.points()).hasSize(2);
    assertThat(report.points().getFirst().amount())
        .isEqualByComparingTo("10400000.00");
    assertThat(report.points().getLast().amount())
        .isEqualByComparingTo("8520000.00");
    assertThat(report.chartCeiling()).isEqualByComparingTo("12000000");
    assertThat(report.points().getFirst().start())
        .isEqualTo(LocalDate.of(2026, 10, 26));
    assertThat(report.points().stream().map(ChartPoint::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(report.summary().totalRevenue());
  }

  @Test
  void monthlyRevenueIncludesOnlySelectedDaysOfEachCalendarMonth() {
    var report = reports.report(RANGE, Grouping.MONTH);
    assertThat(report.grouping()).isEqualTo(Grouping.MONTH);
    assertThat(report.points()).extracting(ChartPoint::amount)
        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .containsExactly(new BigDecimal("7560000"), new BigDecimal("11360000"));
    assertThat(report.points().getFirst().start())
        .isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(report.points().getFirst().end())
        .isEqualTo(LocalDate.of(2026, 10, 31));
    assertThat(report.periodLabel(report.points().getFirst()))
        .isEqualTo("Oct 2026");
    assertThat(report.coveredDates(report.points().getFirst()))
        .isEqualTo("29 Oct 2026 – 31 Oct 2026");
    assertThat(report.coveredDates(report.points().getLast()))
        .isEqualTo("01 Nov 2026 – 04 Nov 2026");
    assertThat(report.points().stream().map(ChartPoint::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(report.summary().totalRevenue());
  }

  @Test
  void monthlyReportSeparatesYearsAndIncludesTheWholeLastDay() {
    completedPayment(1060, 2060, "2026-12-31 23:59:59", "10.00");
    completedPayment(1061, 2061, "2027-01-01 00:00:00", "20.00");
    completedPayment(1062, 2062, "2027-01-31 23:59:59", "30.00");
    completedPayment(1063, 2063, "2027-02-01 00:00:00", "40.00");
    var report = reports.report(
        new DateRange(LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 31)),
        Grouping.MONTH);
    assertThat(report.points()).extracting(report::periodLabel)
        .containsExactly("Dec 2026", "Jan 2027");
    assertThat(report.points()).extracting(ChartPoint::amount)
        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .containsExactly(new BigDecimal("10"), new BigDecimal("50"));
    assertThat(report.summary().totalRevenue()).isEqualByComparingTo("60");
  }

  @ParameterizedTest
  @CsvSource({"7,DAY", "8,DAY", "31,DAY", "32,WEEK", "90,WEEK", "91,MONTH"})
  void automaticGroupingUsesInclusiveRangeLength(int days, Grouping expected)
      throws Exception {
    LocalDate start = LocalDate.of(2026, 9, 1);
    DateRange range = new DateRange(start, start.plusDays(days - 1));
    assertThat(reports.report(range, Grouping.AUTO).grouping())
        .isEqualTo(expected);
    var result = mvc
        .perform(get("/manager/reports").param("from", range.from().toString())
            .param("to", range.to().toString()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("selectedGroup", "AUTO")).andReturn();
    var rendered = (RevenueReport) result.getModelAndView().getModel()
        .get("report");
    assertThat(rendered.grouping()).isEqualTo(expected);
  }

  @Test
  void dailyDetailCanStillBeSelectedForLongRanges() {
    var range = new DateRange(LocalDate.of(2026, 8, 1),
        LocalDate.of(2026, 11, 4));
    assertThat(reports.report(range, Grouping.AUTO).grouping())
        .isEqualTo(Grouping.MONTH);
    assertThat(reports.report(range, Grouping.DAY).grouping())
        .isEqualTo(Grouping.DAY);
  }

  @Test
  void all31DailyAmountsRemainAvailableWhenAxisLabelsAreReduced() {
    LocalDate start = LocalDate.of(2027, 1, 1);
    for (int day = 0; day < 31; day++)
      completedPayment(1100 + day, 2100 + day,
          start.plusDays(day) + " 12:00:00", "1.25");
    var report = reports.report(new DateRange(start, start.plusDays(30)),
        Grouping.AUTO);
    assertThat(report.grouping()).isEqualTo(Grouping.DAY);
    assertThat(report.points()).hasSize(31);
    assertThat(report.summary().totalRevenue()).isEqualByComparingTo("38.75");
    assertThat(java.util.stream.IntStream.range(0, 31)
        .filter(report::showAxisLabel).count()).isBetween(2L, 7L);
    assertThat(report.showAxisLabel(0)).isTrue();
    assertThat(report.showAxisLabel(30)).isTrue();
    var money = new com.restaurant.config.ReportConfig.MoneyFormatter();
    assertThat(report.axisTicks().stream().map(money::axis).toList())
        .doesNotHaveDuplicates();
    assertThat(report.points()).allSatisfy(point -> {
      assertThat(point.transactions()).isEqualTo(1);
      assertThat(point.amount()).isEqualByComparingTo("1.25");
    });
  }

  @Test
  void fractionalRevenueStillHasDistinctAndAccurateVndAxisLabels() {
    completedPayment(1060, 2060, "2027-02-01 12:00:00", "0.01");
    LocalDate day = LocalDate.of(2027, 2, 1);
    var report = reports.report(new DateRange(day, day), Grouping.DAY);
    var money = new com.restaurant.config.ReportConfig.MoneyFormatter();
    assertThat(report.summary().totalRevenue()).isEqualByComparingTo("0.01");
    assertThat(report.axisTicks().stream().map(money::axis).toList())
        .containsExactly("0,04\u00a0VND", "0,03\u00a0VND", "0,02\u00a0VND",
            "0,01\u00a0VND", "0\u00a0VND");
  }

  @Test
  void vndFormattingKeepsStoredMoneyExactForIntegerAndLegacyFractionalAmounts() {
    var money = new com.restaurant.config.ReportConfig.MoneyFormatter();
    assertThat(money.format(new BigDecimal("25000.00")))
        .isEqualTo("25.000\u00a0VND");
    assertThat(money.format(new BigDecimal("85000")))
        .isEqualTo("85.000\u00a0VND");
    assertThat(money.format(new BigDecimal("0.01"))).isEqualTo("0,01\u00a0VND");
    assertThat(money.format(new BigDecimal("9999999999.99")))
        .isEqualTo("9.999.999.999,99\u00a0VND");
  }

  @Test
  void filteringUsesPaymentTimeAndIncludesTheWholeEndDate() {
    completedPayment(1060, 2060, "2026-11-04 23:59:59", "0.01");
    completedPayment(1061, 2061, "2026-11-05 00:00:00", "0.02");
    completedPayment(1062, 2062, "2026-10-28 23:59:59", "0.04");
    var report = reports.report(RANGE, Grouping.DAY);
    assertThat(report.summary().totalRevenue())
        .isEqualByComparingTo("18920000.01");
    assertThat(report.summary().successful()).isEqualTo(25);
    assertThat(monitoring.payments(RANGE, "PAY-206", null, 0).content())
        .extracting(PaymentRow::paymentId).containsExactly(2060);
  }

  @Test
  void corruptPaymentExcludedFromRevenueButRetainedInHistory() {
    // Negative test only: intentionally inconsistent data is never part of the
    // demo seed.
    jdbc.update("INSERT INTO Payment(payment_id, "
        + "order_id, amount, payment_time, status) VALUES"
        + " (2099,1050,999,'2026-11-04 15:00:00','SUCCESSFUL')");
    var report = reports.report(RANGE, Grouping.DAY);
    assertThat(report.summary().totalRevenue())
        .isEqualByComparingTo("18920000");
    assertThat(report.summary().inconsistentSuccessful()).isEqualTo(1);
    assertThat(monitoring.payment(2099).includedInRevenue()).isFalse();
    assertThat(monitoring.paymentCounts(RANGE, "").successful()).isEqualTo(25);
  }

  @Test
  void historyIncludesEveryStatusAndPaginationIsStable() {
    var counts = monitoring.paymentCounts(RANGE, "");
    assertThat(counts.total()).isEqualTo(28);
    var first = monitoring.payments(RANGE, "", null, 0);
    assertThat(first.content()).hasSize(8);
    assertThat(first.total()).isEqualTo(28);
    assertThat(first.pages()).isEqualTo(4);
    assertThat(first.content()).extracting(PaymentRow::paymentId)
        .containsExactly(2048, 2047, 2046, 2045, 2044, 2043, 2042, 2041);
    assertThat(
        monitoring.payments(RANGE, "", PaymentStatus.PENDING, 0).content())
        .hasSize(2);
    assertThat(
        monitoring.payments(RANGE, "", PaymentStatus.FAILED, 0).content())
        .hasSize(2);
    assertThat(monitoring.payments(RANGE, "", null, 3).content()).hasSize(4);
  }

  @Test
  void searchingUsesBoundLiteralParametersAndPreservesBothPaymentAttempts() {
    assertThat(monitoring.payments(RANGE, "ORD-1042", null, 0).content())
        .extracting(PaymentRow::paymentId).containsExactly(2046, 2045);
    assertThat(monitoring.payments(RANGE, "%' OR 1=1 --", null, 0).content())
        .isEmpty();
    assertThat(monitoring.orders("ORD-1042", null, 0).content()).hasSize(1);
    assertThat(monitoring.orders("%", null, 0).content()).isEmpty();
    assertThat(monitoring.orders("", OrderStatus.CANCELLED, 0).content())
        .extracting(OrderRow::orderId).containsExactly(1050);
  }

  @Test
  void orderDetailsUseHistoricalItemNamesAndPricesAndExcludeCancelledLines() {
    jdbc.update("UPDATE Menu_item SET "
        + "unit_price=1999.99, item_name='TEST NEW MENU NAME' WHERE"
        + " item_id=9001");
    var details = monitoring.order(1042);
    assertThat(details.items()).hasSize(3);
    assertThat(details.items().getFirst().itemName())
        .isEqualTo("DEMO Grilled Chicken");
    assertThat(details.items().getFirst().unitPrice())
        .isEqualByComparingTo("195000.00");
    assertThat(details.calculatedTotal()).isEqualByComparingTo("780000.00");
    assertThat(details.payments()).hasSize(2);
  }

  @Test
  void emptyPeriodHasZeroRevenueAndNoFakeChartPoints() {
    var empty = reports.report(
        new DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)),
        Grouping.DAY);
    assertThat(empty.summary().totalRevenue())
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(empty.summary().successful()).isZero();
    assertThat(empty.points()).isEmpty();
    assertThat(empty.recentPayments()).isEmpty();
  }

  @Test
  void reportingSeedIsRepeatable() {
    new ResourceDatabasePopulator(new ByteArrayResource(readSeed()))
        .execute(dataSource);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Payment", Long.class))
        .isEqualTo(28);
    assertThat(reports.report(RANGE, Grouping.DAY).summary().totalRevenue())
        .isEqualByComparingTo("18920000");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/manager/reports", "/manager/reports?group=WEEK",
      "/manager/reports?group=MONTH", "/manager/reports?group=AUTO",
      "/manager/payment-history", "/manager/payment-history/2046",
      "/manager/payment-history/2047", "/manager/payment-history/2045",
      "/manager/orders", "/manager/orders/1042"})
  void allMemberFiveViewsRenderForManager(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"))
        .andExpect(content()
            .string(org.hamcrest.Matchers.containsString("Read-only")));
  }

  @Test
  void reportHtmlDisplaysTheDatabaseTotal() throws Exception {
    mvc.perform(get("/manager/reports")).andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("18.920.000\u00a0VND")));
    mvc.perform(get("/manager/payment-history").param("q", "NO_MATCH"))
        .andExpect(status().isOk()).andExpect(content()
            .string(org.hamcrest.Matchers.containsString("No payments found")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"WAITER", "CHEF", "CASHIER", "USER"})
  void otherRolesCannotOpenAnyMemberFiveView(String role) throws Exception {
    for (String path : new String[]{"/manager/reports", "/manager/orders",
        "/manager/orders/1042", "/manager/payment-history",
        "/manager/payment-history/2046"}) {
      mvc.perform(get(path).with(user("TEST_OTHER_ROLE").roles(role)))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  @WithMockUser(roles = "CASHIER")
  void serviceLayerAlsoRejectsNonManager() {
    assertThatThrownBy(() -> reports.report(RANGE, Grouping.DAY))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> monitoring.order(1042))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> monitoring.payment(2046))
        .isInstanceOf(AccessDeniedException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/manager/reports?from=2026-11-05&to=2026-11-04",
      "/manager/reports?from=not-a-date", "/manager/reports?group=YEAR",
      "/manager/payment-history?status=PAID", "/manager/orders?page=-1",
      "/manager/payment-history?page=invalid"})
  void invalidFiltersAreRejectedBeforeQuerying(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isBadRequest());
  }

  @Test
  void missingRecordsReturn404AndWriteRequestsAreRejectedEvenWithCsrfToken()
      throws Exception {
    mvc.perform(get("/manager/orders/999999")).andExpect(status().isNotFound());
    mvc.perform(get("/manager/payment-history/999999"))
        .andExpect(status().isNotFound());
    mvc.perform(post("/manager/orders/1042").with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/manager/payment-history/2046").with(csrf()))
        .andExpect(status().isForbidden());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Payment", Long.class))
        .isEqualTo(28);
  }

  @Test
  void retainedTextIsHtmlEscaped() throws Exception {
    jdbc.update("UPDATE Orders SET "
        + "notes='<script>alert(1)</script>' WHERE order_id=1042");
    mvc.perform(get("/manager/orders/1042")).andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers
            .containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
        .andExpect(
            content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers
                .containsString("<script>alert(1)</script>"))));
  }

  @Test
  @org.springframework.security.test.context.support.WithAnonymousUser
  void anonymousRequestsRequireAuthentication() throws Exception {
    mvc.perform(get("/manager/reports")).andExpect(status().isFound());
    mvc.perform(get("/manager/payment-history/2046"))
        .andExpect(status().isFound());
  }

  private byte[] readSeed() {
    try {
      return Files
          .readString(Path.of("src", "test", "resources", "reporting-data.sql"),
              StandardCharsets.UTF_8)
          .replace("START TRANSACTION;", "").replace("COMMIT;", "")
          .getBytes(StandardCharsets.UTF_8);
    } catch (java.io.IOException exception) {
      throw new java.io.UncheckedIOException(exception);
    }
  }

  private void completedPayment(int orderId, int paymentId, String time,
      String amount) {
    jdbc.update(
        "INSERT INTO Orders(order_id,table_id,employ"
            + "ee_id,created_at,status,total_amount,notes)"
            + " VALUES (?,9007,9001,'2020-01-01 "
            + "12:00:00','COMPLETED',?,'TEST date boundary')",
        orderId, new BigDecimal(amount));
    jdbc.update(
        "INSERT INTO Order_Detail(order_id,item_id,quantity,unit_"
            + "price_at_order,item_name_at_order,preparation_status)"
            + " VALUES (?,9002,1,?,'TEST boundary item','SERVED')",
        orderId, new BigDecimal(amount));
    jdbc.update("INSERT INTO Payment(payment_id,order_"
        + "id,payment_time,amount,status) VALUES" + " (?,?,?,?,'SUCCESSFUL')",
        paymentId, orderId, time, new BigDecimal(amount));
  }
}
