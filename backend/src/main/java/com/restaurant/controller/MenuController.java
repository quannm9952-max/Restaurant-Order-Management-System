package com.restaurant.controller;

import com.restaurant.exception.NotFoundException;
import com.restaurant.config.UploadConfig;
import com.restaurant.service.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/menu")
public class MenuController {
  private static final Logger LOG = LoggerFactory
      .getLogger(MenuController.class);

  private final MenuService service;

  public MenuController(MenuService service) {
    this.service = service;
  }

  /** UC09 – 5-Star Menu Management Main View */
  @GetMapping
  @SuppressWarnings("unchecked")
  public String menuManagement(@RequestParam(required = false) String search,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String availability,
      @RequestParam(required = false, defaultValue = "all") String priceRange,
      @RequestParam(required = false, defaultValue = "default") String sortBy,
      Model model, HttpServletRequest request) {

    var session = request.getSession(false);
    if (session != null
        && session.getAttribute(UploadConfig.ERROR_ATTRIBUTE) != null) {
      model.addAttribute("error",
          session.getAttribute(UploadConfig.ERROR_ATTRIBUTE));
      session.removeAttribute(UploadConfig.ERROR_ATTRIBUTE);
    }

    Boolean availableFilter = null;
    if ("available".equalsIgnoreCase(availability)) {
      availableFilter = true;
    } else if ("unavailable".equalsIgnoreCase(availability)) {
      availableFilter = false;
    }

    List<Map<String, Object>> items = service.searchMenu(search, category,
        availableFilter, sortBy, priceRange);
    var draft = model.asMap().get("menuDraft");
    if (draft instanceof Map<?, ?> submitted && submitted.get("id") != null) {
      items = items.stream()
          .map(item -> item.get("id").toString()
              .equals(submitted.get("id").toString())
                  ? mergedDraft(item, (Map<String, Object>) draft)
                  : item)
          .toList();
    }
    Map<String, Object> stats = service.menuStats();
    List<String> categories = service.categories();

    model.addAttribute("items", items);
    model.addAttribute("stats", stats);
    model.addAttribute("categories", categories);
    model.addAttribute("categoryRows", service.categoryRows());
    model.addAttribute("currentSearch", search != null ? search : "");
    model.addAttribute("currentCategory", category != null ? category : "All");
    model.addAttribute("currentAvailability",
        availability != null ? availability : "all");
    model.addAttribute("currentPriceRange",
        priceRange != null ? priceRange : "all");
    model.addAttribute("currentSort", sortBy);

    return "menu";
  }

