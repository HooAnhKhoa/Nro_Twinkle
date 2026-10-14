<?php
// One entry per actual random roll in Mob.getItemMobReward; zones within a map share rules.
function dropRules(): array {
    $rules=[];
    $add=function($key,$category,$label,$base,$maps,$note='')use(&$rules){$rules[$key]=compact('category','label','base','maps','note');};
    $early='Map 1–3, 8, 9, 11, 15–17';
    $regions='Ba hành tinh, Nappa 63–83, tương lai 92–94/96–100/102/103, lạnh 105–110';
    $add('dragon_ball.all','dragon_ball','Mọi map',1,'Mọi map có quái hợp lệ','Lượt rơi ngọc 4–7 sao độc lập.');
    $add('dragon_ball.regions','dragon_ball','Ba hành tinh / Nappa / tương lai / lạnh',100/7,$regions,'Ngọc 6–7 sao. Xét thêm lượt tài khoản kích hoạt nếu lượt này thất bại.');
    $add('dragon_ball.active','dragon_ball','Các vùng trên · tài khoản kích hoạt',1,$regions,'Chỉ xét khi lượt vùng thất bại và tài khoản đã kích hoạt.');
    $add('upgrade_stone.normal','upgrade_stone','Map thường',2,'Mọi map trừ map lạnh 105–110','Ngẫu nhiên một trong 5 loại đá nâng cấp.');
    $add('upgrade_stone.cold','upgrade_stone','Map lạnh',20,'Map 105–110','Một lượt xét; không cộng thêm lượt map thường.');
    $add('shop.gate','shop','Map SKH · lượt xét ngoài',100/8000,$early,'Phải qua cả lượt xét ngoài và lượt xét trong.');
    $add('shop.inner','shop','Map SKH · lượt xét trong',100/19999,$early,'Sau lượt xét ngoài; vẫn giữ bước tạo thuộc tính sao.');
    $add('shop.cloth','shop','Map SKH · nhánh đồ vải thô',.1,$early,'Còn bước giới hạn sức mạnh và tạo thuộc tính sao.');
    $add('god.cold','god','Map lạnh',.002,'Map 105–110');
    $add('crystal.all','crystal','Mọi map',10,'Mọi map có quái hợp lệ','Ngẫu nhiên một trong 7 loại sao pha lê.');
    $add('food.god','food','Mọi map · đủ set thần linh',300/333,'Mọi map có quái hợp lệ','Chỉ rơi khi mặc đủ set thần linh.');
    $add('porata.soul','porata','Map bông tai · mảnh hồn',10,'Map 156–159','Nhánh đầu, vật phẩm 933.');
    $add('porata.fragment','porata','Map bông tai · mảnh vỡ',5,'Map 156–159','Nhánh kế tiếp, vật phẩm 934; giữ giới hạn 150.');
    $add('porata.last','porata','Map bông tai · nhánh cuối',.2,'Map 156–159','Vật phẩm 935; các nhánh được xét nối tiếp.');
    $add('angel.active','angel','Ngục tù · tài khoản kích hoạt',200/555,'Map 155','Cần set hủy diệt; xét trước lượt thường.');
    $add('angel.normal','angel','Ngục tù · lượt thường',10,'Map 155','Cần set hủy diệt; xét nếu lượt kích hoạt thất bại/không đủ điều kiện.');
    $add('skh.private','skh','Map riêng tư',200/9999,'Map 164','Giữ bonus option 236 và cỏ bốn lá.');
    $add('skh.early','skh','Các map SKH',200/9999,$early,'Giữ bonus option 236 và cỏ bốn lá.');
    $add('gold.planets','gold','Ba hành tinh',5,'Map ≤38, trừ 0, 7, 14, 21–26','Giữ nguyên số vàng mỗi lần rơi.');
    $add('gold.nappa','gold','Nappa',1,'Map 63–83');
    $add('gold.cold','gold','Map lạnh',30,'Map 105–110');
    $add('gold.future','gold','Tương lai',15,'Map 92–94, 96–100, 102, 103');
    $add('gold.dungeon','gold','Phó bản',1,'Kho báu, Doanh trại, Con đường rắn độc, Khí gas hủy diệt');
    $add('capsule.detector','capsule','Quái có máy dò',20,'Quái template 58–65, khi dùng máy dò','Capsule kỳ bí, vật phẩm 380.');
    $add('capsule.early','capsule','Map SKH',100/9999,$early,'Capsule vỡ, vật phẩm 1634.');
    $add('capsule.private','capsule','Map riêng tư',100/19999,'Map 164','Capsule vỡ, vật phẩm 1634.');
    foreach([1798=>5,1799=>5,1800=>5,1612=>5,1801=>1,1802=>100/130] as $id=>$base)$add('event.item'.$id,'event','Vật phẩm #'.$id,$base,'Map 0–163','Một lượt xét độc lập cho vật phẩm này.');
    $add('event.camp','event','Doanh trại',20,'Map 53–62','Vật phẩm 1778.');
    $add('event.mountain','event','Ngũ Hành Sơn',10,'Map 122–124','Ngẫu nhiên vật phẩm 541–542.');
    $add('gem.all','gem','Mọi map',.0001,'Mọi map có quái hợp lệ','Ngọc miễn phí bảo đảm không bị thay đổi.');
    $add('skill.normal','skill','Ngục tù · lượt thường',10,'Map 155','Bí kíp tuyệt kỹ, vật phẩm 1229.');
    $add('skill.active','skill','Ngục tù · kích hoạt và set hủy diệt',20,'Map 155','Xét khi lượt thường thất bại và đủ cả hai điều kiện.');
    return $rules;
}
