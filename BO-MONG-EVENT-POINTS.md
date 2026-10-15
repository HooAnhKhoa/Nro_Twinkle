# Điểm sự kiện và Bò Mộng ở nhà

## Điểm tại cửa hàng trong ảnh

Đây là `player.event.eventPoint`, lưu vào cột `player.event_point`, dùng ở Quy Lão Kame → Đổi điểm sự kiện (`SHOP_DOI_DIEM`, tab 59). Không phải điểm danh hoặc trường `inventory.coupon`.

Cách có điểm trong source hiện tại:

- Người kết liễu nhiều boss được 5 điểm/lần: Kuku, Mập Đầu Đinh, Rambo; Android 13/14/15/19, Dr. Kôrê, Píc/Pọc; Fide, Cooler; các Xen con và Siêu Bọ Hung; Bojack và nhóm; Black Goku, các boss Mabư. Mỗi lớp boss có hàm reward riêng; không mặc định mọi boss đều cho điểm.
- Sơn Tinh/Thủy Tinh cho 5 điểm khi sự kiện Giỗ Tổ đang bật và phần thưởng boss hợp lệ.
- Ăn Trộm cho 5 điểm khi đã trộm được vàng (`goldAnTrom != 0`).
- Đổi số dư VNĐ sang ngọc hoặc thỏi vàng: `(số VNĐ / 10.000) * 5.000` điểm, lấy phần nguyên. Ví dụ 10.000 VNĐ được 5.000 điểm; 20.000 VNĐ được 10.000 điểm. Chỉ nạp số dư mà chưa đổi chưa chạy hai nhánh cộng điểm này.

Ví dụ giá đổi hiện tại: Black Goku Rose 999; Cân Đẩu Vân Thơ Mộng 750; Khủng Long Thơ Mộng, Hải Ly, Urôn 499; hộp Goku/Cađíc VIP 99. Điểm hiện có nằm trong nhãn menu Đổi điểm sự kiện ở Quy Lão.

Mã liên quan: `src/nro/models/player/PlayerEvent.java`, `src/nro/models/services_func/Input.java` (TRADE_GOLD/TRADE_GEM), `src/nro/models/shop/ShopService.java` (tab 59), các hàm `reward` của từng boss.

## Bò Mộng

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