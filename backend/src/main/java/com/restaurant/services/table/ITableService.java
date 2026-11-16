package com.restaurant.services.table;

import com.restaurant.dto.table.TableView;
import com.restaurant.entities.RestaurantTable;
import com.restaurant.entities.enums.TableStatus;
import com.restaurant.exceptions.BusinessException;
import com.restaurant.exceptions.NotFoundException;
import java.util.List;

/**
 * Service interface for managing restaurant tables (UC04). // TODO(auth): WAITER, MANAGER - chỉ
 * WAITER, MANAGER được cập nhật trạng thái bàn và xem đơn/bàn.
 */
public interface ITableService {

  /**
   * Get all tables as view DTOs. // TODO(auth): WAITER, MANAGER
   *
   * @return list of all table views
   */
  List<TableView> listTables();

  /**
   * Get tables filtered by status as view DTOs. // TODO(auth): WAITER, MANAGER
   *
   * @param status the status filter string
   * @return list of table views with the given status
   */
  List<TableView> listTablesByStatus(String status);

  /**
   * Get a table by ID as a view DTO. // TODO(auth): WAITER, MANAGER
   *
   * @param id the table ID
   * @return the table view
   * @throws NotFoundException if table not found
   */
  TableView getTable(Integer id);

  /**
   * Update table status manually. Only allows AVAILABLE <-> OUT_OF_SERVICE transitions.
   *
   * <p>Rules: - Cannot manually set OCCUPIED (only system sets this when creating an order). -
   * Table with OPEN order cannot be changed to AVAILABLE or OUT_OF_SERVICE. - Cannot change to the
   * same status it already has.
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
