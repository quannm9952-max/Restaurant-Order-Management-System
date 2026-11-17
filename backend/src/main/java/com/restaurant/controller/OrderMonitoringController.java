package com.restaurant.controller;

import com.restaurant.entity.enums.OrderStatus;

import com.restaurant.service.HistoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OrderMonitoringController {
  private final HistoryService monitoring;
  private final ReportFilters filters;

  public OrderMonitoringController(HistoryService monitoring,
      ReportFilters filters) {
    this.monitoring = monitoring;
    this.filters = filters;
  }

  @GetMapping("/manager/orders")
  String orders(@RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page, Model model) {
    OrderStatus selected = filters.status(status, OrderStatus.class);
    model.addAttribute("result", monitoring.orders(q, selected, page));
    model.addAttribute("orderCounts", monitoring.orderCounts(q));
    model.addAttribute("q", q.trim());
    model.addAttribute("selectedStatus",
        selected == null ? "ALL" : selected.name());
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
