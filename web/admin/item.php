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

// ===== TRUY VẤN TOÀN BỘ ITEM CHO MODAL =====
$all_res = $conn->query("SELECT id, NAME, icon_id FROM item_template ORDER BY id ASC");
$all_items = ($all_res) ? $all_res->fetch_all(MYSQLI_ASSOC) : [];

// ===== XỬ LÝ PHÂN TRANG =====
$item_per_page = 500; 
$current_page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
if($current_page < 1) $current_page = 1;
$offset = ($current_page - 1) * $item_per_page;

$total_res = $conn->query("SELECT COUNT(*) as total FROM item_template");
$total_row = $total_res->fetch_assoc();
$total_items = $total_row['total'];
$total_pages = ceil($total_items / $item_per_page);

// ===== HANDLE SAVE (AJAX) =====
if($_SERVER['REQUEST_METHOD'] === 'POST'){
    $data = json_decode(file_get_contents("php://input"), true);
    if(!$data){ echo json_encode(["status"=>"error"]); exit; }

    $id = (int)$data['id'];
    $type = (int)$data['type'];
    $gender = (int)$data['gender'];
    $name = $conn->real_escape_string($data['name']);
    $description = $conn->real_escape_string($data['description']);
    $icon_id = (int)$data['icon_id'];
    $head = (int)$data['head'];
    $body = (int)$data['body'];
    $leg = (int)$data['leg'];

    $sql = "UPDATE item_template SET TYPE='$type', gender='$gender', NAME='$name', 
            description='$description', icon_id='$icon_id', head='$head', body='$body', leg='$leg' 
            WHERE id=$id";
            
    if($conn->query($sql)) echo json_encode(["status"=>"success"]);
    else echo json_encode(["status"=>"error", "msg" => $conn->error]);
    exit;
}

$list_res = $conn->query("SELECT * FROM item_template ORDER BY id ASC LIMIT $item_per_page OFFSET $offset");
$items_list = $list_res->fetch_all(MYSQLI_ASSOC);
?>

<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>Quản lý Vật Phẩm</title>
<link rel="icon" type="image/png" href="/img/icon.jpg">

<style>
/* ===== BASE STYLE ===== */
body{
    margin:0; font-family:'Segoe UI'; font-size:1.1em;
    background:linear-gradient(135deg,#e0f2fe,#bae6fd);
}
.container{ max-width:1300px; margin:auto; padding:15px; }

/* ===== HEADER BOX (ĐỒNG BỘ ACCOUNT) ===== */
.header-box{
    display:flex; justify-content:space-between; align-items:center;
    background:white; padding:20px; border-radius:16px;
    box-shadow:0 10px 25px rgba(0,0,0,.15); margin-bottom:20px;
}
/* ===== TABLE STYLE (ĐÃ CĂN GIỮA TOÀN BỘ) ===== */
.table-wrap { overflow-x: auto; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,.2); }
table { width: 100%; border-collapse: collapse; background: white; table-layout: fixed; } /* Thêm fixed để kích thước cột chuẩn hơn */

