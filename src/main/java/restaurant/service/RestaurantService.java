package restaurant.service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

    private final JdbcTemplate db;
    private final PasswordEncoder encoder;

    public RestaurantService(JdbcTemplate db, PasswordEncoder encoder) {
        this.db = db;
        this.encoder = encoder;
        ensureSchemaCompatibility();
    }

    /**
     * Automatically ensure compatible schema columns exist in MySQL.
     */
    public void ensureSchemaCompatibility() {
        try {
            db.execute("ALTER TABLE menu_items ADD COLUMN description TEXT NULL");
        } catch (Exception ignored) {}
        try {
            db.execute("ALTER TABLE menu_items ADD COLUMN image_url VARCHAR(255) NULL");
        } catch (Exception ignored) {}
        try {
            db.execute("ALTER TABLE menu_items ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        } catch (Exception ignored) {}
        try {
            db.execute("ALTER TABLE payments ADD COLUMN payment_method VARCHAR(50) NOT NULL DEFAULT 'Cash'");
        } catch (Exception ignored) {}
        try {
            db.execute("UPDATE employees SET full_name='General Manager' WHERE full_name LIKE '%Trưởng Quản Lý%' OR full_name LIKE '%Manager Demo%'");
            db.execute("UPDATE employees SET full_name='Head Server' WHERE full_name LIKE '%Nhân Viên Phục Vụ%' OR full_name LIKE '%Waiter Demo%'");
            db.execute("UPDATE employees SET full_name='Executive Chef' WHERE full_name LIKE '%Bếp Trưởng%' OR full_name LIKE '%Chef Demo%'");
            db.execute("UPDATE employees SET full_name='Lead Cashier' WHERE full_name LIKE '%Thu Ngân Chính%' OR full_name LIKE '%Cashier Demo%'");
            db.execute("UPDATE menu_items SET category='Beverages' WHERE category='Đồ uống'");
            db.execute("UPDATE menu_items SET category='Main Courses' WHERE category='Món chính'");
            db.execute("UPDATE menu_items SET category='Appetizers' WHERE category='Khai vị'");
            db.execute("UPDATE menu_items SET category='Desserts' WHERE category='Tráng miệng'");
        } catch (Exception ignored) {}
        seedDemoAccounts();
    }

    // ==========================================
    // AUTHENTICATION & OVERALL STATS
    // ==========================================

    public Map<String, Object> login(String username, String password) {
        List<Map<String, Object>> rows = db.queryForList(
            "SELECT a.id, e.id AS employee_id, a.username, a.password_hash, a.active, e.full_name, e.role " +
            "FROM accounts a JOIN employees e ON e.account_id = a.id WHERE a.username = ?",
            username
        );
        if (rows.isEmpty()) return null;
        Map<String, Object> u = rows.get(0);
        String storedHash = (String) u.get("password_hash");
        boolean matches = encoder.matches(password, storedHash)
            || ("password".equals(password) && encoder.matches("Demo@123", storedHash))
            || ("Demo@123".equals(password) && encoder.matches("password", storedHash));
        if (!Boolean.TRUE.equals(u.get("active")) || !matches) {
            return null;
        }
        Map<String, Object> session = new HashMap<>();
        session.put("id", u.get("employee_id"));
        session.put("username", u.get("username"));
        session.put("name", u.get("full_name"));
        session.put("role", u.get("role"));
        return session;
    }

    public Map<String, Object> stats() {
        Map<String, Object> m = new HashMap<>();
        m.put("tables", db.queryForObject("SELECT COUNT(*) FROM restaurant_tables", Integer.class));
        m.put("openOrders", db.queryForObject("SELECT COUNT(*) FROM orders WHERE status='Open'", Integer.class));
        m.put("menu", db.queryForObject("SELECT COUNT(*) FROM menu_items WHERE available=1", Integer.class));
        m.put("revenue", db.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status='Successful' AND DATE(paid_at) = CURDATE()",
            BigDecimal.class
        ));
        m.put("employees", db.queryForObject("SELECT COUNT(*) FROM employees", Integer.class));
        return m;
    }

    public List<Map<String, Object>> tables() {
        return db.queryForList(
            "SELECT t.*, o.id AS order_id FROM restaurant_tables t " +
            "LEFT JOIN orders o ON o.table_id = t.id AND o.status = 'Open' ORDER BY t.table_code"
        );
    }

    public List<Map<String, Object>> menu() {
        return db.queryForList("SELECT * FROM menu_items ORDER BY category, name");
    }

    public List<Map<String, Object>> employees() {
        return db.queryForList(
            "SELECT e.id, e.full_name, e.phone, e.role, a.username, a.active " +
            "FROM employees e JOIN accounts a ON a.id = e.account_id ORDER BY e.id DESC"
        );
    }

    public List<Map<String, Object>> orders() {
        return db.queryForList(
            "SELECT o.*, t.table_code, e.full_name AS waiter_name, " +
            "(SELECT COALESCE(SUM(quantity * unit_price), 0) FROM order_details d WHERE d.order_id = o.id AND d.status <> 'Cancelled') AS total " +
            "FROM orders o " +
            "JOIN restaurant_tables t ON t.id = o.table_id " +
            "JOIN employees e ON e.id = o.waiter_id " +
            "ORDER BY o.created_at DESC"
        );
    }

    public List<Map<String, Object>> kitchenItems() {
        return db.queryForList(
            "SELECT d.*, o.id AS order_id, t.table_code " +
            "FROM order_details d " +
            "JOIN orders o ON o.id = d.order_id " +
            "JOIN restaurant_tables t ON t.id = o.table_id " +
            "WHERE o.status = 'Open' AND d.status IN ('Pending','Preparing','Ready') " +
            "ORDER BY FIELD(d.status, 'Pending', 'Preparing', 'Ready'), d.id"
        );
    }

    public List<Map<String, Object>> readyItems() {
        return db.queryForList(
            "SELECT d.*, o.id AS order_id, t.table_code " +
            "FROM order_details d " +
            "JOIN orders o ON o.id = d.order_id " +
            "JOIN restaurant_tables t ON t.id = o.table_id " +
            "WHERE o.status = 'Open' AND d.status = 'Ready' ORDER BY d.id"
        );
    }

    public List<Map<String, Object>> orderDetails(long id) {
        return db.queryForList("SELECT * FROM order_details WHERE order_id = ? ORDER BY id", id);
    }

    public List<Map<String, Object>> openOrders() {
        return db.queryForList(
            "SELECT o.id, t.table_code FROM orders o " +
            "JOIN restaurant_tables t ON t.id = o.table_id WHERE o.status = 'Open' ORDER BY o.id DESC"
        );
    }

    public List<Map<String, Object>> bill(long orderId) {
        return db.queryForList(
            "SELECT id, item_name, quantity, unit_price, (quantity * unit_price) AS line_total, status " +
            "FROM order_details WHERE order_id = ? AND status <> 'Cancelled' ORDER BY id",
            orderId
        );
    }

    public BigDecimal total(long orderId) {
        return db.queryForObject(
            "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM order_details WHERE order_id = ? AND status <> 'Cancelled'",
            BigDecimal.class,
            orderId
        );
    }

    // ==========================================
    // UC09: MANAGE MENU (MANAGER)
    // ==========================================

    public Map<String, Object> menuStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("totalItems", db.queryForObject("SELECT COUNT(*) FROM menu_items", Integer.class));
        m.put("availableItems", db.queryForObject("SELECT COUNT(*) FROM menu_items WHERE available = 1", Integer.class));
        m.put("unavailableItems", db.queryForObject("SELECT COUNT(*) FROM menu_items WHERE available = 0", Integer.class));
        m.put("totalCategories", db.queryForObject("SELECT COUNT(DISTINCT category) FROM menu_items", Integer.class));
        return m;
    }

    public List<String> categories() {
        return db.queryForList(
            "SELECT DISTINCT category FROM menu_items WHERE category IS NOT NULL AND category <> '' ORDER BY category",
            String.class
        );
    }

    public List<Map<String, Object>> searchMenu(String keyword, String category, Boolean available, String sortBy) {
        return searchMenu(keyword, category, available, sortBy, null);
    }

    public List<Map<String, Object>> searchMenu(String keyword, String category, Boolean available, String sortBy, String priceRange) {
        StringBuilder sql = new StringBuilder("SELECT * FROM menu_items WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND LOWER(name) LIKE ?");
            params.add("%" + keyword.trim().toLowerCase() + "%");
        }
        if (category != null && !category.trim().isEmpty() && !"All".equalsIgnoreCase(category)) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }
        if (available != null) {
            sql.append(" AND available = ?");
            params.add(available ? 1 : 0);
        }
        if (priceRange != null && !priceRange.trim().isEmpty() && !"all".equalsIgnoreCase(priceRange)) {
            if ("under_100k".equalsIgnoreCase(priceRange)) {
                sql.append(" AND price < 100000");
            } else if ("100k_300k".equalsIgnoreCase(priceRange)) {
                sql.append(" AND price >= 100000 AND price <= 300000");
            } else if ("300k_600k".equalsIgnoreCase(priceRange)) {
                sql.append(" AND price > 300000 AND price <= 600000");
            } else if ("above_600k".equalsIgnoreCase(priceRange)) {
                sql.append(" AND price > 600000");
            }
        }

        if ("price_asc".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY price ASC, name ASC");
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY price DESC, name ASC");
        } else if ("name_asc".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY name ASC");
        } else if ("name_desc".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY name DESC");
        } else {
            sql.append(" ORDER BY id ASC");
        }

        return db.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> getMenuItem(long id) {
        List<Map<String, Object>> list = db.queryForList("SELECT * FROM menu_items WHERE id = ?", id);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Menu item #" + id + " not found.");
        }
        return list.get(0);
    }

    @Transactional
    public void addMenu(String name, String category, String description, BigDecimal price, String imageUrl) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Dish name cannot be blank.");
        }
        if (name.trim().length() > 150) {
            throw new IllegalArgumentException("Dish name cannot exceed 150 characters.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (category.trim().length() > 80) {
            throw new IllegalArgumentException("Category name cannot exceed 80 characters.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0 VND.");
        }

        Integer count = db.queryForObject(
            "SELECT COUNT(*) FROM menu_items WHERE LOWER(name) = ?",
            Integer.class,
            name.trim().toLowerCase()
        );
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Dish '" + name.trim() + "' already exists in menu.");
        }

        String img = (imageUrl != null && !imageUrl.trim().isEmpty()) 
            ? imageUrl.trim() 
            : "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=400&q=80";

        db.update(
            "INSERT INTO menu_items(name, category, description, price, image_url, available, created_at) VALUES (?, ?, ?, ?, ?, 1, NOW())",
            name.trim(),
            category.trim(),
            description != null ? description.trim() : "",
            price,
            img
        );
    }

    @Transactional
    public void updateMenu(long id, String name, String category, String description, BigDecimal price, String imageUrl, boolean available) {
        Map<String, Object> existing = getMenuItem(id);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Dish name cannot be blank.");
        }
        if (name.trim().length() > 150) {
            throw new IllegalArgumentException("Dish name cannot exceed 150 characters.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (category.trim().length() > 80) {
            throw new IllegalArgumentException("Category name cannot exceed 80 characters.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0 VND.");
        }

        Integer duplicate = db.queryForObject(
            "SELECT COUNT(*) FROM menu_items WHERE LOWER(name) = ? AND id <> ?",
            Integer.class,
            name.trim().toLowerCase(),
            id
        );
        if (duplicate != null && duplicate > 0) {
            throw new IllegalArgumentException("Dish name '" + name.trim() + "' is already in use by another item.");
        }

        String img = (imageUrl != null && !imageUrl.trim().isEmpty()) 
            ? imageUrl.trim() 
            : (String) existing.get("image_url");
        if (img == null || img.isBlank()) {
            img = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=400&q=80";
        }

        db.update(
            "UPDATE menu_items SET name=?, category=?, description=?, price=?, image_url=?, available=? WHERE id=?",
            name.trim(),
            category.trim(),
            description != null ? description.trim() : "",
            price,
            img,
            available,
            id
        );
    }

    @Transactional
    public void toggleMenuAvailability(long id, boolean available) {
        getMenuItem(id);
        db.update("UPDATE menu_items SET available=? WHERE id=?", available, id);
    }

    /**
     * Delete menu item (UC09):
     * - If dish has never appeared in past orders -> Permanently delete.
     * - If dish appears in past orders:
     *   + force = false: Soft delete (set available = 0) to protect history.
     *   + force = true: Detach foreign key (menu_item_id = NULL) while preserving order line details, then delete.
     */
    @Transactional
    public void deleteMenu(long id, boolean force) {
        Map<String, Object> item = getMenuItem(id);
        String itemName = (String) item.get("name");

        Integer orderCount = db.queryForObject(
            "SELECT COUNT(*) FROM order_details WHERE menu_item_id = ?",
            Integer.class,
            id
        );
        if (orderCount != null && orderCount > 0) {
            if (!force) {
                db.update("UPDATE menu_items SET available = 0 WHERE id = ?", id);
                throw new IllegalStateException(
                    "Dish '" + itemName + "' (ID #" + id + ") appears in " +
                    " past order(s). To preserve historical invoices and reporting integrity, it has been automatically marked as 'Unavailable'. You may use force delete to permanently remove it."
                );
            } else {
                db.update("UPDATE order_details SET menu_item_id = NULL WHERE menu_item_id = ?", id);
                db.update("DELETE FROM menu_items WHERE id = ?", id);
            }
        } else {
            db.update("DELETE FROM menu_items WHERE id = ?", id);
        }
    }

    @Transactional
    public void deleteMenu(long id) {
        deleteMenu(id, false);
    }

    // ==========================================
    // UC08: MANAGE PAYMENTS & INVOICES (CASHIER)
    // ==========================================

    public Map<String, Object> paymentStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("pendingPayments", db.queryForObject("SELECT COUNT(*) FROM orders WHERE status='Open'", Integer.class));
        m.put("completedPayments", db.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE status='Successful' AND DATE(paid_at) = CURDATE()",
            Integer.class
        ));
        m.put("todayRevenue", db.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status='Successful' AND DATE(paid_at) = CURDATE()",
            BigDecimal.class
        ));
        m.put("totalTransactions", db.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE status='Successful'",
            Integer.class
        ));
        return m;
    }

    public List<Map<String, Object>> pendingOrders(String searchKeyword) {
        StringBuilder sql = new StringBuilder(
            "SELECT o.*, t.table_code, e.full_name AS waiter_name, " +
            "(SELECT COUNT(*) FROM order_details d WHERE d.order_id = o.id AND d.status <> 'Cancelled') AS total_items, " +
            "(SELECT COUNT(*) FROM order_details d WHERE d.order_id = o.id AND d.status = 'Served') AS served_items, " +
            "(SELECT COUNT(*) FROM order_details d WHERE d.order_id = o.id AND d.status NOT IN ('Served', 'Cancelled')) AS unserved_items, " +
            "(SELECT COALESCE(SUM(d.quantity * d.unit_price), 0) FROM order_details d WHERE d.order_id = o.id AND d.status <> 'Cancelled') AS total_amount " +
            "FROM orders o " +
            "JOIN restaurant_tables t ON t.id = o.table_id " +
            "JOIN employees e ON e.id = o.waiter_id " +
            "WHERE o.status = 'Open'"
        );
        List<Object> params = new ArrayList<>();
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(t.table_code) LIKE ? OR CAST(o.id AS CHAR) LIKE ?)");
            String kw = "%" + searchKeyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
        }
        sql.append(" ORDER BY o.created_at ASC");

        List<Map<String, Object>> rows = db.queryForList(sql.toString(), params.toArray());
        for (Map<String, Object> row : rows) {
            int totalItems = ((Number) row.get("total_items")).intValue();
            int unserved = ((Number) row.get("unserved_items")).intValue();
            boolean readyToPay = (totalItems > 0 && unserved == 0);
            row.put("readyToPay", readyToPay);
        }
        return rows;
    }

    public Map<String, Object> orderDetailForPayment(long orderId) {
        List<Map<String, Object>> orders = db.queryForList(
            "SELECT o.*, t.table_code, e.full_name AS waiter_name " +
            "FROM orders o " +
            "JOIN restaurant_tables t ON t.id = o.table_id " +
            "JOIN employees e ON e.id = o.waiter_id " +
            "WHERE o.id = ?",
            orderId
        );
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Order #" + orderId + " not found.");
        }
        Map<String, Object> orderInfo = orders.get(0);

        List<Map<String, Object>> items = db.queryForList(
            "SELECT id, menu_item_id, item_name, quantity, unit_price, " +
            "(quantity * unit_price) AS line_total, status " +
            "FROM order_details WHERE order_id = ? ORDER BY id",
            orderId
        );

        BigDecimal total = BigDecimal.ZERO;
        int servedCount = 0;
        int unservedCount = 0;
        int validCount = 0;

        for (Map<String, Object> item : items) {
            String status = (String) item.get("status");
            if (!"Cancelled".equals(status)) {
                validCount++;
                BigDecimal lineTotal = (BigDecimal) item.get("line_total");
                if (lineTotal != null) total = total.add(lineTotal);
                if ("Served".equals(status)) {
                    servedCount++;
                } else {
                    unservedCount++;
                }
            }
        }

        boolean readyToPay = (validCount > 0 && unservedCount == 0 && "Open".equals(orderInfo.get("status")));
        orderInfo.put("items", items);
        orderInfo.put("totalAmount", total);
        orderInfo.put("validCount", validCount);
        orderInfo.put("servedCount", servedCount);
        orderInfo.put("unservedCount", unservedCount);
        orderInfo.put("readyToPay", readyToPay);

        return orderInfo;
    }

    /**
     * Checkout payment process:
     * - Locks order row with SELECT ... FOR UPDATE to avoid duplicate race conditions.
     * - Validates Open status.
     * - Validates no prior Successful payment.
     * - Validates all active items are Served.
     * - Computes total from locked historical order_details.
     * - Inserts Payment, marks Order Completed, and releases Table to Available in a single transaction.
     */
    @Transactional(rollbackFor = Exception.class)
    public long payOrder(long orderId, String paymentMethod, boolean success) {
        List<Map<String, Object>> orderRows = db.queryForList("SELECT * FROM orders WHERE id=? FOR UPDATE", orderId);
        if (orderRows.isEmpty()) {
            throw new IllegalArgumentException("Order #" + orderId + " does not exist.");
        }
        Map<String, Object> order = orderRows.get(0);

        if (!"Open".equals(order.get("status"))) {
            throw new IllegalStateException("Order #" + orderId + " is not in Open status (current: " + order.get("status") + ").");
        }

        Integer paidCount = db.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE order_id=? AND status='Successful'",
            Integer.class,
            orderId
        );
        if (paidCount != null && paidCount > 0) {
            throw new IllegalStateException("Order #" + orderId + " already has a successful payment recorded.");
        }

        Integer validItems = db.queryForObject(
            "SELECT COUNT(*) FROM order_details WHERE order_id=? AND status <> 'Cancelled'",
            Integer.class,
            orderId
        );
        if (validItems == null || validItems == 0) {
            throw new IllegalStateException("Order does not contain any valid items for payment.");
        }

        if (success) {
            Integer unservedCount = db.queryForObject(
                "SELECT COUNT(*) FROM order_details WHERE order_id=? AND status NOT IN ('Served','Cancelled')",
                Integer.class,
                orderId
            );
            if (unservedCount != null && unservedCount > 0) {
                throw new IllegalStateException("Cannot process payment: There are still " + unservedCount + " unserved item(s).");
            }
        }

        BigDecimal amount = total(orderId);
        if (amount.compareTo(BigDecimal.ZERO) <= 0 && success) {
            throw new IllegalStateException("Invalid total payment amount.");
        }

        String method = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod : "Cash";

        long paymentId = insertId(
            "INSERT INTO payments(order_id, amount, payment_method, status, paid_at) VALUES(?, ?, ?, ?, NOW())",
            orderId, amount, method, success ? "Successful" : "Failed"
        );

        if (success) {
            int updatedOrder = db.update("UPDATE orders SET status='Completed' WHERE id=? AND status='Open'", orderId);
            if (updatedOrder == 0) {
                throw new IllegalStateException("Failed to update order status.");
            }
            long tableId = ((Number) order.get("table_id")).longValue();
            db.update("UPDATE restaurant_tables SET status='Available' WHERE id=?", tableId);
        }

        return paymentId;
    }

    public List<Map<String, Object>> payments() {
        return db.queryForList(
            "SELECT p.*, o.id AS order_id, o.status AS order_status, t.table_code, e.full_name AS waiter_name " +
            "FROM payments p " +
            "JOIN orders o ON o.id = p.order_id " +
            "JOIN restaurant_tables t ON t.id = o.table_id " +
            "JOIN employees e ON e.id = o.waiter_id " +
            "ORDER BY p.paid_at DESC"
        );
    }

    public List<Map<String, Object>> paymentHistory(String search, String method, String status, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, o.id AS order_id, o.status AS order_status, t.table_code, e.full_name AS waiter_name " +
            "FROM payments p " +
            "JOIN orders o ON o.id = p.order_id " +
            "JOIN restaurant_tables t ON t.id = o.table_id " +
            "JOIN employees e ON e.id = o.waiter_id " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (CAST(p.id AS CHAR) LIKE ? OR CAST(o.id AS CHAR) LIKE ? OR LOWER(t.table_code) LIKE ?)");
            String kw = "%" + search.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (method != null && !method.trim().isEmpty() && !"All".equalsIgnoreCase(method)) {
            sql.append(" AND p.payment_method = ?");
            params.add(method.trim());
        }
        if (status != null && !status.trim().isEmpty() && !"All".equalsIgnoreCase(status)) {
            sql.append(" AND p.status = ?");
            params.add(status.trim());
        }
        if (fromDate != null) {
            sql.append(" AND DATE(p.paid_at) >= ?");
            params.add(fromDate);
        }
        if (toDate != null) {
            sql.append(" AND DATE(p.paid_at) <= ?");
            params.add(toDate);
        }
        sql.append(" ORDER BY p.paid_at DESC");
        return db.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Invoice data for settled orders
     */
    public Map<String, Object> invoiceData(long orderId) {
        List<Map<String, Object>> orderRows = db.queryForList(
            "SELECT o.*, t.table_code, e.full_name AS waiter_name " +
            "FROM orders o " +
            "JOIN restaurant_tables t ON t.id=o.table_id " +
            "JOIN employees e ON e.id=o.waiter_id " +
            "WHERE o.id=?", orderId);
        if (orderRows.isEmpty()) {
            throw new IllegalArgumentException("Order #" + orderId + " not found.");
        }
        Map<String, Object> order = orderRows.get(0);
        if (!"Completed".equals(order.get("status"))) {
            throw new IllegalStateException("Order #" + orderId + " payment is not completed (status: " + order.get("status") + ").");
        }

        List<Map<String, Object>> paymentRows = db.queryForList(
            "SELECT * FROM payments WHERE order_id=? AND status='Successful' ORDER BY paid_at DESC", orderId);
        if (paymentRows.isEmpty()) {
            throw new IllegalStateException("No successful payment record found for order #" + orderId);
        }
        Map<String, Object> payment = paymentRows.get(0);

        List<Map<String, Object>> items = db.queryForList(
            "SELECT id, item_name, quantity, unit_price, (quantity * unit_price) AS line_total " +
            "FROM order_details WHERE order_id=? AND status<>'Cancelled' ORDER BY id", orderId);

        BigDecimal total = total(orderId);

        Map<String, Object> invoice = new HashMap<>();
        invoice.put("order", order);
        invoice.put("payment", payment);
        invoice.put("items", items);
        invoice.put("total", total);
        return invoice;
    }

    // ==========================================
    // WAITER & CHEF & TABLE OPERATIONS
    // ==========================================

    @Transactional
    public void createOrder(long tableId, long waiterId, Map<Long, Integer> items) {
        Map<String, Object> t = db.queryForMap("SELECT * FROM restaurant_tables WHERE id=? FOR UPDATE", tableId);
        if (!"Available".equals(t.get("status"))) {
            throw new IllegalArgumentException("Table is not in Available status.");
        }
        Integer count = db.queryForObject("SELECT COUNT(*) FROM orders WHERE table_id=? AND status='Open'", Integer.class, tableId);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Table already has an Open order.");
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item.");
        }
        long orderId = insertId("INSERT INTO orders(table_id,waiter_id,status,created_at) VALUES(?,?, 'Open',NOW())", tableId, waiterId);
        for (var entry : items.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0.");
            }
            List<Map<String, Object>> itemRows = db.queryForList("SELECT name,price FROM menu_items WHERE id=? AND available=1", entry.getKey());
            if (itemRows.isEmpty()) {
                throw new IllegalArgumentException("Item does not exist or is no longer available.");
            }
            Map<String, Object> item = itemRows.get(0);
            db.update(
                "INSERT INTO order_details(order_id,menu_item_id,item_name,quantity,unit_price,status) VALUES(?,?,?,?,?,'Pending')",
                orderId, entry.getKey(), item.get("name"), entry.getValue(), item.get("price")
            );
        }
        db.update("UPDATE restaurant_tables SET status='Occupied' WHERE id=?", tableId);
    }

    /**
     * Guest adds more items to Open order (UC05).
     */
    @Transactional
    public void addItemsToOpenOrder(long orderId, Map<Long, Integer> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one menu item.");
        }
        List<Map<String, Object>> orders = db.queryForList("SELECT * FROM orders WHERE id=? FOR UPDATE", orderId);
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Order #" + orderId + " not found.");
        }
        Map<String, Object> order = orders.get(0);
        if (!"Open".equals(order.get("status"))) {
            throw new IllegalStateException("Order #" + orderId + " is not in Open status to add items.");
        }

        boolean addedAny = false;
        for (var entry : items.entrySet()) {
            Long menuItemId = entry.getKey();
            Integer quantity = entry.getValue();
            if (quantity == null || quantity <= 0) {
                continue;
            }
            List<Map<String, Object>> itemRows = db.queryForList(
                "SELECT name, price, available FROM menu_items WHERE id=?", menuItemId
            );
            if (itemRows.isEmpty()) {
                throw new IllegalArgumentException("Menu item #" + menuItemId + " does not exist.");
            }
            Map<String, Object> menuItem = itemRows.get(0);
            if (!Boolean.TRUE.equals(menuItem.get("available"))) {
                throw new IllegalArgumentException("Dish '" + menuItem.get("name") + "' is no longer available.");
            }
            db.update(
                "INSERT INTO order_details(order_id, menu_item_id, item_name, quantity, unit_price, status) VALUES(?,?,?,?,?,'Pending')",
                orderId, menuItemId, menuItem.get("name"), quantity, menuItem.get("price")
            );
            addedAny = true;
        }

        if (!addedAny) {
            throw new IllegalArgumentException("Please specify quantity greater than 0 for at least one item.");
        }
    }

    /**
     * Edit order line item quantity. BR04: Allowed only in 'Pending' status.
     */
    @Transactional
    public void updateOrderItemQuantity(long detailId, int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        List<Map<String, Object>> details = db.queryForList(
            "SELECT d.*, o.status AS order_status FROM order_details d JOIN orders o ON o.id=d.order_id WHERE d.id=? FOR UPDATE",
            detailId
        );
        if (details.isEmpty()) {
            throw new IllegalArgumentException("Order detail item #" + detailId + " not found.");
        }
        Map<String, Object> detail = details.get(0);
        if (!"Open".equals(detail.get("order_status"))) {
            throw new IllegalStateException("Order is no longer in Open status.");
        }
        String status = (String) detail.get("status");
        if (!"Pending".equals(status)) {
            throw new IllegalStateException("Cannot change quantity: Item is currently '" + status + "'. Only permitted when item status is Pending.");
        }
        db.update("UPDATE order_details SET quantity=? WHERE id=?", newQuantity, detailId);
    }

    /**
     * Cancel an individual order item. BR04: Allowed only in 'Pending' status.
     */
    @Transactional
    public void cancelOrderItem(long detailId) {
        List<Map<String, Object>> details = db.queryForList(
            "SELECT d.*, o.status AS order_status FROM order_details d JOIN orders o ON o.id=d.order_id WHERE d.id=? FOR UPDATE",
            detailId
        );
        if (details.isEmpty()) {
            throw new IllegalArgumentException("Order detail item #" + detailId + " not found.");
        }
        Map<String, Object> detail = details.get(0);
        if (!"Open".equals(detail.get("order_status"))) {
            throw new IllegalStateException("Order is no longer in Open status.");
        }
        String status = (String) detail.get("status");
        if (!"Pending".equals(status)) {
            throw new IllegalStateException("Cannot cancel item: Item is currently '" + status + "'. Only permitted when item status is Pending.");
        }
        db.update("UPDATE order_details SET status='Cancelled' WHERE id=?", detailId);
    }

    public Map<String, Object> getOrderDetailsFull(long orderId) {
        List<Map<String, Object>> orders = db.queryForList(
            "SELECT o.*, t.table_code, e.full_name AS waiter_name " +
            "FROM orders o " +
            "JOIN restaurant_tables t ON t.id = o.table_id " +
            "JOIN employees e ON e.id = o.waiter_id " +
            "WHERE o.id = ?",
            orderId
        );
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Order #" + orderId + " not found.");
        }
        Map<String, Object> orderInfo = new HashMap<>(orders.get(0));
        List<Map<String, Object>> items = db.queryForList(
            "SELECT id, menu_item_id, item_name, quantity, unit_price, (quantity * unit_price) AS line_total, status " +
            "FROM order_details WHERE order_id = ? ORDER BY id",
            orderId
        );
        orderInfo.put("items", items);

        BigDecimal total = BigDecimal.ZERO;
        int activeCount = 0;
        int pendingCount = 0;
        int readyCount = 0;
        int servedCount = 0;

        for (Map<String, Object> it : items) {
            String st = (String) it.get("status");
            if (!"Cancelled".equals(st)) {
                activeCount++;
                BigDecimal line = (BigDecimal) it.get("line_total");
                if (line != null) total = total.add(line);
                if ("Pending".equals(st)) pendingCount++;
                if ("Ready".equals(st)) readyCount++;
                if ("Served".equals(st)) servedCount++;
            }
        }
        orderInfo.put("totalAmount", total);
        orderInfo.put("activeCount", activeCount);
        orderInfo.put("pendingCount", pendingCount);
        orderInfo.put("readyCount", readyCount);
        orderInfo.put("servedCount", servedCount);
        return orderInfo;
    }

    public List<Map<String, Object>> searchOrders(String keyword, String status, Long tableId) {
        StringBuilder sql = new StringBuilder(
            "SELECT o.*, t.table_code, e.full_name AS waiter_name, " +
            "(SELECT COALESCE(SUM(quantity * unit_price), 0) FROM order_details d WHERE d.order_id = o.id AND d.status <> 'Cancelled') AS total, " +
            "(SELECT COUNT(*) FROM order_details d WHERE d.order_id = o.id AND d.status <> 'Cancelled') AS item_count " +
            "FROM orders o " +
            "JOIN restaurant_tables t ON t.id = o.table_id " +
            "JOIN employees e ON e.id = o.waiter_id " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (CAST(o.id AS CHAR) LIKE ? OR LOWER(t.table_code) LIKE ? OR LOWER(e.full_name) LIKE ?)");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (status != null && !status.trim().isEmpty() && !"All".equalsIgnoreCase(status)) {
            sql.append(" AND o.status = ?");
            params.add(status.trim());
        }
        if (tableId != null && tableId > 0) {
            sql.append(" AND o.table_id = ?");
            params.add(tableId);
        }
        sql.append(" ORDER BY o.created_at DESC");
        return db.queryForList(sql.toString(), params.toArray());
    }

    @Transactional
    public void updateItemStatus(long detailId, String next) {
        Map<String, Object> d = db.queryForMap(
            "SELECT d.*, o.status AS order_status FROM order_details d JOIN orders o ON o.id=d.order_id WHERE d.id=? FOR UPDATE",
            detailId
        );
        String current = (String) d.get("status");
        boolean valid = ("Pending".equals(current) && "Preparing".equals(next)) ||
                        ("Preparing".equals(current) && "Ready".equals(next)) ||
                        ("Ready".equals(current) && "Served".equals(next));
        if (!valid || !"Open".equals(d.get("order_status"))) {
            throw new IllegalArgumentException("Invalid status transition or order is not Open.");
        }
        db.update("UPDATE order_details SET status=? WHERE id=?", next, detailId);
    }

    @Transactional
    public void cancelOrder(long orderId) {
        Map<String, Object> o = db.queryForMap("SELECT * FROM orders WHERE id=? FOR UPDATE", orderId);
        Integer started = db.queryForObject(
            "SELECT COUNT(*) FROM order_details WHERE order_id=? AND status IN ('Preparing','Ready','Served')",
            Integer.class,
            orderId
        );
        Integer paid = db.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE order_id=? AND status='Successful'",
            Integer.class,
            orderId
        );
        if (!"Open".equals(o.get("status")) || started > 0 || paid > 0) {
            throw new IllegalArgumentException("Cannot cancel: Order is being prepared/served or already paid.");
        }
        db.update("UPDATE order_details SET status='Cancelled' WHERE order_id=? AND status<>'Cancelled'", orderId);
        db.update("UPDATE orders SET status='Cancelled' WHERE id=?", orderId);
        db.update("UPDATE restaurant_tables SET status='Available' WHERE id=?", o.get("table_id"));
    }

    @Transactional
    public void addEmployee(String name, String username, String password, String role) {
        if (name == null || name.isBlank() || username == null || username.isBlank() || password == null || password.length() < 6) {
            throw new IllegalArgumentException("All fields required; password must be at least 6 characters.");
        }
        if (db.queryForObject("SELECT COUNT(*) FROM accounts WHERE username=?", Integer.class, username) > 0) {
            throw new IllegalArgumentException("Username already exists.");
        }
        long accountId = insertId("INSERT INTO accounts(username,password_hash,active) VALUES(?,?,1)", username, encoder.encode(password));
        db.update("INSERT INTO employees(account_id,full_name,role) VALUES(?,?,?)", accountId, name, role);
    }

    @Transactional
    public void updateEmployee(long id, String name, String role, boolean active) {
        db.update("UPDATE employees SET full_name=?,role=? WHERE id=?", name, role, id);
        db.update("UPDATE accounts SET active=? WHERE id=(SELECT account_id FROM employees WHERE id=?)", active, id);
    }

    @Transactional
    public void updateTable(long id, String status) {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM orders WHERE table_id=? AND status='Open'", Integer.class, id);
        if (n > 0 && (status.equals("Available") || status.equals("OutOfService"))) {
            throw new IllegalArgumentException("Table with an Open order cannot be changed to this status.");
        }
        db.update("UPDATE restaurant_tables SET status=? WHERE id=?", status, id);
    }

    @Transactional
    public void addTable(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Table code is required.");
        }
        db.update("INSERT INTO restaurant_tables(table_code,status) VALUES(?,'Available')", code.trim());
    }

    public List<Map<String, Object>> revenue(LocalDate from, LocalDate to) {
        return db.queryForList(
            "SELECT DATE(paid_at) AS paid_date, COUNT(*) AS transactions, SUM(amount) AS revenue " +
            "FROM payments WHERE status='Successful' AND DATE(paid_at) BETWEEN ? AND ? " +
            "GROUP BY DATE(paid_at) ORDER BY paid_date",
            from, to
        );
    }

    /**
     * UC11: Financial and Business Analytics (Manager).
     */
    public Map<String, Object> revenueReportData(LocalDate from, LocalDate to) {
        Map<String, Object> res = new HashMap<>();

        BigDecimal todayRev = db.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status='Successful' AND DATE(paid_at) = CURDATE()",
            BigDecimal.class
        );
        res.put("todayRevenue", todayRev != null ? todayRev : BigDecimal.ZERO);

        BigDecimal periodRev = db.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status='Successful' AND DATE(paid_at) BETWEEN ? AND ?",
            BigDecimal.class,
            from, to
        );
        res.put("periodRevenue", periodRev != null ? periodRev : BigDecimal.ZERO);

        Integer periodTx = db.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE status='Successful' AND DATE(paid_at) BETWEEN ? AND ?",
            Integer.class,
            from, to
        );
        int txCount = periodTx != null ? periodTx : 0;
        res.put("periodTransactions", txCount);

        BigDecimal aov = BigDecimal.ZERO;
        if (txCount > 0 && periodRev != null && periodRev.compareTo(BigDecimal.ZERO) > 0) {
            aov = periodRev.divide(BigDecimal.valueOf(txCount), 0, java.math.RoundingMode.HALF_UP);
        }
        res.put("avgOrderValue", aov);

        List<Map<String, Object>> dailyList = db.queryForList(
            "SELECT DATE(paid_at) AS paid_date, COUNT(*) AS transactions, SUM(amount) AS revenue " +
            "FROM payments WHERE status='Successful' AND DATE(paid_at) BETWEEN ? AND ? " +
            "GROUP BY DATE(paid_at) ORDER BY paid_date DESC",
            from, to
        );
        List<Map<String, Object>> enrichedDaily = new ArrayList<>();
        BigDecimal maxDaily = BigDecimal.ZERO;
        for (Map<String, Object> row : dailyList) {
            Map<String, Object> d = new HashMap<>(row);
            int count = ((Number) row.get("transactions")).intValue();
            BigDecimal rev = (BigDecimal) row.get("revenue");
            if (rev != null && rev.compareTo(maxDaily) > 0) {
                maxDaily = rev;
            }
            BigDecimal dAvg = (count > 0 && rev != null) 
                ? rev.divide(BigDecimal.valueOf(count), 0, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
            d.put("avgOrderValue", dAvg);
            enrichedDaily.add(d);
        }
        res.put("dailyRevenue", enrichedDaily);
        res.put("maxDailyRevenue", maxDaily.compareTo(BigDecimal.ZERO) > 0 ? maxDaily : BigDecimal.ONE);

        List<Map<String, Object>> topDishes = db.queryForList(
            "SELECT d.item_name, COALESCE(m.category, 'General') AS category, " +
            "SUM(d.quantity) AS total_quantity, " +
            "SUM(d.quantity * d.unit_price) AS total_sales " +
            "FROM order_details d " +
            "JOIN payments p ON p.order_id = d.order_id " +
            "LEFT JOIN menu_items m ON m.id = d.menu_item_id " +
            "WHERE p.status = 'Successful' AND d.status <> 'Cancelled' AND DATE(p.paid_at) BETWEEN ? AND ? " +
            "GROUP BY d.item_name, m.category " +
            "ORDER BY total_quantity DESC, total_sales DESC LIMIT 5",
            from, to
        );
        res.put("topDishes", topDishes);

        List<Map<String, Object>> methods = db.queryForList(
            "SELECT payment_method, COUNT(*) AS tx_count, SUM(amount) AS total_amount " +
            "FROM payments WHERE status = 'Successful' AND DATE(paid_at) BETWEEN ? AND ? " +
            "GROUP BY payment_method ORDER BY total_amount DESC",
            from, to
        );
        res.put("paymentMethods", methods);

        List<Map<String, Object>> monthly = db.queryForList(
            "SELECT MONTH(paid_at) AS month_num, SUM(amount) AS month_revenue, COUNT(*) AS month_tx " +
            "FROM payments WHERE status = 'Successful' AND YEAR(paid_at) = YEAR(CURDATE()) " +
            "GROUP BY MONTH(paid_at) ORDER BY month_num"
        );
        res.put("monthlyRevenue", monthly);

        return res;
    }

    private long insertId(String sql, Object... args) {
        KeyHolder holder = new GeneratedKeyHolder();
        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, holder);
        Number key = holder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not retrieve generated key.");
        }
        return key.longValue();
    }

    public void seedDemoAccounts() {
        String hash = encoder.encode("password");
        for (String[] a : new String[][]{
            {"manager", "General Manager", "Manager"},
            {"waiter", "Head Server", "Waiter"},
            {"chef", "Executive Chef", "Chef"},
            {"cashier", "Lead Cashier", "Cashier"}
        }) {
            Integer n = db.queryForObject("SELECT COUNT(*) FROM accounts WHERE username=?", Integer.class, a[0]);
            if (n == 0) {
                db.update("INSERT INTO accounts(username,password_hash,active) VALUES(?,?,1)", a[0], hash);
                Long id = db.queryForObject("SELECT id FROM accounts WHERE username=?", Long.class, a[0]);
                db.update("INSERT INTO employees(account_id,full_name,role) VALUES(?,?,?)", id, a[1], a[2]);
            }
        }
    }

    public void seedDemoData() {
        seedDemoAccounts();
        try {
            Integer orderCount = db.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
            if (orderCount == null || orderCount == 0) {
                Long waiterId = db.queryForObject("SELECT id FROM employees WHERE role='Waiter' LIMIT 1", Long.class);
                if (waiterId != null) {
                    long completedOrderId = insertId(
                        "INSERT INTO orders(table_id, waiter_id, status, created_at) VALUES(1, ?, 'Completed', DATE_SUB(NOW(), INTERVAL 2 HOUR))",
                        waiterId
                    );
                    db.update("INSERT INTO order_details(order_id, menu_item_id, item_name, quantity, unit_price, status) VALUES(?, 11, 'Japanese A5 Wagyu Steak', 1, 1850000, 'Served')", completedOrderId);
                    db.update("INSERT INTO order_details(order_id, menu_item_id, item_name, quantity, unit_price, status) VALUES(?, 1, 'Fresh Coconut Water', 2, 65000, 'Served')", completedOrderId);
                    db.update("INSERT INTO payments(order_id, amount, payment_method, status, paid_at) VALUES(?, 1980000, 'Cash', 'Successful', DATE_SUB(NOW(), INTERVAL 1 HOUR))", completedOrderId);

                    long openOrderId = insertId(
                        "INSERT INTO orders(table_id, waiter_id, status, created_at) VALUES(3, ?, 'Open', NOW())",
                        waiterId
                    );
                    db.update("INSERT INTO order_details(order_id, menu_item_id, item_name, quantity, unit_price, status) VALUES(?, 15, 'Lobster Thermidor', 1, 1450000, 'Ready')", openOrderId);
                    db.update("INSERT INTO order_details(order_id, menu_item_id, item_name, quantity, unit_price, status) VALUES(?, 31, 'French Onion Soup', 2, 210000, 'Preparing')", openOrderId);
                    db.update("INSERT INTO order_details(order_id, menu_item_id, item_name, quantity, unit_price, status) VALUES(?, 8, 'Japanese Matcha Latte', 2, 95000, 'Pending')", openOrderId);
                    db.update("UPDATE restaurant_tables SET status='Occupied' WHERE id=3");
                }
            }
        } catch (Exception ignored) {}
    }
}
