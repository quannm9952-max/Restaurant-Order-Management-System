package com.example.restaurantmanagement.reponsitories;

import com.example.restaurantmanagement.entities.RestaurantTable;
import com.example.restaurantmanagement.entities.enums.TableStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TableRepository extends JpaRepository<RestaurantTable, Integer> {

    @Query("SELECT t FROM RestaurantTable t WHERE t.status = :status")
    List<RestaurantTable> findByStatus(@Param("status") TableStatus status);

    /**
     * Find a table by ID with pessimistic write lock for concurrent order creation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RestaurantTable t WHERE t.id = :id")
    Optional<RestaurantTable> findByIdWithPessimisticLock(@Param("id") Integer id);
}
