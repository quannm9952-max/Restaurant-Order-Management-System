package com.restaurant;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.restaurant.config.DemoAccounts;
import com.restaurant.dto.order.OrderLine;
import com.restaurant.dto.report.ReportModels.*;
import com.restaurant.entity.enums.Role;
import com.restaurant.repository.AccountRepository;
import com.restaurant.repository.EmployeeRepository;
import com.restaurant.security.AccountUserDetailsService;
import com.restaurant.support.Usernames;
import com.restaurant.service.EmployeeService;
import com.restaurant.service.MenuService;
import com.restaurant.service.OrderService;
import com.restaurant.service.PaymentService;
import com.restaurant.service.ReportService;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:integr"
    + "ated-system;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class SystemIntegrationTest {
  @Autowired
  DataSource dataSource;
  @Autowired
  JdbcTemplate jdbc;
  @Autowired
  MockMvc mvc;
  @Autowired
  PasswordEncoder encoder;
  @Autowired
  PaymentService payments;
  @Autowired
  MenuService menu;
  @Autowired
  EmployeeService employees;
  @Autowired
  AccountRepository accounts;
  @Autowired
  EmployeeRepository employeeRepository;
  @Autowired
  OrderService orders;
  @Autowired
  ReportService reports;
  @Autowired
  EntityManager entityManager;

  @BeforeAll
  void fixture() throws Exception {
    var populator = new ResourceDatabasePopulator();
    for (String name : List.of("schema.sql", "test-data.sql",
        "demo-data.sql")) {
      String sql = Files
          .readString(Path.of("..", "database", name), StandardCharsets.UTF_8)
          .replace(" ENGINE=InnoDB", "").replace(" STORED", "")
          .replace("START TRANSACTION;", "").replace("COMMIT;", "");
      populator.addScript(
          new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
    }
    populator.execute(dataSource);
    int id = 9700;
    for (String role : List.of("MANAGER", "WAITER", "CASHIER", "CHEF")) {
      id++;
      jdbc.update(
          "INSERT INTO Employee(employee_id,full_name,position) VALUES(?,?,?)",
          id, "TEST " + role, role);
      jdbc.update(
          "INSERT INTO Account(account_id,employee_"
              + "id,username,password_hash,role,is_active)"
              + " VALUES(?,?,?,?,?,true)",
          id, id, "test_" + role.toLowerCase(),
          encoder.encode("TEST_ONLY_2026"), role);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"manager", "waiter", "cashier", "chef"})
  void allFourRolesUseTheSharedDatabaseLogin(String role) throws Exception {
    var login = mvc
        .perform(formLogin().user("test_" + role).password("TEST_ONLY_2026"))
        .andExpect(authenticated().withUsername("test_" + role)
            .withRoles(role.toUpperCase()))
        .andExpect(redirectedUrl("/home")).andReturn();
    mvc.perform(get("/home")
        .session((MockHttpSession) login.getRequest().getSession(false)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers
            .containsString("Welcome, TEST " + role.toUpperCase())));
  }

  @ParameterizedTest
  @ValueSource(strings = {"manager", "waiter", "cashier", "chef"})
  void employeeDisplayNameCannotBeUsedInsteadOfUsername(String role)
      throws Exception {
    mvc.perform(formLogin().user("TEST " + role.toUpperCase())
        .password("TEST_ONLY_2026")).andExpect(unauthenticated())
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  @WithMockUser(username = "test_waiter", roles = "WAITER")
  void welcomeUsesTheCurrentFullEmployeeName() throws Exception {
    jdbc.update("UPDATE Employee SET full_name=? WHERE employee_id=9702",
        "Nguyễn Thị Minh Anh");
    entityManager.clear();
    var page = mvc.perform(get("/home")).andExpect(status().isOk()).andReturn()
        .getResponse().getContentAsString(StandardCharsets.UTF_8);
    assertThat(page).contains("Welcome, Nguyễn Thị Minh Anh")
        .doesNotContain("Welcome back, Nguyễn.");
  }

  @Test
  void managerCreatedEmployeeCanLoginByUsernameAndSeeTheirName()
      throws Exception {
    mvc.perform(
        post("/employees/save").with(user("test_manager").roles("MANAGER"))
            .with(csrf()).param("fullName", "Trần Minh Châu")
            .param("position", "WAITER").param("username", "Minh_Chau_01")
            .param("password", "123456").param("confirmPassword", "123456")
            .param("role", "WAITER").param("active", "on"))
        .andExpect(redirectedUrl("/employees?saved"));
    var login = mvc.perform(formLogin().user("Minh_Chau_01").password("123456"))
        .andExpect(
            authenticated().withUsername("Minh_Chau_01").withRoles("WAITER"))
        .andReturn();
    var page = mvc
        .perform(get("/home")
            .session((MockHttpSession) login.getRequest().getSession(false)))
        .andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
    assertThat(page).contains("Welcome, Trần Minh Châu");
  }

  @ParameterizedTest
  @ValueSource(strings = {"nguyễn", "đang_nhap", "ngu\u0303yen", "user name",
      "test.manager", "test-manager", "user@name", "user/name", "user\\name",
      "user😀", " test_manager", "test_manager ", "test\tmanager"})
  void invalidUsernamesCannotBeCreatedUpdatedOrUsedToLogin(String username)
      throws Exception {
    int accountCount = jdbc.queryForObject("SELECT COUNT(*) FROM Account",
        Integer.class);
    int employeeCount = jdbc.queryForObject("SELECT COUNT(*) FROM Employee",
        Integer.class);
    for (String id : List.of("", "9702")) {
      mvc.perform(
          post("/employees/save").with(user("test_manager").roles("MANAGER"))
              .with(csrf()).param("id", id).param("fullName", "Nguyễn Minh Anh")
              .param("position", "WAITER").param("username", username)
              .param("password", "123456").param("confirmPassword", "123456")
              .param("role", "WAITER").param("active", "on"))
          .andExpect(status().isOk())
          .andExpect(model().attribute("error", Usernames.INVALID_MESSAGE));
    }
    entityManager.clear();
    assertThat(
        jdbc.queryForObject("SELECT COUNT(*) FROM Account", Integer.class))
        .isEqualTo(accountCount);
    assertThat(
        jdbc.queryForObject("SELECT COUNT(*) FROM Employee", Integer.class))
        .isEqualTo(employeeCount);
    assertThat(accounts.findById(9702).orElseThrow().getUsername())
        .isEqualTo("test_waiter");
    assertThat(
        accounts.findById(9702).orElseThrow().getEmployee().getFullName())
        .isEqualTo("TEST WAITER");
    mvc.perform(formLogin().user(username).password("TEST_ONLY_2026"))
        .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void legacyInvalidUsernameCannotBypassLoginValidation() throws Exception {
    jdbc.update("UPDATE Account SET username=? WHERE account_id=9702",
        "legacy.name");
    entityManager.clear();
    mvc.perform(formLogin().user("legacy.name").password("TEST_ONLY_2026"))
        .andExpect(unauthenticated());
    var details = new AccountUserDetailsService(accounts);
    assertThatThrownBy(() -> details.loadUserByUsername("legacy.name"))
        .isInstanceOf(
            org.springframework.security.core.userdetails.UsernameNotFoundException.class);
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void usernamesAcceptAsciiLettersDigitsUnderscoreAndRespectLengthLimit() {
    for (String username : List.of("a", "123", "USER_01", "_",
        "u".repeat(50))) {
      employees.saveAccount(null, "Nguyễn Minh Anh", "WAITER", username,
          "123456", Role.WAITER, true);
      assertThat(accounts.findByUsername(username).orElseThrow().getEmployee()
          .getFullName()).isEqualTo("Nguyễn Minh Anh");
    }
    assertThatThrownBy(() -> employees.saveAccount(null, "Nguyễn Minh Anh",
        "WAITER", "u".repeat(51), "123456", Role.WAITER, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(Usernames.INVALID_MESSAGE);
  }

  @Test
  void demoSetupMakesAllEightSampleAccountsUsableWithoutReplacingEmployees()
      throws Exception {
    var setup = new DemoAccounts(accounts, employeeRepository, encoder,
        "123456");
    setup.run();
    for (Role role : Role.values()) {
      for (String username : List.of("demo_" + role.name().toLowerCase(),
          "DEMO_FIXTURE_" + role.name())) {
        var account = accounts.findByUsername(username).orElseThrow();
        assertThat(account.isActive()).isTrue();
        assertThat(encoder.matches("123456", account.getPasswordHash()))
            .isTrue();
        if (username.startsWith("DEMO_FIXTURE_"))
          assertThat(account.getEmployee().getId()).isEqualTo(account.getId());
        var login = mvc.perform(formLogin().user(username).password("123456"))
            .andExpect(
                authenticated().withUsername(username).withRoles(role.name()))
            .andReturn();
        var page = mvc
            .perform(get("/home").session(
                (MockHttpSession) login.getRequest().getSession(false)))
            .andExpect(status().isOk()).andReturn().getResponse()
            .getContentAsString();
        assertThat(page)
            .contains("Welcome, " + account.getEmployee().getFullName());
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"DEMO_FIXTURE_WAITER", "demo_waiter"})
  void demoRestartPreservesChangedPasswordsAndLockedAccounts(String username)
      throws Exception {
    var setup = new DemoAccounts(accounts, employeeRepository, encoder,
        "123456");
    setup.run();
    var account = accounts.findByUsername(username).orElseThrow();
    account.setActive(false);
    account.setPasswordHash(encoder.encode("CHANGED_TEST_PASSWORD"));
    accounts.saveAndFlush(account);
    setup.run();
    entityManager.clear();
    var unchanged = accounts.findByUsername(username).orElseThrow();
    assertThat(unchanged.isActive()).isFalse();
    assertThat(
        encoder.matches("CHANGED_TEST_PASSWORD", unchanged.getPasswordHash()))
        .isTrue();
    mvc.perform(formLogin().user(username).password("CHANGED_TEST_PASSWORD"))
        .andExpect(unauthenticated());
  }

  @Test
  void wrongPasswordAndDisabledAccountCannotLogin() throws Exception {
    mvc.perform(formLogin().user("test_manager").password("wrong"))
        .andExpect(unauthenticated());
    jdbc.update(
        "UPDATE Account SET is_active=false WHERE username='test_waiter'");
    mvc.perform(formLogin().user("test_waiter").password("TEST_ONLY_2026"))
        .andExpect(unauthenticated());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/home", "/employees", "/employees/new",
      "/employees/9702/edit", "/tables", "/tables/new", "/tables/9004/edit",
      "/tables/9001", "/orders", "/orders/new?tableId=9004", "/orders/1042",
      "/menu", "/payments", "/invoices/1042", "/manager/orders",
      "/manager/reports", "/manager/payment-history"})
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void screensRenderAgainstOneSharedSchema(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "test_waiter", roles = "WAITER")
  void createOrderUsesTheActualEmployeeAndHistoricalPriceThenCancels() {
    var order = orders.createOrder(9004, "TEST integration",
        List.of(new OrderLine(9001, 2)));
    entityManager.flush();
    assertThat(order.getEmployee().getId()).isEqualTo(9702);
    assertThat(order.getTotalAmount()).isEqualByComparingTo("1999.98");
    jdbc.update("UPDATE Menu_item SET unit_price=1.00 WHERE item_id=9001");
    assertThat(order.getDetails().getFirst().getUnitPriceAtOrder())
        .isEqualByComparingTo("999.99");
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Restaurant_tables WHERE table_id=9004",
        String.class)).isEqualTo("OCCUPIED");
    orders.cancelOrder(order.getId(), "TEST cancel");
    entityManager.flush();
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Restaurant_tables WHERE table_id=9004",
        String.class)).isEqualTo("AVAILABLE");
  }

  @Test
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void checkoutCreatesOnePaymentAndInvoiceAndReleasesTable() throws Exception {
    long id = payments.payOrder(1100, "Cash", true);
    assertThat(id).isPositive();
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Orders WHERE order_id=1100", String.class))
        .isEqualTo("COMPLETED");
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Restaurant_tables WHERE table_id=9010",
        String.class)).isEqualTo("AVAILABLE");
    assertThat(jdbc.queryForObject(
        "SELECT total_amount FROM Invoice WHERE order_id=1100",
        BigDecimal.class)).isEqualByComparingTo("39");
    assertThatThrownBy(() -> payments.payOrder(1100, "Cash", true))
        .isInstanceOf(IllegalStateException.class);
    mvc.perform(get("/invoices/1100")).andExpect(status().isOk()).andExpect(
        content().string(org.hamcrest.Matchers.containsString("INV-1100")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "PREPARING", "READY"})
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void rejectsPaymentAttemptsBeforeAllItemsAreServed(String status) {
    jdbc.update(
        "UPDATE Order_Detail SET preparation_status=? WHERE order_id=1100",
        status);
    assertThatThrownBy(() -> payments.payOrder(1100, "Cash", true))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> payments.payOrder(1100, "Cash", false))
        .isInstanceOf(IllegalStateException.class);
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Payment WHERE order_id=1100", Long.class))
        .isZero();
  }

  @Test
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void recordsFailedPaymentForAServedOrderWithoutClosingIt() {
    payments.payOrder(1100, "Cash", false);
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Orders WHERE order_id=1100", String.class))
        .isEqualTo("OPEN");
    assertThat(jdbc.queryForObject(
        "SELECT status FROM Restaurant_tables WHERE table_id=9010",
        String.class)).isEqualTo("OCCUPIED");
  }

  @ParameterizedTest
  @ValueSource(strings = {"AVAILABLE", "OCCUPIED", "OUT_OF_SERVICE"})
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void tableFiltersUseTheSharedEnum(String status) throws Exception {
    mvc.perform(get("/tables").param("status", status))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void invalidTableFilterIsRejected() throws Exception {
    mvc.perform(get("/tables").param("status", "INVALID"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void invoiceFailureRollsBackPaymentOrderAndTableTogether() {
    long count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM Payment WHERE order_id=1100", Long.class);
    jdbc.update(
        "INSERT INTO Invoice(order_id,invoice_number,issued_at,total_amount)"
            + " VALUES(1100,'TEST-CONFLICT',CURRENT_TIMESTAMP,39)");
    try {
      assertThatThrownBy(() -> payments.payOrder(1100, "Cash", true))
          .isInstanceOf(
              org.springframework.dao.DataIntegrityViolationException.class);
      assertThat(jdbc.queryForObject(
          "SELECT COUNT(*) FROM Payment WHERE order_id=1100", Long.class))
          .isEqualTo(count);
      assertThat(jdbc.queryForObject(
          "SELECT status FROM Orders WHERE order_id=1100", String.class))
          .isEqualTo("OPEN");
      assertThat(jdbc.queryForObject(
          "SELECT status FROM Restaurant_tables WHERE table_id=9010",
          String.class)).isEqualTo("OCCUPIED");
    } finally {
      jdbc.update("DELETE FROM Invoice WHERE order_id=1100");
    }
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void checkoutAppearsInMemberFiveRevenueAndHistory() {
    jdbc.update("UPDATE Orders SET status='COMPLETED' WHERE order_id=1100");
    jdbc.update("INSERT INTO Payment(order_id,amount"
        + ",payment_method,status,payment_time)"
        + " VALUES(1100,39,'Cash','SUCCESSFUL','2026-11-05 12:30:00')");
    var range = new DateRange(LocalDate.of(2026, 11, 5),
        LocalDate.of(2026, 11, 5));
    assertThat(reports.report(range, Grouping.DAY).summary().totalRevenue())
        .isEqualByComparingTo("39");
  }

  @Test
  @WithMockUser(username = "test_waiter", roles = "WAITER")
  void aNoteAcceptedByTheOrderFormPersistsWithoutAnEntityValidationFailure() {
    String note = "N".repeat(500);
    var order = orders.createOrder(9004, note, List.of(new OrderLine(9001, 1)));
    entityManager.flush();
    assertThat(jdbc.queryForObject("SELECT notes FROM Orders WHERE order_id=?",
        String.class, order.getId())).isEqualTo(note);
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void menuChangesAndDeactivationPreserveOrderHistory() {
    menu.updateMenu(9001, "TEST renamed dish", "TEST category",
        "TEST description", new BigDecimal("29.50"), true);
    menu.deleteMenu(9001, true);
    assertThat(jdbc.queryForObject(
        "SELECT is_active FROM Menu_item WHERE item_id=9001", Boolean.class))
        .isFalse();
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM Order_Detail WHERE item_id=9001", Long.class))
        .isPositive();
    assertThat(jdbc.queryForObject("SELECT item_name_at_order FROM "
        + "Order_Detail WHERE order_id=1042 ORDER BY"
        + " order_detail_id LIMIT 1", String.class))
        .isNotEqualTo("TEST renamed dish");
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void employeeManagementUsesBcryptAndChecksDuplicates() {
    employees.saveAccount(null, "TEST employee", "Waiter", "test_new_employee",
        "TEST_password_2026", com.restaurant.entity.enums.Role.WAITER, true);
    var account = accounts.findByUsername("test_new_employee").orElseThrow();
    assertThat(encoder.matches("TEST_password_2026", account.getPasswordHash()))
        .isTrue();
    assertThatThrownBy(() -> employees.saveAccount(null, "TEST duplicate",
        "Waiter", "test_new_employee", "TEST_password_2026",
        com.restaurant.entity.enums.Role.WAITER, true))
        .isInstanceOf(IllegalArgumentException.class);
    employees.saveAccount(account.getId(), "TEST updated", "Waiter",
        "test_new_employee", "", com.restaurant.entity.enums.Role.WAITER,
        false);
    assertThat(
        accounts.findByUsername("test_new_employee").orElseThrow().isActive())
        .isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"WAITER", "CHEF", "CASHIER"})
  void otherRolesCannotUseManagerModules(String role) throws Exception {
    for (String path : List.of("/employees", "/menu", "/manager/reports",
        "/manager/payment-history"))
      mvc.perform(
          get(path).with(user("test_" + role.toLowerCase()).roles(role)))
          .andExpect(status().isForbidden());
  }

  @Test
  void csrfAndKitchenServingRoleBoundariesAreEnforced() throws Exception {
    mvc.perform(post("/menu").with(user("test_manager").roles("MANAGER")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/payments/1100/confirm").with(csrf())
        .with(user("test_manager").roles("MANAGER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/kitchen").with(user("test_chef").roles("CHEF")))
        .andExpect(status().isOk());
    mvc.perform(get("/kitchen").with(user("test_waiter").roles("WAITER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/serving").with(user("test_waiter").roles("WAITER")))
        .andExpect(status().isOk());
    mvc.perform(get("/serving").with(user("test_chef").roles("CHEF")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/home").with(user("test_chef").roles("CHEF")))
        .andExpect(status().isOk())
        .andExpect(result -> assertThat(
            result.getResponse().getContentAsString().replaceAll("\\s+", " "))
            .contains("Kitchen Management"));
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void roleAndLockActionsPreserveEmployeeAndPassword() throws Exception {
    var original = accounts.findById(9702).orElseThrow();
    var password = original.getPasswordHash();
    var name = original.getEmployee().getFullName();
    mvc.perform(
        post("/employees/9702/role").with(csrf()).param("role", "CASHIER"))
        .andExpect(redirectedUrl("/employees"));
    mvc.perform(post("/employees/9702/lock").with(csrf()))
        .andExpect(redirectedUrl("/employees"));
    entityManager.flush();
    entityManager.clear();
    var changed = accounts.findById(9702).orElseThrow();
    assertThat(changed.getRole())
        .isEqualTo(com.restaurant.entity.enums.Role.CASHIER);
    assertThat(changed.isActive()).isFalse();
    assertThat(changed.getPasswordHash()).isEqualTo(password);
    assertThat(changed.getEmployee().getFullName()).isEqualTo(name);
  }

  @Test
  @WithMockUser(username = "test_waiter", roles = "WAITER")
  void waiterCannotChangeAccountsOrMenuCategories() throws Exception {
    mvc.perform(post("/employees/9703/lock").with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(
        post("/employees/9703/role").with(csrf()).param("role", "MANAGER"))
        .andExpect(status().isForbidden());
    mvc.perform(
        post("/menu/categories/save").with(csrf()).param("name", "Forbidden"))
        .andExpect(status().isForbidden());
    for (String path : List.of("/orders?status=COMPLETED",
        "/orders?status=CANCELLED", "/orders/1042"))
      mvc.perform(get(path)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void categoryEditorValidatesNamesAndRetainsOrderSnapshots() {
    var snapshot = jdbc.queryForList("SELECT item_name_at_order,unit_price"
        + "_at_order FROM Order_Detail ORDER BY" + " order_detail_id");
    menu.saveCategory(null, "TEST category");
    long id = jdbc.queryForObject("SELECT category_id FROM "
        + "Menu_category WHERE category_name='TEST category'", Long.class);
    menu.saveCategory(id, "TEST renamed category");
    assertThat(menu.categories()).contains("TEST renamed category")
        .doesNotContain("TEST category");
    assertThatThrownBy(() -> menu.saveCategory(null, "test renamed category"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> menu.saveCategory(null, " "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> menu.saveCategory(999999L, "Missing"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(jdbc.queryForList("SELECT item_name_at_order,unit_price"
        + "_at_order FROM Order_Detail ORDER BY" + " order_detail_id"))
        .isEqualTo(snapshot);
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void mismatchedPasswordConfirmationDoesNotCreateAnAccount() throws Exception {
    mvc.perform(
        post("/employees/save").with(csrf()).param("fullName", "TEST mismatch")
            .param("username", "test_mismatch").param("role", "WAITER")
            .param("active", "on").param("password", "Password_2026")
            .param("confirmPassword", "Different_2026"))
        .andExpect(status().isOk()).andExpect(content().string(
            org.hamcrest.Matchers.containsString("Passwords do not match.")));
    assertThat(accounts.findByUsername("test_mismatch")).isEmpty();
  }

  @Test
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void checkoutIncludesClosedOrdersWithoutMakingThemPayable() throws Exception {
    var order = payments.checkoutOrders("1042").getFirst();
    assertThat(order.get("status")).isEqualTo("COMPLETED");
    assertThat(order.get("readyToPay")).isEqualTo(false);
    assertThat(payments.checkoutOrders("%' OR 1=1 --")).isEmpty();
    mvc.perform(get("/payments?orderId=1042")).andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("INV-1042")));
  }

  @Test
  @WithMockUser(username = "test_waiter", roles = "WAITER")
  void rejectedOrderRetainsDraftForCorrection() throws Exception {
    int itemId = orders.listAvailableItems().getFirst().id();
    var response = mvc
        .perform(post("/orders").with(csrf()).param("tableId", "9001")
            .param("lines[0].itemId", Integer.toString(itemId))
            .param("lines[0].quantity", "2")
            .param("note", "TEST draft on occupied table"))
        .andExpect(redirectedUrl("/orders/new?tableId=9001"))
        .andExpect(flash().attributeExists("form")).andReturn();
    mvc.perform(
        get("/orders/new?tableId=9001").flashAttrs(response.getFlashMap()))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("data-quantity=\"2\"")))
        .andExpect(content().string(org.hamcrest.Matchers
            .containsString("TEST draft on occupied table")));
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void sixCharacterPasswordsWorkForNewAndExistingAccounts() {
    employees.saveAccount(null, "TEST six characters", "Waiter",
        "test_six_password", "123456", com.restaurant.entity.enums.Role.WAITER,
        true);
    var created = accounts.findByUsername("test_six_password").orElseThrow();
    assertThat(encoder.matches("123456", created.getPasswordHash())).isTrue();
    var existing = accounts.findByUsername("test_waiter").orElseThrow();
    employees.saveAccount(existing.getId(),
        existing.getEmployee().getFullName(),
        existing.getEmployee().getPosition(), existing.getUsername(), "123456",
        existing.getRole(), existing.isActive());
    assertThat(encoder.matches("123456", existing.getPasswordHash())).isTrue();
    assertThatThrownBy(() -> employees.saveAccount(null, "TEST too short",
        "Waiter", "test_short_password", "12345",
        com.restaurant.entity.enums.Role.WAITER, true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(accounts.findByUsername("test_short_password")).isEmpty();
  }

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  com.restaurant.repository.CheckoutRepository checkoutRepository;

  @ParameterizedTest
  @ValueSource(strings = {"/payments?orderId=abc", "/tables/abc", "/orders/abc",
      "/menu/abc/json"})
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void malformedRequestIs400InsteadOf500(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void unknownPageAndWrongMethodKeepTheirHttpStatus() throws Exception {
    mvc.perform(get("/css/does-not-exist.css"))
        .andExpect(status().isNotFound());
    mvc.perform(post("/menu/999999/json").with(csrf()))
        .andExpect(status().isMethodNotAllowed());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/menu/999999/json", "/payments/orders/999999/json"})
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void missingJsonRecordIs404(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Record not found."));
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void databaseFailureIsLoggedWithoutExposingSqlOrCredentials()
      throws Exception {
    org.mockito.Mockito
        .doThrow(new org.springframework.dao.DataAccessResourceFailureException(
            "SECRET jdbc:mysql://privatehost internal_table password=secret"))
        .when(checkoutRepository).order(999999L);
    var html = mvc.perform(get("/payments?orderId=999999"))
        .andExpect(status().isInternalServerError()).andReturn().getResponse()
        .getContentAsString();
    assertThat(html).contains("Reference:").doesNotContain("SECRET",
        "privatehost", "internal_table", "password=secret");
    mvc.perform(get("/payments/orders/999999/json"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error")
            .value("Unable to load details. Please try again."));
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void passwordsBeyondBcryptByteLimitLeaveAccountsUnchanged() {
    var original = accounts.findById(9702).orElseThrow().getPasswordHash();
    for (String password : List.of("x".repeat(73), "ế".repeat(25))) {
      assertThatThrownBy(() -> employees.saveAccount(9702, "TEST Waiter",
          "Waiter", "test_waiter", password,
          com.restaurant.entity.enums.Role.WAITER, true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("72 UTF-8 bytes");
    }
    entityManager.clear();
    assertThat(accounts.findById(9702).orElseThrow().getPasswordHash())
        .isEqualTo(original);
  }

  @Test
  @WithMockUser(username = "test_cashier", roles = "CASHIER")
  void aggregateCheckoutTotalsMatchHistoricalDetails() {
    for (var summary : payments.checkoutOrders("")) {
      long id = ((Number) summary.get("id")).longValue();
      var detail = payments.orderDetailForPayment(id);
      assertThat((BigDecimal) summary.get("totalAmount"))
          .isEqualByComparingTo((BigDecimal) detail.get("totalAmount"));
      for (String key : List.of("total_items", "served_items",
          "unserved_items"))
        assertThat(((Number) summary.get(key)).longValue())
            .isEqualTo(((Number) detail.get(key)).longValue());
      assertThat(summary.get("readyToPay")).isEqualTo(detail.get("readyToPay"));
    }
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void rejectedTableRetainsDraftAndDoesNotWriteDatabase() throws Exception {
    int before = jdbc.queryForObject("SELECT COUNT(*) FROM Restaurant_tables",
        Integer.class);
    mvc.perform(post("/tables/save")
        .with(csrf()).param("name", "TEST draft").param("capacity", "101"))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("value=\"TEST draft\"")))
        .andExpect(content()
            .string(org.hamcrest.Matchers.containsString("value=\"101\"")));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Restaurant_tables",
        Integer.class)).isEqualTo(before);
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void menuSearchTreatsPercentUnderscoreAndExclamationAsText() {
    menu.addMenu("TEST 50%_! literal", "TEST literal category", "",
        new BigDecimal("1.00"));
    assertThat(menu.searchMenu("%_!", null, null, null, null)).hasSize(1);
    assertThat(menu.searchMenu("%' OR 1=1 --", null, null, null, null))
        .isEmpty();
  }

  @Test
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void rejectedMenuRetainsDraftAndDoesNotWriteDatabase() throws Exception {
    long before = jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item",
        Long.class);
    var rejected = mvc
        .perform(post("/menu").with(csrf()).param("name", "TEST rejected dish")
            .param("category", "TEST category")
            .param("description", "TEST retained description")
            .param("price", "-1"))
        .andExpect(redirectedUrl("/menu"))
        .andExpect(flash().attributeExists("menuDraft")).andReturn();
    var page = mvc.perform(get("/menu").flashAttrs(rejected.getFlashMap()))
        .andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString();
    assertThat(page).contains("value=\"TEST rejected dish\"",
        "TEST retained description", "value=\"-1\"", "data-auto-open=\"true\"");
    assertThat(
        jdbc.queryForObject("SELECT COUNT(*) FROM Menu_item", Long.class))
        .isEqualTo(before);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"/orders/999999", "/tables/999999", "/employees/999999/edit"})
  @WithMockUser(username = "test_manager", roles = "MANAGER")
  void missingHtmlRecordIs404(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isNotFound());
  }
}
