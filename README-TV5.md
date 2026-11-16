# Phần lập trình thành viên 5

Nhánh bàn giao: `feature/reports-history`. Phạm vi: UC10 tra cứu đơn, UC11 báo cáo doanh thu và UC12 lịch sử thanh toán dành cho Manager. Các phần của TV1–TV4 được ghép sau khi nhóm bàn giao đầy đủ.

## Cấu trúc code

```text
Restaurant-Order-Management-System/
├── frontend/                 Giao diện Thymeleaf, CSS, JavaScript, SVG, font
│   ├── templates/            Các trang và fragment dùng chung
│   └── static/               CSS, JavaScript và tài nguyên giao diện
├── backend/                  Ứng dụng Spring Boot và Maven
│   ├── src/main/java/        Controller, Service, Repository, DTO, config
│   ├── src/main/resources/   Cấu hình ứng dụng và profile demo
│   ├── src/test/             JUnit, MockMvc và cấu hình H2 kiểm thử
│   ├── .mvn/                 Cấu hình Maven Wrapper
│   ├── mvnw, mvnw.cmd        Maven Wrapper
│   └── pom.xml               Dependency và cấu hình đóng gói frontend
├── database/                 Schema, seed DEMO và truy vấn đối soát
├── docs/member5/             Bàn giao, test case và checklist
├── run-tv5.cmd               Build và chạy phần TV5 trên Windows
└── verify-tv5.cmd            Chạy test và đóng gói
```

Frontend dùng Thymeleaf và được Spring Boot render. Maven lấy `frontend/templates` và `frontend/static` để đóng gói vào classpath của backend. Vì vậy vẫn chạy bằng một ứng dụng Spring Boot sau khi tách thư mục.

Xem [bản phân loại từng file](docs/member5/FILE-MAP.md), [bàn giao module](docs/member5/README.md) và [test case](docs/member5/TEST-CASES.md).

## Chạy demo trên Windows

Mở terminal tại thư mục có file này. Cần Java 21 và MySQL Community 8.0+:

```powershell
$env:TV5_DEMO_PASSWORD = 'DEMO_TV5_2026'
.\run-tv5.cmd --spring.profiles.active=tv5-demo
```

Nếu MySQL có mật khẩu, đặt `DB_PASSWORD` trong cùng terminal trước khi chạy. Profile demo sử dụng database riêng `restaurant_order_management_tv5_demo`; các script nằm trong `database/`. Tài liệu [database TV5](database/README-TV5.md) mô tả cách nạp và đối soát.

Vào <http://localhost:8080/login>, tài khoản `demo_manager`, mật khẩu là giá trị `TV5_DEMO_PASSWORD` vừa đặt. Dùng `--server.port=8082` nếu cần chọn cổng khác.

`run-tv5.cmd` tự chuyển vào `backend`, build và chạy JAR bằng đường dẫn tương đối. Khi chạy trực tiếp bằng Maven hoặc Java, dùng `backend` làm thư mục làm việc để profile demo tìm được `../database`.

## Kiểm tra

```powershell
.\verify-tv5.cmd
node --check frontend/static/js/reporting.js
```

Test dùng H2 riêng, không cần MySQL đang chạy. Báo cáo JUnit nằm trong `backend/target/surefire-reports`. Để build sạch thủ công, chuyển vào `backend` và chạy `mvnw.cmd --no-transfer-progress clean verify`.

## Chuẩn bị đưa lên Git

Code đã được phân loại trực tiếp trong bản clone, trên nhánh `feature/reports-history`. Các file đã có của nhóm được giữ nguyên. Các file cấu hình build, test và giấy phép của tài nguyên giao diện được giữ lại vì cần để build, kiểm tra và phân phối phần TV5.

Các thư mục build, log, cấu hình IDE và `.env` được loại khỏi danh sách Git bằng `.gitignore`. Hook tự động trong `.github/modernize`, `HELP.md`, bản clone lồng nhau và biểu tượng không có tham chiếu không được chép từ thư mục làm việc cũ.

Kiểm tra `git status` và `git diff` trước khi tạo commit. Lần sắp xếp này chỉ chuẩn bị file cục bộ; chưa commit, push hoặc merge.
