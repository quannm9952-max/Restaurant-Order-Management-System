package com.restaurant.service;

import com.restaurant.repository.CheckoutRepository;
import com.restaurant.dto.payment.PaymentConfirmation;
import com.restaurant.dto.payment.BillReview;
import com.restaurant.entity.enums.PaymentMethod;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('CASHIER')")
public class PaymentService {
  private final CheckoutRepository checkoutRepository;

  public PaymentService(CheckoutRepository checkoutRepository) {
    this.checkoutRepository = checkoutRepository;
  }

  private List<Map<String, Object>> orderLines(long id) {
    var rows = checkoutRepository.lines(id);
    rows.forEach(m -> m.put("status", statusLabel(m.get("status"))));
    return rows;
  }

  private String statusLabel(Object value) {
    String s = value.toString().toLowerCase(Locale.ROOT);
    return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
  }

  public Map<String, Object> orderDetailForPayment(long id) {
    var order = checkoutRepository.order(id);
    var items = orderLines(id);
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
    boolean ready = "OPEN".equals(order.get("status")) && active > 0
        && unserved == 0 && amount.signum() > 0
        && !checkoutRepository.hasSuccessfulPayment(id);
    order.put("readyToPay", ready);
    order.put("canEditQuantities", ready);
    order.put("billRevision", revision(order, items));
    return order;
  }

  public List<Map<String, Object>> checkoutOrders(String search) {
    var orders = checkoutRepository.checkoutOrders(search);
    orders.forEach(order -> {
      var amount = (BigDecimal) order.remove("amount");
      long active = ((Number) order.get("total_items")).longValue();
      long unserved = ((Number) order.get("unserved_items")).longValue();
      order.put("totalAmount", amount);
      order.put("total_amount", amount);
      order.put("unservedCount", unserved);
      order.put("readyToPay",
          "OPEN".equals(order.get("status")) && active > 0 && unserved == 0
              && amount.signum() > 0
              && ((Number) order.get("paid")).intValue() == 0);
    });
    return orders;
  }

  @Transactional
  public Map<String, Object> reviewBill(long id, BillReview review) {
    payableOrder(id);
    applyReview(id, review);
    return orderDetailForPayment(id);
  }

  private BigDecimal applyReview(long id, BillReview review) {
    var current = orderDetailForPayment(id);
    if (review == null || review.revision() == null
        || review.expectedTotal() == null)
      throw new IllegalArgumentException(
          "Review all item quantities before continuing.");
    if (!review.revision().equals(current.get("billRevision")))
      throw new IllegalStateException(
          "Bill changed. Reload Payment and review the latest order.");
    var quantities = review.lines();
    var active = checkoutRepository.lines(id).stream()
        .filter(line -> !"CANCELLED".equals(line.get("status"))).toList();
    if (quantities == null || quantities.size() != active.size())
      throw new IllegalArgumentException(
          "Order changed. Reload and review all item quantities.");
    var reviewed = new HashMap<Long, Integer>();
    for (var line : quantities) {
      if (line == null || line.detailId() == null || line.quantity() == null
          || line.quantity() < 1
          || reviewed.put(line.detailId(), line.quantity()) != null)
        throw new IllegalArgumentException(
            "Enter a whole quantity of at least 1 for every item.");
    }
    BigDecimal total = BigDecimal.ZERO;
    for (var line : active) {
      long detailId = ((Number) line.get("id")).longValue();
      Integer quantity = reviewed.get(detailId);
      if (quantity == null)
        throw new IllegalArgumentException(
            "Order changed. Reload and review all item quantities.");
      total = total.add(((BigDecimal) line.get("unit_price"))
          .multiply(BigDecimal.valueOf(quantity)));
    }
    if (total.signum() <= 0
        || total.compareTo(new BigDecimal("9999999999.99")) > 0)
      throw new IllegalArgumentException(
          "The bill total must be positive and within the supported amount.");
    if (total.compareTo(review.expectedTotal()) != 0)
      throw new IllegalStateException(
          "Bill total changed. Review the amount before continuing.");
    for (var line : active) {
      long detailId = ((Number) line.get("id")).longValue();
      int quantity = reviewed.get(detailId);
      if (quantity != ((Number) line.get("quantity")).intValue())
        checkoutRepository.updateBillQuantity(id, detailId, quantity);
    }
    checkoutRepository.updateOrderTotal(id, total);
    return total;
  }

