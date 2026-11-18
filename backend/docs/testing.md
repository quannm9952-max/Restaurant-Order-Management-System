## Thu ngân sửa số lượng và khóa bill (18/11/2026)

Docker verify bản cuối: **348 test, 0 failure, 0 error, 0 skipped**.
Node syntax, Prettier check và git diff --check đạt. Không thêm bảng/cột
hoặc migration; dùng Invoice làm mốc bill đã được tạo trong database.

JUnit kiểm tra tăng/giảm số lượng, giữ đơn giá snapshot, khóa sau tải lại,
chặn bỏ bước tạo bill, số lượng sai/tràn tổng, ID thiếu/trùng/khác đơn,
dòng Cancelled, quyền Cashier/CSRF và chặn mọi sửa đơn từ Waiter. Khi tạo
Invoice lỗi, số lượng/tổng đơn rollback; thanh toán lỗi rollback Payment,
Order/Table và giữ bill đã khóa. Bill chưa trả không vào doanh thu và không
mở trang hóa đơn Paid. Regression đặt món, bếp, phục vụ, báo cáo vẫn đạt.

MySQL bản sao được khôi phục từ backup và khớp toàn bộ 9 bảng. Chrome thực
hiện sửa từ 365.000 VND lên 645.000 VND, tạo bill và reload giữ khóa; tất cả
endpoint sửa đơn của Waiter bị chặn. Cash nhận 700.000 VND trả 55.000 VND;
Bank Transfer dùng tổng đã duyệt. Payment và Invoice khớp, chỉ một Invoice
được lưu và không đổi thời điểm tạo bill khi trả tiền. Chỉ số doanh thu tăng
sau Successful + Completed. Hai Cashier đồng thời tạo bill với số lượng
khác nhau chỉ có một người thành công; không ghi đè hoặc tạo Invoice trùng.
Khởi động lại app và đăng nhập phiên mới vẫn thấy bill khóa.

16 bố cục 1440/820/390/320 px cho bill đang sửa, Cash sau tạo bill, Bank
Transfer và hóa đơn đã trả; đã xem ảnh desktop/mobile, không lỗi JavaScript
hoặc tràn toàn trang. Mọi thao tác tạo bill/thanh toán thử chỉ trên bản sao.
Database chính giữ nguyên cả 9 bảng trong suốt kiểm thử.
Sau triển khai Docker chính: 12 bố cục draft/hóa đơn đã trả/báo cáo tại
4 kích thước đạt, bốn actor đăng nhập và mở đúng trang. Không gửi tạo bill
hoặc thanh toán trên bản chính; đối chiếu đủ 9 bảng trước/sau không đổi.
Trang login và JS/CSS công khai qua Cloudflare khớp mã nguồn; không gửi
request có đăng nhập ra Cloudflare.

## Chuyển toàn bộ số tiền cũ sang VND ×10.000 (18/11/2026)

Dữ liệu demo và fixture kiểm thử nhân riêng 5 cột tiền; giữ ID, số lượng,
ngày giờ, trạng thái và thông tin tài khoản. Ví dụ 8.50 cũ thành 85000.00,
hiện **85.000 VND**. Các khoảng lọc giá tương ứng cũng đổi sang VND.
Docker verify đạt **334 test, 0 failure, 0 error, 0 skipped**.

Trên MySQL bản sao, khôi phục backup khớp toàn bộ 9 bảng; chuyển đổi đúng
155 ô tiền và giữ nguyên mọi trường khác. SQL chạy lại không nhân lặp;
gây lỗi sau cả 5 UPDATE thì tất cả rollback; sửa lệch mốc gốc thì bị chặn.
Backup do PowerShell tạo cũng được khôi phục và đối chiếu thành công.

Chrome headless: 24 bố cục 1440/820/390/320 px cho menu, đơn, bill, hóa đơn,
báo cáo và lịch sử; không tràn toàn trang, cắt nhãn trục hay lỗi JavaScript.
Giá menu 85.000 VND và hóa đơn cũ 390.000 VND đúng dữ liệu chuyển đổi.
Cột tiền bill/hóa đơn giữ VND trên một dòng; bảng cuộn trong khung khi
màn hình hẹp. Chế độ in ẩn nút thao tác và hiển thị đủ các cột.
Thanh toán mới Cash/Bank Transfer thử riêng trên bản sao đều lưu 85000.00;
nhận 100.000 VND trả lại 15.000 VND. Không tạo thanh toán thử trên bản chính.

