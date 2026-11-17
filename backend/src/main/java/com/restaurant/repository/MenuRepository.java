package com.restaurant.repository;

import com.restaurant.repository.support.JdbcRepositorySupport;

import com.restaurant.dto.menu.MenuItemValues;
import com.restaurant.exception.NotFoundException;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Persistence for menu items and categories in the shared database. */
@Repository
public class MenuRepository extends JdbcRepositorySupport {
  private static final String SELECT = "SELECT m.item_id AS id,m.item_name "
      + "AS name,c.category_name AS category,"
      + "m.description,m.unit_price AS price,m.is_available AS available,"
      + "m.is_active FROM Menu_item m JOIN Menu_category c "
      + "ON c.category_id=m.category_id ";

  public MenuRepository(NamedParameterJdbcTemplate jdbc) {
    super(jdbc);
  }

  public Map<String, Object> stats() {
    return one("""
        SELECT COUNT(*) AS total_items,
               COALESCE(SUM(CASE WHEN is_available=true THEN 1 ELSE 0 END),0)
                   AS available_items,
               COALESCE(SUM(CASE WHEN is_available=false THEN 1 ELSE 0 END),0)
                   AS unavailable_items,
               (SELECT COUNT(*) FROM Menu_category WHERE is_active=true)
                   AS total_categories
        FROM Menu_item WHERE is_active=true
        """, Map.of());
  }

  public List<String> categories() {
    return rows("SELECT category_name FROM Menu_category "
        + "WHERE is_active=true ORDER BY category_name", Map.of()).stream()
        .map(row -> row.get("category_name").toString()).toList();
  }

  public List<Map<String, Object>> categoryRows() {
    return rows("""
        SELECT c.category_id AS id,c.category_name AS name,
               COUNT(m.item_id) AS item_count
        FROM Menu_category c LEFT JOIN Menu_item m
          ON m.category_id=c.category_id AND m.is_active=true
        WHERE c.is_active=true GROUP BY c.category_id,c.category_name
        ORDER BY c.category_name
        """, Map.of());
  }

  public boolean categoryNameExists(String name, Long exceptId) {
    return count(
        "SELECT COUNT(*) FROM Menu_category "
            + "WHERE LOWER(category_name)=LOWER(:name) AND category_id<>:id",
        Map.of("name", name, "id", exceptId == null ? -1L : exceptId)) > 0;
  }

  public boolean activeCategoryExists(long id) {
    return count("SELECT COUNT(*) FROM Menu_category "
        + "WHERE category_id=:id AND is_active=true", Map.of("id", id)) > 0;
  }

  public boolean lockActiveCategory(long id) {
    return !rows("SELECT category_id FROM Menu_category WHERE category_id=:id "
        + "AND is_active=true FOR UPDATE", Map.of("id", id)).isEmpty();
  }

  public long activeItemCount(long categoryId) {
    return count(
        "SELECT COUNT(*) FROM Menu_item WHERE category_id=:id AND is_active=true",
        Map.of("id", categoryId));
  }

  public void deactivateCategory(long id) {
    update("UPDATE Menu_category SET is_active=false WHERE category_id=:id",
        Map.of("id", id));
  }

  public boolean restoreCategory(String name) {
    var params = Map.of("name", name);
    if (count(
        "SELECT COUNT(*) FROM Menu_category WHERE LOWER(category_name)=LOWER(:name) "
            + "AND is_active=true",
        params) > 0)
      return false;
    var found = rows(
        "SELECT category_id FROM Menu_category WHERE LOWER(category_name)=LOWER(:name) "
            + "AND is_active=false FOR UPDATE",
        params);
    if (found.isEmpty())
      return false;
    update(
        "UPDATE Menu_category SET category_name=:name,is_active=true WHERE category_id=:id",
        Map.of("name", name, "id", found.getFirst().get("category_id")));
    return true;
  }

  public void saveCategory(Long id, String name) {
    if (id == null) {
      update("INSERT INTO Menu_category(category_name,is_active) "
          + "VALUES(:name,true)", Map.of("name", name));
    } else {
      update("UPDATE Menu_category SET category_name=:name "
          + "WHERE category_id=:id", Map.of("id", id, "name", name));
    }
  }

