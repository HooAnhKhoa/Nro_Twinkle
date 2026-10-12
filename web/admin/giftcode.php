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
if(empty($_SESSION['giftcode_csrf'])) $_SESSION['giftcode_csrf'] = bin2hex(random_bytes(32));

// ===== HANDLE AJAX REQUESTS =====
if($_SERVER['REQUEST_METHOD'] === 'POST'){
    $data = json_decode(file_get_contents("php://input"), true);
    if(!$data){
        echo json_encode(["status"=>"error", "msg"=>"Dữ liệu không hợp lệ"]);
        exit;
    }

    if(isset($data['action']) && in_array($data['action'], ['reload', 'reload_status'], true)){
        header('Content-Type: application/json; charset=utf-8');
        if(!hash_equals($_SESSION['giftcode_csrf'], $data['csrf'] ?? '')){
            echo json_encode(['status'=>'error', 'msg'=>'Phiên làm việc không hợp lệ']);
            exit;
        }
        if($data['action'] === 'reload'){
            $sql = "CREATE TABLE IF NOT EXISTS giftcode_reload_request (id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, loaded_at DATETIME NULL, code_count INT NULL)";
            if(!$conn->query($sql) || !$conn->query("INSERT INTO giftcode_reload_request () VALUES ()")){
                echo json_encode(['status'=>'error', 'msg'=>'Không thể gửi yêu cầu cập nhật']);
            } else {
                echo json_encode(['status'=>'success', 'id'=>$conn->insert_id]);
            }
        } else {
            $id = (int)($data['id'] ?? 0);
            $stmt = $conn->prepare("SELECT loaded_at, code_count FROM giftcode_reload_request WHERE id = ?");
            if(!$stmt){ echo json_encode(['status'=>'error', 'msg'=>'Không thể kiểm tra trạng thái']); exit; }
            $stmt->bind_param('i', $id);
            $stmt->execute();
            $result = $stmt->get_result()->fetch_assoc();
            echo json_encode($result ? ['status'=>'success', 'loaded'=>$result['loaded_at'] !== null, 'count'=>$result['code_count']] : ['status'=>'error', 'msg'=>'Không tìm thấy yêu cầu']);
            $stmt->close();
        }
        exit;
    }

    // --- XỬ LÝ XÓA ---
    if(isset($data['action']) && $data['action'] === 'delete'){
        $id = (int)$data['id'];
        if($conn->query("DELETE FROM giftcode WHERE id=$id")){
            echo json_encode(["status"=>"success"]);
        } else {
            echo json_encode(["status"=>"error", "msg"=>"Không thể xóa Giftcode này"]);
        }
        exit;
    }

    // --- XỬ LÝ LƯU (UPDATE) ---
    $old_id = (int)$data['old_id']; 
    $new_id = (int)$data['id'];    
    $code = $conn->real_escape_string($data['code']);
    $count_left = (int)$data['count_left'];
    $detail = $conn->real_escape_string($data['detail']);
    $expired = $conn->real_escape_string($data['expired']);

    // Kiểm tra trùng code (trừ chính nó)
    $checkDuplicate = $conn->query("SELECT id FROM giftcode WHERE code='$code' AND id != $old_id");
    if($checkDuplicate->num_rows > 0){
        echo json_encode(["status"=>"error", "msg"=>"Mã Giftcode [$code] đã tồn tại!"]);
        exit;
    }

    $sql = "UPDATE giftcode SET 
            id=$new_id,
            code='$code', 
            count_left='$count_left', 
            detail='$detail', 
            expired='$expired' 
            WHERE id=$old_id";

    if($conn->query($sql)){
        echo json_encode(["status"=>"success"]);
    } else {
        echo json_encode(["status"=>"error", "msg"=>"Lỗi database hoặc trùng ID"]);
    }
    exit;
}

// ===== LOAD DATA =====
$list = $conn->query("SELECT * FROM giftcode ORDER BY id ASC");
?>

<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>Quản lý Giftcode</title>
<link rel="icon" type="image/png" href="/img/icon.jpg">

