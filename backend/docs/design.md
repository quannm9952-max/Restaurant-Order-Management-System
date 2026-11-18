# Giao diện

Nguồn: Figma Restaurant Order Management · Role Dashboards và ZIP PNG do
nhóm cung cấp. [Mở Figma](https://www.figma.com/design/wxeNfF4bp5NJLf0F6t4zgE/Restaurant-Order-Management-%C2%B7-Role-Dashboards?node-id=32-59163).
Giao diện gồm bốn actor: Manager, Waiter, Chef, Cashier.

- `shared/shell.html`: sidebar, header, tài khoản và liên kết theo quyền.
- `shared/reports.html`: bộ lọc ngày và phần dùng chung của báo cáo.
- Màn hình nằm theo nghiệp vụ: employees, tables, orders, preparation, reports.
- CSS/JS dùng chung: `app.css`, `responsive.css`, `app.js`; mã riêng: orders, payments,
  preparation và reports. File HTML/CSS/JS không chứa tên thành viên.
- Ảnh món được chọn trong `MenuImages`. Món không có ảnh phù hợp dùng
  placeholder; ảnh không được ánh xạ đã loại khỏi bản code.
- Font Inter/Manrope và Bootstrap được lưu local cùng thông báo bản quyền.

Giao diện điện thoại và tablet dùng cùng backend/template. Ở chiều rộng tối
đa 1100 px, menu mở thành ngăn bên có nền che, khóa cuộn trang và giữ focus;
đóng bằng nút, nền che hoặc Escape. Resize trở lại desktop khôi phục sidebar.
Dưới 601 px, danh mục món mặc định thu gọn; form/nút phù hợp cảm ứng, bảng
và biểu đồ cuộn trong khung riêng. Hộp thoại/lịch giới hạn theo chiều cao
viewport để còn thao tác ở màn hình ngang. Không thêm thư viện hoặc công nghệ.

Các trang được kiểm tra render bằng Thymeleaf và HTTP. Chưa xác nhận độ
khớp toàn bộ màn hình bằng kiểm tra trực quan trình duyệt; kết quả HTTP không
được coi là xác nhận pixel-perfect.
