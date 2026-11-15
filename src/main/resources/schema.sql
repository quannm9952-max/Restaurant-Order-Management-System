CREATE TABLE IF NOT EXISTS accounts (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 username VARCHAR(60) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS employees (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 account_id BIGINT NOT NULL UNIQUE,
 full_name VARCHAR(120) NOT NULL,
 phone VARCHAR(30),
 role ENUM('Waiter','Chef','Cashier','Manager') NOT NULL,
 FOREIGN KEY(account_id) REFERENCES accounts(id)
);

CREATE TABLE IF NOT EXISTS restaurant_tables (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 table_code VARCHAR(30) NOT NULL UNIQUE,
 status ENUM('Available','Occupied','OutOfService') NOT NULL DEFAULT 'Available'
);

CREATE TABLE IF NOT EXISTS menu_items (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(150) NOT NULL,
 category VARCHAR(80) NOT NULL,
 description TEXT NULL,
 price DECIMAL(12,2) NOT NULL,
 image_url VARCHAR(500) NULL,
 available BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 table_id BIGINT NOT NULL,
 waiter_id BIGINT NOT NULL,
 status ENUM('Open','Completed','Cancelled') NOT NULL DEFAULT 'Open',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(table_id) REFERENCES restaurant_tables(id),
 FOREIGN KEY(waiter_id) REFERENCES employees(id),
 INDEX idx_order_table_status(table_id,status)
);

CREATE TABLE IF NOT EXISTS order_details (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 order_id BIGINT NOT NULL,
 menu_item_id BIGINT NULL,
 item_name VARCHAR(150) NOT NULL,
 quantity INT NOT NULL,
 unit_price DECIMAL(12,2) NOT NULL,
 status ENUM('Pending','Preparing','Ready','Served','Cancelled') NOT NULL DEFAULT 'Pending',
 FOREIGN KEY(order_id) REFERENCES orders(id),
 FOREIGN KEY(menu_item_id) REFERENCES menu_items(id)
);

CREATE TABLE IF NOT EXISTS payments (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 order_id BIGINT NOT NULL,
 amount DECIMAL(12,2) NOT NULL,
 payment_method VARCHAR(50) NOT NULL DEFAULT 'Cash',
 status ENUM('Pending','Successful','Failed') NOT NULL,
 paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES orders(id),
 INDEX idx_payment_status_date(status,paid_at)
);
