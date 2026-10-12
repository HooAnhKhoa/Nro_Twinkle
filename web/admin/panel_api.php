<?php
require_once 'panel_common.php';
panelSchema();
$data=panelInput(); $action=$data['action']??'';
try {
    if($action==='status') {
        $runtime=$conn->query('SELECT *,TIMESTAMPDIFF(SECOND,heartbeat,NOW()) AS age FROM panel_runtime WHERE id=1')->fetch_assoc();
        $codes=$conn->query('SELECT id,code,count_left FROM giftcode ORDER BY id DESC LIMIT 40')->fetch_all(MYSQLI_ASSOC);
        panelReply(['runtime'=>$runtime,'giftcodes'=>$codes]);
    }
    if($action==='players') {
        $q='%'.mb_substr((string)($data['query']??''),0,60).'%';
        $stmt=$conn->prepare('SELECT p.id,p.name,p.account_id,a.vnd FROM player p JOIN account a ON a.id=p.account_id WHERE p.name LIKE ? ORDER BY p.name LIMIT 40');
        $stmt->bind_param('s',$q);$stmt->execute(); panelReply(['players'=>$stmt->get_result()->fetch_all(MYSQLI_ASSOC)]);
    }
    if($action==='gift_history') {
        panelReply(['rows'=>$conn->query('SELECT g.*,p.name FROM panel_vnd_audit g LEFT JOIN player p ON p.id=g.player_id ORDER BY g.id DESC LIMIT 30')->fetch_all(MYSQLI_ASSOC)]);
    }
    if($action==='gift') {
        $id=filter_var($data['player_id']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
        $amount=filter_var($data['amount']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1,'max_range'=>2147483647]]);
        $key=$data['request_key']??'';
        if(!$id||!$amount||!is_string($key)||!preg_match('/^[a-zA-Z0-9-]{16,64}$/',$key)) panelReply(['error'=>'Chọn nhân vật và nhập số VNĐ nguyên dương hợp lệ.'],422);
        $conn->begin_transaction();
        $stmt=$conn->prepare('SELECT account_id FROM player WHERE id=?');$stmt->bind_param('i',$id);$stmt->execute();$p=$stmt->get_result()->fetch_assoc();
        if(!$p) throw new RuntimeException('Không tìm thấy nhân vật');
        $accountId=(int)$p['account_id'];
        $stmt=$conn->prepare('SELECT vnd,last_time_login,last_time_logout FROM account WHERE id=? FOR UPDATE');$stmt->bind_param('i',$accountId);$stmt->execute();$a=$stmt->get_result()->fetch_assoc();
        $stmt=$conn->prepare('SELECT id FROM panel_vnd_audit WHERE request_key=?');$stmt->bind_param('s',$key);$stmt->execute();
        if($stmt->get_result()->fetch_assoc()) {$conn->rollback();panelReply(['message'=>'Lần tặng này đã xử lý; không cộng trùng.']);}
        if(!$a||(int)$a['vnd']+$amount>2147483647) throw new RuntimeException('Số dư vượt giới hạn VNĐ của game');
        if($a['last_time_login']>$a['last_time_logout']) throw new RuntimeException('Nhân vật đang đăng nhập. Hãy cho nhân vật thoát hoặc dùng Kick trước khi tặng để đồng bộ VNĐ.');
        $before=(int)$a['vnd'];$after=$before+$amount;$adminId=(int)$panelAdmin['id'];
        $stmt=$conn->prepare('UPDATE account SET vnd=? WHERE id=?');$stmt->bind_param('ii',$after,$accountId);$stmt->execute();
        $stmt=$conn->prepare('INSERT INTO panel_vnd_audit(request_key,admin_id,player_id,account_id,amount,balance_before,balance_after) VALUES(?,?,?,?,?,?,?)');$stmt->bind_param('siiiiii',$key,$adminId,$id,$accountId,$amount,$before,$after);$stmt->execute();$conn->commit();
        panelReply(['message'=>'Đã cộng '.number_format($amount).' VNĐ. Số dư mới: '.number_format($after).' VNĐ.']);
    }
    if($action==='undo_gift') {
        $id=filter_var($data['id']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);if(!$id) panelReply(['error'=>'Lần tặng không hợp lệ'],422);
        $conn->begin_transaction();$stmt=$conn->prepare('SELECT * FROM panel_vnd_audit WHERE id=? FOR UPDATE');$stmt->bind_param('i',$id);$stmt->execute();$gift=$stmt->get_result()->fetch_assoc();
        if(!$gift||$gift['undone_at']) throw new RuntimeException('Lần tặng đã hoàn tác hoặc không tồn tại');
        $stmt=$conn->prepare('SELECT vnd,last_time_login,last_time_logout FROM account WHERE id=? FOR UPDATE');$stmt->bind_param('i',$gift['account_id']);$stmt->execute();$a=$stmt->get_result()->fetch_assoc();
        if(!$a||$a['last_time_login']>$a['last_time_logout']) throw new RuntimeException('Tài khoản phải thoát game trước khi hoàn tác');
        if((int)$a['vnd']<(int)$gift['amount']) throw new RuntimeException('Không thể hoàn tác: số VNĐ đã được sử dụng');
        $stmt=$conn->prepare('UPDATE account SET vnd=vnd-? WHERE id=?');$stmt->bind_param('ii',$gift['amount'],$gift['account_id']);$stmt->execute();
        $stmt=$conn->prepare('UPDATE panel_vnd_audit SET undone_at=NOW() WHERE id=?');$stmt->bind_param('i',$id);$stmt->execute();$conn->commit();panelReply(['message'=>'Đã hoàn tác lần tặng VNĐ.']);
    }
    if(in_array($action,['kick','maintenance'],true)) {
        $r=$conn->query('SELECT TIMESTAMPDIFF(SECOND,heartbeat,NOW()) AS age,maintenance FROM panel_runtime WHERE id=1')->fetch_assoc();
        if($r['age']===null||(int)$r['age']>20) panelReply(['error'=>'Server chưa kết nối bản panel mới.'],409);
        if((int)$r['maintenance']) panelReply(['error'=>'Server đang bảo trì.'],409);
        $id=$action==='kick'?filter_var($data['player_id']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]):null;
        if($action==='kick'&&!$id) panelReply(['error'=>'Chọn nhân vật cần kick'],422);
        $stmt=$conn->prepare('INSERT INTO panel_command(action,player_id) VALUES(?,?)');$stmt->bind_param('si',$action,$id);$stmt->execute();panelReply(['command_id'=>$conn->insert_id,'message'=>'Đã gửi lệnh; đang chờ server xác nhận.']);
    }
    if($action==='command_status') {
        $id=(int)($data['id']??0);$stmt=$conn->prepare('SELECT completed_at,result FROM panel_command WHERE id=?');$stmt->bind_param('i',$id);$stmt->execute();panelReply(['command'=>$stmt->get_result()->fetch_assoc()]);
    }
    panelReply(['error'=>'Thao tác không hợp lệ'],422);
} catch(Throwable $e) {$conn->rollback();error_log($e->getMessage());panelReply(['error'=>$e instanceof RuntimeException?$e->getMessage():'Không thể thực hiện thao tác. Kiểm tra log server.'],409);}
