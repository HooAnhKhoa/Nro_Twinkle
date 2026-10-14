<?php
session_start();
require_once "../config.php";

// ===== CHECK ADMIN =====
if(!isset($_SESSION['user'])){ header("Location: /admin/panel.php"); exit; }
$userEsc = $conn->real_escape_string($_SESSION['user']);
$q = $conn->query("SELECT is_admin FROM account WHERE username='$userEsc'");
$check = $q->fetch_assoc();
if(!$check || $check['is_admin'] != 1){ echo "Không có quyền!"; exit; }

// ===== XỬ LÝ PHÂN TRANG =====
$item_per_page = 500; 
$current_page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
if($current_page < 1) $current_page = 1;
$offset = ($current_page - 1) * $item_per_page;

$total_res = $conn->query("SELECT COUNT(*) as total FROM history_transaction");
$total_row = $total_res->fetch_assoc();
$total_transactions = $total_row['total'];
$total_pages = ceil($total_transactions / $item_per_page);

// ===== LẤY DANH SÁCH ICON TỪ ITEM_TEMPLATE =====
$itemIcons = [];
$getTemplates = $conn->query("SELECT name, icon_id FROM item_template");
while($t = $getTemplates->fetch_assoc()){
    $itemIcons[trim($t['name'])] = $t['icon_id'];
}

function renderItems($itemString, $iconsMap) {
    if (empty(trim($itemString)) || $itemString == "Gold: 0, ") return "<span style='color:#ccc'>Trống</span>";
    $output = "";
    $items = explode(",", $itemString);
    foreach ($items as $item) {
        $item = trim($item);
        if (empty($item)) continue;
        $iconId = null;
        $formattedItem = preg_replace_callback('/\d+/', function($matches) {
            return number_format($matches[0], 0, ',', '.');
        }, $item);
        if (stripos($item, 'Gold') !== false) { $iconId = 930; } 
        else {
            $nameClean = preg_replace('/ \(x\d+\)/', '', $item);
            if (isset($iconsMap[$nameClean])) $iconId = $iconsMap[$nameClean];
        }
        $output .= "<div class='item-row'>";
        if($iconId) $output .= "<img src='/icon/{$iconId}.png' class='mini-icon' onerror=\"this.src='/icon/1.png'\"> ";
        $output .= "<span>" . htmlspecialchars($formattedItem) . "</span>";
        $output .= "</div>";
    }
    return $output;
}

$sql = "SELECT t.*, h1.avatar_id as avt1, h2.avatar_id as avt2
        FROM history_transaction t
        LEFT JOIN player p1 ON SUBSTRING_INDEX(TRIM(t.player_1), ' (', 1) = p1.name
        LEFT JOIN head_avatar h1 ON p1.head = h1.head_id
        LEFT JOIN player p2 ON SUBSTRING_INDEX(TRIM(t.player_2), ' (', 1) = p2.name
        LEFT JOIN head_avatar h2 ON p2.head = h2.head_id
        ORDER BY t.time_tran DESC LIMIT $item_per_page OFFSET $offset";
