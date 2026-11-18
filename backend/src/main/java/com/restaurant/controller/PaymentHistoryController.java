package com.restaurant.controller;

import com.restaurant.entity.enums.PaymentStatus;
import com.restaurant.entity.enums.PaymentMethod;

import com.restaurant.dto.report.ReportModels.*;
import com.restaurant.service.HistoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PaymentHistoryController {
  private final HistoryService monitoring;
  private final ReportFilters filters;

  public PaymentHistoryController(HistoryService monitoring,
      ReportFilters filters) {
    this.monitoring = monitoring;
    this.filters = filters;
  }

  @GetMapping({"/manager/payment-history", "/manager/payment-history/{id}"})
  String history(@PathVariable(required = false) Integer id,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "ALL") String method,
      @RequestParam(defaultValue = "0") int page, Model model) {
    DateRange range = filters.range(from, to);
    PaymentStatus selected = filters.status(status, PaymentStatus.class);
    PaymentMethod selectedMethod = filters.status(method, PaymentMethod.class);
    model.addAttribute("selectedMethod",
        selectedMethod == null ? "ALL" : selectedMethod.name());
    model.addAttribute("range", range);
    model.addAttribute("q", q.trim());
    model.addAttribute("selectedStatus",
        selected == null ? "ALL" : selected.name());
    model.addAttribute("result",
        monitoring.payments(range, q, selected, selectedMethod, page));
    model.addAttribute("counts",
        monitoring.paymentCounts(range, q, selectedMethod));
    if (id != null)
      model.addAttribute("payment", monitoring.payment(id));
    model.addAttribute("section", "history");
    return "reports/payments";
  }
}
