package com.restaurant.service;

import com.restaurant.entity.Employee;
import com.restaurant.exception.BusinessException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.repository.AccountRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentEmployeeService {
  private final AccountRepository accounts;

  public CurrentEmployeeService(AccountRepository accounts) {
    this.accounts = accounts;
  }

  @Transactional(readOnly = true)
  public Employee getCurrentEmployee() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()
        || auth instanceof AnonymousAuthenticationToken)
      throw new BusinessException("Please sign in before creating an order.");
    var account = accounts.findByUsername(auth.getName()).orElseThrow(
        () -> new NotFoundException("Signed-in account no longer exists."));
    if (!account.isActive())
      throw new BusinessException("This account has been deactivated.");
    return account.getEmployee();
  }
}
