# Bổ sung thanh toán và thống kê doanh thu

Ngày bàn giao: 18/11/2026.

## Luồng hiện tại: xem bill, sửa và thanh toán (18/11/2026)

Bỏ Generate Bill. Payment có Print Bill và Process Payment; thông tin thanh
toán mở ngay khi đơn đủ điều kiện. Print Bill lưu số lượng kiểm tra và mở
bill Unpaid trong dialog trên cùng màn hình. Customer xem/in, Cashier đóng
dialog rồi vẫn sửa được và in lại. Preview dùng chung template hóa đơn với
trạng thái Unpaid/Paid rõ ràng; CSS in chỉ giữ nội dung bill, ẩn Payment.

Review và Confirm khóa Orders để tuần tự hóa ghi dữ liệu. Revision dựa trên
chi tiết đơn chặn phiên cũ ghi đè hoặc xác nhận theo nội dung đã bị thay đổi.
Confirm nhận số lượng đang xem cùng tổng để đối chiếu giá snapshot, ghi tất
cả thay đổi và thanh toán trong một transaction. Chỉ Successful + Completed
mới tính doanh thu/khóa đơn. Invoice cũ chưa trả không còn là mốc khóa; giữ
số hóa đơn và cập nhật đúng tổng khi trả. Không thay schema hoặc công nghệ.

Các phần bên dưới là lịch sử các phiên bản trước, không mô tả luồng hiện tại.

## Cập nhật: chỉnh số lượng và khóa bill (18/11/2026)

Cashier sửa số lượng nguyên dương của các dòng Served trước khi Generate
Bill; đơn giá giữ theo Order_Detail, dòng Cancelled không chỉnh và không tính
tiền. Preview cập nhật ngay; Generate Bill lưu toàn bộ số lượng, tổng Orders
và Invoice cùng một transaction. Khóa Orders tuần tự hóa các yêu cầu cạnh
tranh. Bill đã có thì cả Cashier và Waiter không thay đổi được đơn; trạng
thái khóa đọc từ database nên giữ khi refresh/đổi thiết bị.

Phần nhập phương thức/tiền nhận và xác nhận chỉ mở sau khi Generate Bill
thành công. Backend từ chối thanh toán chưa có bill hoặc tổng khác bill đã
lưu. Thanh toán thành công cập nhật Payment/Orders/Table trong transaction,
sử dụng bill đã lưu; không tạo Invoice lần hai. Invoice chỉ được in sau
Successful + Completed. Bill chưa thanh toán không làm tăng doanh thu.
Không thêm bảng/cột, công nghệ hay migration; không chỉnh dữ liệu cũ.

Các phần bên dưới là ghi nhận triển khai ở phiên bản trước cập nhật này.

## Chức năng đã triển khai

- Giao dịch mới chỉ nhận mã `CASH` hoặc `BANK_TRANSFER`; không tự mặc định Cash khi thiếu phương thức.
- Cashier phải đánh dấu **I have received and verified the payment.** Backend cũng kiểm tra lại, kể cả gửi request trực tiếp.
- Cash bắt buộc Amount Received hợp lệ, tối đa hai chữ số thập phân và đủ hóa đơn. Change Amount hiển thị ở giao diện; số tiền lưu Payment là tổng hóa đơn, không phải tiền khách đưa.
- Bank Transfer hiển thị số tiền, mã đơn gợi ý làm nội dung và hướng dẫn dùng QR in sẵn ở quầy. Cashier tự kiểm tra tài khoản nhà hàng trên điện thoại trước khi xác nhận.
- Kiểm tra lại quyền Cashier, đơn Open, các dòng còn hiệu lực Served, chưa có Successful và tổng từ số lượng × đơn giá lúc đặt. Không lấy tổng tiền do trình duyệt gửi.
- Payment, Order Completed, Table Available và Invoice cùng transaction. Khóa dòng Orders và unique constraint hiện có chống thanh toán thành công trùng.
- Khi xác nhận lỗi, giao diện yêu cầu kiểm tra trạng thái thanh toán trước khi thử lại; đơn đã Completed chỉ xem/in hóa đơn.
- Hóa đơn giữ mã đơn, bàn, món/số lượng/đơn giá, tổng tiền, phương thức và thời gian thanh toán. Cashier truy xuất/in theo chức năng hiện có.
- Manager có Cash Revenue, Bank Transfer Revenue và bảng Cash / Bank Transfer / Unknown / Total; lọc ngày bằng lịch hiện có. Lịch sử có phương thức, số tiền, ngày, trạng thái, mã đơn và bộ lọc phương thức.
- Giữ quy tắc doanh thu đã chốt: **Payment Successful và Order Completed**. Successful lệch trạng thái đơn vẫn xuất hiện trong lịch sử và cần đối soát, không cộng vào doanh thu.
- Query tổng, biểu đồ và phương thức đọc trực tiếp từ Payment/Orders trong cùng snapshot; không join chi tiết món vào phép tổng hợp để tránh nhân doanh thu.
- Manager chỉ đọc; Waiter/Chef không xác nhận thanh toán hoặc truy cập báo cáo. Không mở thêm quyền trong SecurityConfig.

