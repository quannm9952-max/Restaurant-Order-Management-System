package com.restaurant.dto.preparation;

import java.time.LocalDateTime;
import java.util.List;

public record PreparationOrder(Integer orderId, String tableName,
    LocalDateTime createdAt, String orderNote, List<PreparationLine> lines) {
}
