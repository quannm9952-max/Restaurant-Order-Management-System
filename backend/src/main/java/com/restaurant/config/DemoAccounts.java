package com.restaurant.config;

import com.restaurant.entity.*;
import com.restaurant.entity.enums.Role;
import com.restaurant.repository.*;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoAccounts implements CommandLineRunner {
  private static final String LEGACY_PASSWORD_HASH = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20D3A3xH60JYdw6BY2Zr4oW17Q4p4K";
  private final AccountRepository accounts;
  private final EmployeeRepository employees;
  private final PasswordEncoder encoder;
  private final String password;

  public DemoAccounts(AccountRepository accounts, EmployeeRepository employees,
      PasswordEncoder encoder,
      @Value("${DEMO_PASSWORD:123456}") String password) {
    this.accounts = accounts;
    this.employees = employees;
    this.encoder = encoder;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (password.length() < 6)
      throw new IllegalArgumentException(
          "DEMO_PASSWORD must have at least 6 characters.");
    for (Role role : Role.values()) {
      activateLegacyAccount(role);
      createDemoAccount(role);
    }
  }

  private void activateLegacyAccount(Role role) {
    // Upgrade only untouched SQL fixtures; later password/lock edits survive
    // restarts.
    accounts.findByUsername("DEMO_FIXTURE_" + role.name())
        .filter(a -> a.getId() >= 9001 && a.getId() <= 9004)
        .filter(a -> a.getId().equals(a.getEmployee().getId()))
        .filter(a -> LEGACY_PASSWORD_HASH.equals(a.getPasswordHash()))
        .ifPresent(a -> {
          a.setPasswordHash(encoder.encode(password));
          a.setActive(true);
          accounts.save(a);
        });
  }

  private void createDemoAccount(Role role) {
    String username = "demo_" + role.name().toLowerCase(java.util.Locale.ROOT);
    if (accounts.existsByUsername(username))
      return;
    var employee = new Employee();
    employee.setFullName("DEMO " + role.name());
    employee.setPosition(role.name());
    employee.setHireDate(LocalDate.now());
    employees.save(employee);
    var account = new Account();
    account.setEmployee(employee);
    account.setUsername(username);
    account.setRole(role);
    account.setActive(true);
    account.setPasswordHash(encoder.encode(password));
    accounts.save(account);
  }
}
