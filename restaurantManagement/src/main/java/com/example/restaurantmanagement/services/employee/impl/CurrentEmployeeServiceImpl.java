package com.example.restaurantmanagement.services.employee.impl;

import com.example.restaurantmanagement.entities.Account;
import com.example.restaurantmanagement.entities.Employee;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.reponsitories.AccountRepository;
import com.example.restaurantmanagement.reponsitories.EmployeeRepository;
import com.example.restaurantmanagement.services.employee.ICurrentEmployeeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * TẠM THỜI - TV1 sẽ thay bằng nhân viên đang đăng nhập; chỉ sửa class này
 * <p>
 * Returns the current employee based on a configurable username property.
 * Default: waiter_demo. This is a temporary solution until Auth module (TV1) is merged.
 */
@Service
public class CurrentEmployeeServiceImpl implements ICurrentEmployeeService {

    private String currentUsername;

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;

    public CurrentEmployeeServiceImpl(EmployeeRepository employeeRepository, AccountRepository accountRepository) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.currentUsername = "waiter_demo"; // default
    }

    /**
     * Setter for @Value injection — also allows tests to set the username directly.
     */
    @Value("${app.dev.current-username:waiter_demo}")
    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    /**
     * Get the current employee based on the configured username.
     * Throws BusinessException if the account is not found or is inactive.
     */
    @Override
    public Employee getCurrentEmployee() {
        Account account = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy tài khoản: " + currentUsername));

        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new BusinessException(
                    "Tài khoản '" + currentUsername + "' không hoạt động, không thể thực hiện thao tác.");
        }

        Employee employee = employeeRepository.findByAccountUsername(currentUsername)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy nhân viên cho tài khoản: " + currentUsername));

        return employee;
    }
}
