<?php
require __DIR__.'/../build/october20-http/config.php';if($conn->query('SELECT DATABASE()')->fetch_row()[0]!=='nro_october20_top_20261018')throw new Exception('QA guard');
$action=$argv[1]??'';
if($action==='init'){
 $conn->query('CREATE TABLE IF NOT EXISTS account(id INT PRIMARY KEY,username VARCHAR(80) UNIQUE,is_admin TINYINT NOT NULL)');$conn->query("REPLACE INTO account VALUES(90000001,'october-qa-admin',1)");
 foreach(['october20_purchase','october20_prize','october20_score','october20_season'] as $t)$conn->query('DELETE FROM '.$t);
}elseif($action==='start')$conn->query('UPDATE october20_season SET start_ms=UNIX_TIMESTAMP(NOW())*1000-1000');
elseif($action==='end')$conn->query('UPDATE october20_season SET points_end_ms=UNIX_TIMESTAMP(NOW())*1000-2000,end_ms=UNIX_TIMESTAMP(NOW())*1000-1000,closed_at=NOW()');
else throw new Exception('Unexpected QA action');
echo "QA fixture $action completed\n";
