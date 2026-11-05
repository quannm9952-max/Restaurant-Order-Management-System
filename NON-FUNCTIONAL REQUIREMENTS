# 2. Non-Functional Requirements

## 2.1. Usability

### Requirement Description

The restaurant ordering management system must provide a user-friendly, intuitive, and easy-to-use interface for the following user groups: Waiters, Chefs, Cashiers, and Managers. Functions must be clearly arranged and suited to each role’s responsibilities. Table information, orders, dishes, and preparation statuses must be displayed clearly to reduce confusion.

### Evaluation Criteria

The interface has clearly labeled functions; functions are appropriate for each role; error messages explain the cause or provide guidance on how to resolve the issue where possible; table, order, and dish statuses are displayed accurately.

### Testing Method

Ask representatives of each role to perform basic tasks such as viewing tables, creating orders, updating dish statuses, and processing payments. Record operational errors and information that causes confusion.

## 2.2. Performance

### Requirement Description

The system must respond quickly to common operations such as viewing the table list, creating an order, viewing order details, and updating preparation statuses. The target response time is no more than 3 seconds in the defined test environment.

### Evaluation Criteria

The target response time is no more than 3 seconds under agreed testing conditions; results are accurate and contain no missing data; conflicts are minimized when multiple employees operate simultaneously; duplicate orders or transactions caused by repeated actions are minimized.

### Testing Method

Measure response times when viewing tables, creating orders, viewing order details, and updating dish statuses. Test with the specified data and number of concurrent users, and record response times and errors.

## 2.3. Security

### Requirement Description

The system must protect account information, orders, payments, and business data. Users must log in before performing business functions and must be assigned permissions according to their roles. Passwords must be stored using a secure hashing algorithm, not as plain text; access permissions must be checked in the business-logic layer.

### Planned Role-Based Access

- **Waiter:** View/update table statuses, create and update orders, and confirm serving.
- **Chef:** View dishes awaiting preparation and update preparation statuses.
- **Cashier:** Generate bills, process payments, and print invoices.
- **Manager:** Manage accounts, tables, menus, orders, and revenue reports.

### Evaluation Criteria

Unauthenticated users cannot perform functions that require authentication; users cannot access functions beyond their permissions; passwords are not stored as plain text; input data is validated; sensitive operations are controlled.

### Testing Method

Test login with valid and invalid credentials; verify each role and attempt to access unauthorized functions; inspect password storage and the handling of invalid input data.

## 2.4. Reliability

### Requirement Description

The system must ensure consistency among table data, orders, order details, and payments. Important operations should use database transactions where appropriate to prevent partial data updates when errors occur.

### Related Business Rules

- A table can have at most one open order.
- A dish’s quantity may only be reduced or the dish canceled if preparation has not started.
- An entire order may only be canceled if preparation has not started for any dish and no payment has been made.
- Payment is allowed only when all non-canceled dishes have been served.
- Successful payment closes the order and frees the table.
- Each order can have at most one successful payment transaction.

### Evaluation Criteria

No table has two open orders; the total amount matches the order details and the unit prices at the time the order was placed; order, table, and payment statuses are consistent; failed operations do not leave incomplete data; there are no two successful payment transactions for the same order.

### Testing Method

Test duplicate order creation, cancellation of a dish whose preparation has started, payment while dishes remain unserved, and repeated payment. Simulate data-write failures to verify rollback and the final data state.

## 2.5. Availability

### Requirement Description

The system must be available during the restaurant’s operating hours to support order-taking, food preparation, and payment. The database must have a periodic backup and recovery plan for incidents.

### Evaluation Criteria

Core functions are usable during the defined operating period; a backup procedure and responsible person are designated; backup data can be restored in a test environment; a response plan exists for server or database failures.

### Testing Method

Check system startup and use over a specified period; verify database connectivity; simulate incidents in a test environment and restore from backups. The availability rate and recovery time must be determined according to actual deployment conditions.

## 2.6. Maintainability

### Requirement Description

The source code must be organized into separate business components, such as account, table, order, preparation, payment, and revenue-report management. Business-logic classes must be appropriately separated from the data layer. Variable, method, and class names must be clear and consistent.

### Evaluation Criteria

The source code has a consistent structure; functions have clear responsibilities and limited duplication; changing one function does not unnecessarily affect others; important functions can be tested independently where appropriate; installation and user documentation are available.

### Testing Method

Review the source-code structure and degree of duplication; make a small change to one function and rerun related tests to identify effects on other functions.

## 2.7. Summary

Non-functional requirements define the quality standards that the restaurant ordering management system must meet. The six categories, usability, performance, security, reliability, availability, and maintainability, are closely related. Defining and testing these requirements gives the team a basis for system design, technical solution selection, and quality evaluation.
