package com.group09.restaurant.config;

import com.group09.restaurant.entity.Account;
import com.group09.restaurant.entity.Employee;
import com.group09.restaurant.entity.Role;
import com.group09.restaurant.repository.AccountRepository;
import com.group09.restaurant.repository.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Creates TEST/DEMO accounts when the app starts (only if they do not exist yet).
 * All demo accounts use the fake password below. Not real data.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Demo@123";

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(AccountRepository accountRepository,
                          EmployeeRepository employeeRepository,
                          PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createIfMissing("demo_manager",  "DEMO Manager",  Role.MANAGER, true);
        createIfMissing("demo_waiter",   "DEMO Waiter",   Role.WAITER,  true);
        createIfMissing("demo_chef",     "DEMO Chef",     Role.CHEF,    true);
        createIfMissing("demo_cashier",  "DEMO Cashier",  Role.CASHIER, true);
        createIfMissing("demo_disabled", "DEMO Disabled", Role.WAITER,  false);
    }

    private void createIfMissing(String username, String fullName, Role role, boolean active) {
        if (accountRepository.existsByUsername(username)) {
            return;
        }
        Employee employee = new Employee();
        employee.setFullName(fullName);
        employee.setPosition(role.name());
        employee.setHireDate(LocalDate.now());
        employee = employeeRepository.save(employee);

        Account account = new Account();
        account.setEmployee(employee);
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        account.setRole(role);
        account.setActive(active);
        accountRepository.save(account);
    }
}
