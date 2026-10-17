<?php
// CLI migration of recovered canonical flag definitions. Never replaces an existing different flag.
function restoreCanonicalFlags(mysqli $db,string $root):array{
    $definitions=json_decode(file_get_contents($root.'/config/teamobi-restored-flags.json'),true,512,JSON_THROW_ON_ERROR);
    $validated=[];
    foreach($definitions as $row){
        foreach(['item_id','flag_id','icon_id'] as $field)if(!isset($row[$field])||!is_int($row[$field]))throw new RuntimeException('Invalid definition');
        if($row['flag_id']<0||$row['flag_id']>255||empty($row['frames'])||count($row['frames'])>255)throw new RuntimeException('Flag protocol range');
        foreach(array_merge([$row['icon_id']],$row['frames']) as $frame){
            if(!is_int($frame)||$frame<0||$frame>32767)throw new RuntimeException('Frame protocol range');
            foreach(range(1,4) as $zoom){$image=@getimagesize($root.'/data/icon/x'.$zoom.'/'.$frame.'.png');if(!$image||$image[0]<=1||$image[1]<=1)throw new RuntimeException('Missing frame '.$frame.' at x'.$zoom);}
        }
        $validated[]=$row;
    }
    $db->begin_transaction();
    try{
        $added=0;$remapped=0;
        foreach($validated as $row){
            $q=$db->prepare('SELECT id,TYPE,NAME,part,icon_id FROM item_template WHERE id=? FOR UPDATE');$q->bind_param('i',$row['item_id']);$q->execute();$item=$q->get_result()->fetch_assoc();
            if(!$item||(int)$item['TYPE']!==11||!in_array((int)$item['part'],[$row['flag_id'],$row['original_part']??$row['flag_id']],true)||(int)$item['icon_id']!==$row['icon_id']||$item['NAME']!==$row['name'])throw new RuntimeException('Canonical template differs');
            if((int)$item['part']!==$row['flag_id']){
                $update=$db->prepare('UPDATE item_template SET part=? WHERE id=?');
                $update->bind_param('ii',$row['flag_id'],$row['item_id']);$update->execute();$remapped++;
            }
            $q=$db->prepare('SELECT * FROM flag_bag WHERE id=? FOR UPDATE');$q->bind_param('i',$row['flag_id']);$q->execute();$existing=$q->get_result()->fetch_assoc();$frames=implode(',',$row['frames']);
            if($existing){
                $current=array_map('intval',explode(',',$existing['icon_data']));
                if($current!==$row['frames']||(int)$existing['icon_id']!==$row['icon_id'])throw new RuntimeException('Refusing to overwrite existing flag '.$row['flag_id']);
                continue;
            }
            $q=$db->prepare('INSERT INTO flag_bag(id,icon_data,NAME,gold,gem,icon_id) VALUES(?,?,?,-1,-1,?)');
            $q->bind_param('issi',$row['flag_id'],$frames,$row['name'],$row['icon_id']);$q->execute();$added++;
        }
        $db->commit();return ['added'=>$added,'remapped'=>$remapped,'checked'=>count($validated)];
    }catch(Throwable $e){$db->rollback();throw $e;}
}
if(PHP_SAPI==='cli'&&realpath($_SERVER['SCRIPT_FILENAME'])===__FILE__){
    if(count($argv)!==3)throw new RuntimeException('Usage: php restore-flags.php PATH_TO_DB_CONFIG GAME_ROOT');
    require $argv[1];$root=realpath($argv[2]);if(!$root)throw new RuntimeException('Game root not found');
    $database=$conn->query('SELECT DATABASE()')->fetch_row()[0];
    if($database==='srcnrofree'){
        $port=@fsockopen('127.0.0.1',14445,$code,$message,1);if($port){fclose($port);throw new RuntimeException('Save and stop main game first');}
        $state=$conn->query('SELECT maintenance FROM panel_runtime WHERE id=1')->fetch_row();if(!$state||(int)$state[0]!==1)throw new RuntimeException('Main game maintenance guard');
    }elseif($database!=='nro_teamobi_qa_20261017_v3')throw new RuntimeException('Unexpected database');
    echo json_encode(restoreCanonicalFlags($conn,$root),JSON_UNESCAPED_UNICODE).PHP_EOL;
}
