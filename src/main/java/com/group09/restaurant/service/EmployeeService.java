package com.group09.restaurant.service;

import com.group09.restaurant.entity.Account;
import com.group09.restaurant.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final AccountRepository accountRepository;

    public EmployeeService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }
}
