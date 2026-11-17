package com.restaurant.services.table;

import com.restaurant.entities.RestaurantTable;
import com.restaurant.entities.enums.TableStatus;
import com.restaurant.exceptions.*;
import com.restaurant.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('MANAGER')")
public class TableCatalogService {
  private final TableRepository tables;
  private final OrderRepository orders;

  public TableCatalogService(TableRepository tables, OrderRepository orders) {
    this.tables = tables;
    this.orders = orders;
  }

  @Transactional
  public RestaurantTable save(Integer id, String name, int capacity) {
    if (name == null || name.isBlank() || name.trim().length() > 30
        || capacity < 1 || capacity > 100)
      throw new BusinessException("Enter a table name of at most 30 "
          + "characters and a capacity from 1 to 100.");
    RestaurantTable table = id == null
        ? new RestaurantTable()
        : tables.findByIdWithPessimisticLock(id)
            .orElseThrow(() -> new NotFoundException("Table not found."));
    if (id != null && orders.existsOpenByTableId(id))
      throw new BusinessException("An occupied table cannot be edited until "
          + "its open order is completed or cancelled.");
    String normalized = name.trim();
    if (tables.existsNameExcludingId(normalized, id == null ? -1 : id))
      throw new BusinessException("Table name already exists.");
    table.setTableName(normalized);
    table.setCapacity(capacity);
    if (id == null)
      table.setStatus(TableStatus.AVAILABLE);
    return tables.save(table);
  }

  @Transactional(readOnly = true)
  public RestaurantTable get(Integer id) {
    return tables.findById(id)
        .orElseThrow(() -> new NotFoundException("Table not found."));
  }
}
