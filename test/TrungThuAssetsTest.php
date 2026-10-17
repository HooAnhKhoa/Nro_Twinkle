<?php
// Read-only verification against the installed game's templates and real image files.
declare(strict_types=1);
require 'C:/xampp/htdocs/config.php';
$root=dirname(__DIR__);
$config=[];
foreach(file($root.'/config/trung-thu-2026.properties',FILE_IGNORE_NEW_LINES) as $line){
    $line=trim($line);
    if($line===''||$line[0]==='#')continue;
    [$key,$value]=explode('=',$line,2);
    if(isset($config[$key]))throw new RuntimeException('Duplicate config key: '.$key);
    $config[$key]=trim($value);
}
$checks=0;
function check(bool $condition,string $message):void{
    global $checks;
    if(!$condition)throw new RuntimeException($message);
    $checks++;
    echo "PASS: $message\n";
}
$expected=[
    'item.two-can-lantern'=>[1888,11,'Lồng đèn 2 lon'],
    'item.moon-board'=>[735,23,'Lồng đèn cá chép'],
    'item.omega-costume'=>[1685,5,'CT Omega'],
    'item.premium-chicken-cake'=>[890,27,'Bánh trung thu Gà quay'],
    'item.normal-box'=>[2078,27,'Hộp bánh Trung Thu 2026'],
    'item.vegetarian-box'=>[2080,27,'Hộp bánh Trung Thu Chay 2026']
];
$rows=[];
foreach($expected as $key=>[$id,$type,$name]){
    check(($config[$key]??'')===(string)$id,"$key uses the agreed existing template $id");
    $statement=$conn->prepare('SELECT id,TYPE,NAME,icon_id,part,head,body,leg FROM item_template WHERE id=?');
    $statement->bind_param('i',$id);$statement->execute();$item=$statement->get_result()->fetch_assoc();
    check($item!==null&&(int)$item['TYPE']===$type&&$item['NAME']===$name,"$id has the expected real name and functional item type");
    foreach(range(1,4) as $zoom){
        $image=$root.'/data/icon/x'.$zoom.'/'.$item['icon_id'].'.png';
        check(is_file($image)&&getimagesize($image)!==false,"$id has a readable icon at x$zoom");
    }
    $rows[$key]=$item;
}
check((int)$rows['item.two-can-lantern']['part']===162,'lantern has the restored canonical two-can back part');
$mountPart=(int)$rows['item.moon-board']['part'];
foreach([0,1] as $direction){
    $name='mount_'.$mountPart.'_'.$direction;
    $statement=$conn->prepare('SELECT n_frame FROM img_by_name WHERE name=?');
    $statement->bind_param('s',$name);$statement->execute();$frames=$statement->get_result()->fetch_assoc();
    check($frames!==null&&(int)$frames['n_frame']>0,"$name is registered with animation frames");
    foreach(range(1,4) as $zoom){
        $image=$root.'/data/img_by_name/x'.$zoom.'/'.$name.'.png';
        check(is_file($image)&&getimagesize($image)!==false,"$name has a readable sprite at x$zoom");
    }
}
$costume=$rows['item.omega-costume'];
check(min((int)$costume['head'],(int)$costume['body'],(int)$costume['leg'])>=0,'dragon costume has all three appearance parts');
check($config['item.premium-chicken-cake']===$config['item.chicken-cake'],'premium recipe deliberately shares the existing chicken cake');
check($config['item.normal-box']!==$config['item.vegetarian-box'],'normal and vegetarian rewards use distinct box templates');
check(!in_array('1701',[$config['item.normal-box'],$config['item.vegetarian-box']],true),'legacy ranking box is excluded');
$npcId=(int)$config['npc.lantern-offering'];
$statement=$conn->prepare('SELECT NAME FROM npc_template WHERE id=?');
$statement->bind_param('i',$npcId);$statement->execute();$npc=$statement->get_result()->fetch_assoc();
check($npc!==null&&$npcId===(int)$config['npc.event']&&$npc['NAME']==='Trung thu','lantern offering reuses the existing event NPC');
check($config['map.rabbit']==='84,126'&&$config['shop.vip-enabled']==='false','rabbit includes public Santa while VIP ranking box stays excluded');
echo "PASS: $checks resource checks; no templates, players or running server changed\n";
