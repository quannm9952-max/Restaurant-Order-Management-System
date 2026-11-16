package com.example.restaurantmanagement.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * View DTO for displaying a summary of an open order in the orders list.
 */
public record OrderSummaryView(
        Integer id,
        String tableName,
        LocalDateTime createdAt,
        String employeeName,
        BigDecimal totalAmount,
        Integer itemCount
) {
}