Bản chính sau khi chuyển và khởi động image mới: 155 ô tiền khớp chính xác
kết quả ×10.000 từ bản gốc; mọi trường còn lại của 9 bảng giữ nguyên.
Không có lệch tổng giữa chi tiết đơn, Orders, Successful Payment và Invoice.
Bốn actor đăng nhập/mở trang đúng quyền; 24 bố cục trên bản chính được kiểm
tra chỉ đọc, không gửi thanh toán. Seed khi khởi động không nhân lại số tiền.

Các mục bên dưới ghi lại kiểm tra ở các phiên bản trước lần chuyển số tiền.

## Hiển thị mã tiền VND thay ký hiệu (18/11/2026)

MoneyFormatter và hai bộ định dạng JavaScript dùng hậu tố chữ VND, ví dụ
25.000 VND và 8,5 VND. Cập nhật cả nhãn trục, tooltip, giỏ món, tiền thừa,
modal xác nhận, hóa đơn và placeholder báo cáo. Giá trị số giữ nguyên.
Docker verify đạt **333 test, 0 failure, 0 error, 0 skipped**; runtime build,
Node check và git diff --check đạt. Chrome headless kiểm tra **24 bố cục**
ở 1440/820/390/320 px: tiền server/client đều có mã VND, không còn ký hiệu
đồng; nhãn trục không bị cắt, không lỗi JavaScript hoặc tràn toàn trang.
Đã xem ảnh hóa đơn và biểu đồ mobile; không gửi thanh toán khi kiểm tra.
Checksum đủ 9 bảng trước/sau không đổi. Không sửa schema hoặc dữ liệu.

## Định dạng tiền VND toàn hệ thống (18/11/2026)

Theo lựa chọn giữ nguyên số tiền: đổi MoneyFormatter, giỏ món, tiền thừa,
nhãn giá/bill/hóa đơn/báo cáo và thông báo nhập giá sang VND. Dùng vi-VN,
dấu chấm phân nhóm, dấu phẩy phần lẻ và ký hiệu ₫. Giữ tối đa hai số lẻ
để không làm tròn mất các giá trị đã lưu; không dùng tỷ giá chuyển đổi.
Biểu đồ đo nhãn thực sau khi font tải và khi resize, chừa đủ chỗ cho trục
VND/số tiền trên cột, giữ cùng vùng vẽ giữa cột và đường lưới.

Docker verify: **333 test đạt, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**.
Kiểm tra 25.000 ₫, 8,5 ₫, 0,01 ₫ và giá trị tối đa DECIMAL; các nhãn trục
phần lẻ không trùng/mất giá trị. Regression thanh toán, phân quyền, menu,
phục vụ và báo cáo vẫn đạt. Node check orders.js/payments.js/reports.js,
Docker runtime build và git diff --check đạt.

MySQL riêng + Chrome headless: **24 bố cục** ở 1440/820/390/320 px trên Menu,
giỏ món, Payment, Invoice, Revenue Reports và Payment History. Giỏ 2 ×
125.000,5 = 250.001 ₫; nhận 300.000 ₫ thì tiền thừa 49.999 ₫; thanh toán
Cash lưu đúng 250001.00, Bank Transfer giữ 8.50 và hiện 8,5 ₫. Tổng báo cáo
250.009,5 ₫ khớp Cash + Bank Transfer. Nhãn trục số tiền lớn không bị cắt,
không tràn ngang toàn trang hoặc lỗi JavaScript; đã xem ảnh desktop/mobile.
Bảng rộng vẫn cuộn trong khung riêng. Đây là giả lập Chrome.

Bản chính: tất cả số tiền và các bản ghi khác giữ nguyên; chỉ đổi nhãn mô
tả demo khớp đúng chuỗi cũ bằng migration giới hạn 8 ID. Đối chiếu hash
Menu_item cùng checksum 8 bảng còn lại đạt; các mô tả tùy chỉnh không đổi.
Bốn actor mở trang đúng quyền với VND. Bốn asset public qua Cloudflare
khớp nguồn; kiểm tra dữ liệu có đăng nhập chỉ tại localhost. Đã dọn project
và hai volume thử riêng. Không thay schema hoặc công nghệ.

