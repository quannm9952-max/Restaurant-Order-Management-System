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
     * Tự động kiểm tra và thêm các cột cần thiết nếu kết nối tới database MySQL đã tạo bảng trước đó.
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
        if (!Boolean.TRUE.equals(u.get("active")) || !encoder.matches(password, (String) u.get("password_hash"))) {
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

        if ("price_asc".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY price ASC, name ASC");
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY price DESC, name ASC");
        } else if ("name_asc".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY name ASC");
        } else if ("name_desc".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY name DESC");
        } else {
            sql.append(" ORDER BY id DESC");
        }

        return db.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> getMenuItem(long id) {
        List<Map<String, Object>> list = db.queryForList("SELECT * FROM menu_items WHERE id = ?", id);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy món ăn với mã #" + id);
        }
        return list.get(0);
    }

    @Transactional
    public void addMenu(String name, String category, String description, BigDecimal price, String imageUrl) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên món ăn không được để trống.");
        }
        if (name.trim().length() > 150) {
            throw new IllegalArgumentException("Tên món không được vượt quá 150 ký tự.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn hoặc nhập danh mục.");
        }
        if (category.trim().length() > 80) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 80 ký tự.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0 VNĐ.");
        }

        Integer count = db.queryForObject(
            "SELECT COUNT(*) FROM menu_items WHERE LOWER(name) = ?",
            Integer.class,
            name.trim().toLowerCase()
        );
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Món ăn '" + name.trim() + "' đã tồn tại trong thực đơn.");
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
            throw new IllegalArgumentException("Tên món ăn không được để trống.");
        }
        if (name.trim().length() > 150) {
            throw new IllegalArgumentException("Tên món không được vượt quá 150 ký tự.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn hoặc nhập danh mục.");
        }
        if (category.trim().length() > 80) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 80 ký tự.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0 VNĐ.");
        }

        Integer duplicate = db.queryForObject(
            "SELECT COUNT(*) FROM menu_items WHERE LOWER(name) = ? AND id <> ?",
            Integer.class,
            name.trim().toLowerCase(),
            id
        );
        if (duplicate != null && duplicate > 0) {
            throw new IllegalArgumentException("Tên món '" + name.trim() + "' đã bị trùng với một món khác.");
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
        getMenuItem(id); // Kiểm tra tồn tại
        db.update("UPDATE menu_items SET available=? WHERE id=?", available, id);
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
            throw new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId);
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
     * Quy trình thanh toán:
     * - Khóa bản ghi đơn hàng với SELECT ... FOR UPDATE chống duplicate/race-condition.
     * - Kiểm tra đơn hàng đang Open.
     * - Kiểm tra chưa có giao dịch Successful.
     * - Kiểm tra tất cả món không bị hủy đã Served.
     * - Tính tiền từ order_details lịch sử.
     * - Ghi Payment, cập nhật Order thành Completed, giải phóng Table thành Available trong 1 transaction duy nhất.
     */
    @Transactional(rollbackFor = Exception.class)
    public long payOrder(long orderId, String paymentMethod, boolean success) {
        // 1. Khóa bản ghi Order
        List<Map<String, Object>> orderRows = db.queryForList("SELECT * FROM orders WHERE id=? FOR UPDATE", orderId);
        if (orderRows.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng #" + orderId + " không tồn tại.");
        }
        Map<String, Object> order = orderRows.get(0);

        // 2. Kiểm tra trạng thái đơn
        if (!"Open".equals(order.get("status"))) {
            throw new IllegalStateException("Đơn hàng #" + orderId + " không ở trạng thái Open (trạng thái: " + order.get("status") + ").");
        }

        // 3. Kiểm tra duplicate payment
        Integer paidCount = db.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE order_id=? AND status='Successful'",
            Integer.class,
            orderId
        );
        if (paidCount != null && paidCount > 0) {
            throw new IllegalStateException("Đơn hàng #" + orderId + " đã có giao dịch thanh toán thành công trước đó.");
        }

        // 4. Kiểm tra món ăn
        Integer validItems = db.queryForObject(
            "SELECT COUNT(*) FROM order_details WHERE order_id=? AND status <> 'Cancelled'",
            Integer.class,
            orderId
        );
        if (validItems == null || validItems == 0) {
            throw new IllegalStateException("Đơn hàng không có món ăn hợp lệ để thanh toán.");
        }

        if (success) {
            Integer unservedCount = db.queryForObject(
                "SELECT COUNT(*) FROM order_details WHERE order_id=? AND status NOT IN ('Served','Cancelled')",
                Integer.class,
                orderId
            );
            if (unservedCount != null && unservedCount > 0) {
                throw new IllegalStateException("Không thể thanh toán: Vẫn còn " + unservedCount + " món chưa được phục vụ (Served).");
            }
        }

        // 5. Tính tổng tiền từ order_details (giá lịch sử lúc tạo đơn)
        BigDecimal amount = total(orderId);
        if (amount.compareTo(BigDecimal.ZERO) <= 0 && success) {
            throw new IllegalStateException("Tổng tiền thanh toán không hợp lệ.");
        }

        String method = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod : "Cash";

        // 6. Thêm bản ghi Payment
        long paymentId = insertId(
            "INSERT INTO payments(order_id, amount, payment_method, status, paid_at) VALUES(?, ?, ?, ?, NOW())",
            orderId, amount, method, success ? "Successful" : "Failed"
        );

        // 7. Cập nhật Order và Table nếu thanh toán thành công
        if (success) {
            int updatedOrder = db.update("UPDATE orders SET status='Completed' WHERE id=? AND status='Open'", orderId);
            if (updatedOrder == 0) {
                throw new IllegalStateException("Cập nhật đơn hàng thất bại.");
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
     * Lấy dữ liệu hóa đơn cho đơn đã thanh toán thành công.
     */
    public Map<String, Object> invoiceData(long orderId) {
        List<Map<String, Object>> orderRows = db.queryForList(
            "SELECT o.*, t.table_code, e.full_name AS waiter_name " +
            "FROM orders o " +
            "JOIN restaurant_tables t ON t.id=o.table_id " +
            "JOIN employees e ON e.id=o.waiter_id " +
            "WHERE o.id=?", orderId);
        if (orderRows.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId);
        }
        Map<String, Object> order = orderRows.get(0);
        if (!"Completed".equals(order.get("status"))) {
            throw new IllegalStateException("Đơn hàng #" + orderId + " chưa hoàn tất thanh toán (Trạng thái: " + order.get("status") + ").");
        }

        List<Map<String, Object>> paymentRows = db.queryForList(
            "SELECT * FROM payments WHERE order_id=? AND status='Successful' ORDER BY paid_at DESC", orderId);
        if (paymentRows.isEmpty()) {
            throw new IllegalStateException("Chưa có giao dịch thanh toán thành công cho đơn #" + orderId);
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
            throw new IllegalArgumentException("Bàn không ở trạng thái Available.");
        }
        Integer count = db.queryForObject("SELECT COUNT(*) FROM orders WHERE table_id=? AND status='Open'", Integer.class, tableId);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Bàn đã có đơn Open.");
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Đơn phải có ít nhất một món.");
        }
        long orderId = insertId("INSERT INTO orders(table_id,waiter_id,status,created_at) VALUES(?,?, 'Open',NOW())", tableId, waiterId);
        for (var entry : items.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
            }
            List<Map<String, Object>> itemRows = db.queryForList("SELECT name,price FROM menu_items WHERE id=? AND available=1", entry.getKey());
            if (itemRows.isEmpty()) {
                throw new IllegalArgumentException("Món ăn không tồn tại hoặc đã ngừng kinh doanh.");
            }
            Map<String, Object> item = itemRows.get(0);
            db.update(
                "INSERT INTO order_details(order_id,menu_item_id,item_name,quantity,unit_price,status) VALUES(?,?,?,?,?,'Pending')",
                orderId, entry.getKey(), item.get("name"), entry.getValue(), item.get("price")
            );
        }
        db.update("UPDATE restaurant_tables SET status='Occupied' WHERE id=?", tableId);
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
            throw new IllegalArgumentException("Chuyển trạng thái không hợp lệ hoặc đơn không còn Open.");
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
            throw new IllegalArgumentException("Không thể hủy: đơn đã chế biến/phục vụ hoặc đã thanh toán.");
        }
        db.update("UPDATE order_details SET status='Cancelled' WHERE order_id=? AND status<>'Cancelled'", orderId);
        db.update("UPDATE orders SET status='Cancelled' WHERE id=?", orderId);
        db.update("UPDATE restaurant_tables SET status='Available' WHERE id=?", o.get("table_id"));
    }

    @Transactional
    public void addEmployee(String name, String username, String password, String role) {
        if (name == null || name.isBlank() || username == null || username.isBlank() || password == null || password.length() < 6) {
            throw new IllegalArgumentException("Nhập đủ thông tin; mật khẩu tối thiểu 6 ký tự.");
        }
        if (db.queryForObject("SELECT COUNT(*) FROM accounts WHERE username=?", Integer.class, username) > 0) {
            throw new IllegalArgumentException("Username đã tồn tại.");
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
            throw new IllegalArgumentException("Bàn có đơn Open không thể chuyển trạng thái này.");
        }
        db.update("UPDATE restaurant_tables SET status=? WHERE id=?", status, id);
    }

    @Transactional
    public void addTable(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã bàn bắt buộc.");
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
            throw new IllegalStateException("Không lấy được ID bản ghi vừa tạo.");
        }
        return key.longValue();
    }

    public void seedDemoAccounts() {
        String hash = encoder.encode("Demo@123");
        for (String[] a : new String[][]{
            {"manager", "Manager Demo", "Manager"},
            {"waiter", "Waiter Demo", "Waiter"},
            {"chef", "Chef Demo", "Chef"},
            {"cashier", "Cashier Demo", "Cashier"}
        }) {
            Integer n = db.queryForObject("SELECT COUNT(*) FROM accounts WHERE username=?", Integer.class, a[0]);
            if (n == 0) {
                db.update("INSERT INTO accounts(username,password_hash,active) VALUES(?,?,1)", a[0], hash);
                Long id = db.queryForObject("SELECT id FROM accounts WHERE username=?", Long.class, a[0]);
                db.update("INSERT INTO employees(account_id,full_name,role) VALUES(?,?,?)", id, a[1], a[2]);
            }
        }
    }
}

