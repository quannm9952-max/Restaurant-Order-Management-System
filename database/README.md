# Database

MySQL Community, schema 9 bảng theo ERD của nhóm.

| File | Mục đích |
| --- | --- |
| `schema.sql` | Tạo bảng, khóa và ràng buộc |
| `demo-data.sql` | Dữ liệu demo; chạy lại giữ các bản ghi đang có |
| `test-data.sql` | Fixture/reset dành riêng cho kiểm thử |
| `check.sql` | Truy vấn đối soát doanh thu và trạng thái |

Ứng dụng profile demo nạp schema và demo-data. Profile default không tự nạp
SQL. Tài khoản đăng nhập demo do `DemoAccounts` tạo, mật khẩu mặc định 123456.
Không chạy test-data trên database đang sử dụng vì fixture có thể ghi đè dữ liệu.

Sau khi chọn đúng database trong DBeaver/MySQL client, chạy schema.sql rồi
demo-data.sql để chuẩn bị dữ liệu thủ công. Không có bảng Kitchen hoặc Revenue;
bếp/phục vụ dùng Orders/Order_Detail, báo cáo đọc Payment/Orders.
