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

// 2. Xử lý lưu dữ liệu AJAX (POST)
if($_SERVER['REQUEST_METHOD'] === 'POST'){
    $data = json_decode(file_get_contents("php://input"), true);
    if($data){
        $id = (int)$data['id'];
        $name = $conn->real_escape_string($data['name']);
        $zones = (int)$data['zones'];
        $max_player = (int)$data['max_player'];
        $mobs = !empty($data['mobs']) ? $conn->real_escape_string($data['mobs']) : '[]';
        $npcs = !empty($data['npcs']) ? $conn->real_escape_string($data['npcs']) : '[]';

        $sql = "UPDATE map_template SET 
                NAME='$name', zones=$zones, max_player=$max_player, 
                mobs='$mobs', npcs='$npcs' 
                WHERE id=$id";

        if($conn->query($sql)) echo json_encode(["status"=>"success"]);
        else echo json_encode(["status"=>"error", "msg" => $conn->error]);
        exit;
    }
}

// 3. Load Data bổ trợ
$npcAvatars = [];
$npcNames = [];
$npcFullList = []; // Dùng cho bảng chọn
$getNpc = $conn->query("SELECT id, NAME, avatar FROM npc_template");
while($n = $getNpc->fetch_assoc()) {
    $npcAvatars[$n['id']] = $n['avatar'];
    $npcNames[$n['id']] = $n['NAME'];
    $npcFullList[] = $n;
}

$mobNames = [];
$mobFullList = []; // Dùng cho bảng chọn
$getMobNames = $conn->query("SELECT id, NAME FROM mob_template");
while($m = $getMobNames->fetch_assoc()) {
    $mobNames[$m['id']] = $m['NAME'];
    $mobFullList[] = $m;
}

