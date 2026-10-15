<div class="intrinsic-tabs" role="tablist" aria-label="Phạm vi chỉnh nội tại">
<button type="button" id="catalogTab" role="tab" aria-selected="true" aria-controls="intrinsicCatalogPanel">Nội tại toàn server</button>
<button type="button" id="playerTab" role="tab" aria-selected="false" aria-controls="playerIntrinsicSection">Sửa theo nhân vật</button>
</div>
<section id="intrinsicCatalogPanel" role="tabpanel" aria-labelledby="catalogTab">
<div class="intrinsic-catalog-heading"><div><h1>Danh sách nội tại toàn server</h1><p>Chỉnh mức thấp nhất/cao nhất cho danh sách trong game và các lần mở nội tại mới. Nhân vật đã có nội tại giữ chỉ số đã quay.</p></div><span id="intrinsicSync" role="status">Đang tải…</span></div>
<div class="intrinsic-tools"><label>Tìm nội tại<input id="intrinsicSearch" placeholder="Tên hoặc ID nội tại…" type="search"></label><label>Chủng tộc<select id="intrinsicRace"><option value="all">Tất cả</option><option value="0">Trái Đất</option><option value="1">Namek</option><option value="2">Xayda</option><option value="3">Dùng chung</option></select></label><button type="button" id="reloadIntrinsicCatalog">Tải lại / Hủy thay đổi</button></div>
<p id="intrinsicCount" class="intrinsic-help"></p><p id="intrinsicCatalogMessage" role="status"></p>
<div id="intrinsicTemplateCards" class="intrinsic-template-grid"></div>
<details class="intrinsic-history"><summary>Lịch sử cấu hình / Hoàn tác</summary><div id="intrinsicTemplateHistory"></div></details>
</section>
