package com.restaurant.checkout.controller;

import com.restaurant.checkout.service.PaymentService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentController {

  private final PaymentService service;

  public PaymentController(PaymentService service) {
    this.service = service;
  }

  /** Billing & Payment Management Dashboard */
  @GetMapping("/payments")
  public String paymentDashboard(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long orderId,
      Model model) {

    Map<String, Object> stats = service.paymentStats();
    List<Map<String, Object>> pendingList = service.pendingOrders(search);
    List<Map<String, Object>> recentPayments = service.payments();

    model.addAttribute("stats", stats);
    model.addAttribute("pendingOrders", pendingList);
    model.addAttribute("recentPayments", recentPayments);
    model.addAttribute("currentSearch", search != null ? search : "");
    var checkoutOrders = service.checkoutOrders(search);
    model.addAttribute("checkoutOrders", checkoutOrders);
    if (orderId != null)
      model.addAttribute("selectedOrder", service.orderDetailForPayment(orderId));
    else if (!checkoutOrders.isEmpty())
      model.addAttribute("selectedOrder", checkoutOrders.getFirst());

    return "payments";
  }

  /** JSON API to retrieve order details pre-checkout */
  @GetMapping("/payments/orders/{orderId}/json")
  @ResponseBody
  public ResponseEntity<?> getOrderDetailJson(@PathVariable long orderId) {
    try {
      Map<String, Object> detail = service.orderDetailForPayment(orderId);
      return ResponseEntity.ok(detail);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  /** Confirm payment with transaction lock and duplicate protection */
  @PostMapping("/payments/{orderId}/confirm")
  public String confirmPayment(
      @PathVariable long orderId,
      @RequestParam(defaultValue = "Cash") String paymentMethod,
      RedirectAttributes redirectAttrs) {
    try {
      long paymentId = service.payOrder(orderId, paymentMethod, true);
      redirectAttrs.addFlashAttribute(
          "success",
          "Payment successfully recorded for Order #"
              + orderId
              + " (Txn #"
              + paymentId
              + ", method: "
              + paymentMethod
              + "). Order completed and table released.");
      return "redirect:/invoices/" + orderId;
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute(
          "error", "Payment error for Order #" + orderId + ": " + e.getMessage());
      return "redirect:/payments";
    }
  }

  /** Record payment failure (Order remains Open, Table remains Occupied) */
  @PostMapping("/payments/{orderId}/fail")
  public String recordPaymentFailure(
      @PathVariable long orderId,
      @RequestParam(defaultValue = "Cash") String paymentMethod,
      RedirectAttributes redirectAttrs) {
    try {
      service.payOrder(orderId, paymentMethod, false);
      redirectAttrs.addFlashAttribute(
          "error",
          "Payment marked as failed for Order #" + orderId + ". Order remains in Open status.");
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute("error", "Error: " + e.getMessage());
    }
    return "redirect:/payments";
  }

  /** View and print invoice */
  @GetMapping("/invoices/{orderId}")
  public String viewInvoice(
      @PathVariable long orderId, Model model, RedirectAttributes redirectAttrs) {
    try {
      Map<String, Object> invoice = service.invoiceData(orderId);
      model.addAttribute("order", invoice.get("order"));
      model.addAttribute("payment", invoice.get("payment"));
      model.addAttribute("items", invoice.get("items"));
      model.addAttribute("total", invoice.get("total"));
      model.addAttribute("invoiceNumber", invoice.get("invoiceNumber"));
      return "invoice";
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute(
          "error", "Cannot view invoice for Order #" + orderId + ": " + e.getMessage());
      return "redirect:/payments";
    }
  }

  /** Payment transactions history audit */
  @GetMapping("/history")
  public String paymentHistory() {
    return "redirect:/manager/payment-history";
  }
}
