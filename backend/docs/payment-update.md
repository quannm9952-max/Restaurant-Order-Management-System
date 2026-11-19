# Thanh toán và báo cáo doanh thu

Cập nhật: 19/11/2026.

## Quy trình của thu ngân

1. Chọn đơn Open có ít nhất một dòng món còn hiệu lực và tất cả đều Served.
2. Kiểm tra món, số lượng và tổng tiền theo đơn giá tại thời điểm đặt món.
3. Chọn Print Bill để xem/in bill cho khách. Đóng bản xem trước vẫn sửa số
   lượng và in lại được; việc in chưa ghi nhận thanh toán.
4. Chọn Cash hoặc Bank Transfer, kiểm tra tiền thực nhận và đánh dấu xác nhận.
5. Chọn Process Payment rồi Confirm Payment. Hệ thống lưu giao dịch và báo
   thành công ngay trên Payment, giữ đơn được chọn và bộ lọc tìm kiếm.

Sau thanh toán, đơn đã Completed không thể sửa hoặc thanh toán lại. Thu ngân
có thể chủ động chọn Print Invoice để xem/in hóa đơn đã lưu.

## Nhập tiền và kiểm tra dữ liệu

- Giá món nhập theo số VND thực tế: `85000` hiển thị `85.000 VND`.
- Amount Received nhập theo nghìn đồng: `10` → `10.000 VND`, `380` →
  `380.000 VND`, `10,5` → `10.500 VND`. Dán số đã định dạng kèm VND cũng được.
- Backend nhận số tiền VND thực tế. Số tiền tối đa là `9.999.999.999,99 VND`,
  với tối đa hai chữ số thập phân.
- Giá món âm hoặc sai định dạng có cảnh báo dưới ô nhập. Ô nhận tiền từ chối
  ký tự sai, số âm, số vượt giới hạn và phần lẻ vượt độ chính xác cho phép.
- Cash phải đủ tổng bill. Change Amount chỉ đọc và tự tính khi tiền hợp lệ.
  Payment lưu tổng bill, không lưu số tiền khách đưa thành doanh thu.
- Bank Transfer dùng QR in sẵn tại quầy. Thu ngân tự kiểm tra tiền thực nhận,
  số tiền và mã đơn trước khi xác nhận; ứng dụng không tự xác nhận ngân hàng.

## Ghi nhận và bảo vệ giao dịch

Review và Confirm khóa dòng Orders. Revision của bill ngăn phiên cũ ghi đè
số lượng hoặc xác nhận một bill đã thay đổi. Backend đối chiếu đầy đủ các
ID dòng món, số lượng nguyên dương, đơn giá lúc đặt và tổng tiền.

Payment Successful, Order Completed, Table Available và Invoice được lưu
trong cùng transaction. Khóa dòng và ràng buộc unique ngăn thanh toán trùng;
lỗi ghi dữ liệu làm rollback toàn bộ. Invoice chưa trả từ phiên bản cũ giữ
số hóa đơn và được cập nhật đúng tổng khi thanh toán thành công.

Print Bill gửi dữ liệu số lượng riêng với mã CSRF; không gửi xác nhận nhận
tiền thay cho bước thanh toán. Phiên hết hạn có hướng dẫn Reload Payment và
đăng nhập lại. Lỗi HTML, JSON hoặc kết nối không hiện thông báo kỹ thuật.

## Báo cáo và phân quyền

Doanh thu chỉ tính Payment Successful của Order Completed. Manager xem
Revenue Reports và Payment History, lọc ngày và phương thức thanh toán.
Báo cáo đọc tổng, biểu đồ và phương thức trong cùng snapshot, tránh nhân
số tiền do nối thêm các dòng món.

Cash Revenue + Bank Transfer Revenue + Unknown Revenue bằng Total Revenue.
Dữ liệu lịch sử ngoài hai phương thức mới được phân loại khi đọc; phương
thức, thời gian và số tiền gốc không bị thay đổi.

Chỉ Cashier được thanh toán và xem/in hóa đơn. Manager chỉ đọc báo cáo;
Waiter và Chef không có quyền xác nhận thanh toán.

## Kiểm thử

Xem [bộ kiểm thử và cách chạy](testing.md). Không thay đổi schema hoặc công
nghệ trong các điều chỉnh giao diện và điều hướng này. Quy trình chuyển số
tiền cũ sang VND được ghi riêng trong [database/README.md](../../database/README.md).
