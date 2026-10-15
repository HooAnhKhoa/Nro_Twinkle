# Quản lý Tori Bot

Menu **Tori Bot** trên panel quản lý mùa VIP và 4 gói đang có trong source.

- Bật/tắt bán, thời gian bắt đầu/kết thúc theo giờ Việt Nam, mã mùa và giới hạn mua mỗi nhân vật.
- Từng gói chỉnh giá VNĐ, số ngày VIP, EXP thưởng thêm, tặng đệ tử thường khi chưa có.
- Thêm/xóa vật phẩm bằng kho ảnh/tên/ID, chỉnh số lượng, option và hạn dùng (option 93).
- Lịch sử cấu hình có hoàn tác lần thay đổi gần nhất. Mỗi lần lưu tạo revision mới, tránh ghi đè giữa hai phiên. Game nhận cấu hình qua PanelBridge mỗi 3 giây; web báo xác nhận.
- Lịch sử mua ghi nhân vật, gói, giá và toàn bộ cấu hình thưởng tại thời điểm mua. Hoàn tác cấu hình không thu hồi giao dịch đã thực hiện.

## Quy tắc

Giữ giá/phần thưởng thực tế của 4 gói cũ làm mặc định. EXP mặc định +300% giữ đúng mức code cũ (phần nền X4), các hệ số khác tính riêng. Giá trừ **account.vnd**, không phải vàng hay ngọc. VIP 4 có trứng đệ tử trong phần thưởng; không tự tạo đệ tử Mabu.

Bật/tắt chỉ điều khiển bán. Ngoài lịch mùa không mua được và không hưởng EXP của mùa. Thời hạn VIP nối từ ngày hết hạn hiện tại hoặc hôm nay, tối đa cuối mùa. Thời hạn vật phẩm độc lập với hạn VIP. Khi mua gói thấp hơn, cấp VIP và EXP đang có giữ mức cao hơn. Không thay đổi cơ chế VIP tài khoản khác.

Đổi **mã mùa** đặt lại cấp VIP, hạn và lượt mua của mùa trước khi nhân vật mở Tori Bot hoặc đăng nhập; EXP mùa cũ ngừng ngay. Giữ nguyên mã mùa khi chỉ điều chỉnh lịch hoặc phần thưởng. Dữ liệu cũ chưa có mã mùa được nhận vào mùa hiện tại, giữ số lượt cũ.

Mua yêu cầu xác nhận với đúng revision; đóng menu không mua. Khóa dòng cấu hình, tài khoản và nhân vật trong transaction InnoDB. VNĐ, hành trang, VIP, đệ tử mới và lịch sử mua cùng commit; lỗi SQL/thiếu túi không trừ tiền hay trao thưởng. Từ chối phiên nhân vật có số lượt cũ. Đệ tử đang có giữ nguyên.

## Kiểm tra

- `test/ToriVipTest.java`: 52 kiểm tra trên DB QA riêng, 4 gói, phần thưởng, đệ tử, giới hạn, lỗi SQL/đầy túi/thiếu tiền, dữ liệu cũ, đóng menu, revision thay đổi và mùa mới.
- `test/ToriVipApiTest.ps1`: tìm đồ thật, phân trang, lưu giá/hạn/EXP, từ chối revision cũ và dữ liệu sai, hoàn tác.
- Kiểm tra lại PointShop (14), đổi VNĐ (25), bánh Trung Thu (33), Himmel (79).
- Biên dịch toàn bộ source Java 17 và kiểm tra các lớp trong JAR đóng gói; không đóng gói lớp test.

Chạy Java test từ `build/panel-qa` với cấu hình QA và truyền đường dẫn repo cho ToriVipTest. API test dùng PHP QA localhost:8766, có chặn nhầm DB thật. Không chạy test bằng Config.properties của game thật.

## Khôi phục bản trước

Backup triển khai tại `D:/NroFreeSetup/Backups/toribot-20261016`: JAR trước, sidebar trước và trạng thái bảng Tori trước. Muốn lùi runtime: bảo trì lưu dữ liệu, đổi JAR trong khoảng chờ khởi động, khôi phục sidebar và gỡ các file web mới theo manifest. Giữ bảng lịch sử mua để đối soát; không hoàn tác dữ liệu người chơi hay VNĐ sau khi có giao dịch. Cấu hình thường dùng nút Hoàn tác trên web.