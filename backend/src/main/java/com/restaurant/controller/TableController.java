package com.restaurant.controller;

import com.restaurant.dto.table.TableView;
import com.restaurant.entity.RestaurantTable;
import org.springframework.security.access.prepost.PreAuthorize;
import com.restaurant.entity.enums.TableStatus;
import com.restaurant.exception.BusinessException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.service.TableService;
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

  private final TableService tableService;

  public TableController(TableService tableService) {
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
    } catch (BusinessException | NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "Trạng thái không hợp lệ: " + newStatusStr);
    }
    return "redirect:/tables/" + id;
  }
  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/new")
  public String create(Model model) {
    model.addAttribute("table", new RestaurantTable());
    return "tables/form";
  }

  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/{id}/edit")
  public String edit(@PathVariable Integer id, Model model) {
    model.addAttribute("table", tableService.getTable(id));
    return "tables/form";
  }

  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping("/save")
  public String save(@RequestParam(required = false) Integer id,
      @RequestParam String name, @RequestParam int capacity,
      @RequestParam(required = false) TableStatus status,
      RedirectAttributes flash, Model model) {
    try {
      var table = tableService.saveTable(id, name, capacity, status);
      flash.addFlashAttribute("successMessage", "Table saved.");
      return "redirect:/tables/" + table.getId();
    } catch (BusinessException e) {
      var draft = new RestaurantTable();
      draft.setId(id);
      draft.setTableName(name);
      draft.setCapacity(capacity);
      draft.setStatus(status != null
          ? status
          : id == null
              ? TableStatus.AVAILABLE
              : tableService.getTable(id).status());
      model.addAttribute("table", draft);
      model.addAttribute("errorMessage", e.getMessage());
      return "tables/form";
    }
  }
}
