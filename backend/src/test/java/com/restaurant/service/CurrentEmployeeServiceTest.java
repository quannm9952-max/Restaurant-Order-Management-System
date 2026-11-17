package com.restaurant.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.restaurant.entity.*;
import com.restaurant.exception.*;
import com.restaurant.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentEmployeeServiceTest {
  AccountRepository accounts = mock(AccountRepository.class);
  CurrentEmployeeService service = new CurrentEmployeeService(accounts);

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  void signedIn() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("real_waiter", "unused",
            java.util.List.of()));
  }

  @Test
  void usesTheSignedInEmployee() {
    signedIn();
    var a = new Account();
    var e = new Employee();
    e.setId(72);
    a.setEmployee(e);
    when(accounts.findByUsername("real_waiter")).thenReturn(Optional.of(a));
    assertEquals(72, service.getCurrentEmployee().getId());
  }

  @Test
  void rejectsAnonymous() {
    assertThrows(BusinessException.class, () -> service.getCurrentEmployee());
  }

  @Test
  void rejectsDisabledAccount() {
    signedIn();
    var a = new Account();
    a.setActive(false);
    when(accounts.findByUsername("real_waiter")).thenReturn(Optional.of(a));
    assertThrows(BusinessException.class, () -> service.getCurrentEmployee());
  }

  @Test
  void rejectsDeletedAccount() {
    signedIn();
    when(accounts.findByUsername("real_waiter")).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.getCurrentEmployee());
  }
}
