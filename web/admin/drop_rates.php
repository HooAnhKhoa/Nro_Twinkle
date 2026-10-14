<?php
require_once __DIR__.'/panel_common.php';
require_once __DIR__.'/drop_catalog.php';
$catalog=dropCatalog();
foreach(explode(';',file_get_contents(__DIR__.'/../drop_rates.sql')) as $sql)if(trim($sql)!=='')$conn->query($sql);
if($_SERVER['REQUEST_METHOD']==='POST'){
    $data=panelInput();
    if(($data['action']??'')==='read'){
        $rates=$conn->query('SELECT revision,applied_revision,drop_multiplier,TIMESTAMPDIFF(SECOND,heartbeat,NOW()) age FROM server_rates WHERE id=1')->fetch_assoc();
        $groups=[];foreach($conn->query('SELECT category,mode,value FROM panel_drop_rate')->fetch_all(MYSQLI_ASSOC) as $row)$groups[$row['category']]=$row;
        panelReply(['rates'=>$rates,'groups'=>$groups]);
    }
    $revision=filter_var($data['revision']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
    $value=$data['value']??null;$action=$data['action']??'';
    if($revision===false||!is_numeric($value)||!is_finite((float)$value)||$value<0||$value>100)panelReply(['error'=>'Giá trị phải từ 0 đến 100.'],422);
    $value=round((float)$value,3);
    $category=$data['category']??'';$mode=$data['mode']??'multiplier';
    if($action!=='global'&&($action!=='group'||!isset($catalog[$category])||($catalog[$category]['supported']??true)!==true||$mode!=='multiplier'))panelReply(['error'=>'Nhóm hoặc kiểu tỷ lệ không hợp lệ.'],422);
    $conn->begin_transaction();
    try{
        $sql=$action==='global'?'UPDATE server_rates SET drop_multiplier=?,revision=revision+1,updated_at=NOW() WHERE id=1 AND revision=?':'UPDATE server_rates SET revision=revision+1,updated_at=NOW() WHERE id=1 AND revision=?';
        $stmt=$conn->prepare($sql);if($action==='global')$stmt->bind_param('di',$value,$revision);else $stmt->bind_param('i',$revision);$stmt->execute();
        if($stmt->affected_rows!==1){$conn->rollback();panelReply(['error'=>'Cấu hình vừa thay đổi ở phiên khác. Tải lại trước khi lưu.'],409);}
        if($action==='group'){$stmt=$conn->prepare('UPDATE panel_drop_rate SET mode=?,value=?,updated_at=NOW() WHERE category=?');$stmt->bind_param('sds',$mode,$value,$category);$stmt->execute();}
        $conn->commit();panelReply(['message'=>'Đã lưu. Server nhận trong khoảng 3 giây.']);
    }catch(Throwable $error){$conn->rollback();panelReply(['error'=>'Không thể lưu tỷ lệ.'],500);}
}
?><!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Tỷ lệ rơi đồ</title><link rel="stylesheet" href="/typography.css?v=20261014-2"><link rel="stylesheet" href="drop-rates.css?v=2"><link rel="stylesheet" href="admin-ui.css?v=20261015-5"><script src="admin-ui.js?v=20261015-3" defer></script></head><body class="admin-page" data-admin-page="drop_rates"><?php require_once __DIR__.'/admin_ui.php'; adminPageToolbar('Tỷ lệ rơi đồ'); ?>
<main class="drop-wrap"><header class="drop-header"><div><span class="eyebrow">ĐIỀU CHỈNH SERVER</span><h1>Tỷ lệ rơi đồ</h1><p>Chỉnh toàn server hoặc riêng từng nhóm vật phẩm.</p></div><span id="connection" class="connection">Đang kết nối…</span></header>
<section class="global-card"><div><span class="eyebrow">HỆ SỐ TOÀN SERVER</span><strong id="globalNow">—</strong><p>x1 giữ tỷ lệ gốc · x0 tắt các lần rơi ngẫu nhiên.</p></div><form id="globalForm"><label for="globalValue">Hệ số mới</label><div class="input-actions"><span class="prefix">×</span><input id="globalValue" type="number" min="0" max="100" step="0.001" required><button type="submit">Áp dụng</button></div></form></section>
<div class="section-heading"><h2>Nhóm vật phẩm</h2><button id="reload" class="quiet" type="button">↻ Tải lại / Hủy</button></div>
<p class="guide">Hệ số nhóm được nhân với hệ số toàn server. x1 giữ tỷ lệ gốc, x2 nhân đôi và x0 tắt rơi ngẫu nhiên của nhóm. Điều kiện map, set đồ và vật phẩm nhiệm vụ vẫn giữ nguyên.</p>
<div id="groups" class="drop-grid"><?php foreach($catalog as $key=>$group):$supported=$group['supported']??true;?>
<article class="drop-card <?=$supported?'':'unavailable'?>" data-category="<?=panelEscape($key)?>"><div class="card-heading"><span class="group-icon"><?=panelEscape($group['icon'])?></span><h3><?=panelEscape($group['name'])?></h3><span class="group-mode">—</span></div><div class="chance-list" aria-label="Tỷ lệ hiện tại">Đang tải…</div><details><summary>Điều kiện rơi / tỷ lệ gốc</summary><p><?=panelEscape($group['detail'])?></p></details>
<?php if($supported):?><form class="group-form"><label class="sr-only" for="mode-<?=$key?>">Kiểu chỉnh <?=panelEscape($group['name'])?></label><span class="factor-label" aria-hidden="true">×</span><input id="mode-<?=$key?>" name="mode" type="hidden" value="multiplier"><label class="sr-only" for="value-<?=$key?>">Giá trị <?=panelEscape($group['name'])?></label><input id="value-<?=$key?>" name="value" type="number" min="0" max="100" step="0.001" required><button type="submit">Lưu</button></form><span class="card-feedback" role="status"></span><?php else:?><p class="unavailable-note">Chưa có cơ chế rơi trong code hiện tại</p><?php endif;?></article><?php endforeach;?></div><p id="message" role="status" aria-live="polite"></p><footer>Tỷ lệ phía trên là tỷ lệ từng lượt xét; các nhánh nối tiếp và bonus có thể làm xác suất cuối khác nhau.</footer></main>
<script>const csrf=<?=json_encode($_SESSION['panel_csrf'])?>,catalog=<?=json_encode($catalog,JSON_UNESCAPED_UNICODE)?>;</script><script src="drop-rates.js?v=2"></script></body></html>
