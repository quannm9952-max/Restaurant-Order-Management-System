# Restaurant Order Management System

# 1. Functional Requirements

## 1.1. System Overview

### 1.1.1. System Introduction

The **Restaurant Order Management System** is designed to support the daily operations of a dine-in restaurant, including employee management, table management, ordering, food preparation, payment processing, and revenue monitoring.

The system provides four main user roles:

- **Waiter**
- **Chef**
- **Cashier**
- **Manager**

Customers place orders through waiters and do not directly interact with the system.

## 1.2. Account Management

### 1.2.1. Employee Login and Logout

- Employees can log in using their username and password.
- The system verifies employee credentials and grants access according to their assigned roles.
- Employees can log out to terminate their current sessions.

### 1.2.2. Manage Employee Accounts

- Managers can view, create, update, and lock employee accounts.
- Each employee is assigned one of the following roles: Waiter, Chef, Cashier, or Manager.
- Usernames must be unique.

## 1.3. Table Management

### 1.3.1. View Tables

- Waiters and managers can view restaurant tables and their current statuses:
  - Available
  - Occupied
  - OutOfService

### 1.3.2. Manage Tables

- Managers can add, update, and deactivate tables.
- The system automatically:
  - Changes a table's status to **Occupied** when an order is created.
  - Changes a table's status to **Available** when an order is completed or cancelled.
- A table with an Open order cannot be:
  - Marked as Available.
  - Marked as OutOfService.
  - Deactivated.
- The system preserves historical order information when a table is deactivated.

## 1.4. Menu Management

### 1.4.1. Manage Menu

- Managers can add, update, and deactivate menu items.
- Waiters can select only dishes that are available for sale.
- Changes to dish prices do not affect previously created orders.
- Deactivated dishes remain in historical order records.

## 1.5. Order Management

### 1.5.1. Create Order

- When customers place orders, waiters:
  - Select an available table.
  - Choose dishes from the menu.
  - Enter the required quantities.
- The system records order information, including selected dishes, quantities, and unit prices at the time of ordering.
- After successful creation, the system:
  - Assigns the order an **Open** status.
  - Sets new order items to **Pending**.
  - Changes the table status to **Occupied**.
- Each table can have only one Open order at a time.

### 1.5.2. Update Order

- Waiters can add dishes to an existing Open order.
- Waiters can reduce quantities or cancel order items only when those items have a **Pending** status.
- The system updates the order information after valid modifications.

### 1.5.3. Cancel Order

- Waiters can cancel an entire order only when:
  - No order items have started being prepared.
  - No successful payment has been recorded.
- After cancellation, the system:
  - Changes the order status to **Cancelled**.
  - Changes the table status to **Available**.
- Cancelled order information is retained for future reference.

## 1.6. Food Preparation Management

### 1.6.1. Update Preparation Status

- Chefs can view order items that need to be prepared.
- Chefs can update individual order item preparation statuses.
- The system supports the following preparation process:
  1. **Pending**
  2. **Preparing**
  3. **Ready**

### 1.6.2. Confirm Dish Serving

- After delivering dishes to customers, waiters can confirm that the dishes have been served.
- The system changes an order item's status from **Ready** to **Served** when its associated order is still Open.

## 1.7. Payment Management

### 1.7.1. Generate Bill

- When customers request payment, cashiers select the corresponding order to generate a bill.
- The system allows bill generation only when:
  - The order is **Open**.
  - All non-cancelled order items have been served.
- The system calculates the total amount using recorded unit prices and quantities, excluding cancelled items.
- The bill displays:
  - Dish names.
  - Quantities.
  - Unit prices.
  - Subtotals.
  - Total amount due.

### 1.7.2. Process Payment

- Cashiers can record payment after confirming that the full amount has been received from the customer.
- The system records payment transactions, including:
  - Transaction ID.
  - Order ID.
  - Payment amount.
  - Transaction timestamp.
  - Payment status.
- Failed payments are recorded and can be retried while the order remains Open.
- After successful payment, the system:
  - Changes the order status to **Completed**.
  - Changes the table status to **Available**.
- Each order can have only one successful payment transaction.

### 1.7.3. Print Invoice

- After successful payment, cashiers can generate and print an invoice containing:
  - Order information.
  - Dish details.
  - Quantities.
  - Unit prices.
  - Total amount.
  - Payment timestamp.
- Cancelled items are excluded from the invoice.
- The system stores invoice information for future lookup and reconciliation.

## 1.8. Revenue Monitoring

### 1.8.1. View Revenue Reports

- Managers can view restaurant revenue reports based on recorded successful payments and completed orders.


# 2. Non-Functional Requirements

## 2.1. Usability
The system provides a simple and intuitive interface for Waiters, Chefs, Cashiers, and Managers. Functions are organized according to user roles, with clear displays of tables, orders, dishes, and preparation statuses.

## 2.2. Performance
The system responds quickly to common operations, including viewing tables, creating orders, checking order details, and updating dish statuses. The target response time is no more than 3 seconds.

## 2.3. Security
The system requires user authentication and role-based access control.

- **Waiter:** Manage tables and orders.
- **Chef:** View and update dish preparation statuses.
- **Cashier:** Process payments and print invoices.
- **Manager:** Manage accounts, menus, tables, orders, and revenue reports.

Passwords must be securely hashed, and sensitive operations must be protected.

## 2.4. Reliability
The system maintains data consistency between tables, orders, dishes, and payments. Database transactions are used to prevent incomplete updates.

Key rules:
- Each table can have only one open order.
- Dishes cannot be canceled after preparation starts.
- Payment is allowed only when all non-canceled dishes are served.
- Successful payment closes the order and frees the table.
- Each order can have only one successful payment.

## 2.5. Availability
The system must operate during restaurant business hours. Regular database backups and a recovery plan are required to protect business data.

## 2.6. Maintainability
The source code is organized into independent modules, including Account, Table, Order, Preparation, Payment, and Revenue Report. Business logic is separated from data access to simplify maintenance and future development.

# 3. System Architecture Design

The system uses a 3-layer architecture to separate responsibilities and simplify development and maintenance.

## 3.1. Presentation Layer
Provides the user interface for Waiters, Chefs, Cashiers, and Managers.

## 3.2. Business Logic Layer
Processes business operations, validates data, manages user permissions, and enforces restaurant business rules.

## 3.3. Data Access Layer
Handles database operations, including storing and retrieving accounts, tables, orders, dishes, and payment information.

## 3.4. Architecture Overview

```text
PRESENTATION LAYER
        |
        v
BUSINESS LOGIC LAYER
        |
        v
DATA ACCESS LAYER
        |
        v
DATABASE
```

# 4. Technologies Used

| Component | Technology |
|---|---|
| Frontend | HTML, CSS, JavaScript, Thymeleaf, Bootstrap |
| Backend | Java 21 (OpenJDK), Spring Boot 4.1.2, Spring Security |
| Database | MySQL Community; DBeaver Community for inspection |
| Development IDE | Visual Studio Code |
| Version Control | Git and GitHub |
| Architecture | MVC; Controller / Service / Repository, shared entities, DTOs, validation |
