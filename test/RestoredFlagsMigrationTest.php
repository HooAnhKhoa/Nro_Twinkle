<?php
if(PHP_SAPI!=='cli')exit(1);
require $argv[1];
if($conn->query('SELECT DATABASE()')->fetch_row()[0]!=='nro_teamobi_qa_20261017_v3')throw new Exception('QA guard');
require __DIR__.'/../scripts/teamobi/restore-flags.php';
$root=dirname(__DIR__);
$before=$conn->query('SELECT * FROM flag_bag WHERE id=141')->fetch_assoc();
$hash=function()use($conn){return hash('sha256',json_encode([$conn->query('SELECT * FROM flag_bag ORDER BY id')->fetch_all(MYSQLI_ASSOC),$conn->query('SELECT * FROM item_template ORDER BY id')->fetch_all(MYSQLI_ASSOC)]));};
$baseline=$hash();
try{
 $conn->query("UPDATE flag_bag SET icon_data='1,2' WHERE id=141");
 try{restoreCanonicalFlags($conn,$root);throw new Exception('Conflicting frame definition accepted');}
 catch(RuntimeException $expected){if(!str_contains($expected->getMessage(),'Refusing to overwrite'))throw $expected;}
 echo "PASS: conflicting existing flag rejected without overwrite\n";
}finally{$q=$conn->prepare('UPDATE flag_bag SET icon_data=? WHERE id=141');$q->bind_param('s',$before['icon_data']);$q->execute();}
if($hash()!==$baseline)throw new Exception('QA restoration changed unrelated data');
echo "PASS: all flags and templates restored exactly after conflict fixture\n";
$result=restoreCanonicalFlags($conn,$root);
if($result['added']!==0||$result['remapped']!==0)throw new Exception('Restoration not idempotent');
echo "PASS: repeated restoration is idempotent\n";
