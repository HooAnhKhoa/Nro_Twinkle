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
$training=bossGroupRows([$live,row('training:1','Chiến binh-0',[131],1,[]),row('training:2','Tân binh-5',[132],1,[]),row('training:3','Tập sự-4',[133],1,[])]);check(count($training)===1&&$training[0]['name']==='Baby','Yardrat training mobs excluded while real bosses remain');
$questNames=['Baby','Android 13','Số 1','Pic','Kuku','Android 15','Poc','Số 4','Android 14','Xên con 7','Xên con 1'];
$quest=bossGroupRows(array_map(fn($name)=>row($name,$name,[1],1,[]),$questNames));
check(array_column($quest,'name')===['Kuku','Số 4','Số 1','Android 15','Android 14','Android 13','Poc','Pic','Xên con 1','Xên con 7','Baby'],'boss order follows main task and subtask rather than alphabetical order');
check($quest[0]['task_group']==='Nhiệm vụ 19'&&end($quest)['task_group']==='Nhiệm vụ 29','quest headers identify mission bosses and other bosses');