## Ô chọn phương thức thanh toán (18/11/2026)

Ô Payment method chiếm một hàng đầy đủ; Amount Received/Change Amount ở
hàng dưới. Đơn đủ điều kiện cho chọn phương thức trước Generate Bill;
sinh bill, xác nhận nhận tiền và kiểm tra tiền mặt vẫn là điều kiện xử lý.
Đơn đã thanh toán hoặc chưa đủ điều kiện tiếp tục khóa các thao tác.

Node check, Docker runtime build và git diff --check đạt. Chrome headless
kiểm tra 8 viewport: 1918×925, 1440×900, 1180×820, 820×1180, 768×1024,
844×390, 390×844 và 320×740. Có 24 kiểm tra độ rộng cho placeholder/Cash/
Bank Transfer: chữ vừa vùng nội dung, hàng chọn rộng đầy đủ và nhận click
ở trái/giữa/mũi tên. Chọn phương thức trước sinh bill, giữ lựa chọn sau
sinh bill, từ chối thiếu tiền/checkbox và mở/đóng xác nhận cả hai phương
thức đều đạt; không submit thanh toán. Đã xem ảnh desktop/điện thoại.
Không lỗi JavaScript hoặc tràn ngang toàn trang. Checksum đủ 9 bảng chính
không đổi; CSS/JS public qua Cloudflare khớp nguồn. Chỉ sửa frontend và
nhật ký kiểm tra, giữ Java/SQL/nghiệp vụ backend hiện tại.

## Thanh toán xác nhận thủ công và doanh thu theo phương thức (18/11/2026)

Docker verify: **332 test đạt, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**.
PaymentIntegrationTest bổ sung 47 ca. Cập nhật regression của System và
Preparation để dùng API có xác nhận đã nhận tiền; giao dịch mới chỉ CASH
hoặc BANK_TRANSFER. Kiểm tra phương thức trống/sai, thiếu xác nhận, thiếu
hoặc sai định dạng tiền mặt, giá lưu lúc đặt, món chưa Served, quyền/CSRF,
đơn đã có Successful, hóa đơn, dữ liệu cũ nguyên vẹn, Unknown, bộ lọc và
phân trang. Giữ quy tắc doanh thu Successful + Completed; có kiểm thử dòng
Successful lệch trạng thái đơn vẫn ở lịch sử nhưng bị loại khỏi doanh thu.

Project Docker/MySQL riêng kiểm tra trực tiếp hai luồng Cash/Bank Transfer,
6 tình huống đầu vào sai, hai HTTP request đồng thời với hai phương thức,
unique constraint MySQL và lỗi tạo Invoice sau khi Payment/Order/Table đã
được cập nhật: toàn bộ thay đổi rollback. Đối chiếu tiền/số giao dịch từng
nhóm với SQL, tổng nhóm bằng tổng doanh thu; 28 bản ghi thanh toán mẫu cũ
giữ nguyên checksum theo từng dòng, gồm phương thức và thời gian.

Chrome headless: **40 bố cục** ở 1440×900, 1180×820, 820×1180, 768×1024,
844×390, 1024×768, 390×844 và 320×740. Bao gồm Cash, Bank Transfer, Revenue
Reports, Payment History và chi tiết chỉ đọc. Thử chọn phương thức, nhập
thiếu/đủ tiền, tính tiền thừa, checkbox, modal xác nhận, submit thật, hóa
đơn/in ấn, hướng dẫn QR in sẵn, phân trang giữ phương thức và bộ lọc. Không
lỗi JavaScript hoặc tràn ngang toàn trang; bảng rộng cuộn trong khung.
Đã xem ảnh desktop/điện thoại, gồm thẻ doanh thu và bảng đối soát.

Bản chính Docker: doanh thu từng nhóm khớp MySQL tại localhost, giao diện
xác nhận/lịch sử/phân quyền đạt; đủ 9 bảng giữ nguyên checksum trước/sau.
Ba asset payments.js/app.css/reports.css công khai qua Cloudflare khớp nguồn.
Không truy cập payload tài chính có đăng nhập qua Cloudflare. Đã dọn project
và hai volume thử riêng. Không thêm bảng/cột/dependency hoặc đổi Docker.
Chi tiết file và phạm vi: [payment-update.md](payment-update.md).

