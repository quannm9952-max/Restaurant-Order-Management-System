# Thư viện và thông báo thành phần bên thứ ba

Kiểm kê ngày 17/11/2026. Phiên bản runtime đọc từ `BOOT-INF/lib` của JAR
được Maven đóng gói; danh sách đầy đủ 81 JAR nằm trong
[runtime-dependencies.txt](runtime-dependencies.txt). Spring Boot BOM quản lý
phiên bản phụ thuộc; không nâng phiên bản thư viện trong đợt sửa này.

| Thành phần | Phiên bản kiểm tra | Vai trò | Thông báo giấy phép/nguồn |
| --- | --- | --- | --- |
| Spring Boot | 4.1.2 | Cấu hình, MVC, đóng gói | Apache-2.0 theo POM |
| Spring Security | 7.1.2 | Đăng nhập, CSRF, phân quyền | Apache-2.0 theo POM |
| Thymeleaf và extras Spring Security | 3.1.5.RELEASE | Render HTML theo actor | Apache-2.0, LICENSE/NOTICE trong JAR |
| Hibernate ORM | 7.4.13.Final | JPA entity/repository | Apache-2.0 theo POM của đúng phiên bản |
| MySQL Connector/J | 9.7.0 | JDBC runtime | GNU GPL v2 with Universal FOSS Exception v1.0, theo POM |
| H2 | 2.4.240, chỉ test | Cơ sở dữ liệu kiểm thử độc lập | MPL-2.0 hoặc EPL-1.0; [nguồn H2](https://www.h2database.com/html/license.html) |
| JUnit Jupiter | 6.0.3, chỉ test | Kiểm thử tự động | EPL-2.0 theo POM |
| Lombok | BOM của Spring Boot, chỉ biên dịch | Sinh getter/setter cho entity | MIT, [nguồn Lombok](https://projectlombok.org/LICENSE) |
| Bootstrap | 5.3.8 | CSS/JS local | MIT; giữ header gốc trong file vendor |
| Inter | TTF local | Font giao diện | SIL OFL 1.1; `frontend/static/fonts/Inter-OFL.txt` |
| Manrope | TTF local | Font tiêu đề | SIL OFL 1.1; `frontend/static/fonts/Manrope-OFL.txt` |
| formatter-maven-plugin | 2.29.0, công cụ build | Định dạng và kiểm tra Java | [Thông tin plugin](https://code.revelc.net/formatter-maven-plugin/) |
| Prettier | 3.6.2, công cụ phát triển tùy chọn | Định dạng HTML/CSS/JS | Không phải runtime của ứng dụng |

Bảng ghi nhận metadata của thành phần; các LICENSE/NOTICE kèm theo từng
thư viện là nguồn điều khoản đầy đủ. Giữ nguyên file vendor và các font
license đang có. Asset giao diện từ thiết kế nhóm được ghi nguồn trong
FIGMA-AUDIT.md. Đợt sửa này không bổ sung giấy phép cho mã riêng của nhóm.

JDK 21 và MySQL Community 8.4.13 là môi trường đã dùng để kiểm chứng local,
không được chép vào repository. DBeaver Community là công cụ xem dữ liệu tùy
chọn. H2, JUnit, formatter và Prettier không cần cài trên máy chỉ chạy JAR.
