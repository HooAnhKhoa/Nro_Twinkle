<?php
// Pure display grouping. Keeps the live configuration key and never modifies game data.
function bossGroupRows(array $rows): array {
    $groups=[];
    foreach($rows as $row){
        $maps=json_decode($row['map_ids'],true);$maps=is_array($maps)?array_values(array_unique(array_map('intval',$maps))):[];sort($maps,SORT_NUMERIC);
        $name=mb_strtolower(preg_replace('/\s+/u',' ',trim($row['name'])),'UTF-8');
        $key=$name.'|'.json_encode($maps);$groups[$key][]=$row;
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
        $result[]=$representative;
    }
    usort($result,fn($a,$b)=>strcmp(mb_strtolower($a['name'],'UTF-8'),mb_strtolower($b['name'],'UTF-8')));
    return $result;
}
