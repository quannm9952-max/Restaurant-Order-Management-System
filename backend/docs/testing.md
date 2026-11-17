# Kiểm chứng hệ thống

## Menu Management: xóa danh mục, xóa món và tải ảnh

Docker build stage `verify`: **263 test đạt, 0 failure, 0 error, 0 skipped;
BUILD SUCCESS**. Bổ sung 17 ca MenuManagementIntegrationTest và 2 ca HTTP
multipart thật; không thêm dependency hoặc thay đổi schema.

Kiểm tra danh mục rỗng được xóa/khôi phục cùng tên, danh mục có món (kể cả
ngừng bán) bị chặn xóa, không thêm món vào danh mục đã xóa. Món xóa khỏi
thực đơn và danh sách đặt món mới nhưng giữ các dòng Order_Detail cũ.

Ảnh kiểm tra định dạng thực JPG/PNG, kích thước byte/pixel, từ chối SVG/GIF,
file giả, rỗng hoặc quá lớn; không dùng filename người gửi làm đường dẫn.
Thêm/sửa ảnh, giữ ảnh khi không tải lại và rollback database/ảnh đều có test.
HTTP thật xác nhận multipart đọc CSRF trong body, thiếu CSRF trả 403,
file quá 5 MB hiện lỗi và không tạo món. Waiter không được sửa/xóa menu.
Cập nhật 18/11/2026: ca xóa món xác nhận redirect giữ danh mục, tìm kiếm,
trạng thái, khoảng giá và sắp xếp; nếu món đã bị xóa trước đó, thông báo lỗi
vẫn giữ danh mục. Docker/Cloudflare đã kiểm tra form và chuyển hướng giữ đủ
bộ lọc; checksum 9 bảng chính trước/sau cập nhật không đổi.

Chrome headless trên Docker/MySQL riêng đã thao tác thêm/sửa/chuyển/xóa món,
xóa danh mục rỗng, chặn danh mục còn món, xem trước và thay ảnh. Tạo lại
container app vẫn giữ ảnh và metadata. Xóa món mẫu đã được đặt trước đó giữ
nguyên 27 dòng lịch sử; checksum Orders, Order_Detail, Payment, Invoice,
Employee và Account không đổi. Cơ sở dữ liệu thử tách riêng bản đang sử dụng.
`node --check` và `git diff --check` đạt.

Bản đang dùng trên Docker và Cloudflare đã đăng nhập Manager, render các
nút xóa/trường tải ảnh, tải CSS/JS/icon thành công; ảnh không tồn tại trả 404.
Waiter/Chef/Cashier truy cập Menu Management trả 403. Checksum đủ 9 bảng
database chính trước/sau cập nhật giống nhau; không tạo dữ liệu thử ở bản chính.

## Kết quả

- Maven `verify`: **208 test, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**.
- Cú pháp 5 JavaScript của dự án: `node --check` đạt.
- MySQL thật: **99 mục đạt**, gồm 27 trang của 4 actor và 38 asset local.
- JAR không còn package hoặc đường dẫn asset cũ sau khi chuyển thư mục.
- `git diff --check` đạt; không đưa log, JAR, file tạm hoặc DB thử vào Git.

| Bộ kiểm thử | Số ca đạt |
| --- | ---: |
| ApplicationTest | 1 |
| CurrentEmployeeServiceTest | 4 |
| HttpSecurityTest | 2 |
| OrderServiceTest | 40 |
| PreparationIntegrationTest | 45 |
| ReportingIntegrationTest | 32 |
| SystemIntegrationTest | 64 |
| TableServiceTest | 20 |

## Nghiệp vụ được giữ nguyên

Bốn actor đăng nhập đúng quyền; CSRF, khóa tài khoản, xác nhận và mã hóa mật
khẩu; tạo/sửa bàn; tạo đơn, giảm/hủy món Pending, hủy đơn hợp lệ và giữ giá
lúc đặt. Chef chỉ chuyển Pending → Preparing → Ready, Waiter chỉ xác nhận
Ready → Served. Cashier chỉ thanh toán khi các dòng còn hiệu lực đã Served.

Có kiểm thử rollback và đồng thời: hai Chef, chế biến so với hủy đơn,
phục vụ so với thanh toán, hai Cashier. Chỉ một Payment SUCCESSFUL và một
Invoice; đơn Completed và bàn Available cùng transaction. Báo cáo đọc đúng
Payment/Orders; lỗi đầu vào/không tồn tại/sai method trả đúng mã HTTP.

