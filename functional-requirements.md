# Restaurant Order Management System

# 1. Functional Requirements

## 1.1. System Overview

### 1.1.1. System Introduction

The **Restaurant Order Management System** is a management system designed to support the restaurant's operational processes, including order taking, food preparation, customer service, payment processing, and business management.

The system allows waiters to manage table statuses, receive food orders, and create orders for customers. Chefs can receive lists of dishes that need to be prepared and update their preparation progress. Cashiers can review order information, calculate bills, record payments, and print invoices. Restaurant managers can manage employee accounts, tables, menus, and monitor revenue.

The system stores and updates order information throughout the entire service process. Using this system helps different departments coordinate more effectively, minimizes errors during order processing, and improves customer service quality.

### 1.1.2. System Objectives

The system is developed to achieve the following objectives:

- Optimize the processes of ordering, food preparation, and customer service within the restaurant.
- Reduce errors during order recording and processing.
- Support coordination between waiters, chefs, and cashiers through order information updates.
- Allow employees to monitor table statuses and the preparation progress of dishes within orders.
- Support the management of menus, employee accounts, and restaurant business operations.
- Ensure accuracy in payment processing, data storage, and revenue statistics.

### 1.1.3. System Scope

The system is deployed within a single restaurant and serves customers who dine on the premises. Customers place orders through waiters and do not directly interact with the system.

The system supports the following main functions:

- Employee account management.
- Table management.
- Order management.
- Food preparation management.
- Payment management.
- Menu management.
- Revenue monitoring.

The system does not include online table reservations, home delivery, ingredient inventory management, employee payroll management, or customer self-ordering through QR codes.

### 1.1.4. System Actors

The system has four main actors, corresponding to the roles involved in restaurant operations.

| Actor | Description |
|---|---|
| **Waiter** | Manages table statuses, creates and updates orders, and confirms when dishes have been served. |
| **Chef** | Receives lists of dishes that need to be prepared and updates the preparation status of each dish. |
| **Cashier** | Reviews orders, generates bills, records payments, and prints invoices. |
| **Manager** | Manages employee accounts, tables, menus, monitors orders, and views revenue reports. |

---

## 1.2. Account Management

The system provides account management functions to authenticate users and control access permissions according to their respective roles within the restaurant.

### 1.2.1. Login

- Employees enter their username and password to access the system.
- The system verifies the login credentials and the account's active status.
- If the credentials are valid and the account is active, the system grants the employee access to functions corresponding to their assigned role.
- If the employee enters incorrect credentials or the account has been locked, the system displays an error message and rejects the login request.

### 1.2.2. Logout

- Employees can select the logout function when they finish their working session.
- When an employee requests to log out, the system terminates the current login session.
- After successfully logging out, the system redirects the user to the login screen.

### 1.2.3. Manage Employee Accounts

- The restaurant manager can view the employee list, create new accounts, update account information, and lock employee accounts.
- When creating a new account, the manager enters employee information and selects the corresponding role: **Waiter, Chef, Cashier, or Manager**.
- The system validates the information and ensures that the username is unique before saving the account.
- The manager can update employee information, change roles, or modify account statuses.
- When the manager locks an account, the employee is no longer allowed to use that account to log in.
- The system stores account information and applies access permissions according to the assigned role.

---

## 1.3. Table Management

The system supports waiters and restaurant managers in monitoring table information, updating occupancy statuses, and managing the restaurant's table list.

### 1.3.1. View Tables

- Waiters and managers can view the list of tables in the restaurant.
- The system displays identification information and the current occupancy status of each table.
- The system classifies table statuses into:
  - **Available:** The table is empty.
  - **Occupied:** The table is currently in use.
  - **OutOfService:** The table is unavailable for use.
- Waiters can use table statuses to identify which tables are available and which are currently serving customers.

### 1.3.2. Update Table Status

- Waiters can update table statuses according to actual usage conditions and the system's business rules.
- When a waiter creates a new order for a table, the system automatically changes the table status from **Available** to **Occupied**.
- When an order is successfully paid for or validly cancelled, the system closes the order and changes the table status to **Available**.
- The system does not allow waiters to change a table's status from **Occupied** to **Available** if the table still has an order with an **Open** status.
- The system does not allow new orders to be created for tables with an **OutOfService** status.
- Before updating a table's status, the system verifies the conditions for the change to ensure consistency between table statuses and order statuses.

### 1.3.3. Manage Tables

- The restaurant manager can add new tables, update table information, or deactivate tables.
- When adding a new table, the manager enters the table identification information and requests the system to save the data.
- The system validates the information and ensures that the table ID is unique.
- When the manager updates table information, the system validates the data before saving the changes.
- The system only allows the manager to change a table's status to **OutOfService** when the table has no open orders.
- The system does not allow a table with an **Open** order to be deactivated.
- When a table is deactivated, the system preserves all related order and payment history.

