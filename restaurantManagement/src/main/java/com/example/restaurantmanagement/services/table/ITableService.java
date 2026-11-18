package com.example.restaurantmanagement.services.table;

import com.example.restaurantmanagement.dto.table.CreateTableForm;
import com.example.restaurantmanagement.dto.table.EditTableForm;
import com.example.restaurantmanagement.dto.table.MenuItemOption;
import com.example.restaurantmanagement.dto.table.TableView;
import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing restaurant tables (UC04).
 * // TODO(auth): WAITER, MANAGER - chỉ WAITER, MANAGER được cập nhật trạng thái bàn và xem đơn/bàn.
 */
public interface ITableService {

    /**
     * Get all tables as view DTOs.
     * // TODO(auth): WAITER, MANAGER
     *
     * @return list of all table views
     */
    List<TableView> listTables();

    /**
     * Get tables filtered by status as view DTOs.
     * // TODO(auth): WAITER, MANAGER
     *
     * @param status the status filter string
     * @return list of table views with the given status
     */
    List<TableView> listTablesByStatus(String status);

    /**
     * Get a table by ID as a view DTO.
     * // TODO(auth): WAITER, MANAGER
     *
     * @param id the table ID
     * @return the table view
     * @throws NotFoundException if table not found
     */
    TableView getTable(Integer id);

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

    /**
     * Count tables by status for filter tabs display.
     *
     * @return map of status name to count
     */
    Map<String, Long> countByStatus();

    /**
     * Add a new table. Only MANAGER can perform this action.
     * <p>
     * Rules:
     * - Name is required, trimmed, max 50 characters.
     * - Name must be unique (case-insensitive).
     * - Capacity must be >= 1 and <= 200.
     * - New table has status AVAILABLE.
     *
     * @param form the create table form data
     * @return the created table
     * @throws BusinessException if name is duplicate, capacity is invalid, or user is not MANAGER
     */
    RestaurantTable addTable(CreateTableForm form);

    /**
     * Edit an existing table (name and capacity only). Only MANAGER can perform this action.
     * <p>
     * Rules:
     * - Name is required, trimmed, max 50 characters.
     * - Name must not conflict with another table (case-insensitive), but keeping the same name is allowed.
     * - Capacity must be >= 1 and <= 200.
     * - Table with OPEN order can still be edited (assumption).
     *
     * @param id the table ID
     * @param form the edit table form data
     * @return the updated table
     * @throws NotFoundException if table not found
     * @throws BusinessException if name is duplicate, capacity is invalid, or user is not MANAGER
     */
    RestaurantTable editTable(Integer id, EditTableForm form);
}
