package restaurant.controller;

import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import restaurant.service.RestaurantService;

@Controller
public class OperationsController {

    private final RestaurantService s;

    public OperationsController(RestaurantService s) {
        this.s = s;
    }

    private String done(RedirectAttributes a, Runnable r) {
        try {
            r.run();
            a.addFlashAttribute("success", "Operation completed successfully.");
        } catch (Exception e) {
            a.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    // ==========================================
    // STAFF ACCOUNTS (MANAGER)
    // ==========================================
    @PostMapping("/employees")
    public String employee(
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            RedirectAttributes a) {
        return done(a, () -> s.addEmployee(fullName, username, password, role));
    }

    @PostMapping("/employees/{id}/update")
    public String employeeUpdate(
            @PathVariable long id,
            @RequestParam String fullName,
            @RequestParam String role,
            @RequestParam(defaultValue = "false") boolean active,
            RedirectAttributes a) {
        return done(a, () -> s.updateEmployee(id, fullName, role, active));
    }

    // ==========================================
    // DINING TABLES (MANAGER & WAITER)
    // ==========================================
    @PostMapping("/tables")
    public String tableAdd(@RequestParam String code, RedirectAttributes a) {
        return done(a, () -> s.addTable(code));
    }

    @PostMapping("/tables/{id}/status")
    public String tableStatus(
            @PathVariable long id,
            @RequestParam String status,
            RedirectAttributes a) {
        return done(a, () -> s.updateTable(id, status));
    }

    // ==========================================
    // ORDERS & FLOOR SERVICE (WAITER & MANAGER)
    // ==========================================
    @PostMapping("/orders")
    public String order(
            @RequestParam long tableId,
            @RequestParam Map<String, String> params,
            @SessionAttribute("user") Map<String, Object> user,
            RedirectAttributes a) {
        return done(a, () -> {
            Map<Long, Integer> items = new HashMap<>();
            params.forEach((k, v) -> {
                if (k.startsWith("item_") && !v.isBlank() && Integer.parseInt(v) > 0) {
                    items.put(Long.parseLong(k.substring(5)), Integer.parseInt(v));
                }
            });
            s.createOrder(tableId, ((Number) user.get("id")).longValue(), items);
        });
    }

    @PostMapping("/orders/{id}/items")
    public String addItemsToOrder(
            @PathVariable long id,
            @RequestParam Map<String, String> params,
            RedirectAttributes a) {
        return done(a, () -> {
            Map<Long, Integer> items = new HashMap<>();
            params.forEach((k, v) -> {
                if (k.startsWith("item_") && !v.isBlank()) {
                    try {
                        int q = Integer.parseInt(v.trim());
                        if (q > 0) {
                            items.put(Long.parseLong(k.substring(5)), q);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            });
            s.addItemsToOpenOrder(id, items);
        });
    }

    @PostMapping("/order-items/{id}/quantity")
    public String updateItemQuantity(
            @PathVariable long id,
            @RequestParam int quantity,
            RedirectAttributes a) {
        return done(a, () -> s.updateOrderItemQuantity(id, quantity));
    }

    @PostMapping("/order-items/{id}/cancel")
    public String cancelItem(
            @PathVariable long id,
            RedirectAttributes a) {
        return done(a, () -> s.cancelOrderItem(id));
    }

    @GetMapping("/orders/{id}/details")
    @ResponseBody
    public Map<String, Object> orderDetailsJson(@PathVariable long id) {
        return s.getOrderDetailsFull(id);
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable long id, RedirectAttributes a) {
        return done(a, () -> s.cancelOrder(id));
    }

    @PostMapping("/serving/{id}")
    public String served(@PathVariable long id, RedirectAttributes a) {
        return done(a, () -> s.updateItemStatus(id, "Served"));
    }

    // ==========================================
    // KITCHEN (CHEF)
    // ==========================================
    @PostMapping("/kitchen/{id}/status")
    public String kitchen(
            @PathVariable long id,
            @RequestParam String status,
            RedirectAttributes a) {
        return done(a, () -> s.updateItemStatus(id, status));
    }

    // ==========================================
    // REVENUE REPORTS & MONITOR (MANAGER)
    // ==========================================
    @GetMapping("/monitor")
    public String monitor() {
        return "redirect:/dashboard#orders";
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String preset,
            @SessionAttribute("user") Map<String, Object> user,
            Model m) {
        LocalDate today = LocalDate.now();
        LocalDate f = today.minusDays(6);
        LocalDate t = today;

        if ("today".equalsIgnoreCase(preset)) {
            f = today;
            t = today;
        } else if ("7days".equalsIgnoreCase(preset)) {
            f = today.minusDays(6);
            t = today;
        } else if ("30days".equalsIgnoreCase(preset)) {
            f = today.minusDays(29);
            t = today;
        } else if ("month".equalsIgnoreCase(preset)) {
            f = today.withDayOfMonth(1);
            t = today;
        } else if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            try {
                f = LocalDate.parse(from.trim());
                t = LocalDate.parse(to.trim());
            } catch (Exception ignored) {}
        }

        Map<String, Object> reportData = s.revenueReportData(f, t);

        m.addAttribute("user", user);
        m.addAttribute("from", f);
        m.addAttribute("to", t);
        m.addAttribute("preset", preset != null ? preset : "");
        m.addAttribute("report", reportData);
        m.addAttribute("revenue", reportData.get("dailyRevenue"));
        return "reports";
    }
}
