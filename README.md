# NRO Twinkle — Tài liệu tổng hợp

Tài liệu duy nhất của dự án, gộp đầy đủ 15 file Markdown trước đây ngày 16/10/2026. Các mục lịch sử giữ nguyên thông tin tại thời điểm viết; hash JAR, PID và đường dẫn cũ trong các mục đó không đại diện cho bản đang chạy.

## Chạy trên máy chính

1. Bật **Apache** và **MySQL** trong XAMPP.
2. Mở `D:\NroFreeSetup\Work\SourceGame-441D49A9989D\run.bat` nếu game chưa chạy. Nếu cổng 14445 đang mở thì không chạy thêm server.
3. Chờ `Server initialized and listening on port 14445`.
4. Mở panel tại `http://192.168.2.10/admin/panel.php`.

Game chạy từ ổ D; XAMPP và database vẫn ở `C:\xampp`. Bảo trì trên panel lưu sau 15 giây và tự khởi động lại sau 10 giây. Giữ tiến trình `run.bat` để tự khởi động lại; không tắt MySQL khi game đang hoạt động.

## File cần giữ

- `Teamobi2026-panel-launcher.jar`: JAR đang chạy; `Teamobi2026.jar`: JAR nền để build.
- `src/`, `web/`, `lib/`, `data/`, `config/`, `Config.properties`: source, thư viện, tài nguyên và cấu hình.
- `run.bat`, `restart_server.bat`, các script build/triển khai/khôi phục, `build.xml`, `nbproject/`: công cụ phát triển và vận hành.
- `test/`, `build/panel-qa/`, `build/boss-fixtures.json`: kiểm thử dùng lại và môi trường QA riêng.
- SQL, bảng tính và tài liệu gốc của người dùng được giữ nguyên.

Class biên dịch và `dist/` có thể tạo lại. Chạy `powershell -File .\build-panel.ps1` để build; mặc định tạo `Teamobi2026-panel.jar`, không tự thay JAR đang chạy. Không build đè JAR đang chạy.

## Dọn file ngày 16/10/2026

Đã gộp tài liệu, dọn hai bộ class sinh ra, output NetBeans cũ, log/debug cũ và thư mục QA rỗng. Môi trường QA, tài nguyên và dữ liệu game được giữ nguyên. Bản sao file gốc cùng manifest SHA256 nằm ngoài repo tại `D:\NroFreeSetup\Backups\cleanup-20261016-docs\removed-files.zip`. Có thể giải nén theo đúng đường dẫn trong manifest để khôi phục; sao lưu README hiện tại trước khi khôi phục tài liệu cũ.

## Mục lục

