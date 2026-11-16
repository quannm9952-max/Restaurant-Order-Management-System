package com.group09.restaurant.service;

import com.group09.restaurant.entity.Account;
import com.group09.restaurant.entity.Employee;
import com.group09.restaurant.entity.Role;
import com.group09.restaurant.repository.AccountRepository;
import com.group09.restaurant.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmployeeService {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(AccountRepository accountRepository,
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
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    }

    /**
     * Creates a new employee account (id == null) or updates an existing one (UC03).
     * Empty password on update means "keep the current password".
     */
    @Transactional
    public void saveAccount(Integer id,
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
            accountRepository.findByUsername(username)
                    .filter(other -> !other.getAccountId().equals(id))
                    .ifPresent(other -> {
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
