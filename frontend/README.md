# Frontend

- `templates`: màn hình Thymeleaf theo nghiệp vụ; `shared` chứa phần dùng chung.
- `static/css`: app, login, preparation và reports; vendor giữ nguyên.
- `static/js`: app, orders, payments, preparation và reports.
- `static/images`: icons, ảnh đăng nhập và ảnh món đang dùng.
- `static/fonts`: font và giấy phép OFL.

Frontend được Spring Boot render và đóng gói trong JAR. Không chạy server
frontend riêng. Mở http://localhost:8080 sau khi chạy `run.cmd` ở gốc dự án.