## Xem lại các món/bàn đã phục vụ (18/11/2026)

Docker verify: **286 test đạt, 0 failure, 0 error, 0 skipped**.
PreparationIntegrationTest có 55 ca, thêm 10 ca lịch sử và mở rộng luồng
thanh toán Cash/Card/Bank Transfer, phân quyền HTTP/service cho cả 4 actor.
Kiểm tra xác nhận từng dòng, phục vụ một phần/toàn bộ, loại dòng Cancelled
khỏi tiến độ, bộ lọc không làm sai tổng tiến độ, giữ món theo tên lúc đặt,
đơn Completed và dùng lại bàn vẫn giữ lịch sử theo mã đơn. Ready chỉ đếm
đơn Open; revision của trang và API thống nhất và đổi khi tiến độ/trạng thái
đơn đổi. Lịch sử chỉ đọc, escape tên món/ghi chú, không thêm quyền sửa.

Chrome headless trên project MySQL riêng: **32 bố cục** ở 8 kích thước từ
320 đến 1440 px, gồm Ready, Served, lọc bàn/mã đơn và danh sách rỗng. Thao
tác nút xác nhận thật và liên kết xem lại, kiểm tra phục vụ một phần → đủ
món → thanh toán → tạo đơn mới trên cùng bàn. Không tràn ngang toàn trang,
không lỗi JavaScript, đã xem ảnh desktop/điện thoại. Đây là giả lập trình
duyệt; không phải thử trên thiết bị thật. Không thêm bảng/cột/dependency;
thời gian hiển thị là thời gian tạo đơn, không suy diễn thời gian phục vụ.

# Kiểm chứng hệ thống

Bản chính: Waiter mở Ready/Served/polling thành công, ba actor khác trả 403.
Checksum đủ 9 bảng trước/sau cập nhật không đổi; CSS public qua Cloudflare
khớp nguồn. Chỉ kiểm tra dữ liệu đã đăng nhập tại localhost. Project và hai
volume thử riêng đã được dọn, giữ database và ảnh của bản chính.

## Chiều cao cột và chế độ nhóm doanh thu (18/11/2026)

Sửa vùng vẽ cột bị trừ padding khác với đường lưới/trục Y. Cột, đường lưới
và nhãn trục nay dùng chung vùng vẽ; nhãn được căn giữa đúng vạch chia.
Hiển thị rõ Auto hay nhóm thủ công, thêm Use Auto giữ nguyên khoảng ngày.

Chrome headless đo **35 bố cục** ở 1440/1180/820/390/320 px: tỷ lệ chiều
cao từng cột khớp số tiền chia mức trục cao nhất, sai số dưới 0.15%; chân
cột, trục và vạch chia khớp trong 1.5 px. Bao gồm Day/Week/Month/Auto và
khoảng rỗng. Cả 5 lần đổi Day sang Auto cho khoảng 36 ngày đều trả Week và
giữ ngày lọc; không lỗi JavaScript hoặc tràn trang. Đã xem ảnh biểu đồ.
CSS public khớp nguồn; đủ 9 bảng giữ nguyên checksum. Không sửa số liệu
hoặc logic tính doanh thu; không đọc payload báo cáo qua Cloudflare.

## Chọn ngày trực tiếp trên lịch (18/11/2026)

Sửa lỗi nút ngày/tháng bị thay thế khi render khiến xử lý bấm ngoài lịch
đóng popup và khôi phục ngày cũ. Kiểm tra đường đi gốc của sự kiện click
để giữ các thao tác bên trong lịch, kể cả khi nút đã được vẽ lại.

Chrome headless: **56 tình huống đạt** trên Revenue Reports và Payment
History ở 1440/820/390/320 px. Bao gồm chọn ngày đầu/cuối, qua tháng,
chọn ngược hoặc cùng một ngày, Apply, Cancel, bấm ngoài, Escape, nhập tay
và sửa khoảng không hợp lệ bằng lịch. Giữ Group/keyword/status khi áp dụng.
Không lỗi JavaScript; Node check và Docker build đạt. JS public khớp nguồn,
checksum đủ 9 bảng không đổi; không đọc báo cáo tài chính qua Cloudflare.

