# Dọn file dùng một lần — 15/10/2026

Đã dọn **1.467 file**, gồm các script reset/cấp đồ/đổi JAR đã chạy xong, công cụ debug/probe, log/ảnh kiểm tra tạm, các bản biên dịch kiểm thử cũ và 10 JAR phiên bản cũ ở thư mục gốc. Tổng dữ liệu được dọn khỏi source khoảng127 MiB. Không xóa dữ liệu game, tài khoản, tài nguyên client hay mã nguồn đang sử dụng.

Giữ lại:

- `Teamobi2026-panel-launcher.jar`: game hiện tại.
- `Teamobi2026.jar`: JAR nền cho `build-panel.ps1` biên dịch lại.
- `src`, `web`, `config`, các bộ kiểm thử dùng lại trong `test`, các script build/khởi động/triển khai.
- `build/panel-runtime`: bộ class hiện tại để build.
- `build/panel-qa`: môi trường QA có database riêng.
- `build/boss-fixtures.json`: fixture mà `test/prepare-panel-qa.php` vẫn dùng.

File chuyển túi300→150 và bộ kiểm tra chỉ phục vụ lần chuyển đó đã được dọn. Kiểm thử hành trang150 và lưu trữ vẫn giữ trong `test`.

## Khôi phục khi cần

Danh sách đường dẫn gốc, kích thước, SHA256 và nơi lưu lại nằm tại:

`C:/Downloads/NroFreeSetup/Backups/cleanup-once-20261015/manifest.json`

Các file nhỏ nằm trong `one-time-files.zip`; JAR được đối chiếu SHA256 với bản sao sẵn có hoặc lưu riêng trong `runtime-jars`. Đã kiểm tra checksum trước khi xóa bản trong source. Chỉ khôi phục file cần thiết về đúng đường dẫn trong manifest; không chạy lại script reset/cấp đồ hoặc thay JAR cũ khi chưa đối chiếu dữ liệu hiện tại.
