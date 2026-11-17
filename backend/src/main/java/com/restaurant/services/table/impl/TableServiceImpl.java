package com.restaurant.services.table.impl;

import org.springframework.security.access.prepost.PreAuthorize;

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
@PreAuthorize("hasAnyRole('WAITER','MANAGER')")
public class TableServiceImpl implements ITableService {

  private final TableRepository tableRepository;
  private final OrderRepository orderRepository;

  public TableServiceImpl(TableRepository tableRepository,
      OrderRepository orderRepository) {
    this.tableRepository = tableRepository;
    this.orderRepository = orderRepository;
  }

  /** Get all tables as view DTOs. */
  @Override
  @Transactional(readOnly = true)
  public List<TableView> listTables() {
    return toTableViews(tableRepository.findAll());
  }

  /** Get tables filtered by status as view DTOs. */
  @Override
  @Transactional(readOnly = true)
  public List<TableView> listTablesByStatus(String status) {
    try {
      var tableStatus = TableStatus
          .valueOf(status.toUpperCase(java.util.Locale.ROOT));
      return toTableViews(tableRepository.findByStatus(tableStatus));
    } catch (IllegalArgumentException exception) {
      throw new BusinessException("Trạng thái bàn không hợp lệ.");
    }
  }

  /** Get a table by ID as a view DTO. */
  @Override
  @Transactional(readOnly = true)
  public TableView getTable(Integer id) {
    RestaurantTable table = tableRepository.findById(id).orElseThrow(
        () -> new NotFoundException("Không tìm thấy bàn với mã: " + id));
    return toTableView(table);
  }

  /**
   * Update table status manually. Only allows AVAILABLE <-> OUT_OF_SERVICE
   * transitions.
   *
   * <p>
   * Rules: - Cannot manually set OCCUPIED (only system sets this when creating
   * an order). - Table with OPEN order cannot be changed to AVAILABLE or
   * OUT_OF_SERVICE. - Cannot change to the same status it already has.
   */
  @Override
  @Transactional
  public RestaurantTable updateTableStatus(Integer tableId,
      TableStatus newStatus) {
    RestaurantTable table = tableRepository.findByIdWithPessimisticLock(tableId)
        .orElseThrow(() -> new NotFoundException(
            "Không tìm thấy bàn với mã: " + tableId));

    // Cannot manually set OCCUPIED
    if (newStatus == TableStatus.OCCUPIED) {
      throw new BusinessException("Không thể đặt thủ công trạng thái OCCUPIED. "
          + "Trạng thái này chỉ được đặt tự động khi tạo" + " đơn.");
    }

    // Cannot change to the same status
    if (table.getStatus() == newStatus) {
      throw new BusinessException("Trạng thái không thay đổi.");
    }

    // Table with OPEN order cannot be changed
    if (orderRepository.existsOpenByTableId(tableId)) {
      throw new BusinessException(
          "Bàn đang có đơn OPEN, không thể thay đổi trạng thái.");
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

  private List<TableView> toTableViews(List<RestaurantTable> tables) {
    if (tables.isEmpty())
      return List.of();
    var openOrders = orderRepository.findOpenTableOrders().stream()
        .collect(java.util.stream.Collectors.toMap(
            OrderRepository.OpenTableOrder::getTableId,
            OrderRepository.OpenTableOrder::getOrderId));
    return tables.stream()
        .map(table -> toTableView(table, openOrders.get(table.getId())))
        .toList();
  }

  private TableView toTableView(RestaurantTable table) {
    return toTableView(table,
        orderRepository.findOpenOrderIdByTableId(table.getId()).orElse(null));
  }

  private TableView toTableView(RestaurantTable table, Integer currentOrderId) {
    boolean hasOpen = currentOrderId != null;
    boolean canCreateOrder = table.getStatus() == TableStatus.AVAILABLE
        && !hasOpen;
    boolean canChangeStatus = !hasOpen
        && table.getStatus() != TableStatus.OCCUPIED;

    return new TableView(table.getId(), table.getTableName(),
        table.getCapacity(), table.getStatus(), currentOrderId, canCreateOrder,
        canChangeStatus);
  }
}