Các kiểm thử tích hợp render template nên bắt được đường dẫn Java enum cũ
trong Thymeleaf. Bản hiện tại lấy roles/statuses từ controller và dùng chung
enum trạng thái giữa nghiệp vụ với báo cáo.

## HTTP/MySQL

Database riêng `rom_cleanup_verification` trên MySQL Community 8.4.13, cổng
3307, ứng dụng 8081. Đơn thử **1101**, hai dòng **9206/9207**, tổng **138.50
USD** đi qua toàn bộ đặt món → bếp → phục vụ → Bank Transfer → hóa đơn/báo cáo.
Hai Chef cùng Start chỉ có một lần hợp lệ; hai Cashier cùng xác nhận chỉ tạo
một thanh toán thành công. Các trang và ảnh/font/CSS/JS mới trả HTTP 200.

Bản chính chạy **http://localhost:8080** trên DB `rom_integration_demo`.
Đối chiếu fingerprint toàn bộ dữ liệu của **9 bảng trước và sau khởi động**
đều khớp; bốn actor đăng nhập và mở màn hình nghiệp vụ thành công bằng
mật khẩu demo `123456`. Không reset database chính.

## Thay đổi cấu trúc

251 → 186 file nguồn/cấu hình/tài liệu được quản lý; 80 → 75 file Java chính;
5,095 → 4,368 dòng Java chính. Ảnh/icon: 74 → 34 file.

Bỏ 3 interface service chỉ có một implementation; gộp controller/service
quản lý bàn; bỏ accessor ID trùng; dùng chung enum; rút gọn form bằng Lombok
đã có. Script demo gộp thành demo-data.sql, test-data.sql dành riêng kiểm thử.
Tài liệu gốc, sơ đồ, bản code trước dọn và log cũ đã có bản sao lưu local bên
ngoài thư mục dự án.

Đây là kiểm chứng chức năng và cấu trúc; không phải kiểm thử tải hoặc xác
nhận độ khớp Figma 100% bằng quan sát trình duyệt. GitHub Actions được cấu
hình chạy verify; kết quả trên là kết quả chạy local.

## Bổ sung môi trường Docker Compose

Đợt này chỉ thay file đóng gói/chạy và hướng dẫn. Git diff xác nhận POM,
backend/src, frontend và SQL nghiệp vụ không đổi. Maven test chạy lại:
**208 test đạt, 0 failure, 0 error**. Compose YAML hợp lệ với JSON Schema
chính thức của Compose Specification; có đúng app Spring Boot và db MySQL.

