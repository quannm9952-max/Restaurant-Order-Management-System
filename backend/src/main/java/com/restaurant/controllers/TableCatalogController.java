package com.restaurant.controllers;

import com.restaurant.entities.RestaurantTable;
import com.restaurant.services.table.TableCatalogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('MANAGER')")
public class TableCatalogController {
  private final TableCatalogService catalog;

  public TableCatalogController(TableCatalogService catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/tables/new")
  public String create(Model model) {
    model.addAttribute("table", new RestaurantTable());
    return "tables/form";
  }

  @GetMapping("/tables/{id}/edit")
  public String edit(@PathVariable Integer id, Model model) {
    model.addAttribute("table", catalog.get(id));
    return "tables/form";
  }

  @PostMapping("/tables/save")
  public String save(@RequestParam(required = false) Integer id,
      @RequestParam String name, @RequestParam int capacity,
      RedirectAttributes flash, Model model) {
    try {
      var table = catalog.save(id, name, capacity);
      flash.addFlashAttribute("successMessage", "Table saved.");
      return "redirect:/tables/" + table.getId();
    } catch (com.restaurant.exceptions.BusinessException e) {
      var draft = new RestaurantTable();
      draft.setId(id);
      draft.setTableName(name);
      draft.setCapacity(capacity);
      model.addAttribute("table", draft);
      model.addAttribute("errorMessage", e.getMessage());
      return "tables/form";
    }
  }
}
