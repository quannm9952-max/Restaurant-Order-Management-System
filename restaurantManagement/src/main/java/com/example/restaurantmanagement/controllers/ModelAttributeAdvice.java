package com.example.restaurantmanagement.controllers;

import com.example.restaurantmanagement.entities.Employee;
import com.example.restaurantmanagement.entities.enums.Role;
import com.example.restaurantmanagement.services.employee.ICurrentEmployeeService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds common model attributes to all views.
 * - currentUser: name, role display name, and initials for avatar.
 */
@ControllerAdvice
public class ModelAttributeAdvice {

    private final ICurrentEmployeeService currentEmployeeService;

    public ModelAttributeAdvice(ICurrentEmployeeService currentEmployeeService) {
        this.currentEmployeeService = currentEmployeeService;
    }

    /**
     * Add current user info to every model.
     * Tạm thời lấy từ CurrentEmployeeService; TV1 sẽ thay bằng nhân viên đang đăng nhập.
     */
    @ModelAttribute("currentUser")
    public UserContext getCurrentUser() {
        try {
            Employee emp = currentEmployeeService.getCurrentEmployee();
            String fullName = emp.getFullName();
            String initials = extractInitials(fullName);
            String roleDisplay = getRoleDisplayName(emp.getAccount().getRole());
            Role role = emp.getAccount().getRole();
            return new UserContext(fullName, roleDisplay, initials, role);
        } catch (Exception e) {
            // Fallback if employee service fails
            return new UserContext("Unknown", "No Role", "??", null);
        }
    }

    private String extractInitials(String name) {
        if (name == null || name.isBlank()) {
            return "??";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (Character.toString(parts[0].charAt(0))
                    + Character.toString(parts[parts.length - 1].charAt(0))).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String getRoleDisplayName(Role role) {
        if (role == null) return "No Role";
        return switch (role) {
            case WAITER -> "Waiter";
            case CHEF -> "Chef";
            case CASHIER -> "Cashier";
            case MANAGER -> "Manager";
        };
    }

    /**
     * Simple record for current user context.
     * Includes the Role enum for role-based UI rendering.
     */
    public record UserContext(String name, String role, String initials, Role roleEnum) {
    }
}
