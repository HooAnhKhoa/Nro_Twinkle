# Tài nguyên thay thế Trung Thu 2026

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

## Ảnh hưởng và giới hạn

- Giữ nguyên tỷ lệ thưởng, thời hạn và giá đã duyệt; chỉ thay vật phẩm nhận được bằng tài nguyên tương đương nêu trên.
- Không chạy SQL sửa template, không đổi ảnh hoặc tên vật phẩm của người chơi đang có.
- Hai công thức bánh gà quay tạo cùng ID 890 nên dùng chung khi đổi pet thỏ xám. Phần phân biệt công thức trả ngọc là 30% thưởng bánh thập cẩm.
- Giỏ bánh 1316 và 1317 hiện chưa có nhánh mở trong source. Cần nối nhánh mở với hai bảng thưởng riêng khi triển khai cơ chế shop; chỉ sửa config chưa làm chúng mở được trong game.
- Kiểm tra này xác nhận tài nguyên đầu vào. Sự kiện Trung Thu 2026 mới chưa được tích hợp đầy đủ, chưa build/nạp vào game chính.

## Kiểm tra

`test/TrungThuAssetsTest.php` đọc config đã chốt và DB, xác minh đúng loại item, bộ phận cải trang, hoạt ảnh thú cưỡi, các file icon và việc tách riêng hai hộp. Test chỉ đọc DB; không khởi động server hay sửa tài khoản.
