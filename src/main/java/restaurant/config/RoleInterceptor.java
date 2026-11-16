package restaurant.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String uri = req.getRequestURI();

        // Allow static resources and login
        if (uri.equals("/login") || uri.equals("/error") || uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")) {
            return true;
        }

        Object userObj = req.getSession().getAttribute("user");
        if (userObj == null) {
            res.sendRedirect("/login");
            return false;
        }

        Map<?, ?> user = (Map<?, ?>) userObj;
        String role = (String) user.get("role");
        String method = req.getMethod();

        // 1. Menu Management (UC09), Staff Management & Reports: ONLY MANAGER
        boolean menuRoutes = uri.equals("/menu") || uri.startsWith("/menu/");
        boolean employeeRoutes = uri.startsWith("/employees");
        boolean reportRoutes = uri.startsWith("/reports");
        if ((menuRoutes || employeeRoutes || reportRoutes) && !"Manager".equals(role)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Manager privileges required.");
            return false;
        }

        // 2. Billing & Payments (UC08): CASHIER AND MANAGER
        boolean paymentRoutes = uri.equals("/payments") || uri.startsWith("/payments/");
        if (paymentRoutes) {
            if (method.equals("POST") && !"Cashier".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Only Cashier can process payments.");
                return false;
            }
            if (!"Cashier".equals(role) && !"Manager".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Cashier or Manager privileges required for billing.");
                return false;
            }
        }

        // 3. Invoices: Only Cashier and Manager can view and print receipts
        if (uri.startsWith("/invoices")) {
            if (!"Cashier".equals(role) && !"Manager".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: You do not have permission to view invoices.");
                return false;
            }
        }

        // 4. Payment History: Cashier and Manager
        if (uri.startsWith("/history")) {
            if (!"Cashier".equals(role) && !"Manager".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: You do not have permission to view payment history.");
                return false;
            }
        }

        // 5. Orders & Line Items (UC05, UC10): Waiter and Manager
        boolean orderRoutes = uri.startsWith("/orders") || uri.startsWith("/order-items");
        if (orderRoutes) {
            if (method.equals("POST") && !"Waiter".equals(role) && !"Manager".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Only Waiter or Manager can create or update orders.");
                return false;
            }
        }

        // 6. Serving station: Waiter and Manager
        if (uri.startsWith("/serving")) {
            if (!"Waiter".equals(role) && !"Manager".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Only Waiter can serve dishes to tables.");
                return false;
            }
        }

        // 7. Kitchen: Chef and Manager
        boolean chefOnly = uri.startsWith("/kitchen");
        if (chefOnly && !"Chef".equals(role) && !"Manager".equals(role)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Kitchen privileges (Chef) required.");
            return false;
        }

        // 8. Dining Tables
        boolean tableRoutes = uri.startsWith("/tables");
        if (uri.equals("/tables") && method.equals("POST") && !"Manager".equals(role)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Only Manager can add new tables.");
            return false;
        }
        if (uri.matches("/tables/\\d+/status") && method.equals("POST") && "Waiter".equals(role) && "OutOfService".equals(req.getParameter("status"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Waiter cannot mark tables as OutOfService.");
            return false;
        }
        if (tableRoutes && !("Waiter".equals(role) || "Manager".equals(role) || "Cashier".equals(role))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Unauthorized access to tables.");
            return false;
        }

        return true;
    }
}
