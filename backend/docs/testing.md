# Kiểm thử hệ thống

Cập nhật: 19/11/2026. Bộ kiểm thử hiện có **364 ca**.

## Chạy kiểm thử

Từ thư mục gốc, mở Docker Desktop rồi chạy:

```powershell
.\verify.cmd
```

Hoặc chạy Maven khi máy có JDK 21:

```powershell
cd backend
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Maven kiểm tra định dạng Java, biên dịch, chạy JUnit và đóng gói JAR.
Các kiểm thử dùng H2 theo profile test, không dùng database MySQL đang chạy.

## Phạm vi tự động

| Lớp kiểm thử                  |   Số ca | Nội dung chính                                                         |
| ----------------------------- | ------: | ---------------------------------------------------------------------- |
| SystemIntegrationTest         |     109 | Tài khoản, giao diện, phân quyền, bảng giá, bàn và các module tích hợp |
| TableServiceTest              |      20 | Trạng thái bàn, đơn Open và điều kiện sử dụng bàn                      |
| CurrentEmployeeServiceTest    |       4 | Nhân viên hiện tại và thông tin phiên                                  |
| OrderServiceTest              |      40 | Tạo/sửa/hủy đơn, số lượng và đơn giá đã lưu                            |
| PreparationIntegrationTest    |      54 | Pending → Preparing → Ready → Served, lịch sử phục vụ và quyền actor   |
| ReportingIntegrationTest      |      46 | Khoảng ngày, nhóm ngày/tuần/tháng, lịch sử và phân trang               |
| ApplicationTest               |       1 | Khởi động ứng dụng                                                     |
| MenuManagementIntegrationTest |      18 | Danh mục, món, ảnh, bộ lọc và rollback                                 |
| PaymentIntegrationTest        |      64 | Cash/Bank Transfer, chỉnh bill, chống trùng, rollback và doanh thu     |
| HttpSecurityTest              |       8 | HTTP thật, đăng nhập, CSRF, upload và xem trước bill                   |
| **Tổng**                      | **364** |                                                                        |

Các luồng thanh toán kiểm tra số tiền theo giá lúc đặt, bỏ dòng Cancelled,
chặn bill cũ, chống tổng giả và chống hai xác nhận đồng thời. Thanh toán lỗi
không để lại Payment, trạng thái đơn hoặc trạng thái bàn dở dang.

Sau Confirm Payment, kiểm thử yêu cầu quay về Payment với đơn vừa thanh toán
và bộ lọc hiện tại, hiển thị thông báo thành công; không tự mở hóa đơn. Gửi
lại xác nhận không tạo thêm giao dịch.

HTTP thật kiểm tra Print Bill bằng form mã hóa URL kèm CSRF, bản xem trước
Unpaid và trường hợp phiên hết hạn. Kiểm thử ảnh bao gồm JPG/PNG, ảnh giả,
file rỗng, giới hạn 5 MB, giữ ảnh cũ và rollback khi cập nhật lỗi.

## Kiểm tra frontend và Docker

- Kiểm tra cú pháp cả năm file JavaScript bằng `node --check`.
- Kiểm tra HTML/CSS/JS bằng cấu hình Prettier của dự án, bỏ qua vendor.
- Kiểm tra khoảng trắng bằng `git diff --check` và cấu hình Docker Compose.
- Kiểm tra Chrome headless ở desktop, tablet và điện thoại: điều hướng theo
  actor, form, phân trang, nhập giá/tiền VND, xem trước bill và hóa đơn.
- Kiểm tra trạng thái Paid trên Payment, thông báo hết phiên, tải lại trang
  và gọi lệnh in. Việc mở bill không tự xác nhận thanh toán.

Đối chiếu checksum đủ 9 bảng trước/sau triển khai và thử chỉ đọc trên bản
chính. Các giao dịch thử được thực hiện trong môi trường kiểm thử; không
thanh toán đơn thật để kiểm tra giao diện.

Giới hạn: giao diện nhỏ được kiểm tra bằng giả lập Chrome; chưa xác nhận trên
iPhone/iPad thật hoặc Safari. Kiểm tra lệnh in và bố cục in không thay thế
kiểm tra máy in vật lý.

## Dữ liệu và file sinh tự động

`database/test-data.sql` là fixture của kiểm thử. `database/demo-data.sql`
là dữ liệu khởi tạo còn thiếu cho profile demo. Giữ các migration và công cụ
đối soát trong database; không chạy lại chuyển VND trên dữ liệu đã chuyển.

JAR, báo cáo test, cache trình duyệt, ảnh kiểm tra và log nằm trong
`backend/target` và không được đưa lên Git hoặc image. `.env`, ảnh tải lên
và bản sao lưu database cũng được Git bỏ qua. Không xóa volume database,
ảnh người dùng hoặc bản sao lưu để dọn mã nguồn.
