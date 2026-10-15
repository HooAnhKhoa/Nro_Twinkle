# Trung Thu 2026 — phạm vi và tiến độ

## Quyết định đã chốt

- Bỏ toàn bộ đua top, bảng xếp hạng, chốt top, thưởng top và khuyến mãi X3.
- Thử nghiệm bằng công tắc Trung Thu hiện có trên web; lịch 21/08–21/09 là cấu hình tùy chọn, không ép áp dụng trong chế độ thử.
- Gogeta–Omega là hạ boss đối lập, không phải cơ chế giết người chơi.
- Ngày 15/10/2026 người dùng cho phép thay bằng tài nguyên tương đương hiện có. Mapping đã chốt trong `TRUNG-THU-2026-ASSETS.md`; không cấp ID mới hoặc sửa template dùng chung.
- Thỏ Đại Ca chỉ xuất hiện ở Siêu Thị84 trong đợt này.
- Người dùng đã duyệt bảng thưởng đề xuất ngày 15/10/2026, gồm tỷ lệ nguyên liệu, phần thưởng và HP/sát thương boss. Chưa áp dụng vào game, vẫn chờ đủ ID/hình chính xác.

## Đối chiếu ID thật

| Mục | ID | Ghi chú |
|---|---:|---|
| Ánh trăng tròn | 1305 | Vật phẩm có sẵn, 10 ngọc theo đặc tả |
| Thỏ ngọc | 1310 | Có sẵn, 10 triệu vàng |
| Đuôi khỉ | 1045 | Hiện đang dùng như buff EXP cũ; phải tách/điều chỉnh nhánh sử dụng khi nối cơ chế mới |
| Củ cà rốt | 462 | Đúng tên; 192 và 670 là các template cà rốt khác |
| Lồng đèn treo | 1311 | Đúng vật phẩm nguyên liệu treo đèn |
| Trứng vịt muối / Gà quay nguyên con | 886 / 887 | Cặp nguyên liệu Trung Thu có sẵn |
| Bột mì / Đậu xanh | 888 / 889 | Không nhầm với Đậu xanh 1548 của Giỗ Tổ |
| Hạt sen | 1312 | Có sẵn |
| Bánh gà quay / Thập cẩm / Hạt sen | 890 / 891 / 1313 | Chưa có bánh gà quay hảo hạng đúng tên |
| Thỏ xám / Thỏ trắng / Mèo đen đuôi vàng | 892 / 893 / 1188 | Có template và nhánh sử dụng pet trong source |
| Kiếm gỗ / Cải trang Gogeta | 1304 / 1697 | Có sẵn |
| Cải trang thỏ | 1042 / 1043 | Thỏ đỏ Chi Chi / Thỏ đen Android 18 |
| NPC Trung Thu / Giùma đầu bò | 41 / 47 | NPC47 là NPC đã có ở Lãnh địa Bang Hội, phù hợp vai trò Ngưu Ma Vương |
| NPC Bò Mộng / Quy Lão | 17 / 13 | Có sẵn |
| Thung lũng đen / Vách núi đen / TP Vegeta | 37 / 20 / 19 | Đã xác minh map thật |
| Siêu Thị / Lãnh địa Bang Hội | 84 / 153 | Có sẵn |

Chưa tìm thấy đúng tên Đảo Santa: chỉ có Thành phố Santa126 và149, và người dùng đã chọn chỉ dùng Siêu Thị trước. Các template Lồng đèn lon1047 và Hộp bánh1701 không tự coi là Lồng Đèn 2 Lon hoặc hộp2026 vì chưa đúng đặc tả; item1701 còn mô tả đua top cũ.

## File/class sẽ thêm hoặc sửa

