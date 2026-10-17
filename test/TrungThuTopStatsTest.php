<?php
require __DIR__.'/../web/admin/trung_thu_top_core.php';
$r=ttTopDefaults();$checks=0;
foreach(['normal','vegetarian'] as $b)foreach([30,28,26,24,20,18] as $index=>$value){if($r[$b][$index]['items'][0]['options']!==ttTopStatOptions($value))throw new Exception('Tier stats');$checks++;}
foreach($r['vip'] as $tier)foreach($tier['items'] as $item)if($item['id']===1885){if($item['options']!==ttTopStatOptions(15))throw new Exception('Title stats');$checks++;}
if($r['milestone'][0]['options']!==ttTopStatOptions(15)||$r['normal'][0]['items'][0]['days']!==0||$r['normal'][5]['items'][0]['days']!==60)throw new Exception('Lifetime changed');
foreach($r['vip'] as $tier)foreach($tier['items'] as $item)if($item['id']!==1885&&$item['options']!==[])throw new Exception('Material got stats');
echo 'PASS: '.($checks+2).' reward default checks'.PHP_EOL;