  private String revision(Map<String, Object> order,
      List<Map<String, Object>> items) {
    var state = new StringBuilder().append(order.get("status"));
    for (var item : items)
      state.append('|').append(item.get("id")).append(':')
          .append(item.get("quantity")).append(':')
          .append(item.get("unit_price")).append(':')
          .append(item.get("status"));
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(state.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Cannot identify the current bill.", ex);
    }
  }

  @Transactional
  public long confirmPayment(long id, PaymentConfirmation confirmation) {
    return confirmPayment(id, confirmation, null);
  }

  @Transactional
  public long confirmPayment(long id, PaymentConfirmation confirmation,
      BillReview review) {
    if (confirmation == null)
      throw new IllegalArgumentException("Enter payment details.");
    PaymentMethod method = PaymentMethod
        .forPayment(confirmation.paymentMethod());
    if (!confirmation.paymentVerified())
      throw new IllegalArgumentException(
          "Confirm that you have received and verified the payment.");
    var locked = payableOrder(id);
    BigDecimal amount = review == null
        ? (BigDecimal) locked.get("amount")
        : applyReview(id, review);
    if (method == PaymentMethod.CASH) {
      BigDecimal received = confirmation.amountReceived();
      if (received == null || received.signum() < 0
          || received.stripTrailingZeros().scale() > 2
          || received.compareTo(new BigDecimal("9999999999.99")) > 0)
        throw new IllegalArgumentException(
            "Enter a valid Amount Received with at most two decimal places.");
      if (received.compareTo(amount) < 0)
        throw new IllegalArgumentException(
            "Amount Received must cover the total amount.");
    }
    long paymentId = checkoutRepository.insertPayment(id, amount, method.name(),
        true);
    if (checkoutRepository.completeOrder(id, amount) != 1)
      throw new IllegalStateException("Order changed during checkout.");
    checkoutRepository.releaseTable(locked.get("table_id"));
    checkoutRepository.createInvoice(id, amount);
    return paymentId;
  }

  @Transactional
  public long recordFailedPayment(long id, String paymentMethod) {
    PaymentMethod method = PaymentMethod.forPayment(paymentMethod);
    var locked = payableOrder(id);
    return checkoutRepository.insertPayment(id,
        (BigDecimal) locked.get("amount"), method.name(), false);
  }

  @Transactional(readOnly = true,
      isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
  public Map<String, Object> billData(long id, String expectedRevision) {
    var order = orderDetailForPayment(id);
    if (!Boolean.TRUE.equals(order.get("readyToPay")))
      throw new IllegalStateException(
          "Only an unpaid, fully served order can be previewed.");
    if (!Objects.equals(expectedRevision, order.get("billRevision")))
      throw new IllegalStateException(
          "Bill changed. Reload Payment and review the latest order.");
    var result = new LinkedHashMap<String, Object>();
    result.put("order", order);
    result.put("items", orderLines(id).stream()
        .filter(item -> !"Cancelled".equals(item.get("status"))).toList());
    result.put("total", order.get("totalAmount"));
    result.put("invoiceNumber", "BILL-" + id);
    result.put("paid", false);
    return result;
  }

  private Map<String, Object> payableOrder(long id) {
    var locked = checkoutRepository.lockOrder(id);
    if (!"OPEN".equals(locked.get("status")))
      throw new IllegalStateException("Only an OPEN order can be paid.");
    if (checkoutRepository.hasSuccessfulPayment(id))
      throw new IllegalStateException(
          "Order already paid. Check its invoice before retrying.");
    var detail = orderDetailForPayment(id);
    if (((Number) detail.get("total_items")).longValue() == 0)
      throw new IllegalStateException("Order has no active items.");
    if (!Boolean.TRUE.equals(detail.get("readyToPay")))
      throw new IllegalStateException(
          "Every active item must be Served before payment.");
    locked.put("amount", detail.get("totalAmount"));
    return locked;
  }

  public Map<String, Object> invoiceData(long id) {
    var order = checkoutRepository.completedOrder(id);
    var payment = checkoutRepository.successfulPayment(id);
    payment.put("methodLabel",
        PaymentMethod.display((String) payment.get("payment_method")));
    var result = new LinkedHashMap<String, Object>();
    result.put("order", order);
    result.put("payment", payment);
    result.put("invoiceNumber", checkoutRepository.invoiceNumber(id));
    result.put("items", orderLines(id).stream()
        .filter(item -> !"Cancelled".equals(item.get("status"))).toList());
    result.put("total", payment.get("amount"));
    result.put("paid", true);
    return result;
  }
}
