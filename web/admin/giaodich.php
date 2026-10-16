<?php
session_start();
require_once "../config.php";

// ===== CHECK ADMIN =====
if(!isset($_SESSION['user'])){ header("Location: /admin/panel.php"); exit; }
$userEsc = $conn->real_escape_string($_SESSION['user']);
$q = $conn->query("SELECT is_admin FROM account WHERE username='$userEsc'");
$check = $q->fetch_assoc();
if(!$check || $check['is_admin'] != 1){ echo "Không có quyền!"; exit; }

// Apply filters before pagination across the entire transaction history.
function tradeText($key) { return isset($_GET[$key]) && is_string($_GET[$key]) ? trim($_GET[$key]) : ''; }
function tradeEscape($value) { return htmlspecialchars($value, ENT_QUOTES, 'UTF-8'); }
function tradeDate($value) {
    $date=DateTimeImmutable::createFromFormat('!Y-m-d', $value);
    return $date && $date->format('Y-m-d')===$value && $date->format('Y')>='1000' && $date->format('Y')<='9998' ? $date : null;
}
$from=tradeText('from'); $to=tradeText('to');
$sender=mb_substr(tradeText('sender'),0,100); $recipient=mb_substr(tradeText('recipient'),0,100);
$fromDate=$from!==''?tradeDate($from):null; $toDate=$to!==''?tradeDate($to):null;
$filterError='';
if(($from!==''&&!$fromDate)||($to!==''&&!$toDate)) $filterError='Ngày lọc không hợp lệ. Vui lòng chọn lại ngày.';
elseif($fromDate&&$toDate&&$fromDate>$toDate) $filterError='Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.';
$where=[]; $params=[];
if($filterError) $where[]='1=0';
else {
    if($fromDate){$where[]='t.time_tran >= ?';$params[]=$fromDate->format('Y-m-d 00:00:00');}
    if($toDate){$where[]='t.time_tran < ?';$params[]=$toDate->modify('+1 day')->format('Y-m-d 00:00:00');}
    foreach(['player_1'=>$sender,'player_2'=>$recipient] as $column=>$name) if($name!==''){
        $where[]="INSTR(LOWER(SUBSTRING_INDEX(TRIM(t.$column), ' (', 1)), LOWER(?)) > 0"; $params[]=$name;
    }
}
$whereSql=$where?' WHERE '.implode(' AND ',$where):'';
function tradeQuery($sql) {
    global $conn,$params;
    $stmt=$conn->prepare($sql);
    if($params)$stmt->bind_param(str_repeat('s',count($params)),...$params);
    $stmt->execute(); return $stmt->get_result();
}
$item_per_page=500;
$total_transactions=(int)tradeQuery('SELECT COUNT(*) AS total FROM history_transaction t'.$whereSql)->fetch_assoc()['total'];
$total_pages=max(1,(int)ceil($total_transactions/$item_per_page));
$current_page=isset($_GET['page'])&&is_string($_GET['page'])?max(1,(int)$_GET['page']):1;
$current_page=min($current_page,$total_pages); $offset=($current_page-1)*$item_per_page;
$filterQuery=array_filter(['from'=>$from,'to'=>$to,'sender'=>$sender,'recipient'=>$recipient],static function($v){return $v!=='';});
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
        $whereSql ORDER BY t.time_tran DESC, t.id DESC LIMIT $item_per_page OFFSET $offset";
$list = tradeQuery($sql);
?>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Lịch Sử Giao Dịch</title>
    <link rel="icon" type="image/png" href="/img/icon.jpg">

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
        .trade-filters {display:flex;align-items:flex-end;flex-wrap:wrap;gap:12px;padding:16px;background:#f0f6ff;border:1px solid #dce6f3;border-radius:12px;margin:18px 0;}
        .trade-filters label {display:flex;flex-direction:column;gap:6px;font-size:14px;color:#627e9f;}
        .trade-filters input {min-width:170px;}
        .trade-filters a {padding:10px 14px;color:#6486ae;text-decoration:none;}
        .trade-filter-error {color:#b35461;padding:12px;background:#fff0f1;border-radius:8px;}
        .trade-empty {padding:30px;color:#8194ac;}
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

    <form id="tradeFilters" class="trade-filters" method="get" action="giaodich.php">
        <label>Từ ngày<input type="date" name="from" value="<?= tradeEscape($from) ?>" min="1000-01-01" max="9998-12-31"></label>
        <label>Đến ngày<input type="date" name="to" value="<?= tradeEscape($to) ?>" min="1000-01-01" max="9998-12-31"></label>
        <button type="submit">🔍 Lọc giao dịch</button>
        <a href="giaodich.php">Xóa bộ lọc</a>
    </form>
    <?php if($filterError): ?><p class="trade-filter-error" role="alert"><?= tradeEscape($filterError) ?></p><?php endif; ?>
    <div class="pagination">
        <?php for($i = 1; $i <= $total_pages; $i++): ?>
            <a href="?<?= tradeEscape(http_build_query(array_merge($filterQuery,['page'=>$i]))) ?>" class="<?= ($i == $current_page) ? 'active' : '' ?>"><?= $i ?></a>
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
                    <td><input type="text" id="searchP1" name="sender" form="tradeFilters" value="<?= tradeEscape($sender) ?>" class="search-input" placeholder="🔍 Lọc..." aria-label="Lọc người gửi"></td>
                    <td></td>
                    <td></td>
                    <td><input type="text" id="searchP2" name="recipient" form="tradeFilters" value="<?= tradeEscape($recipient) ?>" class="search-input" placeholder="🔍 Lọc..." aria-label="Lọc người nhận"></td>
                </tr>
            </thead>
            <tbody id="tranTableBody">
                <?php while($row = $list->fetch_assoc()): 
                    $avt1 = $row['avt1'] ?? 1; $avt2 = $row['avt2'] ?? 1;
                    $name1 = explode(" (", $row['player_1'])[0];
                    $name2 = explode(" (", $row['player_2'])[0];
                    $displayTime = str_replace(" ", "<br>", $row['time_tran']);
                ?>
                <tr class="tran-row" data-p1="<?= tradeEscape(mb_strtolower($name1)) ?>" data-p2="<?= tradeEscape(mb_strtolower($name2)) ?>">
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
                <?php if($total_transactions===0): ?><tr><td colspan="5" class="trade-empty">Không có giao dịch phù hợp.</td></tr><?php endif; ?>
            </tbody>
        </table>
    </div>

</div>


</body>
</html>