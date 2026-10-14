<?php
require __DIR__.'/../web/admin/dashboard_data.php';
function check($value,$message){if(!$value)throw new RuntimeException($message);echo 'PASS: '.$message.PHP_EOL;}
check(panelGoldCount(json_encode([json_encode([457,12,[],0]),json_encode([16,999,[],0])]))===12,'legacy nested JSON counts only gold bars');
check(panelGoldCount(json_encode([[457,4],[457,7],[-1,10],[457,-5]]))===11,'multiple stacks summed and negative quantities ignored');
check(panelGoldCount('broken')===0&&panelGoldCount('[null,"bad"]')===0,'invalid inventory data does not break ranking');
$rows=[['id'=>2,'name'=>'Player','gender'=>1,'items_bag'=>'["[457,5]"]','items_box'=>'["[457,15]"]'],['id'=>1,'name'=>'Admin','gender'=>0,'items_bag'=>'[[457,20]]','items_box'=>'[]'],['id'=>3,'name'=>'Empty','gender'=>2,'items_bag'=>'[[16,999999]]','items_box'=>'[]']];
$rank=panelGoldRank($rows);check(count($rank)===2&&$rank[0]['id']===1&&$rank[1]['gold_bars']===20,'bag and box combined, ties stable by player ID, empty players excluded');
check(count(panelGoldRank($rows,1))===1,'ranking respects display limit');
