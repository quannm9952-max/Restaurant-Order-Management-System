package com.restaurant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.restaurant.dto.order.OrderLine;
import com.restaurant.dto.preparation.PreparationLine;
import com.restaurant.dto.report.ReportModels.DateRange;
import com.restaurant.dto.report.ReportModels.Grouping;
import com.restaurant.entity.OrderDetail;
import com.restaurant.entity.enums.CookingStatus;
import com.restaurant.exception.BusinessException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.repository.OrderDetailRepository;
import com.restaurant.service.MenuService;
import com.restaurant.service.OrderService;
import com.restaurant.service.PaymentService;
import com.restaurant.service.PreparationService;
import com.restaurant.service.ReportService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.function.BiFunction;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:preparation"
    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@WithMockUser(username = "prep_chef", roles = "CHEF")
class PreparationIntegrationTest {
  @Autowired
  DataSource dataSource;
  @Autowired
  JdbcTemplate jdbc;
  @Autowired
  PreparationService preparation;
  @Autowired
  OrderService orders;
  @Autowired
  PaymentService payments;
  @Autowired
  MenuService menu;
  @Autowired
  ReportService reports;
  @Autowired
  EntityManager entityManager;
  @Autowired
  MockMvc mvc;
  @Autowired
  PasswordEncoder encoder;
  @MockitoSpyBean
  OrderDetailRepository details;

  @BeforeAll
  void fixture() throws Exception {
    var populator = new ResourceDatabasePopulator();
    for (String file : List.of("schema.sql", "test-data.sql",
        "demo-data.sql")) {
      String sql = Files
          .readString(Path.of("..", "database", file), StandardCharsets.UTF_8)
          .replace(" ENGINE=InnoDB", "").replace(" STORED", "")
          .replace("START TRANSACTION;", "").replace("COMMIT;", "");
      populator.addScript(
          new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
    }
    populator.execute(dataSource);
    int id = 9750;
    for (String role : List.of("WAITER", "CHEF", "CASHIER", "MANAGER")) {
      id++;
      jdbc.update(
          "INSERT INTO Employee(employee_id,full_name,position) VALUES(?,?,?)",
          id, "TEST " + role, role);
      jdbc.update(
          "INSERT INTO Account(account_id,employee_"
              + "id,username,password_hash,role,is_active)"
              + " VALUES(?,?,?,?,?,true)",
          id, id, "prep_" + role.toLowerCase(Locale.ROOT),
          encoder.encode("TEST_PREPARATION_2026"), role);
    }
  }

  private void role(String role) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(
            "prep_" + role.toLowerCase(Locale.ROOT), "",
            List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  private String state(int id) {
    return jdbc.queryForObject(
        "SELECT preparation_status FROM Order_Detail WHERE order_detail_id=?",
        String.class, id);
  }

  private PreparationLine invoke(String action, int orderId, int lineId) {
    role(action.equals("served") ? "WAITER" : "CHEF");
    return switch (action) {
      case "start" -> preparation.startPreparing(orderId, lineId);
      case "ready" -> preparation.markReady(orderId, lineId);
      case "served" -> preparation.confirmServed(orderId, lineId);
      default -> throw new IllegalArgumentException("Unknown test action");
    };
  }

  @Test
  void chefAdvancesOnlySelectedLineAndPreservesSavedData() {
    int paymentsBefore = jdbc.queryForObject(
        "SELECT COUNT(*) FROM Payment WHERE order_id=1052", Integer.class);
    var before = jdbc.queryForMap("SELECT quantity,item_name_at_order,unit"
        + "_price_at_order FROM Order_Detail WHERE" + " order_detail_id=9204");
    var result = preparation.markReady(1052, 9204);
    assertThat(result.status()).isEqualTo(CookingStatus.READY);
    assertThat(state(9204)).isEqualTo("READY");
    assertThat(state(9205)).isEqualTo("READY");
    assertThat(jdbc.queryForMap("SELECT quantity,item_name_at_order,unit"
        + "_price_at_order FROM Order_Detail WHERE" + " order_detail_id=9204"))
        .isEqualTo(before);
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Orders WHERE order_id=1052", String.class))
        .isEqualTo("OPEN");
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Payment WHERE order_id=1052", Integer.class))
        .isEqualTo(paymentsBefore);
  }

  @ParameterizedTest
  @CsvSource({"PREPARING,start", "READY,start", "SERVED,start",
      "CANCELLED,start", "PENDING,ready", "READY,ready", "SERVED,ready",
      "CANCELLED,ready", "PENDING,served", "PREPARING,served", "SERVED,served",
      "CANCELLED,served"})
  void invalidTransitionsDoNotPersist(String state, String action) {
    jdbc.update("UPDATE Order_Detail SET "
        + "preparation_status=? WHERE order_detail_id=9203", state);
    assertThatThrownBy(() -> invoke(action, 1051, 9203))
        .isInstanceOf(BusinessException.class);
    assertThat(state(9203)).isEqualTo(state);
  }

  @ParameterizedTest
  @CsvSource({"COMPLETED,PENDING,start", "CANCELLED,PENDING,start",
      "COMPLETED,PREPARING,ready", "CANCELLED,PREPARING,ready",
      "COMPLETED,READY,served", "CANCELLED,READY,served"})
  void closedOrdersCannotChange(String orderState, String itemState,
      String action) {
    jdbc.update("UPDATE Orders SET status=? WHERE order_id=1051", orderState);
    jdbc.update("UPDATE Order_Detail SET "
        + "preparation_status=? WHERE order_detail_id=9203", itemState);
    assertThatThrownBy(() -> invoke(action, 1051, 9203))
        .isInstanceOf(BusinessException.class).hasMessageContaining("Open");
    assertThat(state(9203)).isEqualTo(itemState);
  }

  @ParameterizedTest
  @CsvSource({"PENDING,start", "PREPARING,ready", "READY,served"})
  void duplicateConfirmationIsRejected(String initial, String action) {
    jdbc.update("UPDATE Order_Detail SET "
        + "preparation_status=? WHERE order_detail_id=9203", initial);
    var first = invoke(action, 1051, 9203);
    assertThatThrownBy(() -> invoke(action, 1051, 9203))
        .isInstanceOf(BusinessException.class);
    assertThat(state(9203)).isEqualTo(first.status().name());
  }

  @Test
  void orderAndLineIdsMustMatch() {
    assertThatThrownBy(() -> preparation.startPreparing(1051, 9205))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> preparation.startPreparing(1051, 999999))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> preparation.startPreparing(999999, 9203))
        .isInstanceOf(NotFoundException.class);
    assertThat(state(9203)).isEqualTo("PENDING");
    assertThat(state(9205)).isEqualTo("READY");
  }

