-- Demo data: reruns preserve existing business records.
START TRANSACTION;

INSERT INTO Employee (employee_id, full_name, position) VALUES
  (9001, 'DEMO Waiter', 'WAITER'),
  (9002, 'DEMO Chef', 'CHEF'),
  (9003, 'DEMO Cashier', 'CASHIER'),
  (9004, 'DEMO Manager', 'MANAGER')
ON DUPLICATE KEY UPDATE employee_id=VALUES(employee_id);

-- Accounts are disabled fixture records. Interactive local demo authentication is profile-scoped.
INSERT INTO Account (account_id, employee_id, username, password_hash, role, is_active) VALUES
  (9001, 9001, 'DEMO_FIXTURE_WAITER', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20D3A3xH60JYdw6BY2Zr4oW17Q4p4K', 'WAITER', 0),
  (9002, 9002, 'DEMO_FIXTURE_CHEF', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20D3A3xH60JYdw6BY2Zr4oW17Q4p4K', 'CHEF', 0),
  (9003, 9003, 'DEMO_FIXTURE_CASHIER', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20D3A3xH60JYdw6BY2Zr4oW17Q4p4K', 'CASHIER', 0),
  (9004, 9004, 'DEMO_FIXTURE_MANAGER', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20D3A3xH60JYdw6BY2Zr4oW17Q4p4K', 'MANAGER', 0)
ON DUPLICATE KEY UPDATE account_id=VALUES(account_id);

INSERT INTO Restaurant_tables (table_id, table_name, capacity, status) VALUES
  (9001, 'DEMO Table 01', 4, 'OCCUPIED'),
  (9002, 'DEMO Table 02', 4, 'OCCUPIED'),
  (9003, 'DEMO Table 03', 4, 'OCCUPIED'),
  (9004, 'DEMO Table 04', 4, 'AVAILABLE'),
  (9005, 'DEMO Table 05', 4, 'AVAILABLE'),
  (9006, 'DEMO Table 06', 4, 'AVAILABLE'),
  (9007, 'DEMO Table 07', 4, 'AVAILABLE'),
  (9008, 'DEMO Table 08', 4, 'OUT_OF_SERVICE')
ON DUPLICATE KEY UPDATE table_id=VALUES(table_id);

INSERT INTO Menu_category (category_id, category_name, description, is_active) VALUES
  (9001, 'DEMO Main courses', 'TEST/DEMO menu data', 1)
ON DUPLICATE KEY UPDATE category_id=VALUES(category_id);

INSERT INTO Menu_item (item_id, category_id, item_name, description, unit_price, is_active, is_available) VALUES
  (9001, 9001, 'DEMO Grilled Chicken', 'TEST current menu price intentionally differs from historical order price', 24.50, 1, 1),
  (9002, 9001, 'DEMO Salad', 'TEST/DEMO', 45, 1, 1),
  (9003, 9001, 'DEMO Discontinued Dessert', 'TEST/DEMO discontinued item', 12, 0, 0)
ON DUPLICATE KEY UPDATE item_id=VALUES(item_id);

INSERT INTO Orders (order_id, table_id, employee_id, created_at, status, total_amount, notes) VALUES
  (1015, 9004, 9001, '2026-10-29 11:00:00', 'COMPLETED', 98, 'DEMO reporting fixture'),
  (1016, 9005, 9001, '2026-10-29 11:00:00', 'COMPLETED', 98, 'DEMO reporting fixture'),
  (1017, 9006, 9001, '2026-10-30 11:00:00', 'COMPLETED', 60, 'DEMO reporting fixture'),
  (1018, 9007, 9001, '2026-10-30 11:00:00', 'COMPLETED', 70, 'DEMO reporting fixture'),
  (1019, 9004, 9001, '2026-10-30 11:00:00', 'COMPLETED', 50, 'DEMO reporting fixture'),
  (1020, 9005, 9001, '2026-10-30 11:00:00', 'COMPLETED', 68, 'DEMO reporting fixture'),
  (1021, 9006, 9001, '2026-10-31 11:00:00', 'COMPLETED', 80, 'DEMO reporting fixture'),
  (1022, 9007, 9001, '2026-10-31 11:00:00', 'COMPLETED', 70, 'DEMO reporting fixture'),
  (1023, 9004, 9001, '2026-10-31 11:00:00', 'COMPLETED', 90, 'DEMO reporting fixture'),
  (1024, 9005, 9001, '2026-10-31 11:00:00', 'COMPLETED', 72, 'DEMO reporting fixture'),
  (1025, 9006, 9001, '2026-11-01 11:00:00', 'COMPLETED', 80, 'DEMO reporting fixture'),
  (1026, 9007, 9001, '2026-11-01 11:00:00', 'COMPLETED', 100, 'DEMO reporting fixture'),
  (1027, 9004, 9001, '2026-11-01 11:00:00', 'COMPLETED', 104, 'DEMO reporting fixture'),
  (1028, 9005, 9001, '2026-11-02 11:00:00', 'COMPLETED', 80, 'DEMO reporting fixture'),
  (1029, 9006, 9001, '2026-11-02 11:00:00', 'COMPLETED', 90, 'DEMO reporting fixture'),
  (1030, 9007, 9001, '2026-11-02 11:00:00', 'COMPLETED', 78, 'DEMO reporting fixture'),
  (1031, 9004, 9001, '2026-11-02 11:00:00', 'COMPLETED', 90, 'DEMO reporting fixture'),
  (1032, 9005, 9001, '2026-11-03 11:00:00', 'COMPLETED', 70, 'DEMO reporting fixture'),
  (1033, 9006, 9001, '2026-11-03 11:00:00', 'COMPLETED', 64, 'DEMO reporting fixture'),
  (1034, 9007, 9001, '2026-11-03 11:00:00', 'COMPLETED', 70, 'DEMO reporting fixture'),
  (1035, 9004, 9001, '2026-11-03 11:00:00', 'COMPLETED', 126, 'DEMO reporting fixture'),
  (1037, 9005, 9001, '2026-11-04 11:00:00', 'COMPLETED', 38, 'DEMO reporting fixture'),
  (1039, 9006, 9001, '2026-11-04 11:00:00', 'COMPLETED', 68, 'DEMO reporting fixture'),
  (1042, 9007, 9001, '2026-11-04 11:00:00', 'COMPLETED', 78, 'DEMO reporting fixture'),
  (1036, 9003, 9001, '2026-11-03 19:30:00', 'OPEN', 44, 'DEMO failed payment; order remains Open'),
  (1050, 9004, 9001, '2026-11-04 10:00:00', 'CANCELLED', 0, 'DEMO cancelled before preparation; no successful payment'),
  (1051, 9001, 9001, '2026-11-04 12:00:00', 'OPEN', 38, 'DEMO pending items and pending payment'),
  (1052, 9002, 9001, '2026-11-04 12:10:00', 'OPEN', 48, 'DEMO Preparing and Ready items; pending payment')
ON DUPLICATE KEY UPDATE order_id=VALUES(order_id);

INSERT INTO Order_Detail (order_detail_id, order_id, item_id, quantity, unit_price_at_order, item_name_at_order, preparation_status) VALUES
  (9100, 1015, 9001, 2, 24.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9101, 1015, 9002, 1, 49, 'DEMO Salad', 'SERVED'),
  (9102, 1016, 9001, 2, 24.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9103, 1016, 9002, 1, 49, 'DEMO Salad', 'SERVED'),
  (9104, 1017, 9001, 2, 15, 'DEMO Grilled Chicken', 'SERVED'),
  (9105, 1017, 9002, 1, 30, 'DEMO Salad', 'SERVED'),
  (9106, 1018, 9001, 2, 17.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9107, 1018, 9002, 1, 35, 'DEMO Salad', 'SERVED'),
  (9108, 1019, 9001, 2, 12.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9109, 1019, 9002, 1, 25, 'DEMO Salad', 'SERVED'),
  (9110, 1020, 9001, 2, 17, 'DEMO Grilled Chicken', 'SERVED'),
  (9111, 1020, 9002, 1, 34, 'DEMO Salad', 'SERVED'),
  (9112, 1021, 9001, 2, 20, 'DEMO Grilled Chicken', 'SERVED'),
  (9113, 1021, 9002, 1, 40, 'DEMO Salad', 'SERVED'),
  (9114, 1022, 9001, 2, 17.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9115, 1022, 9002, 1, 35, 'DEMO Salad', 'SERVED'),
  (9116, 1023, 9001, 2, 22.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9117, 1023, 9002, 1, 45, 'DEMO Salad', 'SERVED'),
  (9118, 1024, 9001, 2, 18, 'DEMO Grilled Chicken', 'SERVED'),
  (9119, 1024, 9002, 1, 36, 'DEMO Salad', 'SERVED'),
  (9120, 1025, 9001, 2, 20, 'DEMO Grilled Chicken', 'SERVED'),
  (9121, 1025, 9002, 1, 40, 'DEMO Salad', 'SERVED'),
  (9122, 1026, 9001, 2, 25, 'DEMO Grilled Chicken', 'SERVED'),
  (9123, 1026, 9002, 1, 50, 'DEMO Salad', 'SERVED'),
  (9124, 1027, 9001, 2, 26, 'DEMO Grilled Chicken', 'SERVED'),
  (9125, 1027, 9002, 1, 52, 'DEMO Salad', 'SERVED'),
  (9126, 1028, 9001, 2, 20, 'DEMO Grilled Chicken', 'SERVED'),
  (9127, 1028, 9002, 1, 40, 'DEMO Salad', 'SERVED'),
  (9128, 1029, 9001, 2, 22.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9129, 1029, 9002, 1, 45, 'DEMO Salad', 'SERVED'),
  (9130, 1030, 9001, 2, 19.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9131, 1030, 9002, 1, 39, 'DEMO Salad', 'SERVED'),
  (9132, 1031, 9001, 2, 22.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9133, 1031, 9002, 1, 45, 'DEMO Salad', 'SERVED'),
  (9134, 1032, 9001, 2, 17.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9135, 1032, 9002, 1, 35, 'DEMO Salad', 'SERVED'),
  (9136, 1033, 9001, 2, 16, 'DEMO Grilled Chicken', 'SERVED'),
  (9137, 1033, 9002, 1, 32, 'DEMO Salad', 'SERVED'),
  (9138, 1034, 9001, 2, 17.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9139, 1034, 9002, 1, 35, 'DEMO Salad', 'SERVED'),
  (9140, 1035, 9001, 2, 31.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9141, 1035, 9002, 1, 63, 'DEMO Salad', 'SERVED'),
  (9142, 1037, 9001, 2, 9.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9143, 1037, 9002, 1, 19, 'DEMO Salad', 'SERVED'),
  (9144, 1039, 9001, 2, 17, 'DEMO Grilled Chicken', 'SERVED'),
  (9145, 1039, 9002, 1, 34, 'DEMO Salad', 'SERVED'),
  (9146, 1042, 9001, 2, 19.5, 'DEMO Grilled Chicken', 'SERVED'),
  (9147, 1042, 9002, 1, 39, 'DEMO Salad', 'SERVED'),
  (9200, 1042, 9003, 1, 12, 'DEMO Discontinued Dessert', 'CANCELLED'),
  (9201, 1036, 9001, 1, 44, 'DEMO Grilled Chicken', 'SERVED'),
  (9202, 1050, 9003, 1, 12, 'DEMO Discontinued Dessert', 'CANCELLED'),
  (9203, 1051, 9002, 2, 19, 'DEMO Salad', 'PENDING'),
  (9204, 1052, 9001, 1, 24, 'DEMO Grilled Chicken', 'PREPARING'),
  (9205, 1052, 9002, 1, 24, 'DEMO Salad', 'READY')
ON DUPLICATE KEY UPDATE order_detail_id=VALUES(order_detail_id);

INSERT INTO Payment (payment_id, order_id, payment_time, amount, payment_method, status, transaction_reference) VALUES
  (2021, 1015, '2026-10-29 12:20:00', 98, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2021'),
  (2022, 1016, '2026-10-29 14:20:00', 98, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2022'),
  (2023, 1017, '2026-10-30 12:20:00', 60, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2023'),
  (2024, 1018, '2026-10-30 14:20:00', 70, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2024'),
  (2025, 1019, '2026-10-30 16:20:00', 50, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2025'),
  (2026, 1020, '2026-10-30 18:20:00', 68, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2026'),
  (2027, 1021, '2026-10-31 12:20:00', 80, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2027'),
  (2028, 1022, '2026-10-31 14:20:00', 70, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2028'),
  (2029, 1023, '2026-10-31 16:20:00', 90, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2029'),
  (2030, 1024, '2026-10-31 18:20:00', 72, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2030'),
  (2031, 1025, '2026-11-01 12:20:00', 80, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2031'),
  (2032, 1026, '2026-11-01 14:20:00', 100, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2032'),
  (2033, 1027, '2026-11-01 16:20:00', 104, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2033'),
  (2034, 1028, '2026-11-02 12:20:00', 80, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2034'),
  (2035, 1029, '2026-11-02 14:20:00', 90, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2035'),
  (2036, 1030, '2026-11-02 16:20:00', 78, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2036'),
  (2037, 1031, '2026-11-02 18:20:00', 90, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2037'),
  (2038, 1032, '2026-11-03 12:20:00', 70, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2038'),
  (2039, 1033, '2026-11-03 14:20:00', 64, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2039'),
  (2040, 1034, '2026-11-03 16:20:00', 70, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2040'),
  (2041, 1035, '2026-11-03 18:20:00', 126, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2041'),
  (2043, 1037, '2026-11-04 12:48:00', 38, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2043'),
  (2044, 1039, '2026-11-04 13:04:00', 68, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2044'),
  (2046, 1042, '2026-11-04 13:20:00', 78, 'Cash', 'SUCCESSFUL', 'DEMO-PAY-2046'),
  (2042, 1036, '2026-11-03 20:34:00', 44, 'Cash', 'FAILED', 'DEMO-PAY-2042'),
  (2045, 1042, '2026-11-04 13:18:00', 78, 'Cash', 'FAILED', 'DEMO-PAY-2045'),
  (2047, 1051, '2026-11-04 13:35:00', 38, 'Cash', 'PENDING', 'DEMO-PAY-2047'),
  (2048, 1052, '2026-11-04 13:40:00', 48, 'Cash', 'PENDING', 'DEMO-PAY-2048')
ON DUPLICATE KEY UPDATE payment_id=VALUES(payment_id);

INSERT INTO Invoice (invoice_id, order_id, invoice_number, issued_at, total_amount) VALUES
  (9001, 1015, 'DEMO-INV-1015', '2026-10-29 12:20:00', 98),
  (9002, 1016, 'DEMO-INV-1016', '2026-10-29 14:20:00', 98),
  (9003, 1017, 'DEMO-INV-1017', '2026-10-30 12:20:00', 60),
  (9004, 1018, 'DEMO-INV-1018', '2026-10-30 14:20:00', 70),
  (9005, 1019, 'DEMO-INV-1019', '2026-10-30 16:20:00', 50),
  (9006, 1020, 'DEMO-INV-1020', '2026-10-30 18:20:00', 68),
  (9007, 1021, 'DEMO-INV-1021', '2026-10-31 12:20:00', 80),
  (9008, 1022, 'DEMO-INV-1022', '2026-10-31 14:20:00', 70),
  (9009, 1023, 'DEMO-INV-1023', '2026-10-31 16:20:00', 90),
  (9010, 1024, 'DEMO-INV-1024', '2026-10-31 18:20:00', 72),
  (9011, 1025, 'DEMO-INV-1025', '2026-11-01 12:20:00', 80),
  (9012, 1026, 'DEMO-INV-1026', '2026-11-01 14:20:00', 100),
  (9013, 1027, 'DEMO-INV-1027', '2026-11-01 16:20:00', 104),
  (9014, 1028, 'DEMO-INV-1028', '2026-11-02 12:20:00', 80),
  (9015, 1029, 'DEMO-INV-1029', '2026-11-02 14:20:00', 90),
  (9016, 1030, 'DEMO-INV-1030', '2026-11-02 16:20:00', 78),
  (9017, 1031, 'DEMO-INV-1031', '2026-11-02 18:20:00', 90),
  (9018, 1032, 'DEMO-INV-1032', '2026-11-03 12:20:00', 70),
  (9019, 1033, 'DEMO-INV-1033', '2026-11-03 14:20:00', 64),
  (9020, 1034, 'DEMO-INV-1034', '2026-11-03 16:20:00', 70),
  (9021, 1035, 'DEMO-INV-1035', '2026-11-03 18:20:00', 126),
  (9022, 1037, 'DEMO-INV-1037', '2026-11-04 12:48:00', 38),
  (9023, 1039, 'DEMO-INV-1039', '2026-11-04 13:04:00', 68),
  (9024, 1042, 'DEMO-INV-1042', '2026-11-04 13:20:00', 78)
ON DUPLICATE KEY UPDATE invoice_id=VALUES(invoice_id);




-- Independent DEMO checkout fixture; reruns preserve existing business records.
INSERT INTO Restaurant_tables(table_id,table_name,capacity,status) VALUES
(9010,'DEMO Checkout Table',4,'OCCUPIED')
ON DUPLICATE KEY UPDATE table_id=VALUES(table_id);
INSERT INTO Orders(order_id,table_id,employee_id,created_at,status,total_amount,notes)
VALUES(1100,9010,9001,'2026-11-05 12:00:00','OPEN',39.00,'DEMO: Served fixture for independent checkout verification')
ON DUPLICATE KEY UPDATE order_id=VALUES(order_id);
INSERT INTO Order_Detail(order_detail_id,order_id,item_id,quantity,unit_price_at_order,item_name_at_order,preparation_status)
VALUES(4100,1100,9001,2,19.50,'DEMO checkout dish','SERVED')
ON DUPLICATE KEY UPDATE order_detail_id=VALUES(order_detail_id);

INSERT INTO Menu_category(category_id,category_name,description,is_active) VALUES
(9101,'Main Courses','DEMO catalog',true),(9102,'Appetizers','DEMO catalog',true),(9103,'Beverages','DEMO catalog',true),(9104,'Desserts','DEMO catalog',true)
ON DUPLICATE KEY UPDATE category_id=VALUES(category_id);
INSERT INTO Menu_item(item_id,category_id,item_name,description,unit_price,is_active,is_available) VALUES
(9111,9101,'DEMO Japanese A5 Wagyu Steak','DEMO price in USD',65.00,true,true),
(9112,9101,'DEMO Lobster Thermidor','DEMO price in USD',48.50,true,true),
(9113,9102,'DEMO Burrata with Heirloom Tomatoes','DEMO price in USD',12.00,true,true),
(9114,9102,'DEMO French Onion Soup','DEMO price in USD',8.50,true,true),
(9115,9103,'DEMO Classic Mojito Mocktail','DEMO price in USD',6.00,true,true),
(9116,9103,'DEMO Premium Jasmine Tea','DEMO price in USD',4.50,true,true),
(9117,9104,'DEMO Italian Tiramisu','DEMO price in USD',9.00,true,true),
(9118,9104,'DEMO Belgian Chocolate Fondant','DEMO price in USD',10.50,true,true)
ON DUPLICATE KEY UPDATE item_id=VALUES(item_id);

COMMIT;
