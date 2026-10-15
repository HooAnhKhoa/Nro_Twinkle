# Điểm sự kiện và Bò Mộng ở nhà

## Điểm tại cửa hàng trong ảnh

Đây là `player.event.eventPoint`, lưu vào cột `player.event_point`, dùng ở Quy Lão Kame → Đổi điểm sự kiện (`SHOP_DOI_DIEM`, tab 59). Không phải điểm danh hoặc trường `inventory.coupon`.

Cách có điểm trong source hiện tại:

- Người kết liễu nhiều boss được 5 điểm/lần: Kuku, Mập Đầu Đinh, Rambo; Android 13/14/15/19, Dr. Kôrê, Píc/Pọc; Fide, Cooler; các Xen con và Siêu Bọ Hung; Bojack và nhóm; Black Goku, các boss Mabư. Mỗi lớp boss có hàm reward riêng; không mặc định mọi boss đều cho điểm.
- Sơn Tinh/Thủy Tinh cho 5 điểm khi sự kiện Giỗ Tổ đang bật và phần thưởng boss hợp lệ.
- Ăn Trộm cho 5 điểm khi đã trộm được vàng (`goldAnTrom != 0`).
- Đổi số dư VNĐ sang ngọc hoặc thỏi vàng: `(số VNĐ / 10.000) * 50` điểm, lấy phần nguyên. Ví dụ 10.000 VNĐ được 50 điểm; 20.000 VNĐ được 100 điểm. Chỉ nạp số dư mà chưa đổi chưa chạy hai nhánh cộng điểm này.

Ví dụ giá đổi hiện tại: Black Goku Rose 999; Cân Đẩu Vân Thơ Mộng 750; Khủng Long Thơ Mộng, Hải Ly, Urôn 499; hộp Goku/Cađíc VIP 99. Điểm hiện có nằm trong nhãn menu Đổi điểm sự kiện ở Quy Lão.

Mã liên quan: `src/nro/models/player/PlayerEvent.java`, `src/nro/models/services_func/Input.java` (TRADE_GOLD/TRADE_GEM), `src/nro/models/shop/ShopService.java` (tab 59), các hàm `reward` của từng boss.

## Bò Mộng

Dữ liệu đã đặt NPC 17 ở Nhà Gôhan (21), Nhà Moori (22), Nhà Broly (23), Rừng Karin (47), Siêu Thị (84). Nhưng cả hàm mở và xử lý menu chỉ chấp nhận map 47/84 ngay từ bản source nhập ban đầu. Vì vậy NPC ở ba nhà hiện mà không mở được chức năng.

Khôi phục bằng cách cho phép ba map nhà dùng cùng luồng dịch vụ cũ:

- Nhiệm vụ hằng ngày: chọn Dễ/Bình thường/Khó/Siêu khó, xem tiến độ, trả hoặc hủy. Giới hạn gốc 10 nhiệm vụ/ngày.
- Nhiệm vụ thành tích: giao diện thành tích sẵn có.
- Nạp Ngọc: form đổi số dư VNĐ sang ngọc, có cộng điểm sự kiện theo công thức trên.
- Điểm danh: 10.000 ngọc + 100 thỏi vàng, kiểm tra không nhận lại cùng ngày. Bản khôi phục giữ nguyên phần thưởng.
- Khi bật Trung Thu, trả nhiệm vụ Khó/Siêu khó vẫn đi qua nhánh TrungThuQuest hiện có để nhận thêm đèn treo/hạt sen theo hạn mỗi ngày.

`test/BoMongHomeTest.java`: 52 kiểm tra không kết nối DB, gồm cả ba nhà và hai vị trí cũ; mở menu, nhận nhiệm vụ, tiến độ/hủy, thành tích, form đổi ngọc, điểm danh và chống nhận lại; từ chối map ngoài phạm vi và NPC khác map.
