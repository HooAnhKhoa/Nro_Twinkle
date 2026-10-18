<?php
require_once __DIR__.'/trung_thu_top_core.php';
function octTopDefaults(){return json_decode(file_get_contents(__DIR__.'/../october20-top-rewards.json'),true,512,JSON_THROW_ON_ERROR);}
function octTopValidate($data,$templates,$missing,$options){
 $start=ttTopDate($data['start']??null);$points=ttTopDate($data['points_end']??null);$end=ttTopDate($data['end']??null);
 if($start>=$points||$points>$end||$end-$start>366*86400000)throw new RuntimeException('Cần: bắt đầu < chốt điểm ≤ kết thúc, tối đa 366 ngày.');
 $name=trim($data['name']??'');if($name===''||mb_strlen($name)>100)throw new RuntimeException('Tên đợt cần 1–100 ký tự.');
 if(!is_bool($data['scheduled']??null))throw new RuntimeException('Trạng thái lịch không hợp lệ.');
 if($data['scheduled']&&$start<=round(microtime(true)*1000))throw new RuntimeException('Chọn giờ bắt đầu trong tương lai trước khi bật lịch.');
 $rose=ttTopInt($data['rose_gems']??null,1,1000000);$card=ttTopInt($data['card_gems']??null,1,1000000);$gold=ttTopInt($data['gift_gold']??null,1,1000000000);
 $rewards=$data['rewards']??null;$result=[];if(!is_array($rewards))throw new RuntimeException('Thiếu bảng thưởng.');
 $assetGuard=$data['scheduled']?$missing:[];
 foreach(['roses','cards','gifts'] as $board){if(!is_array($rewards[$board]??null)||count($rewards[$board])!==6)throw new RuntimeException('Cần đủ sáu nhóm hạng cho '.$board);
  foreach([[1,1],[2,2],[3,3],[4,10],[11,50],[51,100]] as $index=>$range)$result[$board][]=['from'=>$range[0],'to'=>$range[1],'items'=>ttTopValidateItems($rewards[$board][$index]['items']??null,$templates,$assetGuard,$options)];
 }
 foreach(['rose_bonus','card_bonus'] as $board)$result[$board]=ttTopValidateItems($rewards[$board]??null,$templates,$assetGuard,$options);
 if($data['scheduled'])foreach([2101,1718,1719] as $id)if(isset($missing[(string)$id]))throw new RuntimeException('Vật phẩm bán #'.$id.' chưa đủ hình/chức năng. Hoàn thiện trước khi bật lịch.');
 return ['name'=>$name,'start'=>$start,'points'=>$points,'end'=>$end,'scheduled'=>(int)$data['scheduled'],'rose'=>$rose,'card'=>$card,'gold'=>$gold,'rewards'=>json_encode($result,JSON_UNESCAPED_UNICODE|JSON_THROW_ON_ERROR)];
}
