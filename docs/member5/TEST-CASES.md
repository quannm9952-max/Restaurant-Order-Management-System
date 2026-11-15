# Test case và kết quả phần thành viên 5

Ngày kiểm tra: 15/11/2026, múi giờ Asia/Bangkok. Phạm vi là UC10–UC12 và profile demo độc lập; chưa kiểm thử xuyên suốt với code của TV1–TV4.

## Kết quả tự động

`verify-tv5.cmd`: **39 test, 0 failure, 0 error, 0 skipped**, BUILD SUCCESS. Gồm 32 trường hợp của ReportingIntegrationTests, 6 trường hợp của ReportingDemoSecurityTests và 1 contextLoads. Test dùng H2 riêng ở chế độ tương thích MySQL.

| Mã | Trường hợp | Kết quả kỳ vọng | Kết quả |
| --- | --- | --- | --- |
| TV5-01 | Doanh thu 29/10–04/11/2026 | 24 Successful, 1,892.00 USD, 4 giao dịch bị loại; không nhân tiền theo số dòng món | PASS |
| TV5-02 | Nhóm ngày/tuần | Ngày 196/248/312/284/338/330/184; tuần 1,040/852; tổng không đổi | PASS |
| TV5-03 | Tuần bị cắt bởi khoảng chọn | Chỉ lấy payment trong khoảng; nhãn tuần bắt đầu thứ Hai | PASS |
| TV5-04 | Ranh giới ngày và ngày tạo đơn khác ngày thanh toán | Bao gồm 23:59:59 ngày cuối, loại 00:00:00 ngày kế; dùng payment_time | PASS |
| TV5-05 | Dữ liệu âm tính: Successful gắn với Cancelled | Không cộng vào doanh thu; vẫn giữ trong lịch sử để đối soát | PASS |
| TV5-06 | Lịch sử, tab trạng thái và phân trang | 28 bản ghi, 24/2/2 theo trạng thái, 8 dòng/trang, trang cuối 4 dòng | PASS |
| TV5-07 | Tìm ORD-1042 | Hiện PAY-2046 Successful và PAY-2045 Failed; không cộng Failed | PASS |
| TV5-08 | Tìm chuỗi chứa dấu nháy, `%`, `_` | Tham số SQL được bind và ký tự wildcard được xử lý như dữ liệu | PASS |
| TV5-09 | UC10 tìm/lọc đơn | Có Open/Completed/Cancelled, tìm theo mã đơn, lọc Cancelled đúng | PASS |
| TV5-10 | Đổi menu sau khi tạo đơn | Chi tiết giữ tên và giá lúc đặt; ba dòng ORD-1042, tổng hợp lệ 78.00 | PASS |
| TV5-11 | Khoảng không có giao dịch | Tổng 0, danh sách/biểu đồ rỗng, thông báo không có dữ liệu | PASS |
| TV5-12 | Seed chạy lại | Giữ 28 payment, 24 Successful và tổng 1,892.00 | PASS |
| TV5-13 | Render các view chính và ba biến thể chi tiết payment | HTTP 200, HTML hợp lệ được render bởi Thymeleaf, có nhãn Read-only | PASS |
| TV5-14 | Ngày sai, khoảng ngược, enum sai, trang âm hoặc sai kiểu | HTTP 400 trước khi truy vấn không hợp lệ | PASS |
| TV5-15 | Đơn/payment không tồn tại | HTTP 404 với thông báo | PASS |
| TV5-16 | Waiter/Chef/Cashier/USER truy cập trực tiếp các route | HTTP 403 ở backend | PASS |
| TV5-17 | Non-Manager gọi service trực tiếp | AccessDeniedException | PASS |
| TV5-18 | Người chưa đăng nhập | Yêu cầu xác thực; profile demo chuyển tới trang login | PASS |
| TV5-19 | Gửi POST với CSRF token hợp lệ vào route chỉ đọc | HTTP 403, không thay đổi dữ liệu | PASS |
| TV5-20 | Ghi chú có nội dung HTML/script | Thymeleaf escape nội dung, không render script do dữ liệu cung cấp | PASS |
| TV5-21 | Đăng nhập thật bốn tài khoản demo qua encoder | Xác thực đúng vai trò bằng BCrypt có tiền tố `{bcrypt}` | PASS |
| TV5-22 | Mật khẩu demo sai | Từ chối xác thực, chuyển login?error | PASS |
| TV5-23 | CSS/font/icon công khai của profile demo | Tải được mà không cần đăng nhập | PASS |

