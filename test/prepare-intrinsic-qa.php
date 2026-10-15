<?php
require __DIR__.'/../build/panel-qa/config.php';
if($conn->query('SELECT DATABASE() db')->fetch_assoc()['db']!=='nro_panel_qa_20261013')throw new RuntimeException('Refusing non-QA database');
$conn->query('CREATE TABLE IF NOT EXISTS intrinsic LIKE srcnrofree.intrinsic');
if(!$conn->query('SELECT 1 FROM intrinsic LIMIT 1')->num_rows)$conn->query('INSERT INTO intrinsic SELECT * FROM srcnrofree.intrinsic');
foreach(explode(';',file_get_contents(__DIR__.'/../web/intrinsic_schema.sql')) as $sql)if(trim($sql)!=='')$conn->query($sql);
file_put_contents(__DIR__.'/../build/panel-qa/qa-db.php','<?php require __DIR__."/config.php"; echo $conn->query("SELECT DATABASE() db")->fetch_assoc()["db"];');
echo "Intrinsic QA schema ready\n";
