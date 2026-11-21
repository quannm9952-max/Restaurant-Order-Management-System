-- Initial master data only. Existing IDs retain every user-edited value.
-- Orders, payments and invoices are created through the application.
START TRANSACTION;

INSERT INTO Employee (employee_id, full_name, position) VALUES
  (9005, 'Nhân viên phục vụ 02', 'WAITER'),
  (9006, 'Đầu bếp 02', 'CHEF'),
  (9007, 'Nhân viên thu ngân 02', 'CASHIER'),
  (9008, 'Quản lý 02', 'MANAGER')
ON DUPLICATE KEY UPDATE employee_id=VALUES(employee_id);

-- Initial password: 123456. Restarts preserve password, username and lock edits.
INSERT INTO Account (account_id, employee_id, username, password_hash, role, is_active) VALUES
  (9005, 9005, 'waiter', '$2a$10$7Hws5Wt89LqyyvmteKP5nOYHhR/avaqDuaVWw33wFtIyuqaXHabZC', 'WAITER', 1),
  (9006, 9006, 'chef', '$2a$10$7Hws5Wt89LqyyvmteKP5nOYHhR/avaqDuaVWw33wFtIyuqaXHabZC', 'CHEF', 1),
  (9007, 9007, 'cashier', '$2a$10$7Hws5Wt89LqyyvmteKP5nOYHhR/avaqDuaVWw33wFtIyuqaXHabZC', 'CASHIER', 1),
  (9008, 9008, 'manager', '$2a$10$7Hws5Wt89LqyyvmteKP5nOYHhR/avaqDuaVWw33wFtIyuqaXHabZC', 'MANAGER', 1)
ON DUPLICATE KEY UPDATE account_id=VALUES(account_id);

INSERT INTO Restaurant_tables (table_id, table_name, capacity, status) VALUES
  (9001, 'Table 01', 4, 'AVAILABLE'),
  (9002, 'Table 02', 4, 'AVAILABLE'),
  (9003, 'Table 03', 4, 'AVAILABLE'),
  (9004, 'Table 04', 4, 'AVAILABLE'),
  (9005, 'Table 05', 4, 'AVAILABLE'),
  (9006, 'Table 06', 4, 'AVAILABLE'),
  (9007, 'Table 07', 4, 'AVAILABLE'),
  (9008, 'Table 08', 4, 'AVAILABLE'),
  (9010, 'Table 09', 4, 'AVAILABLE')
ON DUPLICATE KEY UPDATE table_id=VALUES(table_id);

INSERT INTO Menu_category (category_id, category_name, description, is_active) VALUES
  (9001, 'Salads & Grills', NULL, 1),
  (9101, 'Main Courses', NULL, 1),
  (9102, 'Appetizers', NULL, 1),
  (9103, 'Beverages', NULL, 1),
  (9104, 'Desserts', NULL, 1)
ON DUPLICATE KEY UPDATE category_id=VALUES(category_id);

INSERT INTO Menu_item (item_id, category_id, item_name, description, unit_price, is_active, is_available) VALUES
  (9001, 9001, 'Grilled Chicken', NULL, 245000.00, 1, 1),
  (9002, 9001, 'Salad', NULL, 450000.00, 1, 1),
  (9003, 9001, 'Chocolate Dessert', NULL, 120000.00, 1, 1),
  (9111, 9101, 'Japanese A5 Wagyu Steak', NULL, 650000.00, 1, 1),
  (9112, 9101, 'Lobster Thermidor', NULL, 485000.00, 1, 1),
  (9113, 9102, 'Burrata with Heirloom Tomatoes', NULL, 120000.00, 1, 1),
  (9114, 9102, 'French Onion Soup', NULL, 85000.00, 1, 1),
  (9115, 9103, 'Classic Mojito Mocktail', NULL, 60000.00, 1, 1),
  (9116, 9103, 'Premium Jasmine Tea', NULL, 45000.00, 1, 1),
  (9117, 9104, 'Italian Tiramisu', NULL, 90000.00, 1, 1),
  (9118, 9104, 'Belgian Chocolate Fondant', NULL, 105000.00, 1, 1)
ON DUPLICATE KEY UPDATE item_id=VALUES(item_id);

COMMIT;
