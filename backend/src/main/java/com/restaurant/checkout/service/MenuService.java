package com.restaurant.checkout.service;

import com.restaurant.checkout.repository.MenuRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('MANAGER')")
public class MenuService {
  private final MenuRepository db;

  public MenuService(MenuRepository db) {
    this.db = db;
  }

  public Map<String, Object> menuStats() {
    var m = new LinkedHashMap<String, Object>();
    m.put("totalItems", db.count("SELECT COUNT(*) FROM Menu_item WHERE is_active=true", Map.of()));
    m.put(
        "availableItems",
        db.count(
            "SELECT COUNT(*) FROM Menu_item WHERE is_active=true AND is_available=true", Map.of()));
    m.put(
        "unavailableItems",
        db.count(
            "SELECT COUNT(*) FROM Menu_item WHERE is_active=true AND is_available=false",
            Map.of()));
    m.put("totalCategories", categories().size());
    return m;
  }

  public List<String> categories() {
    return db
        .rows(
            "SELECT category_name FROM Menu_category WHERE is_active=true ORDER BY category_name",
            Map.of())
        .stream()
        .map(m -> m.get("category_name").toString())
        .toList();
  }

  public List<Map<String, Object>> categoryRows() {
    return db.rows(
        "SELECT c.category_id AS id,c.category_name AS name,COUNT(m.item_id) AS item_count FROM"
            + " Menu_category c LEFT JOIN Menu_item m ON m.category_id=c.category_id AND"
            + " m.is_active=true WHERE c.is_active=true GROUP BY c.category_id,c.category_name"
            + " ORDER BY c.category_name",
        Map.of());
  }

  @Transactional
  public void saveCategory(Long id, String name) {
    if (name == null || name.isBlank() || name.trim().length() > 100)
      throw new IllegalArgumentException(
          "Category name is required and must not exceed 100 characters.");
    var p = new HashMap<String, Object>();
    p.put("name", name.trim());
    p.put("id", id == null ? -1L : id);
    if (db.count(
            "SELECT COUNT(*) FROM Menu_category WHERE LOWER(category_name)=LOWER(:name) AND"
                + " category_id<>:id",
            p)
        > 0) throw new IllegalArgumentException("Category name already exists.");
    if (id == null)
      db.update("INSERT INTO Menu_category(category_name,is_active) VALUES(:name,true)", p);
    else {
      if (db.count("SELECT COUNT(*) FROM Menu_category WHERE category_id=:id AND is_active=true", p)
          == 0) throw new IllegalArgumentException("Category not found.");
      db.update("UPDATE Menu_category SET category_name=:name WHERE category_id=:id", p);
    }
  }