---

## 1.4. Order Management

### 1.4.1. Create Order

- When a customer requests to place an order, the waiter selects the table being served in the system.
- The system checks the table's status and determines whether it already has an open order.
- The system only allows a new order to be created when the table has an **Available** status and no **Open** orders.
- If the table meets the requirements, the waiter selects dishes that are currently available for sale from the menu and enters the corresponding quantities.
- The system validates the selected dishes and ensures that the ordered quantities are greater than zero.
- If the information is valid, the system records the order, including:
  - Order ID.
  - Table being served.
  - Employee who created the order.
  - Creation timestamp.
  - List of ordered dishes.
  - Quantities.
  - Unit prices at the time the order was placed.
- After successfully creating the order, the system sets:
  - Order status to **Open**.
  - Status of newly added order items to **Pending**.
  - Table status to **Occupied**.
- If the table already has an **Open** order, the system rejects the request to create another order and requires the waiter to update the existing order instead.
- If the table has an **OutOfService** status, the system rejects the order creation request and displays an appropriate notification.

### 1.4.2. Update Order

- The waiter selects the **Open** order that needs to be updated.
- The system displays the list of order items, including dish names, quantities, unit prices, and preparation statuses.
- The waiter can add new dishes to the order as long as the order has not been paid for.
- The system checks the dish's availability for sale and validates the quantity before recording the new item.
- The waiter can reduce quantities or cancel an order item if that item currently has a **Pending** status.
- The system does not allow quantities to be reduced or order items to be cancelled if their statuses are **Preparing, Ready, or Served**.
- If the same dish appears in multiple order items with different statuses, the system only allows modifications to items with a **Pending** status.
- When the waiter reduces a quantity, the system checks that the remaining quantity is greater than zero. If the waiter wants to remove the entire quantity of an order item, the system performs the item cancellation function and changes its status to **Cancelled**.
- After receiving a valid update request, the system saves the changes and updates the order information.
- If the request is invalid, the system rejects the operation and retains the previous data.

### 1.4.3. Cancel Order

- The waiter selects the order to be cancelled and submits a cancellation confirmation request.
- The system checks the order status, the statuses of its order items, and the related payment information.
- The system only allows an entire order to be cancelled when:
  - The order has an **Open** status.
  - No dishes have started being prepared.
  - No successful payment transaction has been recorded.
- If the order meets the cancellation requirements, the system changes the status of all active order items to **Cancelled** and changes the order status to **Cancelled**.
- After successfully cancelling the order, the system changes the corresponding table status to **Available**.
- If the order does not meet the cancellation requirements, the system rejects the request and displays the reason.
- The system continues storing cancelled order information for historical lookup and management purposes.

---

## 1.5. Food Preparation Management

### 1.5.1. Update Cooking Status

- The chef selects the order item whose preparation status needs to be updated.
- When preparation begins, the chef changes the order item's status from **Pending** to **Preparing**.
- When preparation is completed, the chef changes the order item's status from **Preparing** to **Ready**.
- The system only allows chefs to perform valid status transitions according to the food preparation workflow.
- The system does not allow chefs to start preparing cancelled order items or items belonging to orders that are no longer in the **Open** status.
- The system tracks the preparation status of each order item individually. Therefore, a single order can contain multiple items with different statuses.
- After a successful update, the system saves the new status so that waiters can monitor preparation progress.

### 1.5.2. Confirm Order Served

- When the chef finishes preparing a dish, the system records the order item's status as **Ready**.
- After bringing the dish to the customer's table, the waiter selects the function to confirm that the dish has been served.
- The system checks the order item's status and the order status before processing the request.
- The system only allows the order item's status to change from **Ready** to **Served** when the order has an **Open** status.
- If the order item does not have a **Ready** status or the order is no longer **Open**, the system rejects the serving confirmation request.
- After successful confirmation, the system saves the **Served** status for order monitoring and payment eligibility verification.

---

## 1.6. Payment Management

### 1.6.1. Generate Bill

- When a customer requests payment, the cashier selects the table or order that needs to be billed.
- The system retrieves the order information and its corresponding order items.
- The system checks the order status and the status of each order item.
- The system only allows a bill to be generated for payment when:
  - The order has an **Open** status.
  - All non-cancelled order items have a **Served** status.
- The system calculates the total amount based on the quantities and unit prices recorded at the time the order was placed for all non-cancelled order items.
- The system excludes the values of order items with a **Cancelled** status from the total amount.
- After calculating the amount, the system displays a bill containing:
  - Dish names.
  - Quantities.
  - Unit prices.
  - Subtotals.
  - Total amount due.