$list = $conn->query("SELECT id, NAME, zones, max_player, mobs, npcs FROM map_template ORDER BY id ASC");
?>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản Lý Maps</title>
    <link rel="icon" type="image/png" href="/img/icon.jpg">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        html, body { margin: 0; padding: 0; width: 100%; overflow-x: hidden; }
        body { font-family: 'Segoe UI', sans-serif; background: linear-gradient(135deg,#e0f2fe,#bae6fd); color: #1e293b; font-size: 1.1em;}
        .container { max-width: 1300px; margin: auto; padding: 15px; box-sizing: border-box; }
        .header-box { display: flex; justify-content: space-between; align-items: center; background: white; padding: 20px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,.15); margin-bottom: 20px; }
        .header-left { display: flex; align-items: center; gap: 20px; }
        .header-left img { width: 200px; height: 200px; object-fit: contain; }
        .header-content h1 { margin: 0; color: #1e3a8a; font-size: 2.5rem; }
        .menu-buttons button, .action-box button, .btn-mobs { position: relative; overflow: hidden; transition: all 0.2s ease; cursor:pointer; border:none; border-radius:10px; color:white; font-weight:600; }
        .menu-buttons button:hover, .action-box button:hover, .btn-mobs:hover { transform: translateY(-2px) scale(1.05); box-shadow: 0 8px 20px rgba(0,0,0,0.25); }
        .menu-buttons button:active, .action-box button:active { transform: scale(0.95); box-shadow: 0 3px 10px rgba(0,0,0,0.2); }
        .menu-buttons { display: flex; gap: 10px; margin-top: 10px; }
        .home { background: #3b82f6; padding: 8px 15px; font-size: 1.2em; } 
        .refresh { background: #22c55e; padding: 8px 15px; font-size: 1.2em; }
        .action-box { display: flex; justify-content: center; margin-bottom: 20px; padding: 15px; background: white; border-radius: 12px; box-shadow: 0 5px 15px rgba(0,0,0,.1); }
        .save-all { background: #16a34a; padding: 10px 30px; font-size: 1.2em; font-weight: bold; }
        .table-wrap { width: 100%; background: white; border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.15); overflow: hidden; }
        table { width: 100%; border-collapse: collapse; table-layout: fixed; }
        th { background: #2563eb; color: white; padding: 15px 5px; font-size: 1rem; }
        td { padding: 10px 5px; text-align: center; border-bottom: 1px solid #e2e8f0; vertical-align: middle; }
        .id-cell { background: #93c5fd; font-weight: bold; color: #1e3a8a; }
        .changed { background: #93c5fd !important; box-shadow: inset 0 0 5px rgba(0,0,0,0.1); border-radius: 4px; }
        input.data { border: none; background: transparent; text-align: center; font-weight: bold; width: 100%; font-size: 1.1em; outline: none; color: #1e3a8a; }
        .search-row input { width: 95%; padding: 8px; border-radius: 8px; border: 1px solid #93c5fd; outline: none; box-sizing: border-box; }
        .npc-container { display: flex; flex-wrap: wrap; gap: 4px; justify-content: center; margin-top: 8px; cursor: pointer; }
        .npc-item { width: 38px; height: 38px; background: #f1f5f9; border: 1px solid #cbd5e1; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
        .npc-item img { max-width: 32px; max-height: 32px; }
        .popup-bg { position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); display:none; align-items:center; justify-content:center; z-index:11000; }
        .popup-box { width:90%; max-width:500px; padding:42px 36px; border-radius:18px; text-align:center; background:linear-gradient(145deg,#f8fafc,#e2e8f0); color:#1e293b; box-shadow: 0 20px 60px rgba(0,0,0,.4); animation:pop .25s ease; position:relative; }
        .popup-icon { font-size:84px; }
        .popup-title { font-size:40px; margin:10px 0; font-weight: bold; }
        .popup-content { font-size:32px; }
        /* Style cho nhóm nút xác nhận xóa */
.confirm-group { display: flex; justify-content: center; gap: 20px; margin-top: 25px; }
.confirm-btn { padding: 12px 30px; border-radius: 12px; border: none; font-size: 1.2rem; font-weight: bold; cursor: pointer; transition: 0.2s; color: white; }
.confirm-yes { background: #ef4444; box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3); }
.confirm-yes:hover { background: #dc2626; transform: scale(1.05); }
.confirm-no { background: #64748b; box-shadow: 0 4px 12px rgba(100, 116, 139, 0.3); }
.confirm-no:hover { background: #475569; transform: scale(1.05); }
        @keyframes pop { from{transform:scale(.8);opacity:0} to{transform:scale(1);opacity:1} }
        .btn-mobs { background: #2563eb; padding: 8px 16px; font-size: 0.9em; width: 100%; max-width: 120px; }
        
        .close-btn { 
            position: absolute; 
            top: 15px; 
            right: 15px; 
            font-size: 24px; 
            font-weight: 900; 
            width: 45px; 
            height: 45px; 
            display: flex; 
            align-items: center; 
            justify-content: center; 
            border-radius: 50%; 
            background: #ef4444; 
            color: white; 
            cursor: pointer; 
            transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); 
            border: none; 
            box-shadow: 0 5px 15px rgba(239, 68, 68, 0.3); 
            z-index: 10005; 
        }
        .close-btn:hover { transform: scale(1.1) rotate(90deg); background: #dc2626; }
        
        .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.8); z-index: 9999; align-items: center; justify-content: center; backdrop-filter: blur(5px); }
        .modal-content { background: #ffffff; width: 95%; max-width: 900px; border-radius: 25px; padding: 35px; max-height: 85vh; overflow: visible; position: relative; display: flex; flex-direction: column; }
        .modal-content h2 { margin-top: 0; color: #1e3a8a; font-size: 28px; border-bottom: 3px solid #3b82f6; padding-bottom: 15px; margin-bottom: 20px; }
        .modal-table-body input { border: 1.5px solid #e2e8f0; border-radius: 8px; padding: 8px; text-align: center; width: 85%; font-weight: 600; color: #1e3a8a; }
        .name-label { display: inline-block; font-size: 12px; color: #4f46e5; font-weight: 800; margin-top: 5px; background: #eef2ff; padding: 3px 10px; border-radius: 15px; border: 1px solid #dbeafe; cursor: pointer; }
        .btn-add-row { width:100%; padding:15px; margin-top:20px; background:#f0fdf4; color:#16a34a; border:2px dashed #16a34a; font-weight:bold; cursor:pointer; border-radius:12px; }
        .btn-confirm-save { width:100%; padding:18px; margin-top:12px; background:#2563eb; color:white; border:none; border-radius:15px; font-weight:bold; cursor:pointer; font-size:1.1em; }

        /* Style mới cho Selection Modal */
        .selection-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 15px; overflow-y: auto; padding: 10px; background: #f8fafc; border-radius: 15px; margin-top: 15px; }
        .select-card { background: white; border: 2px solid #e2e8f0; padding: 12px; border-radius: 16px; text-align: center; cursor: pointer; transition: 0.2s; }
        .select-card:hover { border-color: #3b82f6; background: #eff6ff; transform: translateY(-3px); }
        .select-card img { width: 50px; height: 50px; object-fit: contain; margin-bottom: 8px; }
        .select-card div { font-weight: bold; font-size: 13px; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .select-card small { color: #64748b; }
    </style>
<link rel="stylesheet" href="/typography.css?v=20261014-1"><link rel="stylesheet" href="admin-ui.css?v=20261015-5"><script src="admin-ui.js?v=20261015-3" defer></script><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body class="admin-page" data-admin-page="map"><?php require_once __DIR__.'/admin_ui.php'; adminPageToolbar('Bản đồ'); ?>

<div class="container">
    <div class="header-box">
        <div class="header-left">
            <img src="/img/account.gif"> 
            <div class="header-content">
                <h1>Bản đồ</h1>
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

    <div class="action-box">
        <button class="save-all" onclick="saveAll()">💾 LƯU DỮ LIỆU</button>
    </div>

    <div class="table-wrap">
        <table id="mapsTable">
            <thead>
                <tr>
                    <th style="width: 40px;">ID</th>
                    <th style="width: 22%;">Tên Map</th>
                    <th style="width: 80px;">Số Khu</th>
                    <th style="width: 100px;">Số Người</th>
                    <th style="width: 350px;">Danh Sách Quái</th>
                    <th>Danh Sách NPC</th>
                </tr>
                <tr class="search-row">
                    <td><input type="text" placeholder="ID.."></td>
                    <td><input type="text" placeholder="Tên.."></td>
                    <td><input type="text" placeholder="Max.."></td>
                    <td><input type="text" placeholder="Max.."></td>
                    <td></td><td></td>
                </tr>
            </thead>
            <tbody>
                <?php while($row = $list->fetch_assoc()): ?>
                <tr id="row_<?= $row['id'] ?>" data-id="<?= $row['id'] ?>">
                    <td class="id-cell"><?= $row['id'] ?></td>
                    <td><input class="data track" id="n<?= $row['id'] ?>" value="<?= htmlspecialchars($row['NAME']) ?>"></td>
                    <td><input class="data track" id="z<?= $row['id'] ?>" value="<?= $row['zones'] ?>"></td>
                    <td><input class="data track" id="m<?= $row['id'] ?>" value="<?= $row['max_player'] ?>"></td>
                    <td>
                        <button class="btn-mobs" id="btn_mob_<?= $row['id'] ?>" onclick="editMobs(<?= $row['id'] ?>)">👾 Quái</button>
                        <input type="hidden" class="track" id="mb<?= $row['id'] ?>" value="<?= htmlspecialchars($row['mobs']) ?>">
                    </td>
                    <td>
                        <button class="btn-mobs" style="background:#8b5cf6;" id="btn_npc_<?= $row['id'] ?>" onclick="editNpcs(<?= $row['id'] ?>)">👤 NPC</button>
                        <input type="hidden" class="track" id="np<?= $row['id'] ?>" value="<?= htmlspecialchars($row['npcs']) ?>">
                        <div class="npc-container" id="preview_<?= $row['id'] ?>" onclick="editNpcs(<?= $row['id'] ?>)">
                            <?php 
                            $npcData = json_decode($row['npcs'], true);
                            if(is_array($npcData)){
                                foreach($npcData as $npcItem){
                                    $nid = (int)$npcItem[0];
                                    $img = in_array($nid, [3,4,6]) ? [3=>265, 4=>84, 6=>545][$nid] : ($npcAvatars[$nid] ?? 0);
                                    echo "<div class='npc-item'><img src='/icon/$img.png' onerror=\"this.src='/icon/0.png'\"></div>";
                                }
                            }
                            ?>
                        </div>
                    </td>
                </tr>
                <?php endwhile; ?>
            </tbody>
        </table>
    </div>
</div>

<div id="selectionModal" class="modal" style="z-index: 10500;">
    <div class="modal-content" style="max-width: 700px;">
        <button class="close-btn" onclick="$('#selectionModal').hide()">✖</button>
        <h2 id="selectionTitle">Chọn Quái</h2>
        <input type="text" id="selectionSearch" placeholder="Tìm tên hoặc ID..." style="width:100%; padding:12px; border-radius:10px; border:1px solid #ddd; font-size:16px;">
        <div id="selectionList" class="selection-grid"></div>
    </div>
</div>

<div id="savePopup" class="popup-bg">
    <div class="popup-box">
        <button class="close-btn" onclick="closePopup()">✖</button>
        <div id="savePopupContent"></div>
    </div>
</div>

<div id="mobModal" class="modal">
    <div class="modal-content">
        <button class="close-btn" onclick="closeMobModal()">✖</button>
        <h2>Sửa Quái Vật</h2>
        <input type="hidden" id="editingMapId">
        <div style="flex-grow: 1; overflow-y: auto; margin-top:10px;">
            <table style="width:100%;">
                <thead style="position: sticky; top: 0; background:#2563eb; color:white; z-index:10;">
                    <tr><th>Chọn Quái</th><th>X TNSM</th><th>HP</th><th>Tọa độ X</th><th>Tọa độ Y</th><th>Xóa</th></tr>
                </thead>
                <tbody id="mobEditBody" class="modal-table-body"></tbody>
            </table>
        </div>
        <button class="btn-add-row" onclick="addNewMobRow()">+ Thêm Quái</button>
        <button class="btn-confirm-save" onclick="confirmMobEdit()">XÁC NHẬN LƯU TẠM</button>
    </div>
</div>

<div id="npcModal" class="modal">
    <div class="modal-content">
        <button class="close-btn" onclick="closeNpcModal()">✖</button>
        <h2 style="color:#8b5cf6; border-color:#8b5cf6;">Danh Sách NPC</h2>
        <input type="hidden" id="editingNpcMapId">
        <div style="flex-grow: 1; overflow-y: auto; margin-top:10px;">
            <table style="width:100%;">
                <thead style="position: sticky; top: 0; background:#8b5cf6; color:white; z-index:10;">
                    <tr><th>Chọn NPC</th><th>Tọa độ X</th><th>Tọa độ Y</th><th>Xóa</th></tr>
                </thead>
                <tbody id="npcEditBody" class="modal-table-body"></tbody>
            </table>
        </div>
        <button class="btn-add-row" style="color:#8b5cf6; border-color:#8b5cf6;" onclick="addNewNpcRow()">+ Thêm NPC</button>
        <button class="btn-confirm-save" style="background:#8b5cf6;" onclick="confirmNpcEdit()">XÁC NHẬN LƯU TẠM</button>
    </div>
</div>

<script>
const npcMap = <?= json_encode($npcAvatars) ?>;
const npcNamesMap = <?= json_encode($npcNames) ?>;
const mobNamesMap = <?= json_encode($mobNames) ?>;
const npcFullList = <?= json_encode($npcFullList) ?>;
const mobFullList = <?= json_encode($mobFullList) ?>;

let currentTargetInput = null;

// Search Logic
$(".search-row input").on("input", function() {
    const filters = $(".search-row input").map(function() { return $(this).val().toLowerCase(); }).get();
    $("#mapsTable tbody tr").each(function() {
        let show = true;
        $(this).find("td").each(function(i) {
            if (filters[i]) {
                let val = $(this).find("input").val() || $(this).text();
                if (val.toLowerCase().indexOf(filters[i]) === -1) show = false;
            }
        });
        $(this).toggle(show);
    });
});

$(document).on('input', '.track', function() { $(this).addClass('changed'); });

// --- HÀM CHỌN TỪ BẢNG HIỆN THÊM ---
function openSelection(type, inputEl) {
    currentTargetInput = inputEl;
    $('#selectionTitle').text(type === 'mob' ? 'Danh Sách Quái Vật' : 'Danh Sách NPC');
    $('#selectionModal').css('display', 'flex');
    $('#selectionSearch').val('').focus();
    renderSelectionList(type);
}

function renderSelectionList(type, filter = '') {
    const list = type === 'mob' ? mobFullList : npcFullList;
    const container = $('#selectionList').empty();
    
    list.forEach(item => {
        if (filter && !item.NAME.toLowerCase().includes(filter) && !item.id.toString().includes(filter)) return;
        
        let imgSrc = '/icon/0.png';
        if(type === 'npc') {
            let nid = parseInt(item.id);
            let avatar = [3,4,6].includes(nid) ? {3:265, 4:84, 6:545}[nid] : item.avatar;
            imgSrc = `/icon/${avatar}.png`;
        }
        
        container.append(`
            <div class="select-card" onclick="selectItem('${item.id}', '${item.NAME}')">
                <img src="${imgSrc}" onerror="this.src='/icon/0.png'">
                <div>${item.NAME}</div>
                <small>ID: ${item.id}</small>
            </div>
        `);
    });
}
let rowToDelete = null; 

function confirmDeleteRow(btn, type) {
    rowToDelete = $(btn).closest('tr'); // Xác định hàng chứa nút bấm
    
    // Lấy tên cụ thể từ class .name-label trong hàng đó
    let displayName = rowToDelete.find('.name-label').text(); 
    
    // Xử lý chuỗi (loại bỏ phần ID nếu bạn muốn gọn hơn)
    // Ví dụ: "Gấu Tướng Quân (ID: 10)" -> "Gấu Tướng Quân"
    let cleanName = displayName.split(' (ID:')[0]; 

    const html = `
        <div class="popup-icon">⚠️</div>
        <div class="popup-title">Xác Nhận Xóa</div>
        <div class="popup-content">
            Xóa
            <b style="color:#ef4444; font-size:1.1em;">${cleanName}</b> này
        </div>
        <div class="confirm-group">
            <button class="confirm-btn confirm-yes" onclick="executeDeleteRow()">XÓA NGAY</button>
            <button class="confirm-btn confirm-no" onclick="closePopup()">HỦY</button>
        </div>
    `;
    
    $('#savePopupContent').html(html);
    $('#savePopup').css('display', 'flex');
}

function executeDeleteRow() {
    if (rowToDelete) {
        // Hiệu ứng mờ dần trước khi xóa (tùy chọn cho đẹp)
        rowToDelete.css('background', '#fee2e2');
        rowToDelete.fadeOut(300, function() {
            $(this).remove();
            rowToDelete = null;
        });
        closePopup();
    }
}
function selectItem(id, name) {
    $(currentTargetInput).val(id);
    $(currentTargetInput).siblings('.name-label').text(name + " (ID: " + id + ")");
    $('#selectionModal').hide();
}

$('#selectionSearch').on('input', function() {
    const type = $('#selectionTitle').text().includes('Quái') ? 'mob' : 'npc';
    renderSelectionList(type, $(this).val().toLowerCase());
});

// --- QUẢN LÝ MOBS ---
function editMobs(mapId) {
    const mobJson = $('#mb' + mapId).val();
    $('#editingMapId').val(mapId);
    $('#mobEditBody').empty();
    try {
        const dataArr = JSON.parse(mobJson || "[]");
        if(dataArr.length === 0) addNewMobRow();
        else {
            dataArr.forEach(m => {
                let d = (typeof m === 'string') ? JSON.parse(m.replace(/'/g, '"')) : m;
                addMobRowHTML(d[0], d[1], d[2], d[3], d[4]);
            });
        }
    } catch(e) { addNewMobRow(); }
    $('#mobModal').css('display', 'flex');
}

function addMobRowHTML(id, tnsm, hp, x, y) {
    const name = mobNamesMap[id] || "Bấm để chọn";
    $('#mobEditBody').append(`
        <tr>
            <td>
                <div class="input-group">
                    <input type="hidden" class="mob-id-input" value="${id}">
                    <span class="name-label" onclick="openSelection('mob', $(this).siblings('input'))">${name} (ID: ${id})</span>
                </div>
            </td>
            <td><input type="number" class="mob-tnsm" value="${tnsm}"></td>
            <td><input type="number" class="mob-hp" value="${hp}"></td>
            <td><input type="number" class="mob-x" value="${x}"></td>
            <td><input type="number" class="mob-y" value="${y}"></td>
      <td><button type="button" onclick="confirmDeleteRow(this, 'mob')" style="color:red;border:none;background:none;cursor:pointer;font-size:20px;">✖</button></td>
        </tr>
    `);
}
function addNewMobRow() { addMobRowHTML(0, 1, 100, 400, 400); }

function confirmMobEdit() {
    const mapId = $('#editingMapId').val();
    let res = [];
    $('#mobEditBody tr').each(function() {
        let mid = parseInt($(this).find('.mob-id-input').val());
        res.push([mid, parseInt($(this).find('.mob-tnsm').val())||1, parseInt($(this).find('.mob-hp').val())||100, parseInt($(this).find('.mob-x').val())||400, parseInt($(this).find('.mob-y').val())||400]);
    });
    $('#mb' + mapId).val(JSON.stringify(res)).addClass('changed');
    $('#btn_mob_' + mapId).css('background', '#10b981').text('✓ Đã Lưu Tạm');
    $('#mobModal').hide();
}

// --- QUẢN LÝ NPC ---
function editNpcs(mapId) {
    const npcJson = $('#np' + mapId).val();
    $('#editingNpcMapId').val(mapId);
    $('#npcEditBody').empty();
    try {
        const dataArr = JSON.parse(npcJson || "[]");
        if(dataArr.length === 0) addNewNpcRow();
        else dataArr.forEach(n => addNpcRowHTML(n[0], n[1], n[2]));
    } catch(e) { addNewNpcRow(); }
    $('#npcModal').css('display', 'flex');
}

function addNpcRowHTML(id, x, y) {
    const name = npcNamesMap[id] || "Bấm để chọn";
    $('#npcEditBody').append(`
        <tr>
            <td>
                <div class="input-group">
                    <input type="hidden" class="npc-id-input" value="${id}">
                    <span class="name-label" style="background:#f5f3ff; color:#7c3aed" onclick="openSelection('npc', $(this).siblings('input'))">${name} (ID: ${id})</span>
                </div>
            </td>
            <td><input type="number" class="npc-x-val" value="${x}"></td>
            <td><input type="number" class="npc-y-val" value="${y}"></td>
        <td><button type="button" onclick="confirmDeleteRow(this, 'npc')" style="color:red;border:none;background:none;cursor:pointer;font-size:20px;">✖</button></td>
        </tr>
    `);
}
function addNewNpcRow() { addNpcRowHTML(0, 400, 400); }

function confirmNpcEdit() {
    const mapId = $('#editingNpcMapId').val();
    let res = [];
    $('#npcEditBody tr').each(function() {
        let nid = parseInt($(this).find('.npc-id-input').val());
        res.push([nid, parseInt($(this).find('.npc-x-val').val())||400, parseInt($(this).find('.npc-y-val').val())||400]);
    });
    $('#np' + mapId).val(JSON.stringify(res)).addClass('changed');
    $('#btn_npc_' + mapId).css('background', '#10b981').text('✓ Đã Lưu Tạm');
    updatePreview(mapId);
    $('#npcModal').hide();
}

function updatePreview(id) {
    const container = $('#preview_' + id);
    container.empty();
    try {
        JSON.parse($('#np' + id).val()).forEach(item => {
            const nid = parseInt(item[0]);
            let img = [3,4,6].includes(nid) ? {3:265, 4:84, 6:545}[nid] : (npcMap[nid] || 0);
            container.append(`<div class='npc-item'><img src='/icon/${img}.png' onerror="this.src='/icon/0.png'"></div>`);
        });
    } catch(e) {}
}

async function saveAll() {
    const changes = document.querySelectorAll(".changed");
    if(changes.length === 0) return showPopup("warning", "Chú ý", "Bạn chưa thay đổi gì!");
    
    const ids = new Set();
    changes.forEach(el => { if(el.closest('tr').dataset.id) ids.add(el.closest('tr').dataset.id); });

    let successCount = 0;
    for (let id of ids) {
        const data = { id: id, name: $('#n'+id).val(), zones: $('#z'+id).val(), max_player: $('#m'+id).val(), mobs: $('#mb'+id).val(), npcs: $('#np'+id).val() };
        try {
            const res = await fetch(window.location.href, { method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify(data) });
            if(res.ok) { successCount++; $(`#row_${id} .changed`).removeClass('changed'); }
        } catch(e) {}
    }
    showPopup("success", "Thành Công", `Đã lưu cập nhật cho <b>${successCount}</b> Maps!`);
}

function showPopup(type, title, content){
    let icon = type === "success" ? "✅" : (type === "warning" ? "⚠️" : "❌");
    $('#savePopupContent').html(`<div class="popup-icon">${icon}</div><div class="popup-title">${title}</div><div class="popup-content">${content}</div>`);
    $('#savePopup').css('display', 'flex');
}
function closePopup(){ $('#savePopup').hide(); }
function closeMobModal() { $('#mobModal').hide(); }
function closeNpcModal() { $('#npcModal').hide(); }

window.onclick = function(e) {
    if ($(e.target).hasClass('popup-bg')) closePopup();
    if ($(e.target).hasClass('modal')) { 
        $('#mobModal').hide(); 
        $('#npcModal').hide(); 
        $('#selectionModal').hide(); 
    }
}
</script>
</body>
</html>