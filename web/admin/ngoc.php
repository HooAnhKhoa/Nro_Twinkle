<?php
session_start();
require_once "../config.php";

// ===== CHECK ADMIN =====
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
if (empty($_SESSION['shop_csrf'])) $_SESSION['shop_csrf'] = bin2hex(random_bytes(32));
$message_type = "";
$message_text = "";

// -- THÊM VẬT PHẨM --
if ($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_POST['submit_add_item'])) {
    $temp_id = intval($_POST['temp_id']);
    $cost = intval($_POST['cost']);
    $type_sell = intval($_POST['type_sell']);
    $tab_id = intval($_POST['tab_id']);
    $sort_order = intval($_POST['sort_order']); 

    $stmt = $conn->prepare("INSERT INTO item_shop (temp_id, cost, type_sell, tab_id, icon_spec, sort_order) VALUES (?, ?, ?, ?, 0, ?)");
    $stmt->bind_param("iiiii", $temp_id, $cost, $type_sell, $tab_id, $sort_order);
    
    if ($stmt->execute()) {
        $target_id = $conn->insert_id;
        if (isset($_POST['opt_id']) && is_array($_POST['opt_id'])) {
            foreach ($_POST['opt_id'] as $key => $opt_id) {
                $param = intval($_POST['opt_param'][$key]);
                if ($opt_id !== '') {
                    $stmt_opt = $conn->prepare("INSERT INTO item_shop_option (item_shop_id, option_id, param) VALUES (?, ?, ?)");
                    $stmt_opt->bind_param("iii", $target_id, $opt_id, $param);
                    $stmt_opt->execute();
                }
            }
        }
        $message_type = "success";
        $message_text = "Đã thêm vật phẩm mới thành công!";
    } else {
        $message_type = "error";
        $message_text = "Lỗi khi thêm vật phẩm!";
    }
}

// -- SỬA VẬT PHẨM --
if ($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_POST['submit_edit_full'])) {
    $id = intval($_POST['id']);
    $temp_id = intval($_POST['temp_id']);
    $cost = intval($_POST['cost']);
    $type_sell = intval($_POST['type_sell']);
    $sort_order = intval($_POST['sort_order']); 
    
    $stmt = $conn->prepare("UPDATE item_shop SET temp_id = ?, cost = ?, type_sell = ?, sort_order = ? WHERE id = ?");
    $stmt->bind_param("iiiii", $temp_id, $cost, $type_sell, $sort_order, $id);
    
    if ($stmt->execute()) {
        $conn->query("DELETE FROM item_shop_option WHERE item_shop_id = $id");
        if (isset($_POST['opt_id']) && is_array($_POST['opt_id'])) {
            foreach ($_POST['opt_id'] as $key => $opt_id) {
                $param = intval($_POST['opt_param'][$key]);
                if ($opt_id !== '') {
                    $stmt_opt = $conn->prepare("INSERT INTO item_shop_option (item_shop_id, option_id, param) VALUES (?, ?, ?)");
                    $stmt_opt->bind_param("iii", $id, $opt_id, $param);
                    $stmt_opt->execute();
                }
            }
        }
        $message_type = "success";
        $message_text = "Cập nhật dữ liệu thành công!";
    }
}

// -- XÓA VẬT PHẨM --
if (isset($_GET['delete_id'])) {
    $delete_id = intval($_GET['delete_id']);
    $stmt = $conn->prepare("DELETE FROM item_shop WHERE id = ?");
    $stmt->bind_param("i", $delete_id);
    if ($stmt->execute()) {
        $message_type = "success";
        $message_text = "Đã xóa vật phẩm khỏi shop!";
    }
}

