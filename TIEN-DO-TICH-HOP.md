# Tiến trình tích hợp được tạm dừng ngày 17/11/2026

Người dùng yêu cầu dừng tại đây, lưu tiến trình và thực hiện tiếp vào ngày hôm sau. Không tiếp tục sửa, kiểm thử hoặc đưa code lên main cho đến khi người dùng yêu cầu tiếp tục.

## Mục tiêu và phạm vi đã được chấp thuận

- Tổng hợp TV1, TV2, TV4 và TV5 thành một hệ thống trên main.
- Để TV3 (UC06–UC07, bếp và xác nhận phục vụ) tích hợp sau.
- Giữ schema 9 bảng theo `database/RestaurantOrderManagement_Database.sql`; không tự thêm cột/bảng ngoài ERD.
- Đã được người dùng cho phép tích hợp vào main, nhưng bản hiện tại chưa sẵn sàng và chưa được push.

## Vị trí và Git

- Thư mục bản tích hợp: `D:\công nghệ phần mềm\restaurant-order-management\restaurant-order-management\rom-system-integration`.
- Nhánh làm việc: `integration/system-without-tv3`, tạo từ `origin/main` tại `e676c01`.
- Repository gốc vẫn ở `..\restaurant-order-management-git`, nhánh TV5 `feature/reports-history`; không thay đổi file chưa track của người dùng trong repository đó.
- Đã merge lịch sử TV5 `2fb01ea`, TV1 `67725ac`, TV2 `b74e9c3`, TV4 `f7e5295` vào nhánh tích hợp. TV4 có lịch sử riêng nên đã dùng `--allow-unrelated-histories`.
- Các merge commit: `c39ca27` (TV5), `ecab11a` (TV1), `9fed1e6` (TV2), `c384961` (TV4).
- Xung đột `.gitattributes`/`.gitignore` đã giải quyết; giữ trang login TV1 khi trùng file TV4.
- Local main vẫn `44deada`, remote-tracking origin/main vẫn `e676c01` tại lúc lưu. Chưa merge nhánh tích hợp vào main, chưa push bất kỳ thay đổi tích hợp nào lên GitHub.
- Có checkpoint WIP cục bộ sau các merge; dùng `git log -1` để xem mã commit lưu tiến trình.

## Đã thực hiện

- Gom code thành một ứng dụng Java 21/Spring Boot 4.1.2 trong `backend`, frontend Thymeleaf trong `frontend`, SQL trong `database`.
- Điểm khởi động mới: `backend/src/main/java/com/restaurant/RestaurantApplication.java`.
- Chuyển code TV1/TV2 về namespace `com.restaurant`, dùng chung entity/repository, sửa các ánh xạ lệch ERD.
- Dùng Account/Employee TV1 với FK đúng `Account.employee_id`; sửa cột Employee.position để chạy với H2 và MySQL.
- Một cấu hình Spring Security và đăng nhập DB chung; dùng người đăng nhập thực tế khi TV2 tạo đơn, bỏ tài khoản Waiter gán cứng.
- Phân quyền route và service, giữ CSRF, nối điều hướng giữa các module và logout.
- Chặn route `/kitchen/**`, `/serving/**`; Chef đăng nhập được nhưng thấy thông báo chờ module bếp.
- Chuyển phần TV4 thành MenuService/PaymentService và repository truy cập schema chung; bỏ service toàn hệ thống và auth trùng của TV4.
- Thanh toán kiểm tra Open, Served, chống trùng, khóa đơn; Payment + Invoice + hoàn tất đơn + giải phóng bàn cùng transaction.
- Menu deactivation giữ lịch sử, bỏ hard delete và force delete khỏi nghiệp vụ; không thêm cột ảnh vào ERD, thumbnail dùng tài nguyên tĩnh.
- Repository truy vấn TV5 đổi thành OrderReportingRepository/PaymentReportingRepository để tránh trùng bean với JPA.
- Thêm khóa cho thao tác sửa đơn và cập nhật bàn; có form thêm/sửa bàn dành cho Manager.
- Thống nhất dữ liệu demo USD; có fixture đơn 1100 đã Served để thử checkout trong khi TV3 chưa tích hợp. Đây là dữ liệu demo, không phải chức năng thay TV3.
- `database/seed.sql` giữ nguyên fixture kiểm thử báo cáo; `demo-seed.sql` không reset dữ liệu đã thao tác; `demo-extra.sql` bổ sung checkout và menu demo.
- Thêm profile `demo` và DemoAccounts; chỉ tạo demo account ở profile này. Cấu hình mặc định không tự nạp SQL.
- Bootstrap CSS/JS, font và ảnh menu đã lưu local; đã định dạng 77 file Java bằng google-java-format.
- Bản tích hợp đã từng build/đóng gói thành công trước các chỉnh sửa và kiểm thử bổ sung gần nhất.

## Kiểm thử gần nhất — chưa đạt toàn bộ

Bộ cũ đã có một lần 96 test đạt. Lần `verify` mới nhất tổng cộng **129 test**, gồm **127 đạt, 1 failure, 1 error**:

