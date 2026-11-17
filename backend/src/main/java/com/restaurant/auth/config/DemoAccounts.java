package com.restaurant.auth.config;

import com.restaurant.entities.*;
import com.restaurant.entities.enums.Role;
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
      String username = "demo_"
          + role.name().toLowerCase(java.util.Locale.ROOT);
      if (accounts.existsByUsername(username))
        continue;
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
}
