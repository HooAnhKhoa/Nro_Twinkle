<?php
require_once 'panel_common.php';
$conn->query('CREATE TABLE IF NOT EXISTS panel_intrinsic_audit(id BIGINT AUTO_INCREMENT PRIMARY KEY,player_id BIGINT NOT NULL,admin_id INT NOT NULL,before_json TEXT NOT NULL,after_json TEXT NOT NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,undone_at DATETIME NULL)');
if($_SERVER['REQUEST_METHOD']==='POST') {
    $d=panelInput();$id=(int)($d['player_id']??0);
    try {
        if(($d['action']??'')==='read') {
            $s=$conn->prepare('SELECT id,name,gender,data_intrinsic FROM player WHERE id=?');$s->bind_param('i',$id);$s->execute();$p=$s->get_result()->fetch_assoc();if(!$p)panelReply(['error'=>'Không tìm thấy nhân vật'],404);
            $templates=$conn->query('SELECT * FROM intrinsic ORDER BY id')->fetch_all(MYSQLI_ASSOC);
            $s=$conn->prepare('SELECT id,created_at,undone_at FROM panel_intrinsic_audit WHERE player_id=? ORDER BY id DESC LIMIT 10');$s->bind_param('i',$id);$s->execute();
            panelReply(['player'=>$p,'values'=>json_decode($p['data_intrinsic'],true),'templates'=>$templates,'history'=>$s->get_result()->fetch_all(MYSQLI_ASSOC)]);
        }
        $conn->begin_transaction();$s=$conn->prepare('SELECT p.data_intrinsic,p.gender,a.last_time_login,a.last_time_logout FROM player p JOIN account a ON a.id=p.account_id WHERE p.id=? FOR UPDATE');$s->bind_param('i',$id);$s->execute();$p=$s->get_result()->fetch_assoc();
        if(!$p||$p['last_time_login']>$p['last_time_logout'])throw new RuntimeException('Nhân vật phải thoát game trước khi sửa nội tại');
        $old=$p['data_intrinsic'];
        if(($d['action']??'')==='undo') {
            $auditId=(int)($d['id']??0);$s=$conn->prepare('SELECT * FROM panel_intrinsic_audit WHERE id=? AND player_id=? FOR UPDATE');$s->bind_param('ii',$auditId,$id);$s->execute();$a=$s->get_result()->fetch_assoc();
            if(!$a||$a['undone_at']||$a['after_json']!==$old)throw new RuntimeException('Không thể hoàn tác: dữ liệu đã thay đổi sau lần sửa này');$next=$a['before_json'];
            $s=$conn->prepare('UPDATE panel_intrinsic_audit SET undone_at=NOW() WHERE id=?');$s->bind_param('i',$auditId);$s->execute();
        }elseif(($d['action']??'')==='save') {
            $values=json_decode($old,true);if(!is_array($values)||count($values)<4)throw new RuntimeException('Dữ liệu nội tại hiện tại không hợp lệ');
            $templateId=filter_var($d['intrinsic_id']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>0,'max_range'=>127]]);
            $p1=filter_var($d['param1']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>0,'max_range'=>32767]]);$p2=filter_var($d['param2']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>0,'max_range'=>32767]]);
            if($templateId===false||$p1===false||$p2===false)throw new RuntimeException('Chỉ số nội tại không hợp lệ');
            $s=$conn->prepare('SELECT * FROM intrinsic WHERE id=?');$s->bind_param('i',$templateId);$s->execute();$t=$s->get_result()->fetch_assoc();
            if(!$t||!in_array((int)$t['gender'],[(int)$p['gender'],3],true)||$p1<$t['param_from_1']||$p1>$t['param_to_1']||$p2<$t['param_from_2']||$p2>$t['param_to_2'])throw new RuntimeException('Nội tại hoặc chỉ số không đúng chủng tộc / giới hạn của game');
            $values[0]=$templateId;$values[1]=$p1;$values[2]=$p2;$next=json_encode($values,JSON_UNESCAPED_UNICODE);$admin=(int)$panelAdmin['id'];
            $s=$conn->prepare('INSERT INTO panel_intrinsic_audit(player_id,admin_id,before_json,after_json) VALUES(?,?,?,?)');$s->bind_param('iiss',$id,$admin,$old,$next);$s->execute();
        }else throw new RuntimeException('Thao tác không hợp lệ');
        $s=$conn->prepare('UPDATE player SET data_intrinsic=? WHERE id=?');$s->bind_param('si',$next,$id);$s->execute();$conn->commit();panelReply(['message'=>'Đã cập nhật nội tại. Áp dụng khi nhân vật đăng nhập lại.']);
    }catch(Throwable $e){$conn->rollback();panelReply(['error'=>$e instanceof RuntimeException?$e->getMessage():'Không thể lưu nội tại'],409);}
}
?><!doctype html><html lang="vi"><head><meta charset="utf-8"><link rel="stylesheet" href="panel.css"><style>body{padding:18px;background:#faffff}.field{margin:12px 0}h1{font-size:17px}</style><link rel="stylesheet" href="/typography.css?v=20261014-1"></head><body><h1>🎯 Nội tại nhân vật</h1><div class="field"><label for="search">Tìm nhân vật</label><input id="search" placeholder="Nhập tên nhân vật"></div><div class="field"><label for="players">Chọn nhân vật</label><select id="players"><option value="">Chọn nhân vật…</option></select></div><form id="form" hidden><div class="field"><label for="template">Nội tại</label><select id="template"></select></div><p id="limits" class="hint"></p><div class="form-grid"><div class="field"><label>Tham số 1</label><input id="param1" type="number" required></div><div class="field"><label>Tham số 2</label><input id="param2" type="number" required></div></div><button class="primary">Lưu nội tại</button><h2>Lịch sử sửa / hoàn tác</h2><div id="history"></div></form><p id="result" role="status"></p><script>
const csrf=<?=json_encode($_SESSION['panel_csrf'])?>;const el=id=>document.getElementById(id);let templates=[],timer,version=0,busy=false;
async function api(body,file='intrinsic.php'){const r=await fetch(file,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,csrf})});const j=await r.json();if(!r.ok||j.error)throw Error(j.error||'Không thể thực hiện');return j;}
function limits(){const t=templates.find(t=>Number(t.id)===Number(el('template').value));if(!t)return;for(const n of [1,2]){el('param'+n).min=t['param_from_'+n];el('param'+n).max=t['param_to_'+n];}el('limits').textContent='Tham số 1: '+t.param_from_1+'–'+t.param_to_1+'; Tham số 2: '+t.param_from_2+'–'+t.param_to_2;}
el('search').oninput=()=>{clearTimeout(timer);const v=++version;el('players').replaceChildren(new Option('Chọn nhân vật…',''));el('form').hidden=true;timer=setTimeout(async()=>{try{const j=await api({action:'players',query:el('search').value},'panel_api.php');if(v!==version)return;for(const p of j.players)el('players').append(new Option(p.name+' (#'+p.id+')',p.id));}catch(e){el('result').textContent=e.message;}},250);};
async function load(){try{const j=await api({action:'read',player_id:Number(el('players').value)});templates=j.templates.filter(t=>[Number(j.player.gender),3].includes(Number(t.gender)));el('template').replaceChildren();for(const t of templates)el('template').append(new Option(t.NAME+' (#'+t.id+')',t.id));el('template').value=j.values[0];el('param1').value=j.values[1];el('param2').value=j.values[2];limits();el('form').hidden=false;el('history').replaceChildren();for(const h of j.history){const b=document.createElement('button');b.type='button';b.className='secondary';b.textContent=h.created_at+(h.undone_at?' · Đã hoàn tác':' · Hoàn tác');b.disabled=!!h.undone_at;b.onclick=async()=>{if(!confirm('Hoàn tác lần sửa nội tại này?'))return;try{el('result').textContent=(await api({action:'undo',id:Number(h.id),player_id:Number(el('players').value)})).message;await load();}catch(e){el('result').textContent=e.message;}};el('history').append(b);}}catch(e){el('result').textContent=e.message;}}
el('players').onchange=()=>el('players').value?load():el('form').hidden=true;el('template').onchange=limits;el('form').onsubmit=async e=>{e.preventDefault();if(busy)return;busy=true;try{el('result').textContent=(await api({action:'save',player_id:Number(el('players').value),intrinsic_id:Number(el('template').value),param1:Number(el('param1').value),param2:Number(el('param2').value)})).message;await load();}catch(e){el('result').textContent=e.message;}finally{busy=false;}};
</script></body></html>
