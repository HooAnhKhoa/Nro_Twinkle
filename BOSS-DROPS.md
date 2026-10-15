# Popup Boss và bảng vật phẩm rơi

## Thao tác trên web

Menu **Boss** hiển thị thẻ có ảnh, tên, HP, trạng thái và map. Tìm theo tên, map hoặc HP; lọc đang xuất hiện, chờ xuất hiện, đang tắt. Thứ tự nhiệm vụ và cách gộp các boss trùng vẫn giữ nguyên.

Bấm thẻ mở popup hai tab:

- **Thông tin**: vị trí thực tế, HP, sát thương, thời gian hồi, thời gian tồn tại và hẹn xuất hiện.
- **Vật phẩm rơi**: chọn chế độ, tìm vật phẩm theo tên/ID, sửa tỷ lệ, khoảng số lượng và chỉ số.

Chỉ dùng menu **Boss**. Tab **Vật phẩm rơi** nằm trong popup của boss; liên kết Boss rơi cũ chuyển về Boss. Đóng bằng nút × hoặc Escape; có nhắc khi còn thay đổi chưa lưu.

## Bốn chế độ rơi

| Chế độ | Vật phẩm gốc | Bảng tùy chỉnh |
|---|---|---|
| Giữ nguyên đồ gốc | Theo code gốc | Giữ làm bản nháp |
| Đồ gốc + đồ bổ sung | Theo code gốc | Rơi thêm |
| Thay bằng bảng rơi | Không rơi | Dùng bảng tùy chỉnh |
| Tắt rơi vật phẩm | Không rơi | Giữ bảng để bật lại |

Mỗi dòng quay độc lập theo % tuyệt đối, không cần cộng thành 100%. 0% tắt dòng, 100% luôn rơi. Khoảng số lượng gồm cả hai đầu. Tối đa 30 dòng, số lượng 1–99.999, tối đa 15 chỉ số/dòng; ID vật phẩm/chỉ số phải tồn tại, giá trị chỉ số 0–32.767. Chỉ số tùy chỉnh là giá trị cố định đã nhập. Bảng gốc vẫn chạy theo điều kiện và chỉ số ngẫu nhiên trong source; phần xem trước hiện tên/icon của những món có thể rơi theo nhánh gốc, không suy đoán phần trăm hay chuyển chúng thành dòng tùy chỉnh.

Các thay đổi nhận trong khoảng 3 giây. Web báo chờ nhận/đã nhận theo revision server thực sự nạp. Lưu từ một phiên cũ bị từ chối để tránh đè cấu hình vừa sửa ở phiên khác. Lỗi kiểm tra hoặc SQL không đổi dữ liệu đang lưu.

## Cơ chế server

`Boss.giveReward` là tuyến thưởng khi hạ boss, gọi lại logic `reward` hiện có. Vật phẩm tùy chỉnh thuộc người hạ; nếu đệ tử hạ thì thuộc chủ nhân. Mỗi lượt thưởng chỉ xử lý một lần, đặt lại ở lần hồi sinh. Không phát thưởng mới khi boss chỉ hết thời gian tồn tại.

`BossDrops` dùng scope theo thread để chặn riêng việc đăng ký vật phẩm gốc vào `Zone` và gói tin xuất hiện. Vì `ItemMap` tự thêm vào map ngay trong constructor, chỉ chặn gói tin sẽ để lại đồ ẩn; cả đăng ký và gửi gói tin đều được chặn. Scope được khôi phục bằng `finally`, không ảnh hưởng đồ rơi ngoài scope hoặc trên thread khác.

Phần thưởng trễ trong trạng thái AFK cũng dùng scope. Điểm, nhiệm vụ và việc gọi boss trong chuỗi vẫn chạy. Với Sơn Tinh/Thủy Tinh, vật phẩm trao trễ được điều khiển theo boss thực sự trao nó. Ông già Noel dùng bảng này cho từng lần phát quà theo chu kỳ và điều kiện tiếp cận cũ; quà vẫn công khai cho mọi người.

`boss_web_drop_config` lưu mode, JSON vật phẩm, revision và applied_revision. Boss không có bản ghi mặc định giữ nguyên source. Không tự thêm vật phẩm vào bảng rơi trên máy chính khi triển khai.

## Kiểm tra và hoàn tác

- `test/BossDropsTest.java`: vật phẩm thật trên map, bốn chế độ, tuyến die, tỷ lệ 0/100%, số lượng, chỉ số, chủ sở hữu/đệ tử, chống gọi thưởng lặp, scope trễ, lỗi và thread độc lập, quà định kỳ, xác nhận revision.
- `test/BossDropsApiTest.ps1`: chỉ web/database QA; lưu bốn chế độ, tìm vật phẩm, kiểm tra chỉ số, từ chối revision cũ và dữ liệu sai; khôi phục cấu hình QA sau thử.
- Hồi quy: thời gian Boss, lọc/gộp danh sách, Trung Thu và Noel. Kiểm tra JAR nạp toàn bộ class, bao gồm `ConstIgnoreName`.

Bản sao lưu: `C:\Downloads\NroFreeSetup\Backups\boss-popup-drops-20261015`. Có JAR cũ, web cũ và manifest phân biệt file mới, cấu hình SQL trước sửa, nhật ký kiểm thử và triển khai. Khi hoàn tác mã: bảo trì/lưu, chờ JVM dừng, khôi phục JAR và web cũ theo manifest rồi chạy lại. Có thể giữ bảng SQL mới khi chạy mã cũ; không khôi phục dữ liệu người chơi.

## Xem trước vật phẩm và kho chọn đồ

Trong Boss → chọn boss → Vật phẩm rơi, danh sách **Vật phẩm có thể rơi** gồm ảnh/tên/ID của đồ gốc ở chế độ original/add và các dòng tùy chỉnh có tỷ lệ lớn hơn 0 ở chế độ add/replace. Chế độ off không hiện món rơi; thay đổi chưa lưu được ghi rõ. Các món gốc là khả năng theo điều kiện/nhánh ngẫu nhiên, không bảo đảm cùng rơi mỗi lượt. Bảng này không sửa logic thưởng trong game.

Kho vật phẩm mở/thu gọn, xem 24 món/trang, tìm tên/ID, hiện ảnh thật và bấm thêm vào bản nháp. Enter trong ô tìm kiếm chỉ tìm, không tự lưu cấu hình. Bảng xem trước tùy chỉnh có %, khoảng số lượng và chỉ số đã đặt.

Danh mục đọc từ `web/boss_native_drops.json`, tạo lại bằng `node scripts/build-boss-loot-catalog.cjs` sau khi sửa source thưởng hoặc ID Trung Thu. Script đọc source, không chạy hàm thưởng; các biểu thức chưa giải được được đánh dấu chưa đầy đủ. Các ID sự kiện Trung Thu dùng `config/trung-thu-2026.properties`; tỷ lệ/điều kiện vẫn theo runtime. `node test/BossNativeDropsTest.cjs` đối chiếu các boss phổ biến, nhánh ngẫu nhiên, hàm phụ và boss sự kiện. Đã đối chiếu đủ khóa của 93 lớp boss đang có heartbeat; API tên/icon lấy trực tiếp từ item_template.

Bản dự án đang dùng: `D:\NroFreeSetup\Work\SourceGame-441D49A9989D`. Bản sao lưu thay đổi web lần này: `D:\NroFreeSetup\Backups\boss-loot-preview-20261015\live-before`. Khi hoàn tác giao diện, khôi phục các file theo bản sao lưu; không thay dữ liệu người chơi hoặc bảng rơi đã lưu.