  @PostMapping("/categories/save")
  public String saveCategory(@RequestParam(required = false) Long id,
      @RequestParam String name, RedirectAttributes flash) {
    try {
      service.saveCategory(id, name);
      flash.addFlashAttribute("success", "Category saved.");
    } catch (IllegalArgumentException ex) {
      flash.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/menu";
  }

  @PostMapping("/categories/{id}/delete")
  public String deleteCategory(@PathVariable long id,
      RedirectAttributes flash) {
    try {
      service.deleteCategory(id);
      flash.addFlashAttribute("success", "Category deleted.");
    } catch (IllegalArgumentException
        | com.restaurant.exception.BusinessException ex) {
      flash.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/menu";
  }

  /** JSON API to retrieve item details for Edit / View Modal */
  @GetMapping("/{id}/json")
  @ResponseBody
  public ResponseEntity<?> getMenuItemJson(@PathVariable long id) {
    try {
      return ResponseEntity.ok(service.getMenuItem(id));
    } catch (NotFoundException ex) {
      return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    } catch (org.springframework.dao.DataAccessException ex) {
      LOG.error("Unable to load record details", ex);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Unable to load details. Please try again."));
    }
  }

  /** Add new dish to menu */
  @PostMapping
  public String addMenuItem(@RequestParam String name,
      @RequestParam String category,
      @RequestParam(required = false) String customCategory,
      @RequestParam(required = false) String description,
      @RequestParam BigDecimal price,
      @RequestParam(required = false) MultipartFile image,
      RedirectAttributes redirectAttrs) {
    try {
      String selectedCategory = selectedCategory(category, customCategory);
      service.addMenu(name, selectedCategory, description, price, image);
      redirectAttrs.addFlashAttribute("success",
          "Successfully added new dish '" + name.trim() + "' to menu!");
    } catch (IllegalArgumentException
        | com.restaurant.exception.BusinessException | NotFoundException e) {
      redirectAttrs.addFlashAttribute("error",
          "Error adding menu item: " + e.getMessage());
      retainDraft(redirectAttrs, null, name, category, customCategory,
          description, price, true);
    }
    return "redirect:/menu";
  }

  /** Direct GET /menu/{id}/edit opens modal */
  @GetMapping("/{id}/edit")
  public String editMenuItemGet(@PathVariable long id,
      RedirectAttributes redirectAttrs) {
    redirectAttrs.addFlashAttribute("autoOpenEditId", id);
    return "redirect:/menu";
  }

  /** Direct GET /menu/{id} opens modal */
  @GetMapping("/{id}")
  public String viewMenuItemGet(@PathVariable long id,
      RedirectAttributes redirectAttrs) {
    redirectAttrs.addFlashAttribute("autoOpenViewId", id);
    return "redirect:/menu";
  }

  /** Update menu item */
  @PostMapping({"/{id}/update", "/{id}/edit"})
  public String updateMenuItem(@PathVariable long id, @RequestParam String name,
      @RequestParam String category,
      @RequestParam(required = false) String customCategory,
      @RequestParam(required = false) String description,
      @RequestParam BigDecimal price,
      @RequestParam(defaultValue = "false") boolean available,
      @RequestParam(required = false) MultipartFile image,
      RedirectAttributes redirectAttrs) {
    try {
      String selectedCategory = selectedCategory(category, customCategory);
      service.updateMenu(id, name, selectedCategory, description, price,
          available, image);
      redirectAttrs.addFlashAttribute("success",
          "Successfully updated dish #" + id + " ('" + name.trim() + "')!");
    } catch (IllegalArgumentException
        | com.restaurant.exception.BusinessException | NotFoundException e) {
      redirectAttrs.addFlashAttribute("error",
          "Error updating dish #" + id + ": " + e.getMessage());
      retainDraft(redirectAttrs, id, name, category, customCategory,
          description, price, available);
    }
    return "redirect:/menu";
  }

  /** Toggle dish availability */
  @PostMapping({"/{id}/toggle-status", "/{id}/status"})
  public String toggleStatus(@PathVariable long id,
      @RequestParam boolean available, RedirectAttributes redirectAttrs) {
    try {
      service.toggleMenuAvailability(id, available);
      String msg = available
          ? "Successfully re-activated dish #" + id + " to Available."
          : "Successfully marked dish #" + id + " as Unavailable.";
      redirectAttrs.addFlashAttribute("success", msg);
    } catch (IllegalArgumentException
        | com.restaurant.exception.BusinessException | NotFoundException e) {
      redirectAttrs.addFlashAttribute("error",
          "Error changing dish #" + id + " status: " + e.getMessage());
    }
    return "redirect:/menu";
  }

  /** Delete menu item */
  @PostMapping({"/{id}/delete", "/{id}/remove"})
  public String deleteMenuItem(@PathVariable long id,
      RedirectAttributes redirectAttrs) {
    try {
      service.deleteMenu(id);
      redirectAttrs.addFlashAttribute("success",
          "Menu item deleted. Previous order records have been retained.");
    } catch (IllegalStateException e) {
      redirectAttrs.addFlashAttribute("error", e.getMessage());
    } catch (IllegalArgumentException
        | com.restaurant.exception.BusinessException | NotFoundException e) {
      redirectAttrs.addFlashAttribute("error",
          "Error deleting dish #" + id + ": " + e.getMessage());
    }
    return "redirect:/menu";
  }

  /** Direct GET /menu/{id}/delete opens delete dialog */
  @GetMapping("/{id}/delete")
  public String deleteMenuItemGet(@PathVariable long id,
      RedirectAttributes redirectAttrs) {
    service.getMenuItem(id);
    redirectAttrs.addFlashAttribute("autoOpenDeleteId", id);
    return "redirect:/menu";
  }

  private void retainDraft(RedirectAttributes flash, Long id, String name,
      String category, String customCategory, String description,
      BigDecimal price, boolean available) {
    var draft = new java.util.HashMap<String, Object>();
    draft.put("id", id);
    draft.put("name", name);
    draft.put("category",
        "__new__".equals(category) ? customCategory : category);
    draft.put("description", description);
    draft.put("price", price);
    draft.put("available", available);
    if (id == null)
      draft.put("image_url", null);
    flash.addFlashAttribute("menuDraft", draft);
    if (id != null)
      flash.addFlashAttribute("autoOpenEditId", id);
  }

  private String selectedCategory(String category, String customCategory) {
    return category == null || category.isBlank() || "__new__".equals(category)
        ? customCategory
        : category;
  }

  private Map<String, Object> mergedDraft(Map<String, Object> item,
      Map<String, Object> draft) {
    var merged = new java.util.HashMap<>(item);
    merged.putAll(draft);
    return merged;
  }
}
