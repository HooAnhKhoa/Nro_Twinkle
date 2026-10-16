<?php
require_once __DIR__.'/panel_common.php';
require_once __DIR__.'/october20_config.php';
foreach(explode(';',file_get_contents(__DIR__.'/../october20_schema.sql')) as $sql)if(trim($sql)!=='')$conn->query($sql);
if($_SERVER['REQUEST_METHOD']!=='POST')panelReply(['error'=>'Chỉ hỗ trợ POST.'],405);
$data=panelInput();$action=$data['action']??'';
if($action==='read'){
    $row=$conn->query('SELECT revision,settings FROM october20_config WHERE id=1')->fetch_assoc();
    $row['settings']=json_decode($row['settings'],true,512,JSON_THROW_ON_ERROR);
    $row['history']=$conn->query('SELECT id,admin_id,revision,created_at FROM october20_config_audit ORDER BY id DESC LIMIT 10')->fetch_all(MYSQLI_ASSOC);panelReply($row);
}
if(!in_array($action,['save','undo'],true)||!is_int($data['revision']??null)||$data['revision']<1)panelReply(['error'=>'Thao tác không hợp lệ.'],422);
try{
    $settings=$action==='save'?october20Validate($data['settings']??[]):null;
}catch(Throwable $error){panelReply(['error'=>$error instanceof InvalidArgumentException?$error->getMessage():'Cấu hình không hợp lệ.'],422);}
$conn->begin_transaction();
try{
    $row=$conn->query('SELECT revision,settings FROM october20_config WHERE id=1 FOR UPDATE')->fetch_assoc();
    if((int)$row['revision']!==$data['revision'])throw new RuntimeException('Cấu hình đã thay đổi. Tải lại trước khi lưu.');
    if($action==='undo'){
        $last=$conn->query('SELECT revision,before_settings FROM october20_config_audit ORDER BY id DESC LIMIT 1')->fetch_assoc();
        if(!$last||(int)$last['revision']!==(int)$row['revision'])throw new RuntimeException('Chưa có thay đổi có thể hoàn tác.');
        $settings=october20Validate(json_decode($last['before_settings'],true,512,JSON_THROW_ON_ERROR));
    }
    $next=json_encode($settings,JSON_UNESCAPED_UNICODE|JSON_THROW_ON_ERROR);$before=$row['settings'];$revision=(int)$row['revision']+1;$admin=(int)$panelAdmin['id'];
    $q=$conn->prepare('INSERT INTO october20_config_audit(admin_id,revision,before_settings,after_settings) VALUES(?,?,?,?)');$q->bind_param('iiss',$admin,$revision,$before,$next);$q->execute();
    $q=$conn->prepare('UPDATE october20_config SET revision=?,settings=? WHERE id=1');$q->bind_param('is',$revision,$next);$q->execute();$conn->commit();
    panelReply(['message'=>'Đã lưu cấu hình 20/10. Game nhận trong khoảng 3 giây; menu xác nhận cũ cần mở lại.']);
}catch(Throwable $error){$conn->rollback();panelReply(['error'=>$error instanceof RuntimeException?$error->getMessage():'Không thể lưu cấu hình 20/10.'],409);}
