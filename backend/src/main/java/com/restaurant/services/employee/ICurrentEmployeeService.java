package com.restaurant.services.employee;

import com.restaurant.entities.Employee;
import com.restaurant.exceptions.BusinessException;
import com.restaurant.exceptions.NotFoundException;

/**
 * Resolves the Employee attached to the authenticated shared Account. Missing,
 * anonymous or disabled accounts cannot create an order.
 */
public interface ICurrentEmployeeService {

  /**
   * Get the current employee based on the configured username.
   *
   * @return the current employee
   * @throws NotFoundException
   *           if account not found
   * @throws BusinessException
   *           if account is inactive
   */
  Employee getCurrentEmployee();
}