  @Test
  void kitchenIncludesContextButOnlyForOpenOrdersContainingSelectedStage() {
    var board = preparation.kitchen("PREPARING");
    assertThat(board.orders()).extracting(o -> o.orderId())
        .containsExactly(1052);
    assertThat(board.orders().getFirst().lines())
        .extracting(PreparationLine::status)
        .containsExactly(CookingStatus.PREPARING, CookingStatus.READY);
    assertThat(board.counts().get("PENDING")).isEqualTo(1L);
    assertThat(board.counts().get("PREPARING")).isEqualTo(1L);
    assertThat(board.counts().get("READY")).isEqualTo(1L);
    jdbc.update("UPDATE Order_Detail SET "
        + "preparation_status='READY' WHERE order_detail_id=9146");
    assertThat(preparation.kitchen("READY").orders())
        .extracting(o -> o.orderId()).doesNotContain(1042);
  }

  @Test
  void servingFiltersReadyLinesByFoodTableAndOrderWithLiteralSearch() {
    role("WAITER");
    var board = preparation.serving(null, "");
    assertThat(board.lineCount()).isEqualTo(1);
    var line = board.orders().getFirst().lines().getFirst();
    assertThat(line.lineId()).isEqualTo(9205);
    for (String query : List.of(line.itemName(), line.tableName(), "ORD-1052",
        "Line 9205"))
      assertThat(preparation.serving(line.tableId(), query).lineCount())
          .isEqualTo(1);
    assertThat(preparation.serving(9004, "").orders()).isEmpty();
    assertThat(preparation.serving(null, "%' OR 1=1 --").orders()).isEmpty();
    assertThat(preparation.serving(null, "%").orders()).isEmpty();
  }

  @Test
  void readyCountsAndRevisionFollowIndividualTransitions() {
    String version = preparation.kitchenUpdate().revision();
    preparation.markReady(1052, 9204);
    assertThat(preparation.kitchenUpdate().revision()).isNotEqualTo(version);
    role("WAITER");
    var before = preparation.servingUpdate();
    assertThat(before.readyLines()).isEqualTo(2);
    preparation.confirmServed(1052, 9204);
    assertThat(preparation.servingUpdate().readyLines()).isEqualTo(1);
    assertThat(preparation.servingUpdate().revision())
        .isNotEqualTo(before.revision());
    assertThat(preparation.serving(null, "").orders().getFirst().lines())
        .extracting(PreparationLine::lineId).containsExactly(9205);
  }

