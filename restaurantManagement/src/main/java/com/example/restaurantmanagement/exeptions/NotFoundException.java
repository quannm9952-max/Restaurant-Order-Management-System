package com.example.restaurantmanagement.exeptions;

/**
 * Thrown when an expected entity is not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
