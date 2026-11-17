# Kiểm chứng toàn hệ thống ngày 17/11/2026

Phạm vi: đủ TV1–TV5, UC01–UC12, giao diện theo các PNG Figma đã cung cấp và
đợt rà soát Ch4 - Implementation/Programming. Đây là kết quả bản tích hợp
hiện tại; các ghi chú trước đây về TV3 chờ tích hợp đã được thay thế.

## Build và kiểm thử tự động

Lệnh `backend/mvnw.cmd --no-transfer-progress formatter:format verify`:
**208 test, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**.
Lệnh `verify` bao gồm kiểm tra định dạng Java, biên dịch, JUnit và đóng gói JAR.

| Bộ test | Số test đạt |
| --- | ---: |
| HttpSecurityIntegrationTests | 2 |
| ReportingIntegrationTests | 32 |
| RestaurantOrderManagementApplicationTests | 1 |
| PreparationIntegrationTests | 45 |
| CurrentEmployeeServiceImplTest | 4 |
| OrderServiceImplTest | 40 |
| TableServiceImplTest | 20 |
| SystemIntegrationTests | 64 |

Kiểm tra cú pháp bằng `node --check`: cả 5 JavaScript do dự án viết đều đạt.
Tất cả câu lệnh Java nằm trong 80 ký tự sau định dạng; package/import dài là
ngoại lệ. `git diff --check` không phát hiện lỗi khoảng trắng.

## Các hành vi được kiểm tra

- Bốn actor dùng chung Account/Employee, BCrypt, khóa tài khoản, đăng xuất,
  bảo vệ CSRF và phân quyền ở HTTP lẫn service.
- Tạo/sửa tài khoản, xác nhận mật khẩu; mật khẩu `123456` được chấp nhận;
  từ chối dưới 6 ký tự hoặc vượt 72 byte UTF-8 trước khi mã hóa.
- Bàn Available/Occupied/Out of service, không mở hai đơn trên cùng bàn;
  kiểm tra tên trùng và giữ dữ liệu form khi bị từ chối.
- Tạo đơn bằng nhân viên đang đăng nhập; giữ tên/giá lúc đặt; chỉ sửa hoặc
  hủy món khi Pending; hủy đơn theo đúng điều kiện nghiệp vụ.
- Chef chỉ chuyển từng dòng Pending → Preparing → Ready; Waiter chỉ xác
  nhận Ready → Served trên đơn Open. Từ chối bỏ bước, đi lùi, dòng thuộc đơn
  khác, dòng Cancelled/Served, đơn Completed/Cancelled và xác nhận lặp.
- Kitchen/Serving lọc và đếm theo dữ liệu thực, trạng thái rỗng, thông báo
  Ready, mã revision và khôi phục khi save thất bại. HTML/note được escape.
- Các ca đồng thời: hai Chef, bắt đầu nấu so với hủy đơn, phục vụ so với
  thanh toán; lỗi save phải rollback, không có trạng thái đơn/bàn trộn lẫn.
- Thanh toán Cash/Card/Bank Transfer chỉ khi mọi dòng còn hiệu lực đã Served;
  một lần SUCCESSFUL, một Invoice; Orders Completed và bàn Available cùng
  transaction. Failed giữ Open/Occupied; lỗi tạo Invoice rollback toàn bộ.
- Báo cáo và lịch sử đọc cùng Payment/Orders; fixture 24 SUCCESSFUL,
  1,892.00 USD; không cộng pending/failed hay bản ghi không nhất quán vào doanh thu.
- Danh sách checkout tổng hợp khớp chi tiết lịch sử, kể cả Cancelled và đơn
  không còn dòng hiệu lực; đọc ánh xạ đơn Open một lần khi hiển thị danh sách bàn.
- Tìm kiếm literal với `%`, `_`, `!` và chuỗi giống SQL injection; kiểm tra
  đầu vào sai kiểu 400, bản ghi/tài nguyên không tồn tại 404, sai method 405.
- Giả lập lỗi DB: HTML 500 có mã tra cứu, JSON 500 thông báo trung tính;
  câu SQL/đường dẫn máy chủ/nội dung lỗi nội bộ không xuất hiện ở response.

Chi tiết UC06/UC07 xem [member3/TEST-CASES.md](member3/TEST-CASES.md).
Đối chiếu bài giảng xem [IMPLEMENTATION-REVIEW.md](IMPLEMENTATION-REVIEW.md).

## HTTP và MySQL thật

Dùng OpenJDK 21, MySQL Community 8.4.13, cổng 3307, database thử riêng
`rom_tv3_verification`, ứng dụng cổng 8081. Kết quả: **99 mục kiểm tra đạt**,
bao gồm **27 trang của 4 actor và 38 asset local**, đăng nhập, nghiệp vụ,
quyền, CSRF, mã HTTP và đối soát SQL.

Đơn kiểm chứng cuối **1103**, dòng **9210/9211**, tổng **138.50 USD**:

1. Waiter tạo đơn qua form HTTP; Chef không thể đi từ Pending thẳng Ready.
2. Hai phiên Chef cùng Start một dòng: đúng một lần hợp lệ, một lần bị từ
   chối; dòng khác còn Pending.
3. Chef Preparing/Ready rồi Waiter xác nhận Served, badge/queue cập nhật.
4. Hai Cashier cùng thanh toán Bank Transfer: đúng một Payment SUCCESSFUL,
   một Invoice; đơn Completed, bàn Available. Trước Served không tạo Payment.
5. Invoice render được; báo cáo ngày phát sinh khớp tổng tiền MySQL; snapshot
   tên, số lượng và giá vẫn giữ nguyên xuyên suốt các bước.

Có ba lượt chạy trên DB thử riêng khi hoàn thiện script đọc ảnh; các đơn
1101–1103 đều là dữ liệu thử. Không reset DB demo chính. Sau restart JAR,
đối chiếu fingerprint toàn bộ dữ liệu của **9 bảng ở cả hai database** đều
khớp trước restart. DB chính `rom_integration_demo` vẫn có **9 tài khoản**;
bốn tài khoản demo đăng nhập bằng `123456` và mở được màn hình đúng vai trò
trên bản mới ở **http://localhost:8080**.

Dữ liệu, log, JSON kiểm chứng, PDF trích xuất, công cụ tạm và JAR nằm trong
vùng local bị gitignore; không đưa vào repository. GitHub Actions được cấu
hình chạy cùng `verify` khi push/PR; kết quả Maven ở trên là kết quả chạy local,
không phải tuyên bố workflow GitHub đã chạy thành công.

## Giới hạn kiểm chứng giao diện

Đã đối chiếu cấu trúc và trạng thái với PNG Figma; các trang được render qua
Thymeleaf/HTTP. Đợt trước đã quan sát một số màn hình trên Chrome. Công cụ
Windows về sau dừng vì không xác định chắc URL trình duyệt, nên chưa xác nhận
trực quan toàn bộ Kitchen/Serving mới hoặc độ khớp Figma 100%. Không coi số
trang HTTP thành công là bằng chứng pixel-perfect hay kiểm thử tải.