$list = $conn->query($sql);
?>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Lịch Sử Giao Dịch</title>
    <link rel="icon" type="image/png" href="/img/icon.jpg">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        body {
            margin: 0; font-family: 'Segoe UI'; font-size: 1.1em;
            background: linear-gradient(135deg, #e0f2fe, #bae6fd);
        }
        .container { max-width: 1300px; margin: auto; padding: 15px; }

        /* ===== HEADER BOX & BUTTON EFFECTS (Giữ nguyên từ account.php) ===== */
        .header-box {
            display: flex; justify-content: space-between; align-items: center;
            background: white; padding: 20px; border-radius: 16px;
            box-shadow: 0 10px 25px rgba(0,0,0,.15); margin-bottom: 20px;
        }
        .header-left { display: flex; align-items: center; gap: 20px; }
        .header-left img { width: 200px; height: 200px; object-fit: contain; }
        .header-content { display: flex; flex-direction: column; gap: 10px; }
        .header-content h1 { margin: 0; color: #1e3a8a; }

        .menu-buttons { display: flex; gap: 10px; }
        .menu-buttons button {
            padding: 8px; border: none; border-radius: 10px; color: white;
            font-weight: 600; cursor: pointer; font-size: 1.2em;
            position: relative; overflow: hidden; transition: all 0.2s ease;
        }
        .home { background: #3b82f6; }
        .refresh { background: #22c55e; }

        /* Hiệu ứng Hover & Click giống account.php */
        .menu-buttons button:hover {
            transform: translateY(-2px) scale(1.05);
            box-shadow: 0 8px 20px rgba(0,0,0,0.25);
        }
        .menu-buttons button:active {
            transform: scale(0.95);
            box-shadow: 0 3px 10px rgba(0,0,0,0.2);
        }

        /* ===== PHÂN TRANG ===== */
        .pagination { display: flex; justify-content: center; gap: 8px; margin: 20px 0; }
        .pagination a { 
            padding: 10px 18px; background: white; border-radius: 8px; 
            text-decoration: none; color: #1e3a8a; font-weight: bold; 
            box-shadow: 0 4px 10px rgba(0,0,0,0.1); transition: all 0.2s;
        }
        .pagination a.active { background: #2563eb; color: white; transform: scale(1.1); }
        .pagination a:hover:not(.active) { background: #dbeafe; transform: translateY(-2px); }

        /* ===== TABLE STYLES ===== */
        .table-wrap { 
            overflow: hidden; border-radius: 12px; 
            box-shadow: 0 10px 25px rgba(0,0,0,.2); background: white; 
        }
        table { width: 100%; border-collapse: collapse; table-layout: fixed; }
        th { background: #2563eb; color: white; padding: 15px 5px; }
        td { padding: 12px 5px; text-align: center; border-bottom: 1px solid #e2e8f0; vertical-align: middle; }

        .search-input {
            width: 85%; padding: 6px; border: 2px solid #93c5fd; border-radius: 8px; outline: none;
        }
        .player-name { font-weight: bold; color: #2563eb; font-size: 17px; }
        .avt-img { width: 65px; height: 65px; object-fit: contain; image-rendering: pixelated; }
        .item-list-box {
            background: #f8fafc; padding: 8px; border-radius: 8px;
            border: 1px solid #e2e8f0; text-align: left; width: 90%; margin: 0 auto;
        }
        .item-row { display: flex; align-items: center; gap: 6px; font-size: 15px; margin-bottom: 2px; }
        .mini-icon { width: 24px; height: 24px; }
        .time-col { font-size: 15px; color: #475569; font-weight: bold; }
    </style>
<link rel="stylesheet" href="/typography.css?v=20261014-1"><link rel="stylesheet" href="admin-ui.css?v=20261015-5"><script src="admin-ui.js?v=20261015-3" defer></script><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body class="admin-page" data-admin-page="giaodich"><?php require_once __DIR__.'/admin_ui.php'; adminPageToolbar('Giao dịch'); ?>
<div class="container">
    <div class="header-box">
        <div class="header-left">
            <img src="/img/account.gif">
            <div class="header-content">
                <h1>Giao Dịch <span style="font-size: 0.5em; vertical-align: middle; background: #2563eb; color: white; padding: 4px 12px; border-radius: 20px;">Tổng: <?= number_format($total_transactions) ?></span></h1>
                <div class="menu-buttons">
                    <button class="home" onclick="location.href='/admin/panel.php'">🏠 Go Panel</button>
                    <button class="refresh" onclick="location.reload()">🔄 Làm Mới</button>
                </div>
            </div>
        </div>
        <div class="header-right">
            <img src="/img/vip.jpg" style="width:500px; height:200px; object-fit:contain;">
        </div>
    </div>

    <div class="pagination">
        <?php for($i = 1; $i <= $total_pages; $i++): ?>
            <a href="?page=<?= $i ?>" class="<?= ($i == $current_page) ? 'active' : '' ?>"><?= $i ?></a>
        <?php endfor; ?>
    </div>

    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th width="110">Thời Gian</th>
                    <th width="160">Người Gửi</th>
                    <th width="30%">Vật Phẩm 1</th>
                    <th width="30%">Vật Phẩm 2</th>
                    <th width="160">Người Nhận</th>
                </tr>
                <tr class="search-row" style="background: #dbeafe;">
                    <td></td>
                    <td><input type="text" id="searchP1" class="search-input" placeholder="🔍 Lọc..."></td>
                    <td></td>
                    <td></td>
                    <td><input type="text" id="searchP2" class="search-input" placeholder="🔍 Lọc..."></td>
                </tr>
            </thead>
            <tbody id="tranTableBody">
                <?php while($row = $list->fetch_assoc()): 
                    $avt1 = $row['avt1'] ?? 1; $avt2 = $row['avt2'] ?? 1;
                    $name1 = explode(" (", $row['player_1'])[0];
                    $name2 = explode(" (", $row['player_2'])[0];
                    $displayTime = str_replace(" ", "<br>", $row['time_tran']);
                ?>
                <tr class="tran-row" data-p1="<?= mb_strtolower($name1) ?>" data-p2="<?= mb_strtolower($name2) ?>">
                    <td class="time-col"><?= $displayTime ?></td>
                    <td>
                        <div style="display:flex; flex-direction:column; align-items:center;">
                            <img src="/icon/<?= $avt1 ?>.png" class="avt-img" onerror="this.src='/icon/1.png'">
                            <span class="player-name"><?= htmlspecialchars($name1) ?></span>
                        </div>
                    </td>
                    <td><div class="item-list-box"><?= renderItems($row['item_player_1'], $itemIcons) ?></div></td>
                    <td><div class="item-list-box"><?= renderItems($row['item_player_2'], $itemIcons) ?></div></td>
                    <td>
                        <div style="display:flex; flex-direction:column; align-items:center;">
                            <img src="/icon/<?= $avt2 ?>.png" class="avt-img" onerror="this.src='/icon/1.png'">
                            <span class="player-name"><?= htmlspecialchars($name2) ?></span>
                        </div>
                    </td>
                </tr>
                <?php endwhile; ?>
            </tbody>
        </table>
    </div>

</div>

<script>
$(document).ready(function() {
    $("#searchP1, #searchP2").on("keyup", function() {
        let v1 = $("#searchP1").val().toLowerCase().trim();
        let v2 = $("#searchP2").val().toLowerCase().trim();
        $("#tranTableBody tr").each(function() {
            let p1 = $(this).attr("data-p1");
            let p2 = $(this).attr("data-p2");
            $(this).toggle(p1.indexOf(v1) > -1 && p2.indexOf(v2) > -1);
        });
    });
});
</script>
</body>
</html>