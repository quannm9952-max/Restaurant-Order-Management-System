package com.restaurant.ordermanagement.reporting.controller;

import com.restaurant.ordermanagement.reporting.dto.ReportingModels.*;
import com.restaurant.ordermanagement.reporting.service.MonitoringService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PaymentHistoryController {
  private final MonitoringService monitoring;
  private final ReportingFilters filters;

  public PaymentHistoryController(MonitoringService monitoring,
      ReportingFilters filters) {
    this.monitoring = monitoring;
    this.filters = filters;
  }

  @GetMapping({"/manager/payment-history", "/manager/payment-history/{id}"})
  String history(@PathVariable(required = false) Integer id,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page, Model model) {
    DateRange range = filters.range(from, to);
    PaymentStatus selected = filters.status(status, PaymentStatus.class);
    model.addAttribute("range", range);
    model.addAttribute("q", q.trim());
    model.addAttribute("selectedStatus",
        selected == null ? "ALL" : selected.name());
    model.addAttribute("result", monitoring.payments(range, q, selected, page));
    model.addAttribute("counts", monitoring.paymentCounts(range, q));
    if (id != null)
      model.addAttribute("payment", monitoring.payment(id));
    model.addAttribute("section", "history");
    return "payment-history/list";
  }
}
