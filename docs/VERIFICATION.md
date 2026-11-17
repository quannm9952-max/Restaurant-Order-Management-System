# Kiểm chứng bản tích hợp ngày 17/11/2026

Phạm vi: TV1, TV2, TV4 và TV5. TV3 chưa được tích hợp theo yêu cầu.

## Kết quả

Maven Wrapper `test` hoàn tất ngày 17/11/2026: **145 test, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**. Sau đó chạy `clean package -DskipTests` thành công để loại tài nguyên cũ khỏi JAR; không lặp lại kiểm thử đã đạt chỉ để đóng gói.

| Bộ test | Số test đạt |
| --- | ---: |
| ReportingIntegrationTests | 32 |
| RestaurantOrderManagementApplicationTests | 1 |
| CurrentEmployeeServiceImplTest | 4 |
| OrderServiceImplTest | 40 |
| TableServiceImplTest | 19 |
| SystemIntegrationTests | 47 |
| HttpSecurityIntegrationTests | 2 |

Smoke test HTTP/MySQL thực tế cũng đạt:

- Bốn role đăng nhập, 16 trang Manager render và 36 asset được tham chiếu trả về thành công.
- POST sai quyền trả đúng 403; thiếu CSRF bị chặn, các route TV3 tiếp tục bị chặn; logout kết thúc phiên.
- Waiter tạo đơn 1101 rồi hủy khi còn Pending; bàn được trả về Available.
- Hai phiên Cashier đồng thời thanh toán đơn fixture 1100: đúng **một Payment SUCCESSFUL, một Invoice**; đơn Completed và bàn Available, được đối chiếu lại bằng SQL.
- Tạo/sửa/ngừng bán/ngừng hoạt động món, tạo/sửa/vô hiệu hóa tài khoản qua form HTTP; giao dịch mới xuất hiện trong báo cáo ngày phát sinh.
- Sau restart JAR, đơn Completed/Cancelled, giá và trạng thái món, tài khoản bị khóa, số Payment/Invoice và trạng thái bàn đều được giữ nguyên.
- Cú pháp 7 file JavaScript local/inline hợp lệ. Không commit log, JAR hoặc database kiểm thử.

## Các lớp kiểm tra

- Maven `verify`: biên dịch, JUnit, Spring Security, entity/repository, nghiệp vụ, render Thymeleaf, HTTP servlet và đóng gói JAR.
- H2 độc lập cho test: dùng schema 9 bảng, chỉ bỏ cú pháp storage đặc thù MySQL; không truy cập database nghiệp vụ.
- MySQL Community **8.4.13**, cổng localhost riêng **3307**, database mới `rom_integration_demo`: chạy JAR với profile demo, kiểm tra HTTP và SQL thực tế. Không cài Windows service, không thay đổi database XAMPP cũ.
- Kiểm tra cú pháp toàn bộ JavaScript local và script inline của template.

## Các hành vi được kiểm tra

1. Đăng nhập bốn role bằng Account/Employee chung và BCrypt; sai mật khẩu hoặc tài khoản bị khóa bị từ chối; logout kết thúc phiên.
2. Route Manager, thao tác Waiter và checkout Cashier kiểm tra đúng quyền; POST thiếu CSRF bị chặn. Test HTTP servlet thật kiểm tra trang 403 sau forward, thay vì chỉ kiểm tra forwarded URL trong MockMvc.
3. Render các trang tài khoản, bàn, đơn, thực đơn, thanh toán, hóa đơn và báo cáo trên schema chung; bộ lọc bàn dùng enum đã cập nhật từ TV2.
4. Tạo đơn lưu nhân viên đang đăng nhập và giá lúc đặt; sửa/hủy món chỉ khi Pending; không mở hai đơn cho cùng bàn; hủy hợp lệ giải phóng bàn.
5. Ngừng bán món giữ Order_Detail và giá lịch sử; tạo/sửa tài khoản kiểm tra trùng username và mã hóa mật khẩu.
6. Checkout chỉ khi mọi món còn hiệu lực đã Served; không ghi nhận thành công hai lần. Ghi nhận failed hợp lệ giữ đơn Open và bàn Occupied.
7. Payment + Invoice + trạng thái Orders/Restaurant_tables cùng transaction; lỗi tạo hóa đơn rollback toàn bộ.
8. Doanh thu và lịch sử đọc cùng Payment/Orders; đối soát 24 payment thành công và 1,892.00 USD trong fixture báo cáo.
9. Route TV3 bị chặn; Chef có trang chủ chờ tích hợp. Fixture Served không phải một implementation của TV3.

Kết quả cuối cùng của các lệnh chạy được ghi trong lịch sử commit và nhật ký hoàn tất. File `backend/target/surefire-reports` và log chạy là dữ liệu local, bị gitignore.

## Giới hạn

Không có xác nhận độ khớp toàn bộ giao diện với Figma hoặc kiểm thử trực quan đầy đủ. Công cụ Windows đã tự dừng vì không xác định chắc URL Chrome. Không chuyển sang trình duyệt khác hoặc dùng automation khác để vượt chặn; các kiểm tra mã nguồn, MockMvc và HTTP/MySQL vẫn được thực hiện độc lập.

Không tuyên bố luồng tạo đơn → bếp → phục vụ hoàn chỉnh khi TV3 chưa có trong bản tích hợp. Những đơn mới Pending tiếp tục bị chặn thanh toán theo đúng nghiệp vụ.


## Đợt sửa giao diện theo PNG Figma (17/11/2026)

Bổ sung kiểm thử gán vai trò/khóa tài khoản mà không đổi password và employee, chặn actor khác sửa tài khoản/danh mục, xác nhận mật khẩu, danh mục trùng/rỗng/không tồn tại, giữ snapshot lịch sử, xem bill đơn Completed không cho thanh toán lại, khôi phục draft khi tạo đơn bị từ chối. Xem `FIGMA-AUDIT.md` về các màn hình đã đối chiếu trực quan và giới hạn asset.


Bản JAR sau sửa giao diện đã kiểm tra HTTP trên MySQL: **31 trang của 4 actor, 36 asset local đều trả thành công**, bao gồm form thêm/sửa, lọc trạng thái, đơn lịch sử, bill và hóa đơn. Các script `workspace.js`, `order-workspace.js`, `payment-workspace.js` đều qua `node --check`. Database `rom_figma_ui_check` trên MySQL demo cổng 3307 được tạo riêng cho kiểm tra trạng thái bill; database `rom_integration_demo` được giữ nguyên trong đợt kiểm tra này.

Kiểm tra Chrome trên database UI riêng: Generate Bill, tiền nhận 50 USD cho bill 39 USD, tiền thừa 11 USD, bật Process Payment và mở hộp thoại Confirm Payment. Dữ liệu thử chính vẫn giữ nguyên. Đã kiểm tra JAR sạch có shell mới và không còn theme/template cũ đã loại bỏ.
