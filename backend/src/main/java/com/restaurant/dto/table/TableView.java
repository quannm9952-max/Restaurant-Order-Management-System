package com.restaurant.dto.table;

import com.restaurant.entities.enums.TableStatus;

/** View DTO for displaying a restaurant table. */
public record TableView(Integer id, String tableName, Integer capacity,
    TableStatus status, Integer currentOrderId, boolean canCreateOrder,
    boolean canChangeStatus) {
}
