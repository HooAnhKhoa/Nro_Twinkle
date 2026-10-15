<?php
require __DIR__.'/../web/admin/drop_rules_catalog.php';
require __DIR__.'/../web/admin/drop_catalog.php';
$rules=dropRules();$catalog=dropCatalog();$source=file_get_contents(__DIR__.'/../src/nro/models/mob/Mob.java');
preg_match_all('/PanelBridge\.rollDrop\("([a-z_]+)","([a-z_.0-9]+)"/',$source,$matches,PREG_SET_ORDER);
$found=[];foreach($matches as $m){if(!isset($rules[$m[2]])||$rules[$m[2]]['category']!==$m[1])throw new RuntimeException('Invalid mapping '.$m[2]);$found[$m[2]]=true;}
if(!str_contains($source,'MapService.gI().isMapCold(mapid) ? "upgrade_stone.cold" : "upgrade_stone.normal"'))throw new RuntimeException('Stone regions not exclusive');
$found['upgrade_stone.cold']=$found['upgrade_stone.normal']=true;
foreach([1220,1221,1222] as $id)$found['hung.offering.'.$id]=true;
foreach([1544,1545,1546,1547,1548,1549] as $id)$found['hung.cooking.'.$id]=true;
$found['hung.stamp']=true;
preg_match_all("/VALUES\\('([^']+)'/",file_get_contents(__DIR__.'/../web/drop_rules.sql'),$sqlKeys);
foreach(['egg','chicken','flour','beans','monkey_tail','carrot','gogeta_costume','omega_costume','gogeta_board','omega_board'] as $key)$found['trung_thu.'.$key]=true;
if(count($rules)!==57||count($found)!==57||array_diff(array_keys($rules),$sqlKeys[1]))throw new RuntimeException('Incomplete coverage');
foreach($rules as $key=>$rule)if(!isset($found[$key])||!isset($catalog[$rule['category']])||$rule['base']<=0||$rule['base']>100||!$rule['maps'])throw new RuntimeException('Invalid rule '.$key);
echo "PASS: all 57 drop rules have SQL keys, valid groups and map labels; offering behavior tested by OfferingMaterialsTest\n";
