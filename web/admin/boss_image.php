<?php
require_once __DIR__.'/panel_common.php';
session_write_close();
$head=filter_var($_GET['head']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>0,'max_range'=>32767]]);
if($head===false || $head===null){http_response_code(422);exit;}
$stmt=$conn->prepare('SELECT DATA FROM part WHERE id=? AND TYPE=0 LIMIT 1');
$stmt->bind_param('i',$head);$stmt->execute();$part=$stmt->get_result()->fetch_assoc();
$frames=$part?json_decode($part['DATA'],true):null;
if(is_string($frames))$frames=json_decode($frames,true);
if(!is_array($frames)&&$part)$frames=json_decode(str_replace('"','',$part['DATA']),true);
// When deployed to htdocs/admin, the game root is configured separately below.
$gameRoot=dirname(__DIR__,2);
$pathConfig=__DIR__.'/../game_paths.php';
if(is_file($pathConfig)){$paths=require $pathConfig;if(is_array($paths)&&isset($paths['game_root']))$gameRoot=$paths['game_root'];}
$file=null;
foreach(is_array($frames)?$frames:[] as $frame){
    if(is_string($frame))$frame=json_decode($frame,true);
    $icon=is_array($frame)?filter_var($frame[0]??null,FILTER_VALIDATE_INT):false;
    if($icon===false||$icon===null||$icon<=0)continue;
    foreach(['data/icon/x4/','data/icon/x2/','data/icon/x1/'] as $folder){
        $candidate=$gameRoot.'/'.$folder.$icon.'.png';
        if(is_file($candidate)&&is_readable($candidate)){$file=$candidate;break 2;}
    }
    $candidate=dirname(__DIR__).'/icon/'.$icon.'.png';
    if(is_file($candidate)&&is_readable($candidate)){$file=$candidate;break;}
}
if(!$file){http_response_code(404);exit;}
$etag='"'.sha1($head.':'.$file.':'.filemtime($file).':'.filesize($file)).'"';
header('Content-Type: image/png');header('X-Content-Type-Options: nosniff');
header('Cache-Control: private, max-age=300');header('ETag: '.$etag);
if(($_SERVER['HTTP_IF_NONE_MATCH']??'')===$etag){http_response_code(304);exit;}
header('Content-Length: '.filesize($file));readfile($file);
