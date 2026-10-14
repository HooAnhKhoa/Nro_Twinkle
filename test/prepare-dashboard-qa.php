<?php
require __DIR__.'/prepare-panel-qa.php';
foreach(['items_bag','items_box'] as $column)if(!$conn->query("SHOW COLUMNS FROM player LIKE '$column'")->num_rows)$conn->query("ALTER TABLE player ADD $column TEXT NULL");
$fixtures=[1=>[json_encode([json_encode([457,3])]),json_encode([json_encode([457,7])])],2=>[json_encode([[457,15],[16,50000000]]),json_encode([[457,5]])],3=>[json_encode([[457,2]]),'[]']];
foreach($fixtures as $id=>[$bag,$box]){$stmt=$conn->prepare('UPDATE player SET items_bag=?,items_box=? WHERE id=?');$stmt->bind_param('ssi',$bag,$box,$id);$stmt->execute();}
echo "Dashboard inventory fixtures prepared only in QA.\n";