// ===== 3. TRUY VẤN DỮ LIỆU =====
$shop_list_query = "SELECT DISTINCT s.id, s.npc_id, nt.NAME AS npc_name, nt.avatar FROM shop s JOIN tab_shop ts ON s.id = ts.shop_id JOIN item_shop ish ON ts.id = ish.tab_id JOIN npc_template nt ON s.npc_id = nt.id WHERE ish.icon_spec IN (0,1, 9, 160) ORDER BY s.id ASC";
$shop_list_result = $conn->query($shop_list_query);
$shop_nav = [];
while($row = $shop_list_result->fetch_assoc()) { $shop_nav[] = $row; }
$current_shop_id = isset($_GET['shop_id']) ? intval($_GET['shop_id']) : ($shop_nav[0]['id'] ?? 0);

$items_by_tab = [];
$first_tab_id = 0;
if ($current_shop_id > 0) {
    $sql = "SELECT ish.*, it.name AS item_name, it.icon_id, ts.NAME AS tab_name, nt.avatar AS npc_avatar, 
    (SELECT GROUP_CONCAT(CONCAT(iso.option_id, ':', iso.param) SEPARATOR '|') FROM item_shop_option iso WHERE iso.item_shop_id = ish.id) AS item_options_raw, 
    (SELECT GROUP_CONCAT(CONCAT(iot.name, ':', iso.param) SEPARATOR '|') FROM item_shop_option iso JOIN item_option_template iot ON iso.option_id = iot.id WHERE iso.item_shop_id = ish.id) AS item_options_text 
    FROM item_shop ish 
    JOIN item_template it ON ish.temp_id = it.id 
    JOIN tab_shop ts ON ish.tab_id = ts.id 
    JOIN shop s ON ts.shop_id = s.id 
    JOIN npc_template nt ON s.npc_id = nt.id 
    WHERE s.id = $current_shop_id AND ish.icon_spec IN (0,1, 9, 160) 
    ORDER BY ish.tab_id ASC, ish.sort_order ASC";
    $result = $conn->query($sql);
    while ($row = $result->fetch_assoc()) { 
        if($first_tab_id == 0) $first_tab_id = $row['tab_id'];
        $items_by_tab[$row['tab_id']][] = $row; 
    }
}

