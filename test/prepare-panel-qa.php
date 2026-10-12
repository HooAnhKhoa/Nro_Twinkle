<?php
mysqli_report(MYSQLI_REPORT_ERROR|MYSQLI_REPORT_STRICT);
 $repo=dirname(__DIR__);
require 'C:/xampp/htdocs/config.php';
$qa='nro_panel_qa_20261013';
$conn->query("CREATE DATABASE IF NOT EXISTS `$qa` CHARACTER SET utf8mb4");
$conn->select_db($qa);
if($conn->query('SELECT DATABASE() AS db')->fetch_assoc()['db']!==$qa)throw new RuntimeException('Refusing to run outside QA database');
$conn->query("CREATE TABLE IF NOT EXISTS account(id INT PRIMARY KEY,username VARCHAR(100),is_admin INT,vnd INT NOT NULL DEFAULT 0,last_time_login DATETIME NOT NULL,last_time_logout DATETIME NOT NULL)");
$conn->query("CREATE TABLE IF NOT EXISTS player(id BIGINT PRIMARY KEY,name VARCHAR(100),account_id INT,gender INT DEFAULT 0,data_intrinsic TEXT)");
$conn->query("CREATE TABLE IF NOT EXISTS giftcode(id INT PRIMARY KEY,code VARCHAR(100),count_left INT)");
foreach(['part','skill_template','intrinsic'] as $t) {
    $conn->query("CREATE TABLE IF NOT EXISTS `$t` LIKE srcnrofree.`$t`");
    if(!$conn->query("SELECT 1 FROM `$t` LIMIT 1")->num_rows)$conn->query("INSERT INTO `$t` SELECT * FROM srcnrofree.`$t`");
}
$conn->query("INSERT IGNORE INTO account VALUES(1,'qa_admin',1,1000,'2020-01-01','2020-01-02'),(2,'qa_player',0,1000,'2020-01-01','2020-01-02'),(3,'qa_online',0,1000,'2020-01-02','2020-01-01')");
$conn->query("INSERT IGNORE INTO player VALUES(1,'QA Admin',1,0,'[0,0,0,0]'),(2,'QA Player',2,0,'[0,0,0,0]'),(3,'QA Online',3,0,'[0,0,0,0]')");
$conn->query("INSERT IGNORE INTO giftcode VALUES(1,'qatest',9999),(2,'twinkle',100)");
foreach(['panel_schema.sql','boss_web.sql','server_rates.sql'] as $f)foreach(explode(';',file_get_contents($repo.'/web/'.$f)) as $sql)if(trim($sql)!=='')$conn->query($sql);
$conn->query('UPDATE panel_runtime SET heartbeat=NULL,maintenance=0 WHERE id=1');
$conn->query("DELETE FROM boss_web_config WHERE boss_key NOT LIKE 'qa.boss.%'");
$conn->query('DELETE FROM panel_command');
$fixtures=json_decode(file_get_contents($repo.'/build/boss-fixtures.json'),true);
foreach($fixtures as $index=>$fixture){$key='qa.boss.'.($index+1);$name=$fixture['name'];$hp=$fixture['hp'];$damage=$fixture['damage'];$rest=$fixture['rest'];$head=$fixture['head'];$maps=$fixture['maps'];$s=$conn->prepare('INSERT INTO boss_web_config(boss_key,name,hp,damage,rest_seconds,map_ids,head_id,can_spawn) VALUES(?,?,?,?,?,?,?,1) ON DUPLICATE KEY UPDATE name=VALUES(name),hp=VALUES(hp),damage=VALUES(damage),rest_seconds=VALUES(rest_seconds),head_id=VALUES(head_id)');$s->bind_param('ssiiisi',$key,$name,$hp,$damage,$rest,$maps,$head);$s->execute();}
$root=$repo.'/build/panel-qa';if(!is_dir($root))mkdir($root,0777,true);
$config='<?php $conn=new mysqli("localhost","root","","'.$qa.'");$conn->set_charset("utf8mb4");';file_put_contents($root.'/config.php',$config);
file_put_contents($root.'/qa-login.php','<?php session_start(); $_SESSION["user"]="qa_admin";header("Location: /admin/panel.php");');
echo "QA database ready; production game data unchanged.\n";
