<?php
function eventCatalog(): array {return [
 'broly'=>['name'=>'Broly','icon'=>'⚡','scope'=>'Broly và Super Broly','detail'=>'Bật/tắt quần thể Broly có sẵn. Tắt sẽ cho boss rời map, không phát thưởng.'],
 'hung_vuong'=>['name'=>'Giỗ Tổ Hùng Vương','icon'=>'🏮','scope'=>'Sơn Tinh · Thủy Tinh · Vua Hùng · Nồi bánh','detail'=>'Tắt sẽ ẩn Vua Hùng, Nồi bánh và Dưa hấu; chặn dâng lễ, nấu bánh và thu hoạch. Dữ liệu cây đã trồng được giữ lại để thu hoạch khi bật lại.'],
 'tet'=>['name'=>'Tết Nguyên Đán','icon'=>'🧧','scope'=>'Lân con · Cây nêu','detail'=>'Điều khiển 10 Lân con và ẩn/hiện NPC Cây nêu đã có trên map. Không tạo thêm NPC Đường Tăng từ cấu hình Tết cũ.'],
 'halloween'=>['name'=>'Halloween','icon'=>'🎃','scope'=>'Bí Ma · Ma Trơi · Dơi','detail'=>'Điều khiển 10 boss mỗi loại và phần thưởng của chúng.'],
 'christmas'=>['name'=>'Giáng sinh','icon'=>'🎄','scope'=>'Ông già Noel · Quà rơi từ boss','detail'=>'Điều khiển 30 Ông già Noel. Khi tắt, boss ngừng xuất hiện và ngừng thả quà.'],
 'trung_thu'=>['name'=>'Trung Thu','icon'=>'🌙','scope'=>'Khỉ Đột · Nguyệt Thần · Nhật Thần','detail'=>'Điều khiển boss, phần thưởng và ẩn/hiện NPC Trung Thu đã có trên map. Giữ quan hệ xuất hiện giữa các boss.'],
 'summer'=>['name'=>'Xe nước mía','icon'=>'🥤','scope'=>'Ẩn/hiện NPC · Đổi nước mía','detail'=>'Tắt sẽ ẩn Xe nước mía và chặn đổi; bật sẽ hiện lại trên map. Mía rơi từ quái được quản lý ở công tắc Vật phẩm EV.'],
 'mob_rewards'=>['name'=>'Vật phẩm EV từ quái','icon'=>'🎁','scope'=>'Các nhánh thuộc nhóm rơi đồ Sự kiện','detail'=>'Mía, đồ ăn 1798–1802, vật phẩm trại 1778 và Ngũ Hành Sơn 541–542. Tắt chặn rơi mới; tỷ lệ đã chỉnh được giữ nguyên.'],
];}
