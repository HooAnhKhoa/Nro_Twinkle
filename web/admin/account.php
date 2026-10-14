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

// ===== HANDLE SAVE =====
if($_SERVER['REQUEST_METHOD'] === 'POST'){
    $data = json_decode(file_get_contents("php://input"), true);

    if(!$data){
        echo json_encode(["status"=>"error"]);
        exit;
    }
  // ===== ADD ACCOUNT (CÁCH 1 - CHỈ INSERT CƠ BẢN) =====
if(isset($data['action']) && $data['action'] === 'add'){
    $username = "0325420834";
    $password = "bansrcnro";

    // 1. Check tồn tại
    $check = $conn->query("SELECT id FROM account WHERE username='$username'");
    if($check && $check->num_rows > 0){
        echo json_encode([
            "status" => "error",
            "message" => "Tài khoản đã tồn tại!"
        ]);
        exit;
    }

    // 2. INSERT TỐI GIẢN
    $insert = $conn->query("
        INSERT INTO account (username, password)
        VALUES ('$username', '$password')
    ");

    if(!$insert){
        echo json_encode([
            "status" => "error",
            "message" => "Lỗi SQL: " . $conn->error
        ]);
        exit;
    }

    $newId = $conn->insert_id;

    // 3. Tạo player
    $conn->query("
        INSERT INTO player (account_id, name)
        VALUES ($newId, '')
    ");

    echo json_encode([
        "status" => "success",
        "message" => "Tạo account thành công!"
    ]);
    exit;
}
// Tìm đoạn handle action delete và thay bằng:
if(isset($data['action']) && $data['action'] === 'delete'){
    $delId = (int)$data['id'];

    // Kiểm tra tồn tại và quyền Admin
    $q = $conn->query("SELECT is_admin FROM account WHERE id = $delId");
    $accData = $q->fetch_assoc();

    if(!$accData) {
        echo json_encode(["status" => "error", "message" => "Tài khoản không tồn tại!"]);
        exit;
    }

    if((int)$accData['is_admin'] === 1) {
         echo json_encode(["status" => "error", "message" => "Không thể xóa Quản trị viên!"]);
         exit;
    }

    // Thực hiện xóa
    $conn->query("DELETE FROM player WHERE account_id = $delId");
    $conn->query("DELETE FROM account WHERE id = $delId");

    // TRẢ VỀ JSON CÓ MESSAGE
    echo json_encode([
        "status" => "success", 
        "message" => "Đã xóa tài khoản ID: $delId thành công!"
    ]);
    exit;
}
    
    $id = (int)$data['id'];
    $username = $conn->real_escape_string($data['username']);
    $name = $conn->real_escape_string($data['player_name']);

    // 1. Kiểm tra trùng USERNAME (trừ ID hiện tại)
    $checkUser = $conn->query("SELECT id FROM account WHERE username='$username' AND id != $id");
    if($checkUser && $checkUser->num_rows > 0){
        echo json_encode(["status" => "error", "message" => "Tài khoản <b>'$username'</b> đã có người sử dụng!"]);
        exit;
    }

    // 2. Kiểm tra trùng TÊN NHÂN VẬT (trừ account_id hiện tại)
if(trim($name) !== ""){
    $checkPlayerName = $conn->query("
        SELECT id FROM player 
        WHERE name='$name' AND account_id != $id
    ");

    if($checkPlayerName && $checkPlayerName->num_rows > 0){
        echo json_encode([
            "status" => "error",
            "message" => "Tên nhân vật <b>'$name'</b> đã tồn tại!"
        ]);
        exit;
    }
}

    // --- Nếu không trùng thì mới thực hiện UPDATE ---
    $password = $conn->real_escape_string($data['password']);
    $ban = $conn->real_escape_string($data['ban']);
    $admin = $conn->real_escape_string($data['admin']);
    $active = $conn->real_escape_string($data['active']);
    $vnd = $conn->real_escape_string($data['vnd']);
    $tongnap = $conn->real_escape_string($data['tongnap']);

    // Update Account
    $conn->query("
        UPDATE account SET
        username='$username',
        password='$password',
        ban='$ban',
        is_admin='$admin',
        active='$active',
        vnd='$vnd',
        tongnap='$tongnap'
        WHERE id=$id
    ");

    // Update hoặc Insert Player
    $checkPlayer = $conn->query("SELECT id FROM player WHERE account_id=$id");
    if($checkPlayer && $checkPlayer->num_rows > 0){
        $conn->query("UPDATE player SET name='$name' WHERE account_id=$id");
    }else{
        $conn->query("INSERT INTO player (account_id, name) VALUES ($id, '$name')");
    }

    echo json_encode(["status"=>"success"]);
    exit;
}

// ===== XỬ LÝ PHÂN TRANG =====
$item_per_page = 500; // Mỗi trang hiện 50 tài khoản
$current_page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
if($current_page < 1) $current_page = 1;
$offset = ($current_page - 1) * $item_per_page;

// Đếm tổng số account để tính số trang

// Đếm tổng số account
$total_res = $conn->query("SELECT COUNT(*) as total FROM account");
$total_row = $total_res->fetch_assoc();
$total_accounts = $total_row['total']; // Đây là biến chứa tổng số
$total_pages = ceil($total_accounts / $item_per_page);

// ===== LOAD DATA (CÓ LIMIT) =====
$list = $conn->query("
SELECT 
    account.id, 
    account.username, 
    account.password, 
    account.ban, 
    account.is_admin, 
    account.ip_address, 
    account.active, 
    account.vnd, 
    account.tongnap,
    player.name AS player_name
FROM account
LEFT JOIN player ON player.account_id = account.id
ORDER BY account.id ASC
LIMIT $item_per_page OFFSET $offset
");
?>

<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>Quản lý Account</title>

<link rel="icon" type="image/png" href="/img/icon.jpg">

<style>
body{
margin:0;
font-family:'Segoe UI';
font-size:1.1em;
background:linear-gradient(135deg,#e0f2fe,#bae6fd);
}
/* ===== PHÂN TRANG ===== */
.btn-add{
    background: linear-gradient(135deg,#22c55e,#16a34a);
    padding: 10px 18px;
    font-size: 1.3em;
    border-radius: 10px;
    color: white;
    font-weight: bold;
    cursor: pointer;
}

.btn-add:hover{
    transform: scale(1.1);
    box-shadow: 0 8px 20px rgba(0,0,0,0.25);
}
.pagination { 
    display: flex; 
    justify-content: center; 
    gap: 8px; 
    margin: 20px 0; 
}
.pagination a { 
    padding: 10px 18px; 
    background: white; 
    border-radius: 8px; 
    text-decoration: none; 
    color: #1e3a8a; 
    font-weight: bold; 
    box-shadow: 0 4px 10px rgba(0,0,0,0.1);
    transition: all 0.2s;
}
.pagination a.active { 
    background: #2563eb; 
    color: white; 
    transform: scale(1.1);
}
.pagination a:hover:not(.active) {
    background: #dbeafe;
    transform: translateY(-2px);
}
.container{
max-width:1300px;
margin:auto;
padding:15px;
}
.menu-buttons button,
.action-box button{
    position: relative;
    overflow: hidden;
    transition: all 0.2s ease;
}
/* ===== POPUP SAVE ĐẸP ===== */

.popup-bg{
    position:fixed;
    top:0;
    left:0;
    width:100%;
    height:100%;
    background:rgba(0,0,0,0.7);
    display:none;
    align-items:center;
    justify-content:center;
    z-index:9999;
}



.popup-box{
    width:90%;
    max-width:630px; /* 420 x 1.5 */
    padding:42px 36px; /* tăng theo tỉ lệ */

    border-radius:18px;
    text-align:center;

  background:linear-gradient(145deg,#f8fafc,#e2e8f0);
    color:#1e293b;

    box-shadow:
    0 20px 60px rgba(0,0,0,.4);

    animation:pop .25s ease;
    position:relative;
}

/* ICON */
.popup-icon{
    font-size:84px; /* x2 */
}

.popup-title{
    font-size:40px; /* x2 */
}
/* Hiệu ứng cho các nút trong Popup xác nhận */
#popupActions button {
    position: relative;
    overflow: hidden;
    transition: all 0.2s ease;
    font-size: 1.1em;
    box-shadow: 0 4px 10px rgba(0,0,0,0.1);
}

/* Hiệu ứng khi di chuột qua (Hover) */
#popupActions button:hover {
    transform: translateY(-3px) scale(1.05);
    box-shadow: 0 8px 20px rgba(0,0,0,0.2);
}

/* Hiệu ứng khi nhấn (Click) */
#popupActions button:active {
    transform: scale(0.95);
    box-shadow: 0 2px 5px rgba(0,0,0,0.15);
}

/* Màu riêng cho nút Hủy để trông êm mắt hơn */
.btn-cancel {
    background: #94a3b8;
    color: white;
    padding: 12px 28px;
    border: none;
    border-radius: 10px;
    cursor: pointer;
    font-weight: bold;
}

/* Màu riêng cho nút Xóa (đỏ rực cảnh báo) */
.btn-confirm-del {
    background: linear-gradient(135deg, #ef4444, #dc2626);
    color: white;
    padding: 12px 28px;
    border: none;
    border-radius: 10px;
    cursor: pointer;
    font-weight: bold;
}
.popup-content{
    font-size:32px; /* x2 */
}

/* STATE */
.popup-success .popup-icon{ color:#22c55e; }
.popup-error .popup-icon{ color:#ef4444; }
.popup-warning .popup-icon{ color:#f59e0b; }
@keyframes pop{
    from{transform:scale(.8);opacity:0}
    to{transform:scale(1);opacity:1}
}

.close-btn{
    position:absolute;
    top:-18px;
    right:-10px;

    font-size:40px;
    font-weight:900;

    width:65px;
    height:65px;

    display:flex;
    align-items:center;
    justify-content:center;

    border-radius:50%;
    background:#ef4444;
    color:white;

    cursor:pointer;
    transition:.2s;
}

.close-btn:hover{
    transform:scale(1.15) rotate(90deg);
    background:#dc2626;
}
@keyframes ripple{
    to{
        transform: translate(-50%, -50%) scale(3);
        opacity: 0;
    }
}
/* Hover */
.menu-buttons button:hover,
.action-box button:hover{
    transform: translateY(-2px) scale(1.05);
    box-shadow: 0 8px 20px rgba(0,0,0,0.25);
}

/* Click */
.menu-buttons button:active,
.action-box button:active{
    transform: scale(0.95);
    box-shadow: 0 3px 10px rgba(0,0,0,0.2);
}

/* Ripple effect */
.menu-buttons button::after,
.action-box button::after{
    content: "";
    position: absolute;
    width: 0;
    height: 0;
    background: rgba(255,255,255,0.5);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    opacity: 0;
    pointer-events: none;
}
.btn-del{
    background: #ef4444;
    color: white;
    border: none;
    padding: 5px 10px;
    border-radius: 6px;
    cursor: pointer;
    font-weight: bold;
}
.btn-del:hover{ background: #b91c1c; }
.header-box{
display:flex;
justify-content:space-between;
align-items:center;
background:white;
padding:20px;
border-radius:16px;
box-shadow:0 10px 25px rgba(0,0,0,.15);
margin-bottom:20px;
}

.header-left{display:flex;align-items:center;gap:20px;}
.header-left img{width:200px;height:200px;object-fit:contain;}

.header-content{display:flex;flex-direction:column;gap:10px;}
.header-content h1{margin:0;color:#1e3a8a;}

td.id-cell,
td.ip-cell{
background:#93c5fd;
font-weight:bold;
user-select:none;
}

.menu-buttons{display:flex;gap:10px;}
.menu-buttons button{
padding:8px;
border:none;
border-radius:10px;
color:white;
font-weight:600;
cursor:pointer;
font-size:1.2em;
}
.home{background:#3b82f6;}
.refresh{background:#22c55e;}

.action-box{
display:flex;
justify-content:center;
gap:20px;
margin-bottom:15px;
padding:15px;
background:white;
border-radius:12px;
box-shadow:0 5px 15px rgba(0,0,0,.1);
}

.action-box button{
padding:10px 20px;
border:none;
border-radius:10px;
font-weight:bold;
cursor:pointer;
font-size:1.1em;
color:white;
}

.save-all{background:#16a34a;}
.undo{background:#f59e0b;}

.table-wrap{
overflow-x:auto;
border-radius:12px;
box-shadow:0 10px 25px rgba(0,0,0,.2);
}

table{
width:100%;
border-collapse:collapse;
background:white;
table-layout: fixed;
}

th, td{
padding:10px;
text-align:center;
overflow:hidden;
word-wrap: break-word;
}

th{background:#2563eb;color:white;}

.search-row input{
width:100%;
padding:6px;
border-radius:6px;
border:1px solid #93c5fd;
box-sizing:border-box;
}

input.data{
border:none;
background:transparent;
text-align:center;
font-weight:bold;
width:100%;
box-sizing:border-box;
font-size: 1.14em; /* Đây là đoạn tăng 5% */
}

.changed{
    background:#93c5fd !important;
    box-shadow: inset 0 0 5px rgba(0,0,0,0.1);
}

.admin{background:#fecaca;}
.vip{background:#e9d5ff;}
.active{background:#dcfce7;}
</style>
<link rel="stylesheet" href="/typography.css?v=20261014-1"></head>

<body>

<div class="container">

<div class="header-box">
    <div class="header-left">
        <img src="/img/account.gif">
   <div class="header-content">
    <h1>Account <span style="font-size: 0.5em; vertical-align: middle; background: #2563eb; color: white; padding: 4px 12px; border-radius: 20px;">Tổng: <?= number_format($total_accounts) ?></span></h1>
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
        <button class="save-all" onclick="saveAll()">💾 Lưu Dữ Liệu</button>
       
    </div>

    <div class="pagination" style="margin:0;">
        <?php for($i = 1; $i <= $total_pages; $i++): ?>
            <a href="?page=<?= $i ?>" class="<?= ($i == $current_page) ? 'active' : '' ?>">
                <?= $i ?>
            </a>
        <?php endfor; ?>
    </div>

</div>
<div class="table-wrap">
<table id="table">

<tr>
    <th style="width: 40px;">ID</th>
    <th style="width: 120px;">Tên Nhân vật</th>
    <th style="width: 110px;">Tài Khoản</th>
    <th style="width: 110px;">Mật Khẩu</th>
    <th style="width: 40px;">Ban</th>
    <th style="width: 60px;">Admin</th>
    <th style="width: 120px;">IP</th>
    <th style="width: 40px;">Active</th>
    <th style="width: 80px;">Số Dư</th>
    <th style="width: 80px;">Tổng nạp</th>
    <th style="width: 60px;">Xóa</th> </tr>

<tr class="search-row">
    <td><input type="text" placeholder="ID..." oninput="filterTable()"></td>
    <td><input type="text" placeholder="Tên..." oninput="filterTable()"></td>
    <td><input type="text" placeholder="User..." oninput="filterTable()"></td>
    <td><input type="text" placeholder="Pass..." oninput="filterTable()"></td>
    <td><input type="text" placeholder="Ban..." oninput="filterTable()"></td>
    <td><input type="text" placeholder="Admin..." oninput="filterTable()"></td>
    <td><input type="text" placeholder="IP..." oninput="filterTable()"></td>
    <td><input type="text" placeholder="Active..." oninput="filterTable()"></td>
    <td><input type="text" placeholder="vnd..." oninput="filterTable()"></td>
    <td><input type="text" placeholder="Nạp..." oninput="filterTable()"></td>
    <td></td> </tr>


<?php while($row = $list->fetch_assoc()):
    $class = "";
    if($row['is_admin'] == 1) $class="admin";
    elseif($row['tongnap'] > 499000) $class="vip";
    elseif($row['active'] == 1) $class="active";
?>
<tr class="<?= $class ?>" id="row_<?= $row['id'] ?>">
    <td class="id-cell"><?= $row['id'] ?></td>
    <td><input class="data track" id="pl<?= $row['id'] ?>" value="<?= htmlspecialchars($row['player_name'] ?? '') ?>"></td>
    <td><input class="data track" id="u<?= $row['id'] ?>" value="<?= htmlspecialchars($row['username']) ?>"></td>
    <td><input class="data track" id="p<?= $row['id'] ?>" value="<?= htmlspecialchars($row['password']) ?>"></td>
    <td><input class="data track" id="b<?= $row['id'] ?>" value="<?= $row['ban'] ?>"></td>
    <td><input class="data track" id="a<?= $row['id'] ?>" value="<?= $row['is_admin'] ?>"></td>
    <td class="ip-cell"><?= $row['ip_address'] ?></td>
    <td><input class="data track" id="ac<?= $row['id'] ?>" value="<?= $row['active'] ?>"></td>
    <td><input class="data track" id="v<?= $row['id'] ?>" value="<?= $row['vnd'] ?>"></td>
    <td><input class="data track" id="t<?= $row['id'] ?>" value="<?= $row['tongnap'] ?>"></td>
    <td>
        <button class="btn-del" onclick="deleteAccount(<?= $row['id'] ?>, '<?= htmlspecialchars($row['username']) ?>')">🗑</button>
    </td>
</tr>
<?php endwhile; ?>

</table>
</div>

</div>

<script>
// ===== GETTER FUNCTIONS =====
const u=id=>document.getElementById("u"+id).value;
const p=id=>document.getElementById("p"+id).value;
const b=id=>document.getElementById("b"+id).value;
const a=id=>document.getElementById("a"+id).value;
const ac=id=>document.getElementById("ac"+id).value;
const v=id=>document.getElementById("v"+id).value;
const t=id=>document.getElementById("t"+id).value;
const pl=id=>document.getElementById("pl"+id).value;

// TRACK CHANGES
document.querySelectorAll(".track").forEach(input => {
    input.addEventListener("input", () => input.classList.add("changed"));
});

// POPUP SYSTEM
function showPopup(type, title, content){
    const popup = document.getElementById("savePopup");
    const box = document.querySelector(".popup-box");
    const html = document.getElementById("savePopupContent");
    
    box.className = "popup-box popup-" + type;
    let icon = type === "success" ? "✅" : (type === "error" ? "❌" : "⚠️");

    html.innerHTML = `
        <div class="popup-icon">${icon}</div>
        <div class="popup-title">${title}</div>
        <div class="popup-content">${content}</div>
    `;
    popup.style.display = "flex";
}

function closePopup(){ document.getElementById("savePopup").style.display = "none"; }

// SAVE LOGIC
async function saveAll() {
    const rows = document.querySelectorAll("#table tr");
    let success = 0, fail = 0, totalChanged = 0;

    for (let i = 2; i < rows.length; i++) {
        let row = rows[i];
        let changedInputs = row.querySelectorAll(".changed");
        if (changedInputs.length === 0) continue;

        totalChanged++;
        let idCell = row.querySelector(".id-cell");
        let id = idCell.innerText.trim();

        try {
            let res = await fetch(window.location.href, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    id: id, username: u(id), password: p(id), ban: b(id),
                    admin: a(id), active: ac(id), vnd: v(id), tongnap: t(id), player_name: pl(id)
                })
            });

            let json = await res.json();
            if (json.status === "success") {
                success++;
                changedInputs.forEach(el => el.classList.remove("changed"));
            } else {
                fail++;
                showPopup("error", "Lỗi tại ID " + id, json.message);
                return; // Dừng lại để người dùng sửa lỗi trùng
            }
        } catch (e) {
            fail++;
        }
    }

    if (totalChanged === 0) {
        showPopup("warning", "Không có thay đổi", "Bạn chưa sửa gì cả!");
    } else {
        showPopup("success", "Hoàn tất", `Thành công: <b>${success}</b><br>Thất bại: <b>${fail}</b>`);
    }
}
async function deleteAccount(id, user) {
    const actions = document.getElementById("popupActions");
    const confirmBtn = document.getElementById("confirmBtn");
    
    // 1. Hiển thị Popup hỏi xác nhận (Thay thế cho confirm của localhost)
    showPopup("warning", "Cảnh Báo", `Xóa vĩnh viễn tài khoản:<br> <b>${user}</b>?`);
    
    // Hiện hàng nút bấm
    actions.style.display = "flex";

    // 2. Định nghĩa hành động khi bấm nút "XÓA NGAY"
    confirmBtn.onclick = async () => {
        actions.style.display = "none"; // Ẩn nút đi để tránh bấm nhiều lần
        
        try {
            const response = await fetch(window.location.href, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ action: "delete", id: id })
            });

            const json = await response.json();

            if (json.status === "success") {
                // Hiệu ứng ẩn dòng
                const row = document.getElementById("row_" + id);
                if(row) {
                    row.style.background = "#fca5a5"; 
                    row.style.transition = "0.5s";
                    setTimeout(() => { row.style.display = "none"; }, 500);
                }
                
                // Hiện thông báo thành công (Popup đè lên cái cũ)
                showPopup("success", "Đã Xóa!", `Tài khoản ID ${id} đã bay màu.`);
            } else {
                showPopup("error", "Lỗi", json.message);
            }
        } catch (e) {
            showPopup("error", "Lỗi Kết Nối", "Không thể gọi server!");
        }
    };
}
async function addAccount(){
    try {
        let res = await fetch(window.location.href, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ action: "add" })
        });

        let json = await res.json();

        if(json.status === "success"){
            showPopup("success", "Thành công", json.message);
            setTimeout(() => location.reload(), 1000);
        } else {
            showPopup("error", "Lỗi", json.message);
        }
    } catch(e){
        showPopup("error", "Lỗi", "Không thể kết nối server!");
    }
}
// Chỉnh lại hàm closePopup một chút để reset nút bấm
function closePopup(){ 
    document.getElementById("savePopup").style.display = "none"; 
    document.getElementById("popupActions").style.display = "none"; // Ẩn nút đi khi đóng
}
function filterTable() {
    // 1. Lấy tất cả các ô input của dòng tìm kiếm
    const searchInputs = document.querySelectorAll(".search-row input");
    
    // 2. Lấy tất cả các dòng dữ liệu (loại trừ dòng tiêu đề và dòng tìm kiếm)
    // Dòng 0 là tiêu đề, dòng 1 là search-row, dữ liệu bắt đầu từ dòng 2
    const table = document.getElementById("table");
    const rows = Array.from(table.getElementsByTagName("tr")).slice(2);

    rows.forEach(row => {
        let isMatch = true;

        searchInputs.forEach((input, index) => {
            const filterValue = input.value.toLowerCase().trim();
            if (filterValue === "") return; // Nếu ô này trống thì không lọc cột này

            const cell = row.cells[index];
            if (!cell) return;

            let cellText = "";
            // Kiểm tra nếu ô chứa thẻ input (class data) thì lấy .value, ngược lại lấy .innerText
            const innerInput = cell.querySelector("input.data");
            if (innerInput) {
                cellText = innerInput.value;
            } else {
                cellText = cell.innerText;
            }

            // Nếu dữ liệu trong ô không chứa từ khóa tìm kiếm -> đánh dấu không khớp
            if (!cellText.toLowerCase().includes(filterValue)) {
                isMatch = false;
            }
        });

        // 3. Hiển thị hoặc ẩn dòng dựa trên kết quả lọc
        row.style.display = isMatch ? "" : "none";
    });
}
</script>
<div id="savePopup" class="popup-bg">
    <div class="popup-box">
        <div class="close-btn" onclick="closePopup()">✖</div>
        <div id="savePopupContent"></div>
        <div id="popupActions" style="margin-top:30px; display:none; gap:20px; justify-content:center;">
            <button id="confirmBtn" class="btn-confirm-del">XÓA NGAY</button>
            <button onclick="closePopup()" class="btn-cancel">Quay Lại</button>
        </div>
    </div>
</div>
</body>
</html>