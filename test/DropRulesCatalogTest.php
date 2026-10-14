<?php
require __DIR__.'/../web/admin/drop_rules_catalog.php';
require __DIR__.'/../web/admin/drop_catalog.php';
$rules=dropRules();$catalog=dropCatalog();$source=file_get_contents(__DIR__.'/../src/nro/models/mob/Mob.java');
preg_match_all('/PanelBridge\.rollDrop\("([a-z_]+)","([a-z_.0-9]+)"/',$source,$matches,PREG_SET_ORDER);
$found=[];foreach($matches as $m){if(!isset($rules[$m[2]])||$rules[$m[2]]['category']!==$m[1])throw new RuntimeException('Invalid mapping '.$m[2]);$found[$m[2]]=true;}
if(!str_contains($source,'MapService.gI().isMapCold(mapid) ? "upgrade_stone.cold" : "upgrade_stone.normal"'))throw new RuntimeException('Stone regions not exclusive');
$found['upgrade_stone.cold']=$found['upgrade_stone.normal']=true;
if(count($rules)!==37||count($found)!==37)throw new RuntimeException('Incomplete coverage');
foreach($rules as $key=>$rule)if(!isset($found[$key])||!isset($catalog[$rule['category']])||$rule['base']<=0||$rule['base']>100||!$rule['maps'])throw new RuntimeException('Invalid rule '.$key);
echo "PASS: all 37 rules map to actual roll branches, valid groups and map labels\n";
