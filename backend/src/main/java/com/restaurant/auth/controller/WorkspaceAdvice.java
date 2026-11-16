package com.restaurant.auth.controller;

import com.restaurant.repository.AccountRepository;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class WorkspaceAdvice {
  private final AccountRepository accounts;

  public WorkspaceAdvice(AccountRepository accounts) {
    this.accounts = accounts;
  }

  @ModelAttribute("user")
  public Map<String, Object> currentUser(Authentication auth) {
    var user = new LinkedHashMap<String, Object>();
    if (auth == null) return user;
    String role =
        auth.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .filter(a -> a.startsWith("ROLE_"))
            .findFirst()
            .orElse("ROLE_WAITER")
            .substring(5);
    user.put("name", auth.getName());
    user.put("username", auth.getName());
    user.put("role", role.substring(0, 1) + role.substring(1).toLowerCase(Locale.ROOT));
    accounts
        .findByUsername(auth.getName())
        .ifPresent(
            a -> {
              user.put("id", a.getEmployee().getEmployeeId());
              user.put("name", a.getEmployee().getFullName());
            });
    return user;
  }
}
