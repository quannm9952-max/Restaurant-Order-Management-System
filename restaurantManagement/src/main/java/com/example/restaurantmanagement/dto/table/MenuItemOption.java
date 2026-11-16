package com.example.restaurantmanagement.dto.table;

import java.math.BigDecimal;

/**
 * View DTO for a menu item option in the order creation form.
 */
public record MenuItemOption(
        Integer id,
        String itemName,
        BigDecimal price
) {
}
