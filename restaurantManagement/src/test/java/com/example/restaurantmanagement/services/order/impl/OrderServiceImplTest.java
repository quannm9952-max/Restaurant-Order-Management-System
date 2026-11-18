package com.example.restaurantmanagement.services.order.impl;

import com.example.restaurantmanagement.dto.order.OrderDetailView;
import com.example.restaurantmanagement.dto.order.OrderLineView;
import com.example.restaurantmanagement.dto.order.OrderSummaryView;
import com.example.restaurantmanagement.dto.table.MenuItemOption;
import com.example.restaurantmanagement.entities.Account;
import com.example.restaurantmanagement.entities.Employee;
import com.example.restaurantmanagement.entities.MenuItem;
import com.example.restaurantmanagement.entities.Order;
import com.example.restaurantmanagement.entities.OrderDetail;
import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.CookingStatus;
import com.example.restaurantmanagement.entities.enums.OrderStatus;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.reponsitories.*;
import com.example.restaurantmanagement.services.employee.ICurrentEmployeeService;
import com.example.restaurantmanagement.services.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TableRepository tableRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ICurrentEmployeeService currentEmployeeService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private RestaurantTable table;
    private Employee employee;
    private MenuItem menuItem;
    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1);
        account.setUsername("waiter_demo");
        account.setIsActive(true);

        employee = new Employee();
        employee.setId(1);
        employee.setAccount(account);
        employee.setFullName("Waiter Demo");

        table = new RestaurantTable();
        table.setId(1);
        table.setTableName("Bàn 1");
        table.setCapacity(4);
        table.setStatus(TableStatus.AVAILABLE);

        menuItem = new MenuItem();
        menuItem.setId(1);
        menuItem.setItemName("Phở bò");
        menuItem.setPrice(new BigDecimal("55000"));
        menuItem.setIsAvailable(true);

        lenient().when(currentEmployeeService.getCurrentEmployee()).thenReturn(employee);
    }

    // ==================== CREATE ORDER TESTS ====================

    @Test
    void createOrder_validOrder_shouldSaveOrderAndSetTableOccupied() {
        OrderLine line = new OrderLine(1, 2);
        List<OrderLine> lines = List.of(line);

        when(tableRepository.findByIdWithPessimisticLock(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
        when(menuItemRepository.findById(1)).thenReturn(Optional.of(menuItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(1, "Góc yên tĩnh", lines);

        assertNotNull(order);
        assertEquals(OrderStatus.OPEN, order.getStatus());
        assertEquals(0, new BigDecimal("110000").compareTo(order.getTotalAmount()));
        assertEquals(1, order.getDetails().size());
        assertEquals("Phở bò", order.getDetails().get(0).getItemNameAtOrder());
        assertEquals(0, new BigDecimal("55000").compareTo(order.getDetails().get(0).getUnitPriceAtOrder()));
        assertEquals(2, order.getDetails().get(0).getQuantity());
        assertEquals(CookingStatus.PENDING, order.getDetails().get(0).getCookingStatus());
        assertEquals(employee, order.getEmployee());

        assertEquals(TableStatus.OCCUPIED, table.getStatus());
        verify(orderRepository).save(any(Order.class));
        verify(tableRepository).save(table);
    }

    @Test
    void createOrder_tableNotFound_shouldThrowNotFoundException() {
        OrderLine line = new OrderLine(1, 1);
        when(tableRepository.findByIdWithPessimisticLock(999)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> orderService.createOrder(999, null, List.of(line)));
        assertTrue(ex.getMessage().contains("Không tìm thấy bàn"));
    }

    @Test
    void createOrder_tableOccupied_shouldThrowBusinessException() {
        table.setStatus(TableStatus.OCCUPIED);
        OrderLine line = new OrderLine(1, 1);
        when(tableRepository.findByIdWithPessimisticLock(1)).thenReturn(Optional.of(table));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1, null, List.of(line)));
        assertTrue(ex.getMessage().contains("OCCUPIED"));
    }

    @Test
    void createOrder_tableOutOfService_shouldThrowBusinessException() {
        table.setStatus(TableStatus.OUT_OF_SERVICE);
        OrderLine line = new OrderLine(1, 1);
        when(tableRepository.findByIdWithPessimisticLock(1)).thenReturn(Optional.of(table));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1, null, List.of(line)));
        assertTrue(ex.getMessage().contains("OUT_OF_SERVICE"));
    }

    @Test
    void createOrder_tableHasOpenOrder_shouldThrowBusinessException() {
        OrderLine line = new OrderLine(1, 1);
        when(tableRepository.findByIdWithPessimisticLock(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1, null, List.of(line)));
        assertTrue(ex.getMessage().contains("đã có đơn OPEN"));
    }

    @Test
    void createOrder_emptyLines_shouldThrowBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1, null, List.of()));
        assertTrue(ex.getMessage().contains("ít nhất 1 dòng món"));
    }

    @Test
    void createOrder_nullLines_shouldThrowBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1, null, null));
        assertTrue(ex.getMessage().contains("ít nhất 1 dòng món"));
    }

    @Test
    void createOrder_quantityLessThanOne_shouldThrowBusinessException() {
        OrderLine line = new OrderLine(1, 0);
        when(tableRepository.findByIdWithPessimisticLock(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1, null, List.of(line)));
        assertTrue(ex.getMessage().contains(">= 1"));
    }

    @Test
    void createOrder_unavailableItem_shouldThrowBusinessException() {
        menuItem.setIsAvailable(false);
        OrderLine line = new OrderLine(1, 1);
        when(tableRepository.findByIdWithPessimisticLock(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
        when(menuItemRepository.findById(1)).thenReturn(Optional.of(menuItem));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1, null, List.of(line)));
        assertTrue(ex.getMessage().contains("đang ngừng bán"));
    }

    @Test
    void createOrder_itemNotFound_shouldThrowNotFoundException() {
        OrderLine line = new OrderLine(999, 1);
        when(tableRepository.findByIdWithPessimisticLock(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
        when(menuItemRepository.findById(999)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> orderService.createOrder(1, null, List.of(line)));
        assertTrue(ex.getMessage().contains("Không tìm thấy món"));
    }

    // ==================== ADD ITEM TESTS ====================

    private Order createOpenOrder() {
        Order order = new Order();
        order.setId(1);
        order.setTable(table);
        order.setEmployee(employee);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.OPEN);
        order.setTotalAmount(BigDecimal.ZERO);
        return order;
    }

    @Test
    void addItemToOrder_openOrder_shouldAddItemAndRecalculateTotal() {
        Order order = createOpenOrder();
        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        MenuItem newItem = new MenuItem();
        newItem.setId(2);
        newItem.setItemName("Cà phê");
        newItem.setPrice(new BigDecimal("25000"));
        newItem.setIsAvailable(true);
        when(menuItemRepository.findById(2)).thenReturn(Optional.of(newItem));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderLine line = new OrderLine(2, 3);
        Order result = orderService.addItemToOrder(1, List.of(line));

        assertEquals(1, result.getDetails().size());
        assertEquals("Cà phê", result.getDetails().get(0).getItemNameAtOrder());
        assertEquals(0, new BigDecimal("25000").compareTo(result.getDetails().get(0).getUnitPriceAtOrder()));
        assertEquals(3, result.getDetails().get(0).getQuantity());
        assertEquals(0, new BigDecimal("75000").compareTo(result.getTotalAmount()));
    }

    @Test
    void addItemToOrder_nonOpenOrder_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        OrderLine line = new OrderLine(1, 1);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.addItemToOrder(1, List.of(line)));
        assertTrue(ex.getMessage().contains("không ở trạng thái OPEN"));
    }

    // ==================== REDUCE QUANTITY TESTS ====================

    @Test
    void reduceItemQuantity_pendingDetail_shouldReduceAndRecalculate() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setMenuItem(menuItem);
        detail.setQuantity(5);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setItemNameAtOrder("Phở bò");
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);
        order.setTotalAmount(new BigDecimal("275000"));

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDetail result = orderService.reduceItemQuantity(1, 1, 3);

        assertEquals(3, result.getQuantity());
        assertEquals(0, new BigDecimal("165000").compareTo(order.getTotalAmount()));
    }

    @Test
    void reduceItemQuantity_nonPendingDetail_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(5);
        detail.setCookingStatus(CookingStatus.PREPARING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.reduceItemQuantity(1, 1, 3));
        assertTrue(ex.getMessage().contains("PREPARING"));
    }

    @Test
    void reduceItemQuantity_newQuantityTooHigh_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(3);
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.reduceItemQuantity(1, 1, 5));
        assertTrue(ex.getMessage().contains("nhỏ hơn số lượng hiện tại"));
    }

    @Test
    void reduceItemQuantity_newQuantityZero_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(3);
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.reduceItemQuantity(1, 1, 0));
        assertTrue(ex.getMessage().contains(">= 1"));
    }

    // ==================== CANCEL DETAIL TESTS ====================

    @Test
    void cancelOrderDetail_pendingDetail_shouldCancelAndRecalculate() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setMenuItem(menuItem);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setItemNameAtOrder("Phở bò");
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);
        order.setTotalAmount(new BigDecimal("110000"));

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDetail result = orderService.cancelOrderDetail(1, 1);

        assertEquals(CookingStatus.CANCELLED, result.getCookingStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getTotalAmount()));
    }

    @Test
    void cancelOrderDetail_nonPendingDetail_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setCookingStatus(CookingStatus.READY);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.cancelOrderDetail(1, 1));
        assertTrue(ex.getMessage().contains("READY"));
    }

    @Test
    void cancelOrderDetail_cancelledDetail_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setCookingStatus(CookingStatus.CANCELLED);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.cancelOrderDetail(1, 1));
        assertTrue(ex.getMessage().contains("CANCELLED"));
    }

    // ==================== PRICE IMMUTABILITY TEST ====================

    @Test
    void createOrder_priceShouldNotChange_whenMenuItemPriceChanges() {
        // Create order with item at price 55000
        OrderLine line = new OrderLine(1, 2);
        when(tableRepository.findByIdWithPessimisticLock(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
        when(menuItemRepository.findById(1)).thenReturn(Optional.of(menuItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(1, null, List.of(line));
        assertEquals(0, new BigDecimal("55000").compareTo(order.getDetails().get(0).getUnitPriceAtOrder()));

        // Change menu item price
        menuItem.setPrice(new BigDecimal("70000"));

        // The order detail should still have the old price
        assertEquals(0, new BigDecimal("55000").compareTo(order.getDetails().get(0).getUnitPriceAtOrder()));
        assertEquals(0, new BigDecimal("110000").compareTo(order.getTotalAmount()));
    }

    // ==================== CANCEL ORDER TESTS ====================

    @Test
    void cancelOrder_validOrder_shouldCancelAndFreeTable() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);
        order.setTotalAmount(new BigDecimal("110000"));

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(paymentRepository.existsSuccessfulByOrderId(1)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancelOrder(1, "Đổi ý");

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalAmount()));
        assertTrue(result.getNote().contains("Lý do hủy: Đổi ý"));
        assertEquals(TableStatus.AVAILABLE, table.getStatus());
        assertEquals(CookingStatus.CANCELLED, result.getDetails().get(0).getCookingStatus());
    }

    @Test
    void cancelOrder_nonOpenOrder_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1, null));
        assertTrue(ex.getMessage().contains("không ở trạng thái OPEN"));
    }

    @Test
    void cancelOrder_hasPreparingDetail_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setCookingStatus(CookingStatus.PREPARING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1, null));
        assertTrue(ex.getMessage().contains("PREPARING"));
    }

    @Test
    void cancelOrder_hasReadyDetail_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setCookingStatus(CookingStatus.READY);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1, null));
        assertTrue(ex.getMessage().contains("READY"));
    }

    @Test
    void cancelOrder_hasServedDetail_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setCookingStatus(CookingStatus.SERVED);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1, null));
        assertTrue(ex.getMessage().contains("SERVED"));
    }

    @Test
    void cancelOrder_hasSuccessfulPayment_shouldThrowBusinessException() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(paymentRepository.existsSuccessfulByOrderId(1)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1, null));
        assertTrue(ex.getMessage().contains("thanh toán thành công"));
    }

    @Test
    void cancelOrder_withCancelledAndPendingDetails_shouldCancelAll() {
        Order order = createOpenOrder();
        OrderDetail detail1 = new OrderDetail();
        detail1.setId(1);
        detail1.setOrder(order);
        detail1.setCookingStatus(CookingStatus.CANCELLED);
        detail1.setQuantity(1);
        detail1.setUnitPriceAtOrder(new BigDecimal("50000"));
        order.getDetails().add(detail1);

        OrderDetail detail2 = new OrderDetail();
        detail2.setId(2);
        detail2.setOrder(order);
        detail2.setCookingStatus(CookingStatus.PENDING);
        detail2.setQuantity(2);
        detail2.setUnitPriceAtOrder(new BigDecimal("30000"));
        order.getDetails().add(detail2);
        order.setTotalAmount(new BigDecimal("60000"));

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(paymentRepository.existsSuccessfulByOrderId(1)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancelOrder(1, null);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalAmount()));
        assertEquals(CookingStatus.CANCELLED, detail1.getCookingStatus());
        assertEquals(CookingStatus.CANCELLED, detail2.getCookingStatus());
    }

    // ==================== GET ORDER TESTS ====================

    @Test
    void getOrderDetail_notFound_shouldThrowNotFoundException() {
        when(orderRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> orderService.getOrderDetail(999));
        assertTrue(ex.getMessage().contains("Không tìm thấy đơn"));
    }

    // ==================== VIEW DTO TESTS ====================

    @Test
    void listOpenOrders_shouldReturnSummaryViews() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);
        order.setTotalAmount(new BigDecimal("110000"));

        when(orderRepository.findByStatusWithDetails(OrderStatus.OPEN)).thenReturn(List.of(order));

        List<OrderSummaryView> result = orderService.listOpenOrders();

        assertEquals(1, result.size());
        OrderSummaryView view = result.get(0);
        assertEquals("Bàn 1", view.tableName());
        assertEquals("Waiter Demo", view.employeeName());
        assertEquals(0, new BigDecimal("110000").compareTo(view.totalAmount()));
        assertEquals(1, view.itemCount());
    }

    @Test
    void listOpenOrders_itemCount_excludesCancelled() {
        Order order = createOpenOrder();
        OrderDetail detail1 = new OrderDetail();
        detail1.setId(1);
        detail1.setOrder(order);
        detail1.setQuantity(2);
        detail1.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail1.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail1);

        OrderDetail detail2 = new OrderDetail();
        detail2.setId(2);
        detail2.setOrder(order);
        detail2.setQuantity(1);
        detail2.setUnitPriceAtOrder(new BigDecimal("30000"));
        detail2.setCookingStatus(CookingStatus.CANCELLED);
        order.getDetails().add(detail2);

        when(orderRepository.findByStatusWithDetails(OrderStatus.OPEN)).thenReturn(List.of(order));

        List<OrderSummaryView> result = orderService.listOpenOrders();
        assertEquals(1, result.get(0).itemCount());
    }

    @Test
    void getOrderDetail_shouldReturnDetailView() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setMenuItem(menuItem);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setItemNameAtOrder("Phở bò");
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);
        order.setTotalAmount(new BigDecimal("110000"));

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(paymentRepository.existsSuccessfulByOrderId(1)).thenReturn(false);

        OrderDetailView view = orderService.getOrderDetail(1);

        assertEquals(1, view.id());
        assertEquals("Bàn 1", view.tableName());
        assertEquals(OrderStatus.OPEN, view.status());
        assertEquals("Waiter Demo", view.employeeName());
        assertTrue(view.canEdit());
        assertTrue(view.canCancelOrder());
        assertEquals(1, view.lines().size());
    }

    @Test
    void getOrderDetail_canEdit_false_whenOrderNotOpen() {
        Order order = createOpenOrder();
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        OrderDetailView view = orderService.getOrderDetail(1);

        assertFalse(view.canEdit());
        assertFalse(view.canCancelOrder());
    }

    @Test
    void getOrderDetail_canCancelOrder_false_whenHasPreparingDetail() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(1);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setCookingStatus(CookingStatus.PREPARING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        OrderDetailView view = orderService.getOrderDetail(1);

        assertTrue(view.canEdit()); // order is OPEN
        assertFalse(view.canCancelOrder());
    }

    @Test
    void getOrderDetail_canCancelOrder_false_whenHasSuccessfulPayment() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(1);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(paymentRepository.existsSuccessfulByOrderId(1)).thenReturn(true);

        OrderDetailView view = orderService.getOrderDetail(1);

        assertTrue(view.canEdit());
        assertFalse(view.canCancelOrder());
    }

    @Test
    void getOrderDetail_lineCanEdit_true_whenOpenAndPending() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setItemNameAtOrder("Phở bò");
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(paymentRepository.existsSuccessfulByOrderId(1)).thenReturn(false);

        OrderDetailView view = orderService.getOrderDetail(1);
        OrderLineView line = view.lines().get(0);

        assertTrue(line.canEdit());
        assertEquals(1, line.detailId());
        assertEquals("Phở bò", line.itemNameAtOrder());
        assertEquals(CookingStatus.PENDING, line.cookingStatus());
        assertEquals(0, new BigDecimal("110000").compareTo(line.subtotal()));
    }

    @Test
    void getOrderDetail_lineCanEdit_false_whenPreparing() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setItemNameAtOrder("Phở bò");
        detail.setCookingStatus(CookingStatus.PREPARING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        OrderDetailView view = orderService.getOrderDetail(1);
        assertFalse(view.lines().get(0).canEdit());
    }

    @Test
    void getOrderDetail_lineCanEdit_false_whenReady() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setCookingStatus(CookingStatus.READY);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        assertFalse(orderService.getOrderDetail(1).lines().get(0).canEdit());
    }

    @Test
    void getOrderDetail_lineCanEdit_false_whenServed() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setCookingStatus(CookingStatus.SERVED);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        assertFalse(orderService.getOrderDetail(1).lines().get(0).canEdit());
    }

    @Test
    void getOrderDetail_lineCanEdit_false_whenCancelled() {
        Order order = createOpenOrder();
        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setCookingStatus(CookingStatus.CANCELLED);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(paymentRepository.existsSuccessfulByOrderId(1)).thenReturn(false);

        assertFalse(orderService.getOrderDetail(1).lines().get(0).canEdit());
    }

    @Test
    void listAvailableItems_shouldReturnMenuItemOptions() {
        when(menuItemRepository.findByIsAvailableTrueWithCategory()).thenReturn(List.of(menuItem));

        List<MenuItemOption> result = orderService.listAvailableItems();

        assertEquals(1, result.size());
        MenuItemOption opt = result.get(0);
        assertEquals(1, opt.id());
        assertEquals("Phở bò", opt.itemName());
        assertEquals(0, new BigDecimal("55000").compareTo(opt.price()));
    }

    // ==================== TOTAL AMOUNT EXCLUDES CANCELLED LINES ====================

    /**
     * Simulates order #1 from seed data:
     * - Phở bò 2 x 55,000 (PENDING) = 110,000
     * - Gỏi cuốn 1 x 35,000 (PREPARING) = 35,000
     * - Cơm gà 1 x 50,000 (READY) = 50,000
     * - Trà đá 2 x 5,000 (SERVED) = 10,000
     * - Cà phê sữa 1 x 25,000 (CANCELLED) = 25,000 (excluded)
     * Total should be 205,000 (not 230,000).
     * Even if order.totalAmount in DB is wrong (230,000), the view should recalculate correctly.
     */
    @Test
    void getOrderDetail_totalAmount_excludesCancelledLines() {
        Order order = createOpenOrder();
        // Simulate DB totalAmount that incorrectly includes CANCELLED lines
        order.setTotalAmount(new BigDecimal("230000"));

        // Phở bò 2 x 55,000 (PENDING)
        OrderDetail detail1 = new OrderDetail();
        detail1.setId(1);
        detail1.setOrder(order);
        detail1.setQuantity(2);
        detail1.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail1.setItemNameAtOrder("Phở bò");
        detail1.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail1);

        // Gỏi cuốn 1 x 35,000 (PREPARING)
        OrderDetail detail2 = new OrderDetail();
        detail2.setId(2);
        detail2.setOrder(order);
        detail2.setQuantity(1);
        detail2.setUnitPriceAtOrder(new BigDecimal("35000"));
        detail2.setItemNameAtOrder("Gỏi cuốn");
        detail2.setCookingStatus(CookingStatus.PREPARING);
        order.getDetails().add(detail2);

        // Cơm gà 1 x 50,000 (READY)
        OrderDetail detail3 = new OrderDetail();
        detail3.setId(3);
        detail3.setOrder(order);
        detail3.setQuantity(1);
        detail3.setUnitPriceAtOrder(new BigDecimal("50000"));
        detail3.setItemNameAtOrder("Cơm gà");
        detail3.setCookingStatus(CookingStatus.READY);
        order.getDetails().add(detail3);

        // Trà đá 2 x 5,000 (SERVED)
        OrderDetail detail4 = new OrderDetail();
        detail4.setId(4);
        detail4.setOrder(order);
        detail4.setQuantity(2);
        detail4.setUnitPriceAtOrder(new BigDecimal("5000"));
        detail4.setItemNameAtOrder("Trà đá");
        detail4.setCookingStatus(CookingStatus.SERVED);
        order.getDetails().add(detail4);

        // Cà phê sữa 1 x 25,000 (CANCELLED) — should be excluded
        OrderDetail detail5 = new OrderDetail();
        detail5.setId(5);
        detail5.setOrder(order);
        detail5.setQuantity(1);
        detail5.setUnitPriceAtOrder(new BigDecimal("25000"));
        detail5.setItemNameAtOrder("Cà phê sữa");
        detail5.setCookingStatus(CookingStatus.CANCELLED);
        order.getDetails().add(detail5);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        lenient().when(paymentRepository.existsSuccessfulByOrderId(1)).thenReturn(false);

        OrderDetailView view = orderService.getOrderDetail(1);

        // Total should exclude CANCELLED line: 110,000 + 35,000 + 50,000 + 10,000 = 205,000
        assertEquals(0, new BigDecimal("205000").compareTo(view.totalAmount()));
        // Subtotal of CANCELLED line should still be shown
        OrderLineView cancelledLine = view.lines().stream()
                .filter(l -> l.cookingStatus() == CookingStatus.CANCELLED)
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("25000").compareTo(cancelledLine.subtotal()));
        assertFalse(cancelledLine.canEdit());
    }

    /**
     * After adding 1 Phở bò (55,000) to the order above, total should be 260,000.
     */
    @Test
    void addItemToOrder_withCancelledLines_totalExcludesCancelled() {
        Order order = createOpenOrder();
        order.setTotalAmount(new BigDecimal("230000"));

        // Phở bò 2 x 55,000 (PENDING)
        OrderDetail detail1 = new OrderDetail();
        detail1.setId(1);
        detail1.setOrder(order);
        detail1.setQuantity(2);
        detail1.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail1.setItemNameAtOrder("Phở bò");
        detail1.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail1);

        // Gỏi cuốn 1 x 35,000 (PREPARING)
        OrderDetail detail2 = new OrderDetail();
        detail2.setId(2);
        detail2.setOrder(order);
        detail2.setQuantity(1);
        detail2.setUnitPriceAtOrder(new BigDecimal("35000"));
        detail2.setItemNameAtOrder("Gỏi cuốn");
        detail2.setCookingStatus(CookingStatus.PREPARING);
        order.getDetails().add(detail2);

        // Cơm gà 1 x 50,000 (READY)
        OrderDetail detail3 = new OrderDetail();
        detail3.setId(3);
        detail3.setOrder(order);
        detail3.setQuantity(1);
        detail3.setUnitPriceAtOrder(new BigDecimal("50000"));
        detail3.setItemNameAtOrder("Cơm gà");
        detail3.setCookingStatus(CookingStatus.READY);
        order.getDetails().add(detail3);

        // Trà đá 2 x 5,000 (SERVED)
        OrderDetail detail4 = new OrderDetail();
        detail4.setId(4);
        detail4.setOrder(order);
        detail4.setQuantity(2);
        detail4.setUnitPriceAtOrder(new BigDecimal("5000"));
        detail4.setItemNameAtOrder("Trà đá");
        detail4.setCookingStatus(CookingStatus.SERVED);
        order.getDetails().add(detail4);

        // Cà phê sữa 1 x 25,000 (CANCELLED)
        OrderDetail detail5 = new OrderDetail();
        detail5.setId(5);
        detail5.setOrder(order);
        detail5.setQuantity(1);
        detail5.setUnitPriceAtOrder(new BigDecimal("25000"));
        detail5.setItemNameAtOrder("Cà phê sữa");
        detail5.setCookingStatus(CookingStatus.CANCELLED);
        order.getDetails().add(detail5);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));

        MenuItem newItem = new MenuItem();
        newItem.setId(1);
        newItem.setItemName("Phở bò");
        newItem.setPrice(new BigDecimal("55000"));
        newItem.setIsAvailable(true);
        when(menuItemRepository.findById(1)).thenReturn(Optional.of(newItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderLine line = new OrderLine(1, 1);
        Order result = orderService.addItemToOrder(1, List.of(line));

        // 205,000 + 55,000 = 260,000 (CANCELLED still excluded)
        assertEquals(0, new BigDecimal("260000").compareTo(result.getTotalAmount()));
    }

    /**
     * Reduce quantity of a PENDING line: total should recalculate correctly, excluding CANCELLED.
     */
    @Test
    void reduceItemQuantity_withCancelledLines_totalRecalculated() {
        Order order = createOpenOrder();
        order.setTotalAmount(new BigDecimal("230000"));

        // Phở bò 2 x 55,000 (PENDING)
        OrderDetail detail1 = new OrderDetail();
        detail1.setId(1);
        detail1.setOrder(order);
        detail1.setQuantity(2);
        detail1.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail1.setItemNameAtOrder("Phở bò");
        detail1.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail1);

        // Cà phê sữa 1 x 25,000 (CANCELLED)
        OrderDetail detail5 = new OrderDetail();
        detail5.setId(5);
        detail5.setOrder(order);
        detail5.setQuantity(1);
        detail5.setUnitPriceAtOrder(new BigDecimal("25000"));
        detail5.setItemNameAtOrder("Cà phê sữa");
        detail5.setCookingStatus(CookingStatus.CANCELLED);
        order.getDetails().add(detail5);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDetail result = orderService.reduceItemQuantity(1, 1, 1);

        assertEquals(1, result.getQuantity());
        // 55,000 (1 Phở bò) — CANCELLED excluded
        assertEquals(0, new BigDecimal("55000").compareTo(order.getTotalAmount()));
    }

    /**
     * Cancel the last detail line of an order: total should become 0.
     */
    @Test
    void cancelOrderDetail_lastLine_totalBecomesZero() {
        Order order = createOpenOrder();
        order.setTotalAmount(new BigDecimal("110000"));

        OrderDetail detail = new OrderDetail();
        detail.setId(1);
        detail.setOrder(order);
        detail.setQuantity(2);
        detail.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail.setItemNameAtOrder("Phở bò");
        detail.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDetail result = orderService.cancelOrderDetail(1, 1);

        assertEquals(CookingStatus.CANCELLED, result.getCookingStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getTotalAmount()));
    }

    /**
     * Cancel the last non-cancelled line when there are already CANCELLED lines:
     * total should be 0 (all lines are CANCELLED).
     */
    @Test
    void cancelOrderDetail_lastNonCancelledLine_totalBecomesZero() {
        Order order = createOpenOrder();
        order.setTotalAmount(new BigDecimal("135000"));

        // Phở bò 2 x 55,000 (PENDING)
        OrderDetail detail1 = new OrderDetail();
        detail1.setId(1);
        detail1.setOrder(order);
        detail1.setQuantity(2);
        detail1.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail1.setItemNameAtOrder("Phở bò");
        detail1.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail1);

        // Gỏi cuốn 1 x 35,000 (CANCELLED)
        OrderDetail detail2 = new OrderDetail();
        detail2.setId(2);
        detail2.setOrder(order);
        detail2.setQuantity(1);
        detail2.setUnitPriceAtOrder(new BigDecimal("35000"));
        detail2.setItemNameAtOrder("Gỏi cuốn");
        detail2.setCookingStatus(CookingStatus.CANCELLED);
        order.getDetails().add(detail2);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDetail result = orderService.cancelOrderDetail(1, 1);

        assertEquals(CookingStatus.CANCELLED, result.getCookingStatus());
        // All lines are now CANCELLED: total = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getTotalAmount()));
    }

    /**
     * Create order: total should be calculated correctly from scratch.
     */
    @Test
    void createOrder_totalAmount_calculatedCorrectly() {
        OrderLine line1 = new OrderLine(1, 2); // Phở bò 2 x 55,000
        OrderLine line2 = new OrderLine(2, 1); // Cà phê 1 x 25,000
        List<OrderLine> lines = List.of(line1, line2);

        MenuItem item2 = new MenuItem();
        item2.setId(2);
        item2.setItemName("Cà phê");
        item2.setPrice(new BigDecimal("25000"));
        item2.setIsAvailable(true);

        when(tableRepository.findByIdWithPessimisticLock(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
        when(menuItemRepository.findById(1)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.findById(2)).thenReturn(Optional.of(item2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(1, null, lines);

        // 2 x 55,000 + 1 x 25,000 = 135,000
        assertEquals(0, new BigDecimal("135000").compareTo(order.getTotalAmount()));
        assertEquals(2, order.getDetails().size());
    }

    /**
     * getOrderDetail view DTO: verify total recalculated even when DB total is wrong.
     */
    @Test
    void getOrderDetail_viewTotal_recalculatedRegardlessOfDbValue() {
        Order order = createOpenOrder();
        // DB total is intentionally wrong (includes CANCELLED)
        order.setTotalAmount(new BigDecimal("160000"));

        // Phở bò 2 x 55,000 (PENDING) = 110,000
        OrderDetail detail1 = new OrderDetail();
        detail1.setId(1);
        detail1.setOrder(order);
        detail1.setQuantity(2);
        detail1.setUnitPriceAtOrder(new BigDecimal("55000"));
        detail1.setItemNameAtOrder("Phở bò");
        detail1.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail1);

        // Cà phê 1 x 25,000 (CANCELLED) = 25,000 (excluded)
        OrderDetail detail2 = new OrderDetail();
        detail2.setId(2);
        detail2.setOrder(order);
        detail2.setQuantity(1);
        detail2.setUnitPriceAtOrder(new BigDecimal("25000"));
        detail2.setItemNameAtOrder("Cà phê");
        detail2.setCookingStatus(CookingStatus.CANCELLED);
        order.getDetails().add(detail2);

        // Trà đá 1 x 5,000 (PENDING) = 5,000
        OrderDetail detail3 = new OrderDetail();
        detail3.setId(3);
        detail3.setOrder(order);
        detail3.setQuantity(1);
        detail3.setUnitPriceAtOrder(new BigDecimal("5000"));
        detail3.setItemNameAtOrder("Trà đá");
        detail3.setCookingStatus(CookingStatus.PENDING);
        order.getDetails().add(detail3);

        when(orderRepository.findByIdWithDetails(1)).thenReturn(Optional.of(order));
        when(paymentRepository.existsSuccessfulByOrderId(1)).thenReturn(false);

        OrderDetailView view = orderService.getOrderDetail(1);

        // Recalculated: 110,000 + 5,000 = 115,000 (not 160,000 from DB)
        assertEquals(0, new BigDecimal("115000").compareTo(view.totalAmount()));
    }
}