## Doanh thu tháng và nhóm tự động (18/11/2026)

Docker verify: **276 test đạt, 0 failure, 0 error, 0 skipped**. Kiểm thử nhóm
tháng qua năm, tháng không trọn vẹn và cuối ngày; tổng các cột khớp tổng
doanh thu. Kiểm tra đủ các ngưỡng 7/8/31/32/90/91 ngày, lựa chọn thủ công,
31 số liệu vẫn đầy đủ khi giảm nhãn và nhãn trục tiền không bị làm tròn
trùng nhau, kể cả doanh thu 0.01 USD. Không thêm bảng, cột hoặc dependency.

Chrome headless trên MySQL thử riêng: **36 bố cục biểu đồ** ở 1440, 820,
390 và 320 px. Kiểm tra 1/7/8/31/32/91 ngày, tháng không trọn vẹn, chọn Day
thủ công và tháng không có dữ liệu; hover/focus/chạm/Escape, đổi Month vẫn
giữ ngày lọc. Đủ 31 cột được giữ khi chỉ hiện 7 nhãn; một cột không tạo cuộn
ngang thừa. Không lỗi JavaScript hoặc tràn toàn trang; đã xem ảnh biểu đồ.

Bản chính được đối chiếu tổng, số thanh toán và từng tháng với MySQL tại
localhost. Đủ 9 bảng giữ nguyên checksum trước/sau cập nhật. Trên Cloudflare
chỉ kiểm tra CSS/JS public khớp nguồn; không tải payload báo cáo tài chính.
Dữ liệu 31 thanh toán thử nằm trong project Docker riêng và được dọn sau kiểm tra.

## Giao diện điện thoại và iPad (18/11/2026)

Docker `verify`: **263 test đạt, 0 failure, 0 error, 0 skipped**. Chỉ thay
frontend và tài liệu; Java, Spring Boot, MySQL, schema, nghiệp vụ giữ nguyên.
`node --check` cho app.js/reports.js và `git diff --check` đạt.

Chrome headless kiểm tra **189 bố cục** của 21 trang thuộc cả 4 actor ở
9 kích thước: 320×740, 390×844, 768×1024, 820×1180, 844×390, 1024×768,
1180×820, 1366×1024 và 1440×900. Không tràn ngang toàn trang và không lỗi
JavaScript. Có bảng cuộn riêng; không coi việc bảng cuộn là lỗi tràn trang.

Kiểm tra menu mở/đóng, nền che, xử lý Tab/Shift+Tab/Escape và khôi phục focus;
4 actor đều chọn được liên kết menu và chuyển qua lại mobile/desktop khi
menu đang mở. Form thêm/sửa nhân viên, món, chi tiết thanh toán, danh mục
thu gọn và lịch được kiểm tra giới hạn viewport. Nút form giữ trong vùng
nhìn; lịch mở ở đầu nội dung. Đã xem ảnh chụp điện thoại/iPad để kiểm tra bố cục.

Cloudflare đăng nhập đủ 4 actor và mở trang đúng quyền. CSS/JS/icon public
khớp byte với nguồn bản cuối. Checksum đủ 9 bảng chính trước/sau cập nhật
không đổi. Đây là kiểm tra bằng giả lập trình duyệt; chưa kiểm tra trên
thiết bị iPhone/iPad thật hoặc Safari iOS.

## Menu Management: xóa danh mục, xóa món và tải ảnh

Docker build stage `verify`: **263 test đạt, 0 failure, 0 error, 0 skipped;
BUILD SUCCESS**. Bổ sung 17 ca MenuManagementIntegrationTest và 2 ca HTTP
multipart thật; không thêm dependency hoặc thay đổi schema.

Kiểm tra danh mục rỗng được xóa/khôi phục cùng tên, danh mục có món (kể cả
ngừng bán) bị chặn xóa, không thêm món vào danh mục đã xóa. Món xóa khỏi
thực đơn và danh sách đặt món mới nhưng giữ các dòng Order_Detail cũ.

