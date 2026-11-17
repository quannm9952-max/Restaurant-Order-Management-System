package com.restaurant.ordermanagement.reporting.service;

import com.restaurant.ordermanagement.reporting.dto.ReportingModels.*;
import com.restaurant.ordermanagement.reporting.repository.OrderReportingRepository;
import com.restaurant.ordermanagement.reporting.repository.PaymentReportingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('MANAGER')")
public class MonitoringService {
  public static final int PAGE_SIZE = 8;
  private final OrderReportingRepository orders;
  private final PaymentReportingRepository payments;

  public MonitoringService(OrderReportingRepository orders,
      PaymentReportingRepository payments) {
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
    return payments.findPayments(range, checkQuery(query), status,
        checkPage(page), PAGE_SIZE);
  }

  public PaymentCounts paymentCounts(DateRange range, String query) {
    return payments.counts(range, checkQuery(query));
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
