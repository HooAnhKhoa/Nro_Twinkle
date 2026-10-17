<?php
function botPanelSettings(array $data): array {
    if (!is_bool($data['enabled']??null) || !is_int($data['farm']??null) || !is_int($data['hunt']??null)
        || $data['farm']<0 || $data['hunt']<0 || $data['farm']+$data['hunt']>3
        || ($data['enabled'] && $data['farm']+$data['hunt']===0)) throw new InvalidArgumentException('Chọn từ 0 đến 3 bot mỗi loại, tổng tối đa 3; bật cần ít nhất 1 bot.');
    return ['enabled'=>$data['enabled'],'farm'=>$data['farm'],'hunt'=>$data['hunt']];
}
function botPanelConfig(string $root): array {
    $file=$root.'/bot-trial.properties';$raw=is_file($file)?file_get_contents($file):'';
    if($raw===false)throw new RuntimeException('Không đọc được cấu hình bot.');
    $p=$raw===''?[]:parse_ini_string($raw,false,INI_SCANNER_RAW);if(!is_array($p))throw new RuntimeException('Cấu hình bot không hợp lệ.');
    $enabled=filter_var($p['enabled']??'false',FILTER_VALIDATE_BOOLEAN,FILTER_NULL_ON_FAILURE);
    $farm=filter_var($p['farm']??'2',FILTER_VALIDATE_INT);$hunt=filter_var($p['hunt']??'1',FILTER_VALIDATE_INT);
    $settings=botPanelSettings(['enabled'=>$enabled,'farm'=>$farm,'hunt'=>$hunt]);
    return $settings+['revision'=>hash('sha256',$raw)];
}
function botPanelText(array $s): string {return 'enabled='.($s['enabled']?'true':'false')."\nfarm=".$s['farm']."\nhunt=".$s['hunt']."\n";}
function botPanelAtomic(string $path,string $text): void {
    $temp=tempnam(dirname($path),'bot-tmp-');if($temp===false)throw new RuntimeException('Không tạo được file cấu hình.');
    try {if(file_put_contents($temp,$text,LOCK_EX)!==strlen($text))throw new RuntimeException('Không ghi được cấu hình.');if(!rename($temp,$path))throw new RuntimeException('Không thay được cấu hình; bản cũ được giữ nguyên.');}
    finally {if(is_file($temp))unlink($temp);}
}
function botPanelUndoRecord(string $root): ?array {
    $file=$root.'/data/bot-panel-last-change.json';if(!is_file($file))return null;
    $audit=json_decode(file_get_contents($file),true);return is_array($audit)?$audit:null;
}
function botPanelCanUndo(string $root,array $config): bool {
    $a=botPanelUndoRecord($root);return $a && empty($a['undone']) && is_string($a['after']??null) && hash_equals($a['after'],$config['revision']);
}
function botPanelChange(string $root,string $action,array $data,int $admin): array {
    $dir=$root.'/data';if(!is_dir($dir)&&!mkdir($dir,0777,true))throw new RuntimeException('Không tạo được thư mục dữ liệu bot.');
    $lock=fopen($dir.'/bot-panel.lock','c');if(!$lock||!flock($lock,LOCK_EX))throw new RuntimeException('Không khóa được cấu hình bot.');
    try {
        $current=botPanelConfig($root);$revision=$data['revision']??null;
        if(!is_string($revision)||!hash_equals($current['revision'],$revision))throw new RuntimeException('Cấu hình đã thay đổi. Tải lại trước khi lưu.');
        $before=botPanelSettings($current);
        if($action==='undo') {
            if(!botPanelCanUndo($root,$current))throw new RuntimeException('Không còn thay đổi gần nhất để hoàn tác.');
            $a=botPanelUndoRecord($root);$next=botPanelSettings($a['before']);
            botPanelAtomic($root.'/bot-trial.properties',botPanelText($next));
            $a['undone']=true;botPanelAtomic($dir.'/bot-panel-last-change.json',json_encode($a,JSON_THROW_ON_ERROR));
            return ['message'=>'Đã hoàn tác. Game nhận cấu hình trong khoảng 2–7 giây.'];
        }
        $next=botPanelSettings($data);if($next===$before)return ['message'=>'Cấu hình không thay đổi.'];
        $text=botPanelText($next);$audit=['before'=>$before,'after'=>hash('sha256',$text),'admin'=>$admin,'time'=>time(),'undone'=>false];
        $oldAudit=is_file($dir.'/bot-panel-last-change.json')?file_get_contents($dir.'/bot-panel-last-change.json'):null;
        botPanelAtomic($dir.'/bot-panel-last-change.json',json_encode($audit,JSON_THROW_ON_ERROR));
        try{botPanelAtomic($root.'/bot-trial.properties',$text);}catch(Throwable $e){if($oldAudit!==null)botPanelAtomic($dir.'/bot-panel-last-change.json',$oldAudit);else unlink($dir.'/bot-panel-last-change.json');throw $e;}
        return ['message'=>'Đã lưu. Game nhận cấu hình trong khoảng 2–7 giây; khi server tắt sẽ áp dụng ở lần khởi động tiếp theo.'];
    } finally {flock($lock,LOCK_UN);fclose($lock);}
}
function botPanelRead(string $root,mysqli $conn): array {
    $config=botPanelConfig($root);$snapshot=null;$file=$root.'/data/trial-bots-status.json';
    if(is_file($file)){$raw=file_get_contents($file);$snapshot=json_decode($raw,true);if(!is_array($snapshot)){usleep(20000);$snapshot=json_decode(file_get_contents($file),true);}}
    $age=is_array($snapshot)&&is_numeric($snapshot['updated']??null)?max(0,microtime(true)-$snapshot['updated']/1000):null;
    if(is_array($snapshot['bots']??null)) {
        $ids=array_unique(array_map(static fn($b)=>(int)($b['map']??-1),$snapshot['bots']));$names=[];
        if($ids)foreach($conn->query('SELECT id,name AS map_name FROM map_template WHERE id IN('.implode(',',$ids).')')->fetch_all(MYSQLI_ASSOC) as $row)$names[(int)$row['id']]=$row['map_name'];
        foreach($snapshot['bots'] as &$bot)$bot['map_name']=$names[(int)($bot['map']??-1)]??null;unset($bot);
    }
    $r=$conn->query('SELECT maintenance,TIMESTAMPDIFF(SECOND,heartbeat,NOW()) age FROM panel_runtime WHERE id=1')->fetch_assoc();
    $live=$r && $r['age']!==null && (int)$r['age']>=0 && (int)$r['age']<=20;
    return ['config'=>$config,'can_undo'=>botPanelCanUndo($root,$config),
        'server'=>['live'=>(bool)$live,'maintenance'=>(bool)($r['maintenance']??false)],
        'runtime'=>['fresh'=>$live&&$age!==null&&$age<=15,'age'=>$age,'bots'=>is_array($snapshot['bots']??null)?$snapshot['bots']:[]]];
}
