> Historical member-5 handoff before integration. Use [the current README](../../README.md) and [integration notes](../INTEGRATION.md) for startup, account and architecture details.

# Phân loại file thành viên 5

Bản chuẩn bị nằm trong `restaurant-order-management-git`, trên nhánh `feature/reports-history`. Cấu trúc chính gồm frontend, backend và database. Các file dưới đây là đường dẫn tính từ thư mục repository.

## Frontend

- `frontend/README.md`
- `frontend/static/css/fonts.css`
- `frontend/static/css/reporting.css`
- `frontend/static/css/vendor/bootstrap-5.3.8.min.css`
- `frontend/static/fonts/inter-bold.ttf`
- `frontend/static/fonts/Inter-OFL.txt`
- `frontend/static/fonts/inter-regular.ttf`
- `frontend/static/fonts/inter-semibold.ttf`
- `frontend/static/fonts/manrope-bold.ttf`
- `frontend/static/fonts/Manrope-OFL.txt`
- `frontend/static/images/tv5/calendar.svg`
- `frontend/static/images/tv5/cash.svg`
- `frontend/static/images/tv5/chart.svg`
- `frontend/static/images/tv5/chart1.svg`
- `frontend/static/images/tv5/chart-inactive.svg`
- `frontend/static/images/tv5/check.svg`
- `frontend/static/images/tv5/checkcircle.svg`
- `frontend/static/images/tv5/chevron.svg`
- `frontend/static/images/tv5/clock.svg`
- `frontend/static/images/tv5/close.svg`
- `frontend/static/images/tv5/grid.svg`
- `frontend/static/images/tv5/history.svg`
- `frontend/static/images/tv5/history-active.svg`
- `frontend/static/images/tv5/info.svg`
- `frontend/static/images/tv5/lock.svg`
- `frontend/static/images/tv5/lock1.svg`
- `frontend/static/images/tv5/logout.svg`
- `frontend/static/images/tv5/menu.svg`
- `frontend/static/images/tv5/orders.svg`
- `frontend/static/images/tv5/search.svg`
- `frontend/static/images/tv5/table.svg`
- `frontend/static/images/tv5/users.svg`
- `frontend/static/images/tv5/utensils.svg`
- `frontend/static/js/reporting.js`
- `frontend/templates/payment-history/list.html`
- `frontend/templates/reporting/error.html`
- `frontend/templates/reporting/fragments.html`
- `frontend/templates/reports/order-details.html`
- `frontend/templates/reports/orders.html`
- `frontend/templates/reports/revenue.html`

Có 40 file, gồm sáu template, ba CSS, một JavaScript, 23 SVG, bốn font, hai giấy phép font và hướng dẫn frontend. Các file HTML/CSS/JS và tài nguyên chỉ có một bản nguồn trong `frontend`; Maven đóng gói chúng qua cấu hình resource của backend.

## Backend

- `backend/.mvn/wrapper/maven-wrapper.properties`
- `backend/mvnw`
- `backend/mvnw.cmd`
- `backend/pom.xml`
- `backend/README.md`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/config/ReportingConfiguration.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/config/ReportingDemoSecurityConfiguration.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/config/ReportingSecurityConfiguration.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/controller/OrderMonitoringController.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/controller/PaymentHistoryController.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/controller/ReportController.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/controller/ReportingFilters.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/controller/ReportingViewAdvice.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/dto/ReportingModels.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/repository/OrderRepository.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/repository/PaymentRepository.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/repository/QuerySupport.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/service/MonitoringService.java`
- `backend/src/main/java/com/restaurant/ordermanagement/reporting/service/ReportingService.java`
- `backend/src/main/java/com/restaurant/ordermanagement/RestaurantOrderManagementApplication.java`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-tv5-demo.properties`
- `backend/src/test/java/com/restaurant/ordermanagement/reporting/ReportingDemoSecurityTests.java`
- `backend/src/test/java/com/restaurant/ordermanagement/reporting/ReportingIntegrationTests.java`
- `backend/src/test/java/com/restaurant/ordermanagement/RestaurantOrderManagementApplicationTests.java`
- `backend/src/test/resources/application-test.properties`

Có 26 file. Java nghiệp vụ nằm trong `src/main/java`; JUnit/MockMvc ở `src/test/java`; cấu hình H2 chỉ thuộc scope test. `pom.xml`, Maven Wrapper và các profile là file cần thiết để build/chạy backend.

## Database

- `database/README.md`
- `database/README-TV5.md`
- `database/reconciliation.sql`
- `database/RestaurantOrderManagement_Database.sql`
- `database/schema.sql`
- `database/seed.sql`

Ba script bổ sung phục vụ TV5 là `schema.sql`, `seed.sql`, `reconciliation.sql`. Hai file `database/README.md` và `database/RestaurantOrderManagement_Database.sql` đã có của nhóm được giữ nguyên.

## Tài liệu và file chạy chung

- `docs/member5/INTEGRATION-CHECKLIST.md`
- `docs/member5/README.md`
- `docs/member5/TEST-CASES.md`
- `docs/member5/FILE-MAP.md`
- `README-TV5.md`
- `run-tv5.cmd`
- `verify-tv5.cmd`
- `.gitignore`
- `.gitattributes`

Các tài liệu/test không được xếp vào frontend hoặc database chỉ để đủ ba thư mục; chúng được giữ theo đúng chức năng. File chạy chung chuyển vào `backend` trước khi build/chạy để script SQL nằm tại `../database`.

## File được lọc khỏi bản chuẩn bị Git

- `target`, JAR, `.class`, báo cáo build/test và log phát sinh.
- `.vscode`, cấu hình IDE, `.env` và cấu hình cá nhân.
- `.github/modernize` chứa hook công cụ, `HELP.md` và bản clone lồng nhau từ thư mục nguồn.
- `chevron1.svg`, biểu tượng không có tham chiếu trong HTML/CSS/JS.

Bản làm việc cũ giữ nguyên. Các file đã có trong repository của nhóm chưa bị sửa hoặc xóa; lần này chỉ thêm phần TV5 đã phân loại.

## Kiểm tra sau khi phân loại

- `verify-tv5.cmd`: 39 test, 0 lỗi, 0 thất bại; BUILD SUCCESS.
- Kiểm tra cú pháp `frontend/static/js/reporting.js` bằng `node --check`.
- Đối chiếu các template và tài nguyên trong JAR với thư mục frontend.
- Kiểm tra `.gitignore` loại backend/target, .env, IDE và log khỏi danh sách Git.

Chưa tạo commit, push hoặc merge.
