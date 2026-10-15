<?php
if(!defined('BOSS_API_AUTHORIZED')){http_response_code(404);exit;}
function bossDropInteger($value,int $min,int $max): int {
    if(!is_int($value)||$value<$min||$value>$max)throw new InvalidArgumentException('Số nguyên phải trong khoảng '.$min.'–'.$max.'.');
    return $value;
}
function bossDropItems(mysqli $conn,$raw): array {
    if(!is_array($raw)||$raw!==array_values($raw)||count($raw)>30)throw new InvalidArgumentException('Tối đa 30 dòng vật phẩm.');
    $items=[];
    foreach($raw as $row){
        if(!is_array($row))throw new InvalidArgumentException('Vật phẩm không hợp lệ.');
        $id=bossDropInteger($row['id']??null,0,32767);$min=bossDropInteger($row['min']??null,1,99999);$max=bossDropInteger($row['max']??null,$min,99999);
        $chance=$row['chance']??null;
        if((!is_float($chance)&&!is_int($chance))||!is_finite((float)$chance)||$chance<0||$chance>100)throw new InvalidArgumentException('Tỷ lệ phải từ 0 đến 100%.');
        $check=$conn->prepare('SELECT id FROM item_template WHERE id=?');$check->bind_param('i',$id);$check->execute();
        if(!$check->get_result()->num_rows)throw new InvalidArgumentException('Không tồn tại vật phẩm #'.$id.'.');
        $options=$row['options']??[];
        if(!is_array($options)||$options!==array_values($options)||count($options)>15)throw new InvalidArgumentException('Tối đa 15 chỉ số mỗi vật phẩm.');
        $clean=[];$seen=[];
        foreach($options as $option){
            $oid=bossDropInteger($option['id']??null,0,255);$param=bossDropInteger($option['param']??null,0,32767);
            if(isset($seen[$oid]))throw new InvalidArgumentException('Chỉ số bị lặp trong cùng vật phẩm.');$seen[$oid]=true;
            $check=$conn->prepare('SELECT id FROM item_option_template WHERE id=?');$check->bind_param('i',$oid);$check->execute();
            if(!$check->get_result()->num_rows)throw new InvalidArgumentException('Không tồn tại chỉ số #'.$oid.'.');
            $clean[]=['id'=>$oid,'param'=>$param];
        }
        $items[]=['id'=>$id,'min'=>$min,'max'=>$max,'chance'=>round($chance,4),'options'=>$clean];
    }
    return $items;
}
function bossDropApi(mysqli $conn,array $data): bool {
    $action=$data['action']??'';
    if($action==='drop_search'){
        $query=trim(mb_substr((string)($data['query']??''),0,80));
        $id=ctype_digit($query)?(int)$query:-1;
        $page=max(1,min(10000,(int)($data['page']??1)));$limit=24;
        $stmt=$conn->prepare('SELECT COUNT(*) AS total FROM item_template WHERE id=? OR INSTR(LOWER(NAME),LOWER(?))>0');
        $stmt->bind_param('is',$id,$query);$stmt->execute();$total=(int)$stmt->get_result()->fetch_assoc()['total'];
        $pages=max(1,(int)ceil($total/$limit));$page=min($page,$pages);$offset=($page-1)*$limit;
        $stmt=$conn->prepare('SELECT id,NAME AS name,icon_id FROM item_template WHERE id=? OR INSTR(LOWER(NAME),LOWER(?))>0 ORDER BY (id=?) DESC,id LIMIT ? OFFSET ?');
        $stmt->bind_param('isiii',$id,$query,$id,$limit,$offset);$stmt->execute();reply(['items'=>$stmt->get_result()->fetch_all(MYSQLI_ASSOC),'total'=>$total,'page'=>$page,'pages'=>$pages]);
    }
    if($action==='drop_catalog')reply(['options'=>$conn->query('SELECT id,NAME AS name FROM item_option_template WHERE id BETWEEN 0 AND 255 ORDER BY id')->fetch_all(MYSQLI_ASSOC)]);
    if(!in_array($action,['drop_get','drop_save'],true))return false;
    $key=$data['key']??'';
    if(!is_string($key)||strlen($key)>100)reply(['error'=>'Boss không hợp lệ.'],422);
    $q=$conn->prepare('SELECT boss_key FROM boss_web_config WHERE boss_key=?');$q->bind_param('s',$key);$q->execute();
    if(!$q->get_result()->num_rows)reply(['error'=>'Không tìm thấy boss.'],404);
    if($action==='drop_save'){
        try{
            $mode=$data['mode']??'';
            if(!in_array($mode,['original','add','replace','off'],true))throw new InvalidArgumentException('Chế độ rơi không hợp lệ.');
            $items=bossDropItems($conn,$data['items']??null);
            $revision=bossDropInteger($data['revision']??null,0,2147483647);
            $json=json_encode($items,JSON_UNESCAPED_UNICODE|JSON_THROW_ON_ERROR);
            $conn->begin_transaction();
            // A row exists before it is locked, including concurrent first edits.
            $q=$conn->prepare("INSERT IGNORE INTO boss_web_drop_config(boss_key,mode,items,revision) VALUES(?,'original','[]',0)");$q->bind_param('s',$key);$q->execute();
            $q=$conn->prepare('SELECT revision FROM boss_web_drop_config WHERE boss_key=? FOR UPDATE');$q->bind_param('s',$key);$q->execute();$current=(int)$q->get_result()->fetch_assoc()['revision'];
            if($current!==$revision){$conn->rollback();reply(['error'=>'Bảng rơi đã được sửa ở phiên khác. Bấm Tải lại bảng rơi rồi thử lại.'],409);}
            $q=$conn->prepare('UPDATE boss_web_drop_config SET mode=?,items=?,revision=revision+1,updated_at=NOW(),applied_at=NULL WHERE boss_key=?');$q->bind_param('sss',$mode,$json,$key);$q->execute();$conn->commit();
        }catch(InvalidArgumentException $error){$conn->rollback();reply(['error'=>$error->getMessage()],422);}
        catch(Throwable $error){$conn->rollback();reply(['error'=>'Không lưu được bảng rơi. Dữ liệu cũ được giữ nguyên.'],500);}
    }
    $q=$conn->prepare('SELECT mode,items,revision,applied_revision,applied_at FROM boss_web_drop_config WHERE boss_key=?');$q->bind_param('s',$key);$q->execute();
    $config=$q->get_result()->fetch_assoc()?:['mode'=>'original','items'=>'[]','revision'=>0,'applied_revision'=>0,'applied_at'=>null];
    $config['items']=json_decode($config['items'],true)??[];
    $templates=[];$ids=array_unique(array_map(fn($item)=>(int)$item['id'],$config['items']));
    if($ids)foreach($conn->query('SELECT id,NAME AS name,icon_id FROM item_template WHERE id IN ('.implode(',',$ids).')')->fetch_all(MYSQLI_ASSOC) as $template)$templates[(int)$template['id']]=$template;
    foreach($config['items'] as &$item){$item['name']=$templates[(int)$item['id']]['name']??('Vật phẩm #'.$item['id']);$item['icon_id']=$templates[(int)$item['id']]['icon_id']??1;}unset($item);
    $catalog=json_decode(@file_get_contents(__DIR__.'/../boss_native_drops.json'),true)??[];
    $native=$catalog['bosses'][$key]??['ids'=>[],'complete'=>false,'note'=>'Chưa có danh sách đồ gốc của boss này.'];
    $native['items']=[];$ids=array_unique(array_map('intval',$native['ids']));
    if($ids){$found=[];foreach($conn->query('SELECT id,NAME AS name,icon_id FROM item_template WHERE id IN ('.implode(',',$ids).')')->fetch_all(MYSQLI_ASSOC) as $item){$native['items'][]=$item;$found[]=(int)$item['id'];}
        foreach(array_diff($ids,$found) as $id)$native['items'][]=['id'=>$id,'name'=>'Vật phẩm #'.$id.' (thiếu mẫu dữ liệu)','icon_id'=>1];}
    unset($native['ids'],$native['unresolved']);
    reply(['config'=>$config,'native'=>$native,'message'=>$action==='drop_save'?'Đã lưu bảng rơi. Server nhận trong khoảng 3 giây.':null]);
}
