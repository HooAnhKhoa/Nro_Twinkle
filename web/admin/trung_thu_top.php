<?php
require_once __DIR__.'/panel_common.php';
require_once __DIR__.'/trung_thu_top_core.php';
foreach(explode(';',file_get_contents(__DIR__.'/../trung_thu_top_schema.sql')) as $sql)if(trim($sql)!=='')$conn->query($sql);
$data=panelInput();$action=$data['action']??'';
$templates=[];foreach($conn->query('SELECT id,NAME,TYPE,icon_id FROM item_template')->fetch_all(MYSQLI_ASSOC) as $t)$templates[(int)$t['id']]=$t;
require_once __DIR__.'/shop_catalog_assets.php';
$missing=shopCatalogManifest();
if($missing===null)panelReply(['error'=>'Chưa đọc được danh sách vật phẩm khả dụng.'],503);
$options=[];foreach($conn->query('SELECT id,NAME FROM item_option_template')->fetch_all(MYSQLI_ASSOC) as $o)$options[(int)$o['id']]=$o;
if($action==='read'){
    $season=$conn->query('SELECT * FROM trung_thu_season ORDER BY id DESC LIMIT 1')->fetch_assoc();
    $ranks=[];$prizes=[];
    if($season){
        $season['rewards']=json_decode($season['rewards'],true);$id=(int)$season['id'];
        foreach(['vip','normal','vegetarian'] as $b){$q=$conn->prepare('SELECT s.player_id,p.name,s.score,s.reached_ms FROM trung_thu_score s LEFT JOIN player p ON p.id=s.player_id WHERE season_id=? AND board=? ORDER BY score DESC,reached_ms,player_id LIMIT 100');$q->bind_param('is',$id,$b);$q->execute();$ranks[$b]=$q->get_result()->fetch_all(MYSQLI_ASSOC);}
        $prizes=$conn->query('SELECT board,COUNT(*) total,SUM(claimed_at IS NOT NULL) claimed FROM trung_thu_prize WHERE season_id='.$id.' GROUP BY board')->fetch_all(MYSQLI_ASSOC);
    }
    panelReply(['season'=>$season,'defaults'=>ttTopDefaults(),'ranks'=>$ranks,'prizes'=>$prizes,'items'=>array_values(array_filter($templates,fn($t)=>!isset($missing[(string)$t['id']]))),'server_ms'=>(int)round(microtime(true)*1000)]);
}
if($action!=='save')panelReply(['error'=>'Thao tác không hợp lệ.'],422);
try{
    $v=ttTopValidate($data,$templates,$missing,$options);
    $conn->begin_transaction();
    $conn->query('SELECT revision FROM panel_event_runtime WHERE id=1 FOR UPDATE');
    $old=$conn->query('SELECT * FROM trung_thu_season ORDER BY id DESC LIMIT 1 FOR UPDATE')->fetch_assoc();
    if((int)($old['id']??0)!==ttTopInt($data['season_id']??null,0,PHP_INT_MAX))throw new RuntimeException('Đợt đã thay đổi. Tải lại trước khi lưu.');
    if($old&&$old['scheduled']&&!$old['closed_at']&&(int)$old['start_ms']<=round(microtime(true)*1000))throw new RuntimeException('Đợt đã bắt đầu: khóa thời gian, giá và phần thưởng để giữ công bằng. Chờ chốt xong để tạo đợt mới.');
    $values=[$v['name'],$v['start'],$v['end'],$v['scheduled'],$v['basis'],$v['gold'],$v['gems'],$v['rewards']];
    if(!$old||$old['closed_at']){$q=$conn->prepare('INSERT INTO trung_thu_season(name,start_ms,end_ms,scheduled,basis,vip_gold,vip_gems,rewards) VALUES(?,?,?,?,?,?,?,?)');$q->bind_param('siiisiis',...$values);}
    else{$values[]=(int)$old['id'];$q=$conn->prepare('UPDATE trung_thu_season SET name=?,start_ms=?,end_ms=?,scheduled=?,basis=?,vip_gold=?,vip_gems=?,rewards=? WHERE id=?');$q->bind_param('siiisiisi',...$values);}
    $q->execute();$conn->query('UPDATE panel_event_runtime SET revision=revision+1 WHERE id=1');$conn->commit();
    panelReply(['message'=>'Đã lưu. Lịch dùng giờ Việt Nam. Game tự mở, đóng và lưu quà khi kết thúc; nếu game tắt sẽ chốt khi khởi động lại.']);
}catch(Throwable $e){$conn->rollback();panelReply(['error'=>$e instanceof RuntimeException?$e->getMessage():'Không lưu được lịch đua top.'],409);}
