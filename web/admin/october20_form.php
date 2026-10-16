<?php require_once __DIR__.'/october20_config.php'; ?>
<details class="events-history" id="october20Settings" data-csrf="<?=panelEscape($_SESSION['panel_csrf'])?>">
<summary>Công thức và phần thưởng 20/10</summary>
<p>Gói quà tại Chi Chi → Sự kiện 20/10. Quà thường #1719 và quà đặc biệt #1720 dùng hình có sẵn. Tắt sự kiện dừng nguyên liệu và gói/đổi quà; hộp đã có vẫn mở được. Tỷ lệ nguyên liệu chỉnh trong Rơi đồ → Vật phẩm 20/10.</p>
<p>Mỗi hộp mở nhận đúng một phần thưởng: phụ kiện, một trong 5 đá nâng cấp hoặc một trong Cuồng nộ/Bổ huyết/Bổ khí. Tỷ lệ hỗ trợ = 100% − phụ kiện − đá. Phụ kiện gồm Chậu hoa hồng, Bong bóng hoa hồng và Bó Hoa Hồng, xác suất bằng nhau; có đủ SD, SĐCM, chí mạng và Cute, không thể giao dịch. Tỷ lệ vĩnh viễn chỉ xét khi đã nhận phụ kiện.</p>
<p>Điểm 20/10 nhận khi mở hộp, lưu riêng cho nhân vật. Đủ điểm được chọn một trong ba phụ kiện; đổi điểm luôn có hạn dùng. Không có đua top.</p>
<form id="october20Form"><div class="october20-fields">
<?php foreach(october20Fields() as $key=>$field):?><label><?=panelEscape($field['label'])?><input type="number" name="<?=panelEscape($key)?>" min="<?=$field['min']?>" max="<?=$field['max']?>" step="1" required disabled></label><?php endforeach;?>
</div><p><button type="submit" disabled>Lưu cấu hình 20/10</button> <button type="button" id="october20Reload">Tải lại</button> <button type="button" id="october20Undo" disabled>Hoàn tác lần lưu gần nhất</button></p><p id="october20Message" role="status" aria-live="polite"></p></form>
</details>
<script src="october20.js?v=20261016-1" defer></script>
