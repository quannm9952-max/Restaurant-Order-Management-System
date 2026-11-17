package com.restaurant.repository;

import com.restaurant.entity.MenuItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Shared repository - thêm method mới thì báo nhóm */
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

  /** Find all available menu items. */
  @Query("select m from MenuItem m where "
      + "m.isAvailable = true and m.isActive = true")
  @EntityGraph(attributePaths = "menuCategory")
  List<MenuItem> findByIsAvailableTrue();
}
