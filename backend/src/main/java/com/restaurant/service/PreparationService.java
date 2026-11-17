package com.restaurant.service;

import com.restaurant.dto.preparation.*;
import com.restaurant.dto.preparation.PreparationQueue.TableOption;
import com.restaurant.entity.enums.CookingStatus;
import com.restaurant.entity.enums.OrderStatus;
import com.restaurant.exception.BusinessException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.repository.OrderDetailRepository;
import com.restaurant.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('CHEF','WAITER')")
public class PreparationService {
  private final OrderRepository orders;
  private final OrderDetailRepository details;
  private final EntityManager entityManager;

  public PreparationService(OrderRepository orders,
      OrderDetailRepository details, EntityManager entityManager) {
    this.orders = orders;
    this.details = details;
    this.entityManager = entityManager;
  }

  @PreAuthorize("hasRole('CHEF')")
  public PreparationQueue kitchen(String status) {
    CookingStatus selected;
    try {
      selected = CookingStatus.valueOf(status);
    } catch (IllegalArgumentException | NullPointerException ex) {
      throw new BusinessException("Choose Pending, Preparing or Ready.");
    }
    if (!Set
        .of(CookingStatus.PENDING, CookingStatus.PREPARING, CookingStatus.READY)
        .contains(selected))
      throw new BusinessException("Choose Pending, Preparing or Ready.");
    var all = openLines();
    var grouped = group(all).stream()
        .filter(o -> o.lines().stream().anyMatch(l -> l.status() == selected))
        .toList();
    return queue(grouped, all, all);
  }

  @PreAuthorize("hasRole('WAITER')")
  public PreparationQueue serving(Integer tableId, String query) {
    if (tableId != null && tableId <= 0)
      throw new BusinessException("Choose a valid table.");
    String search = query == null ? "" : query.trim();
    if (search.length() > 100)
      throw new BusinessException("Search must not exceed 100 characters.");
    String needle = search.toLowerCase(Locale.ROOT);
    var ready = readyLines();
    var filtered = ready.stream()
        .filter(l -> tableId == null || tableId.equals(l.tableId()))
        .filter(l -> (l.itemName() + " " + l.tableName() + " ORD-" + l.orderId()
            + " Line " + l.lineId()).toLowerCase(Locale.ROOT).contains(needle))
        .toList();
    return queue(group(filtered), ready, ready);
  }

  @PreAuthorize("hasRole('CHEF')")
  public PreparationQueue.Update kitchenUpdate() {
    return update(openLines());
  }

  @PreAuthorize("hasRole('WAITER')")
  public PreparationQueue.Update servingUpdate() {
    return update(readyLines());
  }

  @Transactional
  @PreAuthorize("hasRole('CHEF')")
  public PreparationLine startPreparing(Integer orderId, Integer lineId) {
    return transition(orderId, lineId, CookingStatus.PENDING,
        CookingStatus.PREPARING);
  }

  @Transactional
  @PreAuthorize("hasRole('CHEF')")
  public PreparationLine markReady(Integer orderId, Integer lineId) {
    return transition(orderId, lineId, CookingStatus.PREPARING,
        CookingStatus.READY);
  }

  @Transactional
  @PreAuthorize("hasRole('WAITER')")
  public PreparationLine confirmServed(Integer orderId, Integer lineId) {
    return transition(orderId, lineId, CookingStatus.READY,
        CookingStatus.SERVED);
  }

  private PreparationLine transition(Integer orderId, Integer lineId,
      CookingStatus expected, CookingStatus next) {
    // The same Orders lock is used by order editing/cancellation and JDBC
    // checkout.
    var order = orders.lockForUpdate(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found."));
    // Read current DB values even if another service loaded this entity earlier
    // in the transaction.
    entityManager.refresh(order);
    if (order.getStatus() != OrderStatus.OPEN)
      throw new BusinessException(
          "Only items in an Open order can be updated. This order is "
              + order.getStatus() + ".");
    var line = details.findByIdAndOrderId(lineId, orderId)
        .orElseThrow(() -> new NotFoundException(
            "This item line does not belong to the selected order or no longer"
                + " exists."));
    entityManager.refresh(line);
    if (line.getCookingStatus() == CookingStatus.CANCELLED)
      throw new BusinessException("Cancelled items cannot be updated.");
    if (line.getCookingStatus() == CookingStatus.SERVED)
      throw new BusinessException(
          "This item has already been Served and cannot be updated again.");
    if (line.getCookingStatus() != expected)
      throw new BusinessException("The item is " + line.getCookingStatus()
          + ". Refresh the workspace; only " + expected + " → " + next
          + " is allowed.");
    line.setCookingStatus(next);
    details.saveAndFlush(line);
    return new PreparationLine(line.getId(), order.getId(),
        order.getTable().getId(), order.getTable().getTableName(),
        line.getItemNameAtOrder(), line.getQuantity(), line.getCookingStatus(),
        order.getCreatedAt(), order.getNote());
  }

  private List<PreparationLine> openLines() {
    return details.findPreparationLines(OrderStatus.OPEN);
  }

  private List<PreparationLine> readyLines() {
    return openLines().stream().filter(l -> l.status() == CookingStatus.READY)
        .toList();
  }

  private List<PreparationOrder> group(List<PreparationLine> lines) {
    var grouped = new LinkedHashMap<Integer, List<PreparationLine>>();
    lines.forEach(l -> grouped
        .computeIfAbsent(l.orderId(), key -> new ArrayList<>()).add(l));
    return grouped.values().stream().map(rows -> {
      var first = rows.getFirst();
      return new PreparationOrder(first.orderId(), first.tableName(),
          first.createdAt(), first.orderNote(), List.copyOf(rows));
    }).toList();
  }

  private PreparationQueue queue(List<PreparationOrder> groups,
      List<PreparationLine> countsSource, List<PreparationLine> tablesSource) {
    var counts = new LinkedHashMap<String, Long>();
    for (var state : List.of(CookingStatus.PENDING, CookingStatus.PREPARING,
        CookingStatus.READY))
      counts.put(state.name(),
          countsSource.stream().filter(l -> l.status() == state).count());
    var tables = tablesSource.stream()
        .map(l -> new TableOption(l.tableId(), l.tableName())).distinct()
        .sorted(Comparator.comparing(TableOption::name)
            .thenComparing(TableOption::id))
        .toList();
    var displayed = groups.stream().flatMap(o -> o.lines().stream()).toList();
    return new PreparationQueue(
        groups, counts, tables, displayed.size(), (int) displayed.stream()
            .map(PreparationLine::tableId).distinct().count(),
        revision(countsSource));
  }

  private PreparationQueue.Update update(List<PreparationLine> lines) {
    return new PreparationQueue.Update(revision(lines),
        lines.stream().filter(l -> l.status() == CookingStatus.READY).count());
  }

  private String revision(List<PreparationLine> lines) {
    return Integer.toHexString(lines.hashCode());
  }
}
