package com.restaurant.auth.service;

import com.restaurant.entities.Account;
import com.restaurant.entities.Employee;
import com.restaurant.entities.enums.Role;
import com.restaurant.repository.AccountRepository;
import com.restaurant.repository.EmployeeRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@org.springframework.security.access.prepost.PreAuthorize("hasRole('MANAGER')")
public class EmployeeService {

  private final AccountRepository accountRepository;
  private final EmployeeRepository employeeRepository;
  private final PasswordEncoder passwordEncoder;

  public EmployeeService(
      AccountRepository accountRepository,
      EmployeeRepository employeeRepository,
      PasswordEncoder passwordEncoder) {
    this.accountRepository = accountRepository;
    this.employeeRepository = employeeRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public List<Account> listAccounts() {
    return accountRepository.findAll();
  }

  public Account getAccount(Integer id) {
    return accountRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Account not found."));
  }

  /**
   * Creates a new employee account (id == null) or updates an existing one (UC03). Empty password
   * on update means "keep the current password".
   */
  @Transactional
  public void saveAccount(
      Integer id,
      String fullName,
      String position,
      String username,
      String password,
      Role role,
      boolean active) {

    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("Full name is required.");
    }
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username is required.");
    }
    if (role == null) {
      throw new IllegalArgumentException("Role is required.");
    }

    if (fullName.trim().length() > 100
        || username.trim().length() > 50
        || (position != null && position.length() > 100))
      throw new IllegalArgumentException("Name, username or position exceeds its maximum length.");
    if (password != null && !password.isBlank() && password.length() < 8)
      throw new IllegalArgumentException("Password must have at least 8 characters.");
    fullName = fullName.trim();
    Account account;
    Employee employee;

    if (id == null) {
      if (password == null || password.isBlank()) {
        throw new IllegalArgumentException("Password is required for a new account.");
      }
      if (accountRepository.existsByUsername(username)) {
        throw new IllegalArgumentException("Username '" + username + "' is already taken.");
      }
      employee = new Employee();
      account = new Account();
    } else {
      account = getAccount(id);
      employee = account.getEmployee();
      accountRepository
          .findByUsername(username)
          .filter(other -> !other.getAccountId().equals(id))
          .ifPresent(
              other -> {
                throw new IllegalArgumentException("Username '" + username + "' is already taken.");
              });
    }

    employee.setFullName(fullName);
    employee.setPosition(position == null || position.isBlank() ? role.name() : position);
    if (employee.getHireDate() == null) {
      employee.setHireDate(LocalDate.now());
    }
    employee = employeeRepository.save(employee);

    account.setEmployee(employee);
    account.setUsername(username);
    account.setRole(role);
    account.setActive(active);
    if (password != null && !password.isBlank()) {
      account.setPasswordHash(passwordEncoder.encode(password));
    }
    accountRepository.save(account);
  }
}