  public Map<String, Object> findItem(long id) {
    return one(SELECT + " WHERE m.item_id=:id AND m.is_active=true",
        Map.of("id", id));
  }

  public boolean itemNameExists(String name, long exceptId) {
    return count(
        "SELECT COUNT(*) FROM Menu_item WHERE LOWER(item_name)=LOWER(:name) "
            + "AND item_id<>:id AND is_active=true",
        Map.of("name", name, "id", exceptId)) > 0;
  }

  private long categoryId(String name) {
    var params = Map.of("category", name);
    var found = rows("SELECT category_id FROM Menu_category "
        + "WHERE LOWER(category_name)=LOWER(:category) AND is_active=true FOR UPDATE",
        params);
    if (found.isEmpty()) {
      if (count(
          "SELECT COUNT(*) FROM Menu_category WHERE LOWER(category_name)=LOWER(:category)",
          params) > 0)
        throw new NotFoundException(
            "This category has been deleted. Choose an active category.");
      update("INSERT INTO Menu_category(category_name,is_active) "
          + "VALUES(:category,true)", params);
      found = rows("SELECT category_id FROM Menu_category "
          + "WHERE category_name=:category", params);
    }
    return ((Number) found.getFirst().get("category_id")).longValue();
  }

  private Map<String, Object> parameters(MenuItemValues values) {
    var params = new HashMap<String, Object>();
    params.put("name", values.name());
    params.put("categoryId", categoryId(values.category()));
    params.put("description", values.description());
    params.put("price", values.price());
    return params;
  }

  public long insertItem(MenuItemValues values) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        "INSERT INTO Menu_item(category_id,item_name,description,"
            + "unit_price,is_active,is_available) "
            + "VALUES(:categoryId,:name,:description,:price,true,true)",
        new MapSqlParameterSource(parameters(values)), key,
        new String[]{"item_id"});
    return key.getKey().longValue();
  }

  public void updateItem(long id, MenuItemValues values, boolean available) {
    var params = parameters(values);
    params.put("id", id);
    params.put("available", available);
    update("UPDATE Menu_item SET item_name=:name,category_id=:categoryId,"
        + "description=:description,unit_price=:price,is_available=:available "
        + "WHERE item_id=:id", params);
  }

  public int setAvailability(long id, boolean available) {
    return update(
        "UPDATE Menu_item SET is_available=:available "
            + "WHERE item_id=:id AND is_active=true",
        Map.of("id", id, "available", available));
  }

  public void deactivate(long id) {
    update("UPDATE Menu_item SET is_active=false,is_available=false "
        + "WHERE item_id=:id", Map.of("id", id));
  }
  public List<Map<String, Object>> searchMenu(String search, String category,
      Boolean available, String sort, String priceRange) {
    var p = new HashMap<String, Object>();
    String sql = SELECT + " WHERE m.is_active=true";
    if (search != null && !search.isBlank()) {
      sql += " AND LOWER(m.item_name) LIKE :q ESCAPE '!'";
      p.put("q", com.restaurant.repository.support.SqlSearch
          .containsLiteral(search.trim().toLowerCase(Locale.ROOT)));
    }
    if (category != null && !category.isBlank() && !"All".equals(category)) {
      sql += " AND c.category_name=:category";
      p.put("category", category);
    }
    if (available != null) {
      sql += " AND m.is_available=:available";
      p.put("available", available);
    }
    sql += switch (priceRange == null ? "all" : priceRange) {
      case "under_10" -> " AND m.unit_price < 10";
      case "10_30" -> " AND m.unit_price BETWEEN 10 AND 30";
      case "30_60" -> " AND m.unit_price > 30 AND m.unit_price <= 60";
      case "above_60" -> " AND m.unit_price > 60";
      default -> "";
    };
    sql += " ORDER BY " + switch (sort == null ? "default" : sort) {
      case "price_asc" -> "m.unit_price ASC";
      case "price_desc" -> "m.unit_price DESC";
      case "name_asc" -> "m.item_name ASC";
      case "name_desc" -> "m.item_name DESC";
      default -> "m.item_id DESC";
    };
    return rows(sql, p);
  }

}
