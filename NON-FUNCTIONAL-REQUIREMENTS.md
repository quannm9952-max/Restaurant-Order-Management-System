2. NON-FUNCTIONAL REQUIREMENTS
2.1 Usability
The system must provide a clear, user-friendly interface for Waiters, Chefs, Cashiers, and Managers. Functions and table/order/dish statuses must be easy to understand.
Test: Ask each role to perform basic tasks and record errors or confusing information.
2.2 Performance
Common operations (view tables, create orders, view details, update dish status) must respond within 3 seconds in the test environment.
Test: Measure response times and check accuracy under the agreed user load.
2.3 Security
Users must log in and receive role-based permissions. Passwords must be securely hashed, and permissions checked in the business-logic layer.
Roles: Waiter manages tables/orders; Chef updates preparation; Cashier handles bills/payments; Manager manages accounts, menus, tables, orders, and reports.
Test: Check login, permissions, password storage, and invalid inputs.
2.4 Reliability
The system must keep table, order, and payment data consistent. Important operations should use database transactions.
Rules: One open order per table; changes/cancellations only before preparation starts; payment only after all active dishes are served; successful payment closes the order and frees the table; one successful payment per order.
Test: Try duplicate orders/payments, invalid cancellations, early payments, and database failures.
2.5 Availability
The system must operate during restaurant hours and have regular database backups and a recovery plan.
Test: Check operation, connectivity, backup restoration, and recovery from simulated failures.
2.6 Maintainability
Code must be modular (account, table, order, preparation, payment, and reports), with business logic separated from data access and consistent naming.
Test: Review code structure, duplication, independent testing, documentation, and the impact of a small change.
2.7 Summary
These six non-functional requirements define the system’s quality standards and guide its design, implementation, and testing.
