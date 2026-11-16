package com.example.restaurantmanagement.services.employee;

import com.example.restaurantmanagement.entities.Employee;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;

/**
 * TẠM THỜI - TV1 sẽ thay bằng nhân viên đang đăng nhập; chỉ sửa class này
 * <p>
 * Returns the current employee based on a configurable username property.
 * Default: waiter_demo. This is a temporary solution until Auth module (TV1) is merged.
 */
public interface ICurrentEmployeeService {

    /**
     * Get the current employee based on the configured username.
     *
     * @return the current employee
     * @throws NotFoundException if account not found
     * @throws BusinessException if account is inactive
     */
    Employee getCurrentEmployee();
}