  @Test
  void emptyKitchenAndServingStatesAreSupported() throws Exception {
    jdbc.update("UPDATE Order_Detail SET "
        + "preparation_status='SERVED' WHERE order_id IN (1051,1052)");
    assertThat(preparation.kitchen("PENDING").orders()).isEmpty();
    mvc.perform(get("/kitchen")).andExpect(status().isOk()).andExpect(content()
        .string(org.hamcrest.Matchers.containsString("No pending items")));
    mvc.perform(get("/serving").with(user("prep_waiter").roles("WAITER")))
        .andExpect(status().isOk()).andExpect(content()
            .string(org.hamcrest.Matchers.containsString("No Ready items")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"CHEF", "WAITER", "CASHIER", "MANAGER"})
  void routesEnforceChefAndWaiterBoundaries(String role) throws Exception {
    var who = user("prep_" + role.toLowerCase(Locale.ROOT)).roles(role);
    mvc.perform(get("/kitchen").with(who))
        .andExpect(status().is(role.equals("CHEF") ? 200 : 403));
    mvc.perform(get("/kitchen/updates").with(who))
        .andExpect(status().is(role.equals("CHEF") ? 200 : 403));
    mvc.perform(get("/serving").with(who))
        .andExpect(status().is(role.equals("WAITER") ? 200 : 403));
    mvc.perform(get("/serving/updates").with(who))
        .andExpect(status().is(role.equals("WAITER") ? 200 : 403));
    mvc.perform(
        post("/kitchen/orders/1051/items/9203/start").with(who).with(csrf()))
        .andExpect(status().is(role.equals("CHEF") ? 302 : 403));
    mvc.perform(
        post("/kitchen/orders/1052/items/9204/ready").with(who).with(csrf()))
        .andExpect(status().is(role.equals("CHEF") ? 302 : 403));
    mvc.perform(
        post("/serving/orders/1052/items/9205/served").with(who).with(csrf()))
        .andExpect(status().is(role.equals("WAITER") ? 302 : 403));
  }

  @ParameterizedTest
  @ValueSource(strings = {"CASHIER", "MANAGER"})
  void servicesAlsoRejectUnauthorizedRoles(String role) {
    role(role);
    assertThatThrownBy(() -> preparation.kitchen("PENDING"))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> preparation.serving(null, ""))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> preparation.startPreparing(1051, 9203))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> preparation.confirmServed(1052, 9205))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void chefCannotServeAndWaiterCannotCookThroughService() {
    assertThatThrownBy(() -> preparation.confirmServed(1052, 9205))
        .isInstanceOf(AccessDeniedException.class);
    role("WAITER");
    assertThatThrownBy(() -> preparation.startPreparing(1051, 9203))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> preparation.markReady(1052, 9204))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void csrfAndAnonymousRequestsCannotChangeItems() throws Exception {
    mvc.perform(post("/kitchen/orders/1051/items/9203/start"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/serving/orders/1052/items/9205/served")
        .with(user("prep_waiter").roles("WAITER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/kitchen").with(anonymous()))
        .andExpect(status().is3xxRedirection());
    assertThat(state(9203)).isEqualTo("PENDING");
    assertThat(state(9205)).isEqualTo("READY");
  }

  @Test
  void staleConfirmationShowsAnActionableError() throws Exception {
    mvc.perform(post("/kitchen/orders/1051/items/9203/start")
        .with(user("prep_chef").roles("CHEF")).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attributeExists("successMessage"));
    mvc.perform(post("/kitchen/orders/1051/items/9203/start")
        .with(user("prep_chef").roles("CHEF")).with(csrf()))
        .andExpect(status().is3xxRedirection()).andExpect(flash().attribute(
            "errorMessage", org.hamcrest.Matchers.containsString("Refresh")));
    assertThat(state(9203)).isEqualTo("PREPARING");
  }

  @Test
  void invalidFiltersAndIdsReturn400() throws Exception {
    mvc.perform(get("/kitchen?status=SERVED"))
        .andExpect(status().isBadRequest());
    var waiter = user("prep_waiter").roles("WAITER");
    mvc.perform(get("/serving?tableId=abc").with(waiter))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/serving?tableId=-1").with(waiter))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/serving").param("q", "a".repeat(101)).with(waiter))
        .andExpect(status().isBadRequest());
  }

  @Test
  void retainedNamesAndNotesAreEscaped() throws Exception {
    jdbc.update("UPDATE Order_Detail SET "
        + "item_name_at_order='<script>alert(1)</script>' WHERE"
        + " order_detail_id=9203");
    jdbc.update("UPDATE Orders SET notes='<img src=x "
        + "onerror=alert(2)>' WHERE order_id=1051");
    mvc.perform(get("/kitchen")).andExpect(status().isOk())
        .andExpect(content()
            .string(org.hamcrest.Matchers.containsString("&lt;script&gt;")))
        .andExpect(
            content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers
                .containsString("<script>alert(1)</script>"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Cash", "Card", "Bank Transfer"})
  void completeOrderKitchenServingPaymentAndReportFlowUsesSavedPrices(
      String paymentMethod) {
    role("WAITER");
    var order = orders.createOrder(9004, "TEST full preparation flow",
        List.of(new OrderLine(9001, 2), new OrderLine(9002, 1)));
    int orderId = order.getId();
    var lines = orders.getOrderDetail(orderId).lines();
    BigDecimal saved = orders.getOrderDetail(orderId).totalAmount();
    role("MANAGER");
    menu.updateMenu(9001, "TEST renamed menu dish", "TEST changed category",
        "TEST description", new BigDecimal("999.00"), true);
    role("CHEF");
    for (var line : lines) {
      preparation.startPreparing(orderId, line.detailId());
      preparation.markReady(orderId, line.detailId());
    }
    role("CASHIER");
    assertThatThrownBy(() -> payments.payOrder(orderId, "Cash", true))
        .isInstanceOf(IllegalStateException.class);
    role("WAITER");
    for (var line : lines)
      preparation.confirmServed(orderId, line.detailId());
    role("CASHIER");
    payments.payOrder(orderId, paymentMethod, true);
    assertThat(jdbc.queryForObject("SELECT status FROM Orders WHERE order_id=?",
        String.class, orderId)).isEqualTo("COMPLETED");
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Restaurant_tables WHERE table_id=9004",
        String.class)).isEqualTo("AVAILABLE");
    assertThat(
        jdbc.queryForObject("SELECT COUNT(*) FROM Invoice WHERE order_id=?",
            Integer.class, orderId))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject(
        "SELECT amount FROM Payment WHERE order_id=? AND status='SUCCESSFUL'",
        BigDecimal.class, orderId)).isEqualByComparingTo(saved);
    assertThat(payments.invoiceData(orderId).get("total")).isEqualTo(saved);
    role("MANAGER");
    var range = new DateRange(LocalDate.now(), LocalDate.now());
    assertThat(reports.report(range, Grouping.DAY).summary().totalRevenue())
        .isEqualByComparingTo(saved);
    assertThat(jdbc.queryForObject(
        "SELECT item_name_at_order FROM Order_Detail WHERE order_detail_id=?",
        String.class, lines.getFirst().detailId()))
        .isEqualTo(lines.getFirst().itemNameAtOrder());
    role("CHEF");
    assertThatThrownBy(
        () -> preparation.startPreparing(orderId, lines.getFirst().detailId()))
        .isInstanceOf(BusinessException.class).hasMessageContaining("Open");
  }

  private void raceFixture(int id, String state) {
    jdbc.update(
        "INSERT INTO Restaurant_tables(table_id,table_name,capacity,status)"
            + " VALUES(?,?,4,'OCCUPIED')",
        id, "TEST race " + id);
    jdbc.update("INSERT INTO Orders(order_id,table_id,emp"
        + "loyee_id,created_at,status,total_amount)"
        + " VALUES(?,?,9751,CURRENT_TIMESTAMP,'OPEN',14.50)", id, id);
    jdbc.update("INSERT INTO Order_Detail(order_detail_id,order_id,item_i"
        + "d,quantity,unit_price_at_order,ite"
        + "m_name_at_order,preparation_status)"
        + " VALUES(?,?,9001,2,7.25,'TEST race dish',?)", id, id, state);
  }

  private void cleanRace(int id) {
    jdbc.update("DELETE FROM Invoice WHERE order_id=?", id);
    jdbc.update("DELETE FROM Payment WHERE order_id=?", id);
    jdbc.update("DELETE FROM Order_Detail WHERE order_id=?", id);
    jdbc.update("DELETE FROM Orders WHERE order_id=?", id);
    jdbc.update("DELETE FROM Restaurant_tables WHERE table_id=?", id);
  }

  private record Attempt(boolean success, Throwable error) {
  }

  private List<Attempt> race(String firstRole, Runnable first,
      String secondRole, Runnable second) throws Exception {
    var ready = new CountDownLatch(2);
    var go = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      BiFunction<String, Runnable, Callable<Attempt>> task = (as,
          action) -> () -> {
            role(as);
            ready.countDown();
            try {
              go.await();
              action.run();
              return new Attempt(true, null);
            } catch (Throwable error) {
              return new Attempt(false, error);
            } finally {
              SecurityContextHolder.clearContext();
            }
          };
      var a = executor.submit(task.apply(firstRole, first));
      var b = executor.submit(task.apply(secondRole, second));
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      go.countDown();
      return List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentChefsCannotAdvanceOnePendingLineTwice() throws Exception {
    int id = 9950;
    raceFixture(id, "PENDING");
    try {
      var outcomes = race("CHEF", () -> preparation.startPreparing(id, id),
          "CHEF", () -> preparation.startPreparing(id, id));
      assertThat(outcomes.stream().filter(Attempt::success).count())
          .isEqualTo(1);
      assertThat(outcomes.stream().filter(a -> !a.success()).findFirst()
          .orElseThrow().error()).isInstanceOf(BusinessException.class);
      assertThat(state(id)).isEqualTo("PREPARING");
    } finally {
      cleanRace(id);
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void preparationAndOrderCancellationHaveOnlyOneWinner() throws Exception {
    int id = 9951;
    raceFixture(id, "PENDING");
    try {
      var outcomes = race("CHEF", () -> preparation.startPreparing(id, id),
          "WAITER", () -> orders.cancelOrder(id, "TEST race"));
      assertThat(outcomes.stream().filter(Attempt::success).count())
          .isEqualTo(1);
      assertThat(outcomes.stream().filter(a -> !a.success()).findFirst()
          .orElseThrow().error()).isInstanceOf(BusinessException.class);
      String orderState = jdbc.queryForObject(
          "SELECT status FROM Orders WHERE order_id=?", String.class, id);
      assertThat(state(id))
          .isEqualTo(orderState.equals("OPEN") ? "PREPARING" : "CANCELLED");
      assertThat(jdbc.queryForObject(
          "SELECT status FROM Restaurant_tables WHERE table_id=?", String.class,
          id)).isEqualTo(orderState.equals("OPEN") ? "OCCUPIED" : "AVAILABLE");
    } finally {
      cleanRace(id);
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentServingAndCheckoutRemainConsistent() throws Exception {
    int id = 9952;
    raceFixture(id, "READY");
    try {
      var outcomes = race("WAITER", () -> preparation.confirmServed(id, id),
          "CASHIER", () -> payments.payOrder(id, "Cash", true));
      assertThat(outcomes.getFirst().success()).isTrue();
      if (!outcomes.get(1).success())
        assertThat(outcomes.get(1).error())
            .isInstanceOf(IllegalStateException.class);
      assertThat(state(id)).isEqualTo("SERVED");
      boolean paid = outcomes.get(1).success();
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Payment WHERE "
          + "order_id=? AND status='SUCCESSFUL'", Integer.class, id))
          .isEqualTo(paid ? 1 : 0);
      assertThat(jdbc.queryForObject(
          "SELECT COUNT(*) FROM Invoice WHERE order_id=?", Integer.class, id))
          .isEqualTo(paid ? 1 : 0);
      assertThat(jdbc.queryForObject(
          "SELECT status FROM Orders WHERE order_id=?", String.class, id))
          .isEqualTo(paid ? "COMPLETED" : "OPEN");
      assertThat(jdbc.queryForObject(
          "SELECT status FROM Restaurant_tables WHERE table_id=?", String.class,
          id)).isEqualTo(paid ? "AVAILABLE" : "OCCUPIED");
    } finally {
      cleanRace(id);
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void failedSaveRollsBackAndShowsARecoveryMessage() throws Exception {
    int id = 9953;
    raceFixture(id, "PENDING");
    try {
      doThrow(new DataAccessResourceFailureException("TEST save failure"))
          .when(details).saveAndFlush(any(OrderDetail.class));
      mvc.perform(post("/kitchen/orders/9953/items/9953/start")
          .with(user("prep_chef").roles("CHEF")).with(csrf()))
          .andExpect(status().is3xxRedirection())
          .andExpect(flash().attribute("errorMessage",
              org.hamcrest.Matchers.containsString("No change was committed")));
      assertThat(state(id)).isEqualTo("PENDING");
      assertThat(jdbc.queryForObject(
          "SELECT status FROM Orders WHERE order_id=?", String.class, id))
          .isEqualTo("OPEN");
    } finally {
      cleanRace(id);
    }
  }
}
