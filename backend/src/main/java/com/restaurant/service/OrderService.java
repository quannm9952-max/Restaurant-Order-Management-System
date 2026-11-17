package com.restaurant.service;

import com.restaurant.dto.menu.MenuItemOption;
import com.restaurant.dto.order.OrderDetailView;
import com.restaurant.dto.order.OrderLine;
import com.restaurant.dto.order.OrderLineView;
import com.restaurant.dto.order.OrderSummaryView;
import com.restaurant.entity.*;
import com.restaurant.entity.enums.CookingStatus;
import com.restaurant.entity.enums.OrderStatus;
import com.restaurant.entity.enums.TableStatus;
import com.restaurant.exception.BusinessException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order changes share one transaction and an Orders row lock. */
@Service
@PreAuthorize("hasAnyRole('WAITER','MANAGER')")
public class OrderService {
  private final OrderRepository orderRepository;
  private final TableRepository tableRepository;
  private final MenuItemRepository menuItemRepository;
  private final PaymentRepository paymentRepository;
  private final CurrentEmployeeService currentEmployeeService;
  private final MenuImageService images;

  public OrderService(OrderRepository orderRepository,
      TableRepository tableRepository, MenuItemRepository menuItemRepository,
      PaymentRepository paymentRepository,
      CurrentEmployeeService currentEmployeeService, MenuImageService images) {
    this.orderRepository = orderRepository;
    this.tableRepository = tableRepository;
    this.menuItemRepository = menuItemRepository;
    this.paymentRepository = paymentRepository;
    this.currentEmployeeService = currentEmployeeService;
    this.images = images;
  }

  @Transactional
  @PreAuthorize("hasRole('WAITER')")
  public Order createOrder(Integer tableId, String note,
      List<OrderLine> lines) {
    requireLines(lines);
    var table = lockAvailableTable(tableId);
    var order = new Order();
    order.setTable(table);
    order.setEmployee(currentEmployeeService.getCurrentEmployee());
    order.setCreatedAt(LocalDateTime.now());
    order.setStatus(OrderStatus.OPEN);
    order.setNote(note);
    order.setDetails(createDetails(order, lines));
    table.setStatus(TableStatus.OCCUPIED);
    tableRepository.save(table);
    return saveTotal(order);
  }

  @Transactional
  @PreAuthorize("hasRole('WAITER')")
  public Order addItemToOrder(Integer orderId, List<OrderLine> lines) {
    requireLines(lines);
    var order = requireOpenOrder(orderId, "thêm món");
    order.getDetails().addAll(createDetails(order, lines));
    return saveTotal(order);
  }

  @Transactional
  @PreAuthorize("hasRole('WAITER')")
  public OrderDetail reduceItemQuantity(Integer orderId, Integer detailId,
      Integer newQuantity) {
    var order = requireOpenOrder(orderId, "sửa số lượng");
    var detail = requirePendingDetail(order, detailId);
    if (newQuantity == null || newQuantity < 1)
      throw new BusinessException("Số lượng mới phải >= 1.");
    if (newQuantity >= detail.getQuantity())
      throw new BusinessException(
          "Số lượng mới phải nhỏ hơn số lượng hiện tại (" + detail.getQuantity()
              + "). Muốn hủy món thì dùng 'Hủy dòng'.");
    detail.setQuantity(newQuantity);
    saveTotal(order);
    return detail;
  }

  @Transactional
  @PreAuthorize("hasRole('WAITER')")
  public OrderDetail cancelOrderDetail(Integer orderId, Integer detailId) {
    var order = requireOpenOrder(orderId, "hủy dòng món");
    var detail = requirePendingDetail(order, detailId);
    detail.setCookingStatus(CookingStatus.CANCELLED);
    saveTotal(order);
    return detail;
  }