## Database và dữ liệu cũ

Không thay đổi schema, khóa chính/khóa ngoại hoặc cấu hình Docker; **không cần SQL migration**. Tái sử dụng `payment_method VARCHAR(30)`, `amount`, `status`, `payment_time`, `order_id` và index `uq_one_successful_payment_per_order`. Payment hiện không có cashier_id nên không thêm cột chỉ để mở rộng phạm vi.

Không chạy UPDATE/backfill trên giao dịch cũ. Khi đọc, `Cash` và `Bank Transfer` cũ được nhận vào nhóm tương ứng; NULL, trống hoặc phương thức khác như Card vào nhóm **Unknown**. Lịch sử/hóa đơn vẫn hiển thị phương thức gốc nếu có, không đổi Card thành Cash hoặc sửa số tiền/thời gian. Tổng Cash + Bank Transfer + Unknown bằng Total với cùng khoảng ngày/quy tắc trạng thái.

Entity Payment được sửa nullable để khớp cột hiện có. `database/check.sql` chỉ thêm truy vấn đối soát, không chứa lệnh migration/xóa dữ liệu mới.

## Build và kiểm thử thực tế

- Docker build stage `verify`: **332 test, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**. PaymentIntegrationTest có 47 ca; các bộ regression hiện có vẫn đạt sau cập nhật API xác nhận.
- Docker runtime build thành công. `node --check` payments.js và `git diff --check` đạt.
- MySQL/InnoDB riêng: hai phương thức thành công; thiếu phương thức/checkbox/tiền, phương thức sai và tiền quá hai số lẻ bị từ chối; giá lấy từ chi tiết đã lưu. Xác nhận đồng thời chỉ tạo một Successful/Invoice; INSERT trực tiếp trùng cũng bị unique constraint chặn.
- Cố ý gây lỗi tạo Invoice sau bước ghi Payment/cập nhật Order/Table: xác nhận lỗi an toàn, Payment không tồn tại, Order vẫn Open và Table vẫn Occupied. Không có cập nhật một phần.
- Doanh thu/số giao dịch từng nhóm được đối chiếu SQL, tổng nhóm khớp tổng; kiểm tra cả NULL, trống, Card, tên cũ và mã mới. 28 giao dịch mẫu cũ giữ nguyên checksum theo từng dòng sau luồng thử.
- Chrome headless: **40 bố cục** ở 8 kích thước từ 320 đến 1440 px cho Cash, Bank Transfer, báo cáo, lịch sử và chi tiết. Có thao tác xác nhận/submit thật, tiền thừa, hướng dẫn QR, hóa đơn/in ấn và phân trang giữ phương thức. Không lỗi JavaScript hoặc tràn ngang toàn trang; bảng rộng có khung cuộn riêng. Đã xem ảnh desktop/điện thoại.
- Bản chính Docker: số liệu nhóm khớp MySQL ở localhost, quyền và giao diện đạt; checksum cả 9 bảng trước/sau cập nhật không đổi. CSS/JS public qua Cloudflare khớp nguồn; không tải dữ liệu tài chính đã đăng nhập qua Cloudflare.
- Project/volume dùng thử đã dọn riêng; database và ảnh của bản chính giữ nguyên.

## Giới hạn và phần giữ nguyên

Không ghi nhận lỗi còn lại trong phạm vi đã kiểm tra. Kiểm tra màn hình nhỏ bằng giả lập Chrome, chưa thử trên thiết bị iPhone/iPad thật hoặc Safari.

Giữ Java, Spring Boot, MySQL, frontend hiện tại, Docker Compose, kiến trúc và 12 Use Case. Không sửa tài liệu lý thuyết. Không tạo/hiển thị QR, kết nối ngân hàng/cổng thanh toán, lưu thông tin đăng nhập ngân hàng hoặc tự xác nhận chuyển khoản. Tiền thực nhận và chênh lệch chuyển khoản do Cashier kiểm tra/xử lý theo quy trình yêu cầu.

