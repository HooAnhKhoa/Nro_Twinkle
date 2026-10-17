<?php
function itemCatalogFields():array{return ['TYPE','gender','NAME','description','level','icon_id','part','is_up_to_up','power_require','head','body','leg'];}
function itemCatalogColumns():string{return 'id,'.implode(',',array_map(static function($s){return "`".$s."`";},itemCatalogFields()));}
function itemCatalogRow(array $row):array{
    $row=array_intersect_key($row,array_flip(array_merge(['id'],itemCatalogFields())));
    foreach($row as $k=>$v)if(!in_array($k,['NAME','description'],true))$row[$k]=(int)$v;
    $ordered=[];foreach(array_merge(['id'],itemCatalogFields()) as $key)$ordered[$key]=$row[$key];
    $ordered['revision']=hash('sha256',json_encode($ordered,JSON_UNESCAPED_UNICODE));return $ordered;
}
function itemCatalogSave(mysqli $db,array $data):array{
    $limits=['id'=>[0,32767],'TYPE'=>[0,127],'gender'=>[0,3],'level'=>[0,127],'icon_id'=>[0,32767],'part'=>[-1,32767],'is_up_to_up'=>[0,1],'power_require'=>[0,2147483647],'head'=>[-1,32767],'body'=>[-1,32767],'leg'=>[-1,32767]];
    $values=[];
    foreach($limits as $key=>$range){
        $value=$data[$key]??null;
        if((!is_int($value)&&!is_string($value))||!preg_match('/^-?\d+$/',(string)$value)||(float)$value<$range[0]||(float)$value>$range[1])throw new InvalidArgumentException('Thông số không hợp lệ: '.$key.'.');
        $values[$key]=(int)$value;
    }
    foreach(['NAME'=>100,'description'=>1024] as $key=>$max){
        if(!isset($data[$key])||!is_string($data[$key])||mb_strlen($data[$key],'UTF-8')>$max||($key==='NAME'&&trim($data[$key])===''))throw new InvalidArgumentException('Tên hoặc mô tả vật phẩm không hợp lệ.');
        $values[$key]=$data[$key];
    }
    if(!is_string($data['revision']??null))throw new InvalidArgumentException('Thiếu phiên bản vật phẩm. Hãy mở lại vật phẩm.');
    $db->begin_transaction();
    try{
        $query=$db->prepare('SELECT '.itemCatalogColumns().' FROM item_template WHERE id=? FOR UPDATE');
        $query->bind_param('i',$values['id']);$query->execute();$row=$query->get_result()->fetch_assoc();
        if(!$row)throw new InvalidArgumentException('Không tìm thấy vật phẩm.');
        if(!hash_equals(itemCatalogRow($row)['revision'],$data['revision']))throw new RuntimeException('Vật phẩm vừa được sửa ở nơi khác. Đóng và tải lại trang trước khi sửa tiếp.');
        $fields=itemCatalogFields();$args=[];foreach($fields as $key)$args[]=$values[$key];$args[]=$values['id'];
        $sql='UPDATE item_template SET '.implode(',',array_map(static function($s){return "`".$s."`=?";},$fields)).' WHERE id=?';
        $update=$db->prepare($sql);$types='iissiiiiiiiii';$update->bind_param($types,...$args);$update->execute();
        $query->execute();$saved=itemCatalogRow($query->get_result()->fetch_assoc());$db->commit();return $saved;
    }catch(Throwable $e){$db->rollback();throw $e;}
}
