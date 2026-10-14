<?php
session_start();
require_once '../config.php';
if (empty($_SESSION['user'])) { header('Location: /dang-nhap.php'); exit; }
$stmt=$conn->prepare('SELECT is_admin FROM account WHERE username=? LIMIT 1');
$stmt->bind_param('s',$_SESSION['user']); $stmt->execute(); $account=$stmt->get_result()->fetch_assoc();
if (!$account || (int)$account['is_admin']!==1) { http_response_code(403); exit('Không có quyền'); }
if (empty($_SESSION['rates_csrf'])) $_SESSION['rates_csrf']=bin2hex(random_bytes(32));
function reply($body,$code=200) { http_response_code($code); header('Content-Type: application/json; charset=utf-8'); echo json_encode($body,JSON_UNESCAPED_UNICODE); exit; }
foreach (explode(';',file_get_contents(__DIR__.'/../server_rates.sql')) as $sql) {
    if (trim($sql)!=='' && !$conn->query($sql)) { http_response_code(500); exit('Không thể khởi tạo bảng tỷ lệ'); }
}
require_once __DIR__.'/rates_schema.php'; ensureStarRates($conn);
if ($_SERVER['REQUEST_METHOD']==='POST') {
    $data=json_decode(file_get_contents('php://input'),true);
    if (!is_array($data) || !is_string($data['csrf']??null) || !hash_equals($_SESSION['rates_csrf'],$data['csrf'])) reply(['error'=>'Phiên thao tác không hợp lệ'],403);
    if (($data['action']??'')==='read') {
        $row=$conn->query('SELECT *,TIMESTAMPDIFF(SECOND,heartbeat,NOW()) AS age FROM server_rates WHERE id=1')->fetch_assoc();
        reply(['row'=>$row]);
    }
    if (($data['action']??'')!=='save') reply(['error'=>'Thao tác không hợp lệ'],422);
    $exp=filter_var($data['exp']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1,'max_range'=>100]]);
    $revision=filter_var($data['revision']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
    $drop=$data['drop']??null; $upgrades=$data['upgrades']??null;
    if ($exp===false || $revision===false || !is_numeric($drop) || !is_finite((float)$drop) || $drop<0 || $drop>100 || !is_array($upgrades) || count($upgrades)!==8) reply(['error'=>'EXP: số nguyên 1–100; rơi đồ: 0–100; cần đủ 8 tỷ lệ đập đồ'],422);
    foreach ($upgrades as $value) {
        if (!is_numeric($value) || !is_finite((float)$value) || $value<0 || $value>100) reply(['error'=>'Tỷ lệ đập đồ phải từ 0 đến 100%'],422);
    }
    $stars=$data['stars']??null;
    if(!is_array($stars)||count($stars)!==9)reply(['error'=>'Cần đủ 9 tỷ lệ đập sao'],422);
    foreach($stars as $value)if(!is_numeric($value)||!is_finite((float)$value)||$value<0||$value>100)reply(['error'=>'Tỷ lệ đập sao phải từ 0 đến 100%'],422);
    $s=array_map(fn($v)=>round((float)$v,3),array_values($stars));
    $u=array_map(fn($v)=>round((float)$v,3),array_values($upgrades)); $drop=round((float)$drop,3);
    $stmt=$conn->prepare('UPDATE server_rates SET exp_rate=?,drop_multiplier=?,upgrade_1=?,upgrade_2=?,upgrade_3=?,upgrade_4=?,upgrade_5=?,upgrade_6=?,upgrade_7=?,upgrade_8=?,star_1=?,star_2=?,star_3=?,star_4=?,star_5=?,star_6=?,star_7=?,star_8=?,star_9=?,revision=revision+1,updated_at=NOW() WHERE id=1 AND revision=?');
    $stmt->bind_param('iddddddddddddddddddi',$exp,$drop,$u[0],$u[1],$u[2],$u[3],$u[4],$u[5],$u[6],$u[7],$s[0],$s[1],$s[2],$s[3],$s[4],$s[5],$s[6],$s[7],$s[8],$revision);
    if (!$stmt->execute()) reply(['error'=>'Không thể lưu cấu hình'],500);
    if ($stmt->affected_rows!==1) reply(['error'=>'Cấu hình vừa được sửa ở phiên khác. Tải lại dữ liệu trước khi lưu.'],409);
    reply(['message'=>'Đã lưu. Server nhận cấu hình trong khoảng 3 giây.']);
}
?>
<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Điều chỉnh tỷ lệ server</title><style>
*{box-sizing:border-box}body{margin:0;padding:28px 16px;background:linear-gradient(120deg,#a9dcfa,#70adfb);font-family:Arial,sans-serif;color:#263e52;min-height:100vh}.wrap{max-width:960px;margin:auto}header{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-bottom:20px}h1{font-size:25px;margin:0}a{color:inherit}.panel{background:white;border-radius:18px;padding:26px;box-shadow:0 20px 50px #25496c40}#status{border:1px solid #b9cddc;background:#eef7ff;border-radius:9px;padding:13px;line-height:1.6;margin-bottom:20px}.columns{display:grid;grid-template-columns:1fr 1fr;gap:18px}.group{border:1px solid #b9cddc;border-radius:12px;padding:20px;background:#f5faff}.group.wide{grid-column:1/-1}h2{font-size:18px;margin:0 0 12px}p{font-size:13px;line-height:1.7;color:#60768b;margin:10px 0}label{display:block;font-size:14px;font-weight:bold;margin-bottom:8px}input{width:100%;padding:11px;border:1px solid #adc2d0;border-radius:7px;font:inherit;background:white;color:#263e52}.upgrades{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-top:18px}.actions{display:flex;gap:10px;flex-wrap:wrap;margin-top:24px}button{border:1px solid #adc2d0;border-radius:7px;padding:12px 18px;cursor:pointer;font:inherit;background:#eef7ff;color:#263e52}#save{background:#16864b;color:white;border-color:#16864b}button:disabled{opacity:.5;cursor:default}#message{font-size:14px;line-height:1.6;margin-top:16px}.error{color:#bc2445}#example{color:#177348}@media(max-width:650px){.columns{grid-template-columns:1fr}.upgrades{grid-template-columns:repeat(2,1fr)}.panel{padding:16px}h1{font-size:21px}}
</style><?php if(isset($_GET["embed"])): ?><style>body{background:#faffff;padding:16px;font-family:Georgia}header{display:none}.panel{padding:16px;box-shadow:none}.group{padding:14px}.columns{grid-template-columns:1fr}<?php $view=$_GET["view"]??"";if($view==="upgrade"):?>.group:not(.wide){display:none}<?php elseif($view==="exp"):?>.group:not(:first-child){display:none}<?php elseif($view==="drop"):?>.group:first-child,.group.wide{display:none}<?php endif;?></style><?php endif;?><link rel="stylesheet" href="/typography.css?v=20261014-1"><link rel="stylesheet" href="admin-ui.css?v=20261015-5"><script src="admin-ui.js?v=20261015-3" defer></script></head><body class="admin-page" data-admin-page="rates"><?php require_once __DIR__.'/admin_ui.php'; adminPageToolbar('Tỷ lệ máy chủ'); ?><div class="wrap"><header><h1>⚙ Điều chỉnh tỷ lệ server</h1><a href="panel.php">← Về panel</a></header><main class="panel"><div id="status" role="status">Đang tải cấu hình…</div><form id="form"><div class="columns"><section class="group"><h2>⚡ EXP toàn server</h2><label for="exp">Hệ số sức mạnh / tiềm năng</label><input id="exp" type="number" min="1" max="100" step="1" required><p>x1 là mức gốc, x3 là gấp 3 lần. Áp dụng tại chỗ tính sức mạnh/tiềm năng hiện có, cùng các điều kiện và hiệu ứng của game.</p></section><section class="group"><h2>🎁 Rơi đồ từ quái</h2><label for="drop">Hệ số xác suất rơi đồ</label><input id="drop" type="number" min="0" max="100" step="0.001" required><p id="example"></p><p>x1 giữ tỷ lệ gốc. Điều chỉnh các lần xét rơi đồ ngẫu nhiên từ quái; điều kiện bản đồ, sự kiện và vật phẩm nhiệm vụ bắt buộc vẫn giữ nguyên. Thưởng boss và số lượng vật phẩm không thay đổi.</p></section><section class="group wide"><h2>🔨 Đập cấp trang bị</h2><p>Xác suất thành công cho từng lần nâng cấp, từ 0 đến 100%. Áp dụng cho nâng trang bị bằng đá ở Bà Hạt Mít. Đập sao được chỉnh riêng bên dưới; bông tai và chế tạo giữ cơ chế riêng.</p><div id="upgrades" class="upgrades"></div></section><section class="group wide"><h2>⭐ Đập sao (pha lê hóa)</h2><p>Tỷ lệ tạo lỗ sao pha lê cho trang bị, từ 0 đến 9 sao. Tỷ lệ hiển thị tại Bà Hạt Mít khớp với tỷ lệ thành công thực tế. Ép viên sao vào lỗ có sẵn và nâng sao pha lê có cơ chế riêng.</p><div id="stars" class="upgrades"></div></section></div><div class="actions"><button id="save" type="submit" disabled>💾 Lưu tỷ lệ</button><button id="reload" type="button">Hủy thay đổi / tải lại</button><button id="defaults" type="button">Điền giá trị mặc định</button></div><div id="message" aria-live="polite"></div></form></main></div><script>
const csrf=<?=json_encode($_SESSION['rates_csrf'])?>, defaults={exp:3,drop:1,upgrades:[80,50,20,10,7,5,1,0.3],stars:[50,20,10,5,1,0.7,0.5,0.1,0.1]};
const view=<?=json_encode(in_array($_GET['view']??'',['exp','drop','upgrade'],true)?$_GET['view']:'all')?>;
const el=id=>document.getElementById(id);let row=null,dirty=false,busy=false,formRevision=null;
for(let i=1;i<=8;i++){const box=document.createElement('div'),label=document.createElement('label'),input=document.createElement('input');label.htmlFor='upgrade_'+i;label.textContent='+'+(i-1)+' → +'+i+' (%)';input.id='upgrade_'+i;input.type='number';input.min=0;input.max=100;input.step='0.001';input.required=true;box.append(label,input);el('upgrades').append(box);}
for(let i=1;i<=9;i++){const box=document.createElement('div'),label=document.createElement('label'),input=document.createElement('input');label.htmlFor='star_'+i;label.textContent=(i-1)+' ⭐ → '+i+' ⭐ (%)';input.id='star_'+i;input.type='number';input.min=0;input.max=100;input.step='0.001';input.required=true;box.append(label,input);el('stars').append(box);}
function message(text,error=false){el('message').textContent=text;el('message').className=error?'error':'';}
async function api(data){const r=await fetch('rates.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...data,csrf})});const j=await r.json();if(!r.ok||j.error)throw Error(j.error||'Không thể thực hiện thao tác');return j;}
function example(){const x=Number(el('drop').value);el('example').textContent='Ví dụ: tỷ lệ gốc 5% → '+Math.min(100,5*x).toLocaleString('vi-VN')+'% khi đặt x'+x.toLocaleString('vi-VN')+'.';}
function fill(values){el('exp').value=values.exp;el('drop').value=values.drop;values.upgrades.forEach((v,i)=>el('upgrade_'+(i+1)).value=v);values.stars.forEach((v,i)=>el('star_'+(i+1)).value=v);example();}
function fillRow(){fill({exp:row.exp_rate,drop:row.drop_multiplier,upgrades:Array.from({length:8},(_,i)=>row['upgrade_'+(i+1)]),stars:Array.from({length:9},(_,i)=>row['star_'+(i+1)])});formRevision=Number(row.revision);}
function status(){if(!row)return;const online=row.age!==null&&Number(row.age)>=0&&Number(row.age)<=20;el('status').textContent=(online?'● Server đang kết nối':'○ Server chưa kết nối bản điều chỉnh tỷ lệ')+' · '+(Number(row.revision)===Number(row.applied_revision)?'Đã áp dụng cấu hình #'+row.revision:'Chờ server nhận cấu hình #'+row.revision)+(dirty?' · Có thay đổi chưa lưu':'');}
async function refresh(force=false){try{row=(await api({action:'read'})).row;if(force||(!dirty&&!busy)){fillRow();dirty=false;}el('save').disabled=busy;status();}catch(e){message(e.message,true);}}
el('form').oninput=()=>{dirty=true;example();status();};el('reload').onclick=()=>{if(!busy)refresh(true);};el('defaults').onclick=()=>{if(!row||busy)return;const values={exp:Number(el('exp').value),drop:Number(el('drop').value),upgrades:Array.from({length:8},(_,i)=>Number(el('upgrade_'+(i+1)).value)),stars:Array.from({length:9},(_,i)=>Number(el('star_'+(i+1)).value))};if(view==='all'||view==='exp')values.exp=defaults.exp;if(view==='all'||view==='drop')values.drop=defaults.drop;if(view==='all'||view==='upgrade'){values.upgrades=defaults.upgrades;values.stars=defaults.stars;}fill(values);dirty=true;status();message('Đã điền giá trị mặc định cho mục đang mở. Bấm Lưu tỷ lệ để áp dụng.');};
el('form').onsubmit=async e=>{e.preventDefault();if(!row||busy)return;busy=true;el('save').disabled=true;try{const result=await api({action:'save',revision:formRevision,exp:Number(el('exp').value),drop:Number(el('drop').value),upgrades:Array.from({length:8},(_,i)=>Number(el('upgrade_'+(i+1)).value)),stars:Array.from({length:9},(_,i)=>Number(el('star_'+(i+1)).value))});dirty=false;message(result.message);await refresh(true);}catch(e){message(e.message,true);}finally{busy=false;el('save').disabled=!row;status();}};
refresh();setInterval(()=>refresh(),5000);
</script></body></html>
