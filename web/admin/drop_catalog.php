<?php
// Base rolls from Mob.getItemMobReward. Conditional/combined checks are described explicitly.
function dropCatalog(): array { return [
 'october20'=>['name'=>'Vật phẩm 20/10','icon'=>'🌹','base'=>[5,5,3,1],'detail'=>'Hoa hồng giấy 5%, giấy màu 5%, Sôcôla Trái Tim 3%, nơ trang trí 1%. Quái thường ở mọi map; không tính mộc nhân, bù nhìn hoặc big boss. Cần bật 20/10 và Vật phẩm EV.'],
 'trung_thu'=>['name'=>'Vật phẩm Trung Thu','icon'=>'🥮','base'=>[2,2,5,5,100,100,20,20,1,1],'detail'=>'Cần bật Trung Thu và Vật phẩm EV. Trứng/gà ở map thường; bột mì/đậu xanh ở phó bản; đuôi khỉ/cà rốt từ boss triệu hồi; cải trang/ván bay từ Gogeta–Omega. Hạt sen nhận qua nhiệm vụ, không rơi ngẫu nhiên.'],
 'dragon_ball'=>['name'=>'Ngọc rồng','icon'=>'🐉','base'=>[1,100/7,1,10,10],'detail'=>'1% ở map thường; 10/70 và nhánh tài khoản kích hoạt ở một số map; ngọc nhiệm vụ không thay đổi.'],
 'upgrade_stone'=>['name'=>'Đá nâng cấp','icon'=>'💎','base'=>[2,20],'detail'=>'Mọi map: 2% rơi ngẫu nhiên 1 trong 5 loại đá (lục bảo, Saphia, Ruby, Titan, thạch anh tím). Map lạnh giữ 20%, chỉ xét một lần. Đã tắt rơi mảnh đá vụn.'],
 'shop'=>['name'=>'Đồ shop có sao','icon'=>'🛍️','base'=>[.0125,100/19999,.1],'detail'=>'Trang bị thường có sao từ randTempItemDoSao / randDoSao. Giữ kiểm tra nối tiếp, giới hạn sức mạnh và điều kiện map SKH.'],
 'god'=>['name'=>'Đồ thần linh','icon'=>'🛡️','base'=>[.002],'detail'=>'Map lạnh: 1/50.000.'],
 'crystal'=>['name'=>'Sao pha lê','icon'=>'⭐','base'=>[10],'detail'=>'10% rơi ngẫu nhiên một trong 7 loại sao pha lê ở mọi map; cỏ bốn lá giữ bonus hiện có.'],
 'food'=>['name'=>'Thức ăn','icon'=>'🍗','base'=>[300/333],'detail'=>'3/333 khi mặc đủ set thần linh.'],
 'porata'=>['name'=>'Mảnh hồn và vỡ bông tai','icon'=>'🧩','base'=>[10,5,.2],'detail'=>'Map bông tai: các nhánh 10%, 5%, 1/500 xét nối tiếp; giữ giới hạn vật phẩm.'],
 'angel'=>['name'=>'Mảnh thiên sứ','icon'=>'🪽','base'=>[200/555,10],'detail'=>'Ngục tù, set hủy diệt: nhánh kích hoạt 2/555 hoặc nhánh 10%.'],
 'skh'=>['name'=>'Set kích hoạt','icon'=>'⚔️','base'=>[200/9999],'detail'=>'2/9.999 tại map SKH và map riêng tư; giữ bonus trang bị và cỏ bốn lá.'],
 'gold'=>['name'=>'Vàng','icon'=>'🪙','base'=>[5,1,30,15,1],'detail'=>'Ba hành tinh 5%; Nappa 1%; map lạnh 30%; tương lai 15%; phó bản 1%. Không đổi số vàng.'],
 'capsule'=>['name'=>'Capsule kỳ bí / vỡ','icon'=>'💊','base'=>[20,100/9999,100/19999],'detail'=>'Capsule kỳ bí cần máy dò; capsule vỡ tại map SKH / riêng tư.'],
 'hung_vuong'=>['name'=>'Nguyên liệu Giỗ Tổ','icon'=>'🏮','base'=>[100,5,100],'detail'=>'Chỉ rơi khi bật Giỗ Tổ và Vật phẩm EV. Sính lễ từ đúng quái, nguyên liệu từ quái thường, Tem từ Sơn Tinh/Thủy Tinh.'],
 'summer'=>['name'=>'Khúc mía','icon'=>'🥤','base'=>[5],'detail'=>'Chỉ rơi khi bật Xe nước mía và Vật phẩm EV. Nước đá giữ nguồn mua tại Chi Chi.'],
 'bill_food'=>['name'=>'Thức ăn cho Bill','icon'=>'🍱','base'=>[5,1,100/130],'detail'=>'Tayaki, Kẹo táo, Kem que đôi, Mochi, Ramen; độc lập với công tắc mùa lễ.'],
 'quest_items'=>['name'=>'Vật phẩm nhiệm vụ','icon'=>'📋','base'=>[20,10],'detail'=>'Cuốn chả giò ở Doanh trại và Hồng Đào ở Ngũ Hành Sơn; độc lập với công tắc mùa lễ.'],
 'gem'=>['name'=>'Ngọc','icon'=>'💚','base'=>[.0001],'detail'=>'1/1.000.000; ngọc miễn phí bảo đảm giữ nguyên.'],
 'skill'=>['name'=>'Bí kíp tuyệt kỹ','icon'=>'📜','base'=>[10,20],'detail'=>'Ngục tù: nhánh 10% hoặc nhánh 20% có điều kiện set hủy diệt / kích hoạt.'],
 ]; }
