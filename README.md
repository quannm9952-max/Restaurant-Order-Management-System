# Restaurant Order Management System

Hệ thống quản lý nhà hàng: tài khoản, nhân viên, bàn, đặt món, bếp, phục vụ,
thực đơn, thanh toán/hóa đơn, tra cứu và báo cáo doanh thu.

## Ba phần mã nguồn

```text
frontend/    HTML, CSS, JavaScript, Thymeleaf, Bootstrap và ảnh/font
backend/     Java 21, Spring Boot, MVC, Security, JPA/JDBC và JUnit
database/   Schema, dữ liệu demo, dữ liệu test và SQL đối soát
```

`run.cmd` chạy ứng dụng; `verify.cmd` kiểm thử và đóng gói.
Tài liệu kỹ thuật hiện tại nằm trong `backend/docs`.

## Chạy trên Windows

Cần OpenJDK 21 và MySQL Community. Mở PowerShell ở gốc dự án:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/restaurant_order_management_demo?createDatabaseIfNotExist=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "mat_khau_mysql_tren_may_ban"
.\run.cmd
```

Nếu MySQL local không có mật khẩu, đặt `$env:DB_PASSWORD = ""`.
Điều chỉnh host/cổng/database trong DB_URL cho máy đang sử dụng.
Mở **http://localhost:8080**. Đổi cổng bằng `$env:PORT = "8081"`.
Không cần Node.js để chạy giao diện.

| Actor | Tài khoản demo | Mật khẩu |
| --- | --- | --- |
| Manager | demo_manager | 123456 |
| Waiter | demo_waiter | 123456 |
| Chef | demo_chef | 123456 |
| Cashier | demo_cashier | 123456 |

`run.cmd` chọn profile demo mặc định. Profile này tạo schema/dữ liệu còn thiếu;
khởi động lại giữ các thay đổi và mật khẩu đã lưu. Profile default không nạp
SQL hay tạo tài khoản. Có thể cấu hình `DEMO_PASSWORD` trước lần tạo tài khoản
đầu tiên. Mật khẩu được lưu bằng BCrypt.

## Kiểm tra

```powershell
.\verify.cmd
```

Kiểm thử dùng H2 riêng. Thư mục `backend/target` và log là kết quả sinh ra,
được Git bỏ qua. Các ca kiểm tra nghiệp vụ, quyền, rollback và đồng thời nằm
trong `backend/src/test/java`.

- [Kiến trúc và quy ước](backend/docs/architecture.md)
- [Yêu cầu nghiệp vụ](backend/docs/requirements.md)
- [Kết quả kiểm thử](backend/docs/testing.md)
- [Giao diện](backend/docs/design.md)
- [Thư viện](backend/docs/dependencies.md)
- [Database](database/README.md)
