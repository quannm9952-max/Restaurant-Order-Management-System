package com.restaurant.services.table.impl;

import com.restaurant.dto.table.TableView;
import com.restaurant.entities.RestaurantTable;
import com.restaurant.entities.enums.TableStatus;
import com.restaurant.exceptions.BusinessException;
import com.restaurant.exceptions.NotFoundException;
import com.restaurant.repository.OrderRepository;
import com.restaurant.repository.TableRepository;
import com.restaurant.services.table.ITableService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing restaurant tables (UC04). */
@Service
@Transactional(readOnly = true)
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('WAITER','MANAGER')")
public class TableServiceImpl implements ITableService {

  private final TableRepository tableRepository;
  private final OrderRepository orderRepository;

  public TableServiceImpl(TableRepository tableRepository, OrderRepository orderRepository) {
    this.tableRepository = tableRepository;
    this.orderRepository = orderRepository;
  }

  /** Get all tables as view DTOs. */
  @Override
  @Transactional(readOnly = true)
  public List<TableView> listTables() {
    return tableRepository.findAll().stream().map(this::toTableView).toList();
  }

  /** Get tables filtered by status as view DTOs. */
  @Override
  @Transactional(readOnly = true)
  public List<TableView> listTablesByStatus(String status) {
    return tableRepository.findByStatus(status).stream().map(this::toTableView).toList();
  }

  /** Get a table by ID as a view DTO. */
  @Override
  @Transactional(readOnly = true)
  public TableView getTable(Integer id) {
    RestaurantTable table =
        tableRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn với mã: " + id));
    return toTableView(table);
  }

  /** Get a table by ID (internal use for write operations). */
  private RestaurantTable getTableById(Integer id) {
    return tableRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn với mã: " + id));
  }

  /**
   * Update table status manually. Only allows AVAILABLE <-> OUT_OF_SERVICE transitions.
   *
   * <p>Rules: - Cannot manually set OCCUPIED (only system sets this when creating an order). -
   * Table with OPEN order cannot be changed to AVAILABLE or OUT_OF_SERVICE. - Cannot change to the
   * same status it already has.
   */
  @Override
  @Transactional
  public RestaurantTable updateTableStatus(Integer tableId, TableStatus newStatus) {
    RestaurantTable table =
        tableRepository
            .findByIdWithPessimisticLock(tableId)
            .orElseThrow(() -> new NotFoundException("Table not found."));

    // Cannot manually set OCCUPIED
    if (newStatus == TableStatus.OCCUPIED) {
      throw new BusinessException(
          "Không thể đặt thủ công trạng thái OCCUPIED. Trạng thái này chỉ được đặt tự động khi tạo"
              + " đơn.");
    }

    // Cannot change to the same status
    if (table.getStatus() == newStatus) {
      throw new BusinessException("Trạng thái không thay đổi.");
    }

    // Table with OPEN order cannot be changed
    if (orderRepository.existsOpenByTableId(tableId)) {
      throw new BusinessException("Bàn đang có đơn OPEN, không thể thay đổi trạng thái.");
    }

    table.setStatus(newStatus);
    return tableRepository.save(table);
  }

  /** Check if a table has an open order. */
  @Override
  public boolean hasOpenOrder(Integer tableId) {
    return orderRepository.existsOpenByTableId(tableId);
  }

  // ==================== Entity to View DTO mapping ====================

  private TableView toTableView(RestaurantTable table) {
    Integer currentOrderId = orderRepository.findOpenOrderIdByTableId(table.getId()).orElse(null);
    boolean hasOpen = currentOrderId != null;
    boolean canCreateOrder = table.getStatus() == TableStatus.AVAILABLE && !hasOpen;
    boolean canChangeStatus = !hasOpen && table.getStatus() != TableStatus.OCCUPIED;

    return new TableView(
        table.getId(),
        table.getTableName(),
        table.getCapacity(),
        table.getStatus(),
        currentOrderId,
        canCreateOrder,
        canChangeStatus);
  }
}
