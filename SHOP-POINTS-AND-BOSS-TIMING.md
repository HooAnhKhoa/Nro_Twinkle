# Giá điểm và thời gian Boss

## Cửa hàng đổi điểm Quy Lão

Tab 59 dùng `item_shop.cost` cho cả gói tin hiển thị và giá mua, kể cả cải trang mới thêm. Ví dụ CT Himmel (1632) giá 10.000 điểm; giá của vật phẩm cũ cũng không còn bị gán cứng theo ID. Sau khi sửa shop trên web, cập nhật shop và mở lại shop trong game để nhận dữ liệu mới.

`PointShop` kiểm tra đủ điểm, thêm vật phẩm và lưu `items_bag`, `data_inventory`, `event_point` trong một giao dịch. Túi đầy hoặc lỗi lưu giữ nguyên điểm và hành trang. Giá âm bị từ chối; giá 0 được quản trị viên cấu hình rõ ràng vẫn là miễn phí. Không dùng ngọc/vàng thay cho điểm.

## Thời gian Boss trên web

- **Hồi cấu hình**: số giây nghỉ trước chu kỳ tiếp theo. Sửa thời gian hồi tác động cả chu kỳ đang nghỉ; boss theo chuỗi, triệu hồi trả phí hoặc lịch riêng vẫn phải thỏa điều kiện gốc.
- **Xuất hiện tiếp**: đếm ngược từ trạng thái thực tế của server. Hết thời gian nhưng map chưa sẵn sàng hiển thị “Đang chờ vào map”; không tự suy diễn thời gian cho boss có điều kiện riêng.
- **Đã tồn tại**: tính từ lần vào map đầu tiên của lượt boss, giữ qua các dạng cho đến khi về trạng thái nghỉ.
- **Thời gian tồn tại**: để trống giữ cơ chế gốc; 0 bỏ giới hạn tự rời map; số dương là giới hạn tính từ lúc vào map. Giảm giới hạn có thể khiến boss đang sống rời map ngay. Hết hạn không phát thưởng hạ boss và kết thúc lượt các dạng trong cùng boss. Vẫn gọi hook rời map để dọn trạng thái riêng; nếu hook gốc gọi một boss khác (như Broly → Super Broly), cơ chế đó vẫn chạy.
- **Hẹn xuất hiện sau**: lịch một lần cho một boss độc lập đang vắng mặt. 0 gọi ngay; số dương ưu tiên hơn thời gian nghỉ tự nhiên của instance được hẹn. Lịch nằm trong RAM, mất khi server khởi động lại; tắt sự kiện hủy lịch. Không gọi miễn phí boss yêu cầu vật phẩm triệu hồi.

Các giá trị chỉnh nhận trong khoảng 3 giây, giới hạn 0–604.800 giây. Đồng hồ hiển thị cập nhật mỗi giây, dữ liệu server nhận mỗi 3 giây và trang làm mới mỗi 5 giây. Khi offline không hiển thị đếm ngược như dữ liệu đang sống. Với nhiều instance của cùng class, hiển thị mốc xuất hiện/kết thúc gần nhất và tuổi lượt lâu nhất; các vị trí thực tế vẫn được giữ.

Schema bổ sung tương thích dữ liệu cũ: `boss_web_config.lifetime_seconds` (NULL = cơ chế gốc), `timing_state` và `boss_web_command.delay_seconds`. Không thay giá shop hay cấu hình Boss đang có khi nâng cấp.

## Kiểm tra và hoàn tác

- `test/PointShopTest.java`: gói tin shop thật, tuyến mua thật, CT mới, đổi giá CT cũ, thiếu điểm, túi đầy, lỗi SQL và giá 0/âm; chỉ database QA.
- `test/BossTimingTest.java`: trạng thái Boss thật, chỉnh hồi, lịch hẹn, vào map, hết hạn, hook dọn trạng thái và sự kiện tắt.
- `test/BossGroupingTest.php`: giữ lọc quái/nhóm trùng và thứ tự nhiệm vụ.
- API QA: lưu giới hạn dương/0/NULL, hẹn xuất hiện, từ chối số âm/vượt mức.
- `test/VerifyRuntimeJar.java`: kiểm tra toàn bộ class trong JAR và `ConstIgnoreName`.

Bản sao lưu lần triển khai: `C:\Downloads\NroFreeSetup\Backups\shop-points-boss-times-20261015`. Có JAR trước sửa, cấu hình shop/Boss trước sửa, web cũ và nhật ký triển khai. Hoàn tác JAR chỉ sau bảo trì/lưu và JVM đã dừng; khôi phục các file trong `live-web-before` về đường dẫn tương ứng ở `C:\xampp\htdocs`. Các cột SQL bổ sung có thể giữ nguyên khi chạy bản cũ. Không khôi phục nguyên bảng người chơi để hoàn tác bản mã này.