| Cơ chế | Thêm | Điểm nối hiện có |
|---|---|---|
| Cấu hình, trạng thái | `TrungThu2026Config`, `TrungThu2026State`, schema SQL, config mẫu | `Manager`, `EventManager`, nạp nhân vật trong `MrBlue` |
| Khỉ triệu hồi, X3 EXP | `MoonMonkey`, dịch vụ triệu hồi, cấp buff cho thành viên kể cả offline | `UseItem`, `Boss`, `SkillService`, `EffectSkillService`, `NPoint`, `Clan` |
| Thỏ Đại Ca | `RabbitBoss`, trạng thái hóa cà rốt theo timestamp | `UseItem`, `NPoint`, `Player`, dịch vụ hiệu ứng |
| Treo đèn | NPC Cây Lồng Đèn, bảng thưởng cấu hình | `NpcFactory`, `TaskService.paySideTask`, `BoMong` |
| Gogeta–Omega | Hai boss và lựa chọn phe theo người chơi | NPC Trung Thu, `SkillService`, `UseItem`, `Boss` |
| Nấu bánh / đổi pet | `TrungThuCrafting`, lõi `BagExchange` | NPC Trung Thu, `ItemService`, `InventoryService` |
| Rước đèn / điểm danh | Dịch vụ phiên bang hội, receipt ngày và buff | `QuyLaoKame`, `GiuMaDauBo`, `Clan`, `NPoint` |
| Shop / hộp bánh | Shop riêng, bảng thưởng hộp | `ShopService`, `UseItem`, schema SQL |
| Ký gửi bằng vàng | Guard vật phẩm Trung Thu theo config | `ConsignShopService` |

Không sửa các phần không liên quan. EXP cần tính theo thời điểm hết hạn tuyệt đối để thời gian offline cũng trôi; buff sự kiện lấy hệ số cao nhất và không cộng với bùa trí tuệ/X2 liên quan. Đuôi/cà rốt nằm trong túi theo cơ chế lưu vật phẩm hiện có; trạng thái buff và nhận thưởng ngày lưu riêng trong DB.

## Phần nền đã viết và kiểm tra

- `src/nro/models/event/trungthu2026/BagExchange.java`: giao dịch trong bộ nhớ, khóa theo người chơi; đếm nguyên liệu qua nhiều chồng; kiểm tra vàng/ngọc; khôi phục cả túi và tiền nếu thêm thưởng thất bại; chống bấm liên tiếp theo thời gian cấu hình.
- `test/TrungThuBagExchangeTest.java`: 13 kiểm tra, gồm rollback khi đã cộng dở vào chồng vật phẩm và khi thưởng phụ không còn chỗ; cạnh tranh hai luồng; bảo toàn metadata của item.
- `config/trung-thu-2026.properties`: các ID, công thức, giá và thời gian đã xác minh, gồm tài nguyên thay thế được người dùng cho phép.
- `test/TrungThuAssetsTest.php`: 53 kiểm tra đúng loại template, icon ở bốn mức zoom, hoạt ảnh thú cưỡi và tách riêng hai hộp; chỉ đọc DB và ảnh thật, không sửa dữ liệu.

Lõi BagExchange chưa xử lý lưu DB/receipt hoặc gửi gói tin; đó là trách nhiệm dịch vụ cơ chế khi tích hợp. Chưa đăng ký các class mới vào game đang chạy.

## Cần chốt trước khi hoàn thiện

1. Tài nguyên đã đủ theo phương án tương đương: lồng đèn1047, thú cưỡi735, cải trang1685, bánh890, hộp1316/1317; treo đèn qua NPC41. Không còn chờ ID/hình người dùng. Cần tích hợp các cơ chế còn lại và kiểm thử trước khi nạp game.
2. Bảng thưởng, tỷ lệ vật phẩm vĩnh viễn và HP/sát thương boss đã duyệt trong `TRUNG-THU-2026-REWARD-PROPOSAL.md`, chưa có hiệu lực trong game. Hộp VIP2026 tạm không triển khai theo đề xuất đã duyệt; muốn bổ sung sau cần chốt công thức/giá.
3. Map Thỏ đã chốt: chỉ Siêu Thị84; không còn chờ chọn map Santa.
4. Các mặc định cần duyệt cùng bảng thưởng: kỹ năng cơ bản là đấm/chưởng của ba hành tinh; nguyên liệu dùng cặp886/887; thời gian buff không cộng nối vô hạn, nhận lại lấy hạn dài hơn; ảnh hóa cà rốt phải có tài nguyên được xác minh; không giả báo hiệu ứng hình ảnh nếu chưa có.

## Cách kiểm tra phần nền

Biên dịch BagExchange và test vào thư mục build riêng, đặt classes mới trước JAR hiện tại trong classpath. Test dùng template giả trong bộ nhớ, không mở server và không chạm DB game chính. Các kiểm tra cơ chế tích hợp sau đó phải dùng DB QA, rồi mới backup và nạp game thật.
