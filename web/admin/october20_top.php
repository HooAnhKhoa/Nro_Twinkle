<?php
require_once __DIR__.'/panel_common.php';require_once __DIR__.'/october20_top_core.php';require_once __DIR__.'/shop_catalog_assets.php';
foreach(explode(';',file_get_contents(__DIR__.'/../october20_top_schema.sql')) as $sql)if(trim($sql)!=='')$conn->query($sql);
$data=panelInput();$action=$data['action']??'';
$templates=[];foreach($conn->query('SELECT id,NAME,TYPE,icon_id FROM item_template')->fetch_all(MYSQLI_ASSOC) as $t)$templates[(int)$t['id']]=$t;
$options=[];foreach($conn->query('SELECT id,NAME FROM item_option_template')->fetch_all(MYSQLI_ASSOC) as $o)$options[(int)$o['id']]=$o;
$missing=shopCatalogManifest();if($missing===null)panelReply(['error'=>'Chưa đọc được danh sách hình vật phẩm.'],503);
if($action==='read'){
 $season=$conn->query('SELECT * FROM october20_season ORDER BY id DESC LIMIT 1')->fetch_assoc();$ranks=[];$prizes=[];
 if($season){$season['rewards']=json_decode($season['rewards'],true);$id=(int)$season['id'];foreach(['roses','cards','gifts'] as $b){$q=$conn->prepare('SELECT s.player_id,p.name,s.score,s.reached_ms FROM october20_score s LEFT JOIN player p ON p.id=s.player_id WHERE season_id=? AND board=? ORDER BY score DESC,reached_ms,player_id LIMIT 100');$q->bind_param('is',$id,$b);$q->execute();$ranks[$b]=$q->get_result()->fetch_all(MYSQLI_ASSOC);}$prizes=$conn->query('SELECT board,COUNT(*) total,SUM(claimed_at IS NOT NULL) claimed FROM october20_prize WHERE season_id='.$id.' GROUP BY board')->fetch_all(MYSQLI_ASSOC);}
 panelReply(['season'=>$season,'defaults'=>octTopDefaults(),'ranks'=>$ranks,'prizes'=>$prizes,'items'=>array_values($templates),'missing'=>$missing,'options'=>array_values($options),'server_ms'=>(int)round(microtime(true)*1000)]);
}
if(!in_array($action,['save','pause'],true))panelReply(['error'=>'Thao tác không hợp lệ.'],422);
try{
 $v=$action==='save'?octTopValidate($data,$templates,$missing,$options):null;
 if($action==='pause'&&!is_bool($data['paused']??null))throw new RuntimeException('Trạng thái tạm tắt không hợp lệ.');
 $conn->begin_transaction();$conn->query('SELECT revision FROM panel_event_runtime WHERE id=1 FOR UPDATE');
 $old=$conn->query('SELECT * FROM october20_season ORDER BY id DESC LIMIT 1 FOR UPDATE')->fetch_assoc();
 if((int)($old['id']??0)!==ttTopInt($data['season_id']??null,0,PHP_INT_MAX))throw new RuntimeException('Đợt đã thay đổi. Tải lại trước khi lưu.');
 if($action==='pause'){
  if(!$old||!(int)$old['scheduled'])throw new RuntimeException('Chưa có lịch đang bật.');$paused=(int)$data['paused'];$id=(int)$old['id'];$q=$conn->prepare('UPDATE october20_season SET paused=? WHERE id=?');$q->bind_param('ii',$paused,$id);
 }else{
  $now=round(microtime(true)*1000);
  if($old&&(int)$old['scheduled']&&(int)$old['start_ms']<=$now&&($now<(int)$old['end_ms']||!$old['closed_at']))throw new RuntimeException('Đợt đã bắt đầu: khóa lịch, giá và thưởng. Có thể tạm tắt/mở lại, hoặc tạo đợt mới sau khi kết thúc và chốt xong.');
  $values=[$v['name'],$v['start'],$v['points'],$v['end'],$v['scheduled'],$v['rose'],$v['card'],$v['gold'],$v['rewards']];
  if(!$old||$old['closed_at']){$q=$conn->prepare('INSERT INTO october20_season(name,start_ms,points_end_ms,end_ms,scheduled,rose_gems,card_gems,gift_gold,rewards) VALUES(?,?,?,?,?,?,?,?,?)');$q->bind_param('siiiiiiis',...$values);}
  else{$values[]=(int)$old['id'];$q=$conn->prepare('UPDATE october20_season SET name=?,start_ms=?,points_end_ms=?,end_ms=?,scheduled=?,rose_gems=?,card_gems=?,gift_gold=?,rewards=? WHERE id=?');$q->bind_param('siiiiiiisi',...$values);}
 }
 $q->execute();$conn->query('UPDATE panel_event_runtime SET revision=revision+1 WHERE id=1');$conn->commit();panelReply(['message'=>$action==='pause'?'Đã đổi trạng thái tạm tắt/mở. Lịch chốt điểm giữ nguyên.':'Đã lưu lịch và thưởng 20/10. Game tự nhận trong khoảng 3 giây.']);
}catch(Throwable $e){$conn->rollback();panelReply(['error'=>$e instanceof RuntimeException?$e->getMessage():'Không lưu được cấu hình 20/10.'],409);}