Ảnh kiểm tra định dạng thực JPG/PNG, kích thước byte/pixel, từ chối SVG/GIF,
file giả, rỗng hoặc quá lớn; không dùng filename người gửi làm đường dẫn.
Thêm/sửa ảnh, giữ ảnh khi không tải lại và rollback database/ảnh đều có test.
HTTP thật xác nhận multipart đọc CSRF trong body, thiếu CSRF trả 403,
file quá 5 MB hiện lỗi và không tạo món. Waiter không được sửa/xóa menu.
Cập nhật 18/11/2026: ca xóa món xác nhận redirect giữ danh mục, tìm kiếm,
trạng thái, khoảng giá và sắp xếp; nếu món đã bị xóa trước đó, thông báo lỗi
vẫn giữ danh mục. Docker/Cloudflare đã kiểm tra form và chuyển hướng giữ đủ
bộ lọc; checksum 9 bảng chính trước/sau cập nhật không đổi.

Lưu chỉnh sửa và đổi trạng thái món dùng chung cơ chế giữ bộ lọc với xóa món.
Danh mục trong form sửa món được tách khỏi danh mục của danh sách: ca chỉnh
sửa chuyển món sang danh mục khác vẫn redirect về danh mục đang xem ban đầu,
giữ từ khóa/trạng thái/khoảng giá/sắp xếp và ảnh cũ. Ca ảnh không hợp lệ giữ
bộ lọc và mở lại form có nội dung nháp. Docker verify vẫn đạt **263/263**.
Localhost và Cloudflare đã kiểm tra form/chuyển hướng của cập nhật, đổi
trạng thái và xóa; checksum 9 bảng chính trước/sau cập nhật không đổi.

Chrome headless trên Docker/MySQL riêng đã thao tác thêm/sửa/chuyển/xóa món,
xóa danh mục rỗng, chặn danh mục còn món, xem trước và thay ảnh. Tạo lại
container app vẫn giữ ảnh và metadata. Xóa món mẫu đã được đặt trước đó giữ
nguyên 27 dòng lịch sử; checksum Orders, Order_Detail, Payment, Invoice,
Employee và Account không đổi. Cơ sở dữ liệu thử tách riêng bản đang sử dụng.
`node --check` và `git diff --check` đạt.

Bản đang dùng trên Docker và Cloudflare đã đăng nhập Manager, render các
nút xóa/trường tải ảnh, tải CSS/JS/icon thành công; ảnh không tồn tại trả 404.
Waiter/Chef/Cashier truy cập Menu Management trả 403. Checksum đủ 9 bảng
database chính trước/sau cập nhật giống nhau; không tạo dữ liệu thử ở bản chính.

## Kết quả

- Maven `verify`: **208 test, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**.
- Cú pháp 5 JavaScript của dự án: `node --check` đạt.
- MySQL thật: **99 mục đạt**, gồm 27 trang của 4 actor và 38 asset local.
- JAR không còn package hoặc đường dẫn asset cũ sau khi chuyển thư mục.
- `git diff --check` đạt; không đưa log, JAR, file tạm hoặc DB thử vào Git.

| Bộ kiểm thử | Số ca đạt |
| --- | ---: |
| ApplicationTest | 1 |
| CurrentEmployeeServiceTest | 4 |
| HttpSecurityTest | 2 |
| OrderServiceTest | 40 |
| PreparationIntegrationTest | 45 |
| ReportingIntegrationTest | 32 |
| SystemIntegrationTest | 64 |
| TableServiceTest | 20 |

## Nghiệp vụ được giữ nguyên

Bốn actor đăng nhập đúng quyền; CSRF, khóa tài khoản, xác nhận và mã hóa mật
khẩu; tạo/sửa bàn; tạo đơn, giảm/hủy món Pending, hủy đơn hợp lệ và giữ giá
lúc đặt. Chef chỉ chuyển Pending → Preparing → Ready, Waiter chỉ xác nhận
Ready → Served. Cashier chỉ thanh toán khi các dòng còn hiệu lực đã Served.

Có kiểm thử rollback và đồng thời: hai Chef, chế biến so với hủy đơn,
phục vụ so với thanh toán, hai Cashier. Chỉ một Payment SUCCESSFUL và một
Invoice; đơn Completed và bàn Available cùng transaction. Báo cáo đọc đúng
Payment/Orders; lỗi đầu vào/không tồn tại/sai method trả đúng mã HTTP.

Các kiểm thử tích hợp render template nên bắt được đường dẫn Java enum cũ
trong Thymeleaf. Bản hiện tại lấy roles/statuses từ controller và dùng chung
enum trạng thái giữa nghiệp vụ với báo cáo.

