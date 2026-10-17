<?php
function ttTopDefaults(){
    $i=function($id,$quantity=1,$days=0){return ['id'=>$id,'quantity'=>$quantity,'days'=>$days,'options'=>[]];};
    $tier=function($from,$to,$items){return compact('from','to','items');};
    return [
      'vip'=>[
        $tier(1,1,[$i(1885,1,365),$i(1283,70),$i(1559,3),$i(1634,3),$i(2046,20),$i(1851,2),$i(2071,3),$i(1868,20000)]),
        $tier(2,2,[$i(1885,1,180),$i(1283,60),$i(1559,2),$i(1634,2),$i(2046,15),$i(1851),$i(2071,2),$i(1868,15000)]),
        $tier(3,3,[$i(1885,1,90),$i(1283,50),$i(1559),$i(1634),$i(2046,10),$i(2071),$i(1868,10000)]),
        $tier(4,10,[$i(1885,1,45),$i(1283,20),$i(1690,50),$i(2046,5),$i(1868,5000)]),
        $tier(11,50,[$i(1809,10),$i(1283,10),$i(1690,20)]),$tier(51,100,[$i(1809,5),$i(1283,5),$i(1690,10)])],
      'normal'=>[
        $tier(1,1,[$i(2059),$i(1283,70),$i(1559,3),$i(2046,10),$i(1634)]),
        $tier(2,2,[$i(2059),$i(1283,60),$i(1559,2),$i(2046,5)]),
        $tier(3,3,[$i(2059),$i(1283,50),$i(1559),$i(2046,2)]),
        $tier(4,10,[$i(2059),$i(1283,20),$i(1690,50)]),
        $tier(11,50,[$i(2059,1,90),$i(1283,10),$i(1690,20)]),$tier(51,100,[$i(2059,1,60),$i(1283,5),$i(1690,10)])],
      'vegetarian'=>[
        $tier(1,1,[$i(2072),$i(987,60),$i(1559,3),$i(1634)]),
        $tier(2,2,[$i(2072),$i(987,40),$i(1559,2)]),$tier(3,3,[$i(2072),$i(987,30),$i(1559)]),
        $tier(4,10,[$i(2072),$i(987,20),$i(1690,50)]),
        $tier(11,50,[$i(2072,1,90),$i(987,10),$i(1690,20)]),$tier(51,100,[$i(2072,1,60),$i(987,5),$i(1690,10)])],
      'milestone'=>[$i(2076)]
    ];
}
function ttTopInt($v,$min,$max){if(!is_int($v)||$v<$min||$v>$max)throw new RuntimeException('Số nhập nằm ngoài giới hạn.');return $v;}
function ttTopDate($v){
    if(!is_string($v))throw new RuntimeException('Chưa chọn ngày giờ.');
    $d=DateTimeImmutable::createFromFormat('!Y-m-d\TH:i',$v,new DateTimeZone('Asia/Ho_Chi_Minh'));
    if(!$d||$d->format('Y-m-d\TH:i')!==$v)throw new RuntimeException('Ngày giờ không hợp lệ.');
    return $d->getTimestamp()*1000;
}
function ttTopValidateItems($items,$templates,$missing,$options){
    if(!is_array($items)||count($items)<1||count($items)>25)throw new RuntimeException('Mỗi gói cần 1–25 vật phẩm.');
    foreach($items as &$i){
        if(!is_array($i))throw new RuntimeException('Vật phẩm không hợp lệ.');
        $id=ttTopInt($i['id']??null,0,32767);
        if(!isset($templates[$id])||isset($missing[(string)$id])||in_array((int)$templates[$id]['TYPE'],[9,10,34],true)||in_array($id,[517,518],true))throw new RuntimeException('Vật phẩm #'.$id.' thiếu dữ liệu hoặc không thể phát vào túi.');
        ttTopInt($i['quantity']??null,1,99999);ttTopInt($i['days']??null,0,3650);
        if(!is_array($i['options']??null)||count($i['options'])>20)throw new RuntimeException('Chỉ số không hợp lệ.');
        foreach($i['options'] as $o){$option=ttTopInt($o['id']??null,0,32767);ttTopInt($o['param']??null,0,1000000000);if(!isset($options[$option])||$option===93)throw new RuntimeException('Option không hợp lệ. Hạn dùng nhập tại ô ngày.');}
        $i=['id'=>$id,'quantity'=>$i['quantity'],'days'=>$i['days'],'options'=>$i['options']];
    }unset($i);return $items;
}
function ttTopValidate($data,$templates,$missing,$options){
    $start=ttTopDate($data['start']??null);$end=ttTopDate($data['end']??null);
    if($end<=$start||$end-$start>366*86400000)throw new RuntimeException('Giờ kết thúc phải sau bắt đầu, tối đa 366 ngày.');
    $name=trim($data['name']??'');if($name===''||mb_strlen($name)>100)throw new RuntimeException('Tên đợt cần 1–100 ký tự.');
    $basis=$data['basis']??'';if(!in_array($basis,['opened','acquired'],true))throw new RuntimeException('Cách tính điểm không hợp lệ.');
    if(!is_bool($data['scheduled']??null))throw new RuntimeException('Trạng thái lịch không hợp lệ.');
    $gold=ttTopInt($data['vip_gold']??null,0,1000000000);$gems=ttTopInt($data['vip_gems']??null,0,1000000);
    if($data['scheduled']&&($gold+$gems===0||$start<=round(microtime(true)*1000)))throw new RuntimeException('Chọn giá hộp VIP và giờ bắt đầu trong tương lai trước khi bật lịch.');
    $r=$data['rewards']??null;if(!is_array($r))throw new RuntimeException('Thiếu bảng thưởng.');
    $result=[];
    foreach(['vip','normal','vegetarian'] as $b){
        if(!is_array($r[$b]??null)||count($r[$b])!==6)throw new RuntimeException('Cần đủ sáu nhóm hạng cho '.$b);
        $tiers=[[1,1],[2,2],[3,3],[4,10],[11,50],[51,100]];$result[$b]=[];
        foreach($tiers as $index=>$range){$row=$r[$b][$index];$result[$b][]=['from'=>$range[0],'to'=>$range[1],'items'=>ttTopValidateItems($row['items']??null,$templates,$missing,$options)];}
    }
    $result['milestone']=ttTopValidateItems($r['milestone']??null,$templates,$missing,$options);
    return ['name'=>$name,'start'=>$start,'end'=>$end,'basis'=>$basis,'scheduled'=>(int)$data['scheduled'],'gold'=>$gold,'gems'=>$gems,'rewards'=>json_encode($result,JSON_UNESCAPED_UNICODE|JSON_THROW_ON_ERROR)];
}
