
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
