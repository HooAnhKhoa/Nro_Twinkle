# Hành trang tối đa 100 ô

Hành trang có giới hạn **100 ô liền nhau**, dùng giao diện túi gốc của client, không chia trang. Rương đồ giữ nguyên tối đa 100 ô.

- Túi trên 100 ô được thu gọn sau khi game bảo trì lưu và dừng. Giữ nguyên toàn bộ vật phẩm, số lượng, chỉ số và thứ tự tương đối; chỉ bỏ ô rỗng. Nếu thực tế hơn 100 ô có đồ thì từ chối chuyển dữ liệu và không nạp JAR mới.
- Túi đã mở ít hơn hoặc bằng 100 ô giữ nguyên kích thước. Vật phẩm mở rộng hành trang 517 tăng từng ô đến 100 rồi từ chối mở thêm.
- Gói đăng nhập/cập nhật gửi toàn bộ các ô đang có, tối đa 100. Chỉ số hợp lệ 0–99. Giữ cách đọc unsigned và sentinel255/-1 cho các lệnh gốc; không thay giao thức thành 2 byte.
- Giữ kiểm tra xác nhận bán/dùng/bỏ đồ một lần và cột LONGTEXT để lưu đầy đủ vật phẩm nhiều chỉ số.

## Kiểm tra

`test/BagInventory100Test.java`: 30 kiểm tra gói native và các thao tác tại ô80/90/99, gồm dùng, dùng theo ID, mặc, bỏ, bán, cất rương, giao dịch, ký gửi, ghép, ô rỗng, túi cũ nhỏ hơn và giới hạn 100.

`test/GiftCodeRedemptionTest.java`: hành trang mẫu 100 ô, kiểm tra trao thưởng/lưu dữ liệu, đầy túi, nhận trùng và hoàn tác lỗi trên DB QA riêng. `test/BagStorageTest.java` giữ kiểm tra lưu tiếng Việt và hơn64KB trong bảng tạm QA.

Công cụ chuyển chạy một lần được lưu trong bản sao để hoàn tác, không để lại trong source. Có kiểm tra bảo toàn món ở ô100/149 cũ, túi nhỏ hơn, túi đúng100, túi rỗng và từ chối hơn100 món.

## Bản sao để hoàn tác

`C:/Downloads/NroFreeSetup/Backups/bag100-20261015` giữ JAR150 cũ, dữ liệu hành trang được đọc lại sau khi JVM lưu và dừng, biên nhận chuyển100 và kết quả kiểm tra. Chỉ thay JAR sau khi chuyển dữ liệu thành công; nếu chuyển thất bại, giữ JAR cũ để launcher khởi động lại bình thường.

Các bản sao lịch sử 300→150 vẫn giữ tại `Backups/bag150-20261015` và `Backups/cleanup-once-20261015`. Giữ LONGTEXT khi quay lại JAR cũ. Không phục hồi đè bản sao túi lên dữ liệu sau khi người chơi đã nhận, dùng hoặc giao dịch thêm vật phẩm.