- If the order does not meet the payment requirements, the system displays a notification and does not allow payment processing to continue.

### 1.6.2. Process Payment

- The cashier selects an order that meets the payment requirements and performs the payment processing function.
- The system checks the order status, the total amount due, and the related transaction history.
- The system only allows payment processing when:
  - The order has an **Open** status.
  - All active order items have a **Served** status.
  - No **Successful** payment transaction has been recorded.
- The cashier checks the amount due and confirms that the full payment has been received from the customer before recording the payment as successful.
- The system records transaction information, including:
  - Transaction ID.
  - Order ID.
  - Amount.
  - Timestamp.
  - Payment status.
- If the payment transaction fails, the system records its status as **Failed** and keeps the order status as **Open**.
- If the payment transaction succeeds, the system records its status as **Successful**, changes the order status to **Completed**, and changes the table status to **Available**.
- The system records the successful payment, closes the order, and releases the table within a single database transaction to ensure data consistency.
- The system allows multiple payment attempts for a single order but accepts a maximum of one **Successful** transaction.
- If an order already has a **Successful** transaction, the system rejects all subsequent requests to record another successful payment.

### 1.6.3. Print Invoice

- After an order has been successfully paid for, the cashier can select the invoice printing function.
- The system checks the order status and confirms that the order has a **Successful** payment transaction.
- The system generates invoice information, including:
  - Invoice ID.
  - Order ID.
  - Table being served.
  - List of dishes.
  - Quantities.
  - Unit prices.
  - Total amount.
  - Payment timestamp.
- The system excludes cancelled order items from the list of items charged on the invoice.
- The system displays the invoice information so that the cashier can review it before printing.
- After the cashier confirms, the system performs the invoice printing function.
- The system stores invoice information for future lookup and payment data reconciliation.

---

## 1.7. Business Rules

### 1.7.1. Business Rules List

| ID | Business Rule |
|---|---|
| **BR01** | Employees must log in and have the appropriate permissions before performing restricted functions. |
| **BR02** | The system allows each table to have a maximum of one **Open** order at any given time. The system does not allow a table with an **Open** order to be changed to **Available** or **OutOfService**. |
| **BR03** | The system allows waiters to add dishes to an **Open** order before payment. |
| **BR04** | The system only allows quantities to be reduced or order items to be cancelled when those items have a **Pending** status. |
| **BR05** | The system only allows an entire **Open** order to be cancelled when no order items have started being prepared and the order has no **Successful** payment transaction. |
| **BR06** | The system must track the preparation status of each order item individually and only allow valid status transitions. |
| **BR07** | The system only allows payment when the order has an **Open** status and all non-cancelled order items have a **Served** status. |
| **BR08** | When payment succeeds, the system must record the **Successful** transaction, change the order status to **Completed**, and release the table within a single database transaction. |
| **BR09** | The system only allows a maximum of one **Successful** payment transaction per order. |
| **BR10** | The system must preserve historical order and payment data when related dishes or tables are deactivated. |

### 1.7.2. State Management Rules

The system manages table, order, order item, and payment statuses according to the established business rules.

**Table and Order Statuses**

- When a waiter creates a new order for an **Available** table, the system changes the order status to **Open** and the table status to **Occupied**.
- While the order remains **Open**, the system does not allow another order to be created for that table or the table to be released.

**Order Item Preparation Statuses**

- The system tracks the preparation status of each order item individually.
- Chefs are only allowed to change statuses from **Pending** to **Preparing** and from **Preparing** to **Ready**.
- Waiters can only confirm the **Served** status for order items that currently have a **Ready** status.

**Order Item Modification Rules**

- The system only allows waiters to reduce quantities or cancel order items that have a **Pending** status.
- If the same dish appears in multiple order items with different statuses, the system only allows modifications to items that have not yet started being prepared.

**Order Cancellation Rules**

- The system only allows an entire order to be cancelled when:
  - The order has an **Open** status.
  - No order items have started being prepared.
  - No **Successful** payment transaction has been recorded.
- After a valid cancellation, the system changes the order status to **Cancelled** and releases the table.

**Payment Rules**

- The system only allows cashiers to process payment when all non-cancelled order items have been served.
- When the cashier confirms that the full payment has been received, the system records the **Successful** transaction, changes the order status to **Completed**, and releases the table.

**Payment Retry Rules**

- The system can store multiple payment attempts for the same order but only allows a maximum of one **Successful** transaction.
- If a payment transaction fails, the order retains its **Open** status so that the cashier can retry the payment.

**Price and Historical Data Rules**

- The system must store the unit price of each order item at the time the order is placed.
- When the manager changes the selling price in the menu, the system must not modify the unit prices recorded in previous orders.
- The system must preserve order history, order details, and payment transactions for lookup, reconciliation, and revenue reporting purposes.
