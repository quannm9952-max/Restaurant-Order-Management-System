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
  private static final String ORDER =
      "SELECT o.order_id AS id,o.status,o.created_at,t.table_name AS table_code,e.full_name AS"
          + " waiter_name,o.table_id FROM Orders o JOIN Restaurant_tables t ON"
          + " t.table_id=o.table_id JOIN Employee e ON e.employee_id=o.employee_id ";
  private static final String PAYMENT =
      "SELECT p.payment_id AS id,p.order_id,p.amount,p.payment_method,p.payment_time AS"
          + " paid_at,p.status,t.table_name AS table_code FROM Payment p JOIN Orders o ON"
          + " o.order_id=p.order_id JOIN Restaurant_tables t ON t.table_id=o.table_id ";

  public PaymentService(CheckoutRepository db) {
    this.db = db;
  }

  private List<Map<String, Object>> lines(long id) {
    var rows =
        db.rows(
            "SELECT order_detail_id AS id,item_name_at_order AS"
                + " item_name,quantity,unit_price_at_order AS"
                + " unit_price,quantity*unit_price_at_order AS line_total,preparation_status AS"
                + " status FROM Order_Detail WHERE order_id=:id ORDER BY order_detail_id",
            Map.of("id", id));
    rows.forEach(m -> m.put("status", title(m.get("status"))));
    return rows;
  }

  private String title(Object value) {
    String s = value.toString().toLowerCase(Locale.ROOT);
    return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
  }

  public Map<String, Object> orderDetailForPayment(long id) {
    var order = db.one(ORDER + "WHERE o.order_id=:id", Map.of("id", id));
    var items = lines(id);
    long active = items.stream().filter(m -> !"Cancelled".equals(m.get("status"))).count();
    long unserved =
        items.stream()
            .filter(m -> !Set.of("Served", "Cancelled").contains(m.get("status")))
            .count();
    BigDecimal amount =
        items.stream()
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
    order.put(
        "readyToPay",
        "OPEN".equals(order.get("status")) && active > 0 && unserved == 0 && amount.signum() > 0);
    return order;
  }

  public List<Map<String, Object>> pendingOrders(String search) {
    var p = new HashMap<String, Object>();
    String sql = ORDER + "WHERE o.status='OPEN'";
    if (search != null && !search.isBlank()) {
      sql += " AND (CAST(o.order_id AS CHAR) LIKE :q OR LOWER(t.table_name) LIKE :q)";
      p.put("q", "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
    }
    return db.rows(sql + " ORDER BY o.created_at,o.order_id", p).stream()
        .map(m -> orderDetailForPayment(((Number) m.get("id")).longValue()))
        .toList();
  }

  public List<Map<String, Object>> payments() {
    var rows =
        db.rows(PAYMENT + "ORDER BY p.payment_time DESC,p.payment_id DESC LIMIT 20", Map.of());
    rows.forEach(m -> m.put("status", title(m.get("status"))));
    return rows;
  }

  public Map<String, Object> paymentStats() {
    var m = new LinkedHashMap<String, Object>();
    m.put("pendingPayments", db.count("SELECT COUNT(*) FROM Orders WHERE status='OPEN'", Map.of()));
    m.put(
        "completedPayments",
        db.count("SELECT COUNT(*) FROM Payment WHERE status='SUCCESSFUL'", Map.of()));
    m.put("totalTransactions", db.count("SELECT COUNT(*) FROM Payment", Map.of()));
    m.put(
        "todayRevenue",
        db.one(
                "SELECT COALESCE(SUM(p.amount),0) AS amount FROM Payment p JOIN Orders o ON"
                    + " o.order_id=p.order_id WHERE p.status='SUCCESSFUL' AND o.status='COMPLETED'"
                    + " AND CAST(p.payment_time AS DATE)=CURRENT_DATE",
                Map.of())
            .get("amount"));
    return m;
  }

  @Transactional
  @PreAuthorize("hasRole('CASHIER')")
  public long payOrder(long id, String paymentMethod, boolean success) {
    var p = new HashMap<String, Object>();
    p.put("id", id);
    var locked =
        db.one("SELECT order_id,status,table_id FROM Orders WHERE order_id=:id FOR UPDATE", p);
    if (!"OPEN".equals(locked.get("status")))
      throw new IllegalStateException("Only an OPEN order can be paid.");
    if (db.count("SELECT COUNT(*) FROM Payment WHERE order_id=:id AND status='SUCCESSFUL'", p) > 0)
      throw new IllegalStateException("Order already paid.");
    var detail = orderDetailForPayment(id);
    if (((Number) detail.get("total_items")).longValue() == 0)
      throw new IllegalStateException("Order has no active items.");
    if (!Boolean.TRUE.equals(detail.get("readyToPay")))
      throw new IllegalStateException("Every active item must be Served before payment.");
    String method = paymentMethod == null ? "Cash" : paymentMethod.trim();
    if (!Set.of("Cash", "Card", "Bank Transfer").contains(method))
      throw new IllegalArgumentException("Choose Cash, Card or Bank Transfer.");
    p.put("method", method);
    p.put("amount", detail.get("totalAmount"));
    p.put("status", success ? "SUCCESSFUL" : "FAILED");
    db.update(
        "INSERT INTO Payment(order_id,amount,payment_method,status,payment_time)"
            + " VALUES(:id,:amount,:method,:status,CURRENT_TIMESTAMP)",
        p);
    if (success) {
      if (db.update(
              "UPDATE Orders SET status='COMPLETED',total_amount=:amount WHERE order_id=:id AND"
                  + " status='OPEN'",
              p)
          != 1) throw new IllegalStateException("Order changed during checkout.");
      p.put("tableId", locked.get("table_id"));
      db.update("UPDATE Restaurant_tables SET status='AVAILABLE' WHERE table_id=:tableId", p);
      db.update(
          "INSERT INTO Invoice(order_id,invoice_number,issued_at,total_amount)"
              + " VALUES(:id,CONCAT('INV-',:id),CURRENT_TIMESTAMP,:amount)",
          p);
    }
    return ((Number)
            db.one("SELECT MAX(payment_id) AS id FROM Payment WHERE order_id=:id", p).get("id"))
        .longValue();
  }

  public Map<String, Object> invoiceData(long id) {
    var order = db.one(ORDER + "WHERE o.order_id=:id AND o.status='COMPLETED'", Map.of("id", id));
    var payment =
        db.one(PAYMENT + "WHERE p.order_id=:id AND p.status='SUCCESSFUL'", Map.of("id", id));
    var invoice = db.one("SELECT invoice_number FROM Invoice WHERE order_id=:id", Map.of("id", id));
    var m = new LinkedHashMap<String, Object>();
    m.put("order", order);
    m.put("payment", payment);
    m.put("invoiceNumber", invoice.get("invoice_number"));
    m.put("items", lines(id).stream().filter(x -> !"Cancelled".equals(x.get("status"))).toList());
    m.put("total", payment.get("amount"));
    return m;
  }
}
