<?php
require_once __DIR__.'/panel_common.php';require_once __DIR__.'/event_catalog.php';require_once __DIR__.'/shop_catalog_assets.php';
foreach(explode(';',file_get_contents(__DIR__.'/../event_point_shops.sql')) as $sql)if(trim($sql)!=='')$conn->query($sql);
$data=panelInput();$action=$data['action']??'';$keys=['october20','trung_thu','hung_vuong','christmas','halloween','tet','summer'];
if($action==='read')panelReply(['bindings'=>$conn->query('SELECT * FROM panel_event_shop')->fetch_all(MYSQLI_ASSOC),'shops'=>$conn->query("SELECT s.id,s.tag_name,COUNT(i.id) item_count FROM shop s JOIN tab_shop t ON t.shop_id=s.id JOIN item_shop i ON i.tab_id=t.id WHERE s.npc_id=13 AND s.type_shop=3 GROUP BY s.id,s.tag_name ORDER BY s.id")->fetch_all(MYSQLI_ASSOC)]);
if($action!=='save')panelReply(['error'=>'Thao tác không hợp lệ.'],422);
$key=$data['event_key']??'';$id=$data['shop_id']??null;if(!in_array($key,$keys,true)||!is_int($id)||$id<0)panelReply(['error'=>'Sự kiện hoặc shop không hợp lệ.'],422);
try{
 $conn->begin_transaction();$conn->query('SELECT revision FROM panel_event_runtime WHERE id=1 FOR UPDATE');
 if($id){
  $q=$conn->prepare('SELECT id FROM shop WHERE id=? AND npc_id=13 AND type_shop=3 FOR UPDATE');$q->bind_param('i',$id);$q->execute();if(!$q->get_result()->fetch_assoc())throw new RuntimeException('Chọn shop đổi điểm của Quy Lão.');
  $q=$conn->prepare('SELECT i.temp_id,i.cost,it.icon_id FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN item_template it ON it.id=i.temp_id WHERE t.shop_id=?');$q->bind_param('i',$id);$q->execute();$items=$q->get_result()->fetch_all(MYSQLI_ASSOC);if(!$items)throw new RuntimeException('Shop chưa có vật phẩm.');
  foreach($items as $item){if((int)$item['cost']<=0)throw new RuntimeException('Giá điểm phải lớn hơn 0.');if($reason=shopCatalogReason((int)$item['temp_id'],(int)$item['icon_id']))throw new RuntimeException('#'.$item['temp_id'].': '.$reason);}
  $q=$conn->prepare('INSERT INTO panel_event_shop(event_key,shop_id) VALUES(?,?) ON DUPLICATE KEY UPDATE shop_id=VALUES(shop_id),revision=revision+1');$q->bind_param('si',$key,$id);
 }else{$q=$conn->prepare('DELETE FROM panel_event_shop WHERE event_key=?');$q->bind_param('s',$key);}
 $q->execute();$conn->query('UPDATE panel_event_runtime SET revision=revision+1 WHERE id=1');$conn->commit();panelReply(['message'=>'Đã gắn shop theo sự kiện. Game tự nhận trong khoảng 3 giây.']);
}catch(Throwable $e){$conn->rollback();panelReply(['error'=>$e instanceof RuntimeException?$e->getMessage():'Không lưu được shop sự kiện.'],409);}