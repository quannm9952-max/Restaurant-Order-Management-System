# Database

MySQL Community, schema 9 bảng theo ERD của nhóm.

| File | Mục đích |
| --- | --- |
| `schema.sql` | Tạo bảng, khóa và ràng buộc |
| `demo-data.sql` | Dữ liệu demo; chạy lại giữ các bản ghi đang có |
| `test-data.sql` | Fixture/reset dành riêng cho kiểm thử |
| `check.sql` | Truy vấn đối soát doanh thu và trạng thái |
| `migrate-vnd.ps1` | Chuyển mức tiền cũ ×10.000, có backup và chống chạy lặp |

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

## Chuyển dữ liệu cũ sang mức tiền VND

Seed hiện tại đã dùng mức tiền VND: `8.50` cũ thành `85000.00`, hiển thị
`85.000 VND`. **Chỉ database đang giữ mức tiền cũ trước lần chuyển ngày
18/11/2026** cần nhân 10.000. Không chạy công cụ này trên bản cài mới.

Tại gốc dự án trong PowerShell, giữ MySQL chạy và tạm dừng app để tránh
người dùng tạo/sửa đơn trong lúc chuyển:

```powershell
docker compose stop app
powershell -NoProfile -ExecutionPolicy Bypass -File database/migrate-vnd.ps1
docker compose up --build -d app
```

Công cụ sao lưu đầy đủ MySQL rồi chuyển cùng một transaction cho 5 cột:
`Menu_item.unit_price`, `Order_Detail.unit_price_at_order`,
`Orders.total_amount`, `Payment.amount` (mọi trạng thái), `Invoice.total_amount`.
Kiểm tra giới hạn DECIMAL(12,2) trước khi cập nhật; không làm tròn hoặc
thay đổi cấu trúc 9 bảng, mã bản ghi, tài khoản, trạng thái hay phương thức.

Backup `before.sql`, checksum, mốc tiền gốc `before.json`, SQL chuyển và
biên nhận hoàn tất nằm trong `database/backups/vnd-10000-<server>-<database>/`.
Thư mục này được loại khỏi Git và Docker image; giữ riêng cùng backup ảnh.
Giữ nguyên toàn bộ thư mục để lần chạy lại nhận biết đã hoàn tất. Nếu lần
chạy bị ngắt, chạy lại với cùng database/thư mục: SQL chỉ chấp nhận toàn bộ
trạng thái trước hoặc sau chuyển và không nhân lần hai. Dữ liệu lệch hoặc
backup sai checksum sẽ làm công cụ dừng. Không xóa mốc gốc để chạy lại.

Khi cần khôi phục, dừng app, dùng MySQL/DBeaver nhập `before.sql` vào một
database kiểm tra trước. Chỉ thay dữ liệu đang dùng sau khi đối soát; backup
này chứa cả tài khoản nên không đưa lên Git. `check.sql` đối soát tổng tiền
và giữ quy tắc doanh thu Successful + Completed. Fixture gốc 24 thanh toán
trong 29/10–04/11/2026 có tổng **18.920.000 VND**.

Migration `migrations/20261118_vnd_labels.sql` trước đó chỉ sửa mô tả demo
gốc của món 9111–9118 sang VND; không nhân tiền và vẫn chạy lại an toàn.

## Bill xem trước và hóa đơn đã thanh toán

Print Bill cập nhật số lượng và tổng Orders trong transaction có khóa Orders,
không thêm Payment/Invoice. Bản in trước thanh toán ghi Unpaid; in lại/sửa lại
được khi đơn chưa trả. Revision từ dữ liệu chi tiết đơn phát hiện sửa đổi từ
phiên khác; không thêm bảng/cột hoặc migration.

Confirm Payment ghi số lượng cuối, Payment Successful, Orders Completed,
Invoice và giải phóng bàn trong cùng transaction. Chỉ khi này đơn bị khóa
và khoản tiền được tính doanh thu. Invoice chưa trả do phiên bản Generate
Bill trước đây không còn khóa đơn: khi thanh toán sẽ cập nhật đúng tổng,
thời gian phát hành và giữ số hóa đơn cũ. Không cập nhật hóa đơn đã thanh toán.
