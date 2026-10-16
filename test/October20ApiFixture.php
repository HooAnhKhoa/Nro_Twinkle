<?php
require __DIR__.'/../build/panel-qa/config.php';
if($conn->query('SELECT DATABASE() db')->fetch_assoc()['db']!=='nro_panel_qa_20261013')throw new RuntimeException('QA guard');
$file=__DIR__.'/../build/october20-http-before.json';
if(($argv[1]??'')==='backup'){
 foreach(explode(';',file_get_contents(__DIR__.'/../web/october20_schema.sql')) as $sql)if(trim($sql)!=='')$conn->query($sql);
 file_put_contents($file,json_encode(['config'=>$conn->query('SELECT * FROM october20_config')->fetch_all(MYSQLI_ASSOC),'history'=>$conn->query('SELECT * FROM october20_config_audit')->fetch_all(MYSQLI_ASSOC)],JSON_THROW_ON_ERROR));echo "QA config backed up\n";
}elseif(($argv[1]??'')==='restore'){
 $data=json_decode(file_get_contents($file),true,512,JSON_THROW_ON_ERROR);$conn->begin_transaction();
 foreach(['october20_config'=>'config','october20_config_audit'=>'history'] as $table=>$key){$conn->query("DELETE FROM $table");foreach($data[$key] as $row){$columns=implode(',',array_keys($row));$q=$conn->prepare("INSERT INTO $table($columns) VALUES(".implode(',',array_fill(0,count($row),'?')).')');$values=array_values($row);$q->bind_param(str_repeat('s',count($values)),...$values);$q->execute();}}
 $conn->commit();unlink($file);echo "QA config restored\n";
}else throw new RuntimeException('backup or restore required');
