# Tích hợp hệ thống TV1 TV2 TV4 TV5

Bản này ghép UC01–UC05 và UC08–UC12 trong một ứng dụng Spring Boot, cùng schema 9 bảng và cơ chế đăng nhập. UC06–UC07 của TV3 chưa được tích hợp theo yêu cầu của người phụ trách tổng hợp.

## Nguồn đóng góp

| Phần | Nhánh nguồn | Commit nguồn đã ghép |
| --- | --- | --- |
| TV1: xác thực và tài khoản | feature/auth-employee | 67725ac |
| TV2: bàn và đơn | feature/table-order | 65b2eee |
| TV4: menu, thanh toán, hóa đơn | feature/menu-payments | f7e5295 |
| TV5: tra cứu và báo cáo | feature/reports-history | 2fb01ea |

Lịch sử các nhánh được giữ bằng merge commit. Các thay đổi tích hợp được thực hiện ở nhánh tổng hợp, không sửa nhánh cá nhân.

## Kiến trúc dùng chung

- `backend/src/main/java/com/restaurant/RestaurantApplication.java`: một điểm khởi động, Java 21 / Spring Boot 4.1.2.
- `auth`: đăng nhập, BCrypt, nhân viên, phân quyền và model người dùng chung.
- `controllers`, `services`, `dto`: bàn và đơn của TV2, đã thay tài khoản development bằng principal đang đăng nhập.
- `entities`, `repository`: entity/JPA repository dùng chung, ánh xạ theo SQL chốt.
- `checkout`: controller/service/repository thực đơn và thanh toán của TV4; truy cập cùng datasource/transaction manager.
- `ordermanagement/reporting`: truy vấn và DTO chỉ đọc của TV5. Repository đọc được đặt tên riêng để không trùng Spring bean của JPA.
- `frontend/templates`, `frontend/static`: Thymeleaf, HTML, CSS, JavaScript, Bootstrap và tài nguyên local, đóng gói trong cùng JAR.
- `database`: schema, fixture kiểm thử, dữ liệu demo không ghi đè thay đổi khi khởi động lại.

Không còn nhiều ứng dụng Spring Boot, login riêng hoặc RestaurantService chứa nghiệp vụ của cả nhóm. Không thêm cột ảnh, cashier hay ngày tạo menu ngoài ERD; ảnh được chọn từ tài nguyên tĩnh. Hóa đơn ghi `Not recorded` cho cashier vì ERD không lưu quan hệ đó.

## Quyền và các luồng đã nối

| Vai trò | Chức năng trong bản này |
| --- | --- |
| Manager | Tài khoản, thực đơn, bàn, xem đơn, xem thanh toán/hóa đơn, báo cáo và lịch sử |
| Waiter | Bàn, tạo/thêm món/giảm số lượng/hủy dòng Pending, hủy đơn hợp lệ |
| Cashier | Kiểm tra điều kiện thanh toán, ghi nhận thanh toán, xem/in hóa đơn |
| Chef | Đăng nhập và trang chủ thông báo chờ module bếp |

HTTP route và service cùng kiểm tra quyền. CSRF được giữ cho POST và logout. Giá lịch sử lấy từ Order_Detail; ngừng bán món không xóa lịch sử. Doanh thu chỉ lấy Payment SUCCESSFUL gắn với Orders COMPLETED.

Các thao tác sửa đơn khóa Orders; tạo đơn và đổi trạng thái bàn khóa Restaurant_tables. Checkout khóa Orders, kiểm tra món Served và thanh toán trùng, rồi ghi Payment + Invoice + hoàn tất đơn + giải phóng bàn trong một transaction. Unique key của schema tiếp tục bảo vệ một đơn Open/bàn và một payment thành công/đơn.

## Chuẩn bị tích hợp TV3 sau

1. Dùng lại `entities.Order`, `OrderDetail`, enum `CookingStatus`, và các repository hiện có. Không tạo lại Account/Employee hoặc bảng Kitchen.
2. Khóa đơn bằng `OrderRepository.lockForUpdate` trước khi kiểm tra/chuyển trạng thái dòng món để phối hợp với checkout và cập nhật đơn.
3. Trong transaction, xác minh đơn OPEN và chuyển đúng Pending → Preparing → Ready; xác nhận Served chỉ từ Ready. Không đổi món Cancelled, không cập nhật trái thứ tự hoặc phục vụ trùng.
4. Dùng principal Spring Security; bảo vệ route/service theo Chef/Waiter trong đặc tả cuối. Cập nhật SecurityConfig, trang chủ và điều hướng khi module đã có test.
5. Bổ sung test tích hợp tạo đơn → bếp → phục vụ → Cashier thanh toán → Manager xem báo cáo.

Hiện `/kitchen/**` và `/serving/**` bị chặn. Không có nút thay đổi trạng thái bếp để giả lập module TV3. Đơn DEMO 1100 đã Served chỉ là fixture để kiểm thử phần checkout độc lập; đơn mới vẫn Pending và chưa thể thanh toán cho đến khi TV3 được tích hợp.

## Dữ liệu và giới hạn kiểm chứng

- Demo dùng USD để thống nhất với báo cáo TV5; giá là số liệu giả lập, không chuyển đổi từ tiền thật.
- `seed.sql` là fixture đối soát kiểm thử, có thể reset các bản ghi fixture. Không chạy trên DB đang sử dụng cho nghiệp vụ.
- Profile demo dùng `demo-seed.sql` + `demo-extra.sql`, chỉ thêm bản ghi thiếu và không đặt lại trạng thái đơn đã xử lý.
- Bộ báo cáo mẫu 29/10–04/11/2026: 24 thanh toán thành công, tổng 1,892.00 USD; 4 giao dịch bị loại. Thanh toán mới xuất hiện khi chọn khoảng ngày chứa ngày phát sinh.
- Chưa nghiệm thu độ khớp toàn bộ màn hình với Figma. TV2 còn ghi chú giao diện draft từ bài gốc.
- Công cụ Windows kiểm tra giao diện đã dừng vì không xác định chắc URL Chrome; không dùng trình duyệt khác để vượt chặn. Kiểm tra tự động tiếp tục ở lớp mã nguồn, MockMvc và HTTP/MySQL.
