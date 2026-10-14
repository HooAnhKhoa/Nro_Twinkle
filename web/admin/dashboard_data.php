<?php
function panelGoldCount($text): int {
    $items=is_array($text)?$text:json_decode((string)$text,true);
    if(!is_array($items))return 0;
    $total=0;
    foreach($items as $raw){
        $item=is_array($raw)?$raw:(is_string($raw)?json_decode($raw,true):null);
        if(!is_array($item)||(int)($item[0]??-1)!==457)continue;
        $quantity=filter_var($item[1]??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>0]]);
        if($quantity!==false)$total+=$quantity;
    }
    return $total;
}
function panelGoldRank(array $rows,int $limit=20): array {
    $top=[];
    foreach($rows as $row){
        $total=panelGoldCount($row['items_bag']??'[]')+panelGoldCount($row['items_box']??'[]');
        if($total<=0)continue;
        $top[]=['id'=>(int)$row['id'],'name'=>$row['name'],'gender'=>(int)$row['gender'],'gold_bars'=>$total];
    }
    usort($top,fn($a,$b)=>($b['gold_bars']<=>$a['gold_bars'])?:($a['id']<=>$b['id']));
    return array_slice($top,0,$limit);
}
function panelOverview(mysqli $conn): array {
    $rows=$conn->query('SELECT id,name,gender,items_bag,items_box FROM player')->fetch_all(MYSQLI_ASSOC);
    $counts=['players'=>count($rows),'accounts'=>(int)$conn->query('SELECT COUNT(*) n FROM account')->fetch_assoc()['n'],'giftcodes'=>(int)$conn->query('SELECT COUNT(*) n FROM giftcode')->fetch_assoc()['n']];
    $activity=$conn->query("SELECT action,requested_at AS time,completed_at,result FROM panel_command ORDER BY id DESC LIMIT 8")->fetch_all(MYSQLI_ASSOC);
    foreach($conn->query('SELECT g.amount,g.created_at AS time,g.undone_at,p.name FROM panel_vnd_audit g LEFT JOIN player p ON p.id=g.player_id ORDER BY g.id DESC LIMIT 8')->fetch_all(MYSQLI_ASSOC) as $gift){
        $activity[]=['action'=>'gift','time'=>$gift['time'],'completed_at'=>$gift['time'],'result'=>'+'.number_format($gift['amount'],0,',','.').' VNĐ cho '.($gift['name']??'nhân vật đã xóa')];
        if($gift['undone_at'])$activity[]=['action'=>'undo','time'=>$gift['undone_at'],'completed_at'=>$gift['undone_at'],'result'=>'Hoàn tác tặng VNĐ cho '.($gift['name']??'nhân vật đã xóa')];
    }
    usort($activity,fn($a,$b)=>strcmp($b['time'],$a['time']));
    return ['counts'=>$counts,'gold_top'=>panelGoldRank($rows),'activity'=>array_slice($activity,0,6),'updated_at'=>(new DateTimeImmutable('now',new DateTimeZone('Asia/Ho_Chi_Minh')))->format('Y-m-d H:i:s')];
}
