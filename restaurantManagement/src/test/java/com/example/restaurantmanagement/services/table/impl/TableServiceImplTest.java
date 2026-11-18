package com.example.restaurantmanagement.services.table.impl;

import com.example.restaurantmanagement.dto.table.CreateTableForm;
import com.example.restaurantmanagement.dto.table.EditTableForm;
import com.example.restaurantmanagement.dto.table.TableView;
import com.example.restaurantmanagement.entities.Account;
import com.example.restaurantmanagement.entities.Employee;
import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.Role;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.reponsitories.OrderRepository;
import com.example.restaurantmanagement.reponsitories.TableRepository;
import com.example.restaurantmanagement.services.employee.ICurrentEmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableServiceImplTest {

    @Mock
    private TableRepository tableRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ICurrentEmployeeService currentEmployeeService;

    @InjectMocks
    private TableServiceImpl tableService;

    private RestaurantTable table;
    private Employee managerEmployee;
    private Employee waiterEmployee;

    @BeforeEach
    void setUp() {
        table = new RestaurantTable();
        table.setId(1);
        table.setTableName("Bàn 1");
        table.setCapacity(4);
        table.setStatus(TableStatus.AVAILABLE);

        // Manager employee
        Account managerAccount = new Account();
        managerAccount.setId(1);
        managerAccount.setRole(Role.MANAGER);
        managerEmployee = new Employee();
        managerEmployee.setId(1);
        managerEmployee.setAccount(managerAccount);
        managerEmployee.setFullName("Manager Demo");

        // Waiter employee
        Account waiterAccount = new Account();
        waiterAccount.setId(2);
        waiterAccount.setRole(Role.WAITER);
        waiterEmployee = new Employee();
        waiterEmployee.setId(2);
        waiterEmployee.setAccount(waiterAccount);
        waiterEmployee.setFullName("Waiter Demo");
    }

    // ==================== GET TABLES TESTS ====================

    @Test
    void listTables_shouldReturnTableViewList() {
        when(tableRepository.findAll()).thenReturn(List.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.empty());
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        List<TableView> result = tableService.listTables();

        assertEquals(1, result.size());
        TableView view = result.get(0);
        assertEquals("Bàn 1", view.tableName());
        assertEquals(4, view.capacity());
        assertEquals(TableStatus.AVAILABLE, view.status());
        assertNull(view.currentOrderId());
        assertFalse(view.canAddTable());
        assertFalse(view.canEditTable());
    }

    @Test
    void listTablesByStatus_shouldReturnFilteredTableViewList() {
        when(tableRepository.findByStatus(TableStatus.AVAILABLE)).thenReturn(List.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.empty());
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

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

    // ==================== UPDATE TABLE STATUS TESTS ====================

    @Test
    void updateTableStatus_availableToOutOfService_shouldSucceed() {
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        RestaurantTable result = tableService.updateTableStatus(1, TableStatus.OUT_OF_SERVICE);

        assertEquals(TableStatus.OUT_OF_SERVICE, result.getStatus());
        verify(tableRepository).save(table);
    }

    @Test
    void updateTableStatus_outOfServiceToAvailable_shouldSucceed() {
        table.setStatus(TableStatus.OUT_OF_SERVICE);
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        RestaurantTable result = tableService.updateTableStatus(1, TableStatus.AVAILABLE);

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

    // ==================== HAS OPEN ORDER TESTS ====================

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

    // ==================== VIEW DTO FLAGS TESTS ====================

    @Test
    void getTable_canCreateOrder_true_whenAvailableAndNoOpenOrder() {
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.empty());
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        TableView view = tableService.getTable(1);

        assertTrue(view.canCreateOrder());
    }

    @Test
    void getTable_canCreateOrder_false_whenOccupied() {
        table.setStatus(TableStatus.OCCUPIED);
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.of(10));
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        TableView view = tableService.getTable(1);

        assertFalse(view.canCreateOrder());
        assertEquals(10, view.currentOrderId());
    }

    @Test
    void getTable_canCreateOrder_false_whenOutOfService() {
        table.setStatus(TableStatus.OUT_OF_SERVICE);
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.empty());
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        TableView view = tableService.getTable(1);

        assertFalse(view.canCreateOrder());
    }

    @Test
    void getTable_canCreateOrder_false_whenHasOpenOrder() {
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.of(10));
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        TableView view = tableService.getTable(1);

        assertFalse(view.canCreateOrder());
        assertEquals(10, view.currentOrderId());
    }

    @Test
    void getTable_canChangeStatus_true_whenAvailableAndNoOpenOrder() {
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.empty());
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        TableView view = tableService.getTable(1);

        assertTrue(view.canChangeStatus());
    }

    @Test
    void getTable_canChangeStatus_true_whenOutOfServiceAndNoOpenOrder() {
        table.setStatus(TableStatus.OUT_OF_SERVICE);
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.empty());
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        TableView view = tableService.getTable(1);

        assertTrue(view.canChangeStatus());
    }

    @Test
    void getTable_canChangeStatus_false_whenOccupied() {
        table.setStatus(TableStatus.OCCUPIED);
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.of(10));
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        TableView view = tableService.getTable(1);

        assertFalse(view.canChangeStatus());
    }

    @Test
    void getTable_canChangeStatus_false_whenHasOpenOrder() {
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.of(10));
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        TableView view = tableService.getTable(1);

        assertFalse(view.canChangeStatus());
    }

    // ==================== ADD TABLE TESTS ====================

    @Test
    void addTable_valid_shouldSucceed() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);
        when(tableRepository.findByTableNameIgnoreCase("Bàn mới")).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> {
            RestaurantTable t = inv.getArgument(0);
            t.setId(99);
            return t;
        });

        CreateTableForm form = new CreateTableForm("Bàn mới", 6);
        RestaurantTable result = tableService.addTable(form);

        assertEquals(99, result.getId());
        assertEquals("Bàn mới", result.getTableName());
        assertEquals(6, result.getCapacity());
        assertEquals(TableStatus.AVAILABLE, result.getStatus());
        verify(tableRepository).save(any(RestaurantTable.class));
    }

    @Test
    void addTable_blankName_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);

        CreateTableForm form = new CreateTableForm("   ", 4);
        BusinessException ex = assertThrows(BusinessException.class, () -> tableService.addTable(form));
        assertTrue(ex.getMessage().contains("để trống"));
    }

    @Test
    void addTable_nameTooLong_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);

        String longName = "A".repeat(51);
        CreateTableForm form = new CreateTableForm(longName, 4);
        BusinessException ex = assertThrows(BusinessException.class, () -> tableService.addTable(form));
        assertTrue(ex.getMessage().contains("50"));
    }

    @Test
    void addTable_capacityZero_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);

        CreateTableForm form = new CreateTableForm("Bàn mới", 0);
        BusinessException ex = assertThrows(BusinessException.class, () -> tableService.addTable(form));
        assertTrue(ex.getMessage().contains("lớn hơn 0"));
    }

    @Test
    void addTable_capacityNegative_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);

        CreateTableForm form = new CreateTableForm("Bàn mới", -1);
        assertThrows(BusinessException.class, () -> tableService.addTable(form));
    }

    @Test
    void addTable_duplicateName_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);
        RestaurantTable existing = new RestaurantTable();
        existing.setId(1);
        existing.setTableName("Bàn 1");
        when(tableRepository.findByTableNameIgnoreCase("Bàn 1")).thenReturn(Optional.of(existing));

        CreateTableForm form = new CreateTableForm("Bàn 1", 4);
        BusinessException ex = assertThrows(BusinessException.class, () -> tableService.addTable(form));
        assertTrue(ex.getMessage().contains("đã tồn tại"));
    }

    @Test
    void addTable_duplicateNameDifferentCase_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);
        RestaurantTable existing = new RestaurantTable();
        existing.setId(1);
        existing.setTableName("Bàn 1");
        when(tableRepository.findByTableNameIgnoreCase("bàn 1")).thenReturn(Optional.of(existing));

        CreateTableForm form = new CreateTableForm("bàn 1", 4);
        assertThrows(BusinessException.class, () -> tableService.addTable(form));
    }

    @Test
    void addTable_duplicateNameWithExtraSpaces_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);
        RestaurantTable existing = new RestaurantTable();
        existing.setId(1);
        existing.setTableName("Bàn 1");
        when(tableRepository.findByTableNameIgnoreCase("Bàn 1")).thenReturn(Optional.of(existing));

        CreateTableForm form = new CreateTableForm("  Bàn 1  ", 4);
        assertThrows(BusinessException.class, () -> tableService.addTable(form));
    }

    @Test
    void addTable_waiter_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        CreateTableForm form = new CreateTableForm("Bàn mới", 4);
        BusinessException ex = assertThrows(BusinessException.class, () -> tableService.addTable(form));
        assertTrue(ex.getMessage().contains("không có quyền"));
    }

    // ==================== EDIT TABLE TESTS ====================

    @Test
    void editTable_valid_shouldSucceed() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(tableRepository.findByTableNameIgnoreCaseAndIdNot("Bàn mới", 1)).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        EditTableForm form = new EditTableForm("Bàn mới", 8);
        RestaurantTable result = tableService.editTable(1, form);

        assertEquals("Bàn mới", result.getTableName());
        assertEquals(8, result.getCapacity());
        verify(tableRepository).save(table);
    }

    @Test
    void editTable_keepSameName_shouldSucceed() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(tableRepository.findByTableNameIgnoreCaseAndIdNot("Bàn 1", 1)).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        EditTableForm form = new EditTableForm("Bàn 1", 6);
        RestaurantTable result = tableService.editTable(1, form);

        assertEquals("Bàn 1", result.getTableName());
        assertEquals(6, result.getCapacity());
    }

    @Test
    void editTable_duplicateName_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        RestaurantTable other = new RestaurantTable();
        other.setId(2);
        other.setTableName("Bàn 2");
        when(tableRepository.findByTableNameIgnoreCaseAndIdNot("Bàn 2", 1)).thenReturn(Optional.of(other));

        EditTableForm form = new EditTableForm("Bàn 2", 4);
        BusinessException ex = assertThrows(BusinessException.class, () -> tableService.editTable(1, form));
        assertTrue(ex.getMessage().contains("đã tồn tại"));
    }

    @Test
    void editTable_notFound_shouldThrowNotFoundException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);
        when(tableRepository.findById(999)).thenReturn(Optional.empty());

        EditTableForm form = new EditTableForm("Bàn mới", 4);
        NotFoundException ex = assertThrows(NotFoundException.class, () -> tableService.editTable(999, form));
        assertTrue(ex.getMessage().contains("Không tìm thấy bàn"));
    }

    @Test
    void editTable_waiter_shouldThrowBusinessException() {
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        EditTableForm form = new EditTableForm("Bàn mới", 4);
        BusinessException ex = assertThrows(BusinessException.class, () -> tableService.editTable(1, form));
        assertTrue(ex.getMessage().contains("không có quyền"));
    }

    // ==================== ROLE-BASED FLAGS TESTS ====================

    @Test
    void getTable_manager_canAddTable_true() {
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.empty());
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(managerEmployee);

        TableView view = tableService.getTable(1);

        assertTrue(view.canAddTable());
        assertTrue(view.canEditTable());
    }

    @Test
    void getTable_waiter_canAddTable_false() {
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenOrderIdByTableId(1)).thenReturn(Optional.empty());
        when(currentEmployeeService.getCurrentEmployee()).thenReturn(waiterEmployee);

        TableView view = tableService.getTable(1);

        assertFalse(view.canAddTable());
        assertFalse(view.canEditTable());
    }

    @Test
    void updateTableStatus_waiter_allowed() {
        when(tableRepository.findById(1)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenByTableId(1)).thenReturn(false);
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));

        RestaurantTable result = tableService.updateTableStatus(1, TableStatus.OUT_OF_SERVICE);

        assertEquals(TableStatus.OUT_OF_SERVICE, result.getStatus());
    }
}
