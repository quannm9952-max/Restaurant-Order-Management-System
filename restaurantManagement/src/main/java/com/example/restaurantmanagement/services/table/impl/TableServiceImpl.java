package com.example.restaurantmanagement.services.table.impl;

import com.example.restaurantmanagement.dto.table.CreateTableForm;
import com.example.restaurantmanagement.dto.table.EditTableForm;
import com.example.restaurantmanagement.dto.table.TableView;
import com.example.restaurantmanagement.entities.Employee;
import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.Role;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.reponsitories.OrderRepository;
import com.example.restaurantmanagement.reponsitories.TableRepository;
import com.example.restaurantmanagement.services.employee.ICurrentEmployeeService;
import com.example.restaurantmanagement.services.table.ITableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing restaurant tables (UC04).
 * // TODO(auth): WAITER, MANAGER - chỉ WAITER, MANAGER được cập nhật trạng thái bàn và xem đơn/bàn.
 */
@Service
public class TableServiceImpl implements ITableService {

    private final TableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final ICurrentEmployeeService currentEmployeeService;

    public TableServiceImpl(TableRepository tableRepository, OrderRepository orderRepository,
                            ICurrentEmployeeService currentEmployeeService) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.currentEmployeeService = currentEmployeeService;
    }

    /**
     * Get all tables as view DTOs.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TableView> listTables() {
        return tableRepository.findAll().stream()
                .map(this::toTableView)
                .toList();
    }

    /**
     * Get tables filtered by status as view DTOs.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TableView> listTablesByStatus(String status) {
        TableStatus tableStatus = TableStatus.valueOf(status.toUpperCase());
        return tableRepository.findByStatus(tableStatus).stream()
                .map(this::toTableView)
                .toList();
    }

    /**
     * Get a table by ID as a view DTO.
     */
    @Override
    @Transactional(readOnly = true)
    public TableView getTable(Integer id) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn với mã: " + id));
        return toTableView(table);
    }

    /**
     * Get a table by ID (internal use for write operations).
     */
    private RestaurantTable getTableById(Integer id) {
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

    /**
     * Count tables by status for filter tabs display.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus() {
        List<RestaurantTable> all = tableRepository.findAll();
        return all.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus().name(),
                        Collectors.counting()
                ));
    }

    // ==================== Entity to View DTO mapping ====================

    private TableView toTableView(RestaurantTable table) {
        Integer currentOrderId = orderRepository.findOpenOrderIdByTableId(table.getId()).orElse(null);
        boolean hasOpen = currentOrderId != null;
        boolean canCreateOrder = table.getStatus() == TableStatus.AVAILABLE && !hasOpen;
        boolean canChangeStatus = !hasOpen && table.getStatus() != TableStatus.OCCUPIED;
        String orderCode = (currentOrderId != null)
                ? String.format("ORD-%04d", currentOrderId)
                : null;

        // Determine current user role for permission flags
        boolean isManager = false;
        try {
            Employee emp = currentEmployeeService.getCurrentEmployee();
            isManager = emp.getAccount().getRole() == Role.MANAGER;
        } catch (Exception ignored) {
            // If we can't determine the role, default to false
        }

        boolean canAddTable = isManager;
        boolean canEditTable = isManager;

        return new TableView(
                table.getId(),
                table.getTableName(),
                table.getCapacity(),
                table.getStatus(),
                currentOrderId,
                orderCode,
                canCreateOrder,
                canChangeStatus,
                canAddTable,
                canEditTable
        );
    }

    // ==================== Add / Edit Table (MANAGER only) ====================

    /**
     * Check if the current employee is a MANAGER.
     * // TODO(auth): Replace with real auth-based role check when TV1 is merged.
     */
    private void checkManagerRole() {
        Employee emp = currentEmployeeService.getCurrentEmployee();
        if (emp.getAccount().getRole() != Role.MANAGER) {
            throw new BusinessException("Bạn không có quyền thực hiện thao tác này.");
        }
    }

    @Override
    @Transactional
    public RestaurantTable addTable(CreateTableForm form) {
        checkManagerRole();

        String name = form.tableName().trim();
        if (name.isBlank()) {
            throw new BusinessException("Tên bàn không được để trống.");
        }
        if (name.length() > 50) {
            throw new BusinessException("Tên bàn tối đa 50 ký tự.");
        }
        if (form.capacity() == null || form.capacity() < 1) {
            throw new BusinessException("Sức chứa phải lớn hơn 0.");
        }
        if (form.capacity() > 200) {
            throw new BusinessException("Sức chứa tối đa 200 chỗ.");
        }

        // Check duplicate name (case-insensitive)
        Optional<RestaurantTable> existing = tableRepository.findByTableNameIgnoreCase(name);
        if (existing.isPresent()) {
            throw new BusinessException("Tên bàn '" + name + "' đã tồn tại.");
        }

        RestaurantTable table = new RestaurantTable();
        table.setTableName(name);
        table.setCapacity(form.capacity());
        table.setStatus(TableStatus.AVAILABLE);
        return tableRepository.save(table);
    }

    @Override
    @Transactional
    public RestaurantTable editTable(Integer id, EditTableForm form) {
        checkManagerRole();

        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn với mã: " + id));

        String name = form.tableName().trim();
        if (name.isBlank()) {
            throw new BusinessException("Tên bàn không được để trống.");
        }
        if (name.length() > 50) {
            throw new BusinessException("Tên bàn tối đa 50 ký tự.");
        }
        if (form.capacity() == null || form.capacity() < 1) {
            throw new BusinessException("Sức chứa phải lớn hơn 0.");
        }
        if (form.capacity() > 200) {
            throw new BusinessException("Sức chứa tối đa 200 chỗ.");
        }

        // Check duplicate name excluding self (case-insensitive)
        Optional<RestaurantTable> existing = tableRepository.findByTableNameIgnoreCaseAndIdNot(name, id);
        if (existing.isPresent()) {
            throw new BusinessException("Tên bàn '" + name + "' đã tồn tại.");
        }

        table.setTableName(name);
        table.setCapacity(form.capacity());
        return tableRepository.save(table);
    }
}
