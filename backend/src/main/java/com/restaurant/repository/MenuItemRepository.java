package com.restaurant.repository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.EntityGraph;

import com.restaurant.entities.MenuItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Shared repository - thêm method mới thì báo nhóm */
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

  /** Find all available menu items. */
  @Query("select m from MenuItem m where "
      + "m.isAvailable = true and m.isActive = true")
  @EntityGraph(attributePaths = "menuCategory")
  List<MenuItem> findByIsAvailableTrue();
}
