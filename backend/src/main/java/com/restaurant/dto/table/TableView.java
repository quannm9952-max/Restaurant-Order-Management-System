package com.restaurant.dto.table;

import com.restaurant.entity.enums.TableStatus;

/** View DTO for displaying a restaurant table. */
public record TableView(Integer id, String tableName, Integer capacity,
    TableStatus status, Integer currentOrderId, boolean canCreateOrder,
    boolean canChangeStatus) {
}
