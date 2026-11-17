# Tích hợp hệ thống TV1 TV2 TV3 TV4 TV5

Bản này ghép đủ UC01–UC12 trong một ứng dụng Spring Boot, cùng schema 9 bảng và cơ chế đăng nhập. UC06–UC07 được triển khai theo `RequirementAndDesignDocument.docx`, dùng dữ liệu đơn và dòng món hiện có.

## Nguồn đóng góp

| Phần | Nhánh nguồn | Commit nguồn đã ghép |
| --- | --- | --- |
| TV1: xác thực và tài khoản | feature/auth-employee | 67725ac |
| TV2: bàn và đơn | feature/table-order | 65b2eee |
| TV4: menu, thanh toán, hóa đơn | feature/menu-payments | f7e5295 |
| TV5: tra cứu và báo cáo | feature/reports-history | 2fb01ea |
| TV3: bếp và phục vụ | feature/tv3-integration | Theo UC06–UC07 trong tài liệu chính |

Lịch sử các nhánh được giữ bằng merge commit. Các thay đổi tích hợp được thực hiện ở nhánh tổng hợp, không sửa nhánh cá nhân.

## Kiến trúc dùng chung

- `backend/src/main/java/com/restaurant/RestaurantApplication.java`: một điểm khởi động, Java 21 / Spring Boot 4.1.2.
- `auth`: đăng nhập, BCrypt, nhân viên, phân quyền và model người dùng chung.
- `controllers`, `services`, `dto`: bàn và đơn của TV2, đã thay tài khoản development bằng principal đang đăng nhập.
- `services/preparation`, `dto/preparation`, `controllers/PreparationController`: bếp và phục vụ TV3, queue DTO và chuyển trạng thái theo từng dòng.
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
| Waiter | Bàn, tạo/thêm món/giảm số lượng/hủy dòng Pending, hủy đơn hợp lệ, Ready → Served |
| Cashier | Kiểm tra điều kiện thanh toán, ghi nhận thanh toán, xem/in hóa đơn |
| Chef | Queue bếp, Pending → Preparing → Ready theo từng dòng món |

HTTP route và service cùng kiểm tra quyền. CSRF được giữ cho POST và logout. Giá lịch sử lấy từ Order_Detail; ngừng bán món không xóa lịch sử. Doanh thu chỉ lấy Payment SUCCESSFUL gắn với Orders COMPLETED.

Các thao tác sửa đơn khóa Orders; tạo đơn và đổi trạng thái bàn khóa Restaurant_tables. Checkout khóa Orders, kiểm tra món Served và thanh toán trùng, rồi ghi Payment + Invoice + hoàn tất đơn + giải phóng bàn trong một transaction. Unique key của schema tiếp tục bảo vệ một đơn Open/bàn và một payment thành công/đơn.

## Tích hợp TV3

Nguồn chính là mục 4.4.6 (UC06), 4.4.7 (UC07), BR06/BR07 và ERD 6.1 trong Word. Chi tiết đối chiếu, route và cách demo: [member3/REQUIREMENTS.md](member3/REQUIREMENTS.md). Ca kiểm thử: [member3/TEST-CASES.md](member3/TEST-CASES.md).

Chef chỉ chuyển Pending → Preparing → Ready. Waiter chỉ chuyển Ready → Served. Mỗi thao tác khóa Orders trước, refresh dữ liệu hiện tại, kiểm tra đơn Open và line ID thuộc đúng đơn rồi lưu trong transaction. Vì sửa/hủy đơn và checkout dùng cùng khóa, các thao tác đồng thời không vượt điều kiện nghiệp vụ. Save lỗi rollback, bấm trùng hoặc trạng thái cũ hiển thị lý do và yêu cầu refresh.

Queue nhóm theo đơn/bàn, giữ thứ tự tạo; Kitchen có các tab trạng thái và các dòng khác để tham khảo, Serving chỉ liệt kê Ready và hỗ trợ tìm/lọc. Thông báo Ready cho Waiter dùng count/revision đọc từ DB, kiểm tra mỗi 15 giây. Không cần bảng Kitchen, notification table hay cột thời gian/chef ngoài ERD.

Luồng đã nối: Waiter tạo đơn Pending → Chef Preparing/Ready → Waiter Served → Cashier thanh toán/invoice → Manager tra cứu/revenue/history. Mọi dòng không bị hủy phải Served trước checkout. Đơn hoàn tất và bàn Available được cập nhật cùng Payment/Invoice; không tự động hoàn tất đơn khi vừa phục vụ xong.

## Dữ liệu và giới hạn kiểm chứng

- Demo dùng USD để thống nhất với báo cáo TV5; giá là số liệu giả lập, không chuyển đổi từ tiền thật.
- `seed.sql` là fixture đối soát kiểm thử, có thể reset các bản ghi fixture. Không chạy trên DB đang sử dụng cho nghiệp vụ.
- Profile demo dùng `demo-seed.sql` + `demo-extra.sql`, chỉ thêm bản ghi thiếu và không đặt lại trạng thái đơn đã xử lý.
- Bộ báo cáo mẫu 29/10–04/11/2026: 24 thanh toán thành công, tổng 1,892.00 USD; 4 giao dịch bị loại. Thanh toán mới xuất hiện khi chọn khoảng ngày chứa ngày phát sinh.
- Các màn hình đã được chỉnh theo PNG Figma. Một số icon/minh họa chưa có asset riêng; xem `FIGMA-AUDIT.md` về phạm vi đối chiếu.
- JUnit/H2, HTTP/MySQL và kiểm tra Chrome được báo cáo riêng trong `VERIFICATION.md`; kết quả ở một lớp không thay cho lớp khác.
