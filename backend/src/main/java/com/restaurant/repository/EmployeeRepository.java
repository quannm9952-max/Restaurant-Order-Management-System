package com.restaurant.repository;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Query;

import com.restaurant.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
  @Query("select a.employee from Account " + "a where a.username = :username")
  java.util.Optional<Employee> findByAccountUsername(
      @Param("username") String username);
}
