# Trạng thái bàn giao ngày 17/11/2026

- Đã tích hợp đủ TV1–TV5 và UC01–UC12; TV3 có bếp, phục vụ, thông báo Ready.
- Đã đọc toàn bộ 47 trang hướng dẫn Ch4 và sửa kiến trúc/truy vấn/xử lý lỗi,
  định dạng mã, kiểm thử và tài liệu phụ thuộc theo bảng IMPLEMENTATION-REVIEW.md.
- 208 kiểm thử Maven đạt; 99 mục HTTP/MySQL đạt; kiểm tra restart giữ nguyên
  dữ liệu của 9 bảng ở cả database chính và database thử riêng.
- Ứng dụng chính chạy http://localhost:8080, DB `rom_integration_demo` tại
  MySQL local 3307. Bốn username: demo_manager, demo_waiter, demo_cashier,
  demo_chef; mật khẩu 123456.
- Code làm việc nằm ở worktree `rom-system-integration`, chia backend,
  frontend, database và docs. Repository nhóm có bản tích hợp trong main;
  nhánh feature/tv3-integration giữ lịch sử phần hoàn thiện này.
- Chưa xác nhận độ khớp toàn bộ màn hình mới bằng kiểm tra trực quan trình
  duyệt; xem giới hạn trong VERIFICATION.md và nguồn PNG trong FIGMA-AUDIT.md.

Để chạy lại: mở PowerShell ở gốc worktree, cấu hình DB_URL/DB_USERNAME/
DB_PASSWORD cho MySQL đang sử dụng rồi chạy `run.cmd`. Không nạp `seed.sql`
để reset database đang dùng. Để kiểm tra: `verify.cmd`.
