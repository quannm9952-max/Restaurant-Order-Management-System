package com.restaurant.repository;

import com.restaurant.entity.Order;
import com.restaurant.entity.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Integer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.id = :id")
  Optional<Order> lockForUpdate(@Param("id") Integer id);

  @Query("SELECT COUNT(i) > 0 FROM Invoice i WHERE i.order.id = :id")
  boolean hasGeneratedBill(@Param("id") Integer id);

  /** Check if a table has an OPEN order. */
  @Query("SELECT COUNT(o) > 0 FROM Order o WHERE "
      + "o.table.id = :tableId AND o.status = 'OPEN'")
  boolean existsOpenByTableId(@Param("tableId") Integer tableId);

  /**
   * Find the OPEN order for a specific table (returns just the order ID). Used
   * by TableService to populate currentOrderId in TableView.
   */
  @Query("SELECT o.id FROM Order o WHERE "
      + "o.table.id = :tableId AND o.status = 'OPEN'")
  Optional<Integer> findOpenOrderIdByTableId(@Param("tableId") Integer tableId);

  interface OpenTableOrder {
    Integer getTableId();
    Integer getOrderId();
  }

  @Query("SELECT o.table.id AS tableId,o.id AS orderId FROM Order o "
      + "WHERE o.status='OPEN'")
  List<OpenTableOrder> findOpenTableOrders();

  /** Find orders by status with JOIN FETCH to avoid N+1 for details. */
  @Query("SELECT DISTINCT o FROM Order o LEFT JOIN "
      + "FETCH o.details WHERE o.status = :status")
  List<Order> findByStatusWithDetails(@Param("status") OrderStatus status);

  /** Find an order by ID with JOIN FETCH for details, table, and employee. */
  @Query("SELECT DISTINCT o FROM Order o LEFT "
      + "JOIN FETCH o.details WHERE o.id = :id")
  Optional<Order> findByIdWithDetails(@Param("id") Integer id);

  /** Find open orders created after a given date, for listing purposes. */
  @Query("SELECT DISTINCT o FROM Order o LEFT JOIN "
      + "FETCH o.details WHERE o.status = 'OPEN' AND"
      + " o.createdAt >= :since ORDER BY o.createdAt DESC")
  List<Order> findOpenOrdersSince(@Param("since") LocalDateTime since);
}
