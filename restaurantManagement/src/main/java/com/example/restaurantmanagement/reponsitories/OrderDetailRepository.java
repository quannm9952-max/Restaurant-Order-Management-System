package com.example.restaurantmanagement.reponsitories;

import com.example.restaurantmanagement.entities.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Shared repository - thêm method mới thì báo nhóm
 */
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    /**
     * Find all order details by order ID.
     */
    List<OrderDetail> findByOrderId(Integer orderId);
}
