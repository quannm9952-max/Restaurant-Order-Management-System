# Backend

Java 21, Spring Boot. Điểm khởi động:
`src/main/java/com/restaurant/RestaurantApplication.java`.

Dockerfile đóng gói cùng mã hiện tại bằng JDK/JRE 21.

Mã chia theo controller, service, repository, entity, dto, config, security,
exception và support. Kiểm thử nằm trong `src/test/java`.

Từ gốc dự án: `run.cmd` chạy Docker Compose, `verify.cmd` kiểm thử trong Docker.
Maven đóng gói template/static từ `../frontend` vào cùng JAR.
SQL nằm trong `../database`; tài liệu hiện tại nằm trong `docs`.
