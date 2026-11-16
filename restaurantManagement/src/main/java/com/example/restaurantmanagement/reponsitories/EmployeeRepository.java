package com.example.restaurantmanagement.reponsitories;

import com.example.restaurantmanagement.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    /**
     * Find employee by account username, joining through the account relationship.
     */
    @Query("SELECT e FROM Employee e JOIN FETCH e.account a WHERE a.username = :username")
    Optional<Employee> findByAccountUsername(@Param("username") String username);
}
