# CT Himmel và option 156

Bản sửa thay cách tính cũ của option 156 bằng cơ chế cộng dồn theo đúng tên trong item_option_template: mỗi đòn đấm cơ bản gây sát thương tăng 1% sức đánh, tối đa theo param. Áp dụng cho Dragon, Demon, Galick, gồm đánh quái và đánh người chơi/boss. Đòn hụt hoặc sát thương bằng 0 không tăng. Phần cộng dồn có hiệu lực sau đòn trúng, cho đòn kế tiếp.

Dùng chiêu khác xóa chuỗi trước khi tính sát thương của chiêu đó; đổi món cung cấp hiệu ứng, đổi param, chết hoặc đăng nhập lại bắt đầu ở 0. Nếu nhiều nguồn có option này thì lấy trần cao nhất, không cộng trần. Radar đang dùng cũng tuân thủ điều kiện kích hoạt option như bộ tính chỉ số. Trạng thái chuỗi nằm riêng trên từng NPoint, không lưu SQL và không thay dữ liệu vật phẩm.

Option 156 không còn trừ 50% sức đánh hoặc cộng SM/TN/vàng trái mô tả. Cute 117/226 chỉ được áp dụng một lần thay vì hai lần trong setDame. Các cơ chế giảm sức đánh khác như giáp luyện tập vẫn giữ phép tính riêng.

Bộ CT Himmel ở shop điểm có SD25%, HP25%, chí mạng20%, Cute25% và trần cộng dồn25%. Không có option KI nên không tự thêm KI. Chỉ số hiển thị và gói tin -42 được kiểm tra qua tuyến mặc/tháo thực tế.

## Kiểm tra

`test/HimmelPunchComboTest.java` chạy với database QA, cần class test/TrungThuRuntimeTest.java cùng classpath JAR và lib/*. Lấy cấu hình từ thư mục build/panel-qa. Có thể truyền một file JSON snapshot local gồm gender/data_point/items_body để kiểm lại trang bị cụ thể; snapshot người chơi không đưa vào Git. Bài kiểm tra gồm chỉ số thực, packet client, đòn trúng/hụt PvE/PvP, đổi chiêu, trần khác nhau, nhiều nguồn, đổi đồ, chết và phiên mới.

Đã qua 80 kiểm tra cùng snapshot người chơi, hồi quy PointShop 14, TrungThuCakeBuff 33, BossDrops 19. JAR xác nhận đủ ConstIgnoreName và 638 class ứng dụng nạp được trên Java 17.

## Triển khai và hoàn tác

Game đang chạy ở D:\NroFreeSetup\Work\SourceGame-441D49A9989D. Bản trước được giữ ở D:\NroFreeSetup\Backups\himmel-stats-20261015\launcher-before.jar; deployment.json lưu SHA256 trước/sau và log kiểm thử ở cùng thư mục. Đã bảo trì/lưu 15 giây, thay JAR khi JVM dừng và launcher tự chạy lại sau 10 giây. Không chỉnh vật phẩm hay tài khoản trong database máy chính.

Để hoàn tác: bảo trì/lưu, chờ JVM dừng, khôi phục launcher-before.jar vào Teamobi2026-panel-launcher.jar trước lần chạy kế tiếp. Không khôi phục database người chơi.