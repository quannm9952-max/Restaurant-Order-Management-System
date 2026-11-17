# Rà soát giao diện với Figma ngày 17/11/2026

## Kết luận

Bản tích hợp hiện tại chưa được nghiệm thu độ khớp giao diện với Figma. Các phần của thành viên vẫn dùng nhiều hệ bố cục và stylesheet khác nhau. Kiểm thử nghiệp vụ/render Thymeleaf thành công không chứng minh thiết kế hiển thị giống Figma.

Đã yêu cầu lại design context và screenshot từ file `wxeNfF4bp5NJLf0F6t4zgE`. Figma MCP trả về lỗi hết hạn mức Starter cho design context, screenshot và metadata từng trang. Chỉ lấy được danh sách trang cấp cao; chưa lấy được ảnh hoặc thông số chi tiết mới để đánh giá độ khớp từng frame. Khung Figma nhúng cũng chưa đọc được vì browser access host không khả dụng.

## Những vấn đề xác nhận từ mã hiện tại

| Phần | Mã nguồn hiện tại | Cần đối chiếu và sửa theo frame |
| --- | --- | --- |
| Dashboard theo vai trò | `home.html` là một lưới thẻ liên kết, dùng chung cho nhiều vai trò | Dựng đúng dashboard của từng actor, thành phần và thứ tự dữ liệu trong thiết kế |
| Điều hướng chung | Tài khoản/bàn/đơn dùng thanh ngang `shared/navigation`; báo cáo dùng sidebar riêng; menu/thanh toán dùng sidebar khác | Đối chiếu shared components và role dashboard, thống nhất shell theo đúng từng thiết kế |
| Quản lý nhân viên | Bảng và form cơ bản, font Arial và CSS inline riêng | Đối chiếu danh sách, form/modal, bộ lọc và trạng thái tài khoản |
| Bàn và đơn | Bố cục Bootstrap cơ bản; các template còn ghi rõ chưa gán frame/chưa đối chiếu Figma | Dựng lại danh sách bàn, chi tiết bàn, tạo/sửa/xem đơn từ đúng frame |
| Menu/thanh toán/hóa đơn | Theme xanh lá, sidebar trắng, thương hiệu `Gourmet Palace 5★`, hero banner và nhiều emoji | Chưa có căn cứ xác nhận các chi tiết này thuộc Figma cuối; cần đối chiếu trực tiếp, không chỉ đổi màu theo theme báo cáo |
| Tra cứu đơn UC10 | Dùng shell Manager chung nhưng chưa đọc đầy đủ frame chi tiết ở lần triển khai trước | Đối chiếu danh sách, bộ lọc, chi tiết và các trạng thái |
| Báo cáo/lịch sử TV5 | Trước đây đã đọc ba frame chi tiết, nhưng chưa nghiệm thu toàn bộ biến thể | Kiểm tra lại trạng thái rỗng, lỗi, Pending/Failed, modal, lịch ngày và responsive |
| Typography và biểu tượng | Arial ở TV1, Bootstrap ở TV2, DM Sans/Playfair ở TV4, Inter/Manrope ở TV5; có cả emoji và SVG | Chọn đúng font, icon và kích thước từ từng frame/component, dùng tài nguyên local đúng vị trí |

Đây là kết quả rà soát cấu trúc mã, không phải kết quả đo pixel so với ảnh Figma mới.

## Lỗi CSS đã sửa độc lập với thiết kế

Đầu `frontend/static/css/menu-payment.css` còn một phần URL Google Fonts bị cắt dở sau lệnh import local:

```css
@import url('/css/fonts.css');0,500;0,600;...
```

Đã bỏ phần dư, giữ lại lệnh import local hợp lệ. Phần dư có thể khiến rule biến màu `:root` kế tiếp bị phân tích thành selector không hợp lệ. Bản sửa này khôi phục cú pháp stylesheet hiện có; không có nghĩa theme xanh lá/5★ đã được xác nhận giống Figma.

## Ánh xạ trang cần lấy mẫu

| Trang Figma | Node trang | View liên quan |
| --- | --- | --- |
| 01 · Login | 0:1 | login.html |
| 03 · Role Dashboards | 16:526 | home.html |
| 02 · Shared Components | 11:2 | navigation và các layout/fragment |
| 04 · Employee Management | 16:1847 | employees/* |
| 05 · Table Management | 16:8114 | tables/* |
| 06 · Order Management | 16:14857 | orders/* |
| 09 · Payment & Invoice | 32:31991 | payments.html, invoice.html |
| 10 · Menu Management | 32:39141 | menu.html |
| 11 · Restaurant Orders | 32:53215 | reports/orders.html, reports/order-details.html |
| 12 · Reports & Payment History | 32:59163 | reports/revenue.html, payment-history/list.html |

Các node trên là trang cấp cao, chưa phải frame cụ thể để nghiệm thu. TV3 tiếp tục để tích hợp sau; chưa sửa hoặc thêm luồng bếp/phục vụ trong đợt này.

## Cần có để sửa chính xác

Bản xuất `.fig` hoặc PDF/PNG các màn hình Figma, hoặc quyền truy xuất design context hoạt động trở lại. Khi có tham chiếu, chọn đúng frame chuẩn và các trạng thái, lấy font/asset/thông số, sửa component/layout dùng chung, rồi kiểm tra từng màn hình thực tế. Không tự dựng thêm một bộ giao diện theo phỏng đoán.
