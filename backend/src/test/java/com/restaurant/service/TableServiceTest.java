package com.restaurant.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.restaurant.dto.table.TableView;
import com.restaurant.entity.RestaurantTable;
import com.restaurant.entity.enums.TableStatus;
import com.restaurant.exception.BusinessException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.repository.OrderRepository;
import com.restaurant.repository.TableRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableServiceTest {

  @Mock
  private TableRepository tableRepository;

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private TableService tableService;

  private RestaurantTable table;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.lenient()
        .when(tableRepository
            .findByIdWithPessimisticLock(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(inv -> tableRepository.findById(inv.getArgument(0)));
    table = new RestaurantTable();
    table.setId(1);
    table.setTableName("Bàn 1");
    table.setCapacity(4);
    table.setStatus(TableStatus.AVAILABLE);
  }

  @Test
  void listTables_shouldReturnTableViewList() {
    when(tableRepository.findAll()).thenReturn(List.of(table));
    when(orderRepository.findOpenTableOrders()).thenReturn(List.of());

    List<TableView> result = tableService.listTables();

    assertEquals(1, result.size());
    TableView view = result.get(0);
    assertEquals("Bàn 1", view.tableName());
    assertEquals(4, view.capacity());
    assertEquals(TableStatus.AVAILABLE, view.status());
    assertNull(view.currentOrderId());
  }

  @Test
  void listTablesByStatus_shouldReturnFilteredTableViewList() {
    when(tableRepository.findByStatus(TableStatus.AVAILABLE))
        .thenReturn(List.of(table));
    when(orderRepository.findOpenTableOrders()).thenReturn(List.of());

    List<TableView> result = tableService.listTablesByStatus("AVAILABLE");

    assertEquals(1, result.size());
    assertEquals(TableStatus.AVAILABLE, result.get(0).status());
  }

  @Test
  void getTable_notFound_shouldThrowNotFoundException() {
    when(tableRepository.findById(999)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class,
        () -> tableService.getTable(999));
    assertTrue(ex.getMessage().contains("Không tìm thấy bàn"));
  }

  @Test
  void updateTableStatus_availableToOutOfService_shouldSucceed() {
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
    when(tableRepository.save(any(RestaurantTable.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    RestaurantTable result = tableService.updateTableStatus(1,
        TableStatus.OUT_OF_SERVICE);

    assertEquals(TableStatus.OUT_OF_SERVICE, result.getStatus());
    verify(tableRepository).save(table);
  }

  @Test
  void updateTableStatus_outOfServiceToAvailable_shouldSucceed() {
    table.setStatus(TableStatus.OUT_OF_SERVICE);
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
    when(tableRepository.save(any(RestaurantTable.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    RestaurantTable result = tableService.updateTableStatus(1,
        TableStatus.AVAILABLE);

    assertEquals(TableStatus.AVAILABLE, result.getStatus());
    verify(tableRepository).save(table);
  }

  @Test
  void updateTableStatus_manualOccupied_shouldThrowBusinessException() {
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> tableService.updateTableStatus(1, TableStatus.OCCUPIED));
    assertTrue(ex.getMessage().contains("thủ công"));
    assertTrue(ex.getMessage().contains("OCCUPIED"));
  }

  @Test
  void updateTableStatus_sameStatus_shouldThrowBusinessException() {
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> tableService.updateTableStatus(1, TableStatus.AVAILABLE));
    assertTrue(ex.getMessage().contains("không thay đổi"));
  }

  @Test
  void updateTableStatus_tableHasOpenOrder_shouldThrowBusinessException() {
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.existsOpenByTableId(1)).thenReturn(true);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> tableService.updateTableStatus(1, TableStatus.OUT_OF_SERVICE));
    assertTrue(ex.getMessage().contains("đơn OPEN"));
  }

  @Test
  void updateTableStatus_tableNotFound_shouldThrowNotFoundException() {
    when(tableRepository.findById(999)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class,
        () -> tableService.updateTableStatus(999, TableStatus.OUT_OF_SERVICE));
    assertTrue(ex.getMessage().contains("Không tìm thấy bàn"));
  }

  @Test
  void hasOpenOrder_shouldReturnTrue_whenOpenOrderExists() {
    when(orderRepository.existsOpenByTableId(1)).thenReturn(true);

    assertTrue(tableService.hasOpenOrder(1));
  }

  @Test
  void hasOpenOrder_shouldReturnFalse_whenNoOpenOrder() {
    when(orderRepository.existsOpenByTableId(1)).thenReturn(false);

    assertFalse(tableService.hasOpenOrder(1));
  }

  @Test
  void getTable_canCreateOrder_true_whenAvailableAndNoOpenOrder() {
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.findOpenOrderIdByTableId(1))
        .thenReturn(Optional.empty());

    TableView view = tableService.getTable(1);

    assertTrue(view.canCreateOrder());
  }

  @Test
  void getTable_canCreateOrder_false_whenOccupied() {
    table.setStatus(TableStatus.OCCUPIED);
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.findOpenOrderIdByTableId(1))
        .thenReturn(Optional.of(10));

    TableView view = tableService.getTable(1);

    assertFalse(view.canCreateOrder());
    assertEquals(10, view.currentOrderId());
  }

  @Test
  void getTable_canCreateOrder_false_whenOutOfService() {
    table.setStatus(TableStatus.OUT_OF_SERVICE);
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.findOpenOrderIdByTableId(1))
        .thenReturn(Optional.empty());

    TableView view = tableService.getTable(1);

    assertFalse(view.canCreateOrder());
  }

  @Test
  void getTable_canCreateOrder_false_whenHasOpenOrder() {
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.findOpenOrderIdByTableId(1))
        .thenReturn(Optional.of(10));

    TableView view = tableService.getTable(1);

    assertFalse(view.canCreateOrder());
    assertEquals(10, view.currentOrderId());
  }

  @Test
  void getTable_canChangeStatus_true_whenAvailableAndNoOpenOrder() {
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.findOpenOrderIdByTableId(1))
        .thenReturn(Optional.empty());

    TableView view = tableService.getTable(1);

    assertTrue(view.canChangeStatus());
  }

  @Test
  void getTable_canChangeStatus_true_whenOutOfServiceAndNoOpenOrder() {
    table.setStatus(TableStatus.OUT_OF_SERVICE);
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.findOpenOrderIdByTableId(1))
        .thenReturn(Optional.empty());

    TableView view = tableService.getTable(1);

    assertTrue(view.canChangeStatus());
  }

  @Test
  void getTable_canChangeStatus_false_whenOccupied() {
    table.setStatus(TableStatus.OCCUPIED);
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.findOpenOrderIdByTableId(1))
        .thenReturn(Optional.of(10));

    TableView view = tableService.getTable(1);

    assertFalse(view.canChangeStatus());
  }

  @Test
  void getTable_canChangeStatus_false_whenHasOpenOrder() {
    when(tableRepository.findById(1)).thenReturn(Optional.of(table));
    when(orderRepository.findOpenOrderIdByTableId(1))
        .thenReturn(Optional.of(10));

    TableView view = tableService.getTable(1);

    assertFalse(view.canChangeStatus());
  }

  @Test
  void tableListReadsOpenOrdersOnceAndPreservesOccupiedOrderLink() {
    var second = new RestaurantTable();
    second.setId(2);
    second.setTableName("Table 2");
    second.setCapacity(4);
    second.setStatus(TableStatus.OCCUPIED);
    var open = new OrderRepository.OpenTableOrder() {
      public Integer getTableId() {
        return 2;
      }
      public Integer getOrderId() {
        return 99;
      }
    };
    when(tableRepository.findAll()).thenReturn(List.of(table, second));
    when(orderRepository.findOpenTableOrders()).thenReturn(List.of(open));
    var views = tableService.listTables();
    assertNull(views.getFirst().currentOrderId());
    assertEquals(99, views.get(1).currentOrderId());
    assertFalse(views.get(1).canCreateOrder());
    verify(orderRepository, times(1)).findOpenTableOrders();
    verify(orderRepository, never()).findOpenOrderIdByTableId(any());
  }
}
