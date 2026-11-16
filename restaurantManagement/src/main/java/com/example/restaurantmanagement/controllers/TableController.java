package com.example.restaurantmanagement.controllers;

import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.services.order.IOrderService;
import com.example.restaurantmanagement.services.table.ITableService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for managing restaurant tables (UC04).
 * // TODO(auth): WAITER, MANAGER - chỉ WAITER, MANAGER được cập nhật trạng thái bàn và xem đơn/bàn.
 */
@Controller
@RequestMapping("/tables")
public class TableController {

    private final ITableService tableService;
    private final IOrderService orderService;

    public TableController(ITableService tableService, IOrderService orderService) {
        this.tableService = tableService;
        this.orderService = orderService;
    }

    /**
     * Redirect root URL to tables list.
     */
    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/tables";
    }

    /**
     * List all tables, optionally filtered by status.
     */
    @GetMapping
    public String listTables(@RequestParam(required = false) String status, Model model) {
        List<RestaurantTable> tables;
        if (status != null && !status.isBlank()) {
            tables = tableService.getTablesByStatus(status);
            model.addAttribute("filterStatus", status);
        } else {
            tables = tableService.getAllTables();
        }
        model.addAttribute("tables", tables);
        model.addAttribute("statuses", TableStatus.values());
        return "tables/list";
    }

    /**
     * Show table detail with open order info.
     */
    @GetMapping("/{id}")
    public String tableDetail(@PathVariable("id") Integer id, Model model) {
        try {
            RestaurantTable table = tableService.getTableById(id);
            model.addAttribute("table", table);

            // Check if table has an open order
            if (tableService.hasOpenOrder(id)) {
                // Find the open order for this table
                List<com.example.restaurantmanagement.entities.Order> openOrders = orderService.getOpenOrders();
                com.example.restaurantmanagement.entities.Order openOrder = null;
                for (com.example.restaurantmanagement.entities.Order order : openOrders) {
                    if (order.getTable().getId().equals(id)) {
                        openOrder = order;
                        break;
                    }
                }
                if (openOrder != null) {
                    model.addAttribute("openOrder", openOrder);
                }
            }

            return "tables/detail";
        } catch (NotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    /**
     * Update table status (POST only).
     */
    @PostMapping("/{id}/status")
    public String updateTableStatus(
            @PathVariable("id") Integer id,
            @RequestParam("newStatus") String newStatusStr,
            RedirectAttributes redirectAttributes) {
        try {
            TableStatus newStatus = TableStatus.valueOf(newStatusStr);
            tableService.updateTableStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật trạng thái bàn thành công.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Trạng thái không hợp lệ: " + newStatusStr);
        }
        return "redirect:/tables/" + id;
    }
}
