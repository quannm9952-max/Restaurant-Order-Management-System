# Restaurant Order Management System

Hệ thống quản lý nhà hàng: tài khoản, nhân viên, bàn, đặt món, bếp, phục vụ,
thực đơn, thanh toán/hóa đơn, tra cứu và báo cáo doanh thu.

## Mã nguồn và công nghệ

```text
frontend/    HTML, CSS, JavaScript, Thymeleaf, Bootstrap và ảnh/font
backend/     Java 21, Spring Boot, MVC, Security, JPA/JDBC và JUnit
database/    Schema, dữ liệu demo/test và SQL đối soát
```

Docker Compose đóng gói và chạy Spring Boot cùng MySQL 8.4.13. Công nghệ,
nghiệp vụ và giao diện giữ nguyên. Không cần XAMPP, MySQL hoặc JDK cài riêng
để chạy bằng Docker. VS Code/IntelliJ và GitHub vẫn dùng như trước.

## Chạy bằng Docker Compose

Cài và mở [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/),
chọn Linux containers và chờ Docker Engine hoạt động. Trên Windows, chuẩn bị
WSL2 theo hướng dẫn của Docker Desktop. Sau đó mở terminal tại gốc dự án:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
```

Hoặc chạy `run.cmd`; file này tạo `.env` nếu chưa có và gọi cùng lệnh Compose.
Mở **http://localhost:8080** khi log app báo đã khởi động.
Nếu cổng 8080 đang được ứng dụng khác sử dụng, đổi `APP_PORT` trong `.env`.

```powershell
docker compose ps
docker compose logs -f app
docker compose down
```

`down` giữ database trong volume `mysql-data` và ảnh món tải lên trong
`menu-images`. Không dùng `down -v` khi cần giữ dữ liệu. Đổi mật khẩu DB trong
`.env` không tự đổi mật khẩu của volume đã có.

| Actor | Username | Mật khẩu |
| --- | --- | --- |
| Manager | `demo_manager`, `DEMO_FIXTURE_MANAGER` | 123456 |
| Waiter | `demo_waiter`, `DEMO_FIXTURE_WAITER` | 123456 |
| Chef | `demo_chef`, `DEMO_FIXTURE_CHEF` | 123456 |
| Cashier | `demo_cashier`, `DEMO_FIXTURE_CASHIER` | 123456 |

Dùng đúng giá trị cột **Username** trong Employee Management để đăng nhập.
Username dài 1–50 ký tự, chỉ gồm A–Z, a–z, 0–9 và dấu gạch dưới `_`;
không dùng chữ có dấu, khoảng trắng hoặc ký tự đặc biệt. Khi nhập sai định
dạng, ô giữ nguyên nội dung và hiện cảnh báo đỏ để sửa; chỉ gửi khi username
hợp lệ. Quy tắc áp dụng cho đăng nhập và tạo/sửa tài khoản; họ tên nhân viên
vẫn dùng tiếng Việt.
Trang chủ chào **Welcome, [họ tên nhân viên]** theo tên hiện tại đã lưu.
Sai thông tin báo `Incorrect username or password.`; chỉ khi đúng username
và mật khẩu của tài khoản khóa mới báo `Your account has been locked.`
Trường trống được nhắc bằng tiếng Anh ở giao diện và backend.

Thanh toán và xem/in hóa đơn thuộc Cashier. Manager theo dõi doanh thu,
đơn hàng và lịch sử thanh toán qua các module báo cáo của Manager.

Menu Management cho Manager thêm/sửa món và tải ảnh JPG/PNG tối đa 5 MB.
Khi sửa, không chọn ảnh mới sẽ giữ ảnh hiện tại. Nút Delete xóa món khỏi thực
đơn và danh sách đặt món mới, giữ nguyên dữ liệu trong các đơn trước đó.
Chỉ xóa danh mục khi đã chuyển hoặc xóa hết món, kể cả món đang Stop Selling.
Xóa món giữ nguyên danh mục, từ khóa và bộ lọc của danh sách đang xem.

Container app dùng profile demo để tạo schema/dữ liệu còn thiếu; khởi động
lại giữ các thay đổi và mật khẩu đã lưu. `.env` chứa cấu hình local và được
Git bỏ qua. Xem `.env.example` để biết cổng và tài khoản MySQL mẫu.

MySQL Docker mặc định ở **localhost:3308** để không trùng MySQL/XAMPP cũ.
DBeaver kết nối bằng MYSQL_DATABASE/MYSQL_USER/MYSQL_PASSWORD trong `.env`.
Trong mạng Docker, app kết nối `db:3306`.

## Kiểm thử

```powershell
.\verify.cmd
```

Lệnh build stage verify trong Docker, chạy Maven/JUnit/H2 như trước. Có thể
chạy trực tiếp khi máy phát triển đã có JDK 21:

```powershell
cd backend
.\mvnw.cmd --no-transfer-progress verify
```

GitHub Actions chạy kiểm thử Java và kiểm tra khởi động Docker Compose trên
Linux. Image runtime chứa JRE, JAR và SQL; frontend nằm trong JAR. Các file
build, log, `.env` và bản sao lưu DB không được đưa vào image hoặc Git.

- [Hướng dẫn Docker và chuyển dữ liệu cũ](backend/docs/docker.md)
- [Kiến trúc và quy ước](backend/docs/architecture.md)
- [Yêu cầu nghiệp vụ](backend/docs/requirements.md)
- [Kết quả kiểm thử](backend/docs/testing.md)
- [Giao diện](backend/docs/design.md)
- [Thư viện](backend/docs/dependencies.md)
- [Database](database/README.md)
