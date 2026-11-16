package com.example.restaurantmanagement.services.table.impl;

import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.reponsitories.OrderRepository;
import com.example.restaurantmanagement.reponsitories.TableRepository;
import com.example.restaurantmanagement.services.table.ITableService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing restaurant tables (UC04).
 * // TODO(auth): WAITER, MANAGER - chỉ WAITER, MANAGER được cập nhật trạng thái bàn và xem đơn/bàn.
 */
@Service
public class TableServiceImpl implements ITableService {

    private final TableRepository tableRepository;
    private final OrderRepository orderRepository;

    public TableServiceImpl(TableRepository tableRepository, OrderRepository orderRepository) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Get all tables.
     */
    @Override
    public List<RestaurantTable> getAllTables() {
        return tableRepository.findAll();
    }

    /**
     * Get tables filtered by status.
     */
    @Override
    public List<RestaurantTable> getTablesByStatus(String status) {
        return tableRepository.findByStatus(status);
    }

    /**
     * Get a table by ID.
     */
    @Override
    public RestaurantTable getTableById(Integer id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn với mã: " + id));
    }

    /**
     * Update table status manually. Only allows AVAILABLE <-> OUT_OF_SERVICE transitions.
     *
     * Rules:
     * - Cannot manually set OCCUPIED (only system sets this when creating an order).
     * - Table with OPEN order cannot be changed to AVAILABLE or OUT_OF_SERVICE.
     * - Cannot change to the same status it already has.
     */
    @Override
    public RestaurantTable updateTableStatus(Integer tableId, TableStatus newStatus) {
        // TODO(auth): WAITER, MANAGER
        RestaurantTable table = getTableById(tableId);

        // Cannot manually set OCCUPIED
        if (newStatus == TableStatus.OCCUPIED) {
            throw new BusinessException(
                    "Không thể đặt thủ công trạng thái OCCUPIED. Trạng thái này chỉ được đặt tự động khi tạo đơn.");
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

    /**
     * Check if a table has an open order.
     */
    @Override
    public boolean hasOpenOrder(Integer tableId) {
        return orderRepository.existsOpenByTableId(tableId);
    }
}
