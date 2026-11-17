> Historical member-5 handoff before integration. Use [the current README](../../README.md) and [integration notes](../INTEGRATION.md) for startup, account and architecture details.

# Checklist phối hợp và demo của thành viên 5

Checklist này chuẩn bị cho Stage 4. Chưa thực hiện việc tổng hợp hoặc merge bài các thành viên khác.

## Khi nhận đủ module

- [ ] Nhận branch/PR của TV1–TV4 và xác nhận ERD, enum, Figma cuối.
- [ ] TV1 thống nhất `ROLE_MANAGER`, thông tin người đăng nhập và thứ tự filter chain.
- [ ] Tắt `tv5-demo`, dùng dữ liệu/tài khoản MySQL chung do TV1 bàn giao.
- [ ] TV2 thống nhất OrderRepository, cột và trạng thái Orders/Order_Detail; không tạo entity/repository trùng khi ghép.
- [ ] TV4 thống nhất PaymentRepository, các lần thử thanh toán, payment_time, Successful và Completed.
- [ ] Đối chiếu trường Cashier của Figma với ERD cuối; chỉ đổi schema khi nhóm thống nhất và có PR.
- [ ] Chạy database scripts có thứ tự; dùng dữ liệu DEMO/TEST và không đưa mật khẩu thật vào Git.
- [ ] TV1/TV2/TV3/TV4 tự cung cấp kết quả unit test của module mình.
- [ ] Review ít nhất một người khác, sửa xung đột, kiểm tra build trước khi merge.

## Kịch bản xuyên suốt khi đã ghép đủ code

| Mã | Thao tác với các module đã bàn giao | Kết quả cần kiểm tra |
| --- | --- | --- |
| INT-01 | Waiter tạo đơn, Chef Pending → Preparing → Ready, Waiter Served, Cashier Successful, Manager xem báo cáo | Đơn Completed, bàn Available, đúng một payment Successful, doanh thu tăng đúng một lần |
| INT-02 | Chưa thanh toán một đơn Open | Đơn vẫn xem được trong UC10; doanh thu không tăng |
| INT-03 | Cashier ghi nhận Failed, sau đó thử lại Successful | Lịch sử có cả hai lần; chỉ lần Successful vào doanh thu |
| INT-04 | Cashier gửi lại xác nhận Successful | Giao dịch trùng bị chặn; doanh thu không tăng lần hai |
| INT-05 | Waiter hủy đơn hợp lệ trước chế biến | UC10 hiển thị Cancelled; doanh thu không tăng |
| INT-06 | Manager đổi tên/giá/ngừng bán món sau khi đơn đã hoàn tất | Chi tiết đơn vẫn giữ tên và giá lúc đặt; báo cáo cũ không đổi |
| INT-07 | Đơn tạo ngày A nhưng thanh toán ngày B | Doanh thu thuộc B, không thuộc A |
| INT-08 | Thanh toán sát 23:59:59 hoặc 00:00:00 | Bộ lọc ngày không bỏ sót, không đếm trùng ranh giới |
| INT-09 | Waiter/Chef/Cashier truy cập trực tiếp URL báo cáo/chi tiết | HTTP 403, không chỉ ẩn nút |
| INT-10 | Mô phỏng lỗi transaction thanh toán ở TV4 | Rollback đơn/bàn/payment; báo cáo không xuất hiện khoản thanh toán dở dang |

## Trước demo riêng TV5

- [ ] Chạy MySQL Community đúng yêu cầu môi trường và JDK 21.
- [ ] Nạp lại schema/seed vào database demo riêng.
- [ ] Đăng nhập `demo_manager`; kiểm tra các tài khoản vai trò khác bị chặn.
- [ ] UC10 tìm ORD-1042, xem ba dòng món và hai lần thanh toán.
- [ ] UC11 chọn 29/10–04/11/2026: 24 Successful, 4 bị loại, 1,892.00 USD.
- [ ] Chuyển Day/Week: tổng không đổi, tuần 1,040.00 và 852.00.
- [ ] Chọn khoảng không có dữ liệu và khoảng không hợp lệ.
- [ ] UC12 tìm ORD-1042; xem PAY-2045 Failed và PAY-2046 Successful.
- [ ] Kiểm tra các tab trạng thái, phân trang và giữ bộ lọc khi đóng hộp thoại.
- [ ] Đối chiếu màn hình thực tế với từng frame Figma, chụp ảnh bằng chứng và ghi sai khác.
- [ ] Không đánh dấu checklist toàn nhóm đã hoàn thành khi chưa nhận đủ module.
