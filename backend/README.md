# Backend
Java 21 / Spring Boot 4.1.2. Entry point: src/main/java/com/restaurant/RestaurantApplication.java.

Use run.cmd or verify.cmd at the repository root. The Maven Wrapper is in this directory.

- auth: shared identity, accounts and role protection.
- controllers/services/dto: tables and orders.
- entities/repository: the nine-table shared domain and JPA persistence.
- checkout: menu, checkout and invoices on the same datasource.
- ordermanagement/reporting: read-only reporting queries and DTOs.

The frontend is packaged from ../frontend; SQL scripts are in ../database. Read the root README for MySQL and demo configuration. H2 is test-only.
