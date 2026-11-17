# Biên bản tích hợp ngày 17/11/2026

Đã tiếp tục từ checkpoint 148078b và hoàn tất bản tích hợp TV1, TV2, TV4, TV5. TV3 (UC06–UC07) được để tích hợp sau theo yêu cầu.

## Bản hiện tại

- Thư mục làm việc: rom-system-integration; repository gốc restaurant-order-management-git và nhánh TV5 của người dùng được giữ nguyên.
- Nguồn đã ghép: TV1 67725ac, TV2 65b2eee, TV4 f7e5295, TV5 2fb01ea. Lịch sử đóng góp được giữ bằng merge commit.
- Đã giữ cập nhật tài liệu trên origin/main tại 6e6e940; đích bàn giao là main, không force push.
- Một backend Spring Boot 4.1.2 / Java 21, frontend Thymeleaf đóng gói cùng JAR, schema MySQL 9 bảng, entity/repository và đăng nhập dùng chung.
- Đã sửa hai lỗi lúc tạm dừng, lỗi enum TV2, lỗi HTTP 403 khi forward POST và kiểm tra điều kiện Served cho mọi yêu cầu ghi nhận thanh toán.
- Có run.cmd, verify.cmd, hướng dẫn cấu hình MySQL/tài khoản demo trong README.md. Các script TV5 cũ là alias tương thích.

## Xác minh

- Maven Wrapper verify: 139 test đạt, không failure/error/skipped; đóng gói thành công.
- MySQL Community 8.4.13 ở localhost:3307 với database riêng rom_integration_demo; không sửa database XAMPP cũ.
- HTTP: đăng nhập 4 role, 16 trang chính, 36 asset, CSRF/phân quyền/logout, tạo/hủy đơn, CRUD thực đơn và tài khoản.
- Hai Cashier thanh toán đồng thời chỉ tạo một payment thành công và một hóa đơn; đối chiếu SQL xác nhận đơn hoàn tất, bàn được giải phóng.
- Restart không đặt lại đơn đã xử lý, giá/trạng thái món hay tài khoản bị khóa.
- Kiểm tra trực quan bằng công cụ Windows chưa thực hiện được: công cụ đã dừng vì không xác định chắc URL Chrome. Không dùng automation khác để vượt chặn. Không tuyên bố độ khớp Figma đã được nghiệm thu toàn bộ.

## Tài liệu bàn giao

- README.md: chạy, cấu hình MySQL, tài khoản demo và dữ liệu mẫu.
- docs/INTEGRATION.md: kiến trúc, nguồn đóng góp, quyền và hợp đồng để bổ sung TV3.
- docs/VERIFICATION.md: kết quả và giới hạn kiểm chứng.

Không chạy lại script migration của bản checkpoint; script này đã được gỡ khỏi bản bàn giao. Nội dung checkpoint cũ còn trong lịch sử Git tại 148078b.
