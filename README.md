# Restaurant Order Management System

Bản tích hợp đủ TV1–TV5, UC01–UC12: đăng nhập, nhân viên, bàn, đơn hàng, bếp, phục vụ, thực đơn, thanh toán/hóa đơn, tra cứu đơn, doanh thu và lịch sử thanh toán. TV3 được triển khai theo UC06–UC07 trong `RequirementAndDesignDocument.docx` và hai màn Figma Kitchen/Serving Management.

## Chạy trên Windows

Cần OpenJDK 21 và MySQL Community đang chạy. Maven Wrapper được cung cấp trong `backend`; không cần cài Node.js để chạy giao diện Thymeleaf.

Mở PowerShell tại thư mục repository:

```powershell
# Thay các giá trị theo MySQL trên máy của bạn; không commit mật khẩu thật.
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "mat_khau_mysql_tren_may_ban"
.\run.cmd
```

Nếu tài khoản MySQL local không có mật khẩu, đặt `$env:DB_PASSWORD = ""`. Mặc định profile `demo` dùng database riêng `restaurant_order_management_demo` ở cổng 3306, tạo schema 9 bảng và dữ liệu giả lập. Tài khoản DB cần quyền tạo database/bảng; có thể tạo database trước bằng MySQL client hoặc DBeaver nếu không cấp quyền tạo database cho ứng dụng.

Nếu dùng cổng hoặc tên database khác:

```powershell
$env:DB_URL = "jdbc:mysql://127.0.0.1:3307/restaurant_order_management_demo?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false"
.\run.cmd
```

Mở **http://localhost:8080**. Có thể đổi cổng ứng dụng bằng `$env:PORT = "8081"`.

| Vai trò | Tài khoản demo |
| --- | --- |
| Manager | `demo_manager` |
| Waiter | `demo_waiter` |
| Cashier | `demo_cashier` |
| Chef | `demo_chef` |

Mật khẩu demo mặc định cho cả bốn tài khoản: **`123456`**. Đây là thông tin thử nghiệm công khai, chỉ tạo trong profile `demo`. Đặt `$env:DEMO_PASSWORD` trước lần tạo tài khoản đầu tiên để dùng mật khẩu thử nghiệm khác. Khởi động lại không đặt lại mật khẩu hoặc thay đổi nghiệp vụ đã lưu; tài khoản đã có có thể đổi qua trang quản lý nhân viên.

`run.cmd` chọn profile `demo` nếu chưa đặt `SPRING_PROFILES_ACTIVE`. Cấu hình mặc định ngoài demo không tự tạo tài khoản hay nạp SQL và dùng database `restaurant_order_management`; có thể đặt `$env:SPRING_PROFILES_ACTIVE = "default"` sau khi chuẩn bị DB/tài khoản theo hướng dẫn triển khai của nhóm.

## Kiểm tra và demo

```powershell
.\verify.cmd
```

Lệnh chạy JUnit, kiểm thử Spring Security, JPA/JDBC, render Thymeleaf và HTTP servlet, rồi đóng gói JAR. Kiểm thử tự động dùng H2 riêng, không ghi vào MySQL đang sử dụng. Chi tiết kết quả MySQL và giới hạn kiểm chứng nằm trong [docs/VERIFICATION.md](docs/VERIFICATION.md).

