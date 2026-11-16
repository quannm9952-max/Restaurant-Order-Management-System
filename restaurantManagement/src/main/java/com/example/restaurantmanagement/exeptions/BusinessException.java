package com.example.restaurantmanagement.exeptions;

/**
 * Thrown when a business rule is violated.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
