package com.restaurant.ordermanagement.reporting.controller;

import com.restaurant.ordermanagement.reporting.dto.ReportingModels.*;
import com.restaurant.ordermanagement.reporting.service.ReportingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportController {
  private final ReportingService reports;
  private final ReportingFilters filters;

  public ReportController(ReportingService reports, ReportingFilters filters) {
    this.reports = reports;
    this.filters = filters;
  }

  @GetMapping("/manager/reports")
  String revenue(@RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "DAY") String group, Model model) {
    Grouping grouping = filters.status(group, Grouping.class);
    if (grouping == null)
      grouping = Grouping.DAY;
    DateRange range = filters.range(from, to);
    model.addAttribute("range", range);
    model.addAttribute("report", reports.report(range, grouping));
    model.addAttribute("section", "revenue");
    return "reports/revenue";
  }
}
