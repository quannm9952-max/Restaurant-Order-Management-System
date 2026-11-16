package com.restaurant.dto.order;

import com.restaurant.entities.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** View DTO for displaying full order detail information. */
public record OrderDetailView(
    Integer id,
    Integer tableId,
    String tableName,
    OrderStatus status,
    LocalDateTime createdAt,
    String employeeName,
    String note,
    BigDecimal totalAmount,
    boolean canEdit,
    boolean canCancelOrder,
    List<OrderLineView> lines) {}
