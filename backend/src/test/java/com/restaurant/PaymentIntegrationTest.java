package com.restaurant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.restaurant.dto.payment.PaymentConfirmation;
import com.restaurant.dto.payment.BillLineQuantity;
import com.restaurant.dto.payment.BillReview;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.restaurant.dto.report.ReportModels.*;
import com.restaurant.entity.enums.PaymentMethod;
import com.restaurant.entity.enums.PaymentStatus;
import com.restaurant.repository.CheckoutRepository;
import com.restaurant.service.PaymentService;
import com.restaurant.service.OrderService;
import com.restaurant.dto.order.OrderLine;
import com.restaurant.service.ReportService;
import com.restaurant.service.HistoryService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:payment-business;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@WithMockUser(username = "DEMO_FIXTURE_CASHIER", roles = "CASHIER")
class PaymentIntegrationTest {
  @Autowired
  DataSource dataSource;
  @Autowired
  JdbcTemplate jdbc;
  @Autowired
  PaymentService payments;
  @Autowired
  OrderService orders;
  @Autowired
  ReportService reports;
  @Autowired
  HistoryService history;
  @Autowired
  MockMvc mvc;
  @MockitoSpyBean
  CheckoutRepository checkout;
  private static final DateRange RANGE = new DateRange(LocalDate.of(2030, 1, 1),
      LocalDate.of(2030, 1, 2));

