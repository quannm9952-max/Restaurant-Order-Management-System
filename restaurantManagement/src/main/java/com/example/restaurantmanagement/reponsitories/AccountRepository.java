package com.example.restaurantmanagement.reponsitories;

import com.example.restaurantmanagement.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    /**
     * Find account by username.
     */
    Optional<Account> findByUsername(String username);
}
