# Frontend

- `templates`: màn hình Thymeleaf theo nghiệp vụ; `shared` chứa phần dùng chung.
- `static/css`: app, login, preparation, reports và responsive; vendor giữ nguyên.
- `static/js`: app, orders, payments, preparation và reports.
- `static/images`: icons, ảnh đăng nhập và ảnh món đang dùng.
- `static/fonts`: font và giấy phép OFL.

Frontend được Spring Boot render và đóng gói trong JAR. Không chạy server
frontend riêng. Mở http://localhost:8080 sau khi chạy `run.cmd` ở gốc dự án.

`responsive.css` dùng chung cho điện thoại, tablet và desktop. Dưới 1101 px,
sidebar chuyển thành menu mở/đóng; dưới 601 px, bố cục xếp thành cột và các
điều khiển được tăng kích thước. Bảng/biểu đồ rộng cuộn trong khung riêng.

Serving Management dùng `status=READY|SERVED`, mặc định Ready. Mục Served
không có form/nút xác nhận; hiển thị theo bàn và đơn, giữ bộ lọc khi chuyển
mục. Tiến độ tính toàn đơn, không phụ thuộc từ khóa tìm kiếm. Polling giữ
thông báo Ready riêng và cập nhật lịch sử khi món/đơn thay đổi.

## Nội dung hướng dẫn trên giao diện

Chỉ hiển thị hướng dẫn thao tác, điều kiện nghiệp vụ, cảnh báo và trạng thái
cần thiết cho nhân viên/khách. Không đưa ghi chú kiểm thử, lý do triển khai,
cơ chế lưu dữ liệu hoặc chi tiết polling lên giao diện. Ghi chú lập trình
nằm trong mã nguồn hoặc tài liệu phát triển. Ghi chú đơn và mô tả món do
nhân viên nhập vẫn được hiển thị, có escape HTML như trước.

Phân trang Employee Management, Restaurant Orders và Payment History dùng
cùng `.pagination-buttons`: mũi tên trái, trang hiện tại / tổng trang, mũi tên
phải. SVG dùng `shared/shell :: pageArrow`; các trang báo cáo dùng chung
fragment `pagination`. Đầu/cuối danh sách dùng button disabled; liên kết
chuyển trang giữ đầy đủ tìm kiếm và bộ lọc.