  @BeforeAll
  void fixture() throws Exception {
    var populator = new ResourceDatabasePopulator();
    for (String name : List.of("../database/schema.sql",
        "src/test/resources/reporting-data.sql",
        "src/test/resources/checkout-data.sql")) {
      String sql = Files.readString(Path.of(name), StandardCharsets.UTF_8)
          .replace(" ENGINE=InnoDB", "").replace(" STORED", "")
          .replace("START TRANSACTION;", "").replace("COMMIT;", "");
      populator.addScript(
          new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
    }
    populator.execute(dataSource);
  }

  private void role(String role) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("DEMO_FIXTURE_" + role, "",
            List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  private PaymentConfirmation confirmed(String method, String received) {
    return new PaymentConfirmation(method, true,
        received == null ? null : new BigDecimal(received));
  }

  private BillReview currentReview(long id) {
    var view = payments.orderDetailForPayment(id);
    var lines = jdbc.query(
        "SELECT order_detail_id,quantity FROM Order_Detail WHERE order_id=? AND preparation_status<>'CANCELLED' ORDER BY order_detail_id",
        (row, index) -> new BillLineQuantity(row.getLong(1), row.getInt(2)),
        id);
    return new BillReview(lines, (String) view.get("billRevision"),
        (BigDecimal) view.get("totalAmount"));
  }

  private BillReview reviewed(int quantity, String total) {
    return new BillReview(List.of(new BillLineQuantity(4100L, quantity)),
        (String) payments.orderDetailForPayment(1100).get("billRevision"),
        new BigDecimal(total));
  }

  private MockHttpServletRequestBuilder paymentPost(long id) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    try {
      role("CASHIER");
      var review = currentReview(id);
      var request = post("/payments/" + id + "/confirm")
          .param("billRevision", review.revision())
          .param("expectedTotal", review.expectedTotal().toPlainString());
      for (var line : review.lines())
        request.param("detailId", line.detailId().toString()).param("quantity",
            line.quantity().toString());
      return request;
    } finally {
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
  }

  private MockHttpServletRequestBuilder reviewPost(int quantity, String total) {
    return post("/payments/1100/review").param("detailId", "4100")
        .param("quantity", String.valueOf(quantity))
        .param("expectedTotal", total).param("billRevision",
            (String) payments.orderDetailForPayment(1100).get("billRevision"));
  }

  private void unpaid(long id, long table) {
    assertThat(jdbc.queryForObject("SELECT status FROM Orders WHERE order_id=?",
        String.class, id)).isEqualTo("OPEN");
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Restaurant_tables WHERE table_id=?", String.class,
        table)).isEqualTo("OCCUPIED");
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Payment WHERE order_id=?", Integer.class, id))
        .isZero();
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Invoice WHERE order_id=?", Integer.class, id))
        .isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"CASH", "BANK_TRANSFER"})
  void successfulPaymentUsesSavedAmountAndCanonicalMethodAndCreatesInvoice(
      String method) throws Exception {
    var old = jdbc.queryForList("SELECT * FROM Payment ORDER BY payment_id");
    long id = payments.confirmPayment(1100,
        confirmed(method, method.equals("CASH") ? "500000.00" : null));
    assertThat(jdbc.queryForObject(
        "SELECT amount FROM Payment WHERE payment_id=?", BigDecimal.class, id))
        .isEqualByComparingTo("390000.00");
    assertThat(jdbc.queryForObject(
        "SELECT payment_method FROM Payment WHERE payment_id=?", String.class,
        id)).isEqualTo(method);
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Orders WHERE order_id=1100", String.class))
        .isEqualTo("COMPLETED");
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Restaurant_tables WHERE table_id=9010",
        String.class)).isEqualTo("AVAILABLE");
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Invoice WHERE order_id=1100", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject(
        "SELECT payment_time FROM Payment WHERE payment_id=?",
        java.sql.Timestamp.class, id)).isNotNull();
    assertThat(jdbc.queryForList(
        "SELECT * FROM Payment WHERE payment_id<>? ORDER BY payment_id", id))
        .isEqualTo(old);
    assertThat(payments.invoiceData(1100).get("total"))
        .isEqualTo(new BigDecimal("390000.00"));
    mvc.perform(get("/invoices/1100")).andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers
            .containsString(PaymentMethod.forPayment(method).label())));
    role("CASHIER");
    assertThatThrownBy(
        () -> payments.confirmPayment(1100, confirmed(method, "390000")))
        .isInstanceOf(IllegalStateException.class);
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Payment WHERE order_id=1100 AND status='SUCCESSFUL'",
        Integer.class)).isEqualTo(1);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"Card", "Cash", "Bank Transfer", "UNKNOWN", "PAYPAL",
      "cash", "CASH "})
  void invalidOrMissingNewMethodCannotCreatePayment(String method) {
    role("CASHIER");
    assertThatThrownBy(
        () -> payments.confirmPayment(1100, confirmed(method, "390000")))
        .isInstanceOf(IllegalArgumentException.class);
    unpaid(1100, 9010);
  }

  @ParameterizedTest
  @ValueSource(strings = {"CASH", "BANK_TRANSFER"})
  void missingReceiptVerificationIsRejectedByServiceAndHttp(String method)
      throws Exception {
    assertThatThrownBy(() -> payments.confirmPayment(1100,
        new PaymentConfirmation(method, false, new BigDecimal("390000"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("received and verified");
    mvc.perform(paymentPost(1100).with(csrf()).param("paymentMethod", method)
        .param("amountReceived", "390000"))
        .andExpect(redirectedUrl("/payments?orderId=1100"))
        .andExpect(flash().attribute("error",
            org.hamcrest.Matchers.containsString("received and verified")));
    unpaid(1100, 9010);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"389999.99", "0", "-1", "390000.001", "10000000000"})
  void cashMustHaveSufficientValidTender(String received) throws Exception {
    assertThatThrownBy(
        () -> payments.confirmPayment(1100, confirmed("CASH", received)))
        .isInstanceOf(IllegalArgumentException.class);
    var request = paymentPost(1100).with(csrf()).param("paymentMethod", "CASH")
        .param("paymentVerified", "true");
    if (received != null)
      request.param("amountReceived", received);
    mvc.perform(request).andExpect(redirectedUrl("/payments?orderId=1100"))
        .andExpect(flash().attributeExists("error"));
    unpaid(1100, 9010);
  }

  @Test
  void httpCannotDefaultToCashOrAcceptMalformedMoney() throws Exception {
    mvc.perform(paymentPost(1100).with(csrf()).param("paymentVerified", "true")
        .param("amountReceived", "390000"))
        .andExpect(redirectedUrl("/payments?orderId=1100"))
        .andExpect(flash().attributeExists("error"));
    mvc.perform(paymentPost(1100).with(csrf()).param("paymentMethod", "CASH")
        .param("paymentVerified", "true")
        .param("amountReceived", "not-a-number"))
        .andExpect(status().isBadRequest());
    unpaid(1100, 9010);
  }

  @ParameterizedTest
  @ValueSource(strings = {"CASH", "BANK_TRANSFER"})
  void verifiedHttpPaymentRecordsTransactionAndStaysOnPaymentPage(String method)
      throws Exception {
    var request = paymentPost(1100).with(csrf()).param("paymentMethod", method)
        .param("paymentVerified", "true").param("amountReceived", "390000.00")
        .param("totalAmount", "0.01");
    var saved = mvc.perform(request)
        .andExpect(redirectedUrl("/payments?orderId=1100"))
        .andExpect(flash().attributeExists("success")).andReturn();
    assertThat(jdbc.queryForObject(
        "SELECT amount FROM Payment WHERE order_id=1100", BigDecimal.class))
        .isEqualByComparingTo("390000");
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Orders WHERE order_id=1100", String.class))
        .isEqualTo("COMPLETED");
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Restaurant_tables WHERE table_id=9010",
        String.class)).isEqualTo("AVAILABLE");
    var page = mvc
        .perform(get("/payments").param("orderId", "1100")
            .flashAttrs(saved.getFlashMap()))
        .andExpect(status().isOk()).andExpect(view().name("payments"))
        .andReturn().getResponse().getContentAsString();
    assertThat(page)
        .contains("Payment successfully recorded for Order #1100",
            "data-order-id=\"1100\"", "data-paid=\"true\"", "Print Invoice")
        .doesNotContain("class=\"invoice-paper\"");
    mvc.perform(request).andExpect(redirectedUrl("/payments?orderId=1100"))
        .andExpect(flash().attributeExists("error"));
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Payment WHERE order_id=1100", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void successfulPaymentKeepsTheCurrentOrderSearch() throws Exception {
    var saved = mvc
        .perform(paymentPost(1100).with(csrf())
            .param("paymentMethod", "BANK_TRANSFER")
            .param("paymentVerified", "true").param("search", "ORD-1100"))
        .andExpect(redirectedUrl("/payments?orderId=1100&search=ORD-1100"))
        .andReturn();
    var page = mvc
        .perform(get("/payments").param("orderId", "1100")
            .param("search", "ORD-1100").flashAttrs(saved.getFlashMap()))
        .andExpect(status().isOk()).andExpect(view().name("payments"))
        .andReturn().getResponse().getContentAsString();
    assertThat(page).contains("value=\"ORD-1100\"", "data-paid=\"true\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "PREPARING", "READY", "CANCELLED"})
  void verifiedBankPaymentStillRequiresAnActiveServedOrder(String state) {
    jdbc.update(
        "UPDATE Order_Detail SET preparation_status=? WHERE order_id=1100",
        state);
    assertThatThrownBy(
        () -> payments.confirmPayment(1100, confirmed("BANK_TRANSFER", null)))
        .isInstanceOf(IllegalStateException.class);
    unpaid(1100, 9010);
  }

  @Test
  void openOrderWithExistingSuccessCannotBePaidOrPresentedAsEligible() {
    jdbc.update(
        "INSERT INTO Payment(order_id,amount,payment_method,status) VALUES(1100,390000,'Card','SUCCESSFUL')");
    assertThat(payments.orderDetailForPayment(1100).get("readyToPay"))
        .isEqualTo(false);
    assertThat(payments.checkoutOrders("ORD-1100").getFirst().get("readyToPay"))
        .isEqualTo(false);
    assertThatThrownBy(
        () -> payments.confirmPayment(1100, confirmed("BANK_TRANSFER", null)))
        .isInstanceOf(IllegalStateException.class);
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Payment WHERE order_id=1100", Integer.class))
        .isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "WAITER", "CHEF"})
  void newConfirmationCannotBypassCashierRole(String role) throws Exception {
    role(role);
    assertThatThrownBy(
        () -> payments.confirmPayment(1100, confirmed("BANK_TRANSFER", null)))
        .isInstanceOf(AccessDeniedException.class);
    mvc.perform(paymentPost(1100).with(user("DEMO_FIXTURE_" + role).roles(role))
        .with(csrf()).param("paymentMethod", "BANK_TRANSFER")
        .param("paymentVerified", "true")).andExpect(status().isForbidden());
    unpaid(1100, 9010);
  }

  @Test
  void cashierCannotWriteWithoutCsrfOrOpenManagerReports() throws Exception {
    mvc.perform(paymentPost(1100).param("paymentMethod", "BANK_TRANSFER")
        .param("paymentVerified", "true")).andExpect(status().isForbidden());
    mvc.perform(get("/manager/reports")).andExpect(status().isForbidden());
    mvc.perform(get("/manager/payment-history").param("method", "CASH"))
        .andExpect(status().isForbidden());
    role("CASHIER");
    assertThatThrownBy(() -> reports.report(RANGE, Grouping.DAY))
        .isInstanceOf(AccessDeniedException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3})
  void printingBillKeepsItEditableAndFinalPaymentSavesTheLatestQuantities(
      int quantity) throws Exception {
    jdbc.update("UPDATE Menu_item SET unit_price=1 WHERE item_id=9001");
    String expected = new BigDecimal("195000")
        .multiply(BigDecimal.valueOf(quantity)).toPlainString();
    mvc.perform(reviewPost(quantity, expected).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.canEditQuantities").value(true))
        .andExpect(jsonPath("$.totalAmount")
            .value(new BigDecimal(expected).doubleValue()));
    role("CASHIER");
    var view = payments.orderDetailForPayment(1100);
    mvc.perform(get("/payments/1100/bill")
        .param("revision", (String) view.get("billRevision")))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Unpaid")))
        .andExpect(content().string(org.hamcrest.Matchers
            .not(org.hamcrest.Matchers.containsString("Total paid"))));
    role("CASHIER");
    unpaid(1100, 9010);
    payments.reviewBill(1100, reviewed(4, "780000"));
    assertThat(payments.orderDetailForPayment(1100).get("canEditQuantities"))
        .isEqualTo(true);
    assertThat(jdbc.queryForObject(
        "SELECT quantity FROM Order_Detail WHERE order_detail_id=4100",
        Integer.class)).isEqualTo(4);
    // A further edit can be submitted directly with the final payment, without
    // another print.
    payments.confirmPayment(1100, confirmed("BANK_TRANSFER", null),
        reviewed(2, "390000"));
    assertThat(jdbc.queryForObject(
        "SELECT amount FROM Payment WHERE order_id=1100", BigDecimal.class))
        .isEqualByComparingTo("390000");
    assertThat(jdbc.queryForObject(
        "SELECT total_amount FROM Invoice WHERE order_id=1100",
        BigDecimal.class)).isEqualByComparingTo("390000");
    assertThat(jdbc.queryForObject(
        "SELECT quantity FROM Order_Detail WHERE order_detail_id=4100",
        Integer.class)).isEqualTo(2);
    assertThat(payments.orderDetailForPayment(1100).get("canEditQuantities"))
        .isEqualTo(false);
    assertThatThrownBy(() -> payments.reviewBill(1100, reviewed(1, "195000")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void paymentDoesNotRequirePrintingFirst() throws Exception {
    mvc.perform(
        paymentPost(1100).with(csrf()).param("paymentMethod", "BANK_TRANSFER")
            .param("paymentVerified", "true"))
        .andExpect(redirectedUrl("/payments?orderId=1100"));
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Invoice WHERE order_id=1100", Integer.class))
        .isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 2147483647})
  void invalidOrOverflowingQuantitiesCannotChangeAnyBillData(int quantity) {
    assertThatThrownBy(
        () -> payments.reviewBill(1100, reviewed(quantity, "390000")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(jdbc.queryForObject(
        "SELECT quantity FROM Order_Detail WHERE order_detail_id=4100",
        Integer.class)).isEqualTo(2);
    assertThat(jdbc.queryForObject(
        "SELECT total_amount FROM Orders WHERE order_id=1100",
        BigDecimal.class)).isEqualByComparingTo("390000");
    unpaid(1100, 9010);
  }

  @Test
  void missingForeignAndDuplicateLineIdsCannotBeSubmitted() {
    var current = currentReview(1100);
    for (var lines : java.util.Arrays.<List<BillLineQuantity>>asList(null,
        List.of(), List.of(new BillLineQuantity(9100L, 1)),
        List.of(new BillLineQuantity(null, 1)),
        List.of(new BillLineQuantity(4100L, null)), List.of(
            new BillLineQuantity(4100L, 1), new BillLineQuantity(4100L, 2)))) {
      assertThatThrownBy(() -> payments.reviewBill(1100,
          new BillReview(lines, current.revision(), current.expectedTotal())))
          .isInstanceOf(IllegalArgumentException.class);
    }
    unpaid(1100, 9010);
  }

  @Test
  void cancelledItemsStayUnchangedAndExcludedFromTheReviewedBill() {
    jdbc.update(
        "INSERT INTO Order_Detail(order_detail_id,order_id,item_id,quantity,unit_price_at_order,item_name_at_order,preparation_status) VALUES(9997,1100,9001,7,123,'TEST cancelled item','CANCELLED')");
    payments.reviewBill(1100, reviewed(1, "195000"));
    assertThat(jdbc.queryForObject(
        "SELECT total_amount FROM Orders WHERE order_id=1100",
        BigDecimal.class)).isEqualByComparingTo("195000");
    assertThat(jdbc.queryForObject(
        "SELECT quantity FROM Order_Detail WHERE order_detail_id=9997",
        Integer.class)).isEqualTo(7);
    assertThat(jdbc.queryForObject(
        "SELECT preparation_status FROM Order_Detail WHERE order_detail_id=9997",
        String.class)).isEqualTo("CANCELLED");
    unpaid(1100, 9010);
  }

  @Test
  void waiterCanStillAddItemsAfterBillReviewAndMakesOldCashierSnapshotStale() {
    payments.reviewBill(1100, currentReview(1100));
    var old = currentReview(1100);
    role("WAITER");
    assertThat(orders.getOrderDetail(1100).canEdit()).isTrue();
    orders.addItemToOrder(1100, List.of(new OrderLine(9002, 1)));
    role("CASHIER");
    assertThatThrownBy(() -> payments.reviewBill(1100, old))
        .isInstanceOf(IllegalStateException.class);
    unpaid(1100, 9010);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void reviewFailureRollsBackQuantitiesAndTotal() throws Exception {
    doThrow(new DataAccessResourceFailureException("TEST private SQL error"))
        .when(checkout).updateOrderTotal(eq(1100L), any(BigDecimal.class));
    mvc.perform(reviewPost(1, "195000").with(csrf()))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers
            .not(org.hamcrest.Matchers.containsString("private SQL"))));
    assertThat(jdbc.queryForObject(
        "SELECT quantity FROM Order_Detail WHERE order_detail_id=4100",
        Integer.class)).isEqualTo(2);
    assertThat(jdbc.queryForObject(
        "SELECT total_amount FROM Orders WHERE order_id=1100",
        BigDecimal.class)).isEqualByComparingTo("390000");
    unpaid(1100, 9010);
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "WAITER", "CHEF"})
  void reviewAndPreviewAreCashierOnlyAndWritesRequireCsrf(String otherRole)
      throws Exception {
    var current = currentReview(1100);
    var request = reviewPost(1, "195000");
    role(otherRole);
    assertThatThrownBy(() -> payments.reviewBill(1100, current))
        .isInstanceOf(AccessDeniedException.class);
    mvc.perform(request.with(user("DEMO_FIXTURE_" + otherRole).roles(otherRole))
        .with(csrf())).andExpect(status().isForbidden());
    mvc.perform(get("/payments/1100/bill")
        .with(user("DEMO_FIXTURE_" + otherRole).roles(otherRole))
        .param("revision", current.revision()))
        .andExpect(status().isForbidden());
    role("CASHIER");
    mvc.perform(reviewPost(1, "195000")).andExpect(status().isForbidden());
    unpaid(1100, 9010);
  }

  @Test
  void unpaidPreviewDoesNotCountAsRevenueOrExposeAPaidInvoice()
      throws Exception {
    var today = new DateRange(LocalDate.now(), LocalDate.now());
    role("MANAGER");
    var before = reports.report(today, Grouping.DAY).summary();
    role("CASHIER");
    payments.reviewBill(1100, currentReview(1100));
    mvc.perform(get("/payments/1100/bill").param("revision",
        currentReview(1100).revision())).andExpect(status().isOk());
    mvc.perform(get("/invoices/1100")).andExpect(redirectedUrl("/payments"));
    role("MANAGER");
    assertThat(reports.report(today, Grouping.DAY).summary()).isEqualTo(before);
    unpaid(1100, 9010);
  }

  @Test
  void staleReviewAndStalePaymentCannotOverwriteANewerCashierEdit()
      throws Exception {
    var old = reviewed(1, "195000");
    payments.reviewBill(1100, reviewed(3, "585000"));
    assertThatThrownBy(() -> payments.reviewBill(1100, old))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Bill changed");
    assertThatThrownBy(() -> payments.confirmPayment(1100,
        confirmed("BANK_TRANSFER", null), old))
        .isInstanceOf(IllegalStateException.class);
    assertThat(jdbc.queryForObject(
        "SELECT quantity FROM Order_Detail WHERE order_detail_id=4100",
        Integer.class)).isEqualTo(3);
    unpaid(1100, 9010);
  }

  @Test
  void forgedTotalAndMissingSnapshotCannotBePaid() throws Exception {
    assertThatThrownBy(() -> payments.confirmPayment(1100,
        confirmed("BANK_TRANSFER", null), reviewed(1, "1")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("total changed");
    mvc.perform(post("/payments/1100/confirm").with(csrf())
        .param("paymentMethod", "BANK_TRANSFER")
        .param("paymentVerified", "true"))
        .andExpect(redirectedUrl("/payments?orderId=1100"))
        .andExpect(flash().attributeExists("error"));
    unpaid(1100, 9010);
  }

  private void reportPayment(int id, String method, String amount,
      String paymentState, String orderState, String time) {
    jdbc.update(
        "INSERT INTO Orders(order_id,table_id,employee_id,created_at,status,total_amount) VALUES(?,9004,9001,?, ?, ?)",
        id, time, orderState, new BigDecimal(amount));
    jdbc.update(
        "INSERT INTO Payment(payment_id,order_id,payment_time,amount,payment_method,status) VALUES(?,?,?,?,?,?)",
        id, id, time, new BigDecimal(amount), method, paymentState);
  }

  static Stream<Arguments> legacyMethods() {
    return Stream.of(Arguments.of("CASH", PaymentMethod.CASH),
        Arguments.of("Cash", PaymentMethod.CASH),
        Arguments.of(" cash ", PaymentMethod.CASH),
        Arguments.of("BANK_TRANSFER", PaymentMethod.BANK_TRANSFER),
        Arguments.of("Bank Transfer", PaymentMethod.BANK_TRANSFER),
        Arguments.of(" bank transfer ", PaymentMethod.BANK_TRANSFER),
        Arguments.of(null, PaymentMethod.UNKNOWN),
        Arguments.of("", PaymentMethod.UNKNOWN),
        Arguments.of("  ", PaymentMethod.UNKNOWN),
        Arguments.of("Card", PaymentMethod.UNKNOWN),
        Arguments.of("legacy_wallet", PaymentMethod.UNKNOWN));
  }

  @ParameterizedTest
  @MethodSource("legacyMethods")
  void legacyMethodsAreClassifiedWithoutChangingTheirStoredValues(String method,
      PaymentMethod bucket) {
    reportPayment(9910, method, "2.35", "SUCCESSFUL", "COMPLETED",
        "2030-01-01 23:59:59");
    role("MANAGER");
    var report = reports.report(RANGE, Grouping.DAY);
    assertThat(report.summary().totalRevenue()).isEqualByComparingTo("2.35");
    var selected = report.methods().stream().filter(r -> r.method() == bucket)
        .findFirst().orElseThrow();
    assertThat(selected.revenue()).isEqualByComparingTo("2.35");
    assertThat(selected.transactions()).isEqualTo(1);
    var page = history.payments(RANGE, "PAY-9910", PaymentStatus.SUCCESSFUL,
        bucket, 0);
    assertThat(page.content()).hasSize(1);
    assertThat(page.content().getFirst().paymentMethod()).isEqualTo(method);
    assertThat(page.content().getFirst().methodLabel())
        .isEqualTo(PaymentMethod.display(method));
    assertThat(history.paymentCounts(RANGE, "", bucket).successful())
        .isEqualTo(1);
    assertThat(jdbc.queryForObject(
        "SELECT payment_method FROM Payment WHERE payment_id=9910",
        String.class)).isEqualTo(method);
  }

  @Test
  void methodRevenueReconcilesExactlyAndExcludesFailedPendingAndInconsistentRecords() {
    reportPayment(9910, "CASH", "10.00", "SUCCESSFUL", "COMPLETED",
        "2030-01-01 00:00:00");
    reportPayment(9911, "Bank Transfer", "20.00", "SUCCESSFUL", "COMPLETED",
        "2030-01-02 23:59:59");
    reportPayment(9912, null, "30.00", "SUCCESSFUL", "COMPLETED",
        "2030-01-01 12:00:00");
    reportPayment(9913, "Card", "40.00", "SUCCESSFUL", "COMPLETED",
        "2030-01-01 12:00:00");
    reportPayment(9914, "", "50.00", "SUCCESSFUL", "COMPLETED",
        "2030-01-01 12:00:00");
    reportPayment(9915, "Cash", "0.01", "SUCCESSFUL", "COMPLETED",
        "2030-01-02 12:00:00");
    reportPayment(9916, "CASH", "999", "FAILED", "COMPLETED",
        "2030-01-01 12:00:00");
    reportPayment(9917, "BANK_TRANSFER", "555", "PENDING", "COMPLETED",
        "2030-01-01 12:00:00");
    reportPayment(9918, "CASH", "888", "SUCCESSFUL", "CANCELLED",
        "2030-01-01 12:00:00");
    reportPayment(9919, "CASH", "444", "SUCCESSFUL", "COMPLETED",
        "2030-01-03 00:00:00");
    // Multiple item lines must never multiply a payment aggregate.
    for (int line = 0; line < 3; line++)
      jdbc.update(
          "INSERT INTO Order_Detail(order_id,item_id,quantity,unit_price_at_order,item_name_at_order,preparation_status) VALUES(9910,9001,1,10,'TEST saved item','SERVED')");
    role("MANAGER");
    var report = reports.report(RANGE, Grouping.MONTH);
    assertThat(report.cashRevenue()).isEqualByComparingTo("10.01");
    assertThat(report.bankTransferRevenue()).isEqualByComparingTo("20");
    assertThat(report.methods().get(2).revenue()).isEqualByComparingTo("120");
    assertThat(report.summary().totalRevenue()).isEqualByComparingTo("150.01");
    assertThat(report.methods().stream().map(MethodRevenue::revenue)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(report.summary().totalRevenue());
    assertThat(
        report.methods().stream().mapToLong(MethodRevenue::transactions).sum())
        .isEqualTo(report.summary().successful()).isEqualTo(6);
    assertThat(report.summary().inconsistentSuccessful()).isEqualTo(1);
    assertThat(report.points().getFirst().amount())
        .isEqualByComparingTo("150.01");
    var singleDay = reports.report(new DateRange(RANGE.to(), RANGE.to()),
        Grouping.DAY);
    assertThat(singleDay.summary().totalRevenue())
        .isEqualByComparingTo("20.01");
    assertThat(singleDay.cashRevenue()).isEqualByComparingTo("0.01");
  }

  @Test
  void historyFilterPreservesDatesSearchStatusPaginationAndReadOnlyDetails()
      throws Exception {
    for (int n = 0; n < 10; n++)
      reportPayment(9910 + n, "BANK_TRANSFER", "1", "SUCCESSFUL", "COMPLETED",
          "2030-01-01 12:00:00");
    role("MANAGER");
    var first = history.payments(RANGE, "PAY-99", PaymentStatus.SUCCESSFUL,
        PaymentMethod.BANK_TRANSFER, 0);
    var second = history.payments(RANGE, "PAY-99", PaymentStatus.SUCCESSFUL,
        PaymentMethod.BANK_TRANSFER, 1);
    assertThat(first.content()).hasSize(8);
    assertThat(second.content()).hasSize(2);
    assertThat(first.total()).isEqualTo(10);
    mvc.perform(get("/manager/payment-history/9919")
        .with(user("DEMO_FIXTURE_MANAGER").roles("MANAGER"))
        .param("from", "2030-01-01").param("to", "2030-01-02")
        .param("q", "PAY-99").param("status", "SUCCESSFUL")
        .param("method", "BANK_TRANSFER")).andExpect(status().isOk())
        .andExpect(model().attribute("selectedMethod", "BANK_TRANSFER"))
        .andExpect(content()
            .string(org.hamcrest.Matchers.containsString("Bank Transfer")))
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("method=BANK_TRANSFER")))
        .andExpect(content()
            .string(org.hamcrest.Matchers.containsString("Read-only")));
    mvc.perform(get("/manager/payment-history")
        .with(user("DEMO_FIXTURE_MANAGER").roles("MANAGER"))
        .param("method", "CARD")).andExpect(status().isBadRequest());
    mvc.perform(post("/manager/payment-history/9919")
        .with(user("DEMO_FIXTURE_MANAGER").roles("MANAGER")).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void emptyMethodReportContainsRealZeroTotals() {
    role("MANAGER");
    var report = reports.report(RANGE, Grouping.DAY);
    assertThat(report.summary().totalRevenue()).isEqualByComparingTo("0");
    assertThat(report.methods()).hasSize(3).allSatisfy(row -> {
      assertThat(row.transactions()).isZero();
      assertThat(row.revenue()).isEqualByComparingTo("0");
    });
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void lateInvoiceFailureRollsBackPaymentOrderAndTable() throws Exception {
    try {
      doThrow(new DataAccessResourceFailureException(
          "TEST internal database details")).when(checkout)
          .createInvoice(eq(1100L), any(BigDecimal.class));
      mvc.perform(
          paymentPost(1100).with(csrf()).param("paymentMethod", "BANK_TRANSFER")
              .param("paymentVerified", "true"))
          .andExpect(redirectedUrl("/payments?orderId=1100"))
          .andExpect(flash().attribute("error",
              org.hamcrest.Matchers
                  .containsString("payment status before retrying")))
          .andExpect(flash().attribute("error", org.hamcrest.Matchers
              .not(org.hamcrest.Matchers.containsString("internal database"))));
      unpaid(1100, 9010);
    } finally {
      jdbc.update("DELETE FROM Invoice WHERE order_id=1100");
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentCashAndBankConfirmationsProduceOnlyOneSuccessfulPayment()
      throws Exception {
    int id = 9991;
    jdbc.update(
        "INSERT INTO Restaurant_tables(table_id,table_name,capacity,status) VALUES(?,'TEST payment race',4,'OCCUPIED')",
        id);
    jdbc.update(
        "INSERT INTO Orders(order_id,table_id,employee_id,status,total_amount) VALUES(?,?,9001,'OPEN',39)",
        id, id);
    jdbc.update(
        "INSERT INTO Order_Detail(order_id,item_id,quantity,unit_price_at_order,item_name_at_order,preparation_status) VALUES(?,9001,2,19.50,'TEST race dish','SERVED')",
        id);
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var attempts = List.of("CASH", "BANK_TRANSFER").stream()
          .map(method -> pool.submit(() -> {
            role("CASHIER");
            try {
              start.await();
              payments.confirmPayment(id, confirmed(method, "39"));
              return true;
            } catch (IllegalStateException ex) {
              return false;
            } finally {
              SecurityContextHolder.clearContext();
            }
          })).toList();
      start.countDown();
      int successful = 0;
      for (var attempt : attempts)
        if (attempt.get(20, TimeUnit.SECONDS))
          successful++;
      assertThat(successful).isEqualTo(1);
      assertThat(jdbc.queryForObject(
          "SELECT COUNT(*) FROM Payment WHERE order_id=? AND status='SUCCESSFUL'",
          Integer.class, id)).isEqualTo(1);
      assertThat(jdbc.queryForObject(
          "SELECT COUNT(*) FROM Invoice WHERE order_id=?", Integer.class, id))
          .isEqualTo(1);
      assertThat(jdbc.queryForObject(
          "SELECT status FROM Orders WHERE order_id=?", String.class, id))
          .isEqualTo("COMPLETED");
      assertThat(jdbc.queryForObject(
          "SELECT status FROM Restaurant_tables WHERE table_id=?", String.class,
          id)).isEqualTo("AVAILABLE");
      assertThatThrownBy(() -> jdbc.update(
          "INSERT INTO Payment(order_id,amount,payment_method,status) VALUES(?,39,'CASH','SUCCESSFUL')",
          id)).isInstanceOf(
              org.springframework.dao.DataIntegrityViolationException.class);
    } finally {
      start.countDown();
      pool.shutdownNow();
      pool.awaitTermination(5, TimeUnit.SECONDS);
      jdbc.update("DELETE FROM Invoice WHERE order_id=?", id);
      jdbc.update("DELETE FROM Payment WHERE order_id=?", id);
      jdbc.update("DELETE FROM Order_Detail WHERE order_id=?", id);
      jdbc.update("DELETE FROM Orders WHERE order_id=?", id);
      jdbc.update("DELETE FROM Restaurant_tables WHERE table_id=?", id);
    }
  }
}
