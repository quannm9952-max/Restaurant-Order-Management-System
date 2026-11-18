package com.example.restaurantmanagement.dto.table;

import java.math.BigDecimal;

/**
 * View DTO for a menu item option in the order creation form.
 * categoryName is optional — null when category is not assigned.
 */
public record MenuItemOption(
        Integer id,
        String itemName,
        BigDecimal price,
        String categoryName
) {
}