  @Transactional
  @PreAuthorize("hasRole('WAITER')")
  public Order cancelOrder(Integer orderId, String reason) {
    var order = requireOpenOrder(orderId, "hủy");
    requireCancellable(order);
    order.getDetails()
        .forEach(d -> d.setCookingStatus(CookingStatus.CANCELLED));
    order.setStatus(OrderStatus.CANCELLED);
    order.setTotalAmount(BigDecimal.ZERO);
    if (reason != null && !reason.isBlank()) {
      String previous = order.getNote();
      order.setNote(
          (previous == null ? "" : previous + " | ") + "Lý do hủy: " + reason);
    }
    var table = order.getTable();
    table.setStatus(TableStatus.AVAILABLE);
    tableRepository.save(table);
    return orderRepository.save(order);
  }

  /** Get all OPEN orders as summary views. */
  @Transactional(readOnly = true)
  public List<OrderSummaryView> listOpenOrders() {
    return listOrdersByStatus(OrderStatus.OPEN);
  }

  @Transactional(readOnly = true)
  public List<OrderSummaryView> listOrdersByStatus(OrderStatus status) {
    return orderRepository.findByStatusWithDetails(status).stream()
        .map(this::toOrderSummaryView).toList();
  }

  /** Get an order by ID with details as a view DTO. */
  @Transactional(readOnly = true)
  public OrderDetailView getOrderDetail(Integer id) {
    Order order = orderRepository.findByIdWithDetails(id).orElseThrow(
        () -> new NotFoundException("Không tìm thấy đơn với mã: " + id));
    return toOrderDetailView(order);
  }

  /** Get all available menu items for order creation. */
  @Transactional(readOnly = true)
  public List<MenuItemOption> listAvailableItems() {
    return menuItemRepository.findByIsAvailableTrue().stream()
        .map(this::toMenuItemOption).toList();
  }

  private void requireLines(List<OrderLine> lines) {
    if (lines == null || lines.isEmpty())
      throw new BusinessException("Đơn phải có ít nhất 1 dòng món.");
  }

  private RestaurantTable lockAvailableTable(Integer tableId) {
    var table = tableRepository.findByIdWithPessimisticLock(tableId)
        .orElseThrow(() -> new NotFoundException(
            "Không tìm thấy bàn với mã: " + tableId));
    if (table.getStatus() != TableStatus.AVAILABLE)
      throw new BusinessException(
          "Bàn đang " + table.getStatus() + ", không thể tạo đơn.");
    if (orderRepository.existsOpenByTableId(tableId))
      throw new BusinessException("Bàn đã có đơn OPEN, không thể tạo đơn mới.");
    return table;
  }

  private List<OrderDetail> createDetails(Order order, List<OrderLine> lines) {
    var details = new ArrayList<OrderDetail>();
    for (var line : lines) {
      if (line == null || line.quantity() == null || line.quantity() < 1)
        throw new BusinessException("Số lượng phải >= 1.");
      var item = menuItemRepository.findById(line.itemId())
          .orElseThrow(() -> new NotFoundException(
              "Không tìm thấy món với mã: " + line.itemId()));
      if (!Boolean.TRUE.equals(item.getIsAvailable())
          || !Boolean.TRUE.equals(item.getIsActive()))
        throw new BusinessException("Món '" + item.getItemName()
            + "' đang ngừng bán, không thể thêm vào đơn.");
      var detail = new OrderDetail();
      detail.setOrder(order);
      detail.setMenuItem(item);
      detail.setQuantity(line.quantity());
      detail.setUnitPriceAtOrder(item.getPrice());
      detail.setItemNameAtOrder(item.getItemName());
      detail.setCookingStatus(CookingStatus.PENDING);
      details.add(detail);
    }
    return details;
  }

  private Order requireOpenOrder(Integer id, String action) {
    orderRepository.lockForUpdate(id)
        .orElseThrow(() -> new NotFoundException("Order not found."));
    var order = orderRepository.findByIdWithDetails(id).orElseThrow(
        () -> new NotFoundException("Không tìm thấy đơn với mã: " + id));
    if (order.getStatus() != OrderStatus.OPEN)
      throw new BusinessException(
          "Đơn không ở trạng thái OPEN, không thể " + action + ".");
    return order;
  }

