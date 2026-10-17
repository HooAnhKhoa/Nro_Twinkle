<?php
// Share the game's availability manifest; missing/unreadable metadata fails closed.
function shopCatalogManifest():?array{
    static $loaded=false,$manifest=null;
    if(!$loaded){
        $loaded=true;$paths=require __DIR__.'/../game_paths.php';
        $file=rtrim($paths['game_root']??'','/'.chr(92)).'/config/teamobi-assets.json';
        $decoded=json_decode(@file_get_contents($file),true);
        if(is_array($decoded)&&is_array($decoded['unavailable']??null))$manifest=$decoded['unavailable'];
    }
    return $manifest;
}
function shopCatalogReason(int $id,int $icon):?string{
    $manifest=shopCatalogManifest();
    if($manifest===null)return 'Chưa kiểm tra được tài nguyên vật phẩm.';
    if(isset($manifest[$id]))return 'Vật phẩm chưa có đủ hình ảnh hoặc ngoại hình.';
    if(!is_file(__DIR__.'/../icon/'.$icon.'.png'))return 'Vật phẩm chưa có ảnh.';
    return null;
}
function shopCatalogList(array $rows):array{
    foreach($rows as &$row){$row['unavailable_reason']=shopCatalogReason((int)$row['id'],(int)$row['icon_id']);$row['available']=$row['unavailable_reason']===null;}
    unset($row);return $rows;
}
function shopCatalogAssert(mysqli $db,$id):void{
    if((!is_string($id)&&!is_int($id))||!preg_match('/^\d+$/',(string)$id)||(float)$id>32767)throw new InvalidArgumentException('Vật phẩm không hợp lệ.');
    $id=(int)$id;$query=$db->prepare('SELECT id,icon_id FROM item_template WHERE id=? LIMIT 1');$query->bind_param('i',$id);$query->execute();$row=$query->get_result()->fetch_assoc();
    if(!$row)throw new InvalidArgumentException('Không tìm thấy vật phẩm.');
    $reason=shopCatalogReason((int)$row['id'],(int)$row['icon_id']);
    if($reason!==null)throw new InvalidArgumentException($reason.' Không thể đưa vào cửa hàng.');
}
