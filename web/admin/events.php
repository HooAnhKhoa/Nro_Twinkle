<?php
require_once __DIR__.'/panel_common.php';
require_once __DIR__.'/event_catalog.php';
$catalog=eventCatalog();
foreach(explode(';',file_get_contents(__DIR__.'/../event_schema.sql')) as $sql)if(trim($sql)!=='')$conn->query($sql);
if($_SERVER['REQUEST_METHOD']==='POST'){
    $data=panelInput();$action=$data['action']??'';
    if($action==='read'){
        $config=$conn->query('SELECT revision,applied_revision,TIMESTAMPDIFF(SECOND,heartbeat,NOW()) age FROM panel_event_runtime WHERE id=1')->fetch_assoc();
        $events=$conn->query('SELECT event_key,enabled,updated_at FROM panel_event_config')->fetch_all(MYSQLI_ASSOC);
        $history=$conn->query('SELECT id,event_key,before_enabled,after_enabled,revision,created_at,undone_at FROM panel_event_audit ORDER BY id DESC LIMIT 20')->fetch_all(MYSQLI_ASSOC);
        $hungBosses=$conn->query("SELECT name,online_count,current_locations,TIMESTAMPDIFF(SECOND,heartbeat,NOW()) age FROM boss_web_config WHERE boss_key IN ('nro.models.boss.event_hung_vuong.ThuyTinh','nro.models.boss.event_hung_vuong.SonTinh') ORDER BY name")->fetch_all(MYSQLI_ASSOC);
        $offerings=$conn->query('SELECT locations,TIMESTAMPDIFF(SECOND,heartbeat,NOW()) age FROM panel_event_mob_runtime WHERE id=1')->fetch_assoc();
        panelReply(['config'=>$config,'events'=>$events,'history'=>$history,'hung_bosses'=>$hungBosses,'offerings'=>$offerings]);
    }
    $key=$data['event_key']??null;
    $revision=filter_var($data['revision']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
    if(!is_string($key)||!isset($catalog[$key])||$revision===false||!in_array($action,['set','undo'],true))panelReply(['error'=>'Sự kiện hoặc thao tác không hợp lệ.'],422);
    if($action==='set'&&!is_bool($data['enabled']??null))panelReply(['error'=>'Trạng thái phải là bật hoặc tắt.'],422);
    $conn->begin_transaction();
    try{
        $current=$conn->query('SELECT revision FROM panel_event_runtime WHERE id=1 FOR UPDATE')->fetch_assoc();
        if((int)$current['revision']!==$revision)throw new RuntimeException('Cấu hình đã thay đổi. Tải lại trước khi thao tác.');
        $stmt=$conn->prepare('SELECT enabled FROM panel_event_config WHERE event_key=? FOR UPDATE');$stmt->bind_param('s',$key);$stmt->execute();$row=$stmt->get_result()->fetch_assoc();
        if(!$row)throw new RuntimeException('Không tìm thấy cấu hình sự kiện.');
        $before=(int)$row['enabled'];$next=(int)($data['enabled']??false);
        if($action==='undo'){
            $auditId=filter_var($data['audit_id']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
            if($auditId===false)throw new RuntimeException('Lịch sử không hợp lệ.');
            $stmt=$conn->prepare('SELECT * FROM panel_event_audit WHERE id=? AND event_key=? FOR UPDATE');$stmt->bind_param('is',$auditId,$key);$stmt->execute();$audit=$stmt->get_result()->fetch_assoc();
            $stmt=$conn->prepare('SELECT MAX(revision) latest FROM panel_event_audit WHERE event_key=?');$stmt->bind_param('s',$key);$stmt->execute();$latest=(int)$stmt->get_result()->fetch_assoc()['latest'];
            if(!$audit||$audit['undone_at']||(int)$audit['after_enabled']!==$before||(int)$audit['revision']!==$latest)throw new RuntimeException('Chỉ hoàn tác được thay đổi gần nhất của sự kiện này.');
            $next=(int)$audit['before_enabled'];
            $stmt=$conn->prepare('UPDATE panel_event_audit SET undone_at=NOW() WHERE id=?');$stmt->bind_param('i',$auditId);$stmt->execute();
        }
        if($next===$before){$conn->commit();panelReply(['message'=>'Trạng thái không thay đổi.']);}
        $newRevision=$revision+1;$admin=(int)$panelAdmin['id'];
        $stmt=$conn->prepare('UPDATE panel_event_config SET enabled=? WHERE event_key=?');$stmt->bind_param('is',$next,$key);$stmt->execute();
        $stmt=$conn->prepare('INSERT INTO panel_event_audit(event_key,admin_id,before_enabled,after_enabled,revision) VALUES(?,?,?,?,?)');$stmt->bind_param('siiii',$key,$admin,$before,$next,$newRevision);$stmt->execute();
        $conn->query('UPDATE panel_event_runtime SET revision=revision+1 WHERE id=1');$conn->commit();
        panelReply(['message'=>($action==='undo'?'Đã hoàn tác. ':'Đã lưu. ').'Game nhận cấu hình trong khoảng 3 giây; boss rời map ở lượt cập nhật tiếp theo.']);
    }catch(Throwable $error){$conn->rollback();panelReply(['error'=>$error instanceof RuntimeException?$error->getMessage():'Không thể lưu cấu hình sự kiện.'],409);}
}
?><!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Sự kiện</title><link rel="stylesheet" href="/typography.css?v=20261014-2"><link rel="stylesheet" href="admin-ui.css?v=20261015-5"><link rel="stylesheet" href="events.css?v=4-october20"><script src="admin-ui.js?v=20261015-3" defer></script></head><body class="admin-page"><?php require_once __DIR__.'/admin_ui.php';adminPageToolbar('Sự kiện'); ?>
<main class="events-wrap"><header class="events-heading"><div><h1>Sự kiện</h1><p>Bật/tắt riêng từng hoạt động đang có trong game.</p></div><button id="reloadEvents" type="button">↻ Tải lại</button></header>
<section class="events-summary"><strong id="eventCount">Đang tải…</strong><span id="eventSync" role="status">Đang kết nối game…</span></section>
<p class="events-help">Thay đổi được lưu ngay khi bấm công tắc và giữ sau khi khởi động lại. NPC của EV sẽ ẩn khi tắt và hiện lại khi bật. Tắt EV không xóa vật phẩm đã nhận. Các công tắc độc lập; tỷ lệ rơi vẫn chỉnh ở mục Rơi đồ.</p>
<p id="eventMessage" role="status" aria-live="polite"></p><div class="events-grid">
<?php foreach($catalog as $key=>$event):?><article class="event-card" data-event="<?=panelEscape($key)?>"><header><span class="event-icon"><?=panelEscape($event['icon'])?></span><div><h2><?=panelEscape($event['name'])?></h2><span class="event-state">Đang tải…</span></div><button class="event-switch" type="button" role="switch" aria-checked="false" aria-label="<?=panelEscape($event['name'])?>" disabled><span></span></button></header><p><?=panelEscape($event['scope'])?></p><details><summary>Phạm vi áp dụng</summary><p><?=panelEscape($event['detail'])?></p></details><?php if($key==='october20'):?><button type="button" id="openOctober20Settings">Công thức &amp; phần thưởng</button><?php endif;?></article><?php endforeach;?>
</div><details class="events-incomplete"><summary>Các EV chưa có chức năng hoàn chỉnh</summary><p>8/3 mới có bộ nạp dữ liệu; Top Up mới có khung lớp; 20/11 và Valentine mới có mã định danh. Chưa cung cấp công tắc cho các mục này. Rồng sự kiện có luồng riêng, chưa nằm trong nhóm công tắc mùa lễ này.</p></details>
<?php require __DIR__.'/october20_form.php'; ?>
<details class="events-history"><summary>Lịch sử bật/tắt và hoàn tác</summary><div id="eventHistory">Đang tải…</div></details></main>
<script>const csrf=<?=json_encode($_SESSION['panel_csrf'])?>,eventCatalog=<?=json_encode($catalog,JSON_UNESCAPED_UNICODE|JSON_HEX_TAG|JSON_HEX_AMP|JSON_HEX_APOS|JSON_HEX_QUOT)?>;</script><script src="events.js?v=3-offerings"></script></body></html>
