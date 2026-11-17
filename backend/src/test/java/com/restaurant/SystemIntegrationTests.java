package com.restaurant;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.restaurant.auth.service.EmployeeService;
import com.restaurant.checkout.service.MenuService;
import com.restaurant.checkout.service.PaymentService;
import com.restaurant.ordermanagement.reporting.dto.ReportingModels.*;
import com.restaurant.ordermanagement.reporting.service.ReportingService;
import com.restaurant.repository.AccountRepository;
import com.restaurant.services.order.IOrderService;
import com.restaurant.services.order.OrderLine;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:integrated-system;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class SystemIntegrationTests {
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder encoder;
  @Autowired PaymentService payments;
  @Autowired MenuService menu;
  @Autowired EmployeeService employees;
  @Autowired AccountRepository accounts;
  @Autowired IOrderService orders;
  @Autowired ReportingService reports;
  @Autowired EntityManager entityManager;

  @BeforeAll
  void fixture() throws Exception {
    var populator = new ResourceDatabasePopulator();
    for (String name : List.of("schema.sql", "seed.sql", "demo-extra.sql")) {
      String sql =
          Files.readString(Path.of("..", "database", name), StandardCharsets.UTF_8)
              .replace(" ENGINE=InnoDB", "")
              .replace(" STORED", "")
              .replace("START TRANSACTION;", "")
              .replace("COMMIT;", "");
      populator.addScript(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
    }
    populator.execute(dataSource);
    int id = 9700;
    for (String role : List.of("MANAGER", "WAITER", "CASHIER", "CHEF")) {
      id++;
      jdbc.update(
          "INSERT INTO Employee(employee_id,full_name,position) VALUES(?,?,?)",
          id,
          "TEST " + role,
          role);
      jdbc.update(
          "INSERT INTO Account(account_id,employee_id,username,password_hash,role,is_active)"
              + " VALUES(?,?,?,?,?,true)",
          id,
          id,
          "test_" + role.toLowerCase(),
          encoder.encode("TEST_ONLY_2026"),
          role);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"manager", "waiter", "cashier", "chef"})
  void allFourRolesUseTheSharedDatabaseLogin(String role) throws Exception {
    mvc.perform(formLogin().user("test_" + role).password("TEST_ONLY_2026"))
        .andExpect(authenticated().withRoles(role.toUpperCase()))
        .andExpect(redirectedUrl("/home"));
  }

  @Test
  void wrongPasswordAndDisabledAccountCannotLogin() throws Exception {
    mvc.perform(formLogin().user("test_manager").password("wrong")).andExpect(unauthenticated());
    jdbc.update("UPDATE Account SET is_active=false WHERE username='test_waiter'");
    mvc.perform(formLogin().user("test_waiter").password("TEST_ONLY_2026"))
        .andExpect(unauthenticated());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/home",
        "/employees",
        "/employees/new",
        "/employees/9702/edit",
        "/tables",
        "/tables/new",
        "/tables/9004/edit",
        "/tables/9001",
        "/orders",
        "/orders/new?tableId=9004",
        "/orders/1042",
        "/menu",
        "/payments",
        "/invoices/1042",
        "/manager/orders",
        "/manager/reports",
        "/manager/payment-history"
      })
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void screensRenderAgainstOneSharedSchema(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "test_waiter", roles = "WAITER")
  void createOrderUsesTheActualEmployeeAndHistoricalPriceThenCancels() {
    var order = orders.createOrder(9004, "TEST integration", List.of(new OrderLine(9001, 2)));
    entityManager.flush();
    assertThat(order.getEmployee().getEmployeeId()).isEqualTo(9702);
    assertThat(order.getTotalAmount()).isEqualByComparingTo("1999.98");
    jdbc.update("UPDATE Menu_item SET unit_price=1.00 WHERE item_id=9001");
    assertThat(order.getDetails().getFirst().getUnitPriceAtOrder()).isEqualByComparingTo("999.99");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM Restaurant_tables WHERE table_id=9004", String.class))
        .isEqualTo("OCCUPIED");
    orders.cancelOrder(order.getId(), "TEST cancel");
    entityManager.flush();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM Restaurant_tables WHERE table_id=9004", String.class))
        .isEqualTo("AVAILABLE");
  }

  @Test
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void checkoutCreatesOnePaymentAndInvoiceAndReleasesTable() throws Exception {
    long id = payments.payOrder(1100, "Cash", true);
    assertThat(id).isPositive();
    assertThat(jdbc.queryForObject("SELECT status FROM Orders WHERE order_id=1100", String.class))
        .isEqualTo("COMPLETED");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM Restaurant_tables WHERE table_id=9010", String.class))
        .isEqualTo("AVAILABLE");
    assertThat(
            jdbc.queryForObject(
                "SELECT total_amount FROM Invoice WHERE order_id=1100", BigDecimal.class))
        .isEqualByComparingTo("39");
    assertThatThrownBy(() -> payments.payOrder(1100, "Cash", true))
        .isInstanceOf(IllegalStateException.class);
    mvc.perform(get("/invoices/1100"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("INV-1100")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "PREPARING", "READY"})
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void rejectsPaymentAttemptsBeforeAllItemsAreServed(String status) {
    jdbc.update("UPDATE Order_Detail SET preparation_status=? WHERE order_id=1100", status);
    assertThatThrownBy(() -> payments.payOrder(1100, "Cash", true))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> payments.payOrder(1100, "Cash", false))
        .isInstanceOf(IllegalStateException.class);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Payment WHERE order_id=1100", Long.class))
        .isZero();
  }

  @Test
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void recordsFailedPaymentForAServedOrderWithoutClosingIt() {
    payments.payOrder(1100, "Cash", false);
    assertThat(jdbc.queryForObject("SELECT status FROM Orders WHERE order_id=1100", String.class))
        .isEqualTo("OPEN");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM Restaurant_tables WHERE table_id=9010", String.class))
        .isEqualTo("OCCUPIED");
  }

  @ParameterizedTest
  @ValueSource(strings = {"AVAILABLE", "OCCUPIED", "OUT_OF_SERVICE"})
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void tableFiltersUseTheSharedEnum(String status) throws Exception {
    mvc.perform(get("/tables").param("status", status)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void invalidTableFilterIsRejected() throws Exception {
    mvc.perform(get("/tables").param("status", "INVALID")).andExpect(status().isBadRequest());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void invoiceFailureRollsBackPaymentOrderAndTableTogether() {
    long count =
        jdbc.queryForObject("SELECT COUNT(*) FROM Payment WHERE order_id=1100", Long.class);
    jdbc.update(
        "INSERT INTO Invoice(order_id,invoice_number,issued_at,total_amount)"
            + " VALUES(1100,'TEST-CONFLICT',CURRENT_TIMESTAMP,39)");
    try {
      assertThatThrownBy(() -> payments.payOrder(1100, "Cash", true))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
      assertThat(
              jdbc.queryForObject("SELECT COUNT(*) FROM Payment WHERE order_id=1100", Long.class))
          .isEqualTo(count);
      assertThat(jdbc.queryForObject("SELECT status FROM Orders WHERE order_id=1100", String.class))
          .isEqualTo("OPEN");
      assertThat(
              jdbc.queryForObject(
                  "SELECT status FROM Restaurant_tables WHERE table_id=9010", String.class))
          .isEqualTo("OCCUPIED");
    } finally {
      jdbc.update("DELETE FROM Invoice WHERE order_id=1100");
    }
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void checkoutAppearsInMemberFiveRevenueAndHistory() {
    jdbc.update("UPDATE Orders SET status='COMPLETED' WHERE order_id=1100");
    jdbc.update(
        "INSERT INTO Payment(order_id,amount,payment_method,status,payment_time)"
            + " VALUES(1100,39,'Cash','SUCCESSFUL','2026-11-05 12:30:00')");
    var range = new DateRange(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 5));
    assertThat(reports.report(range, Grouping.DAY).summary().totalRevenue())
        .isEqualByComparingTo("39");
  }

  @Test
  @WithMockUser(username = "test_waiter", roles = "WAITER")
  void aNoteAcceptedByTheOrderFormPersistsWithoutAnEntityValidationFailure() {
    String note = "N".repeat(500);
    var order = orders.createOrder(9004, note, List.of(new OrderLine(9001, 1)));
    entityManager.flush();
    assertThat(
            jdbc.queryForObject(
                "SELECT notes FROM Orders WHERE order_id=?", String.class, order.getId()))
        .isEqualTo(note);
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void menuChangesAndDeactivationPreserveOrderHistory() {
    menu.updateMenu(
        9001,
        "TEST renamed dish",
        "TEST category",
        "TEST description",
        new BigDecimal("29.50"),
        true);
    menu.deleteMenu(9001, true);
    assertThat(
            jdbc.queryForObject(
                "SELECT is_active FROM Menu_item WHERE item_id=9001", Boolean.class))
        .isFalse();
    assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM Order_Detail WHERE item_id=9001", Long.class))
        .isPositive();
    assertThat(
            jdbc.queryForObject(
                "SELECT item_name_at_order FROM Order_Detail WHERE order_id=1042 ORDER BY"
                    + " order_detail_id LIMIT 1",
                String.class))
        .isNotEqualTo("TEST renamed dish");
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void employeeManagementUsesBcryptAndChecksDuplicates() {
    employees.saveAccount(
        null,
        "TEST employee",
        "Waiter",
        " test_new_employee ",
        "TEST_password_2026",
        com.restaurant.entities.enums.Role.WAITER,
        true);
    var account = accounts.findByUsername("test_new_employee").orElseThrow();
    assertThat(encoder.matches("TEST_password_2026", account.getPasswordHash())).isTrue();
    assertThatThrownBy(
            () ->
                employees.saveAccount(
                    null,
                    "TEST duplicate",
                    "Waiter",
                    "test_new_employee",
                    "TEST_password_2026",
                    com.restaurant.entities.enums.Role.WAITER,
                    true))
        .isInstanceOf(IllegalArgumentException.class);
    employees.saveAccount(
        account.getId(),
        "TEST updated",
        "Waiter",
        "test_new_employee",
        "",
        com.restaurant.entities.enums.Role.WAITER,
        false);
    assertThat(accounts.findByUsername("test_new_employee").orElseThrow().isActive()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"WAITER", "CHEF", "CASHIER"})
  void otherRolesCannotUseManagerModules(String role) throws Exception {
    for (String path :
        List.of("/employees", "/menu", "/manager/reports", "/manager/payment-history"))
      mvc.perform(get(path).with(user("test_" + role.toLowerCase()).roles(role)))
          .andExpect(status().isForbidden());
  }

  @Test
  void csrfAndDeferredMemberThreeRoutesAreEnforced() throws Exception {
    mvc.perform(post("/menu").with(user("test_manager").roles("MANAGER")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/payments/1100/confirm").with(csrf()).with(user("test_manager").roles("MANAGER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/kitchen").with(user("test_chef").roles("CHEF")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/serving/4100").with(csrf()).with(user("test_waiter").roles("WAITER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/home").with(user("test_chef").roles("CHEF")))
        .andExpect(status().isOk())
        .andExpect(
            result ->
                assertThat(result.getResponse().getContentAsString().replaceAll("\\s+", " "))
                    .contains("kitchen module is integrated"));
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void roleAndLockActionsPreserveEmployeeAndPassword() throws Exception {
    var original = accounts.findById(9702).orElseThrow();
    var password = original.getPasswordHash();
    var name = original.getEmployee().getFullName();
    mvc.perform(post("/employees/9702/role").with(csrf()).param("role", "CASHIER"))
        .andExpect(redirectedUrl("/employees"));
    mvc.perform(post("/employees/9702/lock").with(csrf())).andExpect(redirectedUrl("/employees"));
    entityManager.flush();
    entityManager.clear();
    var changed = accounts.findById(9702).orElseThrow();
    assertThat(changed.getRole()).isEqualTo(com.restaurant.entities.enums.Role.CASHIER);
    assertThat(changed.isActive()).isFalse();
    assertThat(changed.getPasswordHash()).isEqualTo(password);
    assertThat(changed.getEmployee().getFullName()).isEqualTo(name);
  }

  @Test
  @WithMockUser(username = "test_waiter", roles = "WAITER")
  void waiterCannotChangeAccountsOrMenuCategories() throws Exception {
    mvc.perform(post("/employees/9703/lock").with(csrf())).andExpect(status().isForbidden());
    mvc.perform(post("/employees/9703/role").with(csrf()).param("role", "MANAGER"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/menu/categories/save").with(csrf()).param("name", "Forbidden"))
        .andExpect(status().isForbidden());
    for (String path :
        List.of("/orders?status=COMPLETED", "/orders?status=CANCELLED", "/orders/1042"))
      mvc.perform(get(path)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void categoryEditorValidatesNamesAndRetainsOrderSnapshots() {
    var snapshot =
        jdbc.queryForList(
            "SELECT item_name_at_order,unit_price_at_order FROM Order_Detail ORDER BY"
                + " order_detail_id");
    menu.saveCategory(null, "TEST category");
    long id =
        jdbc.queryForObject(
            "SELECT category_id FROM Menu_category WHERE category_name='TEST category'",
            Long.class);
    menu.saveCategory(id, "TEST renamed category");
    assertThat(menu.categories()).contains("TEST renamed category").doesNotContain("TEST category");
    assertThatThrownBy(() -> menu.saveCategory(null, "test renamed category"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> menu.saveCategory(null, " "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> menu.saveCategory(999999L, "Missing"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(
            jdbc.queryForList(
                "SELECT item_name_at_order,unit_price_at_order FROM Order_Detail ORDER BY"
                    + " order_detail_id"))
        .isEqualTo(snapshot);
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void mismatchedPasswordConfirmationDoesNotCreateAnAccount() throws Exception {
    mvc.perform(
            post("/employees/save")
                .with(csrf())
                .param("fullName", "TEST mismatch")
                .param("username", "test_mismatch")
                .param("role", "WAITER")
                .param("active", "on")
                .param("password", "Password_2026")
                .param("confirmPassword", "Different_2026"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Passwords do not match.")));
    assertThat(accounts.findByUsername("test_mismatch")).isEmpty();
  }

  @Test
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void checkoutIncludesClosedOrdersWithoutMakingThemPayable() throws Exception {
    var order = payments.checkoutOrders("1042").getFirst();
    assertThat(order.get("status")).isEqualTo("COMPLETED");
    assertThat(order.get("readyToPay")).isEqualTo(false);
    assertThat(payments.checkoutOrders("%' OR 1=1 --")).isEmpty();
    mvc.perform(get("/payments?orderId=1042"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("INV-1042")));
  }

  @Test
  @WithMockUser(username = "test_waiter", roles = "WAITER")
  void rejectedOrderRetainsDraftForCorrection() throws Exception {
    int itemId = orders.listAvailableItems().getFirst().id();
    var response =
        mvc.perform(
                post("/orders")
                    .with(csrf())
                    .param("tableId", "9001")
                    .param("lines[0].itemId", Integer.toString(itemId))
                    .param("lines[0].quantity", "2")
                    .param("note", "TEST draft on occupied table"))
            .andExpect(redirectedUrl("/orders/new?tableId=9001"))
            .andExpect(flash().attributeExists("form"))
            .andReturn();
    mvc.perform(get("/orders/new?tableId=9001").flashAttrs(response.getFlashMap()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("data-quantity=\"2\"")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("TEST draft on occupied table")));
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void sixCharacterPasswordsWorkForNewAndExistingAccounts() {
    employees.saveAccount(
        null,
        "TEST six characters",
        "Waiter",
        "test_six_password",
        "123456",
        com.restaurant.entities.enums.Role.WAITER,
        true);
    var created = accounts.findByUsername("test_six_password").orElseThrow();
    assertThat(encoder.matches("123456", created.getPasswordHash())).isTrue();
    var existing = accounts.findByUsername("test_waiter").orElseThrow();
    employees.saveAccount(
        existing.getId(),
        existing.getEmployee().getFullName(),
        existing.getEmployee().getPosition(),
        existing.getUsername(),
        "123456",
        existing.getRole(),
        existing.isActive());
    assertThat(encoder.matches("123456", existing.getPasswordHash())).isTrue();
    assertThatThrownBy(
            () ->
                employees.saveAccount(
                    null,
                    "TEST too short",
                    "Waiter",
                    "test_short_password",
                    "12345",
                    com.restaurant.entities.enums.Role.WAITER,
                    true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(accounts.findByUsername("test_short_password")).isEmpty();
  }
}
