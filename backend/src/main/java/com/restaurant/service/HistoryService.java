package com.restaurant.service;

import com.restaurant.entity.enums.PaymentStatus;
import com.restaurant.entity.enums.PaymentMethod;

import com.restaurant.entity.enums.OrderStatus;

import com.restaurant.dto.report.ReportModels.*;
import com.restaurant.repository.OrderReportRepository;
import com.restaurant.repository.PaymentReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('MANAGER')")
public class HistoryService {
  public static final int PAGE_SIZE = 8;
  private final OrderReportRepository orders;
  private final PaymentReportRepository payments;

  public HistoryService(OrderReportRepository orders,
      PaymentReportRepository payments) {
    this.orders = orders;
    this.payments = payments;
  }

  public Page<OrderRow> orders(String query, OrderStatus status, int page) {
    return orders.findOrders(checkQuery(query), status, checkPage(page),
        PAGE_SIZE);
  }

  public OrderDetails order(int id) {
    OrderRow order = orders.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Order not found."));
    return new OrderDetails(order, orders.findDetails(id),
        payments.findByOrder(id));
  }

  public java.util.Map<String, Long> orderCounts(String query) {
    return orders.counts(checkQuery(query));
  }

  public Page<PaymentRow> payments(DateRange range, String query,
      PaymentStatus status, int page) {
    return payments(range, query, status, null, page);
  }

  public Page<PaymentRow> payments(DateRange range, String query,
      PaymentStatus status, PaymentMethod method, int page) {
    return payments.findPayments(range, checkQuery(query), status, method,
        checkPage(page), PAGE_SIZE);
  }

  public PaymentCounts paymentCounts(DateRange range, String query) {
    return paymentCounts(range, query, null);
  }

  public PaymentCounts paymentCounts(DateRange range, String query,
      PaymentMethod method) {
    return payments.counts(range, checkQuery(query), method);
  }

  public PaymentRow payment(int id) {
    return payments.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Payment not found."));
  }

  private String checkQuery(String value) {
    String query = value == null ? "" : value.trim();
    if (query.length() > 100)
      throw new IllegalArgumentException(
          "Search must contain at most 100 characters.");
    return query;
  }

  private int checkPage(int page) {
    if (page < 0)
      throw new IllegalArgumentException(
          "Page number must be zero or greater.");
    return page;
  }
}
