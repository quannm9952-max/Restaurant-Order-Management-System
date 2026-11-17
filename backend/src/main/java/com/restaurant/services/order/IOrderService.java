package com.restaurant.services.order;

import com.restaurant.dto.order.OrderDetailView;
import com.restaurant.dto.order.OrderSummaryView;
import com.restaurant.dto.table.MenuItemOption;
import com.restaurant.entities.Order;
import com.restaurant.entities.OrderDetail;
import com.restaurant.exceptions.BusinessException;
import com.restaurant.exceptions.NotFoundException;
import java.util.List;

/**
 * Service interface for managing orders (UC05). Access: chỉ WAITER - chỉ WAITER được
 * tạo/sửa/hủy đơn.
 */
public interface IOrderService {

  /**
   * Create a new order for a table. Single transaction with pessimistic lock on the table.
   *
   * <p>Rules: - Table must exist and be AVAILABLE with no OPEN order. - Order must have at least 1
   * item line. - Each item must exist and be available. - On success: table becomes OCCUPIED, order
   * is OPEN.
   *
   * @param tableId the table ID
   * @param note optional note
   * @param lines list of order lines (itemId, quantity)
   * @return the created order
   * @throws NotFoundException if table or menu item not found
   * @throws BusinessException if table is not available, has open order, lines are empty, or item
   *     unavailable
   */
  Order createOrder(Integer tableId, String note, List<OrderLine> lines);

  /**
   * Add items to an existing OPEN order.
   *
   * @param orderId the order ID
   * @param lines list of order lines to add
   * @return the updated order
   * @throws NotFoundException if order or menu item not found
   * @throws BusinessException if order is not OPEN or lines are empty
   */
  Order addItemToOrder(Integer orderId, List<OrderLine> lines);

  /**
   * Reduce quantity of an order detail line.
   *
   * <p>Rules: - Order must be OPEN. - Detail must be PENDING. - New quantity must be >= 1 and <
   * current quantity.
   *
   * @param orderId the order ID
   * @param detailId the detail line ID
   * @param newQuantity the new quantity (>= 1, < current)
   * @return the updated order detail
   * @throws NotFoundException if order or detail not found
   * @throws BusinessException if order not OPEN, detail not PENDING, or quantity invalid
   */
  OrderDetail reduceItemQuantity(Integer orderId, Integer detailId, Integer newQuantity);

  /**
   * Cancel an order detail line (set cookingStatus to CANCELLED).
   *
   * <p>Rules: - Order must be OPEN. - Detail must be PENDING. - Does NOT delete the record, just
   * marks it CANCELLED.
   *
   * @param orderId the order ID
   * @param detailId the detail line ID
   * @return the cancelled order detail
   * @throws NotFoundException if order or detail not found
   * @throws BusinessException if order not OPEN or detail not PENDING
   */
  OrderDetail cancelOrderDetail(Integer orderId, Integer detailId);

  /**
   * Cancel the entire order.
   *
   * <p>Rules: - Order must be OPEN. - No detail line can be PREPARING, READY, or SERVED. - No
   * SUCCESSFUL payment for this order. - On success: all non-CANCELLED details become CANCELLED,
   * order becomes CANCELLED, totalAmount = 0, table becomes AVAILABLE.
   *
   * @param orderId the order ID
   * @param reason optional cancellation reason
   * @return the cancelled order
   * @throws NotFoundException if order not found
   * @throws BusinessException if order not OPEN, has preparing/ready/served details, or has
   *     successful payment
   */
  Order cancelOrder(Integer orderId, String reason);

  /**
   * Get all OPEN orders as summary views. Access: WAITER, MANAGER
   *
   * @return list of open order summary views
   */
  List<OrderSummaryView> listOpenOrders();

  /**
   * Get an order by ID with details as a view DTO. Access: WAITER, MANAGER
   *
   * @param id the order ID
   * @return the order detail view
   * @throws NotFoundException if order not found
   */
  OrderDetailView getOrderDetail(Integer id);

  /**
   * Get all available menu items for order creation.
   *
   * @return list of available menu item options
   */
  List<MenuItemOption> listAvailableItems();
}
