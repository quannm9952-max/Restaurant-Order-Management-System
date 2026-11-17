package com.restaurant.checkout.controller;

import com.restaurant.checkout.service.PaymentService;
import com.restaurant.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentController {
  private static final Logger LOG = LoggerFactory
      .getLogger(PaymentController.class);

  private final PaymentService service;

  public PaymentController(PaymentService service) {
    this.service = service;
  }

  /** Billing & Payment Management Dashboard */
  @GetMapping("/payments")
  public String paymentDashboard(@RequestParam(required = false) String search,
      @RequestParam(required = false) Long orderId, Model model) {

    model.addAttribute("currentSearch", search != null ? search : "");
    var checkoutOrders = service.checkoutOrders(search);
    model.addAttribute("checkoutOrders", checkoutOrders);
    if (orderId != null)
      model.addAttribute("selectedOrder",
          service.orderDetailForPayment(orderId));
    else if (!checkoutOrders.isEmpty())
      model.addAttribute("selectedOrder", service.orderDetailForPayment(
          ((Number) checkoutOrders.getFirst().get("id")).longValue()));

    return "payments";
  }

  /** JSON API to retrieve order details pre-checkout */
  @GetMapping("/payments/orders/{orderId}/json")
  @ResponseBody
  public ResponseEntity<?> getOrderDetailJson(@PathVariable long orderId) {
    try {
      return ResponseEntity.ok(service.orderDetailForPayment(orderId));
    } catch (NotFoundException ex) {
      return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    } catch (org.springframework.dao.DataAccessException ex) {
      LOG.error("Unable to load record details", ex);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Unable to load details. Please try again."));
    }
  }

  /** Confirm payment with transaction lock and duplicate protection */
  @PostMapping("/payments/{orderId}/confirm")
  public String confirmPayment(@PathVariable long orderId,
      @RequestParam(defaultValue = "Cash") String paymentMethod,
      RedirectAttributes redirectAttrs) {
    try {
      long paymentId = service.payOrder(orderId, paymentMethod, true);
      redirectAttrs.addFlashAttribute("success",
          "Payment successfully recorded for Order #" + orderId + " (Txn #"
              + paymentId + ", method: " + paymentMethod
              + "). Order completed and table released.");
      return "redirect:/invoices/" + orderId;
    } catch (IllegalArgumentException
        | com.restaurant.exceptions.BusinessException | NotFoundException
        | IllegalStateException e) {
      redirectAttrs.addFlashAttribute("error",
          "Payment error for Order #" + orderId + ": " + e.getMessage());
      return "redirect:/payments";
    }
  }

  /** Record payment failure (Order remains Open, Table remains Occupied) */
  @PostMapping("/payments/{orderId}/fail")
  public String recordPaymentFailure(@PathVariable long orderId,
      @RequestParam(defaultValue = "Cash") String paymentMethod,
      RedirectAttributes redirectAttrs) {
    try {
      service.payOrder(orderId, paymentMethod, false);
      redirectAttrs.addFlashAttribute("error",
          "Payment marked as failed for Order #" + orderId
              + ". Order remains in Open status.");
    } catch (IllegalArgumentException
        | com.restaurant.exceptions.BusinessException | NotFoundException
        | IllegalStateException e) {
      redirectAttrs.addFlashAttribute("error", "Error: " + e.getMessage());
    }
    return "redirect:/payments";
  }

  /** View and print invoice */
  @GetMapping("/invoices/{orderId}")
  public String viewInvoice(@PathVariable long orderId, Model model,
      RedirectAttributes redirectAttrs) {
    try {
      Map<String, Object> invoice = service.invoiceData(orderId);
      model.addAttribute("order", invoice.get("order"));
      model.addAttribute("payment", invoice.get("payment"));
      model.addAttribute("items", invoice.get("items"));
      model.addAttribute("total", invoice.get("total"));
      model.addAttribute("invoiceNumber", invoice.get("invoiceNumber"));
      return "invoice";
    } catch (IllegalArgumentException
        | com.restaurant.exceptions.BusinessException | NotFoundException
        | IllegalStateException e) {
      redirectAttrs.addFlashAttribute("error",
          "Cannot view invoice for Order #" + orderId + ": " + e.getMessage());
      return "redirect:/payments";
    }
  }

  /** Payment transactions history audit */
  @GetMapping("/history")
  public String paymentHistory() {
    return "redirect:/manager/payment-history";
  }
}
