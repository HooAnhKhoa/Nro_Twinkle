<?php
require __DIR__.'/../build/panel-qa/config.php';
if($conn->query('SELECT DATABASE() db')->fetch_assoc()['db']!=='nro_panel_qa_20261013')throw new RuntimeException('Refusing non-QA database');
$conn->query('CREATE TABLE IF NOT EXISTS item_template LIKE srcnrofree.item_template');
$conn->query('INSERT IGNORE INTO item_template SELECT * FROM srcnrofree.item_template');
foreach(['drop_rules.sql'] as $file)foreach(explode(';',file_get_contents(__DIR__.'/../web/'.$file)) as $sql)if(trim($sql)!=='')$conn->query($sql);
$conn->query('UPDATE panel_drop_rule SET percent_override=NULL');
$conn->query('UPDATE panel_drop_runtime SET heartbeat=NOW(),applied_revision=0 WHERE id=1');
file_put_contents(__DIR__.'/../build/panel-qa/qa-db.php','<?php require __DIR__."/config.php"; echo $conn->query("SELECT DATABASE() db")->fetch_assoc()["db"];');
echo "Regional QA schema ready\n";
