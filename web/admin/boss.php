<?php
session_start();
require_once '../config.php';
if (empty($_SESSION['user'])) { header('Location: /dang-nhap.php'); exit; }
$auth = $conn->prepare('SELECT is_admin FROM account WHERE username=? LIMIT 1');
$auth->bind_param('s', $_SESSION['user']); $auth->execute();
$account = $auth->get_result()->fetch_assoc();
if (!$account || (int)$account['is_admin'] !== 1) { http_response_code(403); exit('Không có quyền quản lý boss'); }
if (empty($_SESSION['boss_csrf'])) $_SESSION['boss_csrf'] = bin2hex(random_bytes(32));
foreach (explode(';', file_get_contents(__DIR__.'/../boss_web.sql')) as $sql) {
    if (trim($sql) !== '' && !$conn->query($sql)) { http_response_code(500); exit('Không thể khởi tạo dữ liệu boss'); }
}
function reply($data, $code=200) { http_response_code($code); header('Content-Type: application/json; charset=utf-8'); echo json_encode($data, JSON_UNESCAPED_UNICODE); exit; }
if (!$conn->query("SHOW COLUMNS FROM boss_web_config LIKE 'head_id'")->num_rows) $conn->query('ALTER TABLE boss_web_config ADD head_id INT NOT NULL DEFAULT 0');
if (!$conn->query("SHOW COLUMNS FROM boss_web_config LIKE 'current_locations'")->num_rows) $conn->query('ALTER TABLE boss_web_config ADD current_locations TEXT NULL');
foreach ([['boss_web_config','lifetime_seconds','INT NULL'],['boss_web_config','timing_state','LONGTEXT NULL'],['boss_web_command','delay_seconds','INT NOT NULL DEFAULT 0']] as [$table,$column,$type]) {
    if (!$conn->query("SHOW COLUMNS FROM $table LIKE '$column'")->num_rows) $conn->query("ALTER TABLE $table ADD $column $type");
}
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);
    if (!is_array($data) || !is_string($data['csrf'] ?? null) || !hash_equals($_SESSION['boss_csrf'], $data['csrf'])) reply(['error'=>'Phiên thao tác không hợp lệ'],403);
    $action=$data['action'] ?? '';
    define('BOSS_API_AUTHORIZED',true);require_once __DIR__.'/boss_drop_api.php';bossDropApi($conn,$data);
    if ($action === 'list') {
        $rows=$conn->query('SELECT *, TIMESTAMPDIFF(SECOND,heartbeat,NOW()) AS age FROM boss_web_config ORDER BY name,(heartbeat IS NULL OR heartbeat<NOW()-INTERVAL 20 SECOND),boss_key')->fetch_all(MYSQLI_ASSOC);
        $catalog=json_decode(file_get_contents(__DIR__.'/../boss_images.json'),true)??[];
        $maps=[];foreach($conn->query('SELECT id,NAME AS map_name FROM map_template')->fetch_all(MYSQLI_ASSOC) as $map)$maps[(int)$map['id']]=$map['map_name'];
        foreach($rows as &$row) {
            $head=(int)$row['head_id'];
            $name=mb_strtolower(trim($row['name']),'UTF-8');
            $location=$name.'|'.preg_replace('/\s+/','',$row['map_ids']);
            if($head===0) $head=$catalog['by_key'][$row['boss_key']]??$catalog['by_name_map'][$location]??$catalog['by_name'][$name]??null;
            $row['image']=$head===null?'':'boss_image.php?head='.(int)$head;
            $ids=json_decode($row['map_ids'],true);
            $row['spawn_maps']=array_map(fn($id)=>['id'=>(int)$id,'name'=>$maps[(int)$id]??('Map #'.(int)$id)],is_array($ids)?$ids:[]);
            $row['locations']=json_decode($row['current_locations']??'[]',true)??[];
            $row['timings']=json_decode($row['timing_state']??'[]',true)??[];
        }
        unset($row);
        require_once __DIR__.'/boss_group.php';
        $rows=bossGroupRows($rows);
        reply(['rows'=>$rows,'server_now'=>(int)round(microtime(true)*1000)]);
    }
    if ($action === 'status') {
        $id=filter_var($data['id'] ?? null,FILTER_VALIDATE_INT);
        if (!$id) reply(['error'=>'Lệnh không hợp lệ'],422);
        $stmt=$conn->prepare('SELECT completed_at,result FROM boss_web_command WHERE id=?');
        $stmt->bind_param('i',$id); $stmt->execute(); reply(['command'=>$stmt->get_result()->fetch_assoc()]);
    }
    $key=$data['key'] ?? '';
    if (!is_string($key) || strlen($key)>100) reply(['error'=>'Boss không hợp lệ'],422);
    $stmt=$conn->prepare('SELECT *, TIMESTAMPDIFF(SECOND,heartbeat,NOW()) AS age FROM boss_web_config WHERE boss_key=?');
    $stmt->bind_param('s',$key); $stmt->execute(); $boss=$stmt->get_result()->fetch_assoc();
    if (!$boss) reply(['error'=>'Không tìm thấy boss'],404);
    if ($action === 'save') {
        $hp=filter_var($data['hp'] ?? null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1,'max_range'=>2147483647]]);
        $damage=filter_var($data['damage'] ?? null,FILTER_VALIDATE_INT,['options'=>['min_range'=>0,'max_range'=>2147483647]]);
        $rest=filter_var($data['rest'] ?? null,FILTER_VALIDATE_INT,['options'=>['min_range'=>0,'max_range'=>604800]]);
        $lifetime=($data['lifetime']??null)===null?null:filter_var($data['lifetime'],FILTER_VALIDATE_INT,['options'=>['min_range'=>0,'max_range'=>604800]]);
        if($lifetime===false)reply(['error'=>'Thời gian tồn tại: 0–604.800 giây; để trống dùng cơ chế gốc.'],422);
        if ($hp===false || $damage===false || $rest===false || !is_bool($data['enabled'] ?? null)) reply(['error'=>'HP: 1–2.147.483.647; sát thương: 0–2.147.483.647; hồi: 0–604.800 giây'],422);
        $enabled=(int)$data['enabled'];
        $stmt=$conn->prepare('UPDATE boss_web_config SET hp=?,damage=?,rest_seconds=?,enabled=?,lifetime_seconds=?,customized=1,applied_at=NULL WHERE boss_key=?');
        $stmt->bind_param('iiiiis',$hp,$damage,$rest,$enabled,$lifetime,$key); $stmt->execute();
        reply(['message'=>'Đã lưu. Server nhận trong khoảng 3 giây. HP/sát thương áp dụng lần xuất hiện tiếp theo; thời gian hồi và tồn tại áp dụng cả chu kỳ hiện tại.']);
    }
    if ($action === 'spawn') {
        if ($boss['age']===null || (int)$boss['age']>20) reply(['error'=>'Server chưa kết nối. Hãy chạy bản server có quản lý boss.'],409);
        if (!(int)$boss['enabled'] || !(int)$boss['can_spawn']) reply(['error'=>'Boss đang tắt hoặc xuất hiện theo chuỗi/giai đoạn.'],409);
        if((int)$boss['online_count']>0)reply(['error'=>'Boss đang xuất hiện, không thể hẹn thêm.'],409);
        $delay=filter_var($data['delay']??0,FILTER_VALIDATE_INT,['options'=>['min_range'=>0,'max_range'=>604800]]);
        if($delay===false)reply(['error'=>'Hẹn xuất hiện: 0–604.800 giây.'],422);
        $stmt=$conn->prepare('INSERT INTO boss_web_command (boss_key,delay_seconds) VALUES (?,?)');
        $stmt->bind_param('si',$key,$delay); $stmt->execute(); reply(['id'=>$conn->insert_id]);
    }
    reply(['error'=>'Thao tác không hợp lệ'],422);
}
?>
<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Quản lý Boss</title><link rel="stylesheet" href="/typography.css?v=20261014-2"><link rel="stylesheet" href="admin-ui.css?v=20261015-times"><link rel="stylesheet" href="boss-manager.css?v=20261015-5"><script src="admin-ui.js?v=20261015-3" defer></script></head>
<body class="admin-page" data-admin-page="boss"><?php require_once __DIR__.'/admin_ui.php';adminPageToolbar(!empty($startDrops)?'Boss rơi':'Boss'); ?>
<main class="boss-manager"><div class="boss-toolbar"><div><h2><?=!empty($startDrops)?'Vật phẩm Boss rơi':'Danh sách Boss'?></h2><p>Chọn một boss để xem thông tin và chỉnh sửa.</p></div><div class="boss-filters"><input id="search" aria-label="Tìm boss" placeholder="Tìm tên, map hoặc HP…"><select id="stateFilter" aria-label="Lọc trạng thái"><option value="all">Tất cả trạng thái</option><option value="live">Đang xuất hiện</option><option value="waiting">Chưa xuất hiện</option><option value="off">Đang tắt</option></select></div></div><div class="boss-list-heading"><span id="count">Đang tải…</span><span>Thứ tự theo nhiệm vụ</span></div><p id="pageMessage" role="status"></p><div id="cards" class="boss-grid"></div><p id="empty" class="boss-empty" hidden>Không tìm thấy boss phù hợp.</p></main>
<dialog id="bossModal" aria-labelledby="bossTitle"><div class="boss-modal-head"><img id="bossPortrait" alt=""><div><h2 id="bossTitle">Thông tin Boss</h2><p id="bossState"></p></div><button type="button" id="closeModal" aria-label="Đóng thông tin boss">×</button></div><div class="boss-tabs" role="tablist" aria-label="Chỉnh sửa boss"><button id="infoTab" role="tab" aria-selected="true" aria-controls="infoPanel">Thông tin</button><button id="dropsTab" role="tab" aria-selected="false" aria-controls="dropsPanel">Vật phẩm rơi</button></div>
<section id="infoPanel" role="tabpanel" aria-labelledby="infoTab"><form id="infoForm"><div class="boss-info-grid"><div><div class="boss-toggle"><label for="enabled">Cho phép xuất hiện</label><input id="enabled" type="checkbox"></div><div id="locationNow" class="boss-location"></div><div class="boss-fields"><label>HP<input id="hp" inputmode="numeric" pattern="[0-9., ]+" required></label><label>Sát thương<input id="damage" inputmode="numeric" pattern="[0-9., ]+" required></label><label>Thời gian hồi · giây<input id="rest" type="number" min="0" max="604800" step="1" required></label><label>Giới hạn tồn tại · giây<input id="lifetime" type="number" min="0" max="604800" step="1" placeholder="Trống = cơ chế gốc"></label></div><p class="boss-help">Tồn tại: để trống giữ cơ chế gốc, 0 không giới hạn. Rút ngắn có thể khiến boss đang sống rời map ngay.</p></div><div><div id="timers" class="boss-timers"></div><p id="restHint" class="boss-help"></p><p id="detail" class="boss-help"></p><div class="boss-spawn"><label for="spawnDelay">Hẹn xuất hiện sau · giây</label><div><input id="spawnDelay" type="number" min="0" max="604800" step="1" value="0"><button id="spawn" type="button">Gọi ngay</button></div><small>Hẹn một lần; mất lịch khi khởi động lại. Boss theo chuỗi hoặc triệu hồi vẫn giữ điều kiện riêng.</small></div></div></div><div class="boss-footer"><span class="boss-help">HP / sát thương áp dụng lần xuất hiện tiếp theo.</span><button type="button" id="cancelInfo">Hủy thay đổi</button><button type="submit" id="saveInfo" class="boss-primary">Lưu thông tin</button></div></form></section>
<section id="dropsPanel" role="tabpanel" aria-labelledby="dropsTab" hidden><form id="dropForm"><div class="boss-drop-top"><label for="dropMode">Cách rơi vật phẩm</label><select id="dropMode"><option value="original">Giữ nguyên đồ gốc</option><option value="add">Đồ gốc + đồ bổ sung</option><option value="replace">Thay bằng bảng rơi bên dưới</option><option value="off">Tắt rơi vật phẩm</option></select><span id="dropSync" class="boss-sync"></span></div><p id="dropModeHint" class="boss-help"></p><p id="periodicHint" class="boss-help" hidden>Ông già Noel phát quà theo chu kỳ gốc, không cần bị hạ. Cấu hình này áp dụng cho mỗi lần phát quà, vật phẩm dành cho mọi người.</p><section class="boss-loot-preview"><h3>Vật phẩm có thể rơi</h3><p id="lootNote" class="boss-help"></p><div id="lootPreview" class="boss-item-grid"></div></section><details id="itemPicker" class="boss-picker"><summary>＋ Thêm vật phẩm — mở kho vật phẩm</summary><input id="itemSearch" placeholder="Thêm vật phẩm: nhập tên hoặc ID…" aria-label="Tìm vật phẩm để thêm" autocomplete="off"><div id="itemResults" class="boss-picker-results boss-item-grid"></div><div class="boss-picker-pages"><button type="button" id="itemsPrev">← Trước</button><span id="itemsPage" role="status"></span><button type="button" id="itemsNext">Sau →</button></div></details><h3>Bảng rơi tùy chỉnh</h3><p class="boss-help">Mỗi dòng quay tỷ lệ độc lập: 100% luôn rơi, 0% tắt dòng. Số lượng nhận ngẫu nhiên trong khoảng đã đặt. Bảng này không nhân hệ số rơi quái.</p><div id="dropRows"></div><p id="dropEmpty" class="boss-empty">Chưa có vật phẩm tùy chỉnh. Dùng ô tìm kiếm để thêm.</p><div class="boss-footer"><span class="boss-help">Điểm, nhiệm vụ và cơ chế chuỗi của boss vẫn giữ nguyên.</span><button type="button" id="reloadDrops">Tải lại bảng rơi</button><button type="submit" id="saveDrops" class="boss-primary">Lưu bảng rơi</button></div></form></section><p id="message" role="status" class="boss-message" aria-live="polite"></p></dialog>
<script>const bossCsrf=<?=json_encode($_SESSION['boss_csrf'])?>;const startBossDrops=<?=!empty($startDrops)?'true':'false'?>;</script><script src="boss_locations.js?v=20261015-times"></script><script src="boss-manager.js?v=20261015-4"></script></body></html>