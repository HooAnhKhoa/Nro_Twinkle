# Trung Thu 2026 — triển khai

Đã tích hợp source, kiểm tra trên DB QA và nạp máy chính ngày 15/10/2026. Hướng dẫn chơi, giá, giả định, bằng chứng nạp và hoàn tác nằm trong `TRUNG-THU-2026.md`. Bảng thưởng đã duyệt nằm trong `TRUNG-THU-2026-REWARD-PROPOSAL.md`; hình/ID thay thế được người dùng cho phép nằm trong `TRUNG-THU-2026-ASSETS.md`.

## Quyết định

- Bỏ toàn bộ đua top và khuyến mãi X3. X3 EXP từ hạ Khỉ vẫn là cơ chế sự kiện.
- Bật/tắt thủ công bằng công tắc web hiện có, không ép lịch cũ.
- Gogeta–Omega là hạ boss phe đối lập; không thêm cơ chế giết người chơi để nhận quà.
- Thỏ chỉ ở Siêu Thị 84. Dùng ID/hình có sẵn, không tạo ID client mới.
- Bánh hảo hạng dùng cùng bánh 890; công thức ngọc có thưởng phụ 30% như đã duyệt.
- Hộp VIP chưa triển khai theo lựa chọn đã duyệt.

## File và điểm nối

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

## Lưu và chống nhận trùng

Hai bảng mới dùng InnoDB: `trung_thu_2026_state`, `trung_thu_2026_daily`. Túi và tiền vẫn dùng cột player hiện có, cùng format JSON của `PlayerDAO`. Không có bảng top.

- Đổi nguyên liệu qua nhiều chồng, khóa theo người chơi, hoàn nguyên nếu thêm thưởng hoặc lưu lỗi.
- Lưu túi và tiền cùng transaction trước khi gửi thông báo.
- Trả nhiệm vụ lưu cả reset nhiệm vụ, thưởng và biên nhận ngày cùng transaction.
- Hoàn tất hoạt động bang lưu biên nhận và buff cho toàn bộ thành viên cùng transaction, gồm offline.
- Buff lưu hạn tuyệt đối; nhận lại lấy hạn dài hơn, offline vẫn trôi giờ. Cache bị vô hiệu khi phát buff mới.
- Nhận tiền ký gửi nguyên liệu xóa giao dịch đã bán và cộng vàng trong cùng transaction; không trả thỏi vàng thay vàng.

## Kiểm tra

- `TrungThuRuntimeTest`: 69 kiểm tra trên DB QA, có chốt tên DB để tránh ghi vào game chính.
- `TrungThuBagExchangeTest`: 13 kiểm tra giao dịch và đa luồng.
- `TrungThuAssetsTest.php`: 53 kiểm tra tài nguyên thật.
- Hồi quy Noel 39, Giỗ Tổ 73; ẩn/hiện NPC và 12 kiểm tra nhóm boss đều qua.
- Kiểm tra JAR có `ConstIgnoreName` và nạp được 617 application class.

Test cố tình gây một lỗi SQL giả (`Expected QA save failure`) để kiểm tra hoàn nguyên; thông báo đó là một phần của test.

Các kiểm tra QA không thay thế việc xem hình/menu bằng client game thật. Các giả định thực thi và cách hoàn tác được ghi rõ trong hướng dẫn chính.
