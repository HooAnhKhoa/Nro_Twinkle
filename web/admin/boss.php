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
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);
    if (!is_array($data) || !is_string($data['csrf'] ?? null) || !hash_equals($_SESSION['boss_csrf'], $data['csrf'])) reply(['error'=>'Phiên thao tác không hợp lệ'],403);
    $action=$data['action'] ?? '';
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
        }
        unset($row);
        require_once __DIR__.'/boss_group.php';
        $rows=bossGroupRows($rows);
        reply(['rows'=>$rows]);
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
        if ($hp===false || $damage===false || $rest===false || !is_bool($data['enabled'] ?? null)) reply(['error'=>'HP: 1–2.147.483.647; sát thương: 0–2.147.483.647; hồi: 0–604.800 giây'],422);
        $enabled=(int)$data['enabled'];
        $stmt=$conn->prepare('UPDATE boss_web_config SET hp=?,damage=?,rest_seconds=?,enabled=?,customized=1,applied_at=NULL WHERE boss_key=?');
        $stmt->bind_param('iiiis',$hp,$damage,$rest,$enabled,$key); $stmt->execute();
        reply(['message'=>'Đã lưu. Server nhận trong khoảng 3 giây; chỉ số áp dụng lần xuất hiện tiếp theo.']);
    }
    if ($action === 'spawn') {
        if ($boss['age']===null || (int)$boss['age']>20) reply(['error'=>'Server chưa kết nối. Hãy chạy bản server có quản lý boss.'],409);
        if (!(int)$boss['enabled'] || !(int)$boss['can_spawn']) reply(['error'=>'Boss đang tắt hoặc xuất hiện theo chuỗi/giai đoạn.'],409);
        $stmt=$conn->prepare('INSERT INTO boss_web_command (boss_key) VALUES (?)');
        $stmt->bind_param('s',$key); $stmt->execute(); reply(['id'=>$conn->insert_id]);
    }
    reply(['error'=>'Thao tác không hợp lệ'],422);
}
?>
<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Quản lý boss</title>
<style>
*{box-sizing:border-box}body{margin:0;background:linear-gradient(120deg,#a9dcfa,#70adfb);font-family:Arial,sans-serif;color:#253b50;min-height:100vh;padding:28px}header{max-width:1100px;margin:0 auto 20px;display:flex;justify-content:space-between;align-items:center}header a{color:#253b50}h1{font-size:25px}.panel{max-width:1100px;margin:auto;background:white;border-radius:18px;padding:24px;box-shadow:0 20px 50px #25496c40;display:grid;grid-template-columns:1fr 290px;gap:20px}.search{display:flex;align-items:center;gap:12px;background:#eef7ff;border:1px solid #b9cddc;border-radius:9px;padding:12px}.search input{flex:1;min-width:0}.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(120px,1fr));gap:9px;max-height:620px;overflow:auto;padding:12px 2px}.card{background:#f1f8ff;border:1px solid #b9cddc;border-radius:9px;text-align:left;padding:10px;cursor:pointer;color:#253b50}.card.selected{border:2px solid #24a665;background:#e9fbef}.card.off{filter:grayscale(1);opacity:.65}.portrait{height:66px;display:flex;align-items:center;justify-content:center;background:white;border-radius:7px;font-size:36px;margin-bottom:9px}.card strong{display:block;font-size:14px;min-height:32px}.card small{display:block;margin-top:5px;font-size:11px;overflow-wrap:anywhere}.editor{background:#eef7ff;border:1px solid #b9cddc;border-radius:10px;padding:16px;align-self:start}h2{font-size:18px;text-align:center;margin:0 0 18px}label{display:block;font-size:14px;font-weight:bold;margin:15px 0 6px}input,button{font:inherit}input{width:100%;padding:10px;border:1px solid #adc2d0;border-radius:6px;background:white}button{padding:10px;border:1px solid #adc2d0;border-radius:6px;cursor:pointer}button:disabled{opacity:.5;cursor:default}.toggle{display:flex;align-items:center;justify-content:space-between;gap:12px}.toggle input{width:auto}#spawn{width:100%;color:#d43358;background:#fff1f5;margin:16px 0 10px}.actions{display:flex;gap:8px}.actions button{flex:1}#save{background:#dff2e9;color:#177348}.note{font-size:12px;line-height:1.6;color:#60768b}#message{margin:14px 0;line-height:1.5;font-size:14px;white-space:pre-wrap}.error{color:#bd2347}.empty{padding:30px;line-height:1.6}.status{font-size:12px;margin-top:10px;color:#60768b}@media(max-width:720px){body{padding:12px}.panel{grid-template-columns:1fr;padding:14px}.editor{grid-row:1}.cards{max-height:450px}h1{font-size:21px}}
</style><?php if(isset($_GET["embed"])): ?><link rel="stylesheet" href="boss-panel.css?v=20261014-font"><?php endif;?><link rel="stylesheet" href="/typography.css?v=20261014-2"><link rel="stylesheet" href="admin-ui.css?v=20261015-4"><script src="admin-ui.js?v=20261015-2" defer></script></head><body class="admin-page" data-admin-page="boss"><?php require_once __DIR__.'/admin_ui.php'; adminPageToolbar('Boss'); ?><header><h1>☠ Quản lý boss</h1><a href="panel.php">← Về panel</a></header><main class="panel"><section><div class="search"><b>Tìm boss</b><input id="search" placeholder="Nhập tên hoặc HP boss…"><small id="count"></small></div><div id="cards" class="cards"></div><div id="empty" class="empty" hidden>Chưa có dữ liệu boss. Hãy khởi động bản server mới. Danh sách sẽ tự cập nhật khi server kết nối.</div></section><aside class="editor"><h2>Thông tin chỉnh sửa</h2><form id="form"><div class="toggle"><b>Cho phép xuất hiện</b><input id="enabled" type="checkbox" aria-label="Cho phép boss xuất hiện"></div><label>Tên boss</label><input id="name" readonly><div id="locationNow" class="current-location" aria-live="polite"></div><label>HP</label><input id="hp" type="text" inputmode="numeric" pattern="[0-9., ]+" required><label>Sát thương</label><input id="damage" type="text" inputmode="numeric" pattern="[0-9., ]+" required><label>Thời gian hồi (Rest):</label><select id="rest"><option value="0">0 giây (không trì hoãn)</option><option value="1">1 giây</option><option value="5">5 giây</option><option value="60">1 phút</option><option value="300">5 phút</option><option value="600">10 phút</option><option value="900">15 phút</option><option value="1800">30 phút</option><option value="3600">1 giờ</option><option value="7200">2 giờ</option><option value="86400">24 giờ</option></select><p id="restHint" class="status"></p><div id="detail" class="status"></div><button id="spawn" type="button" disabled>☠ Gọi boss</button><div class="actions"><button id="save" type="submit" disabled>💾 Lưu</button><button id="cancel" type="button">Hủy thay đổi</button></div></form><div id="message" role="status" aria-live="polite"></div><p class="note">Chỉ số áp dụng cho dạng đầu tiên, từ lần xuất hiện tiếp theo. Tắt boss sẽ đưa boss khỏi bản đồ. Boss theo chuỗi/giai đoạn tiếp tục xuất hiện theo cơ chế game; nút gọi chỉ dành cho boss độc lập.</p></aside></main>
<script src="boss_locations.js?v=20261014"></script><script>
const csrf=<?=json_encode($_SESSION['boss_csrf'])?>;
let rows=[], selected=null, dirty=false, busy=false;
const el=id=>document.getElementById(id), fmt=n=>Number(n).toLocaleString('vi-VN');
function message(text,error=false){el('message').textContent=text;el('message').className=error?'error':'';}
async function api(data){const r=await fetch('boss.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...data,csrf})});const j=await r.json();if(!r.ok||j.error)throw Error(j.error||'Không thể thực hiện thao tác');return j;}
function render(){const q=el('search').value.trim().toLowerCase();const matches=rows.filter(r=>(r.name+' '+bossLocation(r)+' '+bossSpawnMaps(r)).toLowerCase().includes(q)||String(r.hp).includes(q.replace(/[.,\s]/g,'')));el('count').textContent=matches.length+' boss';el('cards').replaceChildren();el('empty').hidden=rows.length>0;for(const r of matches){const card=document.createElement('button');card.type='button';card.className='card'+(selected?.boss_key===r.boss_key?' selected':'')+(Number(r.enabled)?'':' off');const icon=document.createElement('div');icon.className='portrait';const image=document.createElement('img');image.src=r.image;image.alt=r.name;image.onerror=()=>{image.remove();icon.textContent='?';icon.title='Không tìm thấy file ảnh của Boss này';};icon.append(image);const badge=document.createElement('span');badge.className='badge';badge.textContent=Number(r.enabled)?'✓':'×';card.append(badge);const title=document.createElement('strong');title.textContent=r.name;const hp=document.createElement('small');hp.textContent='HP: '+fmt(r.hp);const state=document.createElement('small');state.className='boss-state';state.textContent=(Number(r.enabled)?'● Bật':'○ Tắt')+' · '+r.online_count+' đang xuất hiện';const location=document.createElement('small');location.className='location';location.textContent='📍 '+bossLocation(r,true);location.title=bossLocation(r);card.append(icon,title,hp,state,location);card.onclick=()=>choose(r);el('cards').append(card);}}
function choose(r){if(dirty&&selected?.boss_key!==r.boss_key&&!confirm('Bỏ thay đổi chưa lưu?'))return;selected=r;dirty=false;el('name').value=r.name;el('hp').value=fmt(r.hp);el('damage').value=fmt(r.damage);if(!Array.from(el('rest').options).some(o=>Number(o.value)===Number(r.rest_seconds)))el('rest').append(new Option(fmt(r.rest_seconds)+' giây',r.rest_seconds));el('rest').value=r.rest_seconds;el('enabled').checked=!!Number(r.enabled);el('save').disabled=false;details();render();}
function details(){if(!selected)return;const online=bossLive(selected);el('locationNow').textContent='📍 Đang xuất hiện:\n'+bossLocation(selected);el('detail').textContent='Map có thể xuất hiện: '+bossSpawnMaps(selected)+'\n'+(online?'Server đang kết nối':'Server chưa kết nối')+' · '+(selected.applied_at?'Đã đồng bộ':'Chờ server nhận');el('restHint').textContent=bossRestHint(selected);el('spawn').disabled=busy||!online||!Number(selected.can_spawn)||!Number(selected.enabled)||dirty;}
async function refresh(){try{rows=(await api({action:'list'})).rows;if(selected){const updated=rows.find(r=>r.boss_key===selected.boss_key);if(updated){selected=updated;if(!dirty&&!busy)choose(updated);else details();}}render();}catch(e){message(e.message,true);}}
el('search').oninput=render;el('form').oninput=()=>{dirty=true;details();};el('cancel').onclick=()=>{if(selected)choose(selected);};
el('form').onsubmit=async e=>{e.preventDefault();if(!selected||busy)return;busy=true;el('save').disabled=true;try{const j=await api({action:'save',key:selected.boss_key,hp:Number(el('hp').value.replace(/[.,\s]/g,'')),damage:Number(el('damage').value.replace(/[.,\s]/g,'')),rest:Number(el('rest').value),enabled:el('enabled').checked});dirty=false;message(j.message);await refresh();}catch(e){message(e.message,true);}finally{busy=false;el('save').disabled=!selected;details();}};
el('spawn').onclick=async()=>{if(!selected||busy)return;busy=true;details();try{const j=await api({action:'spawn',key:selected.boss_key});message('Đang chờ server nhận lệnh…');const deadline=Date.now()+20000;let done=false;while(Date.now()<deadline){await new Promise(r=>setTimeout(r,1500));const s=await api({action:'status',id:j.id});if(s.command?.completed_at){message(s.command.result);done=true;break;}}if(!done)message('Chưa nhận được phản hồi. Lệnh đang chờ server xử lý.',true);await refresh();}catch(e){message(e.message,true);}finally{busy=false;details();}};
refresh();setInterval(refresh,5000);
</script></body></html>
