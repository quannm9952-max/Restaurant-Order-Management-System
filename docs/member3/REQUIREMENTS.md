# TV3 Quản lý bếp và xác nhận phục vụ

Nguồn nghiệp vụ: `RequirementAndDesignDocument.docx` do người dùng cung cấp, mục 1.6, 1.9 (BR01, BR04–BR10), 2.3–2.4, 3.4, 4.4.6 (UC06), 4.4.7 (UC07), và ERD hình 6.1. Nguồn giao diện: hai màn Kitchen Management/Serving Management trong ZIP Figma đã cung cấp; hình 7.5–7.6 trong Word dùng cùng bố cục.

## Luồng và quyền

| Actor | Đọc | Cập nhật |
| --- | --- | --- |
| Chef | Đơn Open có dòng Pending/Preparing/Ready; các dòng khác của cùng đơn chỉ để tham khảo | Pending → Preparing; Preparing → Ready |
| Waiter | Dòng Ready thuộc đơn Open; tìm theo món, bàn, mã đơn, mã dòng | Ready → Served |
| Cashier | Tiếp tục dùng màn thanh toán riêng | Chỉ thanh toán sau khi mọi dòng chưa hủy đã Served |
| Manager | Tiếp tục dùng tra cứu đơn/báo cáo | Không thay trạng thái bếp hoặc xác nhận phục vụ |

Mỗi xác nhận áp dụng cho toàn bộ số lượng của đúng một Order_Detail; không chuyển toàn bộ đơn. Cancelled/Served không được đổi tiếp. Không bỏ bước, quay ngược, phục vụ trùng, sửa dòng của đơn khác hay cập nhật đơn Completed/Cancelled. Món đã bắt đầu chuẩn bị không còn được giảm số lượng/hủy theo UC05.

## Triển khai và dữ liệu

- Controller: `controllers/PreparationController.java`; GET không thay dữ liệu, POST có CSRF và PRG/flash kết quả.
- Service: `services/preparation/PreparationService.java`; kiểm tra quyền trên từng chức năng và transaction cho thay đổi.
- Repository/entity: dùng lại OrderRepository, OrderDetailRepository, Order và OrderDetail. Repository thêm projection DTO đọc queue và truy vấn dòng theo cả order ID/line ID.
- Khóa Orders bằng `OrderRepository.lockForUpdate` trước khi kiểm tra/chuyển trạng thái, cùng thứ tự khóa với sửa/hủy đơn và checkout. Refresh entity sau khóa để tránh trạng thái cache cũ trong transaction.
- Chỉ cập nhật `preparation_status`; giữ quantity, item_name_at_order, unit_price_at_order, tổng tiền, payment và trạng thái bàn. Lỗi lưu rollback và hiển thị thông báo thử lại.
- Không thêm bảng/cột ngoài ERD chín bảng; không cần migration trên database hiện tại. Demo seed không ghi đè tên bàn, ghi chú đơn hoặc tên món lịch sử khi chạy lại.
- Queue sắp theo thời điểm tạo đơn rồi mã đơn/mã dòng. Số đếm là số dòng, không cộng số lượng món trong một dòng.
- Thông báo Ready và refresh queue kiểm tra mỗi 15 giây. Không reload trang gọi món hoặc làm mất giỏ; queue tạm hoãn reload khi mở dialog/nhập bộ lọc.

## Route

| Method và URL | Quyền | Mục đích |
| --- | --- | --- |
| GET /kitchen?status=PENDING | Chef | Tab Pending/Preparing/Ready |
| GET /kitchen/updates | Chef | Revision/count để refresh |
| POST /kitchen/orders/{orderId}/items/{lineId}/start | Chef | Pending → Preparing |
| POST /kitchen/orders/{orderId}/items/{lineId}/ready | Chef | Preparing → Ready |
| GET /serving?tableId=…&q=… | Waiter | Queue Ready và bộ lọc |
| GET /serving/updates | Waiter | Revision/count/thông báo Ready |
| POST /serving/orders/{orderId}/items/{lineId}/served | Waiter | Ready → Served |

## Cách demo đầy đủ

1. `demo_waiter` tạo đơn trên bàn Available và thêm món.
2. `demo_chef` mở Kitchen Management, xác nhận Start Preparing rồi Mark Ready cho từng dòng.
3. `demo_waiter` mở Serving Management và Confirm Served sau khi giao đủ món của từng dòng.
4. `demo_cashier` chọn đơn, Generate Bill, chọn phương thức, nhập tiền nhận nếu trả Cash, Process Payment và xác nhận.
5. `demo_manager` chọn khoảng ngày chứa giao dịch mới trong Revenue Reports/Payment History.

Cả bốn tài khoản dùng mật khẩu demo `123456`. Bill/Invoice tiếp tục dùng giá lưu tại thời điểm đặt; Cash/Card/Bank Transfer là ghi nhận tại nhà hàng, không kết nối cổng thu tiền thật.
