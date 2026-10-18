<?php
require __DIR__.'/../web/admin/october20_top_core.php';
function check($b,$text){if(!$b)throw new Exception($text);echo "PASS $text\n";}
$templates=[];$options=[];foreach(json_decode(file_get_contents(__DIR__.'/../config/teamobi-catalog.json'),true)['items'] as $t)$templates[$t['id']]=$t;
for($i=0;$i<250;$i++)$options[$i]=['id'=>$i];
$r=octTopDefaults();$data=['name'=>'QA','start'=>'2027-10-18T00:00','points_end'=>'2027-10-29T23:00','end'=>'2027-10-30T00:00','scheduled'=>true,'rose_gems'=>20,'card_gems'=>50,'gift_gold'=>10000000,'rewards'=>$r];
$v=octTopValidate($data,$templates,[],$options);check($v['end']-$v['points']===3600000,'point cutoff is separate from event end');check($v['rose']===20&&$v['card']===50&&$v['gold']===10000000,'default prices match approved terms');
foreach(['roses','cards','gifts'] as $board)check(count($r[$board])===6,'six rank groups '.$board);
check($r['roses'][0]['items'][0]['id']===2006&&$r['roses'][0]['items'][5]['quantity']===20,'rose top1 exact armor and lock count');
check($r['gifts'][0]['items'][1]['id']===1173&&$r['gifts'][0]['items'][1]['quantity']===999,'gift top1 999 angel chests');
check($r['cards'][3]['items'][0]['days']===365&&$r['gifts'][3]['items'][0]['days']===0,'distinct costume lifetimes match terms');
check(count($r['cards'][0]['items'][0]['options'])===4,'costume rewards have combat stats');
check($r['rose_bonus'][0]['id']===1655&&$r['card_bonus'][0]['id']===1655,'both rank1 milestones use selectable activation capsule');
function fails($data,$templates,$missing,$options,$label){try{octTopValidate($data,$templates,$missing,$options);throw new LogicException('Unexpected pass');}catch(RuntimeException $e){echo "PASS $label\n";}}
$x=$data;$x['points_end']=$x['start'];fails($x,$templates,[],$options,'inverted cutoff rejected');
$x=$data;$x['rose_gems']=0;fails($x,$templates,[],$options,'free purchases rejected');
fails($data,$templates,[1797=>'missing frames'],$options,'missing costume blocks activation');fails($data,$templates,[1718=>'missing icon'],$options,'missing sale icon blocks activation');
$x=$data;$x['scheduled']=false;check(octTopValidate($x,$templates,[1797=>'missing frames'],$options)['scheduled']===0,'draft can be prepared before art is complete');
$x=$data;$x['rewards']['cards'][0]['items'][0]['options'][]=['id'=>93,'param'=>30];fails($x,$templates,[],$options,'expiry must use days field');
$x=$data;$x['card_gems']=1.5;fails($x,$templates,[],$options,'fractional price rejected');
check($r['cards'][0]['items'][0]['id']===1797&&$r['gifts'][0]['items'][0]['id']===1778,'approved available equivalent costumes');
$missing=json_decode(file_get_contents(__DIR__.'/../config/teamobi-assets.json'),true)['unavailable'];
foreach(['cards'=>1797,'gifts'=>1778] as $board=>$id){
 check(!isset($missing[$id]),'equivalent costume has complete assets '.$board);
 foreach($r[$board] as $rank)check($rank['items'][0]['id']===$id,'equivalent applied to rank '.$board.' '.$rank['from']);
}
check(octTopValidate($data,$templates,$missing,$options)['scheduled']===1,'all actual sale and reward assets allow activation');

foreach(['roses','cards','gifts'] as $board)foreach($r[$board] as $rank)foreach($rank['items'] as $item)if($item['id']===2006){$stats=array_column($item['options'],'param','id');check(($stats[77]??null)===15&&($stats[103]??null)===15&&($stats[94]??null)===5&&($stats[47]??null)===50000&&($stats[9]??null)===0,'armor 5 reward stats match reference');}
