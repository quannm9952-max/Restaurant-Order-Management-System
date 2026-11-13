package restaurant.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import restaurant.service.RestaurantService;

@Controller
public class PaymentController {

    private final RestaurantService service;

    public PaymentController(RestaurantService service) {
        this.service = service;
    }

    /**
     * Màn hình chính UC08 – Manage Payments (Dành cho Cashier & Manager)
     */
    @GetMapping("/payments")
    public String paymentDashboard(
            @RequestParam(required = false) String search,
            @SessionAttribute("user") Map<String, Object> user,
            Model model) {

        Map<String, Object> stats = service.paymentStats();
        List<Map<String, Object>> pendingList = service.pendingOrders(search);
        List<Map<String, Object>> recentPayments = service.payments();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("pendingOrders", pendingList);
        model.addAttribute("recentPayments", recentPayments);
        model.addAttribute("currentSearch", search != null ? search : "");

        return "payments";
    }

    /**
     * API JSON lấy chi tiết đơn hàng trước khi thanh toán
     */
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

    /**
     * Xác nhận thanh toán (Xử lý Transaction và chống duplicate)
     */
    @PostMapping("/payments/{orderId}/confirm")
    public String confirmPayment(
            @PathVariable long orderId,
            @RequestParam(defaultValue = "Cash") String paymentMethod,
            RedirectAttributes redirectAttrs) {
        try {
            long paymentId = service.payOrder(orderId, paymentMethod, true);
            redirectAttrs.addFlashAttribute("success", 
                "Thanh toán thành công cho Đơn #" + orderId + " (Giao dịch #" + paymentId + ", hình thức: " + paymentMethod + ")! Đơn hàng đã hoàn tất và bàn đã được giải phóng.");
            return "redirect:/invoices/" + orderId;
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Lỗi thanh toán Đơn #" + orderId + ": " + e.getMessage());
            return "redirect:/payments";
        }
    }

    /**
     * Ghi nhận giao dịch thanh toán thất bại (giữ đơn Open, bàn Occupied)
     */
    @PostMapping("/payments/{orderId}/fail")
    public String recordPaymentFailure(
            @PathVariable long orderId,
            @RequestParam(defaultValue = "Cash") String paymentMethod,
            RedirectAttributes redirectAttrs) {
        try {
            service.payOrder(orderId, paymentMethod, false);
            redirectAttrs.addFlashAttribute("error", "Đã ghi nhận giao dịch thất bại cho Đơn #" + orderId + ". Đơn hàng vẫn được giữ ở trạng thái Open.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/payments";
    }

    /**
     * Màn hình xem và in hóa đơn (Invoice)
     * Đảm bảo bảo mật: Đơn phải Completed và có Payment Successful.
     */
    @GetMapping("/invoices/{orderId}")
    public String viewInvoice(
            @PathVariable long orderId,
            @SessionAttribute("user") Map<String, Object> user,
            Model model,
            RedirectAttributes redirectAttrs) {
        try {
            Map<String, Object> invoice = service.invoiceData(orderId);
            model.addAttribute("user", user);
            model.addAttribute("order", invoice.get("order"));
            model.addAttribute("payment", invoice.get("payment"));
            model.addAttribute("items", invoice.get("items"));
            model.addAttribute("total", invoice.get("total"));
            return "invoice";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Không thể xem hóa đơn đơn #" + orderId + ": " + e.getMessage());
            return "redirect:/payments";
        }
    }

    /**
     * Lịch sử thanh toán chi tiết (Payment History)
     */
    @GetMapping("/history")
    public String paymentHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @SessionAttribute("user") Map<String, Object> user,
            Model model) {

        List<Map<String, Object>> payments = service.paymentHistory(search, method, status, from, to);

        model.addAttribute("user", user);
        model.addAttribute("payments", payments);
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("currentMethod", method != null ? method : "All");
        model.addAttribute("currentStatus", status != null ? status : "All");
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);

        return "history";
    }
}

