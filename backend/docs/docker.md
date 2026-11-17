# Chạy dự án bằng Docker Compose

## Phạm vi thay đổi

Chỉ đổi môi trường đóng gói/chạy. Java 21, Spring Boot 4.1.2, HTML/CSS/JavaScript,
Thymeleaf, Bootstrap, MySQL, JUnit và kiến trúc MVC giữ nguyên. Không thêm
ngôn ngữ, framework, web server hoặc dịch vụ ứng dụng mới.

`compose.yaml` có hai service:

- `db`: MySQL Community 8.4.13; dữ liệu lưu trong named volume mysql-data.
- `app`: build từ backend/Dockerfile; Maven Wrapper cũ tạo cùng một JAR.

Dockerfile có stage verify để chạy kiểm thử, stage build tạo JAR và stage
runtime dùng JRE 21. Runtime chạy UID 10001, không dùng root. Frontend được
Maven đóng gói theo POM hiện có; vị trí SQL tương thích profile demo hiện có.
Không sửa Java, POM, template hoặc schema chỉ để chạy Docker.

## Cài đặt và chạy

Máy cần Docker Desktop với Linux containers; Windows dùng WSL2. Hướng dẫn
chính thức: [Docker Desktop Windows](https://docs.docker.com/desktop/setup/install/windows-install/).
Lần build đầu cần Internet để tải image và các dependency Maven đã có trong POM.

Tại gốc dự án:

```powershell
Copy-Item .env.example .env
docker compose config --quiet
docker compose up --build -d
docker compose logs -f app
```

Chỉ copy `.env.example` lần đầu; giữ `.env` đã cấu hình ở những lần sau.
`run.cmd` làm tự động việc này. App đợi MySQL healthcheck thành công rồi mới
khởi động. Cổng bên trong luôn là app 8080 và db 3306; cổng trên máy thay đổi
bằng APP_PORT/MYSQL_PORT. Mặc định app ở localhost:8080, DB ở localhost:3308.

Dừng bằng `docker compose down` giữ volume. Không xóa volume khi cần giữ dữ
liệu. Thay đổi biến MYSQL_* chỉ tạo tài khoản/database ở lần khởi tạo volume;
không phải lệnh đổi mật khẩu cho database đang tồn tại.

## Tương thích MySQL trên Windows và Linux

Schema/query hiện có dùng một số tên bảng viết hoa/thường. MySQL trên Windows
thường không phân biệt hoa/thường; Linux mặc định có phân biệt. Container db
được cấu hình `lower_case_table_names=1` ngay từ lần tạo volume đầu tiên để
chạy đúng mã/schema hiện tại, không đổi bảng hoặc câu truy vấn nghiệp vụ.
Tham số này không được thay đổi cho một volume đã khởi tạo bằng giá trị khác.

Nguồn: [MySQL case sensitivity](https://dev.mysql.com/doc/refman/8.4/en/identifier-case-sensitivity.html),
[Compose startup order](https://docs.docker.com/compose/how-tos/startup-order/).

## Chuyển dữ liệu từ MySQL đang dùng

Docker tạo volume riêng; không tự lấy dữ liệu XAMPP/MySQL cũ. Giữ database cũ
cho đến khi đã đối soát bản nhập vào Docker. Chỉ thực hiện nhập trên database
Docker mới, trước khi cho người dùng tạo giao dịch tại đó.

1. Xuất đúng database hiện tại bằng mysqldump/MySQL client hoặc DBeaver.
   Không xuất kèm CREATE DATABASE/USE của database khác.
2. Chạy riêng DB Docker bằng `docker compose up -d db` và chờ Healthy.
3. Chép bản dump vào container, nhập vào MYSQL_DATABASE đã cấu hình.
4. Khởi động app bằng `docker compose up --build -d app` rồi đối chiếu số bản
   ghi, tài khoản, đơn, thanh toán và hóa đơn với database cũ.

Ví dụ với file `database/backups/previous.sql` đã xuất từ bước 1:

```powershell
docker compose cp database/backups/previous.sql db:/tmp/previous.sql
docker compose exec db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --user="$MYSQL_USER" "$MYSQL_DATABASE" < /tmp/previous.sql'
docker compose up --build -d app
```

`database/backups` bị bỏ qua trong Git và Docker build context. Không commit
bản dump vì có thể chứa dữ liệu và hash mật khẩu của người dùng.

## Kiểm tra

```powershell
.\verify.cmd
```

GitHub Actions còn kiểm tra Compose trên Linux, build/chạy app + DB, đăng nhập
bốn actor và kiểm tra 9 bảng. Kết quả cụ thể nằm trong testing.md. Trên máy
chưa có Docker/WSL2, cấu hình này chưa thể khởi động container local.
