# Frontend thành viên 5

Các màn hình dùng HTML/Thymeleaf, Bootstrap, CSS và JavaScript, theo bố cục Manager trong Figma.

| Thư mục | Nội dung |
| --- | --- |
| `templates/reports/` | Danh sách đơn, chi tiết đơn, báo cáo doanh thu |
| `templates/payment-history/` | Danh sách thanh toán và hộp thoại chi tiết |
| `templates/reporting/` | Sidebar, header, bộ chọn ngày và trang thông báo lỗi |
| `static/css/` | Giao diện, khai báo font và Bootstrap cục bộ |
| `static/js/` | Lịch chọn ngày, hành vi Cancel và hộp thoại |
| `static/images/tv5/` | Các SVG được giao diện tham chiếu |
| `static/fonts/` | Font Inter, Manrope và giấy phép OFL |

Maven trong `../backend/pom.xml` đóng gói thư mục này vào JAR. Khởi động từ thư mục repository bằng `run-tv5.cmd --spring.profiles.active=tv5-demo`. Dữ liệu được backend truy vấn từ database rồi đưa vào template.
