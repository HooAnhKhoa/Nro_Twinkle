<?php
// Base rolls from Mob.getItemMobReward. Conditional/combined checks are described explicitly.
function dropCatalog(): array { return [
 'dragon_ball'=>['name'=>'Ngọc rồng','icon'=>'🐉','base'=>[1,100/7,1,10,10],'detail'=>'1% ở map thường; 10/70 và nhánh tài khoản kích hoạt ở một số map; ngọc nhiệm vụ không thay đổi.'],
 'upgrade_stone'=>['name'=>'Đá nâng cấp','icon'=>'💎','base'=>[],'detail'=>'Code hiện chưa có nhánh quái rơi đá nâng cấp.','supported'=>false],
 'shop'=>['name'=>'Đồ shop có sao','icon'=>'🛍️','base'=>[.0125,100/19999,.1],'detail'=>'Trang bị thường có sao từ randTempItemDoSao / randDoSao. Giữ kiểm tra nối tiếp, giới hạn sức mạnh và điều kiện map SKH.'],
 'god'=>['name'=>'Đồ thần linh','icon'=>'🛡️','base'=>[.002],'detail'=>'Map lạnh: 1/50.000.'],
 'crystal'=>['name'=>'Sao pha lê','icon'=>'⭐','base'=>[10,20,10],'detail'=>'Map thường 10%; map lạnh 20%; doanh trại và ba hành tinh 10%.'],
 'food'=>['name'=>'Thức ăn','icon'=>'🍗','base'=>[300/333],'detail'=>'3/333 khi mặc đủ set thần linh.'],
 'porata'=>['name'=>'Mảnh hồn và vỡ bông tai','icon'=>'🧩','base'=>[10,5,.2],'detail'=>'Map bông tai: các nhánh 10%, 5%, 1/500 xét nối tiếp; giữ giới hạn vật phẩm.'],
 'angel'=>['name'=>'Mảnh thiên sứ','icon'=>'🪽','base'=>[200/555,10],'detail'=>'Ngục tù, set hủy diệt: nhánh kích hoạt 2/555 hoặc nhánh 10%.'],
 'skh'=>['name'=>'Set kích hoạt','icon'=>'⚔️','base'=>[200/9999],'detail'=>'2/9.999 tại map SKH và map riêng tư; giữ bonus trang bị và cỏ bốn lá.'],
 'gold'=>['name'=>'Vàng','icon'=>'🪙','base'=>[5,1,30,15,1],'detail'=>'Ba hành tinh 5%; Nappa 1%; map lạnh 30%; tương lai 15%; phó bản 1%. Không đổi số vàng.'],
 'capsule'=>['name'=>'Capsule kỳ bí / vỡ','icon'=>'💊','base'=>[20,100/9999,100/19999],'detail'=>'Capsule kỳ bí cần máy dò; capsule vỡ tại map SKH / riêng tư.'],
 'event'=>['name'=>'Vật phẩm sự kiện','icon'=>'🎉','base'=>[5,1,100/130,20,10],'detail'=>'Các món đang có trong code, chả giò và hồng đào; giữ điều kiện map.'],
 'gem'=>['name'=>'Ngọc','icon'=>'💚','base'=>[.0001],'detail'=>'1/1.000.000; ngọc miễn phí bảo đảm giữ nguyên.'],
 'skill'=>['name'=>'Bí kíp tuyệt kỹ','icon'=>'📜','base'=>[10,20],'detail'=>'Ngục tù: nhánh 10% hoặc nhánh 20% có điều kiện set hủy diệt / kích hoạt.'],
 ]; }
