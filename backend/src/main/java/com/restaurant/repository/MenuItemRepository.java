package com.restaurant.repository;

import com.restaurant.entities.MenuItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Shared repository - thêm method mới thì báo nhóm */
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

  /** Find all available menu items. */
  @org.springframework.data.jpa.repository.Query(
      "select m from MenuItem m where m.isAvailable = true and m.isActive = true")
  List<MenuItem> findByIsAvailableTrue();
}
