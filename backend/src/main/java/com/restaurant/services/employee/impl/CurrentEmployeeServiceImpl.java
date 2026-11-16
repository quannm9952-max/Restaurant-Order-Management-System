package com.restaurant.services.employee.impl;

import com.restaurant.entities.Employee;
import com.restaurant.exceptions.BusinessException;
import com.restaurant.exceptions.NotFoundException;
import com.restaurant.repository.AccountRepository;
import com.restaurant.repository.EmployeeRepository;
import com.restaurant.services.employee.ICurrentEmployeeService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentEmployeeServiceImpl implements ICurrentEmployeeService {
  private final AccountRepository accounts;

  public CurrentEmployeeServiceImpl(EmployeeRepository employees, AccountRepository accounts) {
    this.accounts = accounts;
  }

  @Override
  @Transactional(readOnly = true)
  public Employee getCurrentEmployee() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
      throw new BusinessException("Please sign in before creating an order.");
    var account =
        accounts
            .findByUsername(auth.getName())
            .orElseThrow(() -> new NotFoundException("Signed-in account no longer exists."));
    if (!account.isActive()) throw new BusinessException("This account has been deactivated.");
    return account.getEmployee();
  }
}
