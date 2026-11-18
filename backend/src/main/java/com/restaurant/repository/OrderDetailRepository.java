package com.restaurant.repository;

import com.restaurant.dto.preparation.PreparationLine;
import com.restaurant.entity.OrderDetail;
import com.restaurant.entity.enums.OrderStatus;
import com.restaurant.entity.enums.CookingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Shared repository - thêm method mới thì báo nhóm */
public interface OrderDetailRepository
    extends
      JpaRepository<OrderDetail, Integer> {

  /** Find all order details by order ID. */
  List<OrderDetail> findByOrderId(Integer orderId);

  Optional<OrderDetail> findByIdAndOrderId(Integer id, Integer orderId);

  @Query("select new com.restaurant.dto.preparation."
      + "PreparationLine(d.id,o.id,t.id,t.tableName,"
      + "d.itemNameAtOrder,d.quantity,d.cookingStatus,o.createdAt,o.note,o.status)"
      + " from OrderDetail d join d.order o "
      + "join o.table t where o.status=:orderStatus"
      + " order by o.createdAt,o.id,d.id")
  List<PreparationLine> findPreparationLines(
      @Param("orderStatus") OrderStatus orderStatus);
  /** Include all lines for accurate progress, including paid orders. */
  @Query("select new com.restaurant.dto.preparation."
      + "PreparationLine(d.id,o.id,t.id,t.tableName,"
      + "d.itemNameAtOrder,d.quantity,d.cookingStatus,o.createdAt,o.note,o.status)"
      + " from OrderDetail d join d.order o join o.table t"
      + " where exists (select s.id from OrderDetail s"
      + " where s.order=o and s.cookingStatus=:servedStatus)"
      + " order by o.createdAt desc,o.id desc,d.id")
  List<PreparationLine> findOrdersWithServedLines(
      @Param("servedStatus") CookingStatus servedStatus);
}
