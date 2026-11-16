package com.restaurant.repository;

import com.restaurant.entities.OrderDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Shared repository - thêm method mới thì báo nhóm */
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

  /** Find all order details by order ID. */
  List<OrderDetail> findByOrderId(Integer orderId);
}
