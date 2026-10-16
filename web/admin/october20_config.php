<?php
// Shared server-side validation and form metadata for the October 20 event.
function october20Fields(): array {
    $fields=[];
    foreach(['normal'=>'Quà thường','special'=>'Quà đặc biệt'] as $kind=>$label){
        foreach(['roses'=>'Hoa hồng giấy','paper'=>'Giấy màu','chocolate'=>'Sôcôla Trái Tim','bow'=>'Nơ trang trí'] as $key=>$name)$fields["$kind.$key"]=['label'=>"$label · $name",'min'=>1,'max'=>9999];
        $fields["$kind.gold"]=['label'=>"$label · giá vàng",'min'=>0,'max'=>100000000];
        $fields["$kind.accessory"]=['label'=>"$label · % ra phụ kiện",'min'=>0,'max'=>100];
        $fields["$kind.stones"]=['label'=>"$label · % ra đá nâng cấp",'min'=>0,'max'=>100];
        $fields["$kind.days"]=['label'=>"$label · hạn phụ kiện (ngày)",'min'=>1,'max'=>365];
        $fields["$kind.quantity"]=['label'=>"$label · số đá / vật phẩm hỗ trợ",'min'=>1,'max'=>9999];
        $fields["$kind.points"]=['label'=>"$label · điểm nhận khi mở",'min'=>1,'max'=>1000000];
    }
    $fields['permanent']=['label'=>'% vĩnh viễn khi đã quay ra phụ kiện','min'=>0,'max'=>100];
    $fields['redeem.points']=['label'=>'Điểm cần đổi phụ kiện bảo đảm','min'=>1,'max'=>1000000];
    $fields['redeem.days']=['label'=>'Hạn phụ kiện đổi điểm (ngày)','min'=>1,'max'=>365];
    foreach(['sd'=>'Sức đánh (%)','sdcm'=>'Sát thương chí mạng (%)','cm'=>'Chí mạng (%)','cute'=>'Cute tăng SD (%)'] as $key=>$name){
        $fields["$key.min"]=['label'=>"$name · thấp nhất",'min'=>0,'max'=>100];$fields["$key.max"]=['label'=>"$name · cao nhất",'min'=>0,'max'=>100];
    }
    return $fields;
}
function october20Validate(array $settings): array {
    $fields=october20Fields();if(count($settings)!==count($fields)||array_diff_key($settings,$fields)||array_diff_key($fields,$settings))throw new InvalidArgumentException('Cần gửi đầy đủ cấu hình 20/10.');
    foreach($fields as $key=>$field)if(!is_int($settings[$key])||$settings[$key]<$field['min']||$settings[$key]>$field['max'])throw new InvalidArgumentException('Giá trị không hợp lệ: '.$field['label']);
    foreach(['normal','special'] as $kind)if($settings["$kind.accessory"]+$settings["$kind.stones"]>100)throw new InvalidArgumentException('Tổng tỷ lệ phụ kiện và đá không được vượt 100%.');
    foreach(['sd','sdcm','cm','cute'] as $stat)if($settings["$stat.min"]>$settings["$stat.max"])throw new InvalidArgumentException('Chỉ số thấp nhất không được lớn hơn cao nhất.');
    return $settings;
}
