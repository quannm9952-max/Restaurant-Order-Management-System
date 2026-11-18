# Database

MySQL Community, schema 9 bảng theo ERD của nhóm.

| File | Mục đích |
| --- | --- |
| `schema.sql` | Tạo bảng, khóa và ràng buộc |
| `demo-data.sql` | Dữ liệu demo; chạy lại giữ các bản ghi đang có |
| `test-data.sql` | Fixture/reset dành riêng cho kiểm thử |
| `check.sql` | Truy vấn đối soát doanh thu và trạng thái |

Ứng dụng profile demo nạp schema và demo-data. Profile default không tự nạp
SQL. `DemoAccounts` kích hoạt một lần 4 tài khoản mẫu `DEMO_FIXTURE_*` chưa
được cấu hình và tạo thêm 4 tài khoản `demo_*`, mật khẩu mặc định 123456.
Cả 8 đăng nhập bằng username; các lần khởi động sau giữ mật khẩu và trạng thái
khóa do Manager chỉnh. Tài khoản mới do Manager tạo cũng dùng cùng luồng đăng nhập.
Không chạy test-data trên database đang sử dụng vì fixture có thể ghi đè dữ liệu.

Sau khi chọn đúng database trong DBeaver/MySQL client, chạy schema.sql rồi
demo-data.sql để chuẩn bị dữ liệu thủ công. Không có bảng Kitchen hoặc Revenue;
bếp/phục vụ dùng Orders/Order_Detail, báo cáo đọc Payment/Orders.

Môi trường Docker Compose dùng MySQL 8.4.13 tại localhost:3308. Dữ liệu
lưu trong named volume; xem `backend/docs/docker.md` để chuyển dữ liệu cũ.

Xóa món/danh mục là cập nhật `is_active`; món bị xóa cũng có
`is_available=false`. Giữ các bản ghi và khóa ngoại của đơn hàng cũ. Chỉ xóa
danh mục khi không còn món có `is_active=true`, kể cả món ngừng bán.
Ảnh tải lên được lưu theo item_id trong volume `menu-images`, không thêm
bảng/cột vào schema 9 bảng. Khi sao lưu hệ thống cần giữ cả database và ảnh.

Thanh toán mới lưu `payment_method` là `CASH` hoặc `BANK_TRANSFER`; schema
hiện có đã đủ, không cần SQL migration. Không backfill/đổi tên/gán Cash cho
giao dịch cũ. Báo cáo nhận cả tên cũ `Cash`/`Bank Transfer`; NULL, trống và
phương thức khác vào nhóm Unknown, còn lịch sử/hóa đơn giữ nhãn cũ.
Ràng buộc `uq_one_successful_payment_per_order` và khóa Orders được tái sử
dụng để chống hai thanh toán thành công. `check.sql` có truy vấn đối soát
nhóm phương thức với cùng khoảng ngày và quy tắc Successful + Completed.
