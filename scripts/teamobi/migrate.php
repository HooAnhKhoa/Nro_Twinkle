<?php
// CLI-only canonical catalog migration; all row changes commit together.
function catalogIsList(array $a):bool{return !$a||array_keys($a)===range(0,count($a)-1);}
function catalogMapNumber($v,array $map){$n=(int)$v;if(!array_key_exists($n,$map))return $v;return is_string($v)?(string)$map[$n]:$map[$n];}
function catalogRemap($v,array $items,array $parts){
 if(is_string($v)){ $s=ltrim($v);if($s===''||($s[0]!=='['&&$s[0]!=='{'))return $v;$decoded=json_decode($v,true);if(json_last_error()!==JSON_ERROR_NONE)return $v;$next=catalogRemap($decoded,$items,$parts);return $next===$decoded?$v:json_encode($next,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES|JSON_THROW_ON_ERROR); }
 if(!is_array($v))return $v;
 // Persisted item tuple: [templateID, quantity, encoded options, createTime]. Skill/point tuples do not match.
 if(catalogIsList($v)&&count($v)===4&&is_numeric($v[0])&&is_numeric($v[1])&&(is_array($v[2])||is_string($v[2])&&str_starts_with(ltrim($v[2]),'['))){$v[0]=catalogMapNumber($v[0],$items);return $v;}
 if(!catalogIsList($v)){
  if(array_key_exists('id',$v)&&(array_key_exists('quantity',$v)||array_key_exists('chance',$v)||array_key_exists('rate',$v)||array_key_exists('min',$v)&&array_key_exists('max',$v)))$v['id']=catalogMapNumber($v['id'],$items);
  foreach(['item_id','itemId','temp_id','template_id','idItem','cake_id'] as $key)if(isset($v[$key])&&is_numeric($v[$key]))$v[$key]=catalogMapNumber($v[$key],$items);
  foreach(['head','body','leg'] as $key)if(isset($v[$key])&&is_numeric($v[$key]))$v[$key]=catalogMapNumber($v[$key],$parts);
 }
 foreach($v as $k=>$value)if(is_array($value)||is_string($value))$v[$k]=catalogRemap($value,$items,$parts);
 return $v;
}
function catalogConvertCards(string $json,array $map):string {
 $cards=json_decode($json,true,512,JSON_THROW_ON_ERROR);if(!is_array($cards))throw new RuntimeException('Invalid card array');$by=[];
 foreach($cards as $card){if(!isset($card['id']))throw new RuntimeException('Invalid card');$card['id']=catalogMapNumber($card['id'],$map);$key=$card['id'];if(isset($by[$key])){$total=(int)($by[$key]['amount']??0)+(int)($card['amount']??0);foreach(['level','used','max'] as $f)if(isset($card[$f]))$by[$key][$f]=(string)max((int)($by[$key][$f]??0),(int)$card[$f]);$by[$key]['amount']=(string)min(max(1,(int)($by[$key]['max']??$total)),$total);}else $by[$key]=$card;}
 return json_encode(array_values($by),JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES|JSON_THROW_ON_ERROR);
}
function catalogMigrate(mysqli $db,array $plan):array {
 $version=$plan['version'];$items=$plan['itemMap'];$parts=$plan['partMap'];$database=$db->query('SELECT DATABASE()')->fetch_row()[0];
 if(!preg_match('/^[a-zA-Z0-9_]+$/',$database))throw new RuntimeException('Invalid database');
 $db->query('CREATE TABLE IF NOT EXISTS catalog_migration(version VARCHAR(80) PRIMARY KEY,applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,report LONGTEXT NOT NULL) ENGINE=InnoDB');
 $q=$db->prepare('SELECT version FROM catalog_migration WHERE version=?');$q->bind_param('s',$version);$q->execute();if($q->get_result()->num_rows)throw new RuntimeException('Catalog migration already applied; refusing to remap twice');
 foreach(['item_template'=>'baselineItemsSha256','part'=>'baselinePartsSha256'] as $table=>$key){$rows=$db->query("SELECT * FROM `$table` ORDER BY id")->fetch_all(MYSQLI_ASSOC);$hash=hash('sha256',json_encode($rows,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES|JSON_THROW_ON_ERROR));if($hash!==$plan[$key])throw new RuntimeException('Template table changed since preflight: '.$table);}
 $db->query('ALTER TABLE item_template MODIFY description VARCHAR(1024) NULL');
 $columns=$db->query("SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$database'")->fetch_all(MYSQLI_ASSOC);$tables=[];foreach($columns as $c)$tables[$c['TABLE_NAME']][]=$c;
 $counts=[];$db->begin_transaction();
 try{
 foreach($tables as $table=>$cols){
  if(in_array($table,['item_template','part','head_avatar','array_head_2_frames','catalog_migration','radar'],true))continue;
  $numeric=['item_shop'=>['temp_id'],'shop_ky_gui'=>['item_id'],'data_badges'=>['idItem'],'trung_thu_2026_cake'=>['cake_id'],'player'=>['head'],'npc_template'=>['head','body','leg']][$table]??[];
  $text=[];foreach($cols as $c)if(in_array($c['DATA_TYPE'],['text','longtext','mediumtext','varchar'],true)&&preg_match('/items_|^pet$|^friends$|^enemies$|^members$|^reward|^detail$|^items$|^ground_items$|^settings$|_settings$|^config$|_config$|_json$|^body$/',$c['COLUMN_NAME']))$text[]=$c['COLUMN_NAME'];
  if($table==='player')$text[]='data_card';
  if(!$numeric&&!$text)continue;
  $primary=[];foreach($db->query("SHOW COLUMNS FROM `$table`")->fetch_all(MYSQLI_ASSOC) as $c)if($c['Key']==='PRI')$primary[]=$c['Field'];
  if(!$primary)continue;
  foreach($db->query("SELECT * FROM `$table`")->fetch_all(MYSQLI_ASSOC) as $row){$changes=[];
   foreach($numeric as $col){if(!array_key_exists($col,$row))continue;$m=in_array($col,['head','body','leg'],true)?$parts:$items;$new=catalogMapNumber($row[$col],$m);if($new!==$row[$col])$changes[$col]=$new;}
   foreach(array_unique($text) as $col){if(!isset($row[$col]))continue;$new=$col==='data_card'?catalogConvertCards($row[$col],$items):catalogRemap($row[$col],$items,$parts);if($new!==$row[$col])$changes[$col]=$new;}
   if(!$changes)continue;$set=implode(',',array_map(fn($k)=>"`$k`=?",array_keys($changes)));$where=implode(' AND ',array_map(fn($k)=>"`$k`=?",$primary));$args=array_merge(array_values($changes),array_map(fn($k)=>$row[$k],$primary));$q=$db->prepare("UPDATE `$table` SET $set WHERE $where");$q->bind_param(str_repeat('s',count($args)),...$args);$q->execute();$counts[$table]=($counts[$table]??0)+1;
  }
 }
 // Radar IDs correspond to card item IDs; merge aliases after standard replacement.
 if(isset($tables['radar'])){$all=$db->query('SELECT * FROM radar')->fetch_all(MYSQLI_ASSOC);$mapped=[];foreach($all as $row){$row['id']=catalogMapNumber($row['id'],$items);if(isset($row['require']))$row['require']=catalogMapNumber($row['require'],$items);$row['body']=catalogRemap($row['body'],$items,$parts);$mapped[$row['id']]??=$row;}$db->query('DELETE FROM radar');catalogInsertRows($db,'radar',array_values($mapped));}
 // Port existing matching frame definitions to their canonical accessory part IDs.
 foreach($plan['portedFlags']??[] as $flag){$q=$db->prepare('DELETE FROM flag_bag WHERE id=?');$q->bind_param('i',$flag['id']);$q->execute();catalogInsertRows($db,'flag_bag',[$flag]);}
 // Preserve existing avatar graphics at their mapped heads; add avatars for new outfits.
 $avatars=[];foreach($db->query('SELECT * FROM head_avatar')->fetch_all(MYSQLI_ASSOC) as $r){$r['head_id']=catalogMapNumber($r['head_id'],$parts);$avatars[(int)$r['head_id']]=$r;}
 foreach($plan['items'] as $r)if($r['head']>=0&&!isset($avatars[$r['head']]))$avatars[$r['head']]=['head_id'=>$r['head'],'avatar_id'=>$r['icon_id']];
 $db->query('DELETE FROM head_avatar');catalogInsertRows($db,'head_avatar',array_values($avatars));
 // Frame-pair table stores head part IDs, never item IDs.
 foreach($db->query('SELECT * FROM array_head_2_frames')->fetch_all(MYSQLI_ASSOC) as $r){$data=json_decode(preg_replace('/,\s*]/',']',$r['data']),true,512,JSON_THROW_ON_ERROR);foreach($data as &$n)$n=catalogMapNumber($n,$parts);unset($n);$json=json_encode($data);$id=$r['id'];$q=$db->prepare('UPDATE array_head_2_frames SET data=? WHERE id=?');$q->bind_param('si',$json,$id);$q->execute();}
 $db->query('DELETE FROM item_template');catalogInsertRows($db,'item_template',$plan['items']);$db->query('DELETE FROM part');catalogInsertRows($db,'part',$plan['parts']);
 $json=json_encode($counts,JSON_THROW_ON_ERROR);$q=$db->prepare('INSERT INTO catalog_migration(version,report) VALUES(?,?)');$q->bind_param('ss',$version,$json);$q->execute();$db->commit();return $counts;
 }catch(Throwable $e){$db->rollback();throw $e;}
}
function catalogInsertRows(mysqli $db,string $table,array $rows):void {if(!$rows)return;$cols=array_keys($rows[0]);$q=$db->prepare("INSERT INTO `$table` (`".implode('`,`',$cols).'`) VALUES('.implode(',',array_fill(0,count($cols),'?')).')');foreach($rows as $row){$args=array_values($row);$q->bind_param(str_repeat('s',count($args)),...$args);$q->execute();}}
