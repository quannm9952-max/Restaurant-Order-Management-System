package com.restaurant.repository;

import com.restaurant.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Integer> {

  @Override
  @EntityGraph(attributePaths = "employee")
  java.util.List<Account> findAll();

  @EntityGraph(attributePaths = "employee")
  Optional<Account> findByUsername(String username);

  boolean existsByUsername(String username);
}
