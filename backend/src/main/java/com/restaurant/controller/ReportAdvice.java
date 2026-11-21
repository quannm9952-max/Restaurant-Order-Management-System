package com.restaurant.controller;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.core.annotation.Order(0)
@ControllerAdvice(assignableTypes = {ReportController.class,
    OrderMonitoringController.class, PaymentHistoryController.class})
public class ReportAdvice {
  @ModelAttribute
  void common(Model model, Principal principal) {
    String name = principal == null ? "Manager" : principal.getName();
    model.addAttribute("managerName", name);
    model.addAttribute("managerInitials",
        name.substring(0, Math.min(name.length(), 2))
            .toUpperCase(java.util.Locale.ROOT));
  }

  @ExceptionHandler({IllegalArgumentException.class,
      MethodArgumentTypeMismatchException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  String invalid(Exception exception, Model model) {
    model.addAttribute("errorTitle", "Check your filters");
    model.addAttribute("errorMessage",
        exception instanceof IllegalArgumentException
            ? exception.getMessage()
            : "Choose a valid page number or record ID.");
    return "reports/error";
  }

  @ExceptionHandler(ResponseStatusException.class)
  org.springframework.web.servlet.ModelAndView missing(
      ResponseStatusException exception) {
    var view = new org.springframework.web.servlet.ModelAndView(
        "reports/error");
    view.setStatus(exception.getStatusCode());
    view.addObject("errorTitle", "Record not found");
    view.addObject("errorMessage", exception.getReason());
    return view;
  }
}
