# Hành trang 150 ô

Hành trang có giới hạn **150 ô liền nhau**, dùng giao diện túi gốc của client. Đã bỏ chia trang, lệnh `tui` và nút Trang Túi đồ tại Santa. Rương đồ giữ nguyên giới hạn 100 ô.

- Nhân vật `222552`, tài khoản đăng nhập `11`, được chuyển từ 300 ô về 150 ô sau khi bảo trì lưu dữ liệu mới nhất; giữ nguyên đồ thử Trung Thu và các món còn lại.
- Nhân vật khác giữ số ô đã mở; vật phẩm mở rộng hành trang 517 tiếp tục tăng từng ô đến tối đa 150.
- Đọc chỉ số ô dạng unsigned cho dùng đồ, cất rương, mặc đồ, đệ tử và ghép đồ. Giao dịch đổi byte có dấu về chỉ số 0–149, vẫn giữ byte255/-1 cho vàng; ký gửi giữ chỉ số149 ở dạng số nguyên dương.
- Gói đăng nhập và cập nhật hành trang gửi toàn bộ các ô đã mở, tối đa150, cùng các ô rỗng đúng định dạng. Không thay giao thức thành 2 byte.
- Giữ kiểm tra xác nhận bán/dùng/bỏ đồ một lần và cột LONGTEXT để lưu đầy đủ vật phẩm nhiều chỉ số.

## Kiểm tra

`test/BagInventory150Test.java`: 30 kiểm tra gói native và các thao tác tại ô127/128/149, gồm dùng, dùng theo ID, mặc, bỏ, bán, cất rương, giao dịch, ký gửi, ghép, ô rỗng, túi cũ nhỏ hơn và giới hạn150.

`test/Bag150CompactionTest.php`: giữ món tại ô299 khi thu gọn, bảo toàn toàn bộ bản ghi vật phẩm, từ chối nếu có hơn150 món, giữ các ô cũ khi tăng túi nhỏ hơn và kiểm tra chạy lặp lại. Hàm thu gọn thuần dữ liệu trong `tools/bag150-compaction.php`, không kết nối database.

`test/BagStorageTest.java`: bảng tạm trong kết nối QA, xác minh giữ nguyên dữ liệu tiếng Việt và lưu hơn64KB. Luồng Trung Thu được kiểm tra trên database QA riêng.

## Bản sao để hoàn tác

`C:/Downloads/NroFreeSetup/Backups/bag150-20261015` chứa JAR300 cũ, cấu hình, dữ liệu túi đã lưu trước thu gọn và biên nhận. Thao tác database chỉ chạy sau khi JVM cũ lưu và dừng; nếu số vật phẩm thực vượt150 thì từ chối thay đổi, không cắt bỏ vật phẩm.

Giữ LONGTEXT khi quay lại JAR cũ. Không phục hồi đè bản sao túi lên dữ liệu sau khi người chơi đã nhận, dùng hoặc giao dịch thêm vật phẩm. Việc quay lại chế độ300 phải nạp JAR tương ứng trước khi mở lại300 ô.
