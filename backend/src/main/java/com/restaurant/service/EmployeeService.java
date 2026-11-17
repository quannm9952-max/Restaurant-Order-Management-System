package com.restaurant.service;

import com.restaurant.entity.Account;
import com.restaurant.entity.Employee;
import com.restaurant.entity.enums.Role;
import com.restaurant.repository.AccountRepository;
import com.restaurant.repository.EmployeeRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('MANAGER')")
public class EmployeeService {

  private final AccountRepository accountRepository;
  private final EmployeeRepository employeeRepository;
  private final PasswordEncoder passwordEncoder;

  public EmployeeService(AccountRepository accountRepository,
      EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
    this.accountRepository = accountRepository;
    this.employeeRepository = employeeRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public List<Account> listAccounts() {
    return accountRepository.findAll();
  }

  public Account getAccount(Integer id) {
    return accountRepository.findById(id)
        .orElseThrow(() -> new com.restaurant.exception.NotFoundException(
            "Account not found."));
  }

  @Transactional
  public void assignRole(Integer id, Role role) {
    if (role == null)
      throw new IllegalArgumentException("Choose a valid role.");
    getAccount(id).setRole(role);
  }

  @Transactional
  public void lockAccount(Integer id) {
    getAccount(id).setActive(false);
  }

  /**
   * Creates a new employee account (id == null) or updates an existing one
   * (UC03). Empty password on update means "keep the current password".
   */
  @Transactional
  public void saveAccount(Integer id, String fullName, String position,
      String suppliedUsername, String password, Role role, boolean active) {
    String username = suppliedUsername == null ? null : suppliedUsername.trim();
    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("Full name is required.");
    }
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username is required.");
    }
    if (role == null) {
      throw new IllegalArgumentException("Role is required.");
    }

    if (fullName.trim().length() > 100 || username.trim().length() > 50
        || (position != null && position.length() > 100))
      throw new IllegalArgumentException(
          "Name, username or position exceeds its maximum length.");
    if (password != null && !password.isBlank() && password.length() < 6)
      throw new IllegalArgumentException(
          "Password must have at least 6 characters.");
    if (password != null && password
        .getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
      throw new IllegalArgumentException(
          "Password must not exceed 72 UTF-8 bytes.");
    }
    fullName = fullName.trim();
    Account account;
    Employee employee;

    if (id == null) {
      if (password == null || password.isBlank()) {
        throw new IllegalArgumentException(
            "Password is required for a new account.");
      }
      if (accountRepository.existsByUsername(username)) {
        throw new IllegalArgumentException(
            "Username '" + username + "' is already taken.");
      }
      employee = new Employee();
      account = new Account();
    } else {
      account = getAccount(id);
      employee = account.getEmployee();
      accountRepository.findByUsername(username)
          .filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new IllegalArgumentException(
                "Username '" + username + "' is already taken.");
          });
    }

    employee.setFullName(fullName);
    employee.setPosition(
        position == null || position.isBlank() ? role.name() : position);
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
