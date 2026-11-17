package com.restaurant.service;

import com.restaurant.dto.menu.MenuItemValues;
import com.restaurant.exception.BusinessException;
import com.restaurant.repository.MenuRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('MANAGER')")
public class MenuService {
  private final MenuRepository menuRepository;

  public MenuService(MenuRepository menuRepository) {
    this.menuRepository = menuRepository;
  }

  public Map<String, Object> menuStats() {
    var row = menuRepository.stats();
    return Map.of("totalItems", row.get("total_items"), "availableItems",
        row.get("available_items"), "unavailableItems",
        row.get("unavailable_items"), "totalCategories",
        row.get("total_categories"));
  }

  public List<String> categories() {
    return menuRepository.categories();
  }
  public List<Map<String, Object>> categoryRows() {
    return menuRepository.categoryRows();
  }

  @Transactional
  public void saveCategory(Long id, String name) {
    if (name == null || name.isBlank() || name.trim().length() > 100) {
      throw new IllegalArgumentException(
          "Category name is required and must not exceed 100 characters.");
    }
    name = name.trim();
    if (menuRepository.categoryNameExists(name, id)) {
      throw new IllegalArgumentException("Category name already exists.");
    }
    if (id != null && !menuRepository.activeCategoryExists(id)) {
      throw new IllegalArgumentException("Category not found.");
    }
    menuRepository.saveCategory(id, name);
  }

  public List<Map<String, Object>> searchMenu(String search, String category,
      Boolean available, String sort, String priceRange) {
    return menuRepository
        .searchMenu(search, category, available, sort, priceRange).stream()
        .map(this::withPhoto).toList();
  }

  public Map<String, Object> getMenuItem(long id) {
    return withPhoto(menuRepository.findItem(id));
  }

  private Map<String, Object> withPhoto(Map<String, Object> item) {
    item.put("image_url", com.restaurant.support.MenuImages
        .forItemName(item.get("name").toString()));
    item.put("created_at", "");
    return item;
  }

  private MenuItemValues validateItem(String name, String category,
      String description, BigDecimal price) {
    if (name == null || name.isBlank() || name.trim().length() > 150)
      throw new IllegalArgumentException(
          "Dish name is required and must not exceed 150 characters.");
    if (category == null || category.isBlank()
        || category.trim().length() > 100)
      throw new IllegalArgumentException(
          "Category is required and must not exceed 100 characters.");
    if (price == null || price.signum() <= 0 || price.scale() > 2
        || price.compareTo(new BigDecimal("9999999999.99")) > 0)
      throw new IllegalArgumentException(
          "Enter a positive USD price with at most two decimal places.");
    if (description != null && description.trim().length() > 255)
      throw new IllegalArgumentException(
          "Description must not exceed 255 characters.");
    return new MenuItemValues(name.trim(), category.trim(),
        description == null ? "" : description.trim(), price);
  }

  @Transactional
  public void addMenu(String name, String category, String description,
      BigDecimal price) {
    var values = validateItem(name, category, description, price);
    checkDuplicateName(values.name(), -1L);
    menuRepository.insertItem(values);
  }

  @Transactional
  public void updateMenu(long id, String name, String category,
      String description, BigDecimal price, boolean available) {
    getMenuItem(id);
    var values = validateItem(name, category, description, price);
    checkDuplicateName(values.name(), id);
    menuRepository.updateItem(id, values, available);
  }

  private void checkDuplicateName(String name, long exceptId) {
    if (menuRepository.itemNameExists(name, exceptId)) {
      throw new IllegalArgumentException("Dish name already exists.");
    }
  }

  @Transactional
  public void toggleMenuAvailability(long id, boolean available) {
    getMenuItem(id);
    if (menuRepository.setAvailability(id, available) != 1) {
      throw new BusinessException("An inactive dish cannot be made available.");
    }
  }

  @Transactional
  public void deleteMenu(long id, boolean force) {
    getMenuItem(id);
    menuRepository.deactivate(id);
  }
}
