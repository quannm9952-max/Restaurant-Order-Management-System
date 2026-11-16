package com.example.restaurantmanagement.services.employee.impl;

import com.example.restaurantmanagement.entities.Account;
import com.example.restaurantmanagement.entities.Employee;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.reponsitories.AccountRepository;
import com.example.restaurantmanagement.reponsitories.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentEmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CurrentEmployeeServiceImpl currentEmployeeService;

    private Account account;
    private Employee employee;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1);
        account.setUsername("waiter_demo");
        account.setIsActive(true);

        employee = new Employee();
        employee.setId(1);
        employee.setAccount(account);
        employee.setFullName("Waiter Demo");
    }

    @Test
    void getCurrentEmployee_shouldReturnEmployee_whenAccountExistsAndActive() {
        when(accountRepository.findByUsername("waiter_demo")).thenReturn(java.util.Optional.of(account));
        when(employeeRepository.findByAccountUsername("waiter_demo")).thenReturn(java.util.Optional.of(employee));

        Employee result = currentEmployeeService.getCurrentEmployee();

        assertNotNull(result);
        assertEquals("waiter_demo", result.getAccount().getUsername());
        assertEquals("Waiter Demo", result.getFullName());
    }

    @Test
    void getCurrentEmployee_shouldThrowNotFoundException_whenAccountNotFound() {
        when(accountRepository.findByUsername("waiter_demo")).thenReturn(java.util.Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> currentEmployeeService.getCurrentEmployee());
        assertTrue(ex.getMessage().contains("Không tìm thấy tài khoản"));
    }

    @Test
    void getCurrentEmployee_shouldThrowBusinessException_whenAccountInactive() {
        account.setIsActive(false);
        when(accountRepository.findByUsername("waiter_demo")).thenReturn(java.util.Optional.of(account));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> currentEmployeeService.getCurrentEmployee());
        assertTrue(ex.getMessage().contains("không hoạt động"));
    }

    @Test
    void getCurrentEmployee_shouldThrowNotFoundException_whenEmployeeNotFound() {
        when(accountRepository.findByUsername("waiter_demo")).thenReturn(java.util.Optional.of(account));
        when(employeeRepository.findByAccountUsername("waiter_demo")).thenReturn(java.util.Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> currentEmployeeService.getCurrentEmployee());
        assertTrue(ex.getMessage().contains("Không tìm thấy nhân viên"));
    }
}
