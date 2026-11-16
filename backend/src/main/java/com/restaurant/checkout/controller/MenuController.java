package com.restaurant.checkout.controller;

import com.restaurant.checkout.service.MenuService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/menu")
public class MenuController {

  private final MenuService service;

  public MenuController(MenuService service) {
    this.service = service;
  }

  /** UC09 – 5-Star Menu Management Main View */
  @GetMapping
  public String menuManagement(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String availability,
      @RequestParam(required = false, defaultValue = "all") String priceRange,
      @RequestParam(required = false, defaultValue = "default") String sortBy,
      Model model) {

    Boolean availableFilter = null;
    if ("available".equalsIgnoreCase(availability)) {
      availableFilter = true;
    } else if ("unavailable".equalsIgnoreCase(availability)) {
      availableFilter = false;
    }

    List<Map<String, Object>> items =
        service.searchMenu(search, category, availableFilter, sortBy, priceRange);
    Map<String, Object> stats = service.menuStats();
    List<String> categories = service.categories();

    model.addAttribute("items", items);
    model.addAttribute("stats", stats);
    model.addAttribute("categories", categories);
    model.addAttribute("currentSearch", search != null ? search : "");
    model.addAttribute("currentCategory", category != null ? category : "All");
    model.addAttribute("currentAvailability", availability != null ? availability : "all");
    model.addAttribute("currentPriceRange", priceRange != null ? priceRange : "all");
    model.addAttribute("currentSort", sortBy);

    return "menu";
  }

  /** JSON API to retrieve item details for Edit / View Modal */
  @GetMapping("/{id}/json")
  @ResponseBody
  public ResponseEntity<?> getMenuItemJson(@PathVariable long id) {
    try {
      Map<String, Object> item = service.getMenuItem(id);
      return ResponseEntity.ok(item);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  /** Add new dish to menu */
  @PostMapping
  public String addMenuItem(
      @RequestParam String name,
      @RequestParam String category,
      @RequestParam(required = false) String customCategory,
      @RequestParam(required = false) String description,
      @RequestParam BigDecimal price,
      RedirectAttributes redirectAttrs) {
    try {
      String selectedCategory =
          (category != null && !category.isBlank()) ? category : customCategory;
      if ("__new__".equals(category) && customCategory != null && !customCategory.isBlank()) {
        selectedCategory = customCategory;
      }
      service.addMenu(name, selectedCategory, description, price);
      redirectAttrs.addFlashAttribute(
          "success", "Successfully added new dish '" + name.trim() + "' to menu!");
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute("error", "Error adding menu item: " + e.getMessage());
    }
    return "redirect:/menu";
  }

  /** Direct GET /menu/{id}/edit opens modal */
  @GetMapping("/{id}/edit")
  public String editMenuItemGet(@PathVariable long id, RedirectAttributes redirectAttrs) {
    redirectAttrs.addFlashAttribute("autoOpenEditId", id);
    return "redirect:/menu";
  }

  /** Direct GET /menu/{id} opens modal */
  @GetMapping("/{id}")
  public String viewMenuItemGet(@PathVariable long id, RedirectAttributes redirectAttrs) {
    redirectAttrs.addFlashAttribute("autoOpenViewId", id);
    return "redirect:/menu";
  }

  /** Update menu item */
  @PostMapping({"/{id}/update", "/{id}/edit"})
  public String updateMenuItem(
      @PathVariable long id,
      @RequestParam String name,
      @RequestParam String category,
      @RequestParam(required = false) String customCategory,
      @RequestParam(required = false) String description,
      @RequestParam BigDecimal price,
      @RequestParam(defaultValue = "false") boolean available,
      RedirectAttributes redirectAttrs) {
    try {
      String selectedCategory =
          (category != null && !category.isBlank()) ? category : customCategory;
      if ("__new__".equals(category) && customCategory != null && !customCategory.isBlank()) {
        selectedCategory = customCategory;
      }
      service.updateMenu(id, name, selectedCategory, description, price, available);
      redirectAttrs.addFlashAttribute(
          "success", "Successfully updated dish #" + id + " ('" + name.trim() + "')!");
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute(
          "error", "Error updating dish #" + id + ": " + e.getMessage());
    }
    return "redirect:/menu";
  }

  /** Toggle dish availability */
  @PostMapping({"/{id}/toggle-status", "/{id}/status"})
  public String toggleStatus(
      @PathVariable long id, @RequestParam boolean available, RedirectAttributes redirectAttrs) {
    try {
      service.toggleMenuAvailability(id, available);
      String msg =
          available
              ? "Successfully re-activated dish #" + id + " to Available."
              : "Successfully marked dish #" + id + " as Unavailable.";
      redirectAttrs.addFlashAttribute("success", msg);
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute(
          "error", "Error changing dish #" + id + " status: " + e.getMessage());
    }
    return "redirect:/menu";
  }

  /** Delete menu item */
  @PostMapping({"/{id}/delete", "/{id}/remove"})
  public String deleteMenuItem(
      @PathVariable long id,
      @RequestParam(defaultValue = "false") boolean force,
      RedirectAttributes redirectAttrs) {
    try {
      service.deleteMenu(id, force);
      redirectAttrs.addFlashAttribute(
          "success", "Successfully deactivated dish #" + id + " from menu!");
    } catch (IllegalStateException e) {
      redirectAttrs.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute(
          "error", "Error deleting dish #" + id + ": " + e.getMessage());
    }
    return "redirect:/menu";
  }

  /** Direct GET /menu/{id}/delete opens delete dialog */
  @GetMapping("/{id}/delete")
  public String deleteMenuItemGet(@PathVariable long id, RedirectAttributes redirectAttrs) {
    redirectAttrs.addFlashAttribute("autoOpenDeleteId", id);
    return "redirect:/menu";
  }
}
