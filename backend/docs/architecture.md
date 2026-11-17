# Kiến trúc và quy ước mã

Ứng dụng dùng Java 21, Spring Boot, Thymeleaf, Bootstrap và MySQL.
Một JAR chứa cả backend và frontend. H2 chỉ dùng để kiểm thử.
Docker Compose chạy JAR và MySQL thành hai container; chi tiết trong docker.md.

```text
backend/src/main/java/com/restaurant/
  controller/   Nhận HTTP, form, model và chuyển hướng
  service/      Nghiệp vụ, quyền actor và transaction
  repository/   JPA và truy vấn SQL có tham số
  entity/       9 entity dùng chung, enum trạng thái
  dto/          Dữ liệu form và dữ liệu hiển thị theo nghiệp vụ
  config/       Cấu hình bảo mật, thời gian và tài khoản demo
  security/     Đọc tài khoản đăng nhập
  exception/    Lỗi nghiệp vụ, lỗi không tìm thấy và HTTP error handler
  support/      Ánh xạ ảnh món ăn
```

Mỗi chức năng có controller và service mang tên chức năng, ví dụ
`OrderController`, `OrderService`, `OrderRepository`. Chỉ có một cách hiện
thực service nên không thêm interface `I...` và lớp `...Impl` trung gian.
Các view model báo cáo dùng chung `ReportModels` trong `dto/report`.

Hàm public thể hiện thao tác nghiệp vụ. Hàm private kiểm tra hoặc xử lý một
việc: `lockAvailableTable`, `createDetails`, `requireOpenOrder`,
`requirePendingDetail`, `saveTotal`. Tạo đơn và thêm món dùng chung phần tạo
dòng món; giảm số lượng và hủy dòng dùng chung kiểm tra Pending.

Các thao tác sửa/hủy đơn, chế biến/phục vụ và thanh toán dùng cùng khóa
Orders. Chef chuyển Pending → Preparing → Ready; Waiter chuyển Ready →
Served; Cashier chỉ thanh toán khi các dòng còn hiệu lực đều Served.
Payment, Invoice, Completed và giải phóng bàn nằm trong một transaction.
Tên/giá món đã đặt luôn đọc từ snapshot Order_Detail.

Quy ước theo hướng dẫn Ch4 - Implementation/Programming:

- Tên lớp PascalCase, biến/hàm camelCase, hằng UPPER_SNAKE_CASE.
- UTF-8, 2 dấu cách, formatter Java ở mức 80 ký tự; package/import là ngoại lệ.
- Controller không chứa SQL; service không xử lý chi tiết JDBC.
- Lỗi nghiệp vụ có thông báo sửa được; lỗi hệ thống ghi log và trả thông báo
  trung tính. Không trả nội dung SQL hoặc stack trace ra giao diện.
- Danh sách checkout tổng hợp một lần; danh sách bàn đọc ánh xạ đơn Open
  một lần. Helper JDBC và tìm kiếm literal được dùng chung.
- Mọi thay đổi phải qua `verify.cmd`: định dạng, biên dịch, test và đóng gói.

Định dạng Java sau khi sửa:

```powershell
cd backend
.\mvnw.cmd --no-transfer-progress formatter:format
```

Frontend dùng `.prettierrc.json`; không định dạng hoặc sửa nội dung vendor.
File chạy, cấu hình Git/Maven và giấy phép thư viện là thành phần cần thiết.
