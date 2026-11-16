package com.example.restaurantmanagement.services.order.impl;

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
    void getOrderWithDetails_notFound_shouldThrowNotFoundException() {
        when(orderRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> orderService.getOrderWithDetails(999));
        assertTrue(ex.getMessage().contains("Không tìm thấy đơn"));
    }
}
