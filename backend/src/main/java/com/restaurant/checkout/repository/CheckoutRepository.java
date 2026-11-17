package com.restaurant.checkout.repository;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** SQL access for checkout; the service owns payment rules and transactions. */
@Repository
public class CheckoutRepository extends JdbcRepositorySupport {
  private static final String ORDER = """
      SELECT o.order_id AS id,o.status,o.created_at,
             t.table_name AS table_code,e.full_name AS waiter_name,o.table_id
      FROM Orders o JOIN Restaurant_tables t ON t.table_id=o.table_id
      JOIN Employee e ON e.employee_id=o.employee_id
      """;
  private static final String PAYMENT = """
      SELECT p.payment_id AS id,p.order_id,p.amount,p.payment_method,
             p.payment_time AS paid_at,p.status,t.table_name AS table_code
      FROM Payment p JOIN Orders o ON o.order_id=p.order_id
      JOIN Restaurant_tables t ON t.table_id=o.table_id
      """;

  public CheckoutRepository(NamedParameterJdbcTemplate jdbc) {
    super(jdbc);
  }

  public Map<String, Object> order(long id) {
    return one(ORDER + " WHERE o.order_id=:id", Map.of("id", id));
  }

  public List<Map<String, Object>> lines(long id) {
    return rows("""
        SELECT order_detail_id AS id,item_name_at_order AS item_name,
               quantity,unit_price_at_order AS unit_price,
               quantity*unit_price_at_order AS line_total,
               preparation_status AS status
        FROM Order_Detail WHERE order_id=:id ORDER BY order_detail_id
        """, Map.of("id", id));
  }

  /** Aggregate all list rows together, regardless of the number of orders. */
  public List<Map<String, Object>> checkoutOrders(String search) {
    var params = new HashMap<String, Object>();
    String filter = "";
    if (search != null && !search.isBlank()) {
      filter = " AND (LOWER(CONCAT('ORD-',o.order_id)) LIKE :q ESCAPE '!' "
          + "OR LOWER(t.table_name) LIKE :q ESCAPE '!')";
      params.put("q", com.restaurant.repository.support.SqlSearch
          .containsLiteral(search.trim().toLowerCase(Locale.ROOT)));
    }
    return rows("""
        SELECT o.order_id AS id,o.status,o.created_at,
               t.table_name AS table_code,e.full_name AS waiter_name,o.table_id,
               COALESCE(d.amount,0) AS amount,
               COALESCE(d.active_items,0) AS total_items,
               COALESCE(d.served_items,0) AS served_items,
               COALESCE(d.unserved_items,0) AS unserved_items
        FROM Orders o JOIN Restaurant_tables t ON t.table_id=o.table_id
        JOIN Employee e ON e.employee_id=o.employee_id
        LEFT JOIN (
          SELECT order_id,
            SUM(quantity*unit_price_at_order) AS amount,
            COUNT(*) AS active_items,
            SUM(CASE WHEN preparation_status='SERVED' THEN 1 ELSE 0 END)
                AS served_items,
            SUM(CASE WHEN preparation_status<>'SERVED' THEN 1 ELSE 0 END)
                AS unserved_items
          FROM Order_Detail WHERE preparation_status<>'CANCELLED'
          GROUP BY order_id
        ) d ON d.order_id=o.order_id WHERE 1=1
        """ + filter + " ORDER BY CASE WHEN o.status='OPEN' THEN 0 ELSE 1 END,"
        + "o.created_at DESC,o.order_id DESC LIMIT 100", params);
  }

  public Map<String, Object> lockOrder(long id) {
    return one("SELECT order_id,status,table_id FROM Orders "
        + "WHERE order_id=:id FOR UPDATE", Map.of("id", id));
  }

  public boolean hasSuccessfulPayment(long id) {
    return count(
        "SELECT COUNT(*) FROM Payment "
            + "WHERE order_id=:id AND status='SUCCESSFUL'",
        Map.of("id", id)) > 0;
  }

  public long insertPayment(long id, BigDecimal amount, String method,
      boolean success) {
    var key = new GeneratedKeyHolder();
    var params = new MapSqlParameterSource().addValue("id", id)
        .addValue("amount", amount).addValue("method", method)
        .addValue("status", success ? "SUCCESSFUL" : "FAILED");
    jdbc.update("INSERT INTO Payment(order_id,amount,payment_method,status,"
        + "payment_time) VALUES(:id,:amount,:method,:status,CURRENT_TIMESTAMP)",
        params, key, new String[]{"payment_id"});
    return Objects.requireNonNull(key.getKey(), "Missing payment ID")
        .longValue();
  }

  public int completeOrder(long id, BigDecimal amount) {
    return update(
        "UPDATE Orders SET status='COMPLETED',total_amount=:amount "
            + "WHERE order_id=:id AND status='OPEN'",
        Map.of("id", id, "amount", amount));
  }

  public void releaseTable(Object tableId) {
    update("UPDATE Restaurant_tables SET status='AVAILABLE' "
        + "WHERE table_id=:id", Map.of("id", tableId));
  }

  public void createInvoice(long id, BigDecimal amount) {
    update(
        "INSERT INTO Invoice(order_id,invoice_number,issued_at,total_amount) "
            + "VALUES(:id,CONCAT('INV-',:id),CURRENT_TIMESTAMP,:amount)",
        Map.of("id", id, "amount", amount));
  }

  public Map<String, Object> completedOrder(long id) {
    return one(ORDER + " WHERE o.order_id=:id AND o.status='COMPLETED'",
        Map.of("id", id));
  }

  public Map<String, Object> successfulPayment(long id) {
    return one(PAYMENT + " WHERE p.order_id=:id AND p.status='SUCCESSFUL'",
        Map.of("id", id));
  }

  public String invoiceNumber(long id) {
    return one("SELECT invoice_number FROM Invoice WHERE order_id=:id",
        Map.of("id", id)).get("invoice_number").toString();
  }
}
