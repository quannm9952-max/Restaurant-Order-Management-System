-- DEMO fixture for checkout while UC06/UC07 await TV3; idempotent.
INSERT INTO Restaurant_tables(table_id,table_name,capacity,status) VALUES
(9010,'DEMO Checkout Table',4,'OCCUPIED')
ON DUPLICATE KEY UPDATE table_name=VALUES(table_name);
INSERT INTO Orders(order_id,table_id,employee_id,created_at,status,total_amount,notes)
VALUES(1100,9010,9001,'2026-11-05 12:00:00','OPEN',39.00,'DEMO: Served fixture; TV3 not integrated')
ON DUPLICATE KEY UPDATE notes=VALUES(notes);
INSERT INTO Order_Detail(order_detail_id,order_id,item_id,quantity,unit_price_at_order,item_name_at_order,preparation_status)
VALUES(4100,1100,9001,2,19.50,'DEMO checkout dish','SERVED')
ON DUPLICATE KEY UPDATE item_name_at_order=VALUES(item_name_at_order);

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
