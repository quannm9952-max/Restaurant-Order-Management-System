# Database phục vụ thành viên 5

File `RestaurantOrderManagement_Database.sql` và `README.md` đã có trong repository của nhóm được giữ nguyên.

| File bổ sung | Mục đích |
| --- | --- |
| `schema.sql` | Cùng chín bảng theo schema nguồn, tách phần tạo/chọn database |
| `seed.sql` | Dữ liệu TEST/DEMO có thể nạp lại cho UC10–UC12 |
| `reconciliation.sql` | Truy vấn kiểm tra doanh thu, payment bị loại và tổng dòng món |

Chọn một database dành riêng cho demo trước khi chạy `schema.sql`, rồi chạy `seed.sql`. Profile `tv5-demo` tự thực hiện hai bước này cho `restaurant_order_management_tv5_demo` khi ứng dụng được khởi động qua `run-tv5.cmd`.

Ngày 29/10–04/11/2026 có 24 giao dịch Successful, tổng doanh thu 1,892.00 USD; 2 Pending và 2 Failed bị loại. Doanh thu theo tuần tương ứng là 1,040.00 và 852.00 USD.

Không có bảng Revenue riêng. Dữ liệu báo cáo được tổng hợp trực tiếp từ Payment và Orders. Trạng thái và cột giữ theo schema của nhóm; đây là dữ liệu giả lập, không có dữ liệu thanh toán nhạy cảm.