## HTTP/MySQL

Database riêng `rom_cleanup_verification` trên MySQL Community 8.4.13, cổng
3307, ứng dụng 8081. Đơn thử **1101**, hai dòng **9206/9207**, tổng **138.50
USD** đi qua toàn bộ đặt món → bếp → phục vụ → Bank Transfer → hóa đơn/báo cáo.
Hai Chef cùng Start chỉ có một lần hợp lệ; hai Cashier cùng xác nhận chỉ tạo
một thanh toán thành công. Các trang và ảnh/font/CSS/JS mới trả HTTP 200.

Bản chính chạy **http://localhost:8080** trên DB `rom_integration_demo`.
Đối chiếu fingerprint toàn bộ dữ liệu của **9 bảng trước và sau khởi động**
đều khớp; bốn actor đăng nhập và mở màn hình nghiệp vụ thành công bằng
mật khẩu demo `123456`. Không reset database chính.

## Thay đổi cấu trúc

251 → 186 file nguồn/cấu hình/tài liệu được quản lý; 80 → 75 file Java chính;
5,095 → 4,368 dòng Java chính. Ảnh/icon: 74 → 34 file.

Bỏ 3 interface service chỉ có một implementation; gộp controller/service
quản lý bàn; bỏ accessor ID trùng; dùng chung enum; rút gọn form bằng Lombok
đã có. Script demo gộp thành demo-data.sql, test-data.sql dành riêng kiểm thử.
Tài liệu gốc, sơ đồ, bản code trước dọn và log cũ đã có bản sao lưu local bên
ngoài thư mục dự án.

Đây là kiểm chứng chức năng và cấu trúc; không phải kiểm thử tải hoặc xác
nhận độ khớp Figma 100% bằng quan sát trình duyệt. GitHub Actions được cấu
hình chạy verify; kết quả trên là kết quả chạy local.

## Bổ sung môi trường Docker Compose

Đợt này chỉ thay file đóng gói/chạy và hướng dẫn. Git diff xác nhận POM,
backend/src, frontend và SQL nghiệp vụ không đổi. Maven test chạy lại:
**208 test đạt, 0 failure, 0 error**. Compose YAML hợp lệ với JSON Schema
chính thức của Compose Specification; có đúng app Spring Boot và db MySQL.

