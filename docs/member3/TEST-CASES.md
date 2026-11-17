# Ca kiểm thử TV3 UC06 và UC07

Nguồn: UC06/UC07, BR01, BR04–BR10, yêu cầu Security/Reliability trong tài liệu chính. Các ca tự động nằm trong `backend/src/test/java/com/restaurant/PreparationIntegrationTests.java`; bảng dưới gom các tham số kiểm thử cùng nghiệp vụ. Chạy `verify.cmd` để thực hiện trên H2 độc lập; không reset MySQL đang sử dụng.

| ID | Điều kiện và thao tác | Kết quả mong đợi | Bằng chứng tự động |
| --- | --- | --- | --- |
| TV3-01 | Chef đánh dấu Preparing → Ready trên một dòng của đơn Open | Chỉ dòng đó đổi; quantity, tên/giá snapshot, tổng, payment và bàn giữ nguyên | chefAdvancesOnlySelectedLineAndPreservesSavedData |
| TV3-02 | Thử 12 tổ hợp đổi sai thứ tự từ Pending/Preparing/Ready/Served/Cancelled | Từ chối và không lưu sai trạng thái | invalidTransitionsDoNotPersist |
| TV3-03 | Chuyển bước hợp lệ trên đơn Completed hoặc Cancelled, 6 tổ hợp | Từ chối vì đơn không Open | closedOrdersCannotChange |
| TV3-04 | Lặp lại xác nhận Start/Ready/Served, 3 ca | Lần thứ hai bị từ chối; không bỏ bước hoặc phục vụ trùng | duplicateConfirmationIsRejected |
| TV3-05 | Order ID không tồn tại, line ID không tồn tại hoặc thuộc đơn khác | Không đổi dữ liệu; báo không tìm thấy/không thuộc đơn | orderAndLineIdsMustMatch |
| TV3-06 | Chọn tab Preparing và kiểm tra các dòng khác cùng đơn | Chỉ đơn Open chứa Preparing xuất hiện; các dòng khác giữ đúng trạng thái; đếm theo dòng | kitchenIncludesContextButOnlyForOpenOrdersContainingSelectedStage |
| TV3-07 | Waiter lọc theo món/bàn/ORD/line, bàn khác, ký tự tìm kiếm đặc biệt | Chỉ dòng Ready phù hợp; tìm kiếm theo chuỗi literal, không SQL injection | servingFiltersReadyLinesByFoodTableAndOrderWithLiteralSearch |
| TV3-08 | Chef Mark Ready rồi Waiter Served | Ready count/revision đổi đúng; dòng đã Served rời queue Ready | readyCountsAndRevisionFollowIndividualTransitions |
| TV3-09 | Không còn Pending hoặc Ready | Render trạng thái rỗng đúng, không lỗi template | emptyKitchenAndServingStatesAreSupported |
| TV3-10 | Bốn actor gọi GET/JSON/POST Kitchen và Serving | Chef chỉ bếp, Waiter chỉ phục vụ, Cashier/Manager bị chặn | routesEnforceChefAndWaiterBoundaries |
| TV3-11 | Gọi trực tiếp service bằng role không được phép | Chặn ở service, kể cả bỏ qua UI/controller | servicesAlsoRejectUnauthorizedRoles; chefCannotServeAndWaiterCannotCookThroughService |
| TV3-12 | POST không CSRF hoặc chưa đăng nhập | Không thay trạng thái; 403 hoặc yêu cầu đăng nhập phù hợp | csrfAndAnonymousRequestsCannotChangeItems; HttpSecurityIntegrationTests |
| TV3-13 | Mở dialog cũ, xác nhận sau khi dòng đã đổi | Flash lý do trạng thái cũ, hướng dẫn Refresh; dữ liệu hợp lệ giữ nguyên | staleConfirmationShowsAnActionableError |
| TV3-14 | Status/tab không hợp lệ, table ID sai kiểu/âm, chuỗi tìm kiếm quá dài | HTTP 400 và thông báo rõ | invalidFiltersAndIdsReturn400 |
| TV3-15 | Tên món/ghi chú có HTML/JavaScript | Hiển thị như văn bản, không thực thi | retainedNamesAndNotesAreEscaped |
| TV3-16 | Tạo đơn → sửa giá catalog → Chef Preparing/Ready → Waiter Served → thanh toán Cash/Card/Bank Transfer → invoice/report | Chặn thanh toán trước Served; sau Served có đúng Payment/Invoice, Completed, Available; dùng giá snapshot | completeOrderKitchenServingPaymentAndReportFlowUsesSavedPrices (3 phương thức) |
| TV3-17 | Hai Chef cùng Start Preparing một dòng | Đúng một lần thành công, một lỗi nghiệp vụ; trạng thái cuối Preparing | concurrentChefsCannotAdvanceOnePendingLineTwice |
| TV3-18 | Chef bắt đầu nấu và Waiter hủy đơn đồng thời | Chỉ một bên thắng; không có trạng thái đơn/món/bàn trộn lẫn | preparationAndOrderCancellationHaveOnlyOneWinner |
| TV3-19 | Waiter xác nhận Served và Cashier thanh toán đồng thời | Nếu chưa Served thì checkout bị chặn; nếu thanh toán thành công, các bản ghi/trạng thái đồng bộ | concurrentServingAndCheckoutRemainConsistent |
| TV3-20 | Giả lập lỗi DB tại bước saveAndFlush | Rollback về trạng thái cũ; thông báo không có thay đổi được commit | failedSaveRollsBackAndShowsARecoveryMessage |

Kiểm tra thủ công/HTTP trên MySQL và giao diện Chrome được cập nhật trong `../VERIFICATION.md`. Dữ liệu smoke test cần dùng database riêng; tài khoản demo bốn actor đều có mật khẩu `123456`.
