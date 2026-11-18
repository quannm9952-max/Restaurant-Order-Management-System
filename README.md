# Restaurant Order Management System

Hệ thống quản lý nhà hàng: tài khoản, nhân viên, bàn, đặt món, bếp, phục vụ,
thực đơn, thanh toán/hóa đơn, tra cứu và báo cáo doanh thu.

## Mã nguồn và công nghệ

```text
frontend/    HTML, CSS, JavaScript, Thymeleaf, Bootstrap và ảnh/font
backend/     Java 21, Spring Boot, MVC, Security, JPA/JDBC và JUnit
database/    Schema, dữ liệu demo/test và SQL đối soát
```

Docker Compose đóng gói và chạy Spring Boot cùng MySQL 8.4.13. Công nghệ,
nghiệp vụ và giao diện giữ nguyên. Không cần XAMPP, MySQL hoặc JDK cài riêng
để chạy bằng Docker. VS Code/IntelliJ và GitHub vẫn dùng như trước.

## Chạy bằng Docker Compose

Cài và mở [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/),
chọn Linux containers và chờ Docker Engine hoạt động. Trên Windows, chuẩn bị
WSL2 theo hướng dẫn của Docker Desktop. Sau đó mở terminal tại gốc dự án:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
```

Hoặc chạy `run.cmd`; file này tạo `.env` nếu chưa có và gọi cùng lệnh Compose.
Mở **http://localhost:8080** khi log app báo đã khởi động.
Nếu cổng 8080 đang được ứng dụng khác sử dụng, đổi `APP_PORT` trong `.env`.

```powershell
docker compose ps
docker compose logs -f app
docker compose down
```

`down` giữ database trong volume `mysql-data` và ảnh món tải lên trong
`menu-images`. Không dùng `down -v` khi cần giữ dữ liệu. Đổi mật khẩu DB trong
`.env` không tự đổi mật khẩu của volume đã có.

| Actor | Username | Mật khẩu |
| --- | --- | --- |
| Manager | `demo_manager`, `DEMO_FIXTURE_MANAGER` | 123456 |
| Waiter | `demo_waiter`, `DEMO_FIXTURE_WAITER` | 123456 |
| Chef | `demo_chef`, `DEMO_FIXTURE_CHEF` | 123456 |
| Cashier | `demo_cashier`, `DEMO_FIXTURE_CASHIER` | 123456 |

Dùng đúng giá trị cột **Username** trong Employee Management để đăng nhập.
Username dài 1–50 ký tự, chỉ gồm A–Z, a–z, 0–9 và dấu gạch dưới `_`;
không dùng chữ có dấu, khoảng trắng hoặc ký tự đặc biệt. Khi nhập sai định
dạng, ô giữ nguyên nội dung và hiện cảnh báo đỏ để sửa; chỉ gửi khi username
hợp lệ. Quy tắc áp dụng cho đăng nhập và tạo/sửa tài khoản; họ tên nhân viên
vẫn dùng tiếng Việt.
Trang chủ chào **Welcome, [họ tên nhân viên]** theo tên hiện tại đã lưu.
Sai thông tin báo `Incorrect username or password.`; chỉ khi đúng username
và mật khẩu của tài khoản khóa mới báo `Your account has been locked.`
Trường trống được nhắc bằng tiếng Anh ở giao diện và backend.

Giao dịch mới chọn **Cash** hoặc **Bank Transfer**. Cashier phải đánh dấu
`I have received and verified the payment.`; tiền mặt cần nhập Amount Received
đủ tổng hóa đơn và giao diện tính Change Amount. Chuyển khoản dùng bảng QR
in sẵn ở quầy; Cashier tự kiểm tra tiền thực nhận trên tài khoản nhà hàng,
sau đó mới xác nhận. Phần mềm không tạo/hiển thị QR hoặc tự xác nhận ngân hàng.

Revenue Reports có thêm Cash Revenue, Bank Transfer Revenue và bảng tổng
hợp theo phương thức. Giữ quy tắc **Successful + đơn Completed**. Unknown
gồm phương thức trống/chưa phân loại và phương thức cũ ngoài hai nhóm như Card;
lịch sử giữ nguyên phương thức cũ và cho lọc theo phương thức. Xem
[chi tiết triển khai](backend/docs/payment-update.md).

Toàn hệ thống hiển thị tiền bằng **VND** theo định dạng Việt Nam,
ví dụ `85000` → `85.000 VND`. Dữ liệu cũ được chuyển một lần bằng hệ số
**10.000** theo yêu cầu: `8.50` → `85000` → `85.000 VND`. Áp dụng cho giá
món, giá lưu trong chi tiết đơn, tổng đơn, mọi thanh toán và hóa đơn cũ.
Đây là hệ số chuyển dữ liệu của đồ án, không phải tỷ giá tự cập nhật.
Menu, giỏ món, bill, hóa đơn, lịch sử, báo cáo và tiền thừa dùng cùng quy tắc.
Giá/số tiền nhận nhập dạng số; độ chính xác hai chữ số thập phân được giữ.
Dữ liệu demo/test đã dùng mức tiền VND mới. Database cũ cần chuyển theo
[hướng dẫn có sao lưu và chống nhân lặp](database/README.md); không chạy
chuyển đổi trên database mới tạo từ seed hiện tại.

Thanh toán và xem/in hóa đơn thuộc Cashier. Manager theo dõi doanh thu,
đơn hàng và lịch sử thanh toán qua các module báo cáo của Manager.

Serving Management của Waiter có hai mục **Ready to serve** và **Served**.
Sau khi xác nhận, xem lại bàn, mã đơn, món/số lượng và tiến độ đã phục vụ;
phân biệt một phần/toàn bộ, kể cả đơn đã thanh toán và bàn được dùng cho đơn
mới. Lọc theo bàn hoặc tìm món/mã đơn/dòng món; chuyển mục giữ bộ lọc.
Lịch sử chỉ xem, hiển thị thời gian tạo đơn; không xác nhận lại món đã Served.

Revenue Reports có Auto, Day, Week và Month. Auto chọn ngày cho 1–31 ngày,
tuần cho 32–90 ngày, tháng cho khoảng dài hơn; số ngày tính cả hai đầu.
Biểu đồ nhiều cột giảm nhãn trục X; rê chuột, focus hoặc chạm cột để xem
doanh thu và số giao dịch chính xác. Nhóm tuần/tháng chỉ cộng ngày được lọc.

Menu Management cho Manager thêm/sửa món và tải ảnh JPG/PNG tối đa 5 MB.
Khi sửa, không chọn ảnh mới sẽ giữ ảnh hiện tại. Nút Delete xóa món khỏi thực
đơn và danh sách đặt món mới, giữ nguyên dữ liệu trong các đơn trước đó.
Chỉ xóa danh mục khi đã chuyển hoặc xóa hết món, kể cả món đang Stop Selling.
Xóa, cập nhật và đổi trạng thái món giữ nguyên danh mục, từ khóa và bộ lọc
của danh sách đang xem, kể cả khi chuyển món sang danh mục khác.

Container app dùng profile demo để tạo schema/dữ liệu còn thiếu; khởi động
lại giữ các thay đổi và mật khẩu đã lưu. `.env` chứa cấu hình local và được
Git bỏ qua. Xem `.env.example` để biết cổng và tài khoản MySQL mẫu.

MySQL Docker mặc định ở **localhost:3308** để không trùng MySQL/XAMPP cũ.
DBeaver kết nối bằng MYSQL_DATABASE/MYSQL_USER/MYSQL_PASSWORD trong `.env`.
Trong mạng Docker, app kết nối `db:3306`.

Giao diện dùng được trên điện thoại và iPad với cùng link website: menu
điều hướng mở/đóng trên màn hình nhỏ, bố cục tự xếp lại, bảng và biểu đồ rộng
cuộn trong khung riêng. Form/lịch có giới hạn chiều cao và nút dễ chạm.

## Kiểm thử

```powershell
.\verify.cmd
```

Lệnh build stage verify trong Docker, chạy Maven/JUnit/H2 như trước. Có thể
chạy trực tiếp khi máy phát triển đã có JDK 21:

```powershell
cd backend
.\mvnw.cmd --no-transfer-progress verify
```

GitHub Actions chạy kiểm thử Java và kiểm tra khởi động Docker Compose trên
Linux. Image runtime chứa JRE, JAR và SQL; frontend nằm trong JAR. Các file
build, log, `.env` và bản sao lưu DB không được đưa vào image hoặc Git.

- [Hướng dẫn Docker và chuyển dữ liệu cũ](backend/docs/docker.md)
- [Kiến trúc và quy ước](backend/docs/architecture.md)
- [Yêu cầu nghiệp vụ](backend/docs/requirements.md)
- [Kết quả kiểm thử](backend/docs/testing.md)
- [Giao diện](backend/docs/design.md)
- [Thư viện](backend/docs/dependencies.md)
- [Database](database/README.md)
