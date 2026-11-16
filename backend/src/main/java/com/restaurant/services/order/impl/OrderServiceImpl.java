package com.restaurant.services.order.impl;

import com.restaurant.dto.order.OrderDetailView;
import com.restaurant.dto.order.OrderLineView;
import com.restaurant.dto.order.OrderSummaryView;
import com.restaurant.dto.table.MenuItemOption;
import com.restaurant.entities.*;
import com.restaurant.entities.enums.CookingStatus;
import com.restaurant.entities.enums.OrderStatus;
import com.restaurant.entities.enums.TableStatus;
import com.restaurant.exceptions.BusinessException;
import com.restaurant.exceptions.NotFoundException;
import com.restaurant.repository.*;
import com.restaurant.services.employee.ICurrentEmployeeService;
import com.restaurant.services.order.IOrderService;
import com.restaurant.services.order.OrderLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing orders (UC05). */
@Service
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('WAITER','MANAGER')")
public class OrderServiceImpl implements IOrderService {

  private final OrderRepository orderRepository;
  private final TableRepository tableRepository;
  private final MenuItemRepository menuItemRepository;
  private final PaymentRepository paymentRepository;
  private final ICurrentEmployeeService currentEmployeeService;

  public OrderServiceImpl(
      OrderRepository orderRepository,
      TableRepository tableRepository,
      MenuItemRepository menuItemRepository,
      PaymentRepository paymentRepository,
      ICurrentEmployeeService currentEmployeeService) {
    this.orderRepository = orderRepository;
    this.tableRepository = tableRepository;
    this.menuItemRepository = menuItemRepository;
    this.paymentRepository = paymentRepository;
    this.currentEmployeeService = currentEmployeeService;
  }

