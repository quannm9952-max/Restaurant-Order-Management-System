package com.example.restaurantmanagement.services.table;

import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;

import java.util.List;

/**
 * Service interface for managing restaurant tables (UC04).
 * // TODO(auth): WAITER, MANAGER - chỉ WAITER, MANAGER được cập nhật trạng thái bàn và xem đơn/bàn.
 */
public interface ITableService {

    /**
     * Get all tables.
     * // TODO(auth): WAITER, MANAGER
     *
     * @return list of all tables
     */
    List<RestaurantTable> getAllTables();

    /**
     * Get tables filtered by status.
     * // TODO(auth): WAITER, MANAGER
     *
     * @param status the status filter string
     * @return list of tables with the given status
     */
    List<RestaurantTable> getTablesByStatus(String status);

    /**
     * Get a table by ID.
     * // TODO(auth): WAITER, MANAGER
     *
     * @param id the table ID
     * @return the table
     * @throws NotFoundException if table not found
     */
    RestaurantTable getTableById(Integer id);

    /**
     * Update table status manually. Only allows AVAILABLE <-> OUT_OF_SERVICE transitions.
     * <p>
     * Rules:
     * - Cannot manually set OCCUPIED (only system sets this when creating an order).
     * - Table with OPEN order cannot be changed to AVAILABLE or OUT_OF_SERVICE.
     * - Cannot change to the same status it already has.
     *
     * @param tableId the table ID
     * @param newStatus the new status (AVAILABLE or OUT_OF_SERVICE)
     * @return the updated table
     * @throws NotFoundException if table not found
     * @throws BusinessException if status is OCCUPIED, unchanged, or table has open order
     */
    RestaurantTable updateTableStatus(Integer tableId, TableStatus newStatus);

    /**
     * Check if a table has an open order.
     *
     * @param tableId the table ID
     * @return true if the table has an open order
     */
    boolean hasOpenOrder(Integer tableId);
}
