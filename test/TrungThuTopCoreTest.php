<?php
require __DIR__.'/../web/admin/trung_thu_top_core.php';
$catalog=json_decode(file_get_contents(__DIR__.'/../config/teamobi-catalog.json'),true);
$templates=[];foreach($catalog['items'] as $row)$templates[$row['id']]=$row;
$missing=json_decode(file_get_contents(__DIR__.'/../config/teamobi-assets.json'),true)['unavailable'];
$options=array_fill_keys(range(0,255),true);$n=0;
function check($ok,$label){global $n;if(!$ok)throw new RuntimeException($label);$n++;echo "PASS $label\n";}
function bad($data,$label){global $templates,$missing,$options;try{ttTopValidate($data,$templates,$missing,$options);}catch(RuntimeException $e){check(true,$label);return;}throw new RuntimeException('Accepted '.$label);}
$future=(new DateTimeImmutable('+1 day',new DateTimeZone('Asia/Ho_Chi_Minh')))->format('Y-m-d\TH:i');
$end=(new DateTimeImmutable('+3 days',new DateTimeZone('Asia/Ho_Chi_Minh')))->format('Y-m-d\TH:i');
$base=['name'=>'Trung Thu thử','start'=>$future,'end'=>$end,'scheduled'=>true,'basis'=>'opened','vip_gold'=>0,'vip_gems'=>20,'rewards'=>ttTopDefaults()];
$valid=ttTopValidate($base,$templates,$missing,$options);check($valid['end']>$valid['start'],'valid schedule and all canonical default rewards');
check(ttTopDate('2026-10-20T00:00')===1792429200000,'Vietnam timezone timestamp');
foreach(['start'=>'2026-02-30T12:00','end'=>$future,'basis'=>'other','scheduled'=>'true','vip_gems'=>1.1,'vip_gold'=>-1,'name'=>''] as $key=>$value){$d=$base;$d[$key]=$value;bad($d,'reject '.$key);}
$d=$base;$d['vip_gems']=0;bad($d,'free VIP prohibited for scheduled season');
$d=$base;$d['start']='2026-01-01T00:00';bad($d,'expired start prohibited');
$d=$base;$d['rewards']['normal'][0]['items'][0]['id']=2082;bad($d,'missing asset prohibited');
$d=$base;$d['rewards']['normal'][0]['items'][0]['quantity']=0;bad($d,'zero reward prohibited');
$d=$base;$d['rewards']['normal'][0]['items'][0]['days']=-1;bad($d,'negative expiry prohibited');
$d=$base;$d['rewards']['normal'][0]['items'][0]['options']=[['id'=>93,'param'=>10]];bad($d,'duplicate expiry option prohibited');
$d=$base;array_pop($d['rewards']['vip']);bad($d,'missing rank tier prohibited');
$d=$base;$d['basis']='acquired';check(ttTopValidate($d,$templates,$missing,$options)['basis']==='acquired','NPC purchase scoring supported');
echo "PASS $n top configuration checks\n";