  /**
   * Create a new order for a table. Single transaction with pessimistic lock on the table.
   *
   * <p>Rules: - Table must exist and be AVAILABLE with no OPEN order. - Order must have at least 1
   * item line. - Each item must exist and be available. - On success: table becomes OCCUPIED, order
   * is OPEN.
   */
  @Override
  @Transactional
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('WAITER')")
  public Order createOrder(Integer tableId, String note, List<OrderLine> lines) {

    // Validate at least one line
    if (lines == null || lines.isEmpty()) {
      throw new BusinessException("Đơn phải có ít nhất 1 dòng món.");
    }

    // Lock the table row with PESSIMISTIC_WRITE
    RestaurantTable table =
        tableRepository
            .findByIdWithPessimisticLock(tableId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn với mã: " + tableId));

    // Check table status
    if (table.getStatus() != TableStatus.AVAILABLE) {
      if (table.getStatus() == TableStatus.OCCUPIED) {
        throw new BusinessException("Bàn đang được sử dụng (OCCUPIED), không thể tạo đơn.");
      } else {
        throw new BusinessException("Bàn đang ngoài phục vụ (OUT_OF_SERVICE), không thể tạo đơn.");
      }
    }

    // Double-check no OPEN order exists for this table (after lock)
    if (orderRepository.existsOpenByTableId(tableId)) {
      throw new BusinessException("Bàn đã có đơn OPEN, không thể tạo đơn mới.");
    }

    // Validate and create order details
    List<OrderDetail> details = new ArrayList<>();
    for (OrderLine line : lines) {
      if (line.getQuantity() < 1) {
        throw new BusinessException("Số lượng phải >= 1 cho tất cả các dòng món.");
      }

      MenuItem item =
          menuItemRepository
              .findById(line.getItemId())
              .orElseThrow(
                  () -> new NotFoundException("Không tìm thấy món với mã: " + line.getItemId()));

      if ((!Boolean.TRUE.equals(item.getIsAvailable())
          || !Boolean.TRUE.equals(item.getIsActive()))) {
        throw new BusinessException(
            "Món '" + item.getItemName() + "' đang ngừng bán, không thể thêm vào đơn.");
      }

      OrderDetail detail = new OrderDetail();
      detail.setMenuItem(item);
      detail.setQuantity(line.getQuantity());
      detail.setUnitPriceAtOrder(item.getPrice());
      detail.setItemNameAtOrder(item.getItemName());
      detail.setCookingStatus(CookingStatus.PENDING);
      details.add(detail);
    }

    // Create the order
    Order order = new Order();
    order.setTable(table);
    order.setEmployee(currentEmployeeService.getCurrentEmployee());
    order.setCreatedAt(LocalDateTime.now());
    order.setStatus(OrderStatus.OPEN);
    order.setNote(note);
    order.setDetails(details);

    // Set bidirectional relationship
    for (OrderDetail detail : details) {
      detail.setOrder(order);
    }

    // Calculate total amount
    BigDecimal total = calculateTotalAmount(details);
    order.setTotalAmount(total);

    // Set table to OCCUPIED
    table.setStatus(TableStatus.OCCUPIED);
    tableRepository.save(table);

    return orderRepository.save(order);
  }

  /** Add items to an existing OPEN order. */
  @Override
  @Transactional
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('WAITER')")
  public Order addItemToOrder(Integer orderId, List<OrderLine> lines) {

    if (lines == null || lines.isEmpty()) {
      throw new BusinessException("Phải có ít nhất 1 dòng món để thêm.");
    }

    Order order = getOrderWithDetails(orderId);
    if (order.getStatus() != OrderStatus.OPEN) {
      throw new BusinessException("Đơn không ở trạng thái OPEN, không thể thêm món.");
    }

    List<OrderDetail> newDetails = new ArrayList<>();
    for (OrderLine line : lines) {
      if (line.getQuantity() < 1) {
        throw new BusinessException("Số lượng phải >= 1.");
      }

      MenuItem item =
          menuItemRepository
              .findById(line.getItemId())
              .orElseThrow(
                  () -> new NotFoundException("Không tìm thấy món với mã: " + line.getItemId()));

      if ((!Boolean.TRUE.equals(item.getIsAvailable())
          || !Boolean.TRUE.equals(item.getIsActive()))) {
        throw new BusinessException(
            "Món '" + item.getItemName() + "' đang ngừng bán, không thể thêm vào đơn.");
      }

      OrderDetail detail = new OrderDetail();
      detail.setOrder(order);
      detail.setMenuItem(item);
      detail.setQuantity(line.getQuantity());
      detail.setUnitPriceAtOrder(item.getPrice());
      detail.setItemNameAtOrder(item.getItemName());
      detail.setCookingStatus(CookingStatus.PENDING);
      newDetails.add(detail);
    }

    order.getDetails().addAll(newDetails);

    // Recalculate total
    order.setTotalAmount(calculateTotalAmount(order.getDetails()));
    return orderRepository.save(order);
  }

  /**
   * Reduce quantity of an order detail line.
   *
   * <p>Rules: - Order must be OPEN. - Detail must be PENDING. - New quantity must be >= 1 and <
   * current quantity.
   */
  @Override
  @Transactional
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('WAITER')")
  public OrderDetail reduceItemQuantity(Integer orderId, Integer detailId, Integer newQuantity) {

    Order order = getOrderWithDetails(orderId);
    if (order.getStatus() != OrderStatus.OPEN) {
      throw new BusinessException("Đơn không ở trạng thái OPEN, không thể sửa số lượng.");
    }

    OrderDetail detail = findDetailInOrder(order, detailId);

    if (detail.getCookingStatus() != CookingStatus.PENDING) {
      throw new BusinessException(
          "Dòng món đang ở trạng thái "
              + detail.getCookingStatus()
              + ", chỉ có thể sửa khi đang PENDING.");
    }

    if (newQuantity < 1) {
      throw new BusinessException("Số lượng mới phải >= 1.");
    }

    if (newQuantity >= detail.getQuantity()) {
      throw new BusinessException(
          "Số lượng mới phải nhỏ hơn số lượng hiện tại ("
              + detail.getQuantity()
              + "). Muốn hủy món thì dùng 'Hủy dòng'.");
    }

    detail.setQuantity(newQuantity);

    // Recalculate total
    order.setTotalAmount(calculateTotalAmount(order.getDetails()));
    orderRepository.save(order);
    return detail;
  }

  /**
   * Cancel an order detail line (set cookingStatus to CANCELLED).
   *
   * <p>Rules: - Order must be OPEN. - Detail must be PENDING. - Does NOT delete the record, just
   * marks it CANCELLED.
   */
  @Override
  @Transactional
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('WAITER')")
  public OrderDetail cancelOrderDetail(Integer orderId, Integer detailId) {

    Order order = getOrderWithDetails(orderId);
    if (order.getStatus() != OrderStatus.OPEN) {
      throw new BusinessException("Đơn không ở trạng thái OPEN, không thể hủy dòng món.");
    }

    OrderDetail detail = findDetailInOrder(order, detailId);

    if (detail.getCookingStatus() != CookingStatus.PENDING) {
      throw new BusinessException(
          "Dòng món đang ở trạng thái "
              + detail.getCookingStatus()
              + ", chỉ có thể hủy khi đang PENDING.");
    }

    detail.setCookingStatus(CookingStatus.CANCELLED);

    // Recalculate total (excludes CANCELLED lines)
    order.setTotalAmount(calculateTotalAmount(order.getDetails()));
    orderRepository.save(order);
    return detail;
  }

  /**
   * Cancel the entire order.
   *
   * <p>Rules: - Order must be OPEN. - No detail line can be PREPARING, READY, or SERVED. - No
   * SUCCESSFUL payment for this order. - On success: all non-CANCELLED details become CANCELLED,
   * order becomes CANCELLED, totalAmount = 0, table becomes AVAILABLE.
   */
  @Override
  @Transactional
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('WAITER')")
  public Order cancelOrder(Integer orderId, String reason) {

    Order order = getOrderWithDetails(orderId);

    if (order.getStatus() != OrderStatus.OPEN) {
      throw new BusinessException("Đơn không ở trạng thái OPEN, không thể hủy.");
    }

    // Check no detail is PREPARING, READY, or SERVED
    for (OrderDetail detail : order.getDetails()) {
      CookingStatus status = detail.getCookingStatus();
      if (status == CookingStatus.PREPARING
          || status == CookingStatus.READY
          || status == CookingStatus.SERVED) {
        throw new BusinessException(
            "Có dòng món đang ở trạng thái "
                + status
                + ", không thể hủy đơn. Hãy hoàn thành hoặc hủy từng dòng món trước.");
      }
    }

    // Check no successful payment
    if (paymentRepository.existsSuccessfulByOrderId(orderId)) {
      throw new BusinessException("Đơn đã có thanh toán thành công, không thể hủy.");
    }

    // Cancel all non-CANCELLED details
    for (OrderDetail detail : order.getDetails()) {
      if (detail.getCookingStatus() != CookingStatus.CANCELLED) {
        detail.setCookingStatus(CookingStatus.CANCELLED);
      }
    }

    // Update order
    order.setStatus(OrderStatus.CANCELLED);
    order.setTotalAmount(BigDecimal.ZERO);
    if (reason != null && !reason.isBlank()) {
      String existingNote = order.getNote();
      order.setNote((existingNote != null ? existingNote + " | " : "") + "Lý do hủy: " + reason);
    }

    // Set table back to AVAILABLE
    RestaurantTable table = order.getTable();
    table.setStatus(TableStatus.AVAILABLE);
    tableRepository.save(table);

    return orderRepository.save(order);
  }

  /** Get all OPEN orders as summary views. */
  @Override
  @Transactional(readOnly = true)
  public List<OrderSummaryView> listOpenOrders() {
    return orderRepository.findByStatusWithDetails(OrderStatus.OPEN).stream()
        .map(this::toOrderSummaryView)
        .toList();
  }

  /** Get an order by ID with details as a view DTO. */
  @Override
  @Transactional(readOnly = true)
  public OrderDetailView getOrderDetail(Integer id) {
    Order order =
        orderRepository
            .findByIdWithDetails(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn với mã: " + id));
    return toOrderDetailView(order);
  }

  /** Get all available menu items for order creation. */
  @Override
  @Transactional(readOnly = true)
  public List<MenuItemOption> listAvailableItems() {
    return menuItemRepository.findByIsAvailableTrue().stream().map(this::toMenuItemOption).toList();
  }

  /** Get an order by ID with details (internal use for write operations). */
  private Order getOrderWithDetails(Integer id) {
    orderRepository.lockForUpdate(id).orElseThrow(() -> new NotFoundException("Order not found."));
    return orderRepository
        .findByIdWithDetails(id)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn với mã: " + id));
  }

  // ==================== Private helpers ====================

  /**
   * Calculate total amount from order details, excluding CANCELLED lines. This is the single method
   * for recalculating totals.
   */
  private BigDecimal calculateTotalAmount(List<OrderDetail> details) {
    BigDecimal total = BigDecimal.ZERO;
    for (OrderDetail detail : details) {
      if (detail.getCookingStatus() != CookingStatus.CANCELLED) {
        BigDecimal lineTotal =
            detail.getUnitPriceAtOrder().multiply(BigDecimal.valueOf(detail.getQuantity()));
        total = total.add(lineTotal);
      }
    }
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  /** Find an order detail by ID within an order's details list. */
  private OrderDetail findDetailInOrder(Order order, Integer detailId) {
    for (OrderDetail detail : order.getDetails()) {
      if (detail.getId().equals(detailId)) {
        return detail;
      }
    }
    throw new NotFoundException("Không tìm thấy dòng món với mã: " + detailId);
  }

  // ==================== Entity to View DTO mapping ====================

  private OrderSummaryView toOrderSummaryView(Order order) {
    int itemCount =
        (int)
            order.getDetails().stream()
                .filter(d -> d.getCookingStatus() != CookingStatus.CANCELLED)
                .count();
    return new OrderSummaryView(
        order.getId(),
        order.getTable().getTableName(),
        order.getCreatedAt(),
        order.getEmployee() != null ? order.getEmployee().getFullName() : "(Không rõ)",
        order.getTotalAmount(),
        itemCount);
  }

  private OrderDetailView toOrderDetailView(Order order) {
    boolean isOpen = order.getStatus() == OrderStatus.OPEN;
    List<OrderLineView> lines =
        order.getDetails().stream().map(d -> toOrderLineView(d, isOpen)).toList();

    // canCancelOrder: order must be OPEN, no PREPARING/READY/SERVED details, no successful payment
    boolean canCancelOrder =
        isOpen
            && !hasInProgressDetails(order)
            && !paymentRepository.existsSuccessfulByOrderId(order.getId());

    return new OrderDetailView(
        order.getId(),
        order.getTable().getId(),
        order.getTable().getTableName(),
        order.getStatus(),
        order.getCreatedAt(),
        order.getEmployee() != null ? order.getEmployee().getFullName() : "(Không rõ)",
        order.getNote(),
        order.getTotalAmount(),
        isOpen,
        canCancelOrder,
        lines);
  }

  private OrderLineView toOrderLineView(OrderDetail detail, boolean orderIsOpen) {
    boolean canEdit = orderIsOpen && detail.getCookingStatus() == CookingStatus.PENDING;
    BigDecimal subtotal =
        detail
            .getUnitPriceAtOrder()
            .multiply(BigDecimal.valueOf(detail.getQuantity()))
            .setScale(2, RoundingMode.HALF_UP);
    return new OrderLineView(
        detail.getId(),
        detail.getItemNameAtOrder(),
        detail.getQuantity(),
        detail.getUnitPriceAtOrder(),
        subtotal,
        detail.getCookingStatus(),
        canEdit);
  }

  private MenuItemOption toMenuItemOption(MenuItem item) {
    return new MenuItemOption(item.getId(), item.getItemName(), item.getPrice());
  }

  /** Check if any detail line is in PREPARING, READY, or SERVED state. */
  private boolean hasInProgressDetails(Order order) {
    for (OrderDetail detail : order.getDetails()) {
      CookingStatus status = detail.getCookingStatus();
      if (status == CookingStatus.PREPARING
          || status == CookingStatus.READY
          || status == CookingStatus.SERVED) {
        return true;
      }
    }
    return false;
  }
}
