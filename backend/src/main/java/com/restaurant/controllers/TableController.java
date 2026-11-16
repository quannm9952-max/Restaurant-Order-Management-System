package com.restaurant.controllers;

import com.restaurant.dto.order.OrderSummaryView;
import com.restaurant.dto.table.TableView;
import com.restaurant.entities.enums.TableStatus;
import com.restaurant.exceptions.BusinessException;
import com.restaurant.exceptions.NotFoundException;
import com.restaurant.services.order.IOrderService;
import com.restaurant.services.table.ITableService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing restaurant tables (UC04). // TODO(auth): WAITER, MANAGER - chỉ WAITER,
 * MANAGER được cập nhật trạng thái bàn và xem đơn/bàn.
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

  /** Redirect root URL to tables list. */
  @GetMapping("/")
  public String rootRedirect() {
    return "redirect:/tables";
  }

  /** List all tables, optionally filtered by status. */
  @GetMapping
  public String listTables(@RequestParam(required = false) String status, Model model) {
    List<TableView> tables;
    if (status != null && !status.isBlank()) {
      tables = tableService.listTablesByStatus(status);
      model.addAttribute("filterStatus", status);
    } else {
      tables = tableService.listTables();
    }
    model.addAttribute("tables", tables);
    model.addAttribute("statuses", TableStatus.values());
    return "tables/list";
  }

  /** Show table detail with open order info. */
  @GetMapping("/{id}")
  public String tableDetail(@PathVariable("id") Integer id, Model model) {
    try {
      TableView table = tableService.getTable(id);
      model.addAttribute("table", table);

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

  /** Update table status (POST only). */
  @PostMapping("/{id}/status")
  public String updateTableStatus(
      @PathVariable("id") Integer id,
      @RequestParam("newStatus") String newStatusStr,
      RedirectAttributes redirectAttributes) {
    try {
      TableStatus newStatus = TableStatus.valueOf(newStatusStr);
      tableService.updateTableStatus(id, newStatus);
      redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái bàn thành công.");
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Trạng thái không hợp lệ: " + newStatusStr);
    }
    return "redirect:/tables/" + id;
  }
}
