package com.restaurant.dto.preparation;

import com.restaurant.entity.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public record PreparationOrder(Integer orderId, String tableName,
    LocalDateTime createdAt, String orderNote, List<PreparationLine> lines,
    OrderStatus orderStatus, long servedLineCount, long activeLineCount) {
  public boolean allServed() {
    return activeLineCount > 0 && servedLineCount == activeLineCount;
  }

  public String orderStatusLabel() {
    return orderStatus.name().substring(0, 1)
        + orderStatus.name().substring(1).toLowerCase(Locale.ROOT);
  }
}
