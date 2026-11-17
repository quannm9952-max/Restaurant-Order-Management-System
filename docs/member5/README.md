> Historical member-5 handoff before integration. Use [the current README](../../README.md) and [integration notes](../INTEGRATION.md) for startup, account and architecture details.

# Bàn giao phần thành viên 5

Thành viên 5 — Quân — thực hiện **UC10 View Restaurant Orders**, **UC11 View Revenue Reports** và **UC12 View Payment History**. Chỉ Manager được xem các chức năng này. Đây là phần triển khai riêng của thành viên 5; việc ghép code, review và merge với các thành viên khác được thực hiện sau.

## Căn cứ đối chiếu

- Mục 9 và các quy tắc chung trong `KẾ HOẠCH PHÂN CÔNG LẬP TRÌNH VÀ CHUẨN BỊ DỮ LIỆU.docx`.
- `RestaurantOrderManagement_Database.sql` trong thư mục `D:\công nghệ phần mềm`: 9 bảng, tên cột và giá trị trạng thái hiện có.
- Mục 4.4.10–4.4.12 trong `PHAN4.docx` và `PHAN4_English_Software_Engineering_Corrected.docx`.
- [Thiết kế Reports & Payment History](https://www.figma.com/design/wxeNfF4bp5NJLf0F6t4zgE/Restaurant-Order-Management?node-id=32-59163).

`database/schema.sql` giữ các định nghĩa bảng của file SQL nguồn, chỉ tách phần tạo/chọn database để có thể chạy trên database demo riêng. Không bổ sung bảng Revenue, cột Cashier hoặc entity JPA trùng với phần TV1/TV2/TV4.

## Chức năng và route

| Use Case | Route GET | Hành vi |
| --- | --- | --- |
| UC10 | `/manager/orders` | Danh sách Open/Completed/Cancelled; tìm theo mã đơn hoặc tên bàn, lọc trạng thái, phân trang |
| UC10 | `/manager/orders/{id}` | Xem dòng món, số lượng, tên và giá đã lưu lúc đặt, trạng thái món và các lần thanh toán liên quan |
| UC11 | `/manager/reports` | Lọc ngày bắt đầu/kết thúc; nhóm ngày hoặc tuần; tổng doanh thu, số payment thành công, số bị loại, biểu đồ, ba payment gần nhất |
| UC12 | `/manager/payment-history` | Lịch sử đủ Pending/Successful/Failed; lọc ngày và trạng thái; tìm mã payment/đơn; 8 dòng mỗi trang |
| UC12 | `/manager/payment-history/{id}` | Chi tiết giao dịch dạng hộp thoại; giữ bộ lọc và trang khi đóng |

Mọi route của module đều chỉ đọc. Backend chặn các phương thức ghi và kiểm tra `ROLE_MANAGER` cả ở filter chain lẫn service. Các nút thuộc module chưa bàn giao chỉ xuất hiện dưới dạng nhãn không tương tác; không điều hướng tới chức năng giả.

## Quy tắc tính và đối soát

- Dùng `BigDecimal` và `DECIMAL`, không dùng `float`/`double` cho tiền.
- Doanh thu lấy trực tiếp từ `Payment.amount`, với `Payment.status = 'SUCCESSFUL'` và đơn liên quan `COMPLETED`.
- Không cộng Pending, Failed, đơn Cancelled, đơn Open hoặc đơn chưa thanh toán. Giao dịch Successful nhưng đơn chưa Completed vẫn được giữ trong lịch sử và được đánh dấu cần đối soát khi xem báo cáo.
- Lọc theo **Payment.payment_time**, không theo thời điểm tạo đơn. Khoảng ngày bao gồm cả ngày cuối: `>= from 00:00:00` và `< ngày sau to 00:00:00`.
- Tuần bắt đầu thứ Hai; chỉ cộng giao dịch thực sự nằm trong khoảng đã chọn, kể cả khi nhãn tuần có ngày nằm ngoài khoảng.
- Không join Order_Detail trong truy vấn tổng doanh thu, để tránh nhân số tiền khi đơn có nhiều dòng món.
- Chi tiết đơn đọc `item_name_at_order` và `unit_price_at_order`. Món Cancelled vẫn xuất hiện trong lịch sử nhưng bị loại khỏi tổng đang đối soát.
- Báo cáo đọc tổng, biểu đồ và các payment mới nhất trong một transaction `REPEATABLE_READ`.
- Không cập nhật trạng thái đơn/bàn, sửa menu, ghi thanh toán hay tạo hóa đơn trong module này.

## Chạy độc lập với dữ liệu DEMO

Cần Java 21 và MySQL Community 8.0+. Maven Wrapper đã có trong repository. Chạy từ thư mục repository có `run-tv5.cmd`:

```powershell
$env:DB_PASSWORD = 'mat-khau-mysql-cua-ban'
$env:DEMO_PASSWORD = '123456'
.\run-tv5.cmd --spring.profiles.active=tv5-demo
```

Nếu root không có mật khẩu, không cần đặt `DB_PASSWORD`. `DEMO_PASSWORD` là mật khẩu thử nghiệm do người chạy chọn, không được in vào log hoặc đưa mật khẩu thật vào Git.

Script `run-tv5.cmd` chuyển thư mục làm việc vào `backend`; profile tìm script tại `../database/schema.sql` và `../database/seed.sql`.

Profile `tv5-demo` kết nối database riêng **restaurant_order_management_tv5_demo**, tạo database nếu tài khoản có quyền, chạy `database/schema.sql` rồi `database/seed.sql`. Có thể đặt `TV5_DEMO_DB_URL` để trỏ tới một database **dành riêng cho demo**. Profile mặc định không chạy seed.

Truy cập <http://localhost:8080/login>, đăng nhập `demo_manager` bằng giá trị `DEMO_PASSWORD`. Các tài khoản `demo_waiter`, `demo_chef`, `demo_cashier` dùng cùng mật khẩu thử nghiệm và phải bị từ chối khi truy cập `/manager/**`.

Các tài khoản đăng nhập này chỉ tồn tại trong bộ nhớ khi bật profile demo; không phải phần triển khai UC01–UC03 của TV1. Bốn bản ghi `DEMO_FIXTURE_*` trong Account là dữ liệu minh họa vai trò, bị vô hiệu hóa, không dùng để đăng nhập. Tắt profile demo khi ghép phần xác thực chung.

Profile demo chọn mặc định 29/10–04/11/2026 để khớp bộ đối soát. Ngoài profile này, khoảng mặc định là bảy ngày gần nhất theo múi giờ `Asia/Ho_Chi_Minh`. Thiết kế dùng USD; các giá trị trong bộ demo là giả lập.

Nếu chạy trên máy đã có database nhưng thiếu quyền tạo database, tạo `restaurant_order_management_tv5_demo` bằng DBeaver trước. Để chạy thủ công, chọn database demo, thực thi `database/schema.sql`, sau đó `database/seed.sql`.

## Dữ liệu và kết quả kỳ vọng

Các tên, ghi chú và transaction reference đều có nhãn DEMO/TEST. Không có dữ liệu khách hàng thật hoặc thông tin thẻ. Seed có đủ ba trạng thái bàn, món đang bán/ngừng bán, ba trạng thái đơn và năm trạng thái dòng món.

| Khoảng ngày | Successful | Pending | Failed | Doanh thu USD |
| --- | ---: | ---: | ---: | ---: |
| 29/10–04/11/2026 | 24 | 2 | 2 | 1,892.00 |
| 29/10/2026 | 2 | 0 | 0 | 196.00 |
| 30/10/2026 | 4 | 0 | 0 | 248.00 |
| 31/10/2026 | 4 | 0 | 0 | 312.00 |
| 01/11/2026 | 3 | 0 | 0 | 284.00 |
| 02/11/2026 | 4 | 0 | 0 | 338.00 |
| 03/11/2026 | 4 | 0 | 1 | 330.00 |
| 04/11/2026 | 3 | 2 | 1 | 184.00 |

Nhóm theo tuần: 26/10–01/11 = **1,040.00**; 02/11–08/11 = **852.00**. Bốn giao dịch bị loại khỏi doanh thu: PAY-2042, PAY-2045, PAY-2047, PAY-2048. PAY-2045 là lần thất bại trước PAY-2046 của cùng ORD-1042; chỉ lần Successful được cộng.

Chạy `database/reconciliation.sql` trong DBeaver để đối chiếu. Seed có thể chạy lại và không tạo thêm bản ghi trùng.

## Kiến trúc và điểm ghép sau này

Code backend nằm trong `backend/src/main/java/com/restaurant/ordermanagement/reporting`, theo Controller → Service → Repository, trả dữ liệu qua các record DTO. `OrderRepository` và `PaymentRepository` ở đây là truy vấn đọc bằng Spring JDBC trên các bảng chung; không có bản sao entity hoặc phương thức ghi. Khi nhận code TV2/TV4, thống nhất việc chuyển các truy vấn này vào repository chung hoặc giữ làm truy vấn đọc báo cáo, tránh định nghĩa trùng.

`ReportingSecurityConfiguration` chỉ bảo vệ `/manager/**` với thứ tự 50. Khi TV1 bàn giao, đặt các route này sau xác thực chung, giữ `ROLE_MANAGER` và bỏ các cấu hình demo. Cần review thứ tự filter chain với TV1; không coi profile demo là phần tích hợp xác thực đã hoàn thành.

Bản chuẩn bị nằm trong repository của nhóm, trên nhánh `feature/reports-history`. Code được phân loại thành `frontend`, `backend`, `database`; các tài liệu và test được giữ riêng. Chưa commit, push, PR hoặc merge trong lần sắp xếp này. PR sau này cần ghi UC10–UC12 và được ít nhất một thành viên khác review trước khi merge.

## Đối chiếu Figma và phần chưa xác minh

| Màn hình | Node thiết kế đã đọc | File triển khai |
| --- | --- | --- |
| Báo cáo theo ngày | 32:62139 | `frontend/templates/reports/revenue.html` |
| Báo cáo theo tuần | 32:62356 | `frontend/templates/reports/revenue.html` |
| Lịch sử và chi tiết Successful | 32:65602 | `frontend/templates/payment-history/list.html` |
| Toàn bộ biến thể báo cáo/lịch sử | 32:67891, ảnh tổng quan | Các view báo cáo và lịch sử |

SVG dùng cục bộ, giữ kích thước gốc; font Inter/Manrope và Bootstrap cũng được lưu cục bộ. Không dùng ảnh chụp Figma làm giao diện hoặc làm số liệu nghiệp vụ.

Các giới hạn cần ghi nhận trước nghiệm thu:

- Figma giới hạn lượt MCP sau khi đọc các node trên; chưa đọc được đầy đủ node chi tiết UC10 ở trang 32:53215. Giao diện UC10 dùng cùng bố cục Manager và các trường của đặc tả, cần đối chiếu frame cuối khi có quyền truy cập công cụ trở lại.
- Schema Payment chưa có khóa tham chiếu người thu tiền. Trường Cashier trong hộp thoại hiển thị `Not recorded`, không lấy nhầm người tạo đơn thành thu ngân và không tự thêm cột.
- Đã mở bản chạy trên Chrome và quan sát trực tiếp báo cáo doanh thu, lịch sử thanh toán, hộp thoại Successful, danh sách và chi tiết đơn; đã xem ở cửa sổ thường và khi phóng to. Còn cần rà soát toàn bộ biến thể Figma, responsive trên các kích thước khác và thao tác lịch chọn ngày.
- Máy hiện dùng MariaDB 10.4.32 trong XAMPP; đây không phải MySQL Community 8.0. Cần chạy lại trên MySQL Community trước khi đánh dấu nghiệm thu môi trường theo tài liệu.
- Các luồng của TV1–TV4 và kiểm thử xuyên suốt cả nhóm chưa được ghép/chạy. Xem checklist để thực hiện sau khi nhận đủ bài.

## Kiểm tra

```powershell
.\verify-tv5.cmd
```

Test dùng H2 ở scope test, không dùng hay thay đổi database đang làm việc. Schema thử nghiệm giữ chín bảng gốc, bỏ tùy chọn `ENGINE`, `STORED` và lệnh transaction riêng của MySQL. Xem [TEST-CASES.md](TEST-CASES.md) và [INTEGRATION-CHECKLIST.md](INTEGRATION-CHECKLIST.md).

Không chạy `clean`/đóng gói khi một tiến trình Java đang chạy chính file JAR trong `target`; dừng tiến trình bằng Ctrl+C trước. Windows sẽ khóa file JAR đang dùng.
