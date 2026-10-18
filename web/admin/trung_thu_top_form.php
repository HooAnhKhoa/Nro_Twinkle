<details class="events-history" id="trungThuTop"><summary>🌕 Đua top Trung Thu · lịch &amp; phần thưởng</summary>
<p>Giờ Việt Nam (UTC+7). Lịch tự bật/tắt toàn bộ Trung Thu; thời gian kết thúc không tính điểm. Game sẽ chốt và lưu quà cho cả người offline. Nhận từng gói tại Chi Chi → chọn bảng top → Nhận quà; túi đầy giữ quà để nhận sau.</p>
<p id="ttTopState" role="status"></p>
<form id="ttTopForm"><fieldset id="ttTopFields"><div class="october20-fields">
<label>Tên đợt<input id="ttTopName" maxlength="100" value="Trung Thu 2026" required></label>
<label>Bắt đầu<input id="ttTopStart" type="datetime-local" required></label>
<label>Kết thúc<input id="ttTopEnd" type="datetime-local" required></label>
<label>Tính điểm<select id="ttTopBasis"><option value="opened">Số hộp đã mở</option><option value="acquired">Số hộp mua/đổi tại NPC</option></select></label>
<label>Giá vàng / hộp VIP<input id="ttTopGold" type="number" min="0" max="1000000000" step="1" value="0" required></label>
<label>Giá ngọc / hộp VIP<input id="ttTopGems" type="number" min="0" max="1000000" step="1" value="0" required></label>
</div><p><label><input id="ttTopScheduled" type="checkbox"> Bật lịch tự động</label></p>
<p>Hộp ngọc/chay dùng giá cửa hàng hiện tại. Hộp VIP đổi bằng vàng/ngọc tại Chi Chi → Cửa hàng → Hộp VIP Trung Thu, mở theo bảng quà hộp Trung Thu hiện có. Mỗi hộp tính 1 điểm; không tính vật phẩm được admin cấp, giao dịch hoặc nhận lại. Hòa điểm: ai đạt điểm trước xếp trên, sau đó theo ID nhân vật. Mốc 25.000 VIP dùng cùng cách tính điểm đã chọn và được phát khi chốt đợt.</p>
<p>Bảng thưởng mẫu theo thể lệ đã gửi, dùng ID chuẩn. “Khóa chỉ số bông tai” dùng loại hiện có #2046. Mở các nhóm bên dưới để xem và chỉnh trước khi bật lịch. Sau khi bắt đầu, giá, lịch và thưởng sẽ khóa.</p>
<datalist id="ttTopItems"></datalist><div id="ttTopRewards"></div>
<button type="submit" id="ttTopSave">Lưu lịch &amp; thưởng</button></fieldset></form>
<button type="button" id="ttTopNew" hidden>Tạo đợt mới</button> <button type="button" id="ttTopReload">Tải lại đua top</button>
<p id="ttTopMessage" role="status" aria-live="polite"></p><div id="ttTopRanks"></div>
</details>