## Danh sách file thay đổi

| File | Thay đổi |
| --- | --- |
| `README.md` | Cập nhật hướng dẫn sử dụng và liên kết báo cáo bàn giao. |
| `backend/docs/testing.md` | Ghi lại kết quả build, kiểm thử và đối chiếu dữ liệu thực tế. |
| `backend/docs/payment-update.md` | Báo cáo triển khai, danh sách file và giới hạn kiểm thử. |
| `backend/src/main/java/com/restaurant/controller/PaymentController.java` | Nhận phương thức, xác nhận nhận tiền và Amount Received; xử lý lỗi an toàn, giữ đơn được chọn. |
| `backend/src/main/java/com/restaurant/controller/PaymentHistoryController.java` | Nhận và giữ bộ lọc phương thức thanh toán. |
| `backend/src/main/java/com/restaurant/dto/payment/PaymentConfirmation.java` | DTO mới cho thông tin xác nhận của Cashier. |
| `backend/src/main/java/com/restaurant/dto/report/ReportModels.java` | Thêm dữ liệu tổng hợp theo phương thức và nhãn phương thức thân thiện. |
| `backend/src/main/java/com/restaurant/entity/Payment.java` | Khớp nullable của payment_method với schema hiện có để hỗ trợ dữ liệu lịch sử. |
| `backend/src/main/java/com/restaurant/entity/enums/PaymentMethod.java` | Hai phương thức mới hợp lệ, phân loại dữ liệu cũ và nhóm Unknown chỉ dùng khi đọc. |
| `backend/src/main/java/com/restaurant/repository/CheckoutRepository.java` | Bổ sung kiểm tra Successful cho danh sách đơn đủ điều kiện; tái sử dụng ghi Payment/Invoice. |
| `backend/src/main/java/com/restaurant/repository/PaymentReportRepository.java` | SQL tổng hợp Cash/Bank Transfer/Unknown và lọc lịch sử theo cùng cách phân loại. |
| `backend/src/main/java/com/restaurant/service/HistoryService.java` | Truyền bộ lọc phương thức cho lịch sử và số đếm trạng thái. |
| `backend/src/main/java/com/restaurant/service/PaymentService.java` | Kiểm tra xác nhận/phương thức/tiền mặt, khóa đơn và hoàn tất thanh toán trong một transaction. |
| `backend/src/main/java/com/restaurant/service/ReportService.java` | Đọc tổng hợp phương thức trong cùng snapshot với tổng và biểu đồ. |
| `backend/src/test/java/com/restaurant/PaymentIntegrationTest.java` | 47 ca mới cho thanh toán, dữ liệu cũ, báo cáo, rollback và đồng thời. |
| `backend/src/test/java/com/restaurant/PreparationIntegrationTest.java` | Cập nhật luồng phục vụ → thanh toán theo API xác nhận mới. |
| `backend/src/test/java/com/restaurant/SystemIntegrationTest.java` | Cập nhật regression thanh toán/phân quyền/rollback theo API mới. |
| `database/README.md` | Hướng dẫn lưu mã phương thức mới và giữ dữ liệu cũ; xác nhận không cần migration. |
| `database/check.sql` | Thêm truy vấn chỉ đọc đối soát doanh thu theo phương thức. |
| `frontend/static/css/app.css` | Bố cục ô xác nhận và hướng dẫn chuyển khoản trong giao diện hiện tại. |
| `frontend/static/css/reports.css` | Bố cục thẻ doanh thu và bảng nhóm phương thức, phù hợp màn hình nhỏ. |
| `frontend/static/js/payments.js` | Điều kiện bật nút, tiền nhận/tiền thừa theo cent, checkbox và chặn submit lặp. |
| `frontend/templates/invoice.html` | Hiển thị Cash/Bank Transfer; phương thức cũ hoặc Unknown hiển thị đúng lịch sử. |
| `frontend/templates/payments.html` | Chọn hai phương thức, Amount Received/Change Amount, hướng dẫn QR cố định và checkbox. |
| `frontend/templates/reports/payments.html` | Cột phương thức; giữ phương thức khi đổi ngày/tìm kiếm/trạng thái/phân trang/đóng chi tiết. |
| `frontend/templates/reports/revenue.html` | Cash Revenue, Bank Transfer Revenue và bảng Revenue by Payment Method/Total. |
