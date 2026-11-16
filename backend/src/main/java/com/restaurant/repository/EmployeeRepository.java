package com.restaurant.repository;

import com.restaurant.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
  @org.springframework.data.jpa.repository.Query(
      "select a.employee from Account a where a.username = :username")
  java.util.Optional<Employee> findByAccountUsername(
      @org.springframework.data.repository.query.Param("username") String username);
}