- [Thiết lập dự án gốc](#doc-readme)
- [Hành trang](#doc-bag-inventory)
- [Bò Mộng và điểm sự kiện](#doc-bo-mong-event-points)
- [Giftcode](#doc-gift-code)
- [Boss và vật phẩm rơi](#doc-boss-drops)
- [Shop điểm và thời gian Boss](#doc-shop-points-and-boss-timing)
- [Himmel và cộng dồn sát thương](#doc-himmel-stats)
- [Quản lý Tori Bot](#doc-toribot-management)
- [Sự kiện Noel](#doc-noel-logic)
- [Hướng dẫn Trung Thu](#doc-trung-thu-2026)
- [Tài nguyên Trung Thu](#doc-trung-thu-2026-assets)
- [Bảng thưởng Trung Thu đã duyệt](#doc-trung-thu-2026-reward-proposal)
- [Triển khai Trung Thu](#doc-trung-thu-2026-implementation)
- [Lịch sử thay đổi panel](#doc-panel-changes)
- [Lịch sử dọn file](#doc-cleanup)

---

<a id="doc-readme"></a>
## Thiết lập dự án gốc

## Nro Twinkle

Java game server project using NetBeans and Ant.

### Local setup

1. Copy `Config.example.properties` to `Config.properties` and set your database connection.
2. Keep the original `data/` assets and import your local database SQL dump. Assets and SQL dumps are excluded from Git; database dumps may contain player accounts and passwords.
3. Open the project in NetBeans, configure the JDK and libraries, then build using `build.xml`.

Build output, machine-specific IDE settings, logs, and local configuration are excluded from Git.

---

<a id="doc-bag-inventory"></a>
## Hành trang

## Hành trang tối đa 100 ô

Hành trang có giới hạn **100 ô liền nhau**, dùng giao diện túi gốc của client, không chia trang. Rương đồ giữ nguyên tối đa 100 ô.

- Túi trên 100 ô được thu gọn sau khi game bảo trì lưu và dừng. Giữ nguyên toàn bộ vật phẩm, số lượng, chỉ số và thứ tự tương đối; chỉ bỏ ô rỗng. Nếu thực tế hơn 100 ô có đồ thì từ chối chuyển dữ liệu và không nạp JAR mới.
- Túi đã mở ít hơn hoặc bằng 100 ô giữ nguyên kích thước. Vật phẩm mở rộng hành trang 517 tăng từng ô đến 100 rồi từ chối mở thêm.
- Gói đăng nhập/cập nhật gửi toàn bộ các ô đang có, tối đa 100. Chỉ số hợp lệ 0–99. Giữ cách đọc unsigned và sentinel255/-1 cho các lệnh gốc; không thay giao thức thành 2 byte.
- Giữ kiểm tra xác nhận bán/dùng/bỏ đồ một lần và cột LONGTEXT để lưu đầy đủ vật phẩm nhiều chỉ số.

### Kiểm tra

`test/BagInventory100Test.java`: 30 kiểm tra gói native và các thao tác tại ô80/90/99, gồm dùng, dùng theo ID, mặc, bỏ, bán, cất rương, giao dịch, ký gửi, ghép, ô rỗng, túi cũ nhỏ hơn và giới hạn 100.

`test/GiftCodeRedemptionTest.java`: hành trang mẫu 100 ô, kiểm tra trao thưởng/lưu dữ liệu, đầy túi, nhận trùng và hoàn tác lỗi trên DB QA riêng. `test/BagStorageTest.java` giữ kiểm tra lưu tiếng Việt và hơn64KB trong bảng tạm QA.

Công cụ chuyển chạy một lần được lưu trong bản sao để hoàn tác, không để lại trong source. Có kiểm tra bảo toàn món ở ô100/149 cũ, túi nhỏ hơn, túi đúng100, túi rỗng và từ chối hơn100 món.

### Bản sao để hoàn tác

`C:/Downloads/NroFreeSetup/Backups/bag100-20261015` giữ JAR150 cũ, dữ liệu hành trang được đọc lại sau khi JVM lưu và dừng, biên nhận chuyển100 và kết quả kiểm tra. Chỉ thay JAR sau khi chuyển dữ liệu thành công; nếu chuyển thất bại, giữ JAR cũ để launcher khởi động lại bình thường.

Các bản sao lịch sử 300→150 vẫn giữ tại `Backups/bag150-20261015` và `Backups/cleanup-once-20261015`. Giữ LONGTEXT khi quay lại JAR cũ. Không phục hồi đè bản sao túi lên dữ liệu sau khi người chơi đã nhận, dùng hoặc giao dịch thêm vật phẩm.

---

<a id="doc-bo-mong-event-points"></a>
## Bò Mộng và điểm sự kiện

## Điểm sự kiện và Bò Mộng ở nhà

### Điểm tại cửa hàng trong ảnh

Đây là `player.event.eventPoint`, lưu vào cột `player.event_point`, dùng ở Quy Lão Kame → Đổi điểm sự kiện (`SHOP_DOI_DIEM`, tab 59). Không phải điểm danh hoặc trường `inventory.coupon`.

Cách có điểm trong source hiện tại:

- Người kết liễu nhiều boss được 5 điểm/lần: Kuku, Mập Đầu Đinh, Rambo; Android 13/14/15/19, Dr. Kôrê, Píc/Pọc; Fide, Cooler; các Xen con và Siêu Bọ Hung; Bojack và nhóm; Black Goku, các boss Mabư. Mỗi lớp boss có hàm reward riêng; không mặc định mọi boss đều cho điểm.
- Sơn Tinh/Thủy Tinh cho 5 điểm khi sự kiện Giỗ Tổ đang bật và phần thưởng boss hợp lệ.
- Ăn Trộm cho 5 điểm khi đã trộm được vàng (`goldAnTrom != 0`).
- Đổi số dư VNĐ sang ngọc hoặc thỏi vàng: `(số VNĐ / 10.000) * 5.000` điểm, lấy phần nguyên. Ví dụ 10.000 VNĐ được 5.000 điểm; 20.000 VNĐ được 10.000 điểm. Chỉ nạp số dư mà chưa đổi chưa chạy hai nhánh cộng điểm này.

Ví dụ giá đổi hiện tại: Black Goku Rose 999; Cân Đẩu Vân Thơ Mộng 750; Khủng Long Thơ Mộng, Hải Ly, Urôn 499; hộp Goku/Cađíc VIP 99. Điểm hiện có nằm trong nhãn menu Đổi điểm sự kiện ở Quy Lão.

Mã liên quan: `src/nro/models/player/PlayerEvent.java`, `src/nro/models/services_func/Input.java` (TRADE_GOLD/TRADE_GEM), `src/nro/models/shop/ShopService.java` (tab 59), các hàm `reward` của từng boss.

### Bò Mộng

Bò Mộng ở Nhà Gôhan (21), Nhà Moori (22), Nhà Broly (23) chỉ có Đổi thỏi vàng, Đổi ngọc xanh và Đóng. Dùng menu riêng, không nhận các lệnh nhiệm vụ/thành tích/điểm danh của menu cũ.

Bò Mộng ở Rừng Karin (47) và Siêu Thị (84) giữ nhiệm vụ hằng ngày, thành tích, Nạp Ngọc và điểm danh. Nhiệm vụ giới hạn gốc 10/ngày; điểm danh gốc 10.000 ngọc + 100 thỏi vàng/ngày. Khi bật Trung Thu, trả nhiệm vụ Khó/Siêu khó vẫn đi qua nhánh TrungThuQuest hiện có.

Hai lựa chọn tại nhà dùng số dư `account.vnd` trực tiếp trong database:

- 10.000 VNĐ = 40 thỏi vàng hoặc 10.000 ngọc xanh.
- Mỗi 10.000 VNĐ thêm 10 vé tặng ngọc (718) và 5.000 điểm sự kiện dùng tại Quy Lão; tính theo số lần đủ 10.000 VNĐ.
- Nhập từ 10.000 đến 5.000.000 VNĐ/lần. Đổi thỏi vàng yêu cầu tài khoản đã kích hoạt theo điều kiện gốc.
- `VndExchangeService` khóa hàng tài khoản, đọc lại số dư hiện tại, rồi lưu trừ VNĐ, túi, ngọc và điểm sự kiện trong cùng transaction InnoDB. Bản sao số dư trong phiên chơi được cập nhật theo database. Đầy túi, tràn ngọc/điểm, sai tài khoản, bảo trì hoặc lỗi lưu không tiêu hao VNĐ và không nhận thưởng một phần.

Kiểm tra:

- `test/BoMongHomeTest.java`: 43 kiểm tra không DB cho menu riêng ở cả ba nhà, đúng loại form đổi, chặn menu cũ ở nhà, và hồi quy chức năng tại hai NPC bên ngoài.
- `test/VndExchangeTest.java`: 25 kiểm tra chỉ trong DB QA `nro_panel_qa_20261013`, gồm số dư phiên chơi lệch DB, tỷ lệ quà, giới hạn, tài khoản kích hoạt, túi đầy, tràn tiền/điểm, sai tài khoản, lỗi SQL, đồng thời và packet nhập thực tế. Lỗi SQL giả trong test được tạo có chủ đích để xác minh hoàn nguyên.

---

<a id="doc-gift-code"></a>
## Giftcode

## Nhập giftcode trong game

Santa → Nhập mã quà tặng. Mã được đọc trực tiếp từ bảng giftcode khi xác nhận; tạo/sửa mã trên web có hiệu lực ngay, không cần khởi động lại hoặc bấm Update Code. Không phân biệt hoa/thường và bỏ khoảng trắng ở hai đầu.

Luồng nhận kiểm tra hạn hiện tại, ngày bắt đầu, lượt còn lại và lịch sử nhận từ cả DB lẫn phiên chơi. Phần thưởng, túi, tiền, lịch sử nhận và lượt còn lại lưu trong cùng transaction InnoDB. Hết chỗ, tràn tiền, mã lỗi hoặc lỗi lưu đều không tiêu hao mã. Mã có count_left=-1 giữ nguyên chế độ không giới hạn. Giữ nguyên lịch sử đã nhận của người chơi.

Dùng cùng cơ chế hoàn nguyên túi/tiền BagExchange và bộ tuần tự hóa túi hiện có. Mỗi phần thưởng giữ ID, số lượng và option riêng; hỗ trợ các mã tiền -1 (vàng), -2 (ngọc xanh), -3 (hồng ngọc). Vật phẩm có tác dụng đặc biệt ngoài túi chưa được cơ chế trao thưởng an toàn hỗ trợ sẽ bị từ chối toàn bộ, giữ mã cho người chơi.

`test/GiftCodeRedemptionTest.java` chỉ chạy trong DB `nro_panel_qa_20261013`. Kiểm tra mã mới/đổi tên không nạp cache, nhập mã Ngọc Rồng 1–7 sao, form Santa khi có/không phiếu giảm giá, packet nhập mã, nhận trùng/reconnect, hạn/ngày bắt đầu, hết lượt/không giới hạn, túi đầy/gộp chồng, tiền/tràn tiền, lỗi SQL và tranh lượt cuối. Lỗi SQL giả trong test được tạo có chủ đích để xác minh hoàn nguyên.

---

<a id="doc-boss-drops"></a>
## Boss và vật phẩm rơi

## Popup Boss và bảng vật phẩm rơi

### Thao tác trên web

Menu **Boss** hiển thị thẻ có ảnh, tên, HP, trạng thái và map. Tìm theo tên, map hoặc HP; lọc đang xuất hiện, chờ xuất hiện, đang tắt. Thứ tự nhiệm vụ và cách gộp các boss trùng vẫn giữ nguyên.

Bấm thẻ mở popup hai tab:

- **Thông tin**: vị trí thực tế, HP, sát thương, thời gian hồi, thời gian tồn tại và hẹn xuất hiện.
- **Vật phẩm rơi**: chọn chế độ, tìm vật phẩm theo tên/ID, sửa tỷ lệ, khoảng số lượng và chỉ số.

Chỉ dùng menu **Boss**. Tab **Vật phẩm rơi** nằm trong popup của boss; liên kết Boss rơi cũ chuyển về Boss. Đóng bằng nút × hoặc Escape; có nhắc khi còn thay đổi chưa lưu.

### Bốn chế độ rơi

| Chế độ | Vật phẩm gốc | Bảng tùy chỉnh |
|---|---|---|
| Giữ nguyên đồ gốc | Theo code gốc | Giữ làm bản nháp |
| Đồ gốc + đồ bổ sung | Theo code gốc | Rơi thêm |
| Thay bằng bảng rơi | Không rơi | Dùng bảng tùy chỉnh |
| Tắt rơi vật phẩm | Không rơi | Giữ bảng để bật lại |

Mỗi dòng quay độc lập theo % tuyệt đối, không cần cộng thành 100%. 0% tắt dòng, 100% luôn rơi. Khoảng số lượng gồm cả hai đầu. Tối đa 30 dòng, số lượng 1–99.999, tối đa 15 chỉ số/dòng; ID vật phẩm/chỉ số phải tồn tại, giá trị chỉ số 0–32.767. Chỉ số tùy chỉnh là giá trị cố định đã nhập. Bảng gốc vẫn chạy theo điều kiện và chỉ số ngẫu nhiên trong source; phần xem trước hiện tên/icon của những món có thể rơi theo nhánh gốc, không suy đoán phần trăm hay chuyển chúng thành dòng tùy chỉnh.

Các thay đổi nhận trong khoảng 3 giây. Web báo chờ nhận/đã nhận theo revision server thực sự nạp. Lưu từ một phiên cũ bị từ chối để tránh đè cấu hình vừa sửa ở phiên khác. Lỗi kiểm tra hoặc SQL không đổi dữ liệu đang lưu.

### Cơ chế server

`Boss.giveReward` là tuyến thưởng khi hạ boss, gọi lại logic `reward` hiện có. Vật phẩm tùy chỉnh thuộc người hạ; nếu đệ tử hạ thì thuộc chủ nhân. Mỗi lượt thưởng chỉ xử lý một lần, đặt lại ở lần hồi sinh. Không phát thưởng mới khi boss chỉ hết thời gian tồn tại.

`BossDrops` dùng scope theo thread để chặn riêng việc đăng ký vật phẩm gốc vào `Zone` và gói tin xuất hiện. Vì `ItemMap` tự thêm vào map ngay trong constructor, chỉ chặn gói tin sẽ để lại đồ ẩn; cả đăng ký và gửi gói tin đều được chặn. Scope được khôi phục bằng `finally`, không ảnh hưởng đồ rơi ngoài scope hoặc trên thread khác.

Phần thưởng trễ trong trạng thái AFK cũng dùng scope. Điểm, nhiệm vụ và việc gọi boss trong chuỗi vẫn chạy. Với Sơn Tinh/Thủy Tinh, vật phẩm trao trễ được điều khiển theo boss thực sự trao nó. Ông già Noel dùng bảng này cho từng lần phát quà theo chu kỳ và điều kiện tiếp cận cũ; quà vẫn công khai cho mọi người.

`boss_web_drop_config` lưu mode, JSON vật phẩm, revision và applied_revision. Boss không có bản ghi mặc định giữ nguyên source. Không tự thêm vật phẩm vào bảng rơi trên máy chính khi triển khai.

### Kiểm tra và hoàn tác

- `test/BossDropsTest.java`: vật phẩm thật trên map, bốn chế độ, tuyến die, tỷ lệ 0/100%, số lượng, chỉ số, chủ sở hữu/đệ tử, chống gọi thưởng lặp, scope trễ, lỗi và thread độc lập, quà định kỳ, xác nhận revision.
- `test/BossDropsApiTest.ps1`: chỉ web/database QA; lưu bốn chế độ, tìm vật phẩm, kiểm tra chỉ số, từ chối revision cũ và dữ liệu sai; khôi phục cấu hình QA sau thử.
- Hồi quy: thời gian Boss, lọc/gộp danh sách, Trung Thu và Noel. Kiểm tra JAR nạp toàn bộ class, bao gồm `ConstIgnoreName`.

Bản sao lưu: `C:\Downloads\NroFreeSetup\Backups\boss-popup-drops-20261015`. Có JAR cũ, web cũ và manifest phân biệt file mới, cấu hình SQL trước sửa, nhật ký kiểm thử và triển khai. Khi hoàn tác mã: bảo trì/lưu, chờ JVM dừng, khôi phục JAR và web cũ theo manifest rồi chạy lại. Có thể giữ bảng SQL mới khi chạy mã cũ; không khôi phục dữ liệu người chơi.

### Xem trước vật phẩm và kho chọn đồ

Trong Boss → chọn boss → Vật phẩm rơi, danh sách **Vật phẩm có thể rơi** gồm ảnh/tên/ID của đồ gốc ở chế độ original/add và các dòng tùy chỉnh có tỷ lệ lớn hơn 0 ở chế độ add/replace. Chế độ off không hiện món rơi; thay đổi chưa lưu được ghi rõ. Các món gốc là khả năng theo điều kiện/nhánh ngẫu nhiên, không bảo đảm cùng rơi mỗi lượt. Bảng này không sửa logic thưởng trong game.

Kho vật phẩm mở/thu gọn, xem 24 món/trang, tìm tên/ID, hiện ảnh thật và bấm thêm vào bản nháp. Enter trong ô tìm kiếm chỉ tìm, không tự lưu cấu hình. Bảng xem trước tùy chỉnh có %, khoảng số lượng và chỉ số đã đặt.

Danh mục đọc từ `web/boss_native_drops.json`, tạo lại bằng `node scripts/build-boss-loot-catalog.cjs` sau khi sửa source thưởng hoặc ID Trung Thu. Script đọc source, không chạy hàm thưởng; các biểu thức chưa giải được được đánh dấu chưa đầy đủ. Các ID sự kiện Trung Thu dùng `config/trung-thu-2026.properties`; tỷ lệ/điều kiện vẫn theo runtime. `node test/BossNativeDropsTest.cjs` đối chiếu các boss phổ biến, nhánh ngẫu nhiên, hàm phụ và boss sự kiện. Đã đối chiếu đủ khóa của 93 lớp boss đang có heartbeat; API tên/icon lấy trực tiếp từ item_template.

Bản dự án đang dùng: `D:\NroFreeSetup\Work\SourceGame-441D49A9989D`. Bản sao lưu thay đổi web lần này: `D:\NroFreeSetup\Backups\boss-loot-preview-20261015\live-before`. Khi hoàn tác giao diện, khôi phục các file theo bản sao lưu; không thay dữ liệu người chơi hoặc bảng rơi đã lưu.

---

<a id="doc-shop-points-and-boss-timing"></a>
## Shop điểm và thời gian Boss

## Giá điểm và thời gian Boss

### Cửa hàng đổi điểm Quy Lão

Tab 59 dùng `item_shop.cost` cho cả gói tin hiển thị và giá mua, kể cả cải trang mới thêm. Ví dụ CT Himmel (1632) giá 10.000 điểm; giá của vật phẩm cũ cũng không còn bị gán cứng theo ID. Sau khi sửa shop trên web, cập nhật shop và mở lại shop trong game để nhận dữ liệu mới.

`PointShop` kiểm tra đủ điểm, thêm vật phẩm và lưu `items_bag`, `data_inventory`, `event_point` trong một giao dịch. Túi đầy hoặc lỗi lưu giữ nguyên điểm và hành trang. Giá âm bị từ chối; giá 0 được quản trị viên cấu hình rõ ràng vẫn là miễn phí. Không dùng ngọc/vàng thay cho điểm.

### Thời gian Boss trên web

- **Hồi cấu hình**: số giây nghỉ trước chu kỳ tiếp theo. Sửa thời gian hồi tác động cả chu kỳ đang nghỉ; boss theo chuỗi, triệu hồi trả phí hoặc lịch riêng vẫn phải thỏa điều kiện gốc.
- **Xuất hiện tiếp**: đếm ngược từ trạng thái thực tế của server. Hết thời gian nhưng map chưa sẵn sàng hiển thị “Đang chờ vào map”; không tự suy diễn thời gian cho boss có điều kiện riêng.
- **Đã tồn tại**: tính từ lần vào map đầu tiên của lượt boss, giữ qua các dạng cho đến khi về trạng thái nghỉ.
- **Thời gian tồn tại**: để trống giữ cơ chế gốc; 0 bỏ giới hạn tự rời map; số dương là giới hạn tính từ lúc vào map. Giảm giới hạn có thể khiến boss đang sống rời map ngay. Hết hạn không phát thưởng hạ boss và kết thúc lượt các dạng trong cùng boss. Vẫn gọi hook rời map để dọn trạng thái riêng; nếu hook gốc gọi một boss khác (như Broly → Super Broly), cơ chế đó vẫn chạy.
- **Hẹn xuất hiện sau**: lịch một lần cho một boss độc lập đang vắng mặt. 0 gọi ngay; số dương ưu tiên hơn thời gian nghỉ tự nhiên của instance được hẹn. Lịch nằm trong RAM, mất khi server khởi động lại; tắt sự kiện hủy lịch. Không gọi miễn phí boss yêu cầu vật phẩm triệu hồi.

Các giá trị chỉnh nhận trong khoảng 3 giây, giới hạn 0–604.800 giây. Đồng hồ hiển thị cập nhật mỗi giây, dữ liệu server nhận mỗi 3 giây và trang làm mới mỗi 5 giây. Khi offline không hiển thị đếm ngược như dữ liệu đang sống. Với nhiều instance của cùng class, hiển thị mốc xuất hiện/kết thúc gần nhất và tuổi lượt lâu nhất; các vị trí thực tế vẫn được giữ.

Schema bổ sung tương thích dữ liệu cũ: `boss_web_config.lifetime_seconds` (NULL = cơ chế gốc), `timing_state` và `boss_web_command.delay_seconds`. Không thay giá shop hay cấu hình Boss đang có khi nâng cấp.

### Kiểm tra và hoàn tác

- `test/PointShopTest.java`: gói tin shop thật, tuyến mua thật, CT mới, đổi giá CT cũ, thiếu điểm, túi đầy, lỗi SQL và giá 0/âm; chỉ database QA.
- `test/BossTimingTest.java`: trạng thái Boss thật, chỉnh hồi, lịch hẹn, vào map, hết hạn, hook dọn trạng thái và sự kiện tắt.
- `test/BossGroupingTest.php`: giữ lọc quái/nhóm trùng và thứ tự nhiệm vụ.
- API QA: lưu giới hạn dương/0/NULL, hẹn xuất hiện, từ chối số âm/vượt mức.
- `test/VerifyRuntimeJar.java`: kiểm tra toàn bộ class trong JAR và `ConstIgnoreName`.

Bản sao lưu lần triển khai: `C:\Downloads\NroFreeSetup\Backups\shop-points-boss-times-20261015`. Có JAR trước sửa, cấu hình shop/Boss trước sửa, web cũ và nhật ký triển khai. Hoàn tác JAR chỉ sau bảo trì/lưu và JVM đã dừng; khôi phục các file trong `live-web-before` về đường dẫn tương ứng ở `C:\xampp\htdocs`. Các cột SQL bổ sung có thể giữ nguyên khi chạy bản cũ. Không khôi phục nguyên bảng người chơi để hoàn tác bản mã này.

---

<a id="doc-himmel-stats"></a>
## Himmel và cộng dồn sát thương

## CT Himmel và option 156

Bản sửa thay cách tính cũ của option 156 bằng cơ chế cộng dồn theo đúng tên trong item_option_template: mỗi đòn đấm cơ bản gây sát thương tăng 1% sức đánh, tối đa theo param. Áp dụng cho Dragon, Demon, Galick, gồm đánh quái và đánh người chơi/boss. Đòn hụt hoặc sát thương bằng 0 không tăng. Phần cộng dồn có hiệu lực sau đòn trúng, cho đòn kế tiếp.

Dùng chiêu khác xóa chuỗi trước khi tính sát thương của chiêu đó; đổi món cung cấp hiệu ứng, đổi param, chết hoặc đăng nhập lại bắt đầu ở 0. Nếu nhiều nguồn có option này thì lấy trần cao nhất, không cộng trần. Radar đang dùng cũng tuân thủ điều kiện kích hoạt option như bộ tính chỉ số. Trạng thái chuỗi nằm riêng trên từng NPoint, không lưu SQL và không thay dữ liệu vật phẩm.

Option 156 không còn trừ 50% sức đánh hoặc cộng SM/TN/vàng trái mô tả. Cute 117/226 chỉ được áp dụng một lần thay vì hai lần trong setDame. Các cơ chế giảm sức đánh khác như giáp luyện tập vẫn giữ phép tính riêng.

Bộ CT Himmel ở shop điểm có SD25%, HP25%, chí mạng20%, Cute25% và trần cộng dồn25%. Không có option KI nên không tự thêm KI. Chỉ số hiển thị và gói tin -42 được kiểm tra qua tuyến mặc/tháo thực tế.

### Kiểm tra

`test/HimmelPunchComboTest.java` chạy với database QA, cần class test/TrungThuRuntimeTest.java cùng classpath JAR và lib/*. Lấy cấu hình từ thư mục build/panel-qa. Có thể truyền một file JSON snapshot local gồm gender/data_point/items_body để kiểm lại trang bị cụ thể; snapshot người chơi không đưa vào Git. Bài kiểm tra gồm chỉ số thực, packet client, đòn trúng/hụt PvE/PvP, đổi chiêu, trần khác nhau, nhiều nguồn, đổi đồ, chết và phiên mới.

Đã qua 80 kiểm tra cùng snapshot người chơi, hồi quy PointShop 14, TrungThuCakeBuff 33, BossDrops 19. JAR xác nhận đủ ConstIgnoreName và 638 class ứng dụng nạp được trên Java 17.

### Triển khai và hoàn tác

Game đang chạy ở D:\NroFreeSetup\Work\SourceGame-441D49A9989D. Bản trước được giữ ở D:\NroFreeSetup\Backups\himmel-stats-20261015\launcher-before.jar; deployment.json lưu SHA256 trước/sau và log kiểm thử ở cùng thư mục. Đã bảo trì/lưu 15 giây, thay JAR khi JVM dừng và launcher tự chạy lại sau 10 giây. Không chỉnh vật phẩm hay tài khoản trong database máy chính.

Để hoàn tác: bảo trì/lưu, chờ JVM dừng, khôi phục launcher-before.jar vào Teamobi2026-panel-launcher.jar trước lần chạy kế tiếp. Không khôi phục database người chơi.

---

<a id="doc-toribot-management"></a>
## Quản lý Tori Bot

## Quản lý Tori Bot

Menu **Tori Bot** trên panel quản lý mùa VIP và 4 gói đang có trong source.

- Bật/tắt bán, thời gian bắt đầu/kết thúc theo giờ Việt Nam, mã mùa và giới hạn mua mỗi nhân vật.
- Từng gói chỉnh giá VNĐ, số ngày VIP, EXP thưởng thêm, tặng đệ tử thường khi chưa có.
- Thêm/xóa vật phẩm bằng kho ảnh/tên/ID, chỉnh số lượng, option và hạn dùng (option 93).
- Lịch sử cấu hình có hoàn tác lần thay đổi gần nhất. Mỗi lần lưu tạo revision mới, tránh ghi đè giữa hai phiên. Game nhận cấu hình qua PanelBridge mỗi 3 giây; web báo xác nhận.
- Lịch sử mua ghi nhân vật, gói, giá và toàn bộ cấu hình thưởng tại thời điểm mua. Hoàn tác cấu hình không thu hồi giao dịch đã thực hiện.

### Quy tắc

Giữ giá/phần thưởng thực tế của 4 gói cũ làm mặc định. EXP mặc định +300% giữ đúng mức code cũ (phần nền X4), các hệ số khác tính riêng. Giá trừ **account.vnd**, không phải vàng hay ngọc. VIP 4 có trứng đệ tử trong phần thưởng; không tự tạo đệ tử Mabu.

Bật/tắt chỉ điều khiển bán. Ngoài lịch mùa không mua được và không hưởng EXP của mùa. Thời hạn VIP nối từ ngày hết hạn hiện tại hoặc hôm nay, tối đa cuối mùa. Thời hạn vật phẩm độc lập với hạn VIP. Khi mua gói thấp hơn, cấp VIP và EXP đang có giữ mức cao hơn. Không thay đổi cơ chế VIP tài khoản khác.

Đổi **mã mùa** đặt lại cấp VIP, hạn và lượt mua của mùa trước khi nhân vật mở Tori Bot hoặc đăng nhập; EXP mùa cũ ngừng ngay. Giữ nguyên mã mùa khi chỉ điều chỉnh lịch hoặc phần thưởng. Dữ liệu cũ chưa có mã mùa được nhận vào mùa hiện tại, giữ số lượt cũ.

Mua yêu cầu xác nhận với đúng revision; đóng menu không mua. Khóa dòng cấu hình, tài khoản và nhân vật trong transaction InnoDB. VNĐ, hành trang, VIP, đệ tử mới và lịch sử mua cùng commit; lỗi SQL/thiếu túi không trừ tiền hay trao thưởng. Từ chối phiên nhân vật có số lượt cũ. Đệ tử đang có giữ nguyên.

### Kiểm tra

- `test/ToriVipTest.java`: 52 kiểm tra trên DB QA riêng, 4 gói, phần thưởng, đệ tử, giới hạn, lỗi SQL/đầy túi/thiếu tiền, dữ liệu cũ, đóng menu, revision thay đổi và mùa mới.
- `test/ToriVipApiTest.ps1`: tìm đồ thật, phân trang, lưu giá/hạn/EXP, từ chối revision cũ và dữ liệu sai, hoàn tác.
- Kiểm tra lại PointShop (14), đổi VNĐ (25), bánh Trung Thu (33), Himmel (79).
- Biên dịch toàn bộ source Java 17 và kiểm tra các lớp trong JAR đóng gói; không đóng gói lớp test.

Chạy Java test từ `build/panel-qa` với cấu hình QA và truyền đường dẫn repo cho ToriVipTest. API test dùng PHP QA localhost:8766, có chặn nhầm DB thật. Không chạy test bằng Config.properties của game thật.

### Khôi phục bản trước

Backup triển khai tại `D:/NroFreeSetup/Backups/toribot-20261016`: JAR trước, sidebar trước và trạng thái bảng Tori trước. Muốn lùi runtime: bảo trì lưu dữ liệu, đổi JAR trong khoảng chờ khởi động, khôi phục sidebar và gỡ các file web mới theo manifest. Giữ bảng lịch sử mua để đối soát; không hoàn tác dữ liệu người chơi hay VNĐ sau khi có giao dịch. Cấu hình thường dùng nút Hoàn tác trên web.

---

<a id="doc-noel-logic"></a>
## Sự kiện Noel

## Sự kiện Noel

### Luồng hoạt động

1. Bật **Giáng sinh** trên panel. Có 30 Ông già Noel, tối đa một ông trong mỗi khu. Đây là nhân vật phát quà, không phải boss để giết; không nhận sát thương.
2. Đến Santa ở Đảo Kamê, Đảo Guru hoặc Vách núi đen → **Giáng sinh** → **Mua tất Giáng sinh**. Shop riêng bán vật phẩm 649, giá khởi đầu 20 ngọc như shop Bulma sự kiện cũ. Giá chỉnh trên panel được giữ qua restart.
3. Đến gần Ông già Noel trong phạm vi 50. Sau mỗi 60 giây, ông xét ba lượt thả độc lập với xác suất 1/3, 1/5 và 1/7; mỗi lượt thành công thả một hộp 648. Không đảm bảo phút nào cũng có quà.
4. Nhặt mỗi hộp tiêu hao một tất. Túi đầy hoặc không đủ tất thì giữ nguyên cả tất và hộp trên đất. Quà công khai, người nhặt thành công trước nhận; không thể nhặt lại cùng một hộp.
5. Mở hộp trong túi để nhận quà. Hộp đã nhận vẫn dùng được sau khi tắt EV.

Ông già Noel ở map 100–300 giây, sau đó nghỉ theo cấu hình boss (gốc 60 giây). Gọi boss từ web cũng khởi tạo đầy đủ thời gian. Santa thường và các cửa hàng thường vẫn hoạt động khi tắt Noel; mục Giáng sinh và shop riêng chặn thao tác cũ khi EV tắt.

### Phần thưởng hộp 648 đang có trong source

- 5/90 cơ hội nhận 10–20 ngọc xanh.
- Nếu không trúng ngọc, chọn đều một trong bảy nhóm: sao pha lê 441–445; đá nâng cấp 381–384; ngọc rồng 17–20; ngọc rồng băng 925–931; mảnh thiên sứ 1066–1070; kẹo Giáng sinh 533; capsule 380.
- Sao pha lê, đá nâng cấp và mảnh thiên sứ nhận 1–5 vật phẩm, các nhóm còn lại nhận một. Giữ nguyên nhóm và chỉ số sao từ source cũ.
- Không thêm nón, cây thông, tuần lộc hay cải trang vào hộp 648: code cũ có nhánh xử lý chúng nhưng danh sách quay không hề chọn chúng. Gói quà 1170 là vật phẩm khác với danh sách thưởng khác; không tự đổi danh sách đó thành quà Noel.
- Tràn ngọc hoặc không đủ chỗ nhận thì không mất hộp. Chỉ tiêu hao hộp sau khi cấp phần thưởng thành công.

### Các lỗi đã sửa

- Kiểm tra trùng boss bằng quần thể sai và hai vòng chọn khu không xét đồng thời các điều kiện.
- Nhánh gọi boss thủ công không khởi tạo thời gian rời map.
- Tạo ItemMap trước khi kiểm tra xác suất khiến vật phẩm thất bại vẫn nằm trong danh sách map.
- Ông già Noel không tiến lại khi người chơi cách 50–500, hoặc chọn mục tiêu chết/thú thay cho người chơi sống.
- Pickup đánh dấu quá muộn, trừ tất trong phần gửi gói tin; tách riêng để cấp quà/trừ tất/đánh dấu nhận diễn ra trước khi gửi thông báo.
- Hộp quà cũ trừ hộp trước khi thêm thưởng và cộng ngọc có thể vượt giới hạn int.

Các nguồn chính: OngGiaNoel.java, ChristmasGifts.java, Santa.java, Zone.java, UseItem.java, ShopService.java và web/noel_schema.sql. Schema chạy trước khi nạp shop, chỉ thêm dữ liệu còn thiếu.

---

<a id="doc-trung-thu-2026"></a>
## Hướng dẫn Trung Thu

## Trung Thu 2026

Đã nạp trên máy chính ngày 15/10/2026. Giữ công tắc Trung Thu đang bật và các sự kiện khác theo trạng thái có sẵn. Xác nhận heartbeat game, bốn định nghĩa boss mới, Gogeta/Omega đang ở Siêu Thị; Khỉ/Thỏ chưa gọi không tự xuất hiện. JAR SHA256: `4EB31E96DF88C81B445B5529859DFA541B5D03FE0579606DD53E66EA66F0C74E`.

Bản sao lưu trước khi nạp: `C:\Downloads\NroFreeSetup\Backups\trung-thu-20261015-131337`. Server tiếp tục dùng cùng cửa sổ `run.bat`, không mở thêm terminal.

### Cách chơi

1. Web → Sự kiện → Trung Thu 2026 để bật/tắt thủ công. Không chạy theo lịch cũ. Bật **Vật phẩm EV từ quái** nếu muốn rơi nguyên liệu làm bánh.
2. Gặp NPC **Trung thu** tại Đảo Kame hoặc Siêu Thị. Các NPC này tự ẩn khi tắt sự kiện.
3. Mua **Ánh trăng tròn** 10 ngọc rồi dùng: gọi một Khỉ tại map ngẫu nhiên trong Thung lũng đen (37), Vách núi đen (20), Thành phố Vegeta (19). Thông báo cho biết map/khu. Chỉ có một Khỉ được gọi tại một thời điểm; gọi khi đã có Khỉ không mất vật phẩm. Khỉ chỉ nhận đấm/chưởng cơ bản của người chơi, miễn khống chế. Hạ Khỉ rơi 1–3 đuôi, nhận X3 EXP: Namec 50 phút, Trái Đất 40 phút, Xayda 30 phút; thành viên bang kể cả offline nhận 20 phút.
4. **Thỏ ngọc** 10 triệu vàng gọi Thỏ Đại Ca tại Siêu Thị (84), chưa mở map Santa. Đến gần trong phạm vi 40 bị hóa cà rốt 5 phút, giảm 15% sát thương. Cải trang Thỏ đỏ Chi Chi/Thỏ đen Android 18 miễn hóa cà rốt. Hạ Thỏ rơi 1–3 cà rốt.
5. Đổi 99 đuôi hoặc 99 cà rốt tại NPC Trung thu; treo mỗi lần 1 **Lồng đèn treo** để nhận quà. Thưởng theo bảng đã duyệt. Treo đèn dùng menu NPC hiện có.
6. Chọn phe Gogeta hoặc Omega; NPC cấp một **Kiếm gỗ** không giao dịch. Giữ kiếm trong túi và cờ đúng phe, đánh boss đối lập tại Siêu Thị. Có 20% nhận cải trang đối lập hạn 3/5/7/15 ngày; lượt riêng 1% ra thú cưỡi, trong đó 1% vĩnh viễn, còn lại 7 ngày. Hai boss hồi sau 15 phút, tồn tại tối đa 15 phút mỗi lượt.
7. Làm bánh bằng trứng/gà (mỗi nhóm 2% ở map thường) và bột/đậu (mỗi nhóm 5% ở doanh trại, kho báu, đường rắn, khí gas). Đệ tử đánh quái cũng có thể rơi nguyên liệu cho chủ. Mộc nhân/bù nhìn/big boss không rơi nguyên liệu qua nhánh này.
8. Mỗi công thức: 10 bột + 10 đậu + 2 trứng, thêm 1 gà và 20 triệu vàng để làm bánh Gà quay; trả 20 ngọc thay vàng được cùng bánh Gà quay và 30% thêm bánh Thập Cẩm. Bánh Hạt sen dùng 1 hạt sen thay gà, trả 20 ngọc.
9. Đổi 20 Gà quay → Thỏ xám; 10 Thập Cẩm → Thỏ trắng; 10 Hạt sen → Mèo đen đuôi vàng. Hạn 3/5/7/15 ngày/vĩnh viễn theo tỷ lệ 40/30/20/9/1%.
10. Bò Mộng có thêm mức **Siêu khó**. Lần trả nhiệm vụ Khó đầu tiên mỗi ngày nhận 1 đèn + 1 hạt sen; Siêu khó nhận 2 đèn + 1 hạt sen. Mỗi mức nhận riêng một lần/ngày, ngoài thưởng nhiệm vụ thường.
11. Quy Lão → Nói chuyện → Về khu vực bang. Đeo lồng đèn/cờ lồng đèn, gặp Giu-ma Đầu Bò, bang chủ bắt đầu Rước đèn hoặc Điểm danh Trung Thu. Danh sách chốt theo người đang online, ở cùng khu và đeo đèn; từng người xác nhận. Đủ người thì toàn bang, kể cả offline, nhận X2 EXP 1 giờ. Mỗi loại hoạt động một lần/ngày theo giờ Việt Nam. Thành viên bỏ khu/tháo đèn làm hủy lượt; lượt chờ hết sau 5 phút. Bang chủ có thể bắt đầu lại nếu chưa nhận thưởng.

### Vật phẩm và giá bổ sung

- Hộp thường dùng **Giỏ bánh trung thu (1316)** giá 20 ngọc; hộp chay dùng **Giỏ bánh trung thu độc đáo (1317)** giá 10 triệu vàng. Bảng thưởng riêng, không dùng hộp đua top 1701. Hộp chỉ mở khi sự kiện bật, tắt vẫn giữ vật phẩm.
- Lồng Đèn 2 Lon dùng **Lồng đèn lon (1047)**. Ván Bay Cung Trăng dùng **Lồng đèn cá chép bay (735)**. Cải trang Omega dùng **CT rồng (1685)**. Giữ tên/hình thật, không đổi template dùng chung.
- Lồng đèn treo giá 50 ngọc. Có thể mua **Lồng đèn Ông Sao (467)** đeo để rước đèn, giá cấu hình mặc định 50 ngọc. Cải trang thỏ dùng giá cấu hình mặc định 100 ngọc/món; không thêm chỉ số chiến đấu tùy tiện.
- Bột mì, đậu xanh và hạt sen chỉ ký gửi bằng vàng. Người bán nhận vàng sau phí 10% hiện có, không nhận thỏi vàng thay cho vàng.
- Hộp VIP, mọi nội dung đua top và khuyến mãi X3 không triển khai.

### Cấu hình và lưu dữ liệu

`config/trung-thu-2026.properties` chứa ID, giá, công thức, bảng thưởng, tỷ lệ, thời gian, hình boss và danh sách lồng đèn hợp lệ. Thay cấu hình cần khởi động lại; công tắc web áp dụng khi game đang chạy.

`web/trung_thu_schema.sql` tạo hai bảng InnoDB: `trung_thu_2026_state` (thời điểm hết hạn buff/hóa cà rốt, phe) và `trung_thu_2026_daily` (biên nhận thưởng ngày). Tự tạo trước khi nạp dữ liệu game. Không có bảng top.

Buff lấy hạn dài hơn khi nhận lại, không cộng nối vô hạn. Thời gian offline vẫn trôi. Buff Trung Thu lấy hệ số cao nhất (X3 hoặc X2); không cộng bùa trí tuệ, buff đuôi khỉ cũ, X2 kho báu hoặc bùa Santa liên quan. Các hệ số VIP/trang bị/map khác giữ cách tính cũ. Tắt EV tạm ngừng tác dụng, không dừng đồng hồ hết hạn.

Giao dịch khóa theo người chơi, có chống bấm liên tục; lưu túi và tiền trong cùng transaction trước khi gửi gói tin. Hết chỗ, vượt giới hạn tiền hoặc lỗi lưu sẽ hoàn nguyên. Trả nhiệm vụ Trung Thu lưu cả trạng thái nhiệm vụ và biên nhận trong cùng transaction. Autosave dùng cùng khóa để không ghi đè một giao dịch đang thực hiện.

### Kiểm tra đã chạy

- 69 kiểm tra tích hợp Trung Thu trên DB QA: tỷ lệ chính xác trên 10.000 giá trị quay, đổi bánh/mở hộp/lỗi lưu, buff và đăng nhập lại, NPC bán đồ, boss/map/khu, miễn khống chế, phe/kiếm, thưởng nhiệm vụ và bang, ký gửi, nguồn nguyên liệu, tắt EV.
- 13 kiểm tra an toàn BagExchange, 53 kiểm tra template/ảnh, hồi quy Noel 39 và Giỗ Tổ 73 kiểm tra; kiểm tra ẩn/hiện NPC và nhóm boss trên web đều qua.
- JAR kiểm tra đủ `ConstIgnoreName`; 617 application class nạp được, không khởi động server trong kiểm tra đóng gói.

Không giả lập thao tác bằng client game thật trong các bài QA; sau khi nạp cần vào NPC Trung thu để xem hình/menu trên client đang sử dụng.

### Hoàn tác

Trước khi nạp: sao lưu JAR, config, hai file PHP của web và SQL các bảng người chơi/bang/ký gửi/cấu hình sự kiện vào thư mục `Backups/trung-thu-20261015-*`. Muốn quay lại, bảo trì lưu dữ liệu, phục hồi JAR và file web trước đó rồi khởi động lại. Hai bảng mới có thể giữ nguyên; bản cũ không dùng chúng. Không phục hồi toàn DB sau khi người chơi đã tiếp tục chơi nếu chưa đối chiếu dữ liệu phát sinh.

### Cửa hàng chuẩn và chỉnh tỷ lệ rơi trên web

NPC Trung Thu → Cửa hàng mở cửa sổ shop giống các NPC thường: bốn tab **Triệu hồi**, **Đồ sự kiện**, **Trang phục**, **Bánh tăng chỉ số**, mười một món có ảnh và giá vàng/ngọc. Shop có tag `TRUNG_THU_2026`, NPC41. Có thể quản lý giá và các món bán trong mục Cửa hàng trên web. Giá từ file cấu hình chỉ dùng khi tạo món lần đầu; khởi động lại không ghi đè giá admin đã chỉnh. Các mục làm bánh, đổi pet, đổi nguyên liệu và chọn phe vẫn ở menu NPC. Tắt Trung Thu thì cửa hàng đang mở cũng không mua được. Mua hàng tiếp tục dùng giao dịch lưu tiền/vật phẩm cùng lúc, không thu tiền khi đầy túi hoặc lưu thất bại.

Web → Rơi đồ → **Vật phẩm Trung Thu** có 10 dòng chỉnh % độc lập, mặc định thu gọn:

- Trứng vịt muối / gà quay nguyên con: mỗi món2% ở quái map thường và map riêng.
- Bột mì / đậu xanh: mỗi món5% ở Doanh trại, Kho báu, Con đường rắn độc, Khí gas hủy diệt.
- Đuôi khỉ / củ cà rốt:100% từ đúng boss triệu hồi; vẫn giữ1–3 món mỗi lần rơi.
- Cải trang Gogeta / cải trang thay thế Omega:20%, chỉnh riêng từng boss.
- Ván bay thay thế:1%, chỉnh riêng Gogeta/Omega. Khi đã rơi, vẫn giữ1% vĩnh viễn,99% hạn7ngày.

Các % trên là tỷ lệ gốc khi hệ số nhóm và toàn server đều x1. Có thể nhập % riêng thay hệ số, hoặc bấm Mặc định để trở lại cách tính hệ số. x0 vẫn tắt rơi. Phải bật Trung Thu và Vật phẩm EV. Không đổi điều kiện kiếm gỗ/cờ phe, nguồn nhận hạt sen từ nhiệm vụ hay bảng thưởng mở hộp. Game nhận thay đổi tỷ lệ trong khoảng3giây.

### Bánh tăng chỉ số tạm thời

| Bánh | Tăng đồng thời SD / HP / KI | Thời gian mỗi bánh | Giá mặc định |
| --- | --- | --- | --- |
| Bánh Trung Thu 1 trứng (465) | 20% | 10 phút | 20 ngọc xanh |
| Bánh Trung Thu 2 trứng (466) | 25% | 10 phút | 30 ngọc xanh |
| Bánh Trung Thu Đặc Biệt (472) | 30% | 10 phút | 40 ngọc xanh |

Chỉ có một cấp bánh hiệu lực, không cộng dồn phần trăm. Dùng cùng cấp cộng 10 phút thời gian; dùng cấp cao hơn thay buff cũ bằng 10 phút ở cấp mới. Khi đang có cấp cao hơn, bánh yếu hơn không bị tiêu hao và không gia hạn cấp mạnh. Tăng HP/KI tối đa, không tự hồi đầy. Hết hạn sẽ đưa HP/KI hiện tại về trong giới hạn tối đa mới.

Bảng `trung_thu_2026_cake` lưu cấp và mốc hết hạn tuyệt đối. Đăng xuất, khởi động lại hoặc tắt event thì thời gian vẫn trôi. Tắt Trung Thu dừng hiệu lực và chặn dùng bánh; bật lại tiếp tục nếu chưa hết hạn. Buff và trừ bánh lưu cùng transaction, lỗi lưu không mất bánh. Có icon và thời gian còn lại trên client. Dữ liệu item được nâng phiên bản 11 để client cập nhật mô tả 10 phút.

Giá mặc định chỉ dùng khi thêm món mới. Giá/chỉ số shop đã quản trị viên chỉnh không bị ghi đè khi khởi động. Chỉ số có hiệu lực của ba bánh được quy định trong `config/trung-thu-2026.properties`; option trên món là nội dung hiển thị của shop, không thể dùng sửa option để tạo buff vượt cấp đã cấu hình.

---

<a id="doc-trung-thu-2026-assets"></a>
## Tài nguyên Trung Thu

## Tài nguyên thay thế Trung Thu 2026

Ngày 15/10/2026, người dùng cho phép dùng thứ tương đương vì không tìm được ID/hình chính xác. Quyết định này thay yêu cầu chờ tài nguyên trước đó. Các ID dưới đây đã được đọc và đối chiếu từ DB game hiện có; không tự cấp ID hoặc đổi tên template dùng chung.

| Vai trò theo đặc tả | Tài nguyên thực tế | ID | Cách dùng |
|---|---|---:|---|
| Lồng Đèn 2 Lon | Lồng đèn lon | 1047 | Phụ kiện lưng loại 11, part 80; giữ xác suất và hạn đã duyệt |
| Ván Bay Cung Trăng | Lồng đèn cá chép | 735 | Thú cưỡi loại 23, part 3, bay không tốn KI; có hoạt ảnh `mount_3_0` và `mount_3_1` |
| Cải trang phe Omega | CT rồng | 1685 | Cải trang loại 5, bộ phận 1569/1570/1571; không hiển thị tên giả là Omega |
| Bánh Gà Quay hảo hạng | Bánh trung thu Gà quay | 890 | Dùng cùng bánh thường; công thức trả 20 ngọc vẫn có 30% thêm bánh thập cẩm, không tạo biến thể ID mới |
| Hộp Bánh Trung Thu 2026 | Giỏ bánh trung thu | 1316 | Bảng thưởng hộp thường; giá 20 ngọc |
| Hộp Bánh Trung Thu Chay 2026 | Giỏ bánh trung thu độc đáo | 1317 | Bảng thưởng hộp chay; giá 10 triệu vàng; không trộn với hộp thường |
| NPC Cây Lồng Đèn | Menu treo đèn tại NPC Trung thu | 41 | Thêm chức năng vào NPC hiện có; không dùng cây thông Noel làm cây lồng đèn |

Cải trang Gogeta tiếp tục dùng 1697. Thỏ Đại Ca chỉ ở Siêu Thị (84). Hộp VIP vẫn chưa triển khai theo đề xuất đã duyệt. Không dùng hộp 1701 vì là vật phẩm đua top cũ.

### Ảnh hưởng và giới hạn

- Giữ nguyên tỷ lệ thưởng, thời hạn và giá đã duyệt; chỉ thay vật phẩm nhận được bằng tài nguyên tương đương nêu trên.
- Không chạy SQL sửa template, không đổi ảnh hoặc tên vật phẩm của người chơi đang có.
- Hai công thức bánh gà quay tạo cùng ID 890 nên dùng chung khi đổi pet thỏ xám. Phần phân biệt công thức trả ngọc là 30% thưởng bánh thập cẩm.
- Giỏ bánh 1316 và 1317 đã được nối vào hai bảng thưởng riêng qua `TrungThuCommerce.useBox`; không thay đổi nhánh hộp cũ khác.
- Kiểm tra tài nguyên đầu vào đã hoàn tất. Trạng thái triển khai và hướng dẫn hiện tại xem `README.md#doc-trung-thu-2026` và `README.md#doc-trung-thu-2026-implementation`.

### Kiểm tra

`test/TrungThuAssetsTest.php` đọc config đã chốt và DB, xác minh đúng loại item, bộ phận cải trang, hoạt ảnh thú cưỡi, các file icon và việc tách riêng hai hộp. Test chỉ đọc DB; không khởi động server hay sửa tài khoản.

---

<a id="doc-trung-thu-2026-reward-proposal"></a>
## Bảng thưởng Trung Thu đã duyệt

## Bảng thưởng Trung Thu — đã duyệt

Người dùng đã duyệt toàn bộ bảng đề xuất ngày 15/10/2026, gồm phần thưởng, tỷ lệ rơi nguyên liệu và HP/sát thương boss. Sau đó người dùng cho phép dùng tài nguyên tương đương. Các tên theo đặc tả bên dưới được ánh xạ sang vật phẩm thực tế theo `README.md#doc-trung-thu-2026-assets`, giữ nguyên xác suất và thời hạn. Đã được triển khai vào source/config và kiểm tra trên QA; hướng dẫn và trạng thái nạp game xem `README.md#doc-trung-thu-2026`.

### 99 đuôi khỉ

| Xác suất | Phần thưởng |
|---:|---|
| 40% | 10 đá nâng cấp, chọn một loại trong220–224 |
| 30% | 3 sao pha lê, chọn một loại trong441–447 |
| 20% | 50 ngọc xanh |
| 10% | 100 hồng ngọc |

### 99 củ cà rốt

| Xác suất | Phần thưởng |
|---:|---|
| 50% | 5 đá nâng cấp, chọn một loại trong220–224 |
| 30% | 2 sao pha lê, chọn một loại trong441–447 |
| 20% | 50 hồng ngọc |

### Một lượt treo lồng đèn

| Xác suất | Phần thưởng |
|---:|---|
| 45% | 2 triệu vàng |
| 25% | 100 hồng ngọc |
| 20% | 10 đá nâng cấp, chọn một loại |
| 9% | 5 sao pha lê, chọn một loại |
| 0,9% | Lồng Đèn 2 Lon, hạn7 ngày |
| 0,1% | Lồng Đèn 2 Lon vĩnh viễn |

### Hộp Bánh Trung Thu2026 — 20 ngọc

| Xác suất | Phần thưởng |
|---:|---|
| 30% | 2 triệu vàng |
| 20% | 50 hồng ngọc |
| 29% | 5 đá nâng cấp, chọn một loại |
| 20% | 2 sao pha lê, chọn một loại |
| 0,9% | Ván Bay Cung Trăng, hạn7 ngày |
| 0,1% | Ván Bay Cung Trăng vĩnh viễn |

### Hộp Bánh Trung Thu Chay2026 — 10 triệu vàng

| Xác suất | Phần thưởng |
|---:|---|
| 30% | 1 triệu vàng |
| 20% | 20 hồng ngọc |
| 29% | 3 đá nâng cấp, chọn một loại |
| 20% | 1 sao pha lê, chọn một loại |
| 0,99% | Ván Bay Cung Trăng, hạn3 ngày |
| 0,01% | Ván Bay Cung Trăng vĩnh viễn |

Hộp VIP2026 chưa có giá/công thức cụ thể: đề nghị để tắt mục này cho đến khi được chốt, thay vì tự thu tiền hoặc phát phần thưởng.

### Các tỷ lệ khác cần duyệt

- Khỉ rơi1–3 đuôi và Thỏ rơi1–3 cà rốt mỗi lần hạ; không phát lặp lại khi nhận nhiều gói tấn công kết liễu.
- Trứng/gà từ quái thường: 2% mỗi nhóm, mỗi lượt1 món. Bột/đậu từ quái phó bản: 5% mỗi nhóm, mỗi lượt1 món. Không áp dụng lên mộc nhân hoặc boss.
- Nhiệm vụ khó/siêu khó hoàn thành: 1 hạt sen và1/2 lồng đèn như đặc tả; chỉ phát cùng lần trả nhiệm vụ thành công.
- Pet đổi bằng bánh: hạn3/5/7/15 ngày/vĩnh viễn tương ứng40%/30%/20%/9%/1%.
- Hạ boss phe đối lập: 20% ra cải trang của phe đó, hạn3/5/7/15 ngày chọn đều. Ván bay: lượt riêng1%; nếu trúng thì99% hạn7 ngày và1% vĩnh viễn.
- Chỉ số của pet/cải trang/lồng đèn/ván bay cần dùng cấu hình được duyệt, không tự cộng chỉ số vượt template/shop hiện có.
- HP/sát thương Khỉ: dùng thông số source Khỉ đột hiện tại100 triệu/100 nghìn làm giá trị cấu hình ban đầu. Thỏ dùng50 triệu/1 nghìn theo boss sự kiện sẵn có. Gogeta/Omega đề nghị100 triệu/100 nghìn, cần duyệt cùng bảng thưởng.

Không có điểm top, mốc đổi quà top hoặc bảng xếp hạng trong bất kỳ bảng thưởng nào.

---

<a id="doc-trung-thu-2026-implementation"></a>
## Triển khai Trung Thu

## Trung Thu 2026 — triển khai

Đã tích hợp source, kiểm tra trên DB QA và nạp máy chính ngày 15/10/2026. Hướng dẫn chơi, giá, giả định, bằng chứng nạp và hoàn tác nằm trong `README.md#doc-trung-thu-2026`. Bảng thưởng đã duyệt nằm trong `README.md#doc-trung-thu-2026-reward-proposal`; hình/ID thay thế được người dùng cho phép nằm trong `README.md#doc-trung-thu-2026-assets`.

### Quyết định

- Bỏ toàn bộ đua top và khuyến mãi X3. X3 EXP từ hạ Khỉ vẫn là cơ chế sự kiện.
- Bật/tắt thủ công bằng công tắc web hiện có, không ép lịch cũ.
- Gogeta–Omega là hạ boss phe đối lập; không thêm cơ chế giết người chơi để nhận quà.
- Thỏ chỉ ở Siêu Thị 84. Dùng ID/hình có sẵn, không tạo ID client mới.
- Bánh hảo hạng dùng cùng bánh 890; công thức ngọc có thưởng phụ 30% như đã duyệt.
- Hộp VIP chưa triển khai theo lựa chọn đã duyệt.

### File và điểm nối

| Phần | Class/file chính | Điểm nối |
|---|---|---|
| Cấu hình | `TrungThuConfig`, `config/trung-thu-2026.properties` | Nạp/kiểm tra khi khởi tạo sự kiện |
| Trạng thái, giao dịch | `TrungThuStore`, `BagExchange`, `web/trung_thu_schema.sql` | `Manager.loadDatabase`, khóa autosave trong `PlayerDAO` |
| Boss | `event_trung_thu/MoonBoss` với bốn subclass riêng | `EventManager.init`, manager Trung Thu hiện có; thay pool cũ tự sinh Khỉ/Nguyệt/Nhật |
| Triệu hồi, nguyên liệu | `TrungThuRuntime` | `UseItem`, `Mob.getItemMobReward` |
| Menu, shop, bánh, pet, treo đèn | `TrungThu2026`, `TrungThuCommerce`, `TrungThuRewards` | `NpcFactory`, NPC 41 tại Kame/Siêu Thị |
| Nhiệm vụ ngày | `TrungThuQuest` | `TaskService.paySideTask`, thêm mức Siêu khó ở `BoMong` |
| Bang hội | `TrungThuClan` | Menu riêng tại `GiuMaDauBo`, giữ các chức năng thường |
| EXP, cà rốt | `TrungThuStore.State` | `NPoint`, `Player` outfit/update, `PanelBridge.skillDamage` |
| Khống chế, chọn mục tiêu | `MoonBoss.immune/accepts` | `EffectSkillService`, `SkillService` |
| Ký gửi vàng | `TrungThuMarket` | `ConsignShopService`, khóa lưu `ConsignShopManager` |
| Web | `event_catalog.php`, `boss_group.php` | Mô tả mới; ẩn định nghĩa boss cũ khi runtime mới xuất hiện, giữ dữ liệu cấu hình cũ |

### Lưu và chống nhận trùng

Hai bảng mới dùng InnoDB: `trung_thu_2026_state`, `trung_thu_2026_daily`. Túi và tiền vẫn dùng cột player hiện có, cùng format JSON của `PlayerDAO`. Không có bảng top.

- Đổi nguyên liệu qua nhiều chồng, khóa theo người chơi, hoàn nguyên nếu thêm thưởng hoặc lưu lỗi.
- Lưu túi và tiền cùng transaction trước khi gửi thông báo.
- Trả nhiệm vụ lưu cả reset nhiệm vụ, thưởng và biên nhận ngày cùng transaction.
- Hoàn tất hoạt động bang lưu biên nhận và buff cho toàn bộ thành viên cùng transaction, gồm offline.
- Buff lưu hạn tuyệt đối; nhận lại lấy hạn dài hơn, offline vẫn trôi giờ. Cache bị vô hiệu khi phát buff mới.
- Nhận tiền ký gửi nguyên liệu xóa giao dịch đã bán và cộng vàng trong cùng transaction; không trả thỏi vàng thay vàng.

### Kiểm tra

- `TrungThuRuntimeTest`: 151 kiểm tra trên DB QA, có chốt tên DB để tránh ghi vào game chính.
- `TrungThuBagExchangeTest`: 13 kiểm tra giao dịch và đa luồng.
- `TrungThuAssetsTest.php`: 53 kiểm tra tài nguyên thật.
- Hồi quy Noel 39, Giỗ Tổ 73; ẩn/hiện NPC và 12 kiểm tra nhóm boss đều qua.
- Kiểm tra JAR có `ConstIgnoreName` và nạp được 629 application class.

Test cố tình gây một lỗi SQL giả (`Expected QA save failure`) để kiểm tra hoàn nguyên; thông báo đó là một phần của test.

Các kiểm tra QA không thay thế việc xem hình/menu bằng client game thật. Các giả định thực thi và cách hoàn tác được ghi rõ trong hướng dẫn chính.

### Hình NPC và bảng nguyên liệu (15/10/2026)

NPC Trung Thu dùng ngoại hình Chi Chi tai thỏ có sẵn (head 1098, body 1099, leg 1100, icon 9976). Chỉ thay bộ hình giữ chỗ ban đầu của NPC 41; bộ hình quản trị viên tự chỉnh được giữ nguyên. Các hộp thoại Trung Thu gửi kèm icon theo giao thức menu gốc.

Làm bánh, đổi pet, đổi đuôi khỉ/cà rốt và treo đèn đều có bảng xác nhận: mỗi nguyên liệu một dòng tên + đang có/cần, thiếu màu đỏ, đủ màu xanh, giá vàng/ngọc riêng. Dùng cùng hàm chi phí với giao dịch nên đổi cấu hình không làm lệch yêu cầu. Mở bảng và Từ chối không trừ đồ; Đồng ý kiểm tra lại túi/tiền và chỉ xử lý một lần cho mỗi xác nhận.

### Bánh tăng chỉ số

`TrungThuCakeBuff` dùng mẫu465/466/472 và option50/77/103/65, cấp20/25/30%, 10phút, giá20/30/40ngọc. `TrungThuShop` thêm tab bánh và seed item/options trong transaction, giữ món và giá đã có. `NPoint` chỉ cộng một cấp bánh vào bộ tính SD/HP/KI. `TrungThuRuntime` nhận lệnh dùng đồ và xử lý hết hạn/đổi event; `ItemTimeService` khôi phục icon đếm giờ. Mốc thời gian tuyệt đối lưu trong bảng InnoDB mới, túi và buff commit cùng nhau.

`test/TrungThuCakeBuffTest.java`: 33 kiểm tra trên DB QA: giải mã shop và icon timer, mua/dùng qua luồng gốc, chỉ số thực của cả ba cấp, nâng cấp/không cộng dồn, từ chối bánh yếu, cộng thời gian cùng cấp, reconnect, hết hạn, tắt/bật event, lỗi lưu, bảo trì, giữ giá admin và không tạo món trùng. Hồi quy Trung Thu151 và hành trang100ô30 kiểm tra đều qua.

---

<a id="doc-panel-changes"></a>
## Lịch sử thay đổi panel

## Admin panel

### Trung Thu 2026 implementation — 15 October 2026

Implemented manual event control with the existing web switch, NPC 41 at Kame/supermarket, paid monkey/rabbit summons, opposing Gogeta/Omega bosses, persisted EXP/carrot effects, crafting/pet exchanges, lantern offerings, separate box reward tables, daily quest rewards and clan parade/check-in receipts. Uses the approved existing asset replacements; no new client IDs, rankings or X3 promotion. See `README.md#doc-trung-thu-2026` for rules, configuration, assumptions and undo.

Inventory/currency exchanges save together before acknowledgement and roll back on failure. Daily quest rewards also persist the task reset and receipt atomically. Clan rewards include offline members. Flour/beans/lotus consignments require gold and pay actual gold, retaining the existing 10% fee. Other consignments keep their existing payout behavior.

Validation: 69 QA integration assertions, 13 exchange safety checks, 53 real resource checks; Noel (39), Hung offerings (73), NPC visibility and Boss grouping regressions passed. Packaged JAR includes ConstIgnoreName and 617 loadable application classes. Runtime SHA256: `4EB31E96DF88C81B445B5529859DFA541B5D03FE0579606DD53E66EA66F0C74E`. Pre-deployment JAR/web/config/SQL backup: `C:\Downloads\NroFreeSetup\Backups\trung-thu-20261015-131337`.

Installed and verified on the main PC using graceful maintenance and the existing launcher window. Game PID 26036 has the same launcher parent 17752; the two new InnoDB tables exist, all four event boss definitions publish fresh heartbeats, Gogeta/Omega are alive in supermarket zones, and unsummoned monkey/rabbit counts remain zero. Existing event switches were preserved (Trung Thu and EV materials already ON). No real-client visual interaction was automated.

### Trung Thu 2026 preparation — 15 October 2026 (not deployed)

Approved rewards are recorded in `README.md#doc-trung-thu-2026-reward-proposal`. The user authorized equivalent existing assets; `README.md#doc-trung-thu-2026-assets` and `config/trung-thu-2026.properties` map the missing resources to existing templates 1047, 735, 1685, 890, 1316/1317 and NPC 41. No client IDs or live templates were changed. `test/TrungThuAssetsTest.php` passed 53 checks against actual templates and image files, using read-only queries.

The preparation includes an isolated in-memory `BagExchange` implementation with 13 passing safety checks. It is not yet connected to NPCs, shops, persistent event state or the running game; the new Trung Thu event remains incomplete. No deployment, restart or live DB mutation was performed for this checkpoint.

The dashboard now follows the supplied light blue panel reference. Boss editing opens a six-column portrait grid and a compact editor. Portraits resolve `BossData.outfit[0]` through the database `part` table to existing `/icon/<id>.png` files.

Boss image update (14 October 2026): `boss_image.php` reads the original game PNG files directly, preferring `data/icon/x4`, then x2/x1 and the website icon directory. Copy `web/game_paths.example.php` to `game_paths.php` in the deployed website root and set the game directory. Empty icon frames are skipped. Legacy Boss rows with head_id=0 resolve from the generated `boss_images.json` catalog by Boss key, then unambiguous name/map or name. The exporter reads Java metadata without starting the game or altering the database. The main-PC browser check confirmed all 237 configured Boss images load. Original boss.php is backed up in `C:\Downloads\NroFreeSetup\Backups\boss-images-20261014`.

Removed: Chế độ Admin, Đổi Sự Kiện, XNXX, Tài Nguyên, Vòng Quay, Link Share, Phúc Lợi, Tặng Item, Update Database mới. Administrator access checks remain required.

### Behavior

- **Tặng Cá:** select a player, resolve `player.account_id`, add to `account.vnd`. The character must be offline. Account balance is locked in a transaction; repeated request keys cannot add twice. The history allows undo if the account is offline and has enough VNĐ remaining.
- **Boss:** configure HP, damage, respawn delay and enabled state. Settings apply to the first form on the next spawn. Spawn runs on the boss update thread; chain, child and dungeon bosses retain their game rules. Queued spawn and admin commands expire after 30 seconds. HP/damage overrides also apply after specialized spawn logic such as Broly, before the map broadcasts the character.
- **EXP:** changes the existing `Manager.RATE_EXP_SERVER` multiplier.
- **Đập đồ:** controls upgrade levels +0 through +8 in `CombineSystem.getTileNangCapDo`; accessory upgrades, crafting and star fusion retain their separate rules.
- **Rơi đồ:** scales random chance checks in `Mob.getItemMobReward`. Map, task and equipment conditions, guaranteed rewards and boss rewards remain governed by existing code.
- **Dame Skill:** multiplies player skill damage, including the early-return calculations for Ma Kankō Sappō, Quả cầu kênh khí and the self-destruct calculation. Boss damage stays under Boss settings. Support skills without damage have no damage to multiply.
- **Kick:** uses the server's existing save/disconnect procedure.
- **Bảo trì:** invokes the existing 60-second maintenance countdown. The original server then saves players, exits and calls `restart_server.bat`, which starts it again after five minutes.
- **Chỉ số:** opens the existing player editor with character and pet stats and inventory. Account, player, item, shop, map, giftcode and transaction tools retain the existing implementations.
- **Nội tại:** edits the correct intrinsic array positions 0–2 and preserves the remaining state. Templates enforce race and parameter ranges. Offline edits are audited and can be undone while the current data still matches the saved revision.
- **Reset:** requires an offline server, no accounts marked online, InnoDB tables and typed confirmation. Backs up account, clan, player, history_transaction and super_rank before deleting their reset data in one transaction. Administrator accounts remain.

### Run locally

PHP files are deployed to `C:\xampp\htdocs\admin`; open `http://localhost/admin/panel.php` using your existing administrator login. The original website provides authentication and assets. A fresh checkout requires copying `web/config.example.php` to `web/config.php`, configuring the database, and keeping the existing root website and `/icon` directory.

Run `build-panel.ps1` to compile Java 17 and overlay compiled application classes onto the original bundled JAR. The original `Teamobi2026.jar` is retained. The resulting `Teamobi2026-panel.jar` is generated locally and ignored by Git. `run.bat` chooses the panel JAR when present. Launch it from the game directory after stopping any existing game instance through its normal maintenance procedure.

The Java bridge reads SQL schema files from `web/` relative to the game directory. New tables are additive and installed by the CLI-only `web/admin/install_panel.php`. Existing Boss tables receive the `head_id` column automatically.

### Undo

Original live files and JAR: `C:\Downloads\NroFreeSetup\Backups\panel-before-20261013`.

Run `restore-panel.ps1` to restore the original live panel and startup script. This does not alter player data. A running JVM needs a normal restart to load the original JAR.

Git checkpoints:

- `bd5bd4f`: original PHP admin panel.
- `1b3564e`: dashboard and audited VNĐ gifts.
- `a001a9e`: Boss and gameplay rate bridge.
- `8463129`: skill, intrinsic and guarded reset tools.

Reset backups have table names `panel_backup_YYYYmmdd_HHMMSS_<table>`. To restore a reset, stop the game and website writes, verify the five matching backup tables, and restore all five together in a database transaction. Do not restore just one table because player/account and clan relationships must remain consistent. VNĐ gifts and intrinsic edits have their own in-panel undo controls.

### Verification

All PHP files pass `php -l`; the full Java source compiles with JDK 17. Isolated QA database checks cover administrator access, CSRF, character search, VNĐ gifts, duplicate requests, undo, online protection, negative values, overflow, Boss persistence and image mapping, offline spawn rejection, rate revision conflicts, skill settings, intrinsic ranges and intrinsic undo.

`test/PanelBridgeIntegrationTest.java` checks the packaged JAR against the disposable `nro_panel_qa_20261013` database: Boss settings, specialized spawn stats, no repeated healing, EXP, upgrades, drop multipliers, skill damage, disabled Bosses, spawn acknowledgement and expired commands. It refuses other database names.

UI QA uses actual BossData templates exported by `test/ExportBossFixtures.java`, in a separate database and localhost PHP server. Screenshots and generated test files remain in ignored `build/` directories. Live player sessions, maintenance shutdown and production Reset have not been exercised.

2026-10-14: Publish actual live boss map/zone locations; preserve zero-second rest values. Group duplicate boss names with identical configured maps for display, retaining live zones and the current configuration key. Use Segoe UI/Tahoma/Arial for Vietnamese. Runtime packaging now uses a staging JAR; run.bat prefers the verified maps JAR. Verified 586 classes, 17 bridge integration checks, and 5 display grouping checks. Local backups: Backups/boss-maps-20261014 and Backups/boss-font-group-20261014 under NroFreeSetup.

2026-10-14 typography: shared web/typography.css applies Segoe UI/Tahoma/Arial to 26 valid public/admin pages, 15–16px body/form text and readable metadata. Boss dialog enlarged; mobile cards wrap into fewer columns. PHP syntax checked before deployment; desktop and 390px responsive views checked without horizontal overflow. deploy-typography.ps1 prepares, checks and backs up live pages. Existing broken images/avatar/zoom.php is skipped (unmatched brace predates this change). Backup: C:/Downloads/NroFreeSetup/Backups/web-typography-20261014-233140.

2026-10-14: Boss editor sorted by TaskService.checkDoneTaskKillBoss main/subtask progression, including Android 15 -> 14 -> 13 and Poc -> Pic -> King Kong. Mission headers separate stages; unrelated bosses follow afterward. Eight grouping/order checks passed. Backup: Backups/boss-task-order-20261014.

2026-10-14: Upgrade settings now have independent level (+0 to +8) and crystal star (0 to 9) chance arrays. Existing level settings preserved; star defaults use actual PhaLeHoaTrangBi chances. NPC preview now uses the actual star chance instead of getFakeRatio. Additive star columns, revision-protected API, full JAR staged as Teamobi2026-panel-stars.jar; run.bat prefers it. Verified 586 classes, 19 bridge assertions, four star API checks. Backup: Backups/star-rates-20261014.

2026-10-15: New drop_rates.php dashboard with global and per-category multipliers, actual base-roll previews and map conditions. Groups wired to Mob reward branches; x0 suppresses random drops, chance capped at 100%; task/free guaranteed rewards preserved. Shop-with-stars branches verified against ItemService. Upgrade stones have no existing mob reward branch and are shown unavailable. Revision-guarded transactional API: five QA checks passed; bridge: 24 checks passed.
Windows Selector initialization was failing despite a bound port. Network now uses a direct ServerSocket accept fallback if Selector.open fails, preserving existing session setup; real socket test and shutdown test passed. run.bat prefers verified Teamobi2026-panel-network.jar, uses Java 17/IPv4 and keeps the already-running notice visible. Verified live Network thread waiting in ServerSocket.accept. Maintenance countdown is 15s from panel/console/NPC/automatic paths; restart_server.bat waits 10s. A real maintenance/restart cycle completed with a new PID and healthy Network thread. Backup: Backups/drop-rates-20261015.

2026-10-15 upgrade stones: corrected material IDs 220–224 that had been misclassified as crystal-star drops. Ordinary maps now have one 2% stone roll; cold maps retain their single 20% roll. Each success drops one of five existing upgrade materials with existing item options. ID 225 is stone fragment and its existing 10% branches now belong to the stone category. True crystal stars 441–447 retain their own 10% branch and independent multiplier. Enabled upgrade_stone in schema/dashboard. Verified 587 runtime classes, 26 bridge checks and six API checks. Packaged separately as Teamobi2026-panel-stones.jar; rollback retains prior network JAR. Backup: Backups/upgrade-stones-20261015.

2026-10-15: Removed both mob reward branches for stone fragments (item 225). Five upgrade materials retain their 2% ordinary / 20% cold map roll. Panel previews now show only those two rates. No remaining direct item-225 world drop creation found in Java sources. JAR packaged separately, 587 classes verified; backup Backups/no-fragments-20261015.

2026-10-15 same-terminal restart: ServerManager.close saves as before, then exits with code 42 instead of launching cmd/start. run.bat restarts only for that code, waits 10 seconds and loops in the current console; other exits stop for inspection. Legacy restart_server.bat remains a 10-second compatibility entry for old JARs. Live verification: Java PID 10060 changed to 21588 while terminal parent PID remained 17752. Closed idle old restart terminal. Latest runtime: Teamobi2026-panel-launcher.jar; 587 application classes checked. Database backup: Backups/same-terminal-restart-20261015.

2026-10-15 dashboard redesign: replaced tile overview with light sidebar layout, live runtime metrics, grouped existing tools and quick actions. Overview displays top gold-bar players (item 457, bag + box, saved data) instead of bosses. No periodic-maintenance controls. Real activity rows come from panel_command and VNĐ audit; no synthetic server logs. Manual maintenance remains 15s with 10s same-terminal restart information. Gold leaderboard refreshes every 30s, metrics every 5s. PHP, JS checks passed; five inventory/ranking checks and three isolated API checks passed. Desktop and 390px mobile checked without horizontal overflow; existing Boss and VNĐ dialogs checked read-only. Backup: Backups/dashboard-redesign-20261015. No game restart needed.

2026-10-15 shared admin dashboard theme: added subtle pastel metric/action cards and shared admin-ui.css, admin-ui.js, admin_ui.php across 16 functional pages: accounts, players, giftcodes, giftcode creation, item/shop catalogs, gold/gem shops, maps, trades, intrinsic, reset, upgrades/EXP, skill damage, bosses and drops. Removed decorative GIF/banner clutter; retained form fields, IDs and action handlers. Unified readable Vietnamese titles, compact toolbars, scrollable tables, soft save/delete/cancel styles and responsive editors. Password fields are masked with an explicit reveal toggle; player-name fields remain text. Shop edit/delete controls stay visible without hover. Overview links close embedded dialogs rather than nesting dashboards. Verified PHP syntax, JS syntax, all 16 live page loads, desktop and mobile layouts, mobile shop add/cancel and embedded return navigation without saving/deleting production data. Live backup: C:/Downloads/NroFreeSetup/Backups/admin-theme-20261015. No game restart needed; screenshots in ignored build/admin-theme-dashboard.jpg and build/admin-theme-shop.jpg. Git push remains subject to user approval.
2026-10-15 inline dashboard navigation: all 14 functional menu destinations now open in the dashboard workspace beside the persistent sidebar instead of the modal dialog. Boss uses its full page layout. Quick links and giftcode chips use the same navigation; shop subpages retain the shell. Hash addresses restore the selected page on reload and support browser Back/Forward. Active menu, expanded group and page heading follow navigation. The workspace fits the viewport and scrolls long data pages internally. Existing operational confirmations (maintenance, VNĐ gifts, kick) and record editors remain intact. Shared child pages suppress duplicate outer headers and return-to-panel links use the parent overview. Verified every menu destination, shop subnavigation, overview return, browser Back, reload, canceling the maintenance confirmation and 390px mobile menu without horizontal overflow. No production data was saved/deleted and the game was not restarted. PHP and JS syntax checks passed. Backup: C:/Downloads/NroFreeSetup/Backups/admin-inline-pages-20261015; screenshot build/dashboard-inline-boss.jpg. Push requires user approval.
2026-10-15 player inventory layout: scoped inventory grids separately from shop item cards, fixing the shared product-grid override that made previews vertical and detail views only three columns wide. Bag and box previews now flow left-to-right in seven 36px slots per row, clip cleanly after three rows without fading, and include explicit view buttons. Detail grids use compact 40px slots and available width (18 columns in the checked desktop workspace, six on 390px mobile), retaining quantities, names on hover, empty slots and existing delete confirmations. Verified preview coordinates/124px limit, actual live bag detail contents, mobile modal sizing and close action without saving or deleting player data. PHP syntax and diff checks passed. Backup: C:/Downloads/NroFreeSetup/Backups/player-inventory-grid-20261015; screenshots build/inventory-three-rows.jpg and build/inventory-compact-detail.jpg. No game restart.

2026-10-15 inventory detail sizing: increased detail slots from 40px to 48px, icons from 28px to 36px and quantity labels to 12px; preview remains seven compact columns/three rows. Live desktop verified 15 columns with no horizontal overflow; PHP syntax passed. Backup: Backups/inventory-size-20261015; screenshot build/inventory-detail-larger.jpg.


2026-10-15 Giftcode item names: the content column renders actual template names, icons and quantities as wrapping chips. Raw JSON remains in the existing tracked input for unchanged save handling; clicking the preview opens the existing editor, and confirming edits refreshes the names. Search supports displayed names as well as raw IDs. Template names are inserted with textContent and PHP JSON uses HTML-safe flags. Checked live multi-item and single-item codes, name search (one result for Thỏi vàng), editor open/cancel with no changed fields, PHP syntax and diff checks. No Giftcode data was saved/deleted. Backup: Backups/giftcode-item-names-20261015; screenshot build/giftcode-item-names.jpg.

2026-10-15 regional drop percentages: introduced 37 named roll rules covering every category call in Mob.getItemMobReward (36 calls; the stone call selects mutually exclusive ordinary/cold rules). The panel shows map coverage and existing branch conditions next to each percentage, with independent Save and Default controls. A null override inherits the existing group/global formula; a custom percentage replaces positive shared multipliers while preserving the original branch bonus ratio. Global/group x0 still disables custom rules. Chance is capped at 100%. Sequential/OR checks, eligibility, reward quantities and guaranteed task drops remain unchanged.
New additive tables panel_drop_rule and panel_drop_runtime hold nullable percentages and a separate runtime acknowledgement; revision-guarded saves support eight decimal places and reject invalid branches/values. Existing rate values were retained; live verification found 37 rules, zero overrides, global x1 and matching regional revision 20. Regional schema is generated from drop_rules_catalog.php with test/generate-drop-rules.php. UI polling preserves unsaved regional input; Default affects only that row. Event rows use actual item names.
Validation: 13 regional API assertions on isolated nro_panel_qa_20261013, 16 regional bridge assertions, all 26 existing bridge checks, all 37 rule mappings, full compilation and 588 packaged application classes including ConstIgnoreName. QA UI Save/Default and 390px layout checked without horizontal overflow. Production has not been given any trial rate changes.
Runtime: verified Teamobi2026-panel-regional-drops.jar installed atomically as the launcher JAR only after maintenance had saved and exited Java. The first swap failed before replacing anything; the original runtime restarted, then a corrected swap succeeded through another normal maintenance cycle. Final Java PID 12548 retained terminal parent PID 17752, listened on 14445 and published regional heartbeat. run.bat remains unchanged. Backups: C:/Downloads/NroFreeSetup/Backups/regional-drop-rates-20261015 contains prior source/web files, configuration SQL and Teamobi2026-panel-launcher-before.jar. To roll back, stop/save the game normally, restore that JAR and backed-up web files, then restart; new tables may remain for recovery. Never replace a running JAR. Screenshot: build/regional-drop-rates-live.jpg. Git push still requires user approval.

2026-10-15 collapsible drop groups: all 14 groups now use native details/summary controls, closed by default. Compact headers show name, multiplier, condition count and chevron; expanding reveals the existing regional percentage forms and advanced controls. Polling preserves expansion state. Verified all 14 start closed (89px headers), stone group opens both region inputs and closes again, and 390px view has no horizontal overflow. PHP syntax passed; no rate changes or game restart. Backup: Backups/drop-groups-collapse-20261015; screenshot build/drop-groups-collapsed.jpg.

2026-10-15 intrinsic catalog editor: added a default toàn server catalog tab alongside the existing per-player editor. All 25 active template IDs have race/name/ID filters, real icons, current in-game descriptions, collapsible min/max editors, both parameter pairs where defined, live previews and per-template Save/Cancel. Race filters include common templates. Sentinel ID 0, IDs, names, icons and race identity are preserved. Existing combat/effect implementations are unchanged; this feature edits template ranges and the values drawn on future opens, not the effects themselves or already-rolled player values.
The database contains eight identical copies of each ID (208 rows, 26 unique including sentinel). The editor presents one item per ID, validates identical copies and updates all copies together without deletion. Conflicting copies are rejected. Saves use a global revision and template fingerprint; full before/after metadata plus duplicate count are audited. Undo checks both current values and copy count, then restores only that template. Values are nonnegative signed-short integers with ordered bounds; unused second ranges cannot be invented. Existing player edits, online protection, countOpen preservation and undo remain available; their read list now deduplicates identical templates.
IntrinsicService publishes one complete immutable catalog snapshot after validation. Per-race lists have a single leading sentinel, so rerolls cannot select duplicate inactive rows; new descriptions and random roll bounds use the refreshed snapshot. Player-owned Intrinsic objects retain their rolled values. PanelBridge reloads changed catalog revisions and acknowledges heartbeat independently; failed catalog validation keeps the previous snapshot and does not interrupt other bridge features. Future edits synchronize in about three seconds without restarting the game.
Validation: 22 isolated catalog/per-player API assertions, Java checks across all races for uniqueness/eligibility, fresh bounds, existing-player preservation, defensive copies, immutable lists, conflicting duplicate rejection and failed-revision recovery. Existing 26 bridge and 16 regional-drop checks passed. Fixed the bridge test's own customized Boss fixture cleanup so repeat runs are independent, within its existing QA-only database guard. Full source compilation and 589 packaged classes, including ConstIgnoreName, verified. QA UI Save, preview, race/shared filtering, tab switching, four parameter inputs and 390px layout checked. Production has zero template audit changes and retains all 208 rows.
Deployment: original intrinsic table/config and JAR backed up in C:/Downloads/NroFreeSetup/Backups/intrinsic-catalog-20261015. Verified Teamobi2026-panel-intrinsic.jar installed atomically as the launcher JAR after normal 15-second maintenance, followed by the existing 10-second same-console restart. New Java PID 11312 retained parent 17752, accepted TCP connections and acknowledged intrinsic revision 1. run.bat unchanged. Roll back source/web and restore the backed-up JAR only after saving/stopping the game; intrinsic-config-before.sql preserves duplicate rows. Screenshot build/intrinsic-catalog-live.jpg. Push requires user approval.
2026-10-15 event switches: added the Sự kiện sidebar page using the dashboard iframe route. Eight independent persisted switches cover Broly, Hùng Vương, Tết/Lân con, Halloween, Christmas, Trung Thu, the sugarcane NPC and the existing event-category mob drops. Pastel cards start compact, expand for exact scope, show runtime acknowledgement, and keep an audited revision-protected Undo history. Authentication, CSRF, strict boolean input and stale revision guards apply to every mutation. 8/3, Top Up, 20/11 and Valentine remain explicitly unavailable because their source contains loaders/stubs/IDs rather than complete event behavior; the separate event dragon workflow is outside these seasonal switches.
Game wiring: allocate each existing seasonal boss population once at startup; one worker now updates the five seasonal managers that previously had no startup threads. Reload the immutable switch snapshot from DB about every three seconds, independent of other bridge features, and acknowledge only after event initialization. Switching off suspends existing bosses without death rewards, stage progression or linked-boss wakeups. Injury/reward paths are gated too; re-enabling reuses the existing objects. Hùng Vương blocks Vua Hùng/Nồi bánh menus and confirmations, while already-planted fruit remains harvestable. Summer controls the sugarcane exchange; event mob drops have a separate independent switch and preserve all configured percentages. Tết suspends follower/reward eligibility and guards the Kame/reward service paths. No duplicate or unrelated NPC is spawned from the legacy LunarNewYear NPC 49 entry.
Validation: 29 isolated API checks, 23 event bridge/lifecycle checks (including live zone removal and duplicate-population prevention), existing 26 bridge and 16 regional-drop checks; full Java compilation, all 589 packaged classes including ConstIgnoreName, PHP lint and JS syntax passed. QA browser verified sidebar navigation, actual switch save and history Undo. Production runtime uses the verified events JAR atomically installed after normal 15-second maintenance and 10-second same-console restart: PID 16840, same parent 17752, TCP 14445 accepts connections, Network/PanelBridge/Seasonal Event Bosses threads healthy and event revision acknowledged. Live admin switch changes made during rollout were retained. Screenshot: build/events-live.jpg.
Rollback checkpoint: C:/Downloads/NroFreeSetup/Backups/event-switches-20261015 contains original modified source files, the previous launcher JAR, prior live dashboard and pre-migration event config snapshot. Restore source/web from that backup and restore the prior launcher only after saving/stopping game. The additive event tables can remain; do not remove or reset administrator event history. Push requires user approval.

2026-10-15 event NPC visibility correction: OFF now hides registered Vua Hùng, Nồi bánh and personal Dưa hấu (Hùng Vương), Xe nước mía (summer), Trung Thu NPC 41 and Cây nêu 72 (Tết). Map-entry packets carry status 0 for disabled event NPCs; the existing -73 protocol updates players already on the map. A per-player weak cache refreshes on switch snapshot changes, map/zone entry and returning from a null zone; unchanged DB polls keep snapshot identity and do not spam packets. A post-map refresh reconciles switches changed while the map packet was being sent. NPC definitions and registries remain stable, so toggling cannot create duplicates and existing personal eligibility filters stay in effect. Central menu dispatch rejects interaction with hidden event NPCs, including stale confirmations. Santa's permanent shop and other ordinary NPCs remain available. Trees retain their saved growth data and can be harvested after Hùng Vương is enabled again.
Validation: 25 QA-only assertions cover initial map packet statuses for six NPC IDs, live OFF/ON packets, ordinary NPC preservation, menu rejection/restoration, no duplicate NPCs, no packet spam, map-entry race reconciliation, zone changes, leaving/rejoining and personal tree eligibility. Existing 23 event bridge/lifecycle checks passed; full source compilation and all 590 packaged application classes verified, PHP lint and diff checks passed. No production EV configuration values changed during this correction.
Deployment: previous launcher, modified source and live PHP pages backed up under C:/Downloads/NroFreeSetup/Backups/event-npc-visibility-20261015. Updated events JAR installed atomically after normal 15-second maintenance and 10-second same-console restart; new PID 10236 retains parent 17752, TCP 14445 accepts connections, Network/PanelBridge/Seasonal Event Bosses threads healthy and EV revision 5 acknowledged. Restore the backed-up launcher only after saving/stopping game to roll back. Screenshot build/event-npc-visibility-live.jpg. Push requires user approval.

2026-10-15 client compatibility fix after visible Xe nước mía report: the previous status-0/-73 implementation still transmitted disabled NPCs and did not hide them on the user's client. Replaced it with server-side filtering in NpcManager: disabled event NPCs are completely absent from the -24 map NPC list. Original server definitions remain intact for re-enabling. Player visibility tracking now compares actual transmitted event NPC IDs with the current eligible IDs, so a live switch clears the stale client map list and rebuilds the same map at the same coordinates once. It does not teleport/remove/re-add the player on the server. NPC-only load acknowledgements restore actors/effects while skipping ordinary map-transition tasks, notifications, transformations and related actions. Normal zone changes retain normal completion. The separate fruit-growth sprite sender also respects the Hùng Vương switch.
Regression correction: rewrote the visibility fixture to model the reported client, which draws every NPC record regardless of status. 29 QA-only checks verify complete absence/presence for six event NPC IDs, ordinary Santa visibility, stale menu guards, no reload spam, current-map refresh ordering and position preservation, real finishLoadMap completion without replaying map actions, duplicate prevention, race reconciliation and personal fruit eligibility. Existing 23 event lifecycle checks passed. Full source compilation and all 590 packaged classes verified. The prior 25 status-only assertions were insufficient to prove visual hiding on this client; the new assertions inspect NPC list membership instead.
Deployment: normal save/maintenance plus 10-second same-console restart completed; launcher SHA256 0566CBDE4B008AB60A8F1ED3ED01DE0C3C2E02E2E5061B4E7FB6D719B1F87FE8 matches the verified new events JAR. New PID 25408 retains parent 17752; TCP 14445 accepts connections and EV revision 7 is acknowledged. Sugarcane remains disabled according to the user's saved choice. Backup: C:/Downloads/NroFreeSetup/Backups/event-npc-map-filter-20261015. No production switch edits were made by this fix. A proposed direct JVM instrumentation probe was rejected by automatic approval review and was not attached or executed; validation relies on the packaged artifact/hash, live heartbeat/port and QA packet/client-model checks, not a visual inspection of the running DragonBoy client. Push requires user approval.

2026-10-15 Hung Vuong completeness: production map data had Vua Hung only on Thanh Co maps 183/184/185, no Noi Banh anywhere, and only personal fruit NPCs at home. The web switch could reveal only those existing definitions. Added idempotent startup seeding of Vua Hung (52) and Noi Banh (66) in villages 0/7/14; existing Thanh Co and home definitions stay intact. OFF still filters all event NPCs, ON reveals the preseeded village NPCs immediately without duplication. Source/DB respawn defaults were preserved: ten Thuy Tinh roots with linked Son Tinh, random configured maps, 900-second normal respawn cooldown. Enabled the root's normal appearance notification and manual web summon eligibility; companion remains linked to its parent. The events page now displays a compact, expandable live boss count and map/zone list from the bridge, with stale-data markers and NPC location instructions.
Validation: 21 QA-only HungVuong integration assertions verify NPC seeding/idempotence and visibility in three villages; real REST/RESPAWN/JOIN_MAP/ACTIVE transitions, both bosses physically registered in the same zone, linked companion gating, ON/OFF/re-enable without duplicated instances, root-only manual spawn and retained normal cooldown. Existing 29 map-list compatibility checks and 29 event API checks passed. Full source compilation, all 590 packaged classes including ConstIgnoreName, PHP lint and JS syntax verified. Backup: C:/Downloads/NroFreeSetup/Backups/hung-vuong-complete-20261015.
Deployment and live verification: installed verified events JAR atomically after normal maintenance; SHA256 96436659AD09F7E538C914057C2216226133A1CC6459BBDEDC7AED4241C05366, PID 24568, parent 17752 retained. Temporarily enabled Hung Vuong through the authenticated web switch, then verified the real bridge reported ten Thuy Tinh and ten Son Tinh in matching map/zone pairs (including maps 28/68/20/65/11/80/18/15). The live dashboard displayed all 20 and their actual locations. Captured build/hung-vuong-boss-live-proof.jpg while ON. Restored the original OFF setting through the same audited switch after the test; no other EV settings changed. Existing Thanh Co NPC locations and production map-template rows were not edited; village additions are seeded at boot. Previous launcher/source/live web preserved in the backup for rollback after saving/stopping game. Push requires user approval.

2026-10-15 Giỗ Tổ material chain and offering mobs: user clarified that Voi Chín Ngà, Gà Chín Cựa and Ngựa Chín Hồng Mao should appear both randomly in world maps and in Thành Cổ. Added one triplet in each existing zone of maps 183/184/185 and six randomly selected ordinary map/zone groups; random groups are selected once per server boot. Uses actual mob templates 82/83/84 (20m/12m/15m HP), existing giant attack code and the existing zone updater. Stable tail slots, dead-slot retention while ON, a 60-second respawn and personalized revive packets keep client indices intact. OFF hides the entire offering block through the existing map refresh mechanism, suspends attacks and rejects stale damage/rewards. No additional combat updater thread.
Drops: matched offerings 1220/1221/1222, base 100% one item each; six real recipe ingredients 1544–1549, base 5% each from ordinary mobs excluding training dummies and giants; both actual Sơn Tinh/Thủy Tinh reward paths give one owned Tem Mai An Tiêm 1558 at base 100%. All ten rates independently editable on the compact drop page. Seasonal drops require the individual event and mob_rewards; summer sugarcane requires both summer and mob_rewards. Bill food and quest items are separate categories, retaining the old event factor and existing per-rule overrides. Corrected Tayaki consumption ID 1789 to 1798. Nồi Bánh checks exact ingredient quantities and reserves the single cake before consuming ingredients so bag-full/short quantities cannot lose materials.
Web: expandable live offering locations grouped by map/zone on the Hung Vuong card, stale heartbeat indicator, correct Vietnamese horse name, bounded scroll and compact neighboring cards. Updated event scopes and category descriptions. Added idempotent SQL seeds for ten rules and a runtime location table; all 37 pre-existing production percent overrides preserved, and prior group factors copied to the newly separated categories. Production EV settings were not overwritten; the user enabled Hung Vuong during implementation and it remains ON.
Validation: 73 isolated offering/drop/recipe assertions, 29 event NPC visibility checks, 22 Hung Vuong lifecycle/real boss reward checks, 23 event bridge checks, 16 regional bridge checks, 29 event API checks, 13 regional API checks, ten independent material-rate save/restore cases and catalog coverage for all 47 rules. PHP lint, JS syntax, full Java compilation and 592 packaged application classes including ConstIgnoreName passed. First build ran out of disk space; retry succeeded without deleting user data. Tests with database writes used only nro_panel_qa_20261013.
Deployment: normal 15-second save/maintenance and ten-second same-console restart. Verified launcher SHA256 D59DF5F09ED9B72DD6B5064D32A57A827204BCF3411C983A7468E3CE21A239BD, new Java PID 12808 with original console parent 17752, listening TCP 14445 and fresh runtime heartbeat. Real production runtime reported 108 living offering mobs: 90 in thirty Thành Cổ zones plus eighteen across six random zones (maps 1/6/29/72/74/83 this boot). Dashboard confirmed 108 locations and twenty Hung Vuong bosses. Screenshot: build/offering-mobs-live.jpg. This is live server/panel evidence; native DragonBoy rendering was not directly inspected. Backup/source/JAR/web/config and rollback guidance: C:/Downloads/NroFreeSetup/Backups/event-material-drops-20261015. Known unrelated audit finding remains: Trung Thu item 2124 is missing from the database; no replacement item was invented or deployed in this change. Push requires user approval.

2026-10-15 Thành Cổ ticket: user requested a ticket in Chi Chi's shop and selected 5 green gems per one-use ticket. Added item 2000 (type 75, gender 3) with the existing ticket icon 13948 and a short Vietnamese description. Idempotent web/thanh_co_schema.sql seeds the template and one Chi Chi shop entry at the front of Shop sự kiện; preserves later shop price edits. ThanhCo.ensureData runs before Manager loads item templates/shops, so the new ID is available during the same startup. It refuses a conflicting existing item ID. Item cache version 9 -> 10 ensures clients request the added template on login. No new image resource is required.
UseItem dispatches the ticket to a synchronized entry flow. Hung Vuong must be ON, the player alive and outside dungeons; target map 183 selects an available zone using a bounded scan. Only subtracts one owned ticket after the actual ChangeMapService transition succeeds. Full/missing zones, disabled EV, dead player, stale item reference, already-in-castle use and normal map-transition rejection do not consume a ticket. Dynamic castle-only waypoints connect 183 -> 184 -> 185 and permit free exits to map 5 (Đảo Kamê); there is no free outside entry gate. Exit works even with EV OFF. Existing database waypoints and NPCs are untouched.
Validation: 19 isolated tests run the real item dispatcher, map transition, waypoint handler and normal shop purchase path: five green gems deducted, ticket awarded, insufficient funds rejected, available-zone selection, all failure cases preserving tickets, inter-castle traversal, exit and cache version. Five QA-only migration assertions verify safe template/icon, no duplicate shop entries, preserved edited price and collision rejection. Existing 29 NPC visibility and 23 event bridge checks passed. Full Java compilation, all 593 packaged application classes including ConstIgnoreName and git diff checks passed.
Deployment: backed up source, prior launcher and pre-migration ticket/shop state under C:/Downloads/NroFreeSetup/Backups/thanh-co-ticket-20261015 with rollback guidance. Normal 15-second maintenance/save and ten-second restart reused console parent 17752. New PID 3172 listens on TCP 14445; applied event revision 18 heartbeat fresh. Launcher SHA256 77609C82FBB8CF484DFA831F2451E1E468DFFA8123566CC2B505C744698D8840 matches built JAR. Production startup created one ticket template and one shop entry (item_shop 1000, tab 58, SHOP_CHI_CHI, cost 5, type_sell 1, is_sell 1). Verified live admin shop shows ticket name, ticket icon and green-gem price; screenshot build/thanh-co-ticket-shop-live.jpg. Native DragonBoy rendering/purchase was not directly operated; functional purchase and travel were verified against the game methods in isolated tests. No production currency or inventory was edited for testing. Local commit only; ask user before pushing, including the previous unpushed material-drop commit.

2026-10-15 Noel logic audit/fix: first pushed the user-approved material-drop and Thành Cổ ticket commits to origin/main through dfd4f5c. Audited the actual Noel chain: 30 friendly invulnerable Ông già Noel drop item 648, pickup consumes stocking 649, owned boxes open into the existing seven reward groups or 10–20 green gems at 5/90. Stockings already existed at 20 green gems in BULMA_EVENT; no new item templates/assets or invented costume reward pool were required. Full Vietnamese audit/usage notes: README.md#doc-noel-logic. Generic gift 1170 remains separate and unchanged.
Fixed Noel zone selection to consider capacity and existing Christmas actors together, using zone membership rather than the unrelated BossManager list. Manual and normal spawn now initialize 100–300-second visit and 60-second first-drop timers consistently; failed placement enters normal cooldown. Noel follows the nearest living real player, including the former 50–500 distance dead zone, instead of choosing dead players/pets. Construct ItemMap only after each successful 1/3, 1/5 or 1/7 roll, preventing failed drops from appearing later through map reload. Retained friendly/no-damage behavior, existing base drop chances and configured boss respawn cooldown; root is eligible for manual web summon. Replaced inappropriate chat text with Vietnamese Christmas greeting.
Added idempotent SANTA_NOEL stocking shop (NPC39, one item649, initial20 green gems, non-trade option30), initialized before loading shops. Santa appends Giáng sinh only while the EV is ON, retaining ordinary and coupon menus; stale menu/shop requests are rejected after OFF. It remains a permanent ordinary NPC when Noel is OFF. Dedicated gift pickup locks the ground gift and player, verifies ownership/sock quantity/space, grants box then charges socks and marks/removes the ground record before network writes. Simultaneous pickup and retries cannot duplicate that gift. Gift opening preserves original reward groups/chances, reserves item rewards before consuming an owned box, checks green-gem int overflow and keeps owned boxes usable after the EV ends. Legacy NoelItemBox entry delegates to the same implementation; old unreachable seasonal cosmetic branches removed from that method.
Validation: 39 isolated Noel tests execute real spawn/leave/cooldown, attack targeting, deterministic successful/failed gift rolls, real Zone.pickItem (including simultaneous competing threads), all reward groups/currency/overflow/full bag/invalid owner and Santa/coupon/stale menu/shop purchase flows. Four QA migration assertions verify unique shop entry, 20-gem existing price, normal currency tab semantics and preservation of edited prices. Existing29 NPC,22 Hung Vuong,23 event bridge,73 offering and19 castle ticket checks passed. Full compile and594 packaged classes including ConstIgnoreName verified; PHP lint and git whitespace checks passed. Memory socket fixture is in-process only and never connects to the live server. No live inventory or currency changed by tests.
Disk handling: compilation/packaging initially failed because C: was full. Removed one failed staging JAR and eight redundant build-only launcher snapshots after verifying identical SHA256 against retained JARs under Backups (62.4MB). All canonical backup JARs remain intact; duplicate manifest is build/duplicate-build-jars.json. Bounded javac/jar heap to256MB in build-panel.ps1; build then completed. The running launcher was not overwritten until normal maintenance exit.
Deployment: C:/Downloads/NroFreeSetup/Backups/noel-logic-20261015 preserves source, previous launcher, live catalog, config/shop snapshot and rollback guidance. Normal15-second save/maintenance and10-second same-console restart succeeded; new PID12280 retained console parent17752, TCP14445 listens, verified launcher SHA256 A617ECCB0F22DEC42906CB7DF37F6531C667CA00AC7F82CC9D3D6487303EA018 equals built artifact. User had already enabled Noel and disabled Hung Vuong; preserved those selections and all other switches. Fresh real bridge data confirmed30 Noel actors in30 distinct map/zone positions with max1 per zone after boot. Production shop37/tab64 contains stocking649 at20 green gems and admin shop rendering was verified; screenshot build/noel-stocking-shop-live.jpg. Native DragonBoy client rendering/pickup was not directly inspected; gameplay paths were tested on isolated game objects. New Noel changes are a local checkpoint; ask user before pushing them.

2026-10-15 300-slot paged bag and Trung Thu test pack: granted the user-requested 26 Trung Thu test item types to username 11 (account3729, player1368/222552), using an offline guarded transaction and retaining the pre-grant inventory/receipt under Backups/trung-thu-test-account11-20261015. Initially allocated80 safe slots while waiting for the user's explicit choice of three100-slot pages. No repeat grant on expansion; player activity before maintenance was saved and preserved.
Implemented BagPages with native NPC selection (chat tui, tui1/tui 1 and /tui1 variants; Santa appends Trang Túi đồ). Server maximum300, each existing client bag packet at most100. Actual capacities for other characters are unchanged; item517 expands one slot up to300. Initial login and update packets include explicit empty slots. Resolve client-local indices at use/equip/pet/box, throw, sell, trade, combine and consignment gateways; retain the complete300-slot server inventory and trade snapshots. Item-identity/action/location-bound one-shot confirmations reject old/replayed sell/use/throw actions; page changes cancel confirmations and combine selections, while active trades block page changes. Legacy gold-trade sentinel and existing Santa/Noel indices retained. BagPages.ensureStorage widens TEXT to LONGTEXT before game data load, preserving collation/nullability/comment; this prevents truncation of heavily optioned300-item bags. Existing larger column types are retained. Usage/limits/rollback: BAG-PAGES.md.
Validation: full Java compile;67 in-process legacy-packet/action checks (including actual third-page use, equip, bag-to-box, throw, sell, trade, stale/replay rejection, login count, page NPC, empty/partial pages, combine local slot95, consignment and cap299->300);4 QA connection-local temporary-table checks preserve Unicode/column properties and save beyond64KB. Existing39Noel,13safe-exchange and69QA Trung Thu integration checks passed. Packaged619 application classes, including ConstIgnoreName, load successfully. PHP lint and git whitespace checks passed. No native client UI automation was performed.
Deployment: staged JAR SHA256 D869ED02ADEACF91B9F974E3F180D06083A6A8586B448EBD1027869AFD7177FF. Backed up prior launcher/config, all bags before schema change and latest saved player80-slot bag under C:/Downloads/NroFreeSetup/Backups/bag-pages-20261015-144724. Queued normal15-second maintenance command23, waited for graceful JVM exit, replaced launcher and expanded the offline character before normal10-second restart. New PID27904 retains original console parent17752 and listens14445. Live verification confirms300slots, all previous80slots and money unchanged, every other character bag unchanged, LONGTEXT storage and fresh non-maintenance heartbeat; user11 reconnected successfully. build/bag-live-verified.json contains verification. Keep LONGTEXT on rollback; do not run old byte-count protocol with a300-slot bag or restore old inventory over subsequent player activity. Local checkpoint only; ask user before push.

2026-10-15 Return to one150-slot inventory at user's request: removed all three-page state, chat commands, Santa page button, CON_MEO page menu and auto-page combine/Whis behavior. Replaced BagPages with BagProtocol for native index validation, one-shot item confirmations and retained LONGTEXT storage only. Set maximum150; other players keep their actual owned slots. UseItem and combine incoming bag indices now read unsigned bytes so slots128–149 work. Trade normalizes its signed byte only after the existing -1 gold sentinel; consignment index helper now returns int so slot149 is not transmitted as negative -107 in its short identifier. Native login/update packets expose all150 slots in one bag; no pagination and no two-byte protocol change.
Validation:30 isolated native packet/action checks include complete150-slot login/update, slots127/128/149 use, by-template255sentinel, final-slot equip/box/throw/sell/trade/consignment/combine and cap149->150. Six pure PHP compaction cases preserve a real item at slot299 and complete serialized records, refuse151 occupied items without mutation, preserve old slots when expanding small bags and are idempotent. Four QA temporary-table storage checks,39Noel menu/gameplay checks and69QA Trung Thu integration checks pass. Full compile and619 packaged application classes, including ConstIgnoreName, verified. No native client UI automation; do not claim visual rendering was inspected.
Deployment: audited all live inventories; only player1368/222552 (username11) had300slots, with52occupied and no items after150. Backed up previous300-slot launcher/config and latest saved inventory under C:/Downloads/NroFreeSetup/Backups/bag150-20261015. Normal15-second maintenance command24 saved the online player, offline transaction compacted to150 retaining all52complete item records and money, then installed the new runtime before the ordinary10-second same-console restart. Helper refuses compaction when occupied count exceeds150, checks offline state and CAS and validates preserved items/money before committing. New JVM PID10560 retains parent17752 and listens14445; live launcher SHA256 C55EB7BF88D20E3E36B4EBBB0C5D17CB56D56BB7476B8E3FA3F1AC663E351D72 matches staged artifact. Post-boot verification confirms150slots, all52item records and money unchanged, zero oversized inventories, maintenance0 and fresh heartbeat. build/bag150-live-verified.json is the verification receipt. README.md#doc-bag-inventory replaces BAG-PAGES.md. Checkpoint is local only; ask before pushing.

2026-10-15 Trung Thu drop controls, native NPC shop and one-time-file cleanup:
Added the Trung Thu category and ten independent per-source rules to the existing collapsed Drop Rates dashboard: egg/chicken2% in normal/private maps, flour/beans5% in Doanh Trai/Treasure/Snake Way/Gas dungeons, monkey tails/carrot100% from their summoned bosses, separate Gogeta/Omega costume20% and board1% rolls. Real mob and boss reward paths use PanelBridge.rollDrop; group/server multipliers and independent percent overrides work through the existing3-second bridge reload. x0 and both event/material switches still stop drops. Quantities1–3, faction/wooden-sword checks, monkey EXP, conditional board permanence, quest-only lotus and box reward tables are retained. Main server multiplier was1 and new group1, so base chances did not change on installation.
NPC41 now opens the ordinary NORMAL_SHOP packet/window instead of nested purchase menus. DB-backed TRUNG_THU_2026 shop38 has three tabs (Triệu hồi, Đồ sự kiện, Trang phục), eight existing item templates and original gold/green-gem prices. Idempotent seeding runs before Manager loads shops and preserves prices admin edits on the web. Normal shop purchase dispatches into the existing atomic BagExchange/persistBag path: correct currency, inventory reservation, save rollback, cooldown, invalid-item and event-OFF protection. Craft/pet/faction/exchange menus remain separate ordinary NPC options. No new client images were needed.
Validation:91 QA game/event integration checks now include decoding the real native shop packet (three tabs/eight items, prices, costume part fields), native gold/gem purchase, insufficient funds, full bag, stale shop after OFF, migration uniqueness/admin-price preservation, actual egg100/chicken0 and flour100/beans0 mob rewards, independent Gogeta100/Omega0 costume rewards with duplicate-kill protection, and group/global/event/material OFF handling. Extended HTTP API QA checks pass for independent egg/chicken and Gogeta/Omega percentages, default restore, revision conflicts, validation and eight-decimal rates. All57 catalog rules have SQL keys and source groups. Full compile and620 packaged application classes including ConstIgnoreName verified; PHP lint and git whitespace checks pass. Native DragonBoy rendering was not controlled; native UI packet decoding and actual purchase paths were tested against game code.
Deployment: normal15-second maintenance command26 saved players, then same-console10-second restart installed SHA256 0B28504597A06FFAC38FBB3AD400E8849E08E0C35A606958D3F7D919315E2CE9; JVM28596 listens14445. Source web catalog/rules and matching SQL copied to live XAMPP after boot. Live bridge revision39 acknowledges settings, category1 and10 null-override defaults present; shop38 has exactly8items and NPC41. All event selections preserved. Backup launcher, previous web/SQL, data/rates snapshot and live verification under C:/Downloads/NroFreeSetup/Backups/trungthu-shop-rates-20261015. IAB live panel confirms collapsed Trung Thu group and all10 expanded rows with percentages/map descriptions; screenshot drop-panel-live.png is in that backup directory. QA HTTP server started only on127.0.0.1:8766 and stopped afterward.
Cleanup: removed1467 one-time/generated files (133628374bytes,12temporary build directories), including old swap/reset/grant/debug helpers and10obsolete root JARs. The root now retains only the current launcher and build-base JAR; build keeps panel-runtime, isolated panel-qa and boss-fixtures.json required by QA setup. Source, game assets/config and reusable tests/build tools retained. Archived/checksummed every removed file or matched JAR to an existing verified backup first; manifest/journal and ZIP under C:/Downloads/NroFreeSetup/Backups/cleanup-once-20261015. Removed the completed bag150 migration helper and its one-time test, updated README.md#doc-bag-inventory, added README.md#doc-cleanup. Cleanup helpers themselves are also removed. No live player/account data was reset or granted during this change. Checkpoint local only; ask before pushing.

---

<a id="doc-cleanup"></a>
## Lịch sử dọn file

## Dọn file dùng một lần — 15/10/2026

Đã dọn **1.467 file**, gồm các script reset/cấp đồ/đổi JAR đã chạy xong, công cụ debug/probe, log/ảnh kiểm tra tạm, các bản biên dịch kiểm thử cũ và 10 JAR phiên bản cũ ở thư mục gốc. Tổng dữ liệu được dọn khỏi source khoảng127 MiB. Không xóa dữ liệu game, tài khoản, tài nguyên client hay mã nguồn đang sử dụng.

Giữ lại:

- `Teamobi2026-panel-launcher.jar`: game hiện tại.
- `Teamobi2026.jar`: JAR nền cho `build-panel.ps1` biên dịch lại.
- `src`, `web`, `config`, các bộ kiểm thử dùng lại trong `test`, các script build/khởi động/triển khai.
- `build/panel-runtime`: bộ class hiện tại để build.
- `build/panel-qa`: môi trường QA có database riêng.
- `build/boss-fixtures.json`: fixture mà `test/prepare-panel-qa.php` vẫn dùng.

File chuyển túi300→150 và bộ kiểm tra chỉ phục vụ lần chuyển đó đã được dọn. Kiểm thử hành trang150 và lưu trữ vẫn giữ trong `test`.

### Khôi phục khi cần

Danh sách đường dẫn gốc, kích thước, SHA256 và nơi lưu lại nằm tại:

`C:/Downloads/NroFreeSetup/Backups/cleanup-once-20261015/manifest.json`

Các file nhỏ nằm trong `one-time-files.zip`; JAR được đối chiếu SHA256 với bản sao sẵn có hoặc lưu riêng trong `runtime-jars`. Đã kiểm tra checksum trước khi xóa bản trong source. Chỉ khôi phục file cần thiết về đúng đường dẫn trong manifest; không chạy lại script reset/cấp đồ hoặc thay JAR cũ khi chưa đối chiếu dữ liệu hiện tại.


## Lọc lịch sử giao dịch — 16/10/2026

Trang Giao dịch có bộ lọc Từ ngày / Đến ngày, kết hợp người gửi và người nhận. Chọn cùng một ngày để xem riêng ngày đó; để trống một phía để lọc một chiều. Bấm Lọc giao dịch để áp dụng hoặc Xóa bộ lọc để xem tất cả. Lọc trước phân trang, tổng số phản ánh kết quả; liên kết chuyển trang giữ các bộ lọc. Ngày kết thúc tính đến hết ngày. Đã kiểm tra 12 trường hợp trên database QA riêng, gồm biên ngày và phân trang hơn 500 dòng.


## Gọi lại Rồng Thần — 16/10/2026

Đã bỏ thời gian chờ 10 phút giữa hai lần gọi Rồng Thần thường. Sau khi rồng trước rời đi, đủ bộ ngọc có thể gọi tiếp. Vẫn giữ giới hạn một rồng đang xuất hiện, kiểm tra đủ ngọc/địa điểm và 5 phút chọn điều ước. Rồng Băng và Rồng Namek không thay đổi. Đã biên dịch toàn bộ source Java 17 và xác minh hằng số trong JAR đóng gói: hồi gọi 0 ms, chọn điều ước 300000 ms. Có backup trước triển khai tại D:/NroFreeSetup/Backups/payments-dragon-20261016.


## Đệ tử không tự tăng giáp — 16/10/2026

Khi luyện, đệ tử chỉ tự dùng tiềm năng tăng HP, KI, sức đánh và chí mạng. Không còn chọn giáp (loại chỉ số 3) trong vòng tự cộng. Giữ nguyên giáp gốc đã có, giáp trang bị, giới hạn chỉ số và cơ chế cộng chỉ số của nhân vật chính. Không sửa dữ liệu đệ tử trong database hoặc hoàn lại tiềm năng đã dùng. Đã biên dịch toàn bộ source Java 17, xác minh đủ 646 lớp ứng dụng trong JAR và đường gọi tự cộng chỉ chọn các loại 0/1/2/4. Backup: D:/NroFreeSetup/Backups/pet-no-defense-20261016.


## Chi Chi đổi skill 4 và phí mở giới hạn — 16/10/2026

Shop Chi Chi bổ sung vật phẩm 1760 (Phở Công viên Hòa Bình, đổi skill 4 đệ tử), giá 30 ngọc như cửa hàng cũ. Migration tự thêm khi chưa có, không thêm trùng hoặc ghi đè giá quản trị đã chỉnh. Giữ bảng kỹ năng/tỷ lệ đổi hiện tại, có thể trùng kỹ năng. Chỉ trừ một vật phẩm sau khi đổi thành công; chưa có đệ hoặc chiêu 4 còn khóa thì giữ nguyên đồ.

Quốc Vương hiển thị đúng phí mở giới hạn nhanh là 50 triệu vàng, gồm nút đệ tử và thông báo thiếu tiền. Không đổi giá hoặc tiền tệ thực tế. Đã qua 11 kiểm tra trên DB QA: migration, giữ giá tùy chỉnh, sử dụng vật phẩm và menu/trừ vàng thật. Backup trước nạp: D:/NroFreeSetup/Backups/pet-skill4-shop-20261016.
