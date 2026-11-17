# Đối chiếu hướng dẫn Ch4 - Implementation/Programming

Ngày kiểm tra: 17/11/2026. Đã đọc nội dung và xem toàn bộ 47 trang của
`Ch4 - Implementation_Programming.pdf` do người dùng cung cấp.
Phạm vi áp dụng: toàn bộ hệ thống TV1–TV5, UC01–UC12.

## Đối chiếu và sửa đổi

| Nội dung trong bài giảng | Áp dụng trong hệ thống | Bằng chứng |
| --- | --- | --- |
| Trang 3–7: hiện thực thiết kế thành chương trình, tổ chức đối tượng/module | Một ứng dụng Spring Boot; Controller nhận HTTP, Service kiểm tra nghiệp vụ, Repository truy cập DB; dùng chung 9 entity/bảng | `backend/src/main/java/com/restaurant`, `database/schema.sql`, `docs/INTEGRATION.md` |
| Trang 8–13: ngôn ngữ, nền tảng và công cụ phù hợp | Giữ Java 21, Spring Boot, HTML/CSS/JS, Thymeleaf, Bootstrap và MySQL đúng yêu cầu nhóm | `backend/pom.xml`, `frontend`, `run.cmd` |
| Trang 14–15: tên rõ nghĩa, thụt dòng nhất quán, dòng ngắn | Java: PascalCase cho lớp, camelCase cho biến/hàm, UPPER_SNAKE_CASE cho hằng; 2 dấu cách; UTF-8; formatter cấu hình 80 ký tự | `.editorconfig`, `.gitattributes`, `backend/config/eclipse-formatter.xml` |
| Trang 15: tách xử lý lỗi khỏi luồng chính | Lỗi nghiệp vụ trả thông báo có thể sửa; lỗi hệ thống ghi log, trả thông báo trung tính kèm mã tra cứu; HTTP 400/404/405/500 đúng loại | `exceptions/GlobalExceptionHandler.java`, Menu/Payment/PreparationController |
| Trang 18–24: hiệu quả thời gian và bộ nhớ, tránh tính/truy vấn lặp | Danh sách checkout tổng hợp trong một truy vấn, chỉ nạp chi tiết đơn được chọn; danh sách bàn đọc ánh xạ đơn Open một lần; không tải lại toàn danh sách để kiểm tra tên trùng; tải employee/category bằng EntityGraph | CheckoutRepository, TableServiceImpl, TableCatalogService, AccountRepository, MenuItemRepository |
| Trang 25–29: tái sử dụng thành phần | Hai repository menu/checkout dùng chung helper JDBC, không kế thừa lẫn nghiệp vụ; dùng chung SqlSearch để tìm literal; shell, font, Bootstrap local được tái sử dụng | JdbcRepositorySupport, SqlSearch, `frontend/templates/shared/shell.html` |
| Trang 30–33: quản lý phiên bản, build và kiểm tra tích hợp | Nhánh tính năng riêng; Maven Wrapper; `verify` kiểm tra định dạng, biên dịch, chạy JUnit và đóng gói; thêm workflow push/PR | `verify.cmd`, `backend/pom.xml`, `.github/workflows/verify.yml` |
| Trang 34–37: môi trường phát triển và môi trường chạy | Máy phát triển dùng JDK/Maven/test H2; ứng dụng chạy bằng Java và MySQL; frontend nằm trong JAR; cấu hình DB bằng biến môi trường | README, application.properties, application-demo.properties |
| Trang 39–45: sử dụng thư viện mở và ghi nhận giấy phép | Ghi lại các thư viện chính, phiên bản trong JAR, font license và nguồn thông báo bản quyền | `docs/DEPENDENCIES.md`, `docs/runtime-dependencies.txt`, các file OFL trong frontend |

Các đoạn C/C++, bài toán payroll và factorial trong bài giảng là ví dụ minh họa.
Quy ước đặt tên được chuyển sang quy ước Java; không thêm nghiệp vụ ngoài tài liệu
nhà hàng. Bài giảng không yêu cầu đổi ngôn ngữ hoặc công khai repository.

## Ranh giới kiến trúc

- Controller: HTTP, model, form và chuyển hướng sau POST. SQL không nằm trong controller.
- Service: quyền actor, validation nghiệp vụ, trạng thái và transaction.
- Repository: JPA hoặc JDBC có tham số; SQL menu/thanh toán đã chuyển khỏi service.
- Entity: dữ liệu bền vững dùng chung. DTO/read model: dữ liệu phục vụ từng màn hình.
  Các Map ở module menu/checkout là read model tương thích template hiện có;
  dữ liệu ghi menu dùng `MenuItemValues`, không bổ sung schema riêng cho module.
- View: Thymeleaf; CSS/JavaScript nằm trong frontend, Maven đưa vào JAR.
- Database: schema 9 bảng; seed demo chạy lại không ghi đè lịch sử đã lưu.

TV3 bổ sung UC06/UC07 trên chính Orders/Order_Detail. Chef chỉ chuyển
Pending → Preparing → Ready; Waiter chỉ chuyển Ready → Served. Các thao tác
bếp, sửa/hủy đơn và thanh toán dùng cùng khóa Orders để tránh dữ liệu trộn
khi hai actor thao tác đồng thời.

## Quy ước duy trì

Java được kiểm tra tự động ở pha Maven `validate`. Sau khi sửa Java:

```powershell
cd backend
.\mvnw.cmd --no-transfer-progress formatter:format
.\mvnw.cmd --no-transfer-progress verify
```

Formatter dựa trên Eclipse, đặt lineSplit và comment length bằng 80. Toàn bộ
câu lệnh Java hiện tại không vượt 80 ký tự; khai báo package/import dài là
ngoại lệ vì không thể tách identifier. Các chuỗi SQL dài đã xuống dòng mà
không đổi nội dung truy vấn. File thư viện minified, URL/thuộc tính Thymeleaf
và chuỗi nội dung không được cắt tùy tiện chỉ để đạt giới hạn này.

Frontend dùng Prettier 3.6.2 với printWidth 80; không định dạng lại vendor.
Thymeleaf có `th:block` cần công cụ hiểu namespace hoặc đổi tạm tên thẻ trong
bản định dạng rồi khôi phục; không commit tên thẻ thay thế.

Mật khẩu demo bốn actor vẫn là `123456`. Tài khoản mới/sửa mật khẩu yêu cầu
ít nhất 6 ký tự và tối đa 72 byte UTF-8 trước khi mã hóa BCrypt. Mật khẩu rỗng
khi sửa tài khoản giữ nguyên hash cũ.

Kết quả kiểm chứng cụ thể và giới hạn kiểm tra giao diện nằm trong
[VERIFICATION.md](VERIFICATION.md). Cấu hình formatter tham khảo
[tài liệu chính thức](https://code.revelc.net/formatter-maven-plugin/usage.html).
