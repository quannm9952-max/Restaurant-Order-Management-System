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
  void loadSharedSchemaAndDemoData() throws Exception {
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
    assertThat(report.summary().totalRevenue()).isEqualByComparingTo("1892.00");
    assertThat(report.summary().successful()).isEqualTo(24);
    assertThat(report.summary().excluded()).isEqualTo(4);
    assertThat(report.summary().pending()).isEqualTo(2);
    assertThat(report.summary().failed()).isEqualTo(2);
    assertThat(report.chartCeiling()).isEqualByComparingTo("400");
    assertThat(report.points()).extracting(ChartPoint::amount)
        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .containsExactly(new BigDecimal("196"), new BigDecimal("248"),
            new BigDecimal("312"), new BigDecimal("284"), new BigDecimal("338"),
            new BigDecimal("330"), new BigDecimal("184"));
    assertThat(report.recentPayments()).extracting(PaymentRow::paymentId)
        .containsExactly(2046, 2044, 2043);
  }

  @Test
  void weeklyReportIncludesOnlySelectedDaysEvenForPartialWeeks() {
    var report = reports.report(RANGE, Grouping.WEEK);
    assertThat(report.points()).hasSize(2);
    assertThat(report.points().getFirst().amount())
        .isEqualByComparingTo("1040.00");
    assertThat(report.points().getLast().amount())
        .isEqualByComparingTo("852.00");
    assertThat(report.chartCeiling()).isEqualByComparingTo("1200");
    assertThat(report.points().getFirst().start())
        .isEqualTo(LocalDate.of(2026, 10, 26));
    assertThat(report.points().stream().map(ChartPoint::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(report.summary().totalRevenue());
  }

  @Test
  void filteringUsesPaymentTimeAndIncludesTheWholeEndDate() {
    completedPayment(1060, 2060, "2026-11-04 23:59:59", "0.01");
    completedPayment(1061, 2061, "2026-11-05 00:00:00", "0.02");
    completedPayment(1062, 2062, "2026-10-28 23:59:59", "0.04");
    var report = reports.report(RANGE, Grouping.DAY);
    assertThat(report.summary().totalRevenue()).isEqualByComparingTo("1892.01");
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
    assertThat(report.summary().totalRevenue()).isEqualByComparingTo("1892");
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
        .isEqualByComparingTo("19.50");
    assertThat(details.calculatedTotal()).isEqualByComparingTo("78.00");
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
  void demoSeedIsRepeatable() {
    new ResourceDatabasePopulator(new ByteArrayResource(readSeed()))
        .execute(dataSource);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Payment", Long.class))
        .isEqualTo(28);
    assertThat(reports.report(RANGE, Grouping.DAY).summary().totalRevenue())
        .isEqualByComparingTo("1892");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/manager/reports", "/manager/reports?group=WEEK",
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
    mvc.perform(get("/manager/reports")).andExpect(status().isOk()).andExpect(
        content().string(org.hamcrest.Matchers.containsString("$1,892.00")));
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
      "/manager/reports?from=not-a-date", "/manager/reports?group=MONTH",
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
          .readString(Path.of("..", "database", "test-data.sql"),
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
