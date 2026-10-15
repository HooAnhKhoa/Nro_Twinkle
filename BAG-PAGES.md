# Túi đồ 300 ô, chia 3 trang

- Server hỗ trợ tối đa 300 ô hành trang, chia 3 trang, mỗi trang 100 ô. Rương đồ giữ giới hạn 100 ô.
- Chat `tui` để chọn trang; `tui 1`, `tui 2`, `tui 3` để chuyển nhanh. Cũng nhận `/tui 2` và `tui2`.
- Có nút **Trang Túi đồ** tại Santa, sau các nút cũ và mục Giáng sinh nếu sự kiện đang bật.
- Nhân vật chỉ thấy các ô đã mở. Vật phẩm mở rộng hành trang 517 tiếp tục tăng từng ô, tối đa 300; nhân vật khác không tự được cộng thêm ô.
- Khi đăng nhập, bắt đầu ở trang 1. Túi vẫn được lưu nguyên một danh sách trong database; chuyển trang không chuyển quyền sở hữu hoặc xóa vật phẩm.
- Dùng, mặc, bỏ, bán, cất rương, trang bị đệ tử, giao dịch, ký gửi và ghép đồ đều đổi chỉ số ô của trang hiện tại sang vị trí thật trong túi. Không gửi số 300 qua trường 1 byte của client.
- Khi đang giao dịch không được chuyển trang. Khi chuyển trang, hủy xác nhận dùng/bán/bỏ đồ cũ và lựa chọn ghép đồ cũ để tránh tác động nhầm vật phẩm.
- Ghép đồ thủ công cần chọn các vật phẩm trên cùng một trang. Ký gửi hiển thị các vật phẩm có thể đăng bán trên trang đang xem; các món đã đăng vẫn giữ nguyên.
- Hệ thống vẫn dồn các vật phẩm về trước như code cũ sau khi cập nhật túi. Nhận vật phẩm mới sẽ lấp ô trống đầu tiên trong toàn túi; nếu trang đang xem trống hãy kiểm tra trang 1.

## Lưu dữ liệu

`BagPages.ensureStorage` chạy trước khi nạp dữ liệu game, nâng `player.items_bag` từ TEXT lên LONGTEXT khi cần. Giữ nguyên collation, khả năng NULL và chú thích; không thu nhỏ các cột MEDIUMTEXT/LONGTEXT có sẵn. Túi đủ 300 món nhiều chỉ số có thể vượt 65.535 byte của TEXT cũ.

## Kiểm tra

`test/BagPagesTest.java`: gói đăng nhập và cập nhật túi đúng định dạng client cũ; đi đủ 300 ô, trang chưa mở/trang lẻ/ô rỗng; dùng, mặc, cất rương, bỏ, bán, xác nhận cũ/lặp lại, giao dịch, vàng giao dịch, ghép, ký gửi và giới hạn mở ô. Không nối database thật.

`test/BagStorageTest.java`: bảng TEMPORARY chỉ tồn tại trong kết nối QA, kiểm tra nâng cột lặp lại không sai, giữ nguyên dữ liệu tiếng Việt và lưu được hơn 64 KB. Kiểm thử Trung Thu dùng database QA riêng.

## Tài khoản thử và hoàn tác

Đã cấp 26 loại vật phẩm thử Trung Thu cho tài khoản tên đăng nhập `11`, nhân vật `222552`; không nhầm với account ID 11. Bản sao trước cấp và biên nhận trong `C:/Downloads/NroFreeSetup/Backups/trung-thu-test-account11-20261015`.

Bản sao JAR, cấu hình và dữ liệu trước nâng 300 ô trong `C:/Downloads/NroFreeSetup/Backups/bag-pages-20261015-144724`. Lưu dữ liệu mới nhất và dừng game trước khi hoàn tác. Không chạy JAR cũ với túi 300 ô: phải bảo toàn các món ở cả ba trang rồi chuyển về định dạng/dung lượng cũ an toàn. Có thể giữ cột LONGTEXT khi hoàn tác mã nguồn; không thu nhỏ cột nếu dữ liệu đã vượt 64 KB. Không phục hồi đè bản sao túi cũ sau khi người chơi đã nhận/dùng/giao dịch thêm đồ.
