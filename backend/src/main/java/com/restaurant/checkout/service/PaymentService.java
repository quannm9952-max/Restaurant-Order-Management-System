package com.restaurant.checkout.service;

import com.restaurant.checkout.repository.CheckoutRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('CASHIER','MANAGER')")
public class PaymentService {
  private final CheckoutRepository db;

  public PaymentService(CheckoutRepository db) {
    this.db = db;
  }

  private List<Map<String, Object>> lines(long id) {
    var rows = db.lines(id);
    rows.forEach(m -> m.put("status", title(m.get("status"))));
    return rows;
  }

  private String title(Object value) {
    String s = value.toString().toLowerCase(Locale.ROOT);
    return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
  }

  public Map<String, Object> orderDetailForPayment(long id) {
    var order = db.order(id);
    var items = lines(id);
    long active = items.stream()
        .filter(m -> !"Cancelled".equals(m.get("status"))).count();
    long unserved = items.stream()
        .filter(m -> !Set.of("Served", "Cancelled").contains(m.get("status")))
        .count();
    BigDecimal amount = items.stream()
        .filter(m -> !"Cancelled".equals(m.get("status")))
        .map(m -> (BigDecimal) m.get("line_total"))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    order.put("items", items);
    order.put("totalAmount", amount);
    order.put("total_amount", amount);
    order.put("total_items", active);
    order.put("served_items", active - unserved);
    order.put("unserved_items", unserved);
    order.put("unservedCount", unserved);
    order.put("readyToPay", "OPEN".equals(order.get("status")) && active > 0
        && unserved == 0 && amount.signum() > 0);
    return order;
  }

  public List<Map<String, Object>> checkoutOrders(String search) {
    var orders = db.checkoutOrders(search);
    orders.forEach(order -> {
      var amount = (BigDecimal) order.remove("amount");
      long active = ((Number) order.get("total_items")).longValue();
      long unserved = ((Number) order.get("unserved_items")).longValue();
      order.put("totalAmount", amount);
      order.put("total_amount", amount);
      order.put("unservedCount", unserved);
      order.put("readyToPay", "OPEN".equals(order.get("status")) && active > 0
          && unserved == 0 && amount.signum() > 0);
    });
    return orders;
  }

  @Transactional
  @PreAuthorize("hasRole('CASHIER')")
  public long payOrder(long id, String paymentMethod, boolean success) {
    var locked = db.lockOrder(id);
    if (!"OPEN".equals(locked.get("status")))
      throw new IllegalStateException("Only an OPEN order can be paid.");
    if (db.hasSuccessfulPayment(id))
      throw new IllegalStateException("Order already paid.");
    var detail = orderDetailForPayment(id);
    if (((Number) detail.get("total_items")).longValue() == 0)
      throw new IllegalStateException("Order has no active items.");
    if (!Boolean.TRUE.equals(detail.get("readyToPay")))
      throw new IllegalStateException(
          "Every active item must be Served before payment.");
    String method = paymentMethod == null ? "Cash" : paymentMethod.trim();
    if (!Set.of("Cash", "Card", "Bank Transfer").contains(method))
      throw new IllegalArgumentException("Choose Cash, Card or Bank Transfer.");
    var amount = (BigDecimal) detail.get("totalAmount");
    long paymentId = db.insertPayment(id, amount, method, success);
    if (success) {
      if (db.completeOrder(id, amount) != 1) {
        throw new IllegalStateException("Order changed during checkout.");
      }
      db.releaseTable(locked.get("table_id"));
      db.createInvoice(id, amount);
    }
    return paymentId;
  }

  public Map<String, Object> invoiceData(long id) {
    var order = db.completedOrder(id);
    var payment = db.successfulPayment(id);
    var result = new LinkedHashMap<String, Object>();
    result.put("order", order);
    result.put("payment", payment);
    result.put("invoiceNumber", db.invoiceNumber(id));
    result.put("items", lines(id).stream()
        .filter(item -> !"Cancelled".equals(item.get("status"))).toList());
    result.put("total", payment.get("amount"));
    return result;
  }
}
