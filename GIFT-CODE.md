# Nhập giftcode trong game

Santa → Nhập mã quà tặng. Mã được đọc trực tiếp từ bảng giftcode khi xác nhận; tạo/sửa mã trên web có hiệu lực ngay, không cần khởi động lại hoặc bấm Update Code. Không phân biệt hoa/thường và bỏ khoảng trắng ở hai đầu.

Luồng nhận kiểm tra hạn hiện tại, ngày bắt đầu, lượt còn lại và lịch sử nhận từ cả DB lẫn phiên chơi. Phần thưởng, túi, tiền, lịch sử nhận và lượt còn lại lưu trong cùng transaction InnoDB. Hết chỗ, tràn tiền, mã lỗi hoặc lỗi lưu đều không tiêu hao mã. Mã có count_left=-1 giữ nguyên chế độ không giới hạn. Giữ nguyên lịch sử đã nhận của người chơi.

Dùng cùng cơ chế hoàn nguyên túi/tiền BagExchange và bộ tuần tự hóa túi hiện có. Mỗi phần thưởng giữ ID, số lượng và option riêng; hỗ trợ các mã tiền -1 (vàng), -2 (ngọc xanh), -3 (hồng ngọc). Vật phẩm có tác dụng đặc biệt ngoài túi chưa được cơ chế trao thưởng an toàn hỗ trợ sẽ bị từ chối toàn bộ, giữ mã cho người chơi.

`test/GiftCodeRedemptionTest.java` chỉ chạy trong DB `nro_panel_qa_20261013`. Kiểm tra mã mới/đổi tên không nạp cache, nhập mã Ngọc Rồng 1–7 sao, form Santa khi có/không phiếu giảm giá, packet nhập mã, nhận trùng/reconnect, hạn/ngày bắt đầu, hết lượt/không giới hạn, túi đầy/gộp chồng, tiền/tràn tiền, lỗi SQL và tranh lượt cuối. Lỗi SQL giả trong test được tạo có chủ đích để xác minh hoàn nguyên.
