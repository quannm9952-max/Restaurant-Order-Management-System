package com.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "employee")
public class Employee {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employee_id")
  private Integer id;

  @Column(name = "full_name", nullable = false, length = 100)
  private String fullName;

  @Column(name = "phone_number", length = 15)
  private String phoneNumber;

  @Column(name = "email", length = 100)
  private String email;

  @Column(name = "position", length = 100)
  private String position;

  @Column(name = "hire_date")
  private LocalDate hireDate;

  @jakarta.persistence.OneToOne(mappedBy = "employee")
  private Account account;
}
