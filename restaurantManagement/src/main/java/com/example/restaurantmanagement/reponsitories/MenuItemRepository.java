package com.example.restaurantmanagement.reponsitories;

import com.example.restaurantmanagement.entities.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Shared repository - thêm method mới thì báo nhóm
 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

    /**
     * Find all available menu items.
     */
    List<MenuItem> findByIsAvailableTrue();
}
