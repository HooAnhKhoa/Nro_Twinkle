<?php
require_once __DIR__.'/panel_common.php';
require_once __DIR__.'/item_catalog_lib.php';
if ($_SERVER['REQUEST_METHOD']==='POST') {
    $data=panelInput();
    try { panelReply(['item'=>itemCatalogSave($conn,$data)]); }
    catch(InvalidArgumentException $e){panelReply(['error'=>$e->getMessage()],422);}
    catch(RuntimeException $e){panelReply(['error'=>$e->getMessage()],409);}
    catch(Throwable $e){error_log($e->getMessage());panelReply(['error'=>'Chưa lưu được vật phẩm. Vui lòng thử lại.'],500);}
}
$items=[];
$result=$conn->query('SELECT '.itemCatalogColumns().' FROM item_template ORDER BY id');
while($row=$result->fetch_assoc())$items[]=itemCatalogRow($row);
?><!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Vật phẩm</title><link rel="stylesheet" href="/typography.css?v=20261014-2"><link rel="stylesheet" href="item-catalog.css?v=1"></head><body>
<main class="catalog">
<div class="catalog-tools"><label class="search"><span aria-hidden="true">⌕</span><input id="search" type="search" placeholder="Tìm tên, mô tả hoặc ID vật phẩm…" aria-label="Tìm vật phẩm"></label><select id="typeFilter" aria-label="Lọc loại vật phẩm"><option value="">Tất cả loại</option></select><select id="genderFilter" aria-label="Lọc hành tinh"><option value="">Tất cả hành tinh</option><option value="0">Trái Đất</option><option value="1">Namếc</option><option value="2">Xayda</option><option value="3">Dùng chung</option></select><select id="sort" aria-label="Sắp xếp"><option value="id">Sắp theo ID</option><option value="name">Sắp theo tên</option><option value="power">Sức mạnh yêu cầu</option></select><span id="count" role="status"></span></div>
<div id="grid" class="catalog-grid"></div><p id="empty" hidden>Không tìm thấy vật phẩm phù hợp.</p><nav id="pagination" aria-label="Trang vật phẩm"></nav>
</main>
<div id="overlay" class="overlay" hidden><section id="drawer" class="drawer" role="dialog" aria-modal="true" aria-labelledby="itemTitle" tabindex="-1">
<div class="drawer-heading"><div class="item-picture"><img id="detailIcon" alt=""></div><div><h1 id="itemTitle"></h1><p id="itemDescription"></p></div><button id="close" class="icon-button" type="button" aria-label="Đóng">×</button></div>
<form id="itemForm"><div class="drawer-content"><dl id="facts"></dl><div id="editFields" hidden>
<label>Tên vật phẩm<input name="NAME" maxlength="100" required></label><label>Mô tả<textarea name="description" maxlength="1024" rows="3"></textarea></label>
<div class="field-pair"><label>Loại<select name="TYPE"></select></label><label>Hành tinh<select name="gender"><option value="0">Trái Đất</option><option value="1">Namếc</option><option value="2">Xayda</option><option value="3">Dùng chung</option></select></label></div>
<div class="field-pair"><label>Sức mạnh yêu cầu<input name="power_require" type="number" min="0" max="2147483647" required></label><label>Cấp<input name="level" type="number" min="0" max="127" required></label></div>
<label class="checkbox"><input name="is_up_to_up" type="checkbox"> Cho phép xếp chồng</label>
<details><summary>Ngoại hình</summary><div class="field-pair"><label>ID ảnh<input name="icon_id" type="number" min="0" max="32767" required></label><label>Mẫu ngoại hình<input name="part" type="number" min="-1" max="32767" required></label><label>Đầu<input name="head" type="number" min="-1" max="32767" required></label><label>Thân<input name="body" type="number" min="-1" max="32767" required></label><label>Chân<input name="leg" type="number" min="-1" max="32767" required></label></div></details>
</div></div><div class="drawer-footer"><p id="message" role="status" aria-live="polite"></p><div><button id="cancel" type="button">Đóng</button><button id="edit" class="primary" type="button">Chỉnh sửa</button><button id="save" class="primary" type="submit" hidden>Lưu thay đổi</button></div></div></form>
</section></div>
<script id="catalogData" type="application/json"><?=json_encode(['items'=>$items,'csrf'=>$_SESSION['panel_csrf']],JSON_HEX_TAG|JSON_HEX_AMP|JSON_HEX_APOS|JSON_HEX_QUOT|JSON_UNESCAPED_UNICODE)?></script><script src="item-catalog.js?v=1"></script></body></html>
