package com.restaurant.security;

import com.restaurant.entity.Account;
import com.restaurant.repository.AccountRepository;
import com.restaurant.support.Usernames;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tells Spring Security how to find a user: look the username up in the Account
 * table. Other team members never need to touch this file.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

  private final AccountRepository accountRepository;

  public AccountUserDetailsService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username)
      throws UsernameNotFoundException {
    if (!Usernames.isValid(username))
      throw new UsernameNotFoundException("Account not found");
    Account account = accountRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

    return User.withUsername(account.getUsername())
        .password(account.getPasswordHash()).roles(account.getRole().name())
        .disabled(!account.isActive()) // is_active = false -> cannot log in
        .build();
  }
}
