package com.restaurant.controller;

import com.restaurant.dto.report.ReportModels.*;
import com.restaurant.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportController {
  private final ReportService reports;
  private final ReportFilters filters;

  public ReportController(ReportService reports, ReportFilters filters) {
    this.reports = reports;
    this.filters = filters;
  }

  @GetMapping("/manager/reports")
  String revenue(@RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "AUTO") String group, Model model) {
    Grouping grouping = filters.status(group, Grouping.class);
    if (grouping == null)
      grouping = Grouping.AUTO;
    DateRange range = filters.range(from, to);
    model.addAttribute("range", range);
    model.addAttribute("selectedGroup", grouping.name());
    model.addAttribute("report", reports.report(range, grouping));
    model.addAttribute("section", "revenue");
    return "reports/revenue";
  }
}
