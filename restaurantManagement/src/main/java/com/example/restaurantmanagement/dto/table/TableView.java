package com.example.restaurantmanagement.dto.table;

import com.example.restaurantmanagement.entities.enums.TableStatus;

/**
 * View DTO for displaying a restaurant table.
 * orderCode is formatted as "ORD-xxxx" (4-digit zero-padded order ID).
 * canAddTable and canEditTable are true only for MANAGER role.
 */
public record TableView(
        Integer id,
        String tableName,
        Integer capacity,
        TableStatus status,
        Integer currentOrderId,
        String orderCode,
        boolean canCreateOrder,
        boolean canChangeStatus,
        boolean canAddTable,
        boolean canEditTable
) {
}
