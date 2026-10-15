<?php
require_once __DIR__.'/panel_common.php';
require_once __DIR__.'/intrinsic_catalog.php';
foreach(explode(';',file_get_contents(__DIR__.'/../intrinsic_schema.sql')) as $sql)if(trim($sql)!=='')$conn->query($sql);
if($_SERVER['REQUEST_METHOD']!=='POST')panelReply(['error'=>'Cần yêu cầu POST'],405);
$data=panelInput();$action=$data['action']??'';
if($action==='read'){
    $templates=intrinsicTemplates($conn->query('SELECT * FROM intrinsic ORDER BY id')->fetch_all(MYSQLI_ASSOC));
    $config=$conn->query('SELECT revision,applied_revision,TIMESTAMPDIFF(SECOND,heartbeat,NOW()) age FROM panel_intrinsic_template_config WHERE id=1')->fetch_assoc();
    $history=$conn->query('SELECT id,intrinsic_id,created_at,undone_at,before_json,after_json FROM panel_intrinsic_template_audit ORDER BY id DESC LIMIT 20')->fetch_all(MYSQLI_ASSOC);
    panelReply(['templates'=>array_values($templates),'config'=>$config,'history'=>$history]);
}
$revision=filter_var($data['revision']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
$id=filter_var($data['intrinsic_id']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1,'max_range'=>127]]);
if($revision===false||$id===false||!in_array($action,['save','undo'],true))panelReply(['error'=>'Thao tác hoặc ID nội tại không hợp lệ'],422);
$fields=['param_from_1','param_to_1','param_from_2','param_to_2'];$values=[];
if($action==='save'){
    foreach($fields as $field){$values[$field]=filter_var($data[$field]??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>0,'max_range'=>32767]]);if($values[$field]===false)panelReply(['error'=>'Các mức phải là số nguyên từ 0 đến 32767'],422);}
    if($values['param_from_1']>$values['param_to_1']||$values['param_from_2']>$values['param_to_2'])panelReply(['error'=>'Mức thấp nhất không được lớn hơn mức cao nhất'],422);
}
$conn->begin_transaction();
try{
    $config=$conn->query('SELECT revision FROM panel_intrinsic_template_config WHERE id=1 FOR UPDATE')->fetch_assoc();
    if((int)$config['revision']!==$revision)throw new RuntimeException('Cấu hình đã thay đổi ở phiên khác. Tải lại trước khi lưu.');
    $stmt=$conn->prepare('SELECT * FROM intrinsic WHERE id=? FOR UPDATE');$stmt->bind_param('i',$id);$stmt->execute();
    $entries=intrinsicTemplates($stmt->get_result()->fetch_all(MYSQLI_ASSOC));$entry=$entries[$id]??null;
    if(!$entry||$entry['conflict'])throw new RuntimeException('Không tìm thấy nội tại hoặc các bản trùng ID có cấu hình khác nhau.');
    $before=$entry['template'];$copies=$entry['copies'];$beforeJson=json_encode($before,JSON_UNESCAPED_UNICODE);
    if($action==='undo'){
        $auditId=filter_var($data['audit_id']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
        if($auditId===false)throw new RuntimeException('Lịch sử không hợp lệ');
        $stmt=$conn->prepare('SELECT * FROM panel_intrinsic_template_audit WHERE id=? AND intrinsic_id=? FOR UPDATE');$stmt->bind_param('ii',$auditId,$id);$stmt->execute();$audit=$stmt->get_result()->fetch_assoc();
        if(!$audit||$audit['undone_at']||$audit['after_json']!==$beforeJson||(int)$audit['copies']!==$copies)throw new RuntimeException('Không thể hoàn tác vì nội tại đã thay đổi sau lần sửa này.');
        $next=json_decode($audit['before_json'],true,512,JSON_THROW_ON_ERROR);
        foreach($fields as $field)$values[$field]=(int)$next[$field];
        $stmt=$conn->prepare('UPDATE panel_intrinsic_template_audit SET undone_at=NOW() WHERE id=?');$stmt->bind_param('i',$auditId);$stmt->execute();
    }else{
        if(!is_string($data['etag']??null)||!hash_equals($entry['etag'],$data['etag']))throw new RuntimeException('Dữ liệu nội tại đã thay đổi. Tải lại trước khi lưu.');
        if(!str_contains($before['name'],'p2')&&!str_contains($before['name'],'p3')&&($values['param_from_2']!==$before['param_from_2']||$values['param_to_2']!==$before['param_to_2']))throw new RuntimeException('Nội tại này không có cặp chỉ số thứ hai.');
        $next=array_replace($before,$values);$afterJson=json_encode($next,JSON_UNESCAPED_UNICODE);
        if($afterJson===$beforeJson){$conn->commit();panelReply(['message'=>'Các mức không thay đổi.']);}
        $admin=(int)$panelAdmin['id'];$stmt=$conn->prepare('INSERT INTO panel_intrinsic_template_audit(intrinsic_id,admin_id,copies,before_json,after_json) VALUES(?,?,?,?,?)');$stmt->bind_param('iiiss',$id,$admin,$copies,$beforeJson,$afterJson);$stmt->execute();
    }
    $stmt=$conn->prepare('UPDATE intrinsic SET param_from_1=?,param_to_1=?,param_from_2=?,param_to_2=? WHERE id=?');$stmt->bind_param('iiiii',$values['param_from_1'],$values['param_to_1'],$values['param_from_2'],$values['param_to_2'],$id);$stmt->execute();
    $conn->query('UPDATE panel_intrinsic_template_config SET revision=revision+1 WHERE id=1');$conn->commit();
    panelReply(['message'=>$action==='undo'?'Đã hoàn tác cấu hình nội tại.':'Đã lưu. Danh sách và lần mở nội tại mới nhận cấu hình trong khoảng 3 giây.']);
}catch(Throwable $error){$conn->rollback();panelReply(['error'=>$error instanceof RuntimeException?$error->getMessage():'Không thể lưu cấu hình nội tại'],409);}
