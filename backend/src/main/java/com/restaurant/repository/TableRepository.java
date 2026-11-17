package com.restaurant.repository;

import com.restaurant.entities.RestaurantTable;
import com.restaurant.entities.enums.TableStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableRepository
    extends
      JpaRepository<RestaurantTable, Integer> {

  @Query("SELECT COUNT(t)>0 FROM RestaurantTable t "
      + "WHERE LOWER(t.tableName)=LOWER(:name) AND t.id<>:exceptId")
  boolean existsNameExcludingId(@Param("name") String name,
      @Param("exceptId") Integer exceptId);

  @Query("SELECT t FROM RestaurantTable t WHERE t.status = :status")
  List<RestaurantTable> findByStatus(@Param("status") TableStatus status);

  /**
   * Find a table by ID with pessimistic write lock for concurrent order
   * creation.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM RestaurantTable t WHERE t.id = :id")
  Optional<RestaurantTable> findByIdWithPessimisticLock(
      @Param("id") Integer id);
}