| Bộ test | Số test | Failure | Error |
| --- | ---: | ---: | ---: |
| ReportingIntegrationTests | 32 | 0 | 0 |
| RestaurantOrderManagementApplicationTests | 1 | 0 | 0 |
| CurrentEmployeeServiceImplTest | 4 | 0 | 0 |
| OrderServiceImplTest | 40 | 0 | 0 |
| TableServiceImplTest | 19 | 1 | 0 |
| SystemIntegrationTests | 33 | 0 | 1 |

Hai điểm cần xử lý trước:

1. `TableServiceImplTest.updateTableStatus_tableNotFound_shouldThrowNotFoundException`: test đang kiểm tra thông báo chứa `Không tìm thấy bàn`, trong khi code khóa bàn mới trả `Table not found.`. Xem service và test, thống nhất thông báo phù hợp; không bỏ kiểm tra nghiệp vụ.
2. `SystemIntegrationTests.screensRenderAgainstOneSharedSchema(String)[12]`, trang menu: template gọi `#strings.substring(m.created_at,0,10)` khi `created_at` là chuỗi rỗng. ERD không có cột này cho Menu_item. Bỏ hiển thị ngày không có dữ liệu hoặc xử lý giá trị rỗng, không tự tạo ngày/cột giả.

Các kiểm tra mới còn lại đã đạt: đăng nhập 4 role bằng DB/BCrypt, tài khoản bị khóa/sai mật khẩu, tạo/hủy đơn với người dùng thật và giá lịch sử, checkout/hóa đơn, rollback khi tạo hóa đơn lỗi, phân quyền, CSRF, các route TV3 bị chặn, thay đổi menu giữ lịch sử và nghiệp vụ tài khoản.

Log và XML kết quả ở `backend/verify-system.log`, `backend/system-test.log`, `backend/target/surefire-reports/`. Đây là file local bị gitignore, không push log hoặc target.

## MySQL Community để kiểm thử thực tế

- Đã tải bản portable chính thức `mysql-8.4.13-winx64.zip`, kiểm tra MD5 `7b94489d5524dea9b27097068bea25a8` khớp trang MySQL.
- File và công cụ tạm ở `C:\Users\Lenovo\AppData\Local\Temp\rom-integration-tools`.
- Thư mục binary: `mysql\mysql-8.4.13-winx64`; data riêng: `mysql-data`.
- `mysqld --initialize-insecure` đã thành công cho data tạm. Chưa cài Windows service, chưa thay đổi DB XAMPP hiện có.
- Dự kiến chỉ bind `127.0.0.1:3307`, tắt mysqlx, dùng DB mới `rom_integration_demo`; chưa xác nhận tạo DB hoặc chạy app thành công.
- Lần Start-Process đầu chưa mở cổng; lần foreground kế tiếp báo `CreateProcess Failed (267)`. Lệnh thử từ thư mục tạm rõ ràng đã bị người dùng ngắt để dừng công việc. Khi tiếp tục cần kiểm tra process/port trước, không khởi tạo lại hay xóa data mù quáng.
- Chưa xác minh hệ thống trên MySQL thật; chưa kiểm tra giao diện thực tế bằng trình duyệt.
- Formatter jar nằm cùng thư mục tạm; Maven có ở `C:\Users\Lenovo\.m2\wrapper\dists\apache-maven-3.10.0\2aaba761688b1a6ea5a46e7e5f4d5406ed7a75ee8da95e5e3079863a23461c9b\bin\mvn.cmd`.

## Bước tiếp theo sau khi người dùng yêu cầu tiếp tục

1. Đọc file này, kiểm tra `git status` và checkpoint; không chạy lại `integrate_sources.py` vì script nhập nguồn đã được chạy, các thư mục nguồn cũ đã được chuyển/xóa trong worktree.
2. Sửa đúng hai lỗi kiểm thử còn lại; chạy lại test liên quan, rồi `verify` đầy đủ.
3. Kiểm tra các thao tác giao diện, JS inline, CSRF và asset; đối chiếu template Thymeleaf thực tế. Form và link TV3 phải tiếp tục để tích hợp sau.
4. Khởi động MySQL tạm an toàn, chạy bản JAR với profile demo và DB riêng; xác minh schema, login, CRUD cần thiết, checkout, invoice, báo cáo. Giữ database cũ nguyên vẹn.
5. Kiểm tra seed chạy lại không ghi đè thao tác; cân nhắc thêm kiểm thử concurrency và restart khi có rủi ro thực tế cần chứng minh.
6. Cập nhật README chính và hướng dẫn chạy/verify cho hệ thống, frontend/backend/database, tài khoản demo, dữ liệu mẫu và phần TV3 còn thiếu. Các README/script TV5 hiện vẫn là bản cũ và cần chỉnh; không tuyên bố chúng đã hoàn thiện.
7. Gỡ script migration `integrate_sources.py` khỏi bản giao cuối; kiểm tra không còn code trùng, credentials thật, binary hoặc file log trong Git.
8. Fetch main lần nữa để tránh ghi đè cập nhật nhóm; xử lý thay đổi mới nếu có. Chỉ merge/fast-forward và push main khi các kiểm tra cần thiết đạt. Người dùng đã cho phép bước này; không force push.

Không tự động thực hiện tiếp hoặc hẹn giờ. Chờ người dùng yêu cầu tiếp tục.
