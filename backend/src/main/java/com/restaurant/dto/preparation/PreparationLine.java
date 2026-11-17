package com.restaurant.dto.preparation;

import com.restaurant.entities.enums.CookingStatus;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Kitchen/serving view of a saved order line; financial fields are not exposed.
 */
public record PreparationLine(Integer lineId, Integer orderId, Integer tableId,
    String tableName, String itemName, Integer quantity, CookingStatus status,
    LocalDateTime createdAt, String orderNote) {
  public String statusLabel() {
    return status.name().substring(0, 1)
        + status.name().substring(1).toLowerCase(Locale.ROOT);
  }
}
