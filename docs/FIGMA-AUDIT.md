# Đối chiếu giao diện Figma

Nguồn: bộ 13 PNG trong `Restaurant Order Management · Role Dashboards.zip` do người dùng cung cấp ngày 17/11/2026. Các bảng ảnh chứa nhiều frame 1440 × 900 và trạng thái hộp thoại. Đợt sửa đầu triển khai TV1, TV2, TV4 và TV5. Sau khi nhận tài liệu chính, TV3 Kitchen/Serving được triển khai trên cùng shell.

## Các phần đã sửa

| Phần | Kết quả |
| --- | --- |
| Khung chung | Sidebar 256px xanh đậm; header 88px; Inter/Manrope local; tài khoản thật, role, logout; nút cam và trạng thái vô hiệu hóa xám. |
| Login | Brand panel riêng từ ZIP và form HTML thật; hiện/ẩn mật khẩu, lỗi đăng nhập, CSRF. |
| Dashboard | Banner, thẻ module theo actor, sáu module Manager; TV3 hiển thị đang chờ. |
| Nhân viên | Tìm/lọc role và trạng thái; phân trang 8 dòng; hộp thoại thêm/sửa, gán role, khóa; kiểm tra xác nhận mật khẩu. |
| Bàn | Bộ lọc có số lượng thật; lưới thẻ và chi tiết; thêm/sửa bàn, xác nhận trạng thái; giữ chốt Open order. |
| Bếp/phục vụ TV3 | Board hai cột theo đơn/bàn, tab Pending/Preparing/Ready, lọc/tìm Ready, xác nhận từng dòng, feedback và trạng thái rỗng; refresh/notification 15 giây. |
| Gọi món | Ba cột bàn/menu/đơn; lọc tìm món, giỏ số lượng, tổng; thêm món và giữ draft khi tạo lỗi; thao tác chỉ trên Pending. |
| Thực đơn | Danh mục bên trái, bảng món bên phải; thêm/sửa danh mục và món; xác nhận ngừng bán; giữ snapshot giá cũ. |
| Thanh toán | Danh sách đơn và bill; Open/Completed/Cancelled, lọc đủ điều kiện; Generate Bill, phương thức, tiền nhận/tiền thừa, xác nhận payment, hóa đơn in. |
| TV5 | Dùng shell chung; danh sách đơn có tab trạng thái và số lượng; giữ báo cáo, lịch ngày và lịch sử thanh toán đã có. |

Các màn hình dùng dữ liệu MySQL hiện có; không ghi đè tên nhân viên, số bàn, giá hay trạng thái bằng dữ liệu minh họa trong Figma. Biểu mẫu dùng endpoint thật và phân quyền server. Theme xanh lá/Gourmet Palace và template cũ không còn được sử dụng đã được bỏ.

## Kiểm chứng và giới hạn

- Đã quan sát trực tiếp trên Chrome: Login, Dashboard, bảng nhân viên, hộp thoại gán vai trò, thẻ bàn, danh sách thực đơn, hộp thoại thêm món và màn hình gọi món và màn hình thanh toán; đã thêm món vào giỏ và đối chiếu tổng tiền.
- Các kiểm thử JUnit kiểm tra render Thymeleaf, phân quyền/CSRF, giá snapshot, checkout, danh mục, role/khóa tài khoản, xác nhận mật khẩu và khôi phục draft. Kết quả build cuối ghi trong `VERIFICATION.md`.
- ZIP gồm ảnh tổng các màn hình, không có bộ icon và minh họa món xuất riêng. Các SVG local phù hợp được tái sử dụng; ảnh menu dùng catalog đã rà soát, món chưa có ảnh dùng placeholder. Vì vậy chưa tuyên bố khớp từng pixel hoặc toàn bộ hình minh họa. Cần asset SVG/PNG riêng từ Figma để hoàn tất phần này.
- Brand panel là tài nguyên riêng do người dùng cung cấp; không dùng ảnh nguyên màn hình làm giao diện tương tác.
- Chi tiết đơn Manager vẫn có URL riêng để xem/lưu liên kết; hộp thoại chi tiết trong workspace Waiter không thay đổi phân quyền.
- TV3 dùng thao tác Chef/Waiter thật; không tự chuyển món sang Served. Mỗi cập nhật được kiểm tra lại trên server.

- Trên database UI riêng: bill `ORD-1100` là 39 USD; Generate Bill bật trường thanh toán, nhập 50 USD cho tiền thừa 11 USD và bật Process Payment; hộp thoại xác nhận hiển thị đúng đơn/phương thức/tổng. Đóng hộp thoại sau kiểm tra, không ghi giao dịch vào database demo chính.