Job `docker` trong GitHub Actions kiểm tra `compose config`, build/chạy hai
container trên Linux, đăng nhập bốn actor, kiểm tra 9 bảng và giá món đã sửa
được giữ qua down/up bằng cùng volume. Xem kết quả tại
[GitHub Actions](https://github.com/quannm9952-max/Restaurant-Order-Management-System/actions).

Máy làm việc chưa có Docker Desktop/WSL2; theo lựa chọn của người dùng, agent
không cài hệ thống và chưa chạy Docker local. Hướng dẫn ở docker.md để chạy
sau khi người dùng tự cài. MySQL và dữ liệu local cũ không bị sửa/xóa.

## Cập nhật đăng nhập username và lời chào (17/11/2026)

Maven `verify`: **217 test đạt, 0 failure, 0 error, 0 skipped**;
SystemIntegrationTest có 73 ca. Bao gồm đăng nhập 8 username mẫu, từ chối
họ tên/mật khẩu sai, tài khoản mới tạo qua Manager và lời chào đầy đủ họ tên
tiếng Việt. Khóa tài khoản và mật khẩu đã sửa được giữ khi chạy lại khởi tạo demo.

Docker/MySQL thật: cả 8 tài khoản trong Employee Management đăng nhập bằng
đúng cột Username, mật khẩu `123456`, vào được màn hình đúng quyền. Trang chủ
và tên ở header khớp tên nhân viên. Chrome headless kiểm tra giao diện 4 actor,
tài khoản mẫu cũ, tìm kiếm username và bộ lọc role của Manager.

Đã sao lưu DB trước cập nhật. Checksum xác nhận chỉ Account 9001–9004 được
kích hoạt và đặt mật khẩu ban đầu; 4 tài khoản demo đang hoạt động và 8 bảng
khác giữ nguyên. Khởi động lại app giữ nguyên fingerprint của cả 9 bảng và
8 tài khoản. Đăng nhập qua HTTPS Cloudflare Tunnel cũng đạt.

## Giới hạn ký tự username (17/11/2026)

Maven `verify`: **232 test đạt, 0 failure, 0 error, 0 skipped**;
SystemIntegrationTest có 88 ca. Tạo/sửa và đăng nhập từ chối chữ có dấu,
dấu kết hợp Unicode, khoảng trắng, emoji, dấu chấm/gạch ngang/@/slash.
Username 1–50 ký tự ASCII gồm chữ cái, số và gạch dưới được chấp nhận;
họ tên tiếng Việt và quy tắc mật khẩu giữ nguyên.

MySQL/Docker thật: cả 8 tài khoản đăng nhập thành công, biến thể có dấu
`demo_mánager` và khoảng trắng đầu/cuối đều bị từ chối trước khi truy vấn
tài khoản. Đăng nhập hợp lệ/không hợp lệ qua Cloudflare cũng đạt.
Chrome headless kiểm tra gõ, dán, composition của IME và HTML pattern tại
login, thêm nhân viên và sửa nhân viên. Mobile 320×568 không tràn nội dung.
Checksum của cả 9 bảng trước/sau cập nhật không đổi.

## Thông báo đăng nhập (17/11/2026)

Maven `verify`: **236 test đạt, 0 failure, 0 error, 0 skipped**;
SystemIntegrationTest 90 ca, HttpSecurityTest 4 ca. Sai username/mật khẩu,
kể cả mật khẩu sai của tài khoản khóa, chỉ nhận thông báo sai thông tin.
Chỉ đúng cả username và mật khẩu của tài khoản khóa mới hiện trạng thái khóa.
Có kiểm thử trường trống, mật khẩu quá dài, query giả trạng thái và xóa thông
báo sau khi hiển thị. Hai ca HTTP thật xác nhận thông báo qua servlet/redirect.

Chrome headless với locale vi xác nhận nhắc username/password trống vẫn bằng
tiếng Anh, focus đúng trường, sửa lại cho phép đăng nhập và lỗi sai thông tin
chỉ có một dòng. Docker/Cloudflare kiểm tra 7 trường hợp sai/trống; cả 8
tài khoản hiện có vẫn đăng nhập. Checksum cả 9 bảng trước/sau không đổi.

## Cảnh báo username và giữ nội dung (17/11/2026)

Thay đổi frontend: giữ nguyên giá trị khi nhập sai, hiện cảnh báo dưới trường
và viền đỏ, chặn gửi đến khi sửa đúng. Bỏ xử lý xóa/khôi phục chữ và chặn
paste/beforeinput; giới hạn 50 ký tự chuyển sang cảnh báo và kiểm tra pattern.
Quy tắc backend giữ nguyên.

Chrome headless kiểm tra login, thêm và sửa nhân viên: chữ có dấu, khoảng
trắng, dấu chấm/gạch ngang/@, emoji và 51 ký tự không bị xóa hoặc cắt. Sự kiện
paste và composition giữ nội dung/vị trí con trỏ. Sửa thành username hợp lệ
xóa cảnh báo, bỏ trạng thái aria-invalid và cho qua kiểm tra form.
Ba tình huống trường trống vẫn nhắc bằng tiếng Anh, sửa đúng đăng nhập được
và sai mật khẩu vẫn chỉ có một dòng thông báo. `node --check` và Docker build đạt.

## Quyền thanh toán và hóa đơn của Cashier (17/11/2026)

Maven `verify`: **244 test đạt, 0 failure, 0 error, 0 skipped**;
SystemIntegrationTest có 98 ca. Bỏ shortcut View payments & invoices của
Manager. SecurityFilterChain và PaymentService chỉ cho Cashier truy cập
trang thanh toán, API chi tiết thanh toán, hóa đơn và xác nhận thanh toán.
Kiểm thử Manager gọi trực tiếp service cũng bị từ chối.

Docker/MySQL và Cloudflare: Manager bị 403 ở /payments, API và /invoices;
Cashier nhận 200. Waiter/Chef cũng bị từ chối. Dashboard Manager giữ liên kết
báo cáo doanh thu và lịch sử thanh toán; các trang này vẫn trả 200 cho Manager.
Cashier không được vào báo cáo Manager. Checksum cả 9 bảng trước/sau không đổi.