<style>
/* ===== BODY & CONTAINER ===== */
body{margin:0;font-family:'Segoe UI';font-size:1.1em;background:linear-gradient(135deg,#e0f2fe,#bae6fd);}
.container{max-width:1300px;margin:auto;padding:15px;}

/* ===== BUTTON EFFECTS ===== */
.menu-buttons button, .action-box button{ position: relative; overflow: hidden; transition: all 0.2s ease; cursor:pointer; border:none; border-radius:10px; color:white; font-weight:600; }
.menu-buttons button:hover, .action-box button:hover{ transform: translateY(-2px) scale(1.05); box-shadow: 0 8px 20px rgba(0,0,0,0.25); }
.menu-buttons button:active, .action-box button:active{ transform: scale(0.95); box-shadow: 0 3px 10px rgba(0,0,0,0.2); }

/* ===== POPUP ===== */
.popup-bg{ position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); display:none; align-items:center; justify-content:center; z-index:9999; }
.popup-box{ width:90%; max-width:500px; padding:42px 36px; border-radius:18px; text-align:center; background:linear-gradient(145deg,#f8fafc,#e2e8f0); color:#1e293b; box-shadow: 0 20px 60px rgba(0,0,0,.4); animation:pop .25s ease; position:relative; }
.popup-icon{ font-size:84px; }
.popup-title{ font-size:40px; margin:10px 0; }
.popup-content{ font-size:32px; }
.popup-success .popup-icon{ color:#22c55e; }
.popup-error .popup-icon{ color:#ef4444; }
.popup-warning .popup-icon{ color:#f59e0b; }
@keyframes pop{ from{transform:scale(.8);opacity:0} to{transform:scale(1);opacity:1} }
.close-btn{ position:absolute; top:-18px; right:-10px; font-size:40px; font-weight:900; width:65px; height:65px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#ef4444; color:white; cursor:pointer; transition:.2s; }
.close-btn:hover{ transform:scale(1.15) rotate(90deg); background:#dc2626; }

/* ===== DETAIL MODAL ===== */
.detail-modal-bg { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.8); display: none; align-items: center; justify-content: center; z-index: 10000; }
.detail-modal-box { width: 90%; max-width: 900px; background: #f9fafb; padding: 30px; border-radius: 20px; box-shadow: 0 15px 40px rgba(0,0,0,0.5); display: flex; flex-direction: column; gap: 15px; }
.detail-modal-box h2 { margin-top: 0; color: #1e3a8a; font-size: 30px; border-bottom: 2px solid #2563eb; padding-bottom: 10px; }
.detail-textarea { width: 100%; height: 350px; padding: 20px; border: 1px solid #cbd5e1; border-radius: 12px; font-family: 'Courier New', monospace; font-size: 1.1em; box-sizing: border-box; resize: vertical; outline: none; background: #f0f9ff; color: #0f172a; transition: all 0.2s ease; }
.detail-textarea:focus { border-color: #3b82f6; box-shadow: 0 0 10px rgba(59,130,246,0.3); }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; }
.btn-save-detail { background: #2563eb; color: white; padding: 10px 25px; border-radius: 8px; border:none; cursor:pointer; font-weight:bold; font-size: 1em; }
.btn-save-detail:hover { background:#1e40af; }
.btn-close-detail { background: #64748b; color: white; padding: 10px 25px; border-radius: 8px; border:none; cursor:pointer; font-weight:bold; font-size: 1em; }
.btn-close-detail:hover { background:#475569; }
/* ===== CSS CHO NÚT XÁC NHẬN XÓA ===== */
.confirm-group { display: flex; justify-content: center; gap: 20px; margin-top: 25px; }
.confirm-btn { padding: 6px 15px; border-radius: 10px; border: none; font-size: 1.01em; font-weight: bold; cursor: pointer; transition: 0.2s; color: white; }
.confirm-yes { background: #ef4444; } /* Màu đỏ cho nút xóa */
.confirm-yes:hover { background: #dc2626; transform: scale(1.01); }
.confirm-no { background: #64748b; } /* Màu xám cho nút hủy */
.confirm-no:hover { background: #475569; transform: scale(1.01); }
/* ===== HEADER ===== */
.header-box{ display:flex; justify-content:space-between; align-items:center; background:white; padding:20px; border-radius:16px; box-shadow:0 10px 25px rgba(0,0,0,.15); margin-bottom:20px; }
.header-left{display:flex;align-items:center;gap:20px;}
.header-left img{width:200px;height:200px;object-fit:contain;}
.header-content{display:flex;flex-direction:column;gap:12px;}
.header-content h1{margin:0;color:#1e3a8a; line-height:1.2;}
.menu-buttons{display:flex;gap:10px;}
.menu-buttons button{ padding:8px 15px; font-size:1.2em; }
.home{background:#3b82f6;} .refresh{background:#22c55e;}
.action-box{ display:flex; justify-content:center; padding:15px; background:white; border-radius:12px; box-shadow:0 5px 15px rgba(0,0,0,.1); margin-bottom:15px;}
.save-all{background:#16a34a; padding:10px 20px; font-size:1.1em;}
.update-code{background:#f59e0b; padding:10px 20px; font-size:1.1em; margin-left:10px;}

/* ===== TABLE ===== */
.table-wrap{ overflow-x:auto; border-radius:12px; box-shadow:0 10px 25px rgba(0,0,0,.2); }
table{ width:100%; border-collapse:collapse; background:white; table-layout: fixed; } 
th, td{ padding:10px; text-align:center; overflow:hidden; word-wrap: break-word; }
th{ background:#2563eb; color:white; }
.search-row input{ width:100%; padding:6px; border-radius:6px; border:1px solid #93c5fd; box-sizing:border-box; }
input.data, .id-input{ border:none; background:transparent; text-align:center; font-weight:bold; width:100%; box-sizing:border-box; font-size: 1.14em; outline:none; }
td.id-cell{ background:#93c5fd; font-weight:bold; user-select:none; }
.changed{ background:#93c5fd !important; box-shadow: inset 0 0 5px rgba(0,0,0,0.1); }

/* ===== BUTTON DELETE & CREATE ===== */
.create-code { background: #2563eb; color: white; padding: 10px 20px; font-size: 1.1em; font-weight: 600; border-radius: 10px; border: none; cursor: pointer; transition: all 0.2s ease; margin-left: 10px; }
.btn-delete { background: #ef4444; color: white; border: none; border-radius: 6px; width: 35px; height: 35px; font-weight: bold; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; margin: auto; }
.btn-delete:hover { background: #dc2626; transform: scale(1.1); }
.row-fade-out { opacity: 0; transform: translateX(20px); transition: all 0.5s ease; }

.detail-input { cursor: pointer; padding: 6px 8px; border-radius: 6px; background: linear-gradient(90deg, #e0f7fa, #b2ebf2); color: #036d6d; font-weight: 500; text-align: left; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; transition: all 0.2s ease; }
.detail-input:hover { background: linear-gradient(90deg, #b2ebf2, #80deea); box-shadow: 0 2px 6px rgba(0,0,0,0.2); }
.detail-input.changed { border: 2px solid #22c55e; background: #d1fae5; }
</style>
</head>

<body>
<div class="container">
    <div class="header-box">
        <div class="header-left">
            <img src="/img/account.gif">
            <div class="header-content">
                <h1>Giftcode</h1>
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

    <div class="action-box">
        <button class="save-all" onclick="saveAll()">💾 Lưu Dữ Liệu</button>
        <button class="create-code" onclick="location.href='taocode.php'">🎁 Tạo Giftcode</button>
        <button class="update-code" id="updateCodeButton" onclick="updateCode()">🔄 Update Code</button>
    </div>

    <div class="table-wrap">
        <table id="table">
        <thead>
    <tr>
        <th style="width: 70px;">ID</th>          <th style="width: 120px;">Giftcode</th>   <th style="width: 70px;">Lượt</th>       <th>Nội Dung</th>                         <th style="width: 200px;">HSD</th>        <th style="width: 90px;">Xóa</th>         </tr>
                <tr class="search-row">
                    <td><input></td>
                    <td><input></td>
                    <td><input></td>
                    <td><input></td>
                    <td><input></td>
                    <td></td>
                </tr>
            </thead>
            <tbody>
                <?php while($row = $list->fetch_assoc()): ?>
                <tr data-oldid="<?= $row['id'] ?>">
                    <td class="id-cell">
                        <input class="id-input track" id="id<?= $row['id'] ?>" value="<?= $row['id'] ?>">
                    </td>
                    <td><input class="data track" id="code<?= $row['id'] ?>" value="<?= htmlspecialchars($row['code']) ?>"></td>
                    <td><input class="data track" id="left<?= $row['id'] ?>" value="<?= $row['count_left'] ?>"></td>
                    <td>
                        <input class="data track detail-input" 
                               id="det<?= $row['id'] ?>" 
                               value="<?= htmlspecialchars($row['detail']) ?>" 
                               readonly 
                               onclick="openDetailModal(this)"
                               title="<?= htmlspecialchars($row['detail']) ?>"
                               placeholder="Nhấn để sửa chi tiết">
                    </td>
                    <td><input class="data track" id="exp<?= $row['id'] ?>" value="<?= $row['expired'] ?>"></td>
                    <td>
                        <button class="btn-delete" onclick="deleteRow(this, <?= $row['id'] ?>)">✕</button>
                    </td>
                </tr>
                <?php endwhile; ?>
            </tbody>
        </table>
    </div>
</div>

<div id="savePopup" class="popup-bg">
    <div class="popup-box">
        <div class="close-btn" onclick="closePopup()">✖</div>
        <div id="savePopupContent"></div>
    </div>
</div>

<div id="detailModal" class="detail-modal-bg">
    <div class="detail-modal-box">
        <h2>Chỉnh sửa chi tiết</h2>
        <textarea id="modalTextArea" class="detail-textarea"></textarea>
        <div class="modal-footer">
            <button class="btn-close-detail" onclick="closeDetailModal()">Đóng</button>
            <button class="btn-save-detail" onclick="saveDetailFromModal()">Xác nhận</button>
        </div>
    </div>
</div>

<script>
let currentInput = null;
const giftcodeCsrf = <?= json_encode($_SESSION['giftcode_csrf']) ?>;

async function updateCode(){
    const button = document.getElementById('updateCodeButton');
    button.disabled = true;
    try {
        const send = async data => {
            const response = await fetch(window.location.pathname, {
                method:'POST', headers:{'Content-Type':'application/json'},
                body:JSON.stringify({...data, csrf:giftcodeCsrf})
            });
            return response.json();
        };
        const request = await send({action:'reload'});
        if(request.status !== 'success') throw new Error(request.msg || 'Không gửi được yêu cầu');
        for(let attempt = 0; attempt < 15; attempt++){
            await new Promise(resolve => setTimeout(resolve, 1000));
            const result = await send({action:'reload_status', id:request.id});
            if(result.status !== 'success') throw new Error(result.msg || 'Không kiểm tra được trạng thái');
            if(result.loaded){
                showPopup('success', 'Đã cập nhật', `Server đã nạp ${result.count} giftcode. Người chơi có thể nhập ngay.`);
                return;
            }
        }
        throw new Error('Server chưa xác nhận nạp code. Kiểm tra server đã chạy mã Java mới.');
    } catch(error){
        showPopup('error', 'Cập nhật thất bại', error.message);
    } finally {
        button.disabled = false;
    }
}

// ===== SEARCH LOGIC =====
const searchInputs = document.querySelectorAll(".search-row input");
searchInputs.forEach(input => {
    input.addEventListener("input", filterTable);
});

function filterTable() {
    const rows = document.querySelectorAll("#table tbody tr");
    rows.forEach(row => {
        const cells = row.querySelectorAll("td");
        let show = true;
        searchInputs.forEach((input, colIndex) => {
            const filter = input.value.toLowerCase().trim();
            if (!filter) return;
            let target = cells[colIndex].querySelector("input");
            let text = target ? target.value.toLowerCase() : cells[colIndex].innerText.toLowerCase();
            if (!text.includes(filter)) show = false;
        });
        row.style.display = show ? "" : "none";
    });
}

// ===== DELETE LOGIC =====
// ===== DELETE LOGIC =====
let pendingDelete = null; 

function deleteRow(btn, id) {
    // Lấy tên Giftcode từ ô input cùng hàng
    const row = btn.closest('tr');
    const giftcodeName = row.querySelector("input[id^='code']").value;

    pendingDelete = { btn, id, name: giftcodeName };
    
    const content = `
        Xóa Giftcode: <br><b style="color:#ef4444">[${giftcodeName}]</b><br>
        <div class="confirm-group">
            <button class="confirm-btn confirm-yes" onclick="executeDelete()">XÓA NGAY</button>
                   <button class="confirm-btn confirm-no" onclick="closePopup()">HỦY</button>
        </div>
    `;
    showPopup("warning", "Cảnh Báo", content);
}
// Hàm thực hiện xóa thực tế sau khi bấm nút "XÓA NGAY"
async function executeDelete() {
    if(!pendingDelete) return;
    const { btn, id } = pendingDelete;
    closePopup(); // Đóng popup xác nhận trước

    try {
        let res = await fetch(window.location.href, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ action: 'delete', id: id })
        });
        let json = await res.json();
        if(json.status === "success"){
            let row = btn.closest('tr');
            row.classList.add('row-fade-out');
            setTimeout(() => row.remove(), 500);
        } else {
            showPopup("error", "Lỗi", json.msg);
        }
    } catch(e) { 
        showPopup("error", "Lỗi", "Kết nối máy chủ thất bại"); 
    }
    pendingDelete = null; // Xóa dữ liệu tạm
}
// ===== DETAIL MODAL LOGIC =====
function openDetailModal(inputElement) {
    currentInput = inputElement;
    document.getElementById("modalTextArea").value = inputElement.value;
    document.getElementById("detailModal").style.display = "flex";
}
function closeDetailModal() { document.getElementById("detailModal").style.display = "none"; }
function saveDetailFromModal() {
    if(currentInput) {
        const newVal = document.getElementById("modalTextArea").value;
        if(currentInput.value !== newVal) {
            currentInput.value = newVal;
            currentInput.classList.add("changed");
            currentInput.title = newVal;
        }
    }
    closeDetailModal();
}

// ===== POPUP LOGIC =====
function showPopup(type, title, content){
    let popup = document.getElementById("savePopup");
    let box = document.querySelector(".popup-box");
    let html = document.getElementById("savePopupContent");
    box.className = "popup-box popup-" + type;
    let icon = type === "success" ? "✅" : (type === "error" ? "❌" : "⚠️");
    html.innerHTML = `<div class="popup-icon">${icon}</div><div class="popup-title">${title}</div><div class="popup-content">${content}</div>`;
    popup.style.display = "flex";
}
function closePopup(){ document.getElementById("savePopup").style.display = "none"; }

// ===== TRACK CHANGE =====
document.addEventListener('input', (e) => {
    if(e.target.classList.contains('track')) {
        e.target.classList.add("changed");
    }
});

// Đóng modal khi click ra ngoài
window.onclick = function(event) {
    if (event.target == document.getElementById("detailModal")) closeDetailModal();
    if (event.target == document.getElementById("savePopup")) closePopup();
}

// ===== SAVE ALL LOGIC =====
async function saveAll(){
    let rows = document.querySelectorAll("#table tbody tr");
    let success = 0, fail = 0;
    let isAnyChange = false;
    
    // Kiểm tra trùng code trên UI trước
    let seenCodes = new Set();
    let duplicateCode = null;
    for(let row of rows) {
        let codeInput = row.querySelector("input[id^='code']");
        if(!codeInput) continue;
        let val = codeInput.value.trim();
        if(seenCodes.has(val)) { duplicateCode = val; break; }
        seenCodes.add(val);
    }
    if(duplicateCode) {
        showPopup("error", "Trùng lặp", `Mã [<b>${duplicateCode}</b>] lặp lại trên danh sách!`);
        return;
    }

    for(let row of rows){
        let changedInputs = row.querySelectorAll(".changed");
        if(changedInputs.length === 0) continue;
        isAnyChange = true;

        let data = {
            old_id: row.getAttribute("data-oldid"),
            id: row.querySelector(".id-input").value,
            code: row.querySelector("input[id^='code']").value,
            count_left: row.querySelector("input[id^='left']").value,
            detail: row.querySelector(".detail-input").value,
            expired: row.querySelector("input[id^='exp']").value
        };

        try {
            let res = await fetch(window.location.href, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(data)
            });
            let json = await res.json();
            if(json.status === "success"){
                success++;
                row.setAttribute("data-oldid", data.id);
                changedInputs.forEach(i => i.classList.remove("changed"));
            } else {
                fail++;
                showPopup("error", "Thất bại", json.msg);
                return;
            }
        } catch(e) { fail++; }
    }

    if(!isAnyChange) showPopup("warning", "Chú ý", "Không có gì để lưu");
    else if(fail === 0) showPopup("success", "Xong", `Đã cập nhật ${success} dòng.`);
}
</script>
</body>
</html>