$opt_templates = $conn->query("SELECT id, name FROM item_option_template ORDER BY id ASC")->fetch_all(MYSQLI_ASSOC);
$all_items_list = $conn->query("SELECT id, NAME, icon_id FROM item_template ORDER BY id ASC")->fetch_all(MYSQLI_ASSOC);
?>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Shop Cơ Bản</title>
    <link rel="icon" type="image/png" href="/img/icon.jpg">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
    <script>const shopCsrf = <?php echo json_encode($_SESSION['shop_csrf']); ?>;</script>
    <script src="/admin/update_shop.js"></script>
    
    <style>
        :root { --primary: #4e73df; --bg: #f0f7ff; --text: #333; --green: #28a745; }
        body { margin: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; font-size: 17px; background: linear-gradient(135deg, #e0f2fe, #bae6fd); min-height: 100vh; }
        .wrapper { width: 90%; max-width: 1800px; margin: 30px auto; padding: 25px; background: #fff; border-radius: 15px; box-shadow: 0 10px 25px rgba(0,0,0,0.05); }
        .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; position: relative; }
        .header-left { display: flex; gap: 10px; align-items: center; z-index: 2; }
        .page-title { color: #4e73df; font-size: 28px; font-weight: 800; margin: 0; position: absolute; left: 50%; transform: translateX(-50%); white-space: nowrap; }
        .btn { border: none; padding: 10px 15px; border-radius: 8px; color: white; cursor: pointer; font-weight: bold; display: flex; align-items: center; gap: 8px; text-decoration: none; font-size: 14px; transition: 0.2s; white-space: nowrap; }
        .btn-back { background: #5a6268; } .btn-refresh { background: #007bff; }
        .shop-nav-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; margin: 0 auto 30px auto; border: 2px solid #888; padding: 15px; border-radius: 12px; background: #fcfcfc; width: 80%; }
        .shop-card { display: flex; align-items: center; padding: 8px; border: 1px solid #a1a1a1; border-radius: 10px; text-decoration: none; color: #444; transition: 0.3s; background: #fff; }
        .shop-card.active { border: 2px solid var(--primary); background: #f8faff; box-shadow: 0 0 8px rgba(78,115,223,0.2); }
        .npc-img { width: 40px; height: 40px; border-radius: 5px; margin-right: 8px; object-fit: contain; }
        .tab-bar { display: flex; gap: 15px; margin-bottom: 20px; border-bottom: 2px solid #eee; padding-bottom: 10px; overflow-x: auto; align-items: flex-end; position: relative; }
        .tab-link { padding: 8px 15px; background: #f8f9fc; border: 2px solid #666; border-radius: 12px 12px 0 0; cursor: pointer; text-align: center; transition: 0.3s; min-width: 80px; }
        .tab-link img { width: 40px; height: 40px; display: block; margin: 0 auto 5px; object-fit: contain; }
        .tab-link.active { background: #fff; border-color: var(--primary); border-bottom-color: #fff; }
        .tab-content { display: none; } .tab-content.active { display: block; }
        .item-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 15px; }
        .item-card { background: #fff; border-radius: 12px; border: 1px solid #b1b9d1; padding: 15px; position: relative; transition: 0.3s; display: flex; flex-direction: column; justify-content: space-between; }
        
        /* CẬP NHẬT: Ẩn nút mặc định, hiện khi hover */
        .btn-delete-item { position: absolute; top: 8px; right: 8px; background: #ff4d4d; color: white; border: none; width: 29px; height: 29px; border-radius: 50%; font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; z-index: 10; opacity: 0; transform: scale(0.5); transition: 0.2s; }
        .btn-update-icon { position: absolute; top: 42px; right: 8px; background: #4e73df; color: white; border: none; width: 29px; height: 29px; border-radius: 50%; font-size: 15px; cursor: pointer; display: flex; align-items: center; justify-content: center; z-index: 10; opacity: 0; transform: scale(0.5); transition: 0.2s; }
        .item-card:hover .btn-delete-item, .item-card:hover .btn-update-icon { opacity: 1; transform: scale(1); }

        .item-stt { position: absolute; top: 8px; left: 8px; background: #6c757d; color: white; padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: bold; opacity: 0.8; }
        .item-name { font-weight: bold; font-size: 16px; color: #333; margin-right: 35px; }
        .price-text { font-weight: bold; color: #d68a00; font-size: 17px; margin-top: 5px; }
        .item-stats { margin-top: 10px; color: #dc3545; font-size: 16px; font-weight: 500; min-height: 50px; line-height: 1.3; }
        .modal { display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); }
        .modal-content { background: #fff; margin: 2% auto; width: 950px; border-radius: 20px; box-shadow: 0 20px 40px rgba(0,0,0,0.2); overflow: hidden; position: relative; animation: slideDown 0.3s ease; }
        @keyframes slideDown { from { transform: translateY(-50px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
        .modal-header { background: var(--primary); color: #fff; padding: 20px; font-weight: 800; font-size: 20px; display: flex; justify-content: space-between; align-items: center; }
        .modal-header.add-mode { background: var(--green); }
        .modal-body { display: flex; gap: 25px; padding: 25px; max-height: 75vh; overflow-y: auto; }
        .modal-left { flex: 1.5; border-right: 1px solid #eee; padding-right: 20px; }
        .modal-right { flex: 1; position: sticky; top: 0; height: fit-content; }
        .form-group { margin-bottom: 18px; }
        .form-group label { display: block; font-size: 14px; margin-bottom: 8px; font-weight: 700; color: #555; text-transform: uppercase; }
        .form-control { width: 100%; padding: 12px; border: 2px solid #e0e0e0; border-radius: 10px; box-sizing: border-box; font-size: 15px; transition: 0.2s; }
        .form-control:focus { border-color: var(--primary); outline: none; background: #f8faff; }
        .option-section { background: #f9fafb; border: 2px dashed #d1d5db; border-radius: 15px; padding: 15px; margin-top: 10px; }
        .opt-row { display: flex; gap: 8px; margin-bottom: 10px; align-items: center; }
        .btn-add-opt { background: #fff; color: var(--primary); border: 2px solid var(--primary); padding: 8px 15px; border-radius: 8px; cursor: pointer; font-weight: 800; font-size: 13px; width: 100%; margin-top: 5px; }
        .btn-remove-opt { background: #ffeded; color: #ff4d4d; border: none; width: 38px; height: 38px; border-radius: 8px; cursor: pointer; font-size: 18px; display: flex; align-items: center; justify-content: center; }
        .modal-footer { padding: 20px; background: #f3f4f6; display: flex; gap: 10px; }
        .btn-save { flex: 2; background: #28a745; border: none; padding: 15px; color: #fff; font-weight: 800; border-radius: 12px; cursor: pointer; font-size: 16px; }
        .btn-cancel { flex: 1; background: #6c757d; border: none; padding: 15px; color: #fff; font-weight: 800; border-radius: 12px; cursor: pointer; }
        .demo-box { background: #fff; border: 2px solid #4e73df; border-radius: 15px; padding: 20px; box-shadow: 0 5px 15px rgba(78,115,223,0.1); }
        .demo-title { font-size: 12px; font-weight: 800; color: #4e73df; margin-bottom: 15px; text-align: center; border-bottom: 1px solid #eee; padding-bottom: 5px; }
        .demo-header { display: flex; align-items: center; gap: 15px; margin-bottom: 15px; }
        .demo-icon { width: 60px; height: 60px; background: #f3f4f6; border-radius: 12px; display: flex; align-items: center; justify-content: center; border: 1px solid #ddd; }
        .demo-name { font-weight: 800; font-size: 19px; color: #333; }
        .demo-cost { color: #d68a00; font-weight: 800; font-size: 18px; }
        .demo-stats { border-top: 1px dashed #ccc; padding-top: 15px; color: #dc3545; font-size: 15px; font-weight: 600; line-height: 1.6; }
        .select-item-modal { display: none; position: fixed; z-index: 2000; inset: 0; background: rgba(0,0,0,0.8); align-items: center; justify-content: center; }
        .select-item-content { background: #f1f5f9; width: 900px; height: 85vh; border-radius: 20px; display: flex; flex-direction: column; overflow: hidden; }
        .select-item-header { padding: 15px 20px; background: #fff; border-bottom: 2px solid #e2e8f0; display: flex; gap: 15px; align-items: center; }
        .select-item-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 12px; padding: 20px; overflow-y: auto; flex: 1; }
        .pick-card { background: #fff; border-radius: 10px; padding: 12px; text-align: center; cursor: pointer; border: 1px solid #ddd; transition: 0.2s; }
        .pick-card:hover { transform: translateY(-3px); box-shadow: 0 5px 15px rgba(0,0,0,0.1); border-color: #3b82f6; }
        .pick-card img { width: 40px; height: 40px; object-fit: contain; }
        .shop-actions { position: absolute; right: 0; top: 10px; display: flex; gap: 10px; }
        .add-item-header { border: 2px solid var(--green); background-color: #f0fff0; color: var(--green); padding: 12px 20px; border-radius: 12px; font-weight: 800; cursor: pointer; transition: all 0.3s; display: flex; align-items: center; gap: 10px; font-size: 16px; }
        .update-shop-header { border: 2px solid #2563eb; background: #eff6ff; color: #1d4ed8; padding: 12px 20px; border-radius: 12px; font-weight: 800; cursor: pointer; font-size: 16px; }
        .update-shop-header:disabled { opacity: .6; cursor: wait; }
        .add-item-header:hover { background-color: var(--green); color: white; box-shadow: 0 4px 8px rgba(40, 167, 69, 0.3); }
    </style>
<link rel="stylesheet" href="/typography.css?v=20261014-1"><link rel="stylesheet" href="admin-ui.css?v=20261015-4"><script src="admin-ui.js?v=20261015-2" defer></script><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body class="admin-page" data-admin-page="ngoc"><?php require_once __DIR__.'/admin_ui.php'; adminPageToolbar('Cửa hàng ngọc'); ?>

<div class="wrapper">
    <div class="header-row">
        <div class="header-left">
            <a href="/admin/itemshop.php" class="btn btn-back"><i class="fa-solid fa-arrow-left"></i> QUAY LẠI</a>
            <a href="?shop_id=<?php echo $current_shop_id; ?>" class="btn btn-refresh"><i class="fa-solid fa-sync"></i> LÀM MỚI</a>
        </div>
        <h2 class="page-title">Shop Vàng _ Ngọc</h2>
    </div>

    <div class="shop-nav-grid">
        <?php foreach ($shop_nav as $s): ?>
            <a href="?shop_id=<?php echo $s['id']; ?>" class="shop-card <?php echo ($current_shop_id == $s['id']) ? 'active' : ''; ?>">
                <img src="/icon/<?php echo $s['avatar']; ?>.png" class="npc-img" onerror="this.src='/icon/0.png';">
                <div class="npc-info">
                    <span style="font-size: 11px;">ID: <?php echo $s['npc_id']; ?></span>
                    <b style="display:block; font-size:14px;"><?php echo $s['npc_name']; ?></b>
                </div>
            </a>
        <?php endforeach; ?>
    </div>

    <?php if (!empty($items_by_tab)): ?>
        <div class="tab-bar">
            <?php $first_tab = true; foreach ($items_by_tab as $tab_id => $items): ?>
                <div class="tab-link <?php echo $first_tab ? 'active' : ''; ?>" onclick="openTab(event, 'tab-<?php echo $tab_id; ?>')">
                    <img src="/icon/<?php echo $items[0]['npc_avatar']; ?>.png" onerror="this.src='/icon/0.png';">
                    <span><?php echo str_replace('<>', ' ', $items[0]['tab_name']); ?></span>
                </div>
            <?php $first_tab = false; endforeach; ?>
            <div class="shop-actions"><button class="add-item-header" onclick="openAddModal()">
                <i class="fa-solid fa-plus"></i> Thêm vật phẩm
            </button>
            <button class="update-shop-header" onclick="updateShop(this)"><i class="fa-solid fa-rotate"></i> Update Shop</button></div>
        </div>

        <?php $first_content = true; foreach ($items_by_tab as $tab_id => $items): ?>
            <div id="tab-<?php echo $tab_id; ?>" class="tab-content <?php echo $first_content ? 'active' : ''; ?>">
                <div class="item-grid">
                    <?php foreach ($items as $item): ?>
                        <div class="item-card">
                            <div class="item-stt">STT: <?php echo $item['sort_order']; ?></div>
                            <button type="button" class="btn-delete-item" onclick="confirmDelete(<?php echo $item['id']; ?>, '<?php echo $item['item_name']; ?>')">
                                <i class="fa-solid fa-xmark"></i>
                            </button>
                            <button type="button" class="btn-update-icon" onclick='openEditModal(<?php echo json_encode($item); ?>)'>
                                <i class="fa-solid fa-wrench"></i>
                            </button>

                            <div class="item-main" style="padding-top: 15px;">
                                <div class="item-icon-box" style="float:left; margin-right:10px;">
                                    <img src="/icon/<?php echo $item['icon_id']; ?>.png" width="45" height="45" onerror="this.src='/icon/0.png';">
                                </div>
                                <div class="item-details">
                                    <span class="item-name"><?php echo $item['item_name']; ?></span>
                                    <div class="price-text"><?php echo number_format($item['cost']); ?> <img src="/icon/<?php echo ($item['type_sell'] == 0) ? '930.png' : '932.png'; ?>" width="20"></div>
                                    <div style="font-size: 12px; color:#888;">ID: <?php echo $item['temp_id']; ?></div>
                                </div>
                            </div>
                            <div class="item-stats">
                                <?php 
                                if (!empty($item['item_options_text'])) {
                                    $options = explode('|', $item['item_options_text']);
                                    foreach (array_slice($options, 0, 4) as $op) {
                                        $parts = explode(':', $op);
                                        $display = str_replace(['%d', '%1', '#'], $parts[1] ?? '', $parts[0]);
                                        if ($display == $parts[0]) $display .= " " . ($parts[1] ?? '');
                                        echo '<div>• ' . $display . '</div>';
                                    }
                                } else { echo '<div style="color:#999; font-size:13px;">Trống chỉ số</div>'; }
                                ?>
                            </div>
                        </div>
                    <?php endforeach; ?>
                </div>
            </div>
        <?php $first_content = false; endforeach; ?>
    <?php endif; ?>
</div>

<div id="editModal" class="modal">
    <div class="modal-content">
        <div id="modal_header" class="modal-header">
            <span id="modal_title"><i class="fa-solid fa-wrench"></i> CHỈNH SỬA VẬT PHẨM</span>
            <i class="fa-solid fa-xmark" onclick="closeModal()" style="cursor:pointer;"></i>
        </div>
        <form method="POST">
            <div class="modal-body">
                <div class="modal-left">
                    <input type="hidden" name="id" id="edit_id">
                    <input type="hidden" name="tab_id" id="edit_tab_id"> 
                    
                    <div class="form-group">
                        <label>Vật phẩm (Template ID)</label>
                        <div style="display: flex; gap: 8px;">
                            <input type="number" name="temp_id" id="edit_temp_id" class="form-control" readonly required style="flex:1">
                            <button type="button" class="btn" style="background:#8b5cf6;" onclick="openPicker('item')"><i class="fa-solid fa-search"></i> CHỌN</button>
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px;">
                        <div class="form-group">
                            <label>Giá bán</label>
                            <input type="number" name="cost" id="edit_cost" class="form-control" required oninput="updateDemo()">
                        </div>
                        <div class="form-group">
                            <label>Loại tiền</label>
                            <select name="type_sell" id="edit_type_sell" class="form-control" onchange="updateDemo()">
                                <option value="0">Vàng</option>
                                <option value="1">Ngọc</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Số thứ tự</label>
                            <input type="number" name="sort_order" id="edit_sort_order" class="form-control" value="0" required>
                        </div>
                    </div>

                    <label style="font-weight: 800; color: #4e73df; font-size: 13px;">DANH SÁCH CHỈ SỐ</label>
                    <div class="option-section">
                        <div id="option_container"></div>
                        <button type="button" class="btn-add-opt" onclick="addOptionRow()">
                            <i class="fa-solid fa-plus-circle"></i> THÊM CHỈ SỐ
                        </button>
                    </div>
                </div>

                <div class="modal-right">
                    <div class="demo-box">
                        <div class="demo-title">BẢN XEM TRƯỚC (DEMO)</div>
                        <div class="demo-header">
                            <div class="demo-icon"><img id="demo_img" src="/icon/0.png" width="45"></div>
                            <div class="demo-info">
                                <div id="demo_name" class="demo-name">Tên hiển thị</div>
                                <div class="demo-cost">
                                    <span id="demo_cost">0</span> <img id="demo_type_icon" src="/icon/930.png" width="18">
                                </div>
                            </div>
                        </div>
                        <div id="demo_stats" class="demo-stats"></div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-cancel" onclick="closeModal()">HỦY</button>
                <button type="submit" name="submit_edit_full" id="btn_save" class="btn-save">LƯU DỮ LIỆU</button>
            </div>
        </form>
    </div>
</div>

<div id="pickerModal" class="select-item-modal">
    <div class="select-item-content">
        <div class="select-item-header">
            <input type="text" id="pickerSearch" class="form-control" placeholder="Tìm kiếm..." onkeyup="filterPicker()">
            <button class="btn btn-back" onclick="closePicker()">ĐÓNG</button>
        </div>
        <div class="select-item-grid" id="pickerGrid"></div>
    </div>
</div>

<script>
const allItems = <?php echo json_encode($all_items_list); ?>;
const allOptions = <?php echo json_encode($opt_templates); ?>;
const firstTabId = <?php echo intval($first_tab_id); ?>;
let activePickerType = ''; 
let activeOptionTarget = null;

const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true
});

<?php if($message_type != ""): ?>
    Toast.fire({
      icon: '<?php echo $message_type; ?>',
      title: '<?php echo $message_text; ?>'
    });
<?php endif; ?>

function removeTones(str) { return str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase(); }

function updateDemo() {
    const tid = document.getElementById('edit_temp_id').value;
    const item = allItems.find(i => i.id == tid);
    if(item) {
        document.getElementById('demo_name').innerText = item.NAME;
        document.getElementById('demo_img').src = '/icon/' + item.icon_id + '.png';
    }
    document.getElementById('demo_cost').innerText = new Intl.NumberFormat().format(document.getElementById('edit_cost').value);
    document.getElementById('demo_type_icon').src = (document.getElementById('edit_type_sell').value == 0) ? '/icon/930.png' : '/icon/932.png';
    const demoStats = document.getElementById('demo_stats');
    demoStats.innerHTML = "";
    document.querySelectorAll('#option_container .opt-row').forEach(row => {
        const optId = row.querySelector('input[name="opt_id[]"]').value;
        const param = row.querySelector('input[name="opt_param[]"]').value;
        const optTemplate = allOptions.find(o => o.id == optId);
        if (optTemplate && optId !== '') {
            let display = optTemplate.name.replace(/%d|%1|#/g, param || '0');
            if (display === optTemplate.name) display += " " + (param || '0');
            demoStats.innerHTML += "<div>• " + display + "</div>";
        }
    });
}

function openTab(evt, tabId) {
    document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));
    document.querySelectorAll(".tab-link").forEach(l => l.classList.remove("active"));
    document.getElementById(tabId).classList.add("active");
    if(evt) evt.currentTarget.classList.add("active");
}

function openPicker(type, target = null) {
    activePickerType = type; activeOptionTarget = target;
    const grid = document.getElementById('pickerGrid'); grid.innerHTML = '';
    if (type === 'item') {
        allItems.forEach(it => {
            const div = document.createElement('div'); div.className = 'pick-card';
            div.dataset.search = removeTones(it.NAME) + ' ' + it.id;
            div.onclick = () => { document.getElementById("edit_temp_id").value = it.id; updateDemo(); closePicker(); };
            div.innerHTML = `<img src="/icon/${it.icon_id}.png" onerror="this.src='/icon/0.png'"><div style="font-size:12px;">${it.NAME}</div>`;
            grid.appendChild(div);
        });
        

} else {
    const redIds = [0, 5, 6, 7, 14, 30, 50, 77, 103, 95, 96, 101, 106]; // Danh sách ID cần tô đỏ
    allOptions.forEach(opt => {
        const div = document.createElement('div'); 
        div.className = 'pick-card';
        div.dataset.search = removeTones(opt.name) + ' ' + opt.id;
        
        // Kiểm tra nếu ID nằm trong danh sách thì gán màu đỏ
        const isRed = redIds.includes(parseInt(opt.id));
        const redStyle = isRed ? 'style="color: #ff4d4d; font-weight: bold; border-color: #ff4d4d;"' : '';

        div.onclick = () => { 
            const row = activeOptionTarget.parentElement;
            row.querySelector('input[name="opt_id[]"]').value = opt.id;
            row.querySelector('.opt-name-display').value = `[${opt.id}] ${opt.name}`;
            updateDemo(); closePicker(); 
        };
        
        // Áp dụng style vào nội dung hiển thị
        div.innerHTML = `
            <div ${redStyle}>ID: ${opt.id}</div>
            <div style="font-size:11px; ${isRed ? 'color: #ff4d4d;' : ''}">${opt.name}</div>
        `;
        grid.appendChild(div);
    });
}
    document.getElementById('pickerModal').style.display = 'flex';
}

function filterPicker() {
    const val = removeTones(document.getElementById('pickerSearch').value);
    document.querySelectorAll('.pick-card').forEach(card => card.style.display = card.dataset.search.includes(val) ? 'block' : 'none');
}

function closePicker() { document.getElementById('pickerModal').style.display = 'none'; }

function addOptionRow(optId = "", param = "") {
    const container = document.getElementById("option_container");
    const div = document.createElement("div"); div.className = "opt-row";
    let optName = optId !== "" ? `[${optId}] ` + (allOptions.find(o => o.id == optId)?.name || "") : "-- Trống --";
    div.innerHTML = `
        <input type="hidden" name="opt_id[]" value="${optId}">
        <input type="text" class="form-control opt-name-display" readonly value="${optName}" style="flex: 2.5; background: #eee; cursor: pointer; font-size:13px;" onclick="openPicker('option', this)">
        <input type="number" name="opt_param[]" class="form-control" value="${param}" style="flex: 1;" oninput="updateDemo()">
        <button type="button" class="btn-remove-opt" onclick="this.parentElement.remove(); updateDemo();"><i class="fa-solid fa-xmark"></i></button>`;
    container.appendChild(div); updateDemo();
}

function openAddModal() {
    document.getElementById("modal_header").classList.add("add-mode");
    document.getElementById("modal_title").innerHTML = '<i class="fa-solid fa-plus"></i> THÊM VẬT PHẨM MỚI';
    document.getElementById("edit_id").value = "";
    document.getElementById("edit_temp_id").value = "";
    document.getElementById("edit_cost").value = "";
    document.getElementById("edit_type_sell").value = "0";
    document.getElementById("edit_sort_order").value = "0";
    document.getElementById("btn_save").name = "submit_add_item";
    document.getElementById("btn_save").innerText = "THÊM VẬT PHẨM";
    
    let activeTabId = firstTabId;
    const currentTabLink = document.querySelector(".tab-link.active");
    if (currentTabLink) activeTabId = currentTabLink.getAttribute('onclick').match(/'tab-(\d+)'/)[1];
    document.getElementById("edit_tab_id").value = activeTabId;
    
    document.getElementById("option_container").innerHTML = "";
    updateDemo();
    document.getElementById("editModal").style.display = "block";
}

function openEditModal(item) {
    document.getElementById("modal_header").classList.remove("add-mode");
    document.getElementById("modal_title").innerHTML = '<i class="fa-solid fa-wrench"></i> CHỈNH SỬA VẬT PHẨM';
    document.getElementById("edit_id").value = item.id;
    document.getElementById("edit_temp_id").value = item.temp_id;
    document.getElementById("edit_cost").value = item.cost;
    document.getElementById("edit_type_sell").value = item.type_sell;
    document.getElementById("edit_sort_order").value = item.sort_order;
    document.getElementById("btn_save").name = "submit_edit_full";
    document.getElementById("btn_save").innerText = "LƯU DỮ LIỆU";
    
    const container = document.getElementById("option_container");
    container.innerHTML = "";
    if (item.item_options_raw) {
        item.item_options_raw.split('|').forEach(opt => {
            const p = opt.split(':');
            addOptionRow(p[0], p[1]);
        });
    } else { addOptionRow(); }
    updateDemo();
    document.getElementById("editModal").style.display = "block";
}

function closeModal() { document.getElementById("editModal").style.display = "none"; }

function confirmDelete(id, name) {
    Swal.fire({
      title: 'Xác nhận xóa?',
      text: `Xóa [${name}]?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#ff4d4d',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Xóa Ngay',
      cancelButtonText: 'Hủy'
    }).then((result) => {
      if (result.isConfirmed) {
        window.location.href = `?shop_id=<?php echo $current_shop_id; ?>&delete_id=${id}`;
      }
    })
}

window.onclick = e => {
    if (e.target.classList.contains('modal')) closeModal();
    if (e.target.id === 'pickerModal') closePicker();
};
</script>
</body>
</html>