- Manager quản lý nhân viên/thực đơn/bàn, xem đơn và báo cáo; Waiter tạo/sửa/hủy đơn theo điều kiện và xác nhận phục vụ; Chef cập nhật trạng thái bếp; Cashier thanh toán/in hóa đơn.
- Dữ liệu báo cáo mẫu **29/10–04/11/2026** có **24 thanh toán thành công, 1,892.00 USD**, không tính 4 giao dịch pending/failed. Báo cáo mặc định mở khoảng mẫu; để xem thanh toán mới, chọn khoảng ngày chứa ngày phát sinh.
- Đơn **DEMO 1100**, bàn **DEMO Checkout Table**, có món đã Served và tổng **39.00 USD** để thử checkout độc lập. Thanh toán hợp lệ tạo Payment + Invoice, hoàn tất đơn và giải phóng bàn trong một transaction.
- Luồng đầy đủ: Waiter tạo đơn tại `/orders` → Chef chọn `/kitchen` và chuyển từng dòng `Pending → Preparing → Ready` → Waiter xác nhận `Ready → Served` tại `/serving` → Cashier tạo bill và thanh toán tại `/payments` → Manager xem doanh thu/lịch sử. Mỗi bước kiểm tra quyền và trạng thái phía server.
- Kitchen/Serving tự cập nhật khi dữ liệu thay đổi, kiểm tra mỗi 15 giây; tạm dừng refresh khi mở xác nhận hoặc đang nhập bộ lọc. Waiter nhận thông báo/số dòng Ready trên điều hướng.
- Profile demo nạp `demo-seed.sql` và `demo-extra.sql` theo cách không ghi đè thay đổi khi khởi động lại. `seed.sql` là fixture đối soát dành cho kiểm thử/reset dữ liệu mẫu, không chạy trên DB nghiệp vụ đang dùng.
- Tất cả số tiền demo dùng USD; không phải dữ liệu hoặc tỷ giá của nhà hàng thật.

## Cấu trúc mã nguồn và công nghệ

```text
backend/       Java 21, Spring Boot, Controller / Service / Repository,
               entity dùng chung, DTO, validation, Spring Security và JUnit
frontend/      HTML, CSS, JavaScript, Thymeleaf, Bootstrap, ảnh/font local
database/      Schema 9 bảng, seed kiểm thử, seed demo và truy vấn đối soát
docs/          Ghi chú tích hợp, kiểm thử và đối chiếu yêu cầu UC01–UC12
run.cmd        Build và chạy ứng dụng trên localhost
verify.cmd     Kiểm thử và đóng gói
```

Frontend được Maven đóng gói vào cùng một JAR; không chạy một frontend server riêng. Có thể dùng DBeaver Community để kiểm tra schema/dữ liệu trong MySQL. Xem [docs/INTEGRATION.md](docs/INTEGRATION.md) để biết nguồn đóng góp từng thành viên, quyền và cách các module dùng chung dữ liệu. Xem [docs/member3/REQUIREMENTS.md](docs/member3/REQUIREMENTS.md) để đối chiếu nghiệp vụ TV3 và [docs/member3/TEST-CASES.md](docs/member3/TEST-CASES.md) để xem ca kiểm thử.

## Đối chiếu hướng dẫn lập trình

Đã rà soát theo 47 trang của Ch4 - Implementation/Programming: tách SQL khỏi
service, chuẩn hóa xử lý lỗi, giảm truy vấn lặp, dùng chung helper, bổ sung
kiểm tra định dạng Java và workflow build/test khi push hoặc mở Pull Request.
Xem [bảng đối chiếu](docs/IMPLEMENTATION-REVIEW.md),
[kiểm kê thư viện](docs/DEPENDENCIES.md) và
[kết quả kiểm thử](docs/VERIFICATION.md).

## Tài liệu phân tích và thiết kế

[Sơ đồ DFD](https://drive.google.com/drive/folders/1iH7k0nbfwjVc4JCCPAepSWYFlU3fPa_O?usp=sharing)
## GUI Design

[Figma – Restaurant Order Management System](https://www.figma.com/design/wxeNfF4bp5NJLf0F6t4zgE/Restaurant-Order-Management-%C2%B7-Role-Dashboards?node-id=32-59163&t=I7HCaFg8LCHpffTW-1)

## System Diagrams

- [Use Case Diagram](https://drive.google.com/file/d/1xEsPVNDFSx9nrrUEiUo_d0SA5rIboU4T/view?usp=sharing)
- [Data Flow Diagram](https://drive.google.com/file/d/1aCpwMdF4ImwjvxLUB48WkFEY1pNzS18G/view?usp=sharing)
- [Class Diagram](https://drive.google.com/file/d/1RjFUXmWJW-UTqoGynt741rLAZbVXKEbm/view?usp=sharing)
- [Schema 9 bảng dùng chung](database/schema.sql)

## Project Documentation

[View and Edit Project Document](https://docs.google.com/document/d/1vRZHwOJ2bODHx7DynGO5eAYw5ZaRvFG2yhMB57XTNM8/edit?usp=sharing)
