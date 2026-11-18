package com.example.restaurantmanagement.reponsitories;

import com.example.restaurantmanagement.entities.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Shared repository - thêm method mới thì báo nhóm
 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

    /**
     * Find all available menu items.
     */
    List<MenuItem> findByIsAvailableTrue();

    /**
     * Find all available menu items with category eagerly fetched.
     */
    @Query("SELECT DISTINCT m FROM MenuItem m LEFT JOIN FETCH m.menuCategory WHERE m.isAvailable = true")
    List<MenuItem> findByIsAvailableTrueWithCategory();
}