  public List<Map<String, Object>> searchMenu(
      String search, String category, Boolean available, String sort, String priceRange) {
    var p = new HashMap<String, Object>();
    String sql = MenuRepository.SELECT + " WHERE m.is_active=true";
    if (search != null && !search.isBlank()) {
      sql += " AND LOWER(m.item_name) LIKE :q";
      p.put("q", "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
    }
    if (category != null && !category.isBlank() && !"All".equals(category)) {
      sql += " AND c.category_name=:category";
      p.put("category", category);
    }
    if (available != null) {
      sql += " AND m.is_available=:available";
      p.put("available", available);
    }
    sql +=
        switch (priceRange == null ? "all" : priceRange) {
          case "under_10" -> " AND m.unit_price < 10";
          case "10_30" -> " AND m.unit_price BETWEEN 10 AND 30";
          case "30_60" -> " AND m.unit_price > 30 AND m.unit_price <= 60";
          case "above_60" -> " AND m.unit_price > 60";
          default -> "";
        };
    sql +=
        " ORDER BY "
            + switch (sort == null ? "default" : sort) {
              case "price_asc" -> "m.unit_price ASC";
              case "price_desc" -> "m.unit_price DESC";
              case "name_asc" -> "m.item_name ASC";
              case "name_desc" -> "m.item_name DESC";
              default -> "m.item_id DESC";
            };
    return db.rows(sql, p).stream().map(this::decorate).toList();
  }

  public Map<String, Object> getMenuItem(long id) {
    return decorate(db.one(MenuRepository.SELECT + " WHERE m.item_id=:id", Map.of("id", id)));
  }

  private Map<String, Object> decorate(Map<String, Object> m) {
    m.put("image_url", com.restaurant.web.MenuImages.forItemName(m.get("name").toString()));
    m.put("created_at", "");
    return m;
  }

  private Map<String, Object> values(
      String name, String category, String description, BigDecimal price) {
    if (name == null || name.isBlank() || name.trim().length() > 150)
      throw new IllegalArgumentException(
          "Dish name is required and must not exceed 150 characters.");
    if (category == null || category.isBlank() || category.trim().length() > 100)
      throw new IllegalArgumentException(
          "Category is required and must not exceed 100 characters.");
    if (price == null
        || price.signum() <= 0
        || price.scale() > 2
        || price.compareTo(new BigDecimal("9999999999.99")) > 0)
      throw new IllegalArgumentException(
          "Enter a positive USD price with at most two decimal places.");
    if (description != null && description.trim().length() > 255)
      throw new IllegalArgumentException("Description must not exceed 255 characters.");
    var p = new HashMap<String, Object>();
    p.put("name", name.trim());
    p.put("category", category.trim());
    p.put("description", description == null ? "" : description.trim());
    p.put("price", price);
    return p;
  }

  private long categoryId(Map<String, Object> p) {
    var rows = db.rows("SELECT category_id FROM Menu_category WHERE category_name=:category", p);
    if (rows.isEmpty()) {
      db.update("INSERT INTO Menu_category(category_name,is_active) VALUES(:category,true)", p);
      rows = db.rows("SELECT category_id FROM Menu_category WHERE category_name=:category", p);
    }
    return ((Number) rows.getFirst().get("category_id")).longValue();
  }

  @Transactional
  public void addMenu(String name, String category, String description, BigDecimal price) {
    var p = values(name, category, description, price);
    if (db.count(
            "SELECT COUNT(*) FROM Menu_item WHERE LOWER(item_name)=LOWER(:name) AND is_active=true",
            p)
        > 0) throw new IllegalArgumentException("Dish name already exists.");
    p.put("categoryId", categoryId(p));
    db.update(
        "INSERT INTO Menu_item(category_id,item_name,description,unit_price,is_active,is_available)"
            + " VALUES(:categoryId,:name,:description,:price,true,true)",
        p);
  }

  @Transactional
  public void updateMenu(
      long id,
      String name,
      String category,
      String description,
      BigDecimal price,
      boolean available) {
    getMenuItem(id);
    var p = values(name, category, description, price);
    p.put("id", id);
    if (db.count(
            "SELECT COUNT(*) FROM Menu_item WHERE LOWER(item_name)=LOWER(:name) AND item_id<>:id"
                + " AND is_active=true",
            p)
        > 0) throw new IllegalArgumentException("Dish name already exists.");
    p.put("categoryId", categoryId(p));
    p.put("available", available);
    db.update(
        "UPDATE Menu_item SET"
            + " item_name=:name,category_id=:categoryId,description=:description,unit_price=:price,is_available=:available"
            + " WHERE item_id=:id",
        p);
  }

  @Transactional
  public void toggleMenuAvailability(long id, boolean available) {
    getMenuItem(id);
    db.update(
        "UPDATE Menu_item SET is_available=:available WHERE item_id=:id AND is_active=true",
        Map.of("id", id, "available", available));
  }

  @Transactional
  public void deleteMenu(long id, boolean force) {
    getMenuItem(id);
    db.update(
        "UPDATE Menu_item SET is_active=false,is_available=false WHERE item_id=:id",
        Map.of("id", id));
  }
}
