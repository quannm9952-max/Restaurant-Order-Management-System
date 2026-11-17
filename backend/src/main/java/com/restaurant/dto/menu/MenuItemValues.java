package com.restaurant.dto.menu;

import java.math.BigDecimal;

/** Normalized menu values passed from business validation to persistence. */
public record MenuItemValues(String name, String category, String description,
    BigDecimal price) {
}
