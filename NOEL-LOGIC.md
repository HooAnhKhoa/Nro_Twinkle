# Sự kiện Noel

## Luồng hoạt động

1. Bật **Giáng sinh** trên panel. Có 30 Ông già Noel, tối đa một ông trong mỗi khu. Đây là nhân vật phát quà, không phải boss để giết; không nhận sát thương.
2. Đến Santa ở Đảo Kamê, Đảo Guru hoặc Vách núi đen → **Giáng sinh** → **Mua tất Giáng sinh**. Shop riêng bán vật phẩm 649, giá khởi đầu 20 ngọc như shop Bulma sự kiện cũ. Giá chỉnh trên panel được giữ qua restart.
3. Đến gần Ông già Noel trong phạm vi 50. Sau mỗi 60 giây, ông xét ba lượt thả độc lập với xác suất 1/3, 1/5 và 1/7; mỗi lượt thành công thả một hộp 648. Không đảm bảo phút nào cũng có quà.
4. Nhặt mỗi hộp tiêu hao một tất. Túi đầy hoặc không đủ tất thì giữ nguyên cả tất và hộp trên đất. Quà công khai, người nhặt thành công trước nhận; không thể nhặt lại cùng một hộp.
5. Mở hộp trong túi để nhận quà. Hộp đã nhận vẫn dùng được sau khi tắt EV.

Ông già Noel ở map 100–300 giây, sau đó nghỉ theo cấu hình boss (gốc 60 giây). Gọi boss từ web cũng khởi tạo đầy đủ thời gian. Santa thường và các cửa hàng thường vẫn hoạt động khi tắt Noel; mục Giáng sinh và shop riêng chặn thao tác cũ khi EV tắt.

## Phần thưởng hộp 648 đang có trong source

- 5/90 cơ hội nhận 10–20 ngọc xanh.
- Nếu không trúng ngọc, chọn đều một trong bảy nhóm: sao pha lê 441–445; đá nâng cấp 381–384; ngọc rồng 17–20; ngọc rồng băng 925–931; mảnh thiên sứ 1066–1070; kẹo Giáng sinh 533; capsule 380.
- Sao pha lê, đá nâng cấp và mảnh thiên sứ nhận 1–5 vật phẩm, các nhóm còn lại nhận một. Giữ nguyên nhóm và chỉ số sao từ source cũ.
- Không thêm nón, cây thông, tuần lộc hay cải trang vào hộp 648: code cũ có nhánh xử lý chúng nhưng danh sách quay không hề chọn chúng. Gói quà 1170 là vật phẩm khác với danh sách thưởng khác; không tự đổi danh sách đó thành quà Noel.
- Tràn ngọc hoặc không đủ chỗ nhận thì không mất hộp. Chỉ tiêu hao hộp sau khi cấp phần thưởng thành công.

## Các lỗi đã sửa

- Kiểm tra trùng boss bằng quần thể sai và hai vòng chọn khu không xét đồng thời các điều kiện.
- Nhánh gọi boss thủ công không khởi tạo thời gian rời map.
- Tạo ItemMap trước khi kiểm tra xác suất khiến vật phẩm thất bại vẫn nằm trong danh sách map.
- Ông già Noel không tiến lại khi người chơi cách 50–500, hoặc chọn mục tiêu chết/thú thay cho người chơi sống.
- Pickup đánh dấu quá muộn, trừ tất trong phần gửi gói tin; tách riêng để cấp quà/trừ tất/đánh dấu nhận diễn ra trước khi gửi thông báo.
- Hộp quà cũ trừ hộp trước khi thêm thưởng và cộng ngọc có thể vượt giới hạn int.

Các nguồn chính: OngGiaNoel.java, ChristmasGifts.java, Santa.java, Zone.java, UseItem.java, ShopService.java và web/noel_schema.sql. Schema chạy trước khi nạp shop, chỉ thêm dữ liệu còn thiếu.
