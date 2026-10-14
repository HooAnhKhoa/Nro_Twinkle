<?php
require __DIR__.'/../web/admin/boss_group.php';
function check($condition,$name){if(!$condition)throw new RuntimeException('FAIL: '.$name);echo 'PASS: '.$name.PHP_EOL;}
function row($key,$name,$maps,$age,$locations){return ['boss_key'=>$key,'name'=>$name,'map_ids'=>json_encode($maps),'age'=>$age,'heartbeat'=>'2026-10-14 12:00:00','current_locations'=>$locations===null?null:json_encode($locations),'locations'=>$locations??[],'online_count'=>99];}
$p=['map_id'=>14,'map_name'=>'Làng Kakarot','zone_id'=>0,'count'=>1];
$old=row('BossManager:-925','Baby',[14,15],100,null);
$live=row('nro.models.boss.Baby','Baby',[15,14],1,[$p]);
$second=row('nro.models.boss.OtherBaby','Baby',[14,15],2,[array_merge($p,['zone_id'=>1])]);
$otherMap=row('nro.models.boss.DifferentBaby','Baby',[16],1,[]);
$grouped=bossGroupRows([$old,$live,$second,$otherMap]);check(count($grouped)===2,'same name and map list collapses; different map remains separate');
$group=$grouped[0]['map_ids']==='[16]'?$grouped[1]:$grouped[0];
check(str_starts_with($group['boss_key'],'nro.'),'live canonical configuration is kept');check(count($group['locations'])===2,'all live zones retained');check($group['online_count']===2,'stale duplicate count is excluded');
$case=bossGroupRows([$live,row('BossManager:1',' BABY ',[14,15],100,null)]);check(count($case)===1,'whitespace and case differences do not duplicate names');
