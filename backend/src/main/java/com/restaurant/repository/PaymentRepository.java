package com.restaurant.repository;

import com.restaurant.entities.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

  /** Check if an order has a SUCCESSFUL payment. */
  @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE "
      + "p.order.id = :orderId AND p.status = 'SUCCESSFUL'")
  boolean existsSuccessfulByOrderId(@Param("orderId") Integer orderId);

  /** Find all payments for an order. */
  List<Payment> findByOrderId(Integer orderId);
}