Một số hàng bao gồm nhiều biến thể tham số, vì vậy số hàng trong bảng khác số test do JUnit báo cáo. Báo cáo chạy được tạo lại trong `backend/target/surefire-reports`.

## Kiểm tra HTTP và database thực tế

Đã chạy JAR với `tv5-demo` trên cổng 8081 và database riêng `restaurant_order_management_tv5_demo` ở **MariaDB 10.4.32/XAMPP** qua MySQL Connector/J. Không dùng database nghiệp vụ gốc để nạp dữ liệu.

- Database có đúng 9 bảng; Pending 2 giao dịch, tổng 86.00; Successful 24 giao dịch, tổng 1,892.00; Failed 2 giao dịch, tổng 122.00.
- Đăng nhập Manager qua form/CSRF thành công; doanh thu ngày và tuần đúng bộ đối soát.
- Danh sách lịch sử với trạng thái/tìm mã đơn, trang cuối, chi tiết Successful/Pending/Failed, chi tiết đơn, khoảng rỗng và tìm kiếm rỗng đều HTTP 200.
- Khoảng ngày ngược HTTP 400; payment không có HTTP 404.
- Đăng nhập riêng Waiter/Chef/Cashier rồi truy cập báo cáo đều HTTP 403.
- 32 tài nguyên cục bộ gồm CSS, JavaScript, 24 SVG và 4 font được kiểm tra: HTTP 200 và nội dung khác rỗng.
- `node --check frontend/static/js/reporting.js` kiểm tra cú pháp JavaScript thành công.

Các kiểm tra HTTP và quan sát Chrome bên dưới đã thực hiện trên bản chạy trước khi phân loại thư mục. Báo cáo log trong target được giữ ở bản làm việc cũ, không chép vào bản chuẩn bị Git. Sau khi phân loại, chạy lại bộ test qua `verify-tv5.cmd` để kiểm tra đường dẫn resource và script SQL.

## Kiểm tra giao diện trực tiếp trên Chrome

Đã đăng nhập bằng tài khoản demo Manager qua giao diện, mở và quan sát trực tiếp các màn hình sau:

- Báo cáo doanh thu với ba thẻ số liệu, biểu đồ theo ngày và danh sách payment gần nhất.
- Lịch sử thanh toán với các tab trạng thái, ô tìm kiếm, bảng và nút xem chi tiết.
- Hộp thoại PAY-2046 Successful với số tiền, trạng thái, thông tin giao dịch và nhãn chỉ đọc.
- Chi tiết ORD-1042 với ba dòng món, dòng Cancelled bị loại khỏi tổng và hai lần thanh toán liên quan.
- Danh sách tra cứu đơn với ô tìm kiếm, bộ lọc trạng thái và liên kết chi tiết.

Đã xem bố cục ở cửa sổ Chrome thường và khi phóng to. Đây là xác nhận các màn hình có giao diện chạy thực tế; chưa thay thế việc đối chiếu đầy đủ từng frame Figma và kiểm thử tất cả thao tác JavaScript.

## Các phần còn chờ kiểm chứng

- Chạy bộ kiểm tra trên MySQL Community 8.0+ theo yêu cầu tài liệu; MariaDB không được ghi nhận thay cho kết quả MySQL Community.
- Hoàn tất đối chiếu từng frame Figma và kiểm thử responsive, focus, lịch chọn ngày, Cancel/Escape, các biến thể Pending/Failed và các tình huống rỗng trên trình duyệt.
- Đọc/đối chiếu đầy đủ frame UC10 khi Figma MCP có lại quota.
- Các kịch bản INT-01–INT-10 trong [INTEGRATION-CHECKLIST.md](INTEGRATION-CHECKLIST.md) khi nhận đủ bài các thành viên khác.

Các mục này chưa được đánh dấu PASS; không dùng kết quả H2/MockMvc để khẳng định giao diện hoặc toàn bộ hệ thống đã nghiệm thu.