Job `docker` trong GitHub Actions kiểm tra `compose config`, build/chạy hai
container trên Linux, đăng nhập bốn actor, kiểm tra 9 bảng và giá món đã sửa
được giữ qua down/up bằng cùng volume. Xem kết quả tại
[GitHub Actions](https://github.com/quannm9952-max/Restaurant-Order-Management-System/actions).

Máy làm việc chưa có Docker Desktop/WSL2; theo lựa chọn của người dùng, agent
không cài hệ thống và chưa chạy Docker local. Hướng dẫn ở docker.md để chạy
sau khi người dùng tự cài. MySQL và dữ liệu local cũ không bị sửa/xóa.

## Cập nhật đăng nhập username và lời chào (17/11/2026)

Maven `verify`: **217 test đạt, 0 failure, 0 error, 0 skipped**;
SystemIntegrationTest có 73 ca. Bao gồm đăng nhập 8 username mẫu, từ chối
họ tên/mật khẩu sai, tài khoản mới tạo qua Manager và lời chào đầy đủ họ tên
tiếng Việt. Khóa tài khoản và mật khẩu đã sửa được giữ khi chạy lại khởi tạo demo.

Docker/MySQL thật: cả 8 tài khoản trong Employee Management đăng nhập bằng
đúng cột Username, mật khẩu `123456`, vào được màn hình đúng quyền. Trang chủ
và tên ở header khớp tên nhân viên. Chrome headless kiểm tra giao diện 4 actor,
tài khoản mẫu cũ, tìm kiếm username và bộ lọc role của Manager.

Đã sao lưu DB trước cập nhật. Checksum xác nhận chỉ Account 9001–9004 được
kích hoạt và đặt mật khẩu ban đầu; 4 tài khoản demo đang hoạt động và 8 bảng
khác giữ nguyên. Khởi động lại app giữ nguyên fingerprint của cả 9 bảng và
8 tài khoản. Đăng nhập qua HTTPS Cloudflare Tunnel cũng đạt.

## Giới hạn ký tự username (17/11/2026)

Maven `verify`: **232 test đạt, 0 failure, 0 error, 0 skipped**;
SystemIntegrationTest có 88 ca. Tạo/sửa và đăng nhập từ chối chữ có dấu,
dấu kết hợp Unicode, khoảng trắng, emoji, dấu chấm/gạch ngang/@/slash.
Username 1–50 ký tự ASCII gồm chữ cái, số và gạch dưới được chấp nhận;
họ tên tiếng Việt và quy tắc mật khẩu giữ nguyên.

MySQL/Docker thật: cả 8 tài khoản đăng nhập thành công, biến thể có dấu
`demo_mánager` và khoảng trắng đầu/cuối đều bị từ chối trước khi truy vấn
tài khoản. Đăng nhập hợp lệ/không hợp lệ qua Cloudflare cũng đạt.
Chrome headless kiểm tra gõ, dán, composition của IME và HTML pattern tại
login, thêm nhân viên và sửa nhân viên. Mobile 320×568 không tràn nội dung.
Checksum của cả 9 bảng trước/sau cập nhật không đổi.

## Thông báo đăng nhập (17/11/2026)

Maven `verify`: **236 test đạt, 0 failure, 0 error, 0 skipped**;
SystemIntegrationTest 90 ca, HttpSecurityTest 4 ca. Sai username/mật khẩu,
kể cả mật khẩu sai của tài khoản khóa, chỉ nhận thông báo sai thông tin.
Chỉ đúng cả username và mật khẩu của tài khoản khóa mới hiện trạng thái khóa.
Có kiểm thử trường trống, mật khẩu quá dài, query giả trạng thái và xóa thông
báo sau khi hiển thị. Hai ca HTTP thật xác nhận thông báo qua servlet/redirect.

Chrome headless với locale vi xác nhận nhắc username/password trống vẫn bằng
tiếng Anh, focus đúng trường, sửa lại cho phép đăng nhập và lỗi sai thông tin
chỉ có một dòng. Docker/Cloudflare kiểm tra 7 trường hợp sai/trống; cả 8
tài khoản hiện có vẫn đăng nhập. Checksum cả 9 bảng trước/sau không đổi.

## Cảnh báo username và giữ nội dung (17/11/2026)

Thay đổi frontend: giữ nguyên giá trị khi nhập sai, hiện cảnh báo dưới trường
và viền đỏ, chặn gửi đến khi sửa đúng. Bỏ xử lý xóa/khôi phục chữ và chặn
paste/beforeinput; giới hạn 50 ký tự chuyển sang cảnh báo và kiểm tra pattern.
Quy tắc backend giữ nguyên.

Chrome headless kiểm tra login, thêm và sửa nhân viên: chữ có dấu, khoảng
trắng, dấu chấm/gạch ngang/@, emoji và 51 ký tự không bị xóa hoặc cắt. Sự kiện
paste và composition giữ nội dung/vị trí con trỏ. Sửa thành username hợp lệ
xóa cảnh báo, bỏ trạng thái aria-invalid và cho qua kiểm tra form.
Ba tình huống trường trống vẫn nhắc bằng tiếng Anh, sửa đúng đăng nhập được
và sai mật khẩu vẫn chỉ có một dòng thông báo. `node --check` và Docker build đạt.

## Quyền thanh toán và hóa đơn của Cashier (17/11/2026)

Maven `verify`: **244 test đạt, 0 failure, 0 error, 0 skipped**;
SystemIntegrationTest có 98 ca. Bỏ shortcut View payments & invoices của
Manager. SecurityFilterChain và PaymentService chỉ cho Cashier truy cập
trang thanh toán, API chi tiết thanh toán, hóa đơn và xác nhận thanh toán.
Kiểm thử Manager gọi trực tiếp service cũng bị từ chối.

Docker/MySQL và Cloudflare: Manager bị 403 ở /payments, API và /invoices;
Cashier nhận 200. Waiter/Chef cũng bị từ chối. Dashboard Manager giữ liên kết
báo cáo doanh thu và lịch sử thanh toán; các trang này vẫn trả 200 cho Manager.
Cashier không được vào báo cáo Manager. Checksum cả 9 bảng trước/sau không đổi.
