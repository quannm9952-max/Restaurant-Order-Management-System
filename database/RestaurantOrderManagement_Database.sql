-- RESTAURANT ORDER MANAGEMENT SYSTEM - GROUP 09
-- MySQL 8.0+ | Schema based on Figure 6.1 (ERD)
-- Run this file first. It creates tables only (no demo records).
CREATE DATABASE IF NOT EXISTS restaurant_order_management
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE restaurant_order_management;

CREATE TABLE IF NOT EXISTS Employee (
  employee_id INT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  phone_number VARCHAR(15),
  email VARCHAR(100),
  position VARCHAR(100),
  hire_date DATE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Account (
  account_id INT AUTO_INCREMENT PRIMARY KEY,
  employee_id INT NOT NULL UNIQUE,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('WAITER','CHEF','CASHIER','MANAGER') NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_account_employee FOREIGN KEY (employee_id) REFERENCES Employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Restaurant_tables (
  table_id INT AUTO_INCREMENT PRIMARY KEY,
  table_name VARCHAR(30) NOT NULL UNIQUE,
  capacity INT NOT NULL,
  status ENUM('AVAILABLE','OCCUPIED','OUT_OF_SERVICE') NOT NULL DEFAULT 'AVAILABLE',
  CONSTRAINT chk_table_capacity CHECK (capacity > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Menu_category (
  category_id INT AUTO_INCREMENT PRIMARY KEY,
  category_name VARCHAR(100) NOT NULL,
  description TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Menu_item (
  item_id INT AUTO_INCREMENT PRIMARY KEY,
  category_id INT NOT NULL,
  item_name VARCHAR(150) NOT NULL,
  description TEXT,
  unit_price DECIMAL(12,2) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  is_available BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT chk_menu_price CHECK (unit_price >= 0),
  CONSTRAINT fk_menu_item_category FOREIGN KEY (category_id) REFERENCES Menu_category(category_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Orders (
  order_id INT AUTO_INCREMENT PRIMARY KEY,
  table_id INT NOT NULL,
  employee_id INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('OPEN','COMPLETED','CANCELLED') NOT NULL DEFAULT 'OPEN',
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  notes TEXT,
  -- MySQL permits multiple NULLs in a UNIQUE key, but one OPEN order per table.
  open_table_id INT GENERATED ALWAYS AS (CASE WHEN status = 'OPEN' THEN table_id ELSE NULL END) STORED,
  UNIQUE KEY uq_one_open_order_per_table (open_table_id),
  INDEX idx_orders_table_status (table_id, status),
  INDEX idx_orders_created_at (created_at),
  CONSTRAINT chk_order_total CHECK (total_amount >= 0),
  CONSTRAINT fk_orders_table FOREIGN KEY (table_id) REFERENCES Restaurant_tables(table_id),
  CONSTRAINT fk_orders_employee FOREIGN KEY (employee_id) REFERENCES Employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Order_Detail (
  order_detail_id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL,
  item_id INT NOT NULL,
  quantity INT NOT NULL,
  unit_price_at_order DECIMAL(12,2) NOT NULL,
  item_name_at_order VARCHAR(150) NOT NULL,
  preparation_status ENUM('PENDING','PREPARING','READY','SERVED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  INDEX idx_order_detail_order (order_id),
  INDEX idx_order_detail_status (preparation_status),
  CONSTRAINT chk_detail_quantity CHECK (quantity > 0),
  CONSTRAINT chk_detail_price CHECK (unit_price_at_order >= 0),
  CONSTRAINT fk_detail_order FOREIGN KEY (order_id) REFERENCES Orders(order_id),
  CONSTRAINT fk_detail_item FOREIGN KEY (item_id) REFERENCES Menu_item(item_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Payment (
  payment_id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL,
  payment_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  amount DECIMAL(12,2) NOT NULL,
  payment_method VARCHAR(30),
  status ENUM('PENDING','SUCCESSFUL','FAILED') NOT NULL DEFAULT 'PENDING',
  transaction_reference VARCHAR(100),
  successful_order_id INT GENERATED ALWAYS AS (CASE WHEN status = 'SUCCESSFUL' THEN order_id ELSE NULL END) STORED,
  UNIQUE KEY uq_one_successful_payment_per_order (successful_order_id),
  INDEX idx_payment_time_status (payment_time, status),
  INDEX idx_payment_order (order_id),
  CONSTRAINT chk_payment_amount CHECK (amount >= 0),
  CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES Orders(order_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Invoice (
  invoice_id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL UNIQUE,
  invoice_number VARCHAR(30) NOT NULL UNIQUE,
  issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total_amount DECIMAL(12,2) NOT NULL,
  CONSTRAINT chk_invoice_amount CHECK (total_amount >= 0),
  CONSTRAINT fk_invoice_order FOREIGN KEY (order_id) REFERENCES Orders(order_id)
) ENGINE=InnoDB;

-- Important: Service layer must enforce allowed status transitions,
-- table release + payment success as one transaction, and order totals.
-- The unique generated columns enforce BR02 and BR09 even under concurrency.
