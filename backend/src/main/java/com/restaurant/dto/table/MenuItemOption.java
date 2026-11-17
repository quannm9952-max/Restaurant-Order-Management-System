package com.restaurant.dto.table;

import java.math.BigDecimal;

/** View DTO for a menu item option in the order creation form. */
public record MenuItemOption(
    Integer id, String itemName, BigDecimal price, String category, String imageUrl) {
  public MenuItemOption(Integer id, String itemName, BigDecimal price) {
    this(id, itemName, price, "Other", com.restaurant.web.MenuImages.forItemName(itemName));
  }
}