th { background: #2563eb; color: white; padding: 12px; text-align: center; } /* Căn giữa tiêu đề */
td { padding: 10px; text-align: center; border-bottom: 1px solid #e2e8f0; vertical-align: middle; } /* Căn giữa ô dữ liệu */

.id-cell { background: #93c5fd; font-weight: bold; }
.item-img { 
    width: 45px; 
    height: 45px; 
    object-fit: contain; 
    background: #f8fafc; 
    border-radius: 8px;
    display: block; /* Thêm dòng này */
    margin: auto;   /* Thêm dòng này để căn giữa ảnh trong ô td */
}

input.data { 
    border: none; 
    background: transparent; 
    text-align: center; /* Căn giữa chữ trong ô input */
    font-weight: bold; 
    width: 100%; 
    font-size: 1em; 
    outline: none;
}
.header-left{display:flex; align-items:center; gap:20px;}
.header-left img{width:200px;height:200px;object-fit:contain;}
.header-content h1{margin:0; color:#1e3a8a;}
/* Ví dụ: Chỉnh độ rộng cho từng cột cụ thể */
#mainTable th:nth-child(1) { width: 30px; }  /* Cột ID */
#mainTable th:nth-child(2) { width: 30px; }  /* Cột Icon */
#mainTable th:nth-child(3) { width: 25px; }  /* Cột Type */
#mainTable th:nth-child(4) { width: 20px; }  /* Cột Gender */
#mainTable th:nth-child(5) { width: 250px; } /* Cột Tên (nên để rộng nhất) */
#mainTable th:nth-child(6) { width: 350px; } /* Cột Mô tả */
/* Các cột H, B, L còn lại */
#mainTable th:nth-child(n+7) { width: 30px; }
/* ===== BUTTON STYLE (ĐỒNG BỘ HIỆU ỨNG) ===== */
.menu-buttons { display: flex; gap: 10px; margin-top: 15px; /* Thêm dòng này để đẩy nút xuống dưới */}
.menu-buttons button, .action-box button {
    position: relative; overflow: hidden; transition: all 0.2s ease;
    padding: 8px 15px; border: none; border-radius: 10px;
    color: white; font-weight: 600; cursor: pointer; font-size: 1.2em;
}
.home { background: #3b82f6; }
.refresh { background: #22c55e; }
.quick-view { background: #8b5cf6; }
.save-all { background: #16a34a; }

.menu-buttons button:hover, .action-box button:hover {
    transform: translateY(-2px) scale(1.05);
    box-shadow: 0 8px 20px rgba(0,0,0,0.25);
}
.menu-buttons button:active, .action-box button:active {
    transform: scale(0.95);
}

.action-box {
    display: flex; justify-content: center; gap: 20px; margin-bottom: 15px;
    padding: 15px; background: white; border-radius: 12px; box-shadow: 0 5px 15px rgba(0,0,0,.1);
}

/* ===== TABLE STYLE ===== */
.table-wrap { overflow-x: auto; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,.2); }
table { width: 100%; border-collapse: collapse; background: white; }
th { 
    background: #2563eb; 
    color: white; 
    padding: 12px; 
    text-align: center; /* Căn giữa chữ tiêu đề */
}
td { 
    padding: 10px; 
    text-align: center; 
    border-bottom: 1px solid #e2e8f0; 
    vertical-align: middle; /* Căn giữa nội dung theo chiều dọc */
}
.id-cell { background: #93c5fd; font-weight: bold; }
.item-img { width: 45px; height: 45px; object-fit: contain; background: #f8fafc; border-radius: 8px; }
input.data { border: none; background: transparent; text-align: center; font-weight: bold; width: 100%; font-size: 1em; }
.changed { background: #fef08a !important; }

/* ===== MODAL XEM NHANH (FIX LỖI ĐÈ NÚT) ===== */
.modal-overlay { 
    display: none; position: fixed; inset: 0; background: rgba(0, 0, 0, 0.85); 
    z-index: 1000; justify-content: center; align-items: center; backdrop-filter: blur(5px);
}
.modal-content { 
    background: #f8fafc; width: 95%; max-width: 1100px; height: 85vh; 
    border-radius: 20px; display: flex; flex-direction: column; overflow: hidden; position: relative;
}
.modal-header {
    display: grid;
    grid-template-columns: 1fr 60px; /* Chia cột cố định cho nút đóng */
    gap: 15px; padding: 20px; background: white; 
    border-bottom: 2px solid #e2e8f0; align-items: center;
}
.modal-search-wrapper input {
    width: 100%; padding: 12px 20px; border-radius: 12px; border: 2px solid #cbd5e1; 
    font-size: 1.1em; outline: none; box-sizing: border-box;
}
/* ===== ĐỒNG BỘ NÚT ĐÓNG (X) ===== */
/* ===== ĐỒNG BỘ NÚT ĐÓNG (X) TRÒN ===== */
.close-btn {
    position: absolute;
    /* Đưa nút ra mép ngoài góc trên */
    top: -25px; 
    right: -25px;
    
    /* Tạo hình tròn hoàn hảo */
    width: 60px; 
    height: 60px; 
    border-radius: 50%; 
    
    background: #ef4444; 
    color: white; 
    font-size: 35px; 
    font-weight: 900;
    
    display: flex; 
    align-items: center; 
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 5px 15px rgba(0,0,0,0.3);
    border: 3px solid white; /* Thêm viền trắng cho nổi bật */
    z-index: 100;
}

.close-btn:hover { 
    transform: scale(1.1) rotate(90deg); 
    background: #dc2626; 
    box-shadow: 0 8px 20px rgba(0,0,0,0.4);
}

/* Điều chỉnh lại vị trí popup để nút X không bị lệch quá xa */
.popup-box {
    position: relative;
    /* ... các style cũ giữ nguyên ... */
}
.close-modal-btn {
    width: 50px; height: 50px; background: #ef4444; color: white; border: none;
    border-radius: 12px; font-size: 24px; font-weight: bold; cursor: pointer;
    display: flex; align-items: center; justify-content: center;
}
.close-modal-btn:hover { transform: scale(1.1) rotate(90deg); background: #dc2626; }

.icon-grid { 
    display: grid; grid-template-columns: repeat(auto-fill, minmax(135px, 1fr)); 
    gap: 12px; padding: 20px; overflow-y: auto; background: #f1f5f9;
}
.icon-box { 
    background: white; border: 1px solid #e2e8f0; padding: 12px; 
    text-align: center; cursor: pointer; border-radius: 12px; transition: 0.2s;
}
.icon-box:hover { transform: translateY(-3px); box-shadow: 0 5px 15px rgba(0,0,0,0.1); border-color: #3b82f6; }
.icon-box img { width: 45px; height: 45px; }

/* ===== POPUP THÔNG BÁO ===== */
.popup-bg {
    position: fixed; inset: 0; background: rgba(0,0,0,0.7); 
    display: none; align-items: center; justify-content: center; z-index: 9999;
}
.popup-box {
    width: 90%; max-width: 630px; padding: 40px; border-radius: 18px;
    text-align: center; background: white; position: relative;
    animation: pop .25s ease;
}
/* Style cho hàng tìm kiếm */
.search-row {
    background: #f1f5f9;
}
.search-row td {
    padding: 5px !important;
}
.search-row input {
    width: 100%;
    padding: 6px;
    border-radius: 6px;
    border: 1px solid #cbd5e1;
    box-sizing: border-box;
    text-align: center;
    font-size: 0.85em;
    outline: none;
}
.search-row input:focus {
    border-color: #2563eb;
    background: white;
}

.popup-icon { font-size: 80px; margin-bottom: 10px; }
.popup-title { font-size: 35px; font-weight: bold; color: #1e293b; }
.popup-content { font-size: 25px; color: #475569; margin-top: 10px; }
@keyframes pop { from{transform:scale(.8);opacity:0} to{transform:scale(1);opacity:1} }

/* ===== PHÂN TRANG ===== */
.pagination { display: flex; justify-content: center; gap: 8px; margin: 20px 0; }
.pagination a { 
    padding: 8px 16px; background: white; border-radius: 8px; text-decoration: none; 
    color: #1e3a8a; font-weight: bold; box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}
.pagination a.active { background: #2563eb; color: white; }
</style>
<link rel="stylesheet" href="/typography.css?v=20261014-1"><link rel="stylesheet" href="admin-ui.css?v=20261015-4"><script src="admin-ui.js?v=20261015-2" defer></script><meta name="viewport" content="width=device-width,initial-scale=1"></head>

<body class="admin-page" data-admin-page="item"><?php require_once __DIR__.'/admin_ui.php'; adminPageToolbar('Vật phẩm'); ?>

<div class="container">
    <div class="header-box">
        <div class="header-left">
         <img src="/img/account.gif">
         <div class="header-content">
    <h1>Vật phẩm <span style="font-size: 0.5em; vertical-align: middle; background: #2563eb; color: white; padding: 4px 12px; border-radius: 20px;">Tổng: <?= number_format($total_items) ?></span></h1>
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

   <div class="action-box" style="justify-content:space-between; align-items:center; flex-wrap:wrap;">
    
    <div style="display:flex; gap:20px;">
        <button class="quick-view" onclick="openModal()">👁 Xem Nhanh Item</button>
        <button class="save-all" onclick="saveAll()">💾 Lưu Dữ Liệu</button>
    </div>

    <div class="pagination" style="margin:0;">
        <?php for($i = 1; $i <= $total_pages; $i++): ?>
            <a href="?page=<?= $i ?>" class="<?= ($i == $current_page) ? 'active' : '' ?>">
                <?= $i ?>
            </a>
        <?php endfor; ?>
    </div>


    <div class="table-wrap">
        <table id="mainTable">
           <thead>
    <tr>
        <th>ID</th><th>Icon</th><th>Type</th><th>Gen</th><th>Tên</th><th>Mô Tả</th><th>IconID</th><th>H</th><th>B</th><th>L</th>
    </tr>
    <tr class="search-row">
        <td><input type="text" placeholder="ID"></td>
        <td></td> <td><input type="text" placeholder="Type"></td>
        <td><input type="text" placeholder="Gen"></td>
        <td><input type="text" placeholder="Tên..."></td>
        <td><input type="text" placeholder="Mô tả..."></td>
        <td><input type="text" placeholder="IconID"></td>
        <td><input type="text" placeholder="Head"></td>
        <td><input type="text" placeholder="Body"></td>
        <td><input type="text" placeholder="Leg"></td>
    </tr>
</thead>
    <tbody>
    <?php foreach($items_list as $row): ?>
    <tr id="row_<?= $row['id'] ?>">
        <td class="id-cell"><?= $row['id'] ?></td>
        <td><img src="/icon/<?= $row['icon_id'] ?>.png" class="item-img" id="img_<?= $row['id'] ?>" onerror="this.src='https://img.icons8.com/color/48/box--v1.png'"></td>
        <td><input class="data track" id="ty<?= $row['id'] ?>" value="<?= $row['TYPE'] ?>"></td>
        <td><input class="data track" id="ge<?= $row['id'] ?>" value="<?= $row['gender'] ?>"></td>
        <td><input class="data track" id="na<?= $row['id'] ?>" value="<?= htmlspecialchars($row['NAME']) ?>"></td>
        <td><input class="data track" style="font-weight:normal" id="de<?= $row['id'] ?>" value="<?= htmlspecialchars($row['description']) ?>"></td>
        <td><input class="data track icon-input" data-id="<?= $row['id'] ?>" id="ic<?= $row['id'] ?>" value="<?= $row['icon_id'] ?>"></td>
        <td><input class="data track" id="he<?= $row['id'] ?>" value="<?= $row['head'] ?>"></td>
        <td><input class="data track" id="bo<?= $row['id'] ?>" value="<?= $row['body'] ?>"></td>
        <td><input class="data track" id="le<?= $row['id'] ?>" value="<?= $row['leg'] ?>"></td>
    </tr>
    <?php endforeach; ?>
</tbody>
        </table>
    </div>
</div>

<div class="modal-overlay" id="quickModal">
    <div class="modal-content">
        <div class="modal-header">
            <div class="modal-search-wrapper">
                <input type="text" id="modalSearch" placeholder="🔍 Tìm tên hoặc ID vật phẩm..." onkeyup="filterModal()">
            </div>
            <button class="close-modal-btn" onclick="closeModal()">✖</button>
        </div>
       <div class="icon-grid">
    <?php foreach($all_items as $it): ?>
    <div class="icon-box" ondblclick="jumpTo(<?= $it['id'] ?>)" title="Nhấn đúp để chọn" data-search="<?= strtolower($it['NAME']) ?> <?= $it['id'] ?>">
        <img src="/icon/<?= $it['icon_id'] ?>.png" onerror="this.src='https://img.icons8.com/color/48/box--v1.png'">
        <div style="font-size:12px; font-weight:bold; color:#333; margin-top:5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"><?= htmlspecialchars($it['NAME']) ?></div>
        <div style="font-size:11px; color:#2563eb; background:#dbeafe; padding:2px 8px; border-radius:10px; display:inline-block; margin-top:3px;">ID: <?= $it['id'] ?></div>
    </div>
    <?php endforeach; ?>
</div>
    </div>
</div>

<div id="savePopup" class="popup-bg">
    <div class="popup-box">
        <div class="close-btn" onclick="closePopup()">✖</div>
        <div id="savePopupContent"></div>
    </div>
</div>
<script>
    const v = (p, id) => document.getElementById(p + id).value;

    function openModal() { document.getElementById('quickModal').style.display = 'flex'; }
    function closeModal() { document.getElementById('quickModal').style.display = 'none'; }
    
    // Đóng bằng phím ESC
    window.addEventListener('keydown', (e) => { if(e.key === "Escape") closeModal(); });

    function filterModal() {
        const val = document.getElementById('modalSearch').value.toLowerCase();
        document.querySelectorAll('.icon-box').forEach(box => {
            box.style.display = box.dataset.search.includes(val) ? 'block' : 'none';
        });
    }

    function jumpTo(id) {
        closeModal();
        const el = document.getElementById("row_" + id);
        if(el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el.style.background = "#fef08a";
            setTimeout(() => { el.style.background = ""; }, 2000);
        } else {
            alert("ID " + id + " không ở trang này. Vui lòng chuyển trang!");
        }
    }

    document.querySelectorAll(".track").forEach(inp => {
        inp.addEventListener("input", () => inp.classList.add("changed"));
    });

    document.querySelectorAll('.icon-input').forEach(input => {
        input.addEventListener('input', function() {
            const id = this.dataset.id;
            const img = document.getElementById('img_' + id);
            if(img) img.src = '/icon/' + this.value + '.png';
        });
    });

    function showPopup(type, title, content){
        const popup = document.getElementById("savePopup");
        const html = document.getElementById("savePopupContent");
        let icon = type === "success" ? "✅" : (type === "error" ? "❌" : "⚠️");
        html.innerHTML = `<div class="popup-icon">${icon}</div><div class="popup-title">${title}</div><div class="popup-content">${content}</div>`;
        popup.style.display = "flex";
    }
    function closePopup() { document.getElementById("savePopup").style.display = "none"; }

    async function saveAll() {
        const rows = document.querySelectorAll("#mainTable tbody tr");
        let success = 0; let fail = 0; let changed = false;
        for (let row of rows) {
            const changedInputs = row.querySelectorAll(".changed");
            if (changedInputs.length > 0) {
                changed = true;
                const id = row.id.replace("row_", "");
                const payload = {
                    id: id, type: v("ty", id), gender: v("ge", id),
                    name: v("na", id), description: v("de", id),
                    icon_id: v("ic", id), head: v("he", id),
                    body: v("bo", id), leg: v("le", id)
                };
                try {
                    let res = await fetch(window.location.href, {
                        method: "POST", headers: {"Content-Type": "application/json"},
                        body: JSON.stringify(payload)
                    });
                    let json = await res.json();
                    if(json.status === "success") {
                        success++;
                        changedInputs.forEach(i => i.classList.remove("changed"));
                    } else fail++;
                } catch (e) { fail++; }
            }
        }
        if(!changed) return showPopup("warning", "Không có thay đổi", "Bạn chưa sửa gì cả!");
        showPopup("success", "Thành công", `Lưu hoàn tất <b>${success}</b> vật phẩm.`);
    }
    // Logic tìm kiếm cho bảng vật phẩm
// Biến dùng để kiểm soát thời gian chờ
let searchTimeout = null;

function filterTableData() {
    // Lấy tất cả input trong hàng search
    const searchInputs = document.querySelectorAll(".search-row input");
    const rows = document.querySelectorAll("#mainTable tbody tr");

    rows.forEach(row => {
        let isMatch = true;
        const cells = row.querySelectorAll("td");

        // Lặp qua từng ô input tìm kiếm
        searchInputs.forEach(input => {
            const filterValue = input.value.toLowerCase().trim();
            if (filterValue === "") return; // Nếu ô tìm kiếm trống thì bỏ qua

            // Tìm xem input này nằm ở cột (td) thứ mấy trong hàng search
            const colIndex = input.closest('td').cellIndex;
            
            // Lấy dữ liệu tương ứng ở cột đó trong hàng dữ liệu
            const targetCell = cells[colIndex];
            if (targetCell) {
                const inputInside = targetCell.querySelector("input");
                const cellText = inputInside ? inputInside.value.toLowerCase() : targetCell.innerText.toLowerCase();
                
                if (!cellText.includes(filterValue)) {
                    isMatch = false;
                }
            }
        });

        row.style.display = isMatch ? "" : "none";
    });
}

// Gán sự kiện với kỹ thuật Debounce
document.querySelectorAll(".search-row input").forEach((input) => {
    input.addEventListener("input", () => {
        clearTimeout(searchTimeout);
        // Đợi 300 mili giây sau khi người dùng ngừng gõ mới thực hiện lọc
        searchTimeout = setTimeout(filterTableData, 300);
    });
});
</script>
</body>
</html>