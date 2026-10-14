<?php
// Ordered by TaskService.checkDoneTaskKillBoss (main task, then subtask).
function bossTaskOrder(array $row): int {
    $name=mb_strtolower(trim($row['name']),'UTF-8');
    $order=['kuku'=>1900,'mập đầu đinh'=>1901,'rambo'=>1902,
        'số 4'=>2001,'số 3'=>2002,'số 2'=>2003,'số 1'=>2004,'tiểu đội trưởng'=>2005,
        'số 4 namek'=>2011,'số 3 namek'=>2012,'số 2 namek'=>2013,'số 1 namek'=>2014,'tiểu đội trưởng namek'=>2015,
        'fide đại ca 1'=>2101,'fide đại ca 2'=>2102,'fide đại ca 3'=>2103,
        'fide vàng'=>2110,'cooler'=>2120,
        'bojack'=>2200,'siêu bojack'=>2201,'bujin'=>2202,'kogu'=>2203,'bido'=>2204,'zangya'=>2205,
        'android 19'=>2301,'dr.kôrê'=>2302,
        'android 15'=>2401,'android 14'=>2402,'android 13'=>2403,
        'poc'=>2501,'pic'=>2502,'king kong'=>2503,
        'xên bọ hung'=>2601,'xên hoàn thiện'=>2704,'siêu bọ hung'=>2704,
        'drabura'=>2801,'bui bui'=>2802,'ya côn'=>2804,'mabư'=>2806,'mabư mập'=>2807,'super bư'=>2808,
        'baby'=>2900,'black goku'=>2901,'cumber'=>2902,'broly'=>3000,'super broly'=>3001];
    if(preg_match('/^xên con ([1-7])$/u',$name))return 2703;
    // The Cell forms are one runtime boss; the independent Super Cell is task 27.
    if($name==='xên hoàn thiện'&&str_contains($row['boss_key'],'XenBoHung'))return 2602;
    return $order[$name]??9999;
}
// Pure display grouping. Keeps the live configuration key and never modifies game data.
function bossGroupRows(array $rows): array {
    $groups=[];
    foreach($rows as $row){
        $maps=json_decode($row['map_ids'],true);$maps=is_array($maps)?array_values(array_unique(array_map('intval',$maps))):[];sort($maps,SORT_NUMERIC);
        $name=mb_strtolower(preg_replace('/\s+/u',' ',trim($row['name'])),'UTF-8');
        // Yardrat training mobs are not entries in the boss editor.
        if(preg_match('/^(chiến binh|tân binh|tập sự|đội trưởng)(?:\s*-\s*\d+)?$/u',$name)
            ||preg_match('/^ở dơ(?:\s+\d+)?$/u',$name)
            ||$name==='sói hẹc quyn')continue;
        $key=preg_match('/^mặt trời(?:\s+\d+)?$/u',$name)?'mặt trời':$name.'|'.json_encode($maps);
        $groups[$key][]=$row;
    }
    $result=[];
    foreach($groups as $members){
        usort($members,function($a,$b){
            $liveA=$a['age']!==null&&(int)$a['age']>=0&&(int)$a['age']<=20;
            $liveB=$b['age']!==null&&(int)$b['age']>=0&&(int)$b['age']<=20;
            if($liveA!==$liveB)return $liveA?-1:1;
            $canonicalA=str_starts_with($a['boss_key'],'nro.');$canonicalB=str_starts_with($b['boss_key'],'nro.');
            if($canonicalA!==$canonicalB)return $canonicalA?-1:1;
            return strcmp($b['heartbeat']??'',$a['heartbeat']??'');
        });
        $representative=$members[0];$locations=[];$count=0;$hasPosition=false;
        foreach($members as $member){
            $live=$member['age']!==null&&(int)$member['age']>=0&&(int)$member['age']<=20;
            if(!$live||$member['current_locations']===null)continue;
            $hasPosition=true;
            foreach($member['locations'] as $position){
                if(!isset($position['map_id'],$position['zone_id']))continue;
                $place=(int)$position['map_id'].':'.(int)$position['zone_id'];
                if(!isset($locations[$place])){$locations[$place]=$position;$locations[$place]['count']=0;}
                $locations[$place]['count']+=max(1,(int)($position['count']??1));
            }
        }
        if($hasPosition){$representative['locations']=array_values($locations);$representative['current_locations']=json_encode($representative['locations'],JSON_UNESCAPED_UNICODE);$representative['online_count']=array_sum(array_column($representative['locations'],'count'));}
        $representative['task_order']=bossTaskOrder($representative);
        $representative['task_group']=$representative['task_order']===9999?'Boss khác':'Nhiệm vụ '.intdiv($representative['task_order'],100);
        $result[]=$representative;
    }
    usort($result,fn($a,$b)=>($a['task_order']<=>$b['task_order'])?:strnatcasecmp($a['name'],$b['name'])?:strcmp($a['map_ids'],$b['map_ids']));
    return $result;
}
