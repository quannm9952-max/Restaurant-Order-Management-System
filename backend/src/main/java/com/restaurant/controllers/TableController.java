package com.restaurant.controllers;

import com.restaurant.dto.table.TableView;
import com.restaurant.entities.enums.TableStatus;
import com.restaurant.exceptions.BusinessException;
import com.restaurant.exceptions.NotFoundException;
import com.restaurant.services.table.ITableService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing restaurant tables (UC04). Access: WAITER, MANAGER -
 * chỉ WAITER, MANAGER được cập nhật trạng thái bàn và xem đơn/bàn.
 */
@Controller
@RequestMapping("/tables")
public class TableController {

  private final ITableService tableService;

  public TableController(ITableService tableService) {
    this.tableService = tableService;
  }

  /** Redirect root URL to tables list. */
  @GetMapping("/")
  public String rootRedirect() {
    return "redirect:/tables";
  }

  /** List all tables, optionally filtered by status. */
  @GetMapping
  public String listTables(@RequestParam(required = false) String status,
      Model model) {
    var allTables = tableService.listTables();
    var tables = allTables;
    if (status != null && !status.isBlank()) {
      var selected = TableStatus
          .valueOf(status.toUpperCase(java.util.Locale.ROOT));
      tables = allTables.stream().filter(t -> t.status() == selected).toList();
      model.addAttribute("filterStatus", selected.name());
    }
    model.addAttribute("tables", tables);
    model.addAttribute("allTables", allTables);
    if (!tables.isEmpty())
      model.addAttribute("table", tables.getFirst());
    model.addAttribute("statuses", TableStatus.values());
    return "tables/list";
  }

  /** Show table detail with open order info. */
  @GetMapping("/{id}")
  public String tableDetail(@PathVariable("id") Integer id, Model model) {
    TableView table = tableService.getTable(id);
    var tables = tableService.listTables();
    model.addAttribute("table", table);
    model.addAttribute("tables", tables);
    model.addAttribute("allTables", tables);
    model.addAttribute("statuses", TableStatus.values());
    return "tables/list";
  }

  /** Update table status (POST only). */
  @PostMapping("/{id}/status")
  public String updateTableStatus(@PathVariable("id") Integer id,
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
