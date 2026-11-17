package com.restaurant.repository;

import com.restaurant.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
  @Query("select a.employee from Account a where a.username = :username")
  java.util.Optional<Employee> findByAccountUsername(
      @Param("username") String username);
}
