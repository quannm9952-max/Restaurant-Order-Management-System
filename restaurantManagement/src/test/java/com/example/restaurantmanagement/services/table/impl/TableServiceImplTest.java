package com.example.restaurantmanagement.services.table.impl;

import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.reponsitories.OrderRepository;
import com.example.restaurantmanagement.reponsitories.TableRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableServiceImplTest {

    @Mock
    private TableRepository tableRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private TableServiceImpl tableService;

    private RestaurantTable table;

    @BeforeEach
    void setUp() {
        table = new RestaurantTable();
        table.setId(1);
        table.setTableName("Bàn 1");
        table.setCapacity(4);
        table.setStatus(TableStatus.AVAILABLE);
    }

    // ==================== GET TABLES TESTS ====================

    @Test
    void getAllTables_shouldReturnAllTables() {
        when(tableRepository.findAll()).thenReturn(List.of(table));

        List<RestaurantTable> result = tableService.getAllTables();

        assertEquals(1, result.size());
        assertEquals("Bàn 1", result.get(0).getTableName());
    }

    @Test
    void getTablesByStatus_shouldReturnFilteredTables() {
        when(tableRepository.findByStatus("AVAILABLE")).thenReturn(List.of(table));

        List<RestaurantTable> result = tableService.getTablesByStatus("AVAILABLE");

        assertEquals(1, result.size());
        assertEquals(TableStatus.AVAILABLE, result.get(0).getStatus());
    }

    @Test
    void getTableById_notFound_shouldThrowNotFoundException() {
        when(tableRepository.findById(999)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> tableService.getTableById(999));
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
}
