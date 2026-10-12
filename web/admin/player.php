<?php
session_start();
require_once "../config.php";

// 1. Kiểm tra quyền Admin
if(!isset($_SESSION['user'])){
    header("Location: /admin/panel.php");
    exit;
}

$user = $_SESSION['user'];
$userEsc = $conn->real_escape_string($user);
$q = $conn->query("SELECT is_admin FROM account WHERE username='$userEsc'");
$check = $q->fetch_assoc();

if(!$check || $check['is_admin'] != 1){
    echo "Không có quyền!";
    exit;
}

// --- PHẦN XỬ LÝ AJAX (SỬA LẠI CẤU TRÚC ĐỂ KHÔNG BỊ LỖI KẾT NỐI) ---
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
$dataRaw = file_get_contents("php://input");
$data = json_decode($dataRaw, true);

if (!$data || !isset($data['action'])) {
    echo json_encode([
        "status" => "error",
        "msg" => "Invalid request"
    ]);
    exit;
}

$action = $data['action'];

    // 1. LƯU CHỈ SỐ NHÂN VẬT
    if ($action == 'save_single_player') {
        $p_id = (int)$data['id'];
        $res = $conn->query("SELECT * FROM player WHERE id = $p_id");
        $p_old = $res->fetch_assoc();
        
        if (!$p_old) {
            echo json_encode(["status" => "error", "msg" => "Không tìm thấy nhân vật"]);
            exit;
        }

        $new_name = $conn->real_escape_string($data['name']);
        
        // Update data_point (Sức mạnh)
        $dp = json_decode($p_old['data_point'], true);
        $dp[1] = (float)$data['sm_su'];
        $dp_json = json_encode($dp);
        
        // Update pet (Đệ tử)
$pet_json = $p_old['pet'];

if (!empty($p_old['pet'])) {
    $pet = json_decode($p_old['pet'], true);

    if (is_array($pet) && isset($pet[1])) {

        $raw = $pet[1];

        // decode lần 1
        $pet_info = json_decode($raw, true);

        // nếu fail → decode lại
        if (!is_array($pet_info)) {
            $pet_info = json_decode(stripslashes($raw), true);
        }

        if (is_array($pet_info)) {

            // update SM đệ
        $pet_info[1] = (int)$data['sm_de'];   // SM đệ
$pet_info[2] = (int)$data['tn_de'];   // TN đệ

            // encode lại thành string JSON
            $pet[1] = json_encode($pet_info, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);

            // encode toàn bộ pet
            $pet_json = json_encode($pet, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        } else {
            // debug
            error_log("PET DECODE FAIL: " . $raw);
        }
    }
}
        // Update inventory (Vàng, Ngọc, Ruby)
        $inv = json_decode($p_old['data_inventory'], true);
        $inv[0] = (int)$data['gold'];
        $inv[1] = (int)$data['gem'];
        $inv[2] = (int)$data['ruby'];
        $inv_json = json_encode($inv);

    $dp_json_esc = $conn->real_escape_string($dp_json);
$pet_json_esc = $conn->real_escape_string($pet_json);
$inv_json_esc = $conn->real_escape_string($inv_json);

$sql = "UPDATE player 
SET name='$new_name', 
    data_point='$dp_json_esc', 
    pet='$pet_json_esc', 
    data_inventory='$inv_json_esc' 
WHERE id=$p_id";
        
        if($conn->query($sql)) {
            echo json_encode(["status" => "success"]);
        } else {
            echo json_encode(["status" => "error", "msg" => "Lỗi database khi lưu"]);
        }
        exit;
    }

elseif ($action == 'delete_single_item') {
    $p_id = (int)$data['p_id'];

    $allowedCols = ['items_bag', 'items_box'];
    if (!in_array($data['column'], $allowedCols)) {
        echo json_encode(["status" => "error", "msg" => "Column không hợp lệ"]);
        exit;
    }
    $col = $data['column'];

    $idx = (int)$data['index'];

    $res = $conn->query("SELECT `$col` FROM player WHERE id = $p_id LIMIT 1");
    if(!$res || $res->num_rows == 0){
        echo json_encode(["status"=>"error","msg"=>"Không tìm thấy player"]);
        exit;
    }

    $row = $res->fetch_assoc();
    $items = json_decode($row[$col], true);

    if (!is_array($items)) {
        echo json_encode(["status"=>"error","msg"=>"Dữ liệu item lỗi"]);
        exit;
    }

    if (isset($items[$idx])) {
        $empty_item_array = [-1, 0, 0, 0, 0, "[]"];
   $items[$idx] = "[-1,0,0,0,0,\"[]\"]";

        $new_json = json_encode($items, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        $new_json_esc = $conn->real_escape_string($new_json);

        if($conn->query("UPDATE player SET `$col` = '$new_json_esc' WHERE id = $p_id LIMIT 1")) {
            echo json_encode(["status" => "success"]);
        } else {
            echo json_encode(["status" => "error", "msg" => "Lỗi database"]);
        }
    } else {
        echo json_encode(["status"=>"error","msg"=>"Index không tồn tại"]);
    }
    exit;
}
    // 3. XÓA TOÀN BỘ NHÂN VẬT (ĐÃ TÁCH RIÊNG KHỎI KHỐI TRÊN)
    elseif ($action == 'delete_player') {
        $p_id = (int)$data['id'];
        if($conn->query("DELETE FROM player WHERE id = $p_id")) {
            echo json_encode(["status" => "success"]);
        } else {
            echo json_encode(["status" => "error", "msg" => "Lỗi database khi xóa nhân vật"]);
        }
        exit;
    }
}
// 2. Các hàm bổ trợ
function getPower($data_point){
    $arr = json_decode($data_point, true);
    return $arr[1] ?? 0;
}
function getPetInfo($pet){
    $pet = json_decode($pet, true);

    if(isset($pet[1])){
        $info = is_string($pet[1]) ? json_decode($pet[1], true) : $pet[1];

        return [
            'power' => $info[1] ?? 0,   // SM đệ
            'tiemnang' => $info[2] ?? 0 // TN đệ
        ];
    }

    return ['power' => 0, 'tiemnang' => 0];
}
function countItemInBag($bag_json, $target_id) {
    $items = json_decode($bag_json, true) ?? [];
    $total = 0;
    foreach($items as $itemStr) {
        $it = json_decode($itemStr, true);
        if(isset($it[0]) && $it[0] == $target_id) { $total += $it[1] ?? 0; }
    }
    return $total;
}
function formatK($num){
    if($num >= 1000000000) return round($num/1000000000, 1) . " Tỷ";
    if($num >= 1000000) return round($num/1000000, 1) . " Tr";
    if($num >= 1000) return round($num/1000, 1) . " K";
    return number_format($num);
}
// Load data
$item_temp = [];
$t_res = $conn->query("SELECT id, name, icon_id FROM item_template");
while($r = $t_res->fetch_assoc()) { $item_temp[$r['id']] = $r; }

// 1. Lấy toàn bộ danh sách player (hoặc giới hạn một lượng lớn nếu server yếu)
// Vì cần tính toán dựa trên JSON, ta cần fetch dữ liệu về trước
$sql_all = "SELECT p.*, a.username, ha.avatar_id 
            FROM player p 
            LEFT JOIN account a ON p.account_id = a.id 
            LEFT JOIN head_avatar ha ON p.head = ha.head_id";
$res_all = $conn->query($sql_all);

$all_players = [];
while($r = $res_all->fetch_assoc()) {
    // Tính toán số lượng vật phẩm 457 để dùng làm tiêu chí sắp xếp
    $r['tv_count'] = countItemInBag($r['items_bag'], 457);
    $all_players[] = $r;
}

// 2. Sắp xếp mảng $all_players theo tv_count giảm dần
usort($all_players, function($a, $b) {
    return $b['tv_count'] <=> $a['tv_count'];
});

// 3. Xử lý Phân trang trên mảng đã sắp xếp
$item_per_page = 500; 
$total_players = count($all_players);
$total_pages = ceil($total_players / $item_per_page);
$current_page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
if($current_page < 1) $current_page = 1;

$offset = ($current_page - 1) * $item_per_page;

// Cắt mảng để lấy dữ liệu cho trang hiện tại
$display_list = array_slice($all_players, $offset, $item_per_page);

// // Đếm tổng số player
// $total_res = $conn->query("SELECT COUNT(*) as total FROM player");
// $total_row = $total_res->fetch_assoc();
// $total_players = $total_row['total'];
// $total_pages = ceil($total_players / $item_per_page);

// // Truy vấn có giới hạn LIMIT và OFFSET
// $sql = "SELECT p.*, a.username, ha.avatar_id 
//         FROM player p 
//         LEFT JOIN account a ON p.account_id = a.id 
//         LEFT JOIN head_avatar ha ON p.head = ha.head_id 
//         ORDER BY p.id DESC 
//         LIMIT $item_per_page OFFSET $offset";
// $list = $conn->query($sql);

function renderItems($jsonData, $templates, $limit = false) {
    $items = json_decode($jsonData, true) ?? [];
    $html = '<div class="item-grid ' . ($limit ? 'limit-2-rows' : 'full-grid') . '">';
    foreach($items as $itemStr) {
        $it = json_decode($itemStr, true);
        $id = $it[0] ?? -1;
        $qty = $it[1] ?? 0;
        $html .= '<div class="item-slot">';
        if($id != -1 && isset($templates[$id])) {
            $icon = $templates[$id]['icon_id'];
            $html .= "<img src='/icon/$icon.png' onerror=\"this.src='/icon/1.png'\" title='".$templates[$id]['name']."'>";
            if($qty > 1) $html .= "<span class='qty'>$qty</span>";
        }
        $html .= '</div>';
    }
    $html .= '</div>';
    return $html;
}
?>

<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>Quản Lý Player</title>
<link rel="icon" type="image/png" href="/img/icon.jpg">
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

<style>
    .search-input {
    width: 95%;
    padding: 8px 12px;
    border: 2px solid #93c5fd;
    border-radius: 8px;
    outline: none;
    font-size: 0.9em;
    font-weight: bold;
    color: #1e40af;
    transition: all 0.3s;
}

.search-input:focus {
    border-color: #2563eb;
    background: #ffffff;
    box-shadow: 0 0 8px rgba(37, 99, 235, 0.2);
}

/* Hiệu ứng mờ dần khi ẩn dòng không khớp */
.player-row {
    transition: opacity 0.2s ease;
}
/* ===== STYLE ĐỒNG BỘ TRANG GIFTCODE ===== */
body{margin:0;font-family:'Segoe UI';font-size:1.1em;background:linear-gradient(135deg,#e0f2fe,#bae6fd); color: #1e293b;}
.container{max-width:1300px;margin:auto;padding:15px;}
.btn-delete-player {
    margin-top: 5px;
    background: #fee2e2;
    color: #ef4444;
    border: 1px solid #fecaca;
    padding: 4px 8px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: bold;
    cursor: pointer;
    transition: 0.2s;
}
.btn-delete-player:hover {
    background: #ef4444;
    color: white;
}
/* BUTTON EFFECTS */
.menu-buttons button, .action-box button{ position: relative; overflow: hidden; transition: all 0.2s ease; cursor:pointer; border:none; border-radius:10px; color:white; font-weight:600; }
.menu-buttons button:hover, .action-box button:hover{ transform: translateY(-2px) scale(1.05); box-shadow: 0 8px 20px rgba(0,0,0,0.25); }

/* HEADER */
.header-box{ display:flex; justify-content:space-between; align-items:center; background:white; padding:20px; border-radius:16px; box-shadow:0 10px 25px rgba(0,0,0,.15); margin-bottom:20px; }
.header-left{display:flex;align-items:center;gap:20px;}
.header-left img{width:200px;height:200px;object-fit:contain;}
.header-content{display:flex;flex-direction:column;gap:12px;}
.header-content h1{margin:0;color:#1e3a8a; font-size: 2.2em;}
.menu-buttons{display:flex;gap:10px;}
.menu-buttons button{ padding:10px 20px; font-size:1.1em; }
.home{background:#3b82f6;} .refresh{background:#22c55e;}

.action-box{ display:flex; justify-content:center; padding:15px; background:white; border-radius:12px; box-shadow:0 5px 15px rgba(0,0,0,.1); margin-bottom:15px;}
.save-all{background:#16a34a; padding:10px 25px; font-size:1.3em !important;}
/* Căn giữa ô Tài lẻ */
.cell-tai-le {
    display: flex;
    flex-direction: column;
    align-items: center; /* Căn giữa theo chiều ngang */
    justify-content: center; /* Căn giữa theo chiều dọc */
    height: 100%;
    gap: 5px; /* Khoảng cách giữa các dòng */
}

/* Container cho từng dòng (Vàng/Ngọc/Ruby) */
.stat-row {
    display: flex;
    align-items: center;
    width: 100px; /* Độ rộng cố định để icon và số thẳng hàng */
    justify-content: flex-start; /* Nội dung bên trong khối 100px dồn về trái */
}

.stat-row img {
    margin-right: 10px;
    flex-shrink: 0;
}

.stat-row strong {
    white-space: nowrap;
}
/* TABLE */
.table-wrap{ overflow-x:auto; border-radius:12px; box-shadow:0 10px 25px rgba(0,0,0,.2); background:white;}
table{ width:100%; border-collapse:collapse; table-layout: fixed;}
th{ background:#2563eb; color:white; padding:15px; font-size: 1.1em;}
td{ padding:12px; text-align:center; border-bottom:1px solid #e2e8f0; vertical-align: middle; word-wrap: break-word;}

.inp-edit { border: none; background: transparent; font-family: inherit; font-size: inherit; text-align: center; font-weight: bold; width: 120px; outline:none; transition: 0.2s; border-radius: 6px;}
.inp-edit:focus { background: #f1f5f9; box-shadow: 0 0 0 2px #93c5fd; }
.changed { background: #93c5fd !important; color: #1e3a8a; box-shadow: inset 0 0 5px rgba(0,0,0,0.1); }

/* POPUP ĐỒNG BỘ GIFTCODE */
.popup-bg{ position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); display:none; align-items:center; justify-content:center; z-index:9999; }
.popup-box{ width:90%; max-width:500px; padding:42px 36px; border-radius:18px; text-align:center; background:linear-gradient(145deg,#f8fafc,#e2e8f0); color:#1e293b; box-shadow: 0 20px 60px rgba(0,0,0,.4); animation:pop .25s ease; position:relative; }
@keyframes pop{ from{transform:scale(.8);opacity:0} to{transform:scale(1);opacity:1} }
.popup-icon{ font-size:84px; }
.popup-title{ font-size:40px; margin:10px 0; }
.popup-content{ font-size:28px; }
.popup-success .popup-icon{ color:#22c55e; }
.popup-error .popup-icon{ color:#ef4444; }
.popup-warning .popup-icon{ color:#f59e0b; }
.close-btn-pop{ position:absolute; top:-18px; right:-10px; font-size:40px; font-weight:900; width:65px; height:65px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#ef4444; color:white; cursor:pointer; transition:.2s; z-index: 10;}
.close-btn-pop:hover{ transform:scale(1.15) rotate(90deg); }
.btn-save-player {
    margin-top: 5px;
    background: #dcfce7;
    color: #16a34a;
    border: 1px solid #bbf7d0;
    padding: 8px 16px;
    border-radius: 6px;
    font-size: 20px;
    font-weight: bold;
    cursor: pointer;
    transition: 0.2s;
}
.btn-save-player:hover {
    background: #16a34a;
    color: white;
}
/* ITEM GRID - ĐÃ CHỈNH 7 Ô */
.item-grid { display: grid; grid-template-columns: repeat(7, 42px); gap: 4px; justify-content: center; cursor: pointer; padding: 5px;}
.limit-2-rows { max-height: 90px; overflow: hidden; mask-image: linear-gradient(to bottom, black 70%, transparent 100%); }
.item-slot { width: 42px; height: 42px; background: #f8fafc; border: 1px solid #cbd5e1; position: relative; display: flex; align-items: center; justify-content: center; border-radius: 6px; }
.item-slot img { width: 32px; height: 32px; }
.item-slot .qty { position: absolute; bottom: 0; right: 2px; font-size: 10px; color: #1e40af; font-weight: 800; background: rgba(255,255,255,0.7); border-radius: 3px; padding: 0 2px;}
/* Nút bấm trong Popup Xác nhận */
.popup-btns { display: flex; gap: 15px; justify-content: center; margin-top: 25px; }
.btn-confirm { background: #ef4444; color: white; padding: 10px 25px; border-radius: 10px; cursor: pointer; border: none; font-weight: bold; font-size: 1.1em; }
.btn-cancel { background: #94a3b8; color: white; padding: 10px 25px; border-radius: 10px; cursor: pointer; border: none; font-weight: bold; font-size: 1.1em; }
.btn-confirm:hover { background: #dc2626; transform: scale(1.05); }
.btn-cancel:hover { background: #64748b; transform: scale(1.05); }
/* MODAL CHI TIẾT */
.modal { display: none; position: fixed; z-index: 100; left:0; top:0; width:100%; height:100%; background:rgba(0,0,0,0.85); align-items:center; justify-content:center; }
.modal-content { background:white; padding:30px; border-radius:20px; width:700px; position:relative; animation: pop .3s ease; }
.full-grid { grid-template-columns: repeat(10, 55px) !important; gap: 8px; }
.full-grid .item-slot { width: 55px; height: 55px; }
.btn-del-mini { position: absolute; top: -8px; right: -8px; background: #ef4444; color: white; border: 2px solid white; border-radius: 50%; width: 22px; height: 22px; font-size: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; z-index: 10; font-weight: bold; }
</style>
</head>
<body>

<div class="container">
    <div class="header-box">
        <div class="header-left">
            <img src="/img/account.gif">
          <div class="header-content">
    <h1>Player <span style="font-size: 0.5em; vertical-align: middle; background: #2563eb; color: white; padding: 4px 12px; border-radius: 20px;">Tổng: <?= number_format($total_players) ?></span></h1>
    <div class="menu-buttons">
        <button class="home" onclick="location.href='/admin/panel.php'">🏠 Go Panel</button>
        <button class="refresh" onclick="location.reload()">🔄 Làm Mới</button>
    </div>
</div>
        </div>
        <div class="header-right">
            <img src="/img/vip.jpg" style="width:500px;height:200px;object-fit:contain;">
        </div>
    </div>

    <!-- <div class="action-box">
        <button class="save-all" onclick="saveAllData()">💾 Lưu Dữ Liệu</button>
    </div> -->
<div class="pagination" style="display: flex; justify-content: center; gap: 8px; margin-bottom: 20px;">
    <?php 
    // Hiển thị tối đa 10 trang gần nhất để tránh bị tràn màn hình nếu có hàng nghìn trang
    $start_loop = max(1, $current_page - 4);
    $end_loop = min($total_pages, $current_page + 5);
    
    for($i = $start_loop; $i <= $end_loop; $i++): ?>
        <a href="?page=<?= $i ?>" style="padding: 8px 16px; background: <?= ($i == $current_page) ? '#2563eb' : 'white' ?>; color: <?= ($i == $current_page) ? 'white' : '#1e3a8a' ?>; border-radius: 8px; text-decoration: none; font-weight: bold; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
            <?= $i ?>
        </a>
    <?php endfor; ?>
</div>
 <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th width="80">Nhân vật</th> 
                    <th width="120">Sức mạnh</th> 
                    <th width="120">Tài chính</th>  
                    <th width="300">Hành trang</th> 
                    <th width="300">Rương đồ</th>
                    <th width="40">VIP</th> </tr>
            </thead>
            <tr class="search-row" style="background: #dbeafe;">
    <td>
        <input type="text" id="searchPlayer" class="search-input" placeholder="🔍 Nhập tên nhân vật...">
    </td>
    <td colspan="5"></td> </tr>
         <tbody id="playerTableBody">
            <?php 
foreach($display_list as $row): 
    $inv = json_decode($row['data_inventory'], true);
    // Số lượng thỏi vàng đã được tính ở bước sắp xếp phía trên
    $tv_count = $row['tv_count']; 
?>
             <tr class="player-row" data-id="<?= $row['id'] ?>" data-name="<?= mb_strtolower($row['name']) ?>">
                    <td style="background: #f8fafc;">
                        <img src="/icon/<?= $row['avatar_id'] ?? 1 ?>.png" width="45"><br>
                        <input type="text" class="inp-edit track name-input" style="color:#2563eb; width: 100%;" value="<?= htmlspecialchars($row['name']) ?>">
                    </td>

                    <td>
                        <?php 
                           $sm_su = getPower($row['data_point']);
$petInfo = getPetInfo($row['pet']);

$sm_de = $petInfo['power'];
$tn_de = $petInfo['tiemnang'];
                        ?>
   <div class="power-box" onclick="editPower(this)">
    SM Sư: <span><?= formatK($sm_su) ?></span>
    <input type="number" class="inp-edit track sm_su" value="<?= $sm_su ?>" style="display:none;">
</div>
<div class="power-box" onclick="editPower(this)">
    SM Đệ: <span><?= formatK($sm_de) ?></span>
    <input type="number" class="inp-edit track sm_de" value="<?= $sm_de ?>" style="display:none;">
</div>

<div class="power-box" onclick="editPower(this)">
    TN Đệ: <span><?= formatK($tn_de) ?></span>
    <input type="number" class="inp-edit track tn_de" value="<?= $tn_de ?>" style="display:none;">
</div>


</td>

              <td style="background: #ffffff;">
    <div class="cell-tai-le">
        <div class="stat-row">
            <img src="/img/930.png" width="18"> 
            <strong><?= formatK($inv[0] ?? 0) ?></strong>
            <input type="number" class="inp-edit track gold" value="<?= $inv[0] ?? 0 ?>" style="display:none;">
        </div>

        <div class="stat-row">
            <img src="/img/932.png" width="18"> 
            <strong><?= formatK($inv[1] ?? 0) ?></strong>
            <input type="number" class="inp-edit track gem" value="<?= $inv[1] ?? 0 ?>" style="display:none;">
        </div>

        <div class="stat-row">
            <img src="/img/7743.png" width="18"> 
            <strong><?= formatK($inv[2] ?? 0) ?></strong>
            <input type="number" class="inp-edit track ruby" value="<?= $inv[2] ?? 0 ?>" style="display:none;">
        </div>

      <div class="stat-row" style="margin-top: 5px; padding-top: 5px; width: 80px;">
    <img src="/icon/4028.png" width="40"> 
    <span style="font-weight:bold; color:#b45309;"><?= number_format($tv_count) ?></span>
</div>
    </div>
</td>

                    <td onclick='viewBox(<?= $row['id'] ?>, "items_bag", <?= $row['items_bag'] ?>)'>
                        <?= renderItems($row['items_bag'], $item_temp, true) ?>
                    </td>

                    <td onclick='viewBox(<?= $row['id'] ?>, "items_box", <?= $row['items_box'] ?>)'>
                        <?= renderItems($row['items_box'], $item_temp, true) ?>
                    </td>

                  <td style="background: #fff5f5;">
    <button class="btn-delete-player" onclick="deletePlayer(<?= $row['id'] ?>, '<?= htmlspecialchars($row['name']) ?>')">
        🗑️
    </button>

    <button class="btn-save-player" onclick="saveSinglePlayer(this)">
        ✔
    </button>
</td>
                </tr>
<?php endforeach; ?>
            </tbody>
        </table>
    </div>

<div id="savePopup" class="popup-bg">
    <div class="popup-box">
        <div class="close-btn-pop" onclick="closePopup()">✖</div>
        <div id="savePopupContent"></div>
    </div>
</div>

<div id="itemModal" class="modal" onclick="$(this).hide()">
    <div class="modal-content" onclick="event.stopPropagation()">
        <div class="close-btn-pop" style="width:45px; height:45px; font-size:25px;" onclick="$('#itemModal').hide()">✖</div>
        <h2 style="color:#1e3a8a; margin-top:0; font-size:30px; border-bottom: 2px solid #2563eb; padding-bottom:10px;">📦 Chi Tiết Vật Phẩm</h2>
        <div id="modalBody" class="item-grid full-grid" style="margin-top:20px;"></div>
    </div>
</div>

<script>
const itemTemplates = <?= json_encode($item_temp) ?>;

$(document).on('blur', '.sm_su, .sm_de, .tn_de', function(){
    let val = parseInt(this.value || 0);
    let box = this.parentElement;
    let span = box.querySelector("span");

    span.innerText = formatK(val);
    span.style.display = "inline";
    this.style.display = "none";

    // ✅ BẮT BUỘC giữ changed
    this.classList.add("changed");
});
// --- 1. HÀM TẠO POPUP XÁC NHẬN CHUNG ---
function showConfirm(title, content, onConfirm) {
    let popup = document.getElementById("savePopup");
    let box = document.querySelector(".popup-box");
    let html = document.getElementById("savePopupContent");
    
    box.className = "popup-box popup-warning"; // Dùng màu vàng cảnh báo
    html.innerHTML = `
        <div class="popup-icon">⚠️</div>
        <div class="popup-title" style="font-size:30px;">${title}</div>
        <div class="popup-content" style="font-size:20px; margin-bottom:10px;">${content}</div>
        <div class="popup-btns">
            <button class="btn-confirm" id="confirmOk">Xóa Ngay</button>
            <button class="btn-cancel" onclick="closePopup()">Hủy</button>
        </div>
    `;
    
    $(popup).css('display', 'flex');

    // Gán sự kiện cho nút xác nhận
    document.getElementById("confirmOk").onclick = function() {
        closePopup();
        onConfirm();
    };
}
function editPower(el){
    let span = el.querySelector("span");
    let input = el.querySelector("input");

    span.style.display = "none";
    input.style.display = "inline-block";
    input.focus();

    // Đảm bảo luôn track thay đổi
    input.classList.add("changed");
}
function formatK(num){
    num = parseInt(num || 0);
    if(num >= 1000000000) return (num/1000000000).toFixed(1) + " Tỷ";
    if(num >= 1000000) return (num/1000000).toFixed(1) + " Tr";
    if(num >= 1000) return (num/1000).toFixed(1) + " K";
    return num.toLocaleString();
}
// --- 2. SỬA HÀM XÓA NHÂN VẬT ---
function deletePlayer(id, name) {
    showConfirm(
        "Cảnh Báo", 
        `Xóa vĩnh viễn <br><b style="color:#ef4444">[${name}]</b>`, 
        async () => {
            try {
                let response = await fetch(window.location.href, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action: 'delete_player', id: id })
                });
                let res = await response.json();
                if (res.status === 'success') {
                    showPopup("success", "Thành Công", `Nhân vật ${name} đã bị loại bỏ.`);
                    $(`.player-row[data-id="${id}"]`).fadeOut(500);
                } else {
                    showPopup("error", "Lỗi", res.msg);
                }
            } catch (e) {
                showPopup("error", "Lỗi", "Không thể kết nối đến máy chủ.");
            }
        }
    );
}

// --- 3. SỬA HÀM XÓA VẬT PHẨM LẺ ---
function deleteSingle(pId, col, idx, e) {
    if (e) e.stopPropagation();
    showConfirm(
        "Xóa Vật Phẩm", 
        "Xóa vật phẩm này khỏi hành trang", 
        async () => {
            try {
                let response = await fetch('', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ action: 'delete_single_item', p_id: pId, column: col, index: idx })
                });
                let res = await response.json();
                if(res.status === 'success') {
                    // Thay vì reload, hãy đóng modal và báo thành công
                    $('#itemModal').hide();
                    showPopup("success", "Đã Xóa", "Vật phẩm đã được dọn dẹp.");
                    setTimeout(() => location.reload(), 1200); // Reload sau 1.2s để user kịp thấy thông báo
                }
            } catch (e) { 
                showPopup("error", "Lỗi", "Lỗi kết nối server!");
            }
        }
    );
}
// Hàm hiện Popup kiểu Giftcode
function showPopup(type, title, content){
    let popup = document.getElementById("savePopup");
    let box = document.querySelector(".popup-box");
    let html = document.getElementById("savePopupContent");
    box.className = "popup-box popup-" + type;
    let icon = type === "success" ? "✅" : (type === "error" ? "❌" : "⚠️");
    html.innerHTML = `<div class="popup-icon">${icon}</div><div class="popup-title">${title}</div><div class="popup-content">${content}</div>`;
    $(popup).css('display', 'flex');
}
function closePopup(){ $('#savePopup').hide(); }
// Lưu dữ liệu đồng bộ logic Giftcode
// async function saveAllData() {
//     let rows = document.querySelectorAll(".player-row");
//     let success = 0, fail = 0;
//     let isAnyChange = false;

//     for(let row of rows){
//         let changedInputs = row.querySelectorAll(".changed");
//         if(changedInputs.length === 0) continue;
//         isAnyChange = true;

//         let p_id = row.getAttribute("data-id");
//       let data = {
//     action: 'save_single_player',
//     id: p_id,
//     name: row.querySelector('.name-input').value,
//     sm_su: row.querySelector('.sm_su').value,
//     sm_de: row.querySelector('.sm_de').value,
//     tn_de: row.querySelector('.tn_de').value,
//     gold: row.querySelector('.gold').value,
//     gem: row.querySelector('.gem').value,
//     ruby: row.querySelector('.ruby').value
// };
//         try {
//             let res = await fetch(window.location.href, {
//                 method: "POST",
//                 headers: {"Content-Type": "application/json"},
//                 body: JSON.stringify(data)
//             });
//             let json = await res.json();
//             if(json.status === "success"){
//                 success++;
//                 $(changedInputs).removeClass("changed");
//             } else {
//                 fail++;
//                 showPopup("error", "Thất bại", json.msg);
//                 return;
//             }
//         } catch(e) { fail++; }
//     }

//     if(!isAnyChange) showPopup("warning", "Chú ý", "Bạn chưa thay đổi thông số nào!");
//     else if(fail === 0) showPopup("success", "Thành Công", `Đã cập nhật chỉ số cho ${success} nhân vật.`);
// }
$(document).ready(function() {
    $("#searchPlayer").on("keyup", function() {
        var value = $(this).val().toLowerCase().trim(); // Lấy giá trị, viết thường và xóa khoảng trắng thừa

        $("#playerTableBody tr").each(function() {
            // Lấy tên từ thuộc tính data-name mà bạn đã gán ở mỗi dòng <tr>
            var playerName = $(this).attr("data-name");

            if (playerName) {
                // Kiểm tra xem tên nhân vật có chứa từ khóa tìm kiếm không
                if (playerName.indexOf(value) > -1) {
                    $(this).show(); // Hiện nếu khớp
                } else {
                    $(this).hide(); // Ẩn nếu không khớp
                }
            }
        });
    });
});
async function saveSinglePlayer(btn) {
    let row = btn.closest(".player-row");

    // ✅ CHECK: nếu không có thay đổi thì báo luôn
    let changedInputs = row.querySelectorAll(".changed");
    if (changedInputs.length === 0) {
        showPopup("warning", "Chú ý", "Chưa có thay đổi nào!");
        return;
    }

    let p_id = row.getAttribute("data-id");

    let data = {
        action: 'save_single_player',
        id: p_id,
        name: row.querySelector('.name-input').value,
        sm_su: row.querySelector('.sm_su').value,
        sm_de: row.querySelector('.sm_de').value,
        tn_de: row.querySelector('.tn_de').value,
        gold: row.querySelector('.gold').value,
        gem: row.querySelector('.gem').value,
        ruby: row.querySelector('.ruby').value
    };

    try {
        let res = await fetch(window.location.href, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(data)
        });

        let json = await res.json();

        if(json.status === "success"){
            showPopup("success", "Đã Lưu", "Cập nhật thành công!");
            row.querySelectorAll(".changed").forEach(el => el.classList.remove("changed"));
        } else {
            showPopup("error", "Lỗi", json.msg);
        }
    } catch(e){
        showPopup("error", "Lỗi", "Không thể kết nối server!");
    }
}
// Logic xem và xóa vật phẩm - Đã cập nhật hiển thị số lượng
function viewBox(pId, column, data) {
    let html = '';
    data.forEach((itemStr, index) => {
        let it = JSON.parse(itemStr);
        let itemId = it[0];   // ID vật phẩm
        let qty = it[1];      // Số lượng vật phẩm
        
        html += `<div class="item-slot" style="position: relative;">`; // Đảm bảo position relative để hiện qty
        
        if(itemId !== -1 && itemTemplates[itemId]) {
            let iconId = itemTemplates[itemId].icon_id;
            let itemName = itemTemplates[itemId].name;
            
            html += `<img src="/icon/${iconId}.png" onerror="this.src='/icon/1.png'" title="${itemName}">`;
            
            // Hiển thị số lượng nếu lớn hơn 1
            if(qty > 1) {
                html += `<span class="qty" style="position: absolute; bottom: 2px; right: 2px; font-size: 11px; color: #1e40af; font-weight: 800; background: rgba(255,255,255,0.8); border-radius: 3px; padding: 0 3px; pointer-events: none;">${qty}</span>`;
            }
            
            // Nút xóa vật phẩm lẻ
         html += `<button class="btn-del-mini" onclick="event.stopPropagation(); deleteSingle(${pId}, '${column}', ${index})">✕</button>`;
        }
        
        html += `</div>`;
    });
    $('#modalBody').html(html);
    $('#itemModal').css('display', 'flex');
}

window.onclick = function(e) {
    if ($(e.target).hasClass('popup-bg')) closePopup();
    if ($(e.target).hasClass('modal')) $('.modal').hide();
}
</script>
</body>
</html>