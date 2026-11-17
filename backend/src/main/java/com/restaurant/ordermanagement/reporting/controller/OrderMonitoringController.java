package com.restaurant.ordermanagement.reporting.controller;

import com.restaurant.ordermanagement.reporting.dto.ReportingModels.OrderStatus;
import com.restaurant.ordermanagement.reporting.service.MonitoringService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OrderMonitoringController {
  private final MonitoringService monitoring;
  private final ReportingFilters filters;

  public OrderMonitoringController(MonitoringService monitoring, ReportingFilters filters) {
    this.monitoring = monitoring;
    this.filters = filters;
  }

  @GetMapping("/manager/orders")
  String orders(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    OrderStatus selected = filters.status(status, OrderStatus.class);
    model.addAttribute("result", monitoring.orders(q, selected, page));
    model.addAttribute("orderCounts", monitoring.orderCounts(q));
    model.addAttribute("q", q.trim());
    model.addAttribute("selectedStatus", selected == null ? "ALL" : selected.name());
    model.addAttribute("section", "orders");
    return "reports/orders";
  }

  @GetMapping("/manager/orders/{id}")
  String details(@PathVariable int id, Model model) {
    model.addAttribute("details", monitoring.order(id));
    model.addAttribute("section", "orders");
    return "reports/order-details";
  }
}