  private OrderDetail requirePendingDetail(Order order, Integer detailId) {
    var detail = order.getDetails().stream()
        .filter(d -> d.getId().equals(detailId)).findFirst()
        .orElseThrow(() -> new NotFoundException(
            "Không tìm thấy dòng món với mã: " + detailId));
    if (detail.getCookingStatus() != CookingStatus.PENDING)
      throw new BusinessException("Dòng món đang ở trạng thái "
          + detail.getCookingStatus() + ", chỉ có thể sửa khi đang PENDING.");
    return detail;
  }

  private void requireCancellable(Order order) {
    order.getDetails().stream().filter(this::isInProgress).findFirst()
        .ifPresent(detail -> {
          throw new BusinessException("Có dòng món đang ở trạng thái "
              + detail.getCookingStatus() + ", không thể hủy đơn.");
        });
    if (paymentRepository.existsSuccessfulByOrderId(order.getId()))
      throw new BusinessException(
          "Đơn đã có thanh toán thành công, không thể hủy.");
  }

  private Order saveTotal(Order order) {
    var total = order.getDetails().stream()
        .filter(d -> d.getCookingStatus() != CookingStatus.CANCELLED)
        .map(d -> d.getUnitPriceAtOrder()
            .multiply(BigDecimal.valueOf(d.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
    return orderRepository.save(order);
  }

  private boolean isInProgress(OrderDetail detail) {
    return detail.getCookingStatus() != CookingStatus.PENDING
        && detail.getCookingStatus() != CookingStatus.CANCELLED;
  }

  private OrderSummaryView toOrderSummaryView(Order order) {
    int itemCount = (int) order.getDetails().stream()
        .filter(d -> d.getCookingStatus() != CookingStatus.CANCELLED).count();
    return new OrderSummaryView(order.getId(), order.getTable().getTableName(),
        order.getCreatedAt(),
        order.getEmployee() != null
            ? order.getEmployee().getFullName()
            : "(Không rõ)",
        order.getTotalAmount(), itemCount);
  }

  private OrderDetailView toOrderDetailView(Order order) {
    boolean isOpen = order.getStatus() == OrderStatus.OPEN;
    List<OrderLineView> lines = order.getDetails().stream()
        .map(d -> toOrderLineView(d, isOpen)).toList();

    // canCancelOrder: order must be OPEN, no PREPARING/READY/SERVED details, no
    // successful payment
    boolean canCancelOrder = isOpen
        && !order.getDetails().stream().anyMatch(this::isInProgress)
        && !paymentRepository.existsSuccessfulByOrderId(order.getId());

    return new OrderDetailView(order.getId(), order.getTable().getId(),
        order.getTable().getTableName(), order.getStatus(),
        order.getCreatedAt(),
        order.getEmployee() != null
            ? order.getEmployee().getFullName()
            : "(Không rõ)",
        order.getNote(), order.getTotalAmount(), isOpen, canCancelOrder, lines);
  }

  private OrderLineView toOrderLineView(OrderDetail detail,
      boolean orderIsOpen) {
    boolean canEdit = orderIsOpen
        && detail.getCookingStatus() == CookingStatus.PENDING;
    BigDecimal subtotal = detail.getUnitPriceAtOrder()
        .multiply(BigDecimal.valueOf(detail.getQuantity()))
        .setScale(2, RoundingMode.HALF_UP);
    return new OrderLineView(detail.getId(), detail.getItemNameAtOrder(),
        detail.getQuantity(), detail.getUnitPriceAtOrder(), subtotal,
        detail.getCookingStatus(), canEdit);
  }

  private MenuItemOption toMenuItemOption(MenuItem item) {
    return new MenuItemOption(item.getId(), item.getItemName(), item.getPrice(),
        item.getMenuCategory() == null
            ? "Other"
            : item.getMenuCategory().getCategoryName(),
        images.url(item.getId(), item.getItemName()));
  }

}
