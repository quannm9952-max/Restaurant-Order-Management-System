# Backend thành viên 5

Ứng dụng Spring Boot, Java 21. Điểm khởi động là `src/main/java/com/restaurant/ordermanagement/RestaurantOrderManagementApplication.java`.

| Package trong `reporting` | Trách nhiệm |
| --- | --- |
| `controller` | Route UC10–UC12, nhận bộ lọc, chọn view và xử lý lỗi |
| `service` | Đọc dữ liệu, kiểm tra quyền Manager, tổng hợp doanh thu |
| `repository` | SQL đọc Orders/Order_Detail/Payment và các bảng chung |
| `dto` | Kiểu dữ liệu trả cho giao diện, trạng thái, phân trang |
| `config` | Phân quyền, profile demo, đồng hồ và định dạng tiền |

`src/main/resources` giữ cấu hình ứng dụng; `src/test` giữ JUnit/MockMvc và cấu hình H2. Frontend nằm ở `../frontend`, database scripts ở `../database`.

Chạy test từ thư mục này:

```powershell
.\mvnw.cmd --no-transfer-progress verify
```

Hoặc dùng `verify-tv5.cmd` ở thư mục repository. Maven đóng gói các file HTML/CSS/JS từ frontend vào cùng JAR; các đường dẫn `/css`, `/js`, `/images`, `/fonts` và các route `/manager/**` được giữ nguyên.
