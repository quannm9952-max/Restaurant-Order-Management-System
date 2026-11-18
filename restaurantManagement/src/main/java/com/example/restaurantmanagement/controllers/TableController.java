package com.example.restaurantmanagement.controllers;

import com.example.restaurantmanagement.dto.order.OrderSummaryView;
import com.example.restaurantmanagement.dto.table.CreateTableForm;
import com.example.restaurantmanagement.dto.table.EditTableForm;
import com.example.restaurantmanagement.dto.table.TableView;
import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.Role;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.services.order.IOrderService;
import com.example.restaurantmanagement.services.table.ITableService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

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
     * Supports "selected" parameter to highlight a table and show its detail panel.
     */
    @GetMapping
    public String listTables(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer selected,
            @ModelAttribute("currentUser") ModelAttributeAdvice.UserContext currentUser,
            Model model) {
        try {
            List<TableView> tables;
            if (status != null && !status.isBlank()) {
                tables = tableService.listTablesByStatus(status);
                model.addAttribute("filterStatus", status);
            } else {
                tables = tableService.listTables();
            }

            // If no selected table, default to the first one in the list
            Integer selectedId = selected;
            if (selectedId == null && !tables.isEmpty()) {
                selectedId = tables.get(0).id();
            }

            model.addAttribute("tables", tables);
            model.addAttribute("selectedTableId", selectedId);
            model.addAttribute("statuses", TableStatus.values());
            model.addAttribute("tableCounts", tableService.countByStatus());
            model.addAttribute("pageTitle", "Table Management");

            // Page-level canAddTable flag: MANAGER only, independent of selected table
            model.addAttribute("canAddTable", 
                    currentUser != null && currentUser.roleEnum() == Role.MANAGER);

            // Find the selected table for the detail panel
            if (selectedId != null) {
                for (TableView t : tables) {
                    if (t.id().equals(selectedId)) {
                        model.addAttribute("selectedTable", t);
                        break;
                    }
                }
            }
            return "tables/list";
        } catch (Exception e) {
            // Log the exception and show error page with detailed message
            e.printStackTrace();
            model.addAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Show table detail with open order info.
     */
    @GetMapping("/{id}")
    public String tableDetail(@PathVariable("id") Integer id, Model model) {
        try {
            TableView table = tableService.getTable(id);
            model.addAttribute("table", table);
            model.addAttribute("pageTitle", "Table " + table.tableName());

            // If table has an open order, fetch its summary
            if (table.currentOrderId() != null) {
                List<OrderSummaryView> openOrders = orderService.listOpenOrders();
                for (OrderSummaryView order : openOrders) {
                    if (order.id().equals(table.currentOrderId())) {
                        model.addAttribute("openOrder", order);
                        break;
                    }
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

    /**
     * Add a new table (POST only). MANAGER only.
     */
    @PostMapping
    public String addTable(@Valid @ModelAttribute CreateTableForm form,
                           BindingResult bindingResult,
                           @ModelAttribute("currentUser") ModelAttributeAdvice.UserContext currentUser,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            // Re-populate model for modal re-open with validation errors
            model.addAttribute("form", form);
            model.addAttribute("tables", tableService.listTables());
            model.addAttribute("tableCounts", tableService.countByStatus());
            model.addAttribute("pageTitle", "Table Management");
            model.addAttribute("canAddTable",
                    currentUser != null && currentUser.roleEnum() == Role.MANAGER);
            return "tables/list";
        }
        try {
            RestaurantTable table = tableService.addTable(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thêm bàn '" + table.getTableName() + "' thành công.");
            return "redirect:/tables?selected=" + table.getId();
        } catch (BusinessException e) {
            model.addAttribute("form", form);
            model.addAttribute("tables", tableService.listTables());
            model.addAttribute("tableCounts", tableService.countByStatus());
            model.addAttribute("pageTitle", "Table Management");
            bindingResult.rejectValue("tableName", "error.table", e.getMessage());
            return "tables/list";
        }
    }

    /**
     * Edit an existing table (POST only). MANAGER only.
     */
    @PostMapping("/{id}/edit")
    public String editTable(@PathVariable("id") Integer id,
                            @Valid @ModelAttribute EditTableForm form,
                            BindingResult bindingResult,
                            @ModelAttribute("currentUser") ModelAttributeAdvice.UserContext currentUser,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            TableView table = tableService.getTable(id);
            model.addAttribute("selectedTable", table);
            model.addAttribute("tables", tableService.listTables());
            model.addAttribute("tableCounts", tableService.countByStatus());
            model.addAttribute("selectedTableId", id);
            model.addAttribute("pageTitle", "Table Management");
            model.addAttribute("canAddTable",
                    currentUser != null && currentUser.roleEnum() == Role.MANAGER);
            model.addAttribute("editForm", form);
            model.addAttribute("editTableId", id);
            return "tables/list";
        }
        try {
            RestaurantTable table = tableService.editTable(id, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Sửa bàn '" + table.getTableName() + "' thành công.");
            return "redirect:/tables?selected=" + table.getId();
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/tables";
        } catch (BusinessException e) {
            TableView table = null;
            try {
                table = tableService.getTable(id);
            } catch (Exception ignored) {}
            if (table != null) {
                model.addAttribute("selectedTable", table);
            }
            model.addAttribute("tables", tableService.listTables());
            model.addAttribute("tableCounts", tableService.countByStatus());
            model.addAttribute("selectedTableId", id);
            model.addAttribute("pageTitle", "Table Management");
            model.addAttribute("canAddTable",
                    currentUser != null && currentUser.roleEnum() == Role.MANAGER);
            model.addAttribute("editForm", form);
            model.addAttribute("editTableId", id);
            bindingResult.rejectValue("tableName", "error.table", e.getMessage());
            return "tables/list";
        }
    }
}
