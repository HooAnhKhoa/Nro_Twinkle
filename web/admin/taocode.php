<?php
session_start(); // Bắt buộc phải có để dùng $_SESSION
require_once "../config.php"; 

/**
 * KIỂM TRA QUYỀN ADMIN
 * Giả sử khi đăng nhập admin, bạn đã set $_SESSION['admin'] = true;
 * Hoặc bạn có thể check $_SESSION['id_admin'] tùy theo source code của bạn.
 */
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
// Lấy dữ liệu từ Database (Chỉ chạy khi đã qua bước check admin)
$items_res = $conn->query("SELECT id, NAME, icon_id FROM item_template");
$items = ($items_res && $items_res->num_rows > 0) ? $items_res->fetch_all(MYSQLI_ASSOC) : [];

$options_res = $conn->query("SELECT id, NAME FROM item_option_template");
$options = ($options_res && $options_res->num_rows > 0) ? $options_res->fetch_all(MYSQLI_ASSOC) : [];
?>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
   <title>Quản lý Giftcode</title>
<link rel="icon" type="image/png" href="/img/icon.jpg">
    <style>
        :root { --primary: #1d4ed8; --success: #059669; --danger: #dc2626; --bg: #f1f5f9; --dark: #0f172a; }
        * { box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, sans-serif; background: var(--bg); margin: 0; padding: 20px; color: #1e293b; }
        .container { max-width: 1300px; margin: 0 auto; background: white; padding: 25px; border-radius: 15px; box-shadow: 0 5px 25px rgba(0,0,0,0.1); }
        .item-card { border: 2px solid #cbd5e1; border-radius: 12px; padding: 20px; margin-bottom: 20px; position: relative; background: #fff; }
        .header-box { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; border-bottom: 2px solid #f1f5f9; padding-bottom: 15px; }
        
        .btn { border: none; border-radius: 10px; padding: 12px 24px; font-weight: 800; cursor: pointer; color: white; transition: 0.2s; display: inline-flex; align-items: center; justify-content: center; text-decoration: none; text-transform: uppercase; }
        .btn:hover { transform: translateY(-2px); filter: brightness(1.1); }
        .btn-add { background: var(--success); }
        .btn-submit { background: var(--primary); }
        .btn-back { background: #64748b; }

        .option-row { display: grid; grid-template-columns: 1fr 120px 40px; gap: 12px; margin-bottom: 12px; align-items: center; background: #f8fafc; padding: 8px; border-radius: 8px; border: 1px solid #e2e8f0; }
        .select-trigger { background: #fff; border: 2px solid #94a3b8; border-radius: 8px; padding: 14px; text-align: center; cursor: pointer; display: flex; align-items: center; justify-content: center; min-height: 60px; }
        
        input[type="number"], .opt-search-input { padding: 12px; border: 2px solid #94a3b8; border-radius: 8px; font-weight: 600; width: 100%; }
        label { font-size: 0.9rem; font-weight: 800; color: #475569; text-transform: uppercase; margin-bottom: 8px; display: block; }

        /* Style cho phần Demo */
        .demo-section { background: #f8fafc; border: 2px dashed #94a3b8; border-radius: 12px; padding: 20px; margin-top: 20px; }
        .demo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 15px; }
        .demo-item { background: #fff; border: 1px solid #cbd5e1; padding: 12px; border-radius: 10px; display: flex; gap: 12px; align-items: flex-start; }
        .demo-info { flex: 1; }
        .demo-name { font-weight: 800; color: var(--primary); font-size: 1rem; margin-bottom: 2px; }
        .demo-qty { font-weight: 700; font-size: 0.9rem; color: #334155; margin-bottom: 5px; border-bottom: 1px solid #eee; }
        .demo-opt-line { font-size: 0.85rem; color: #475569; line-height: 1.3; }
        .imp-text { color: var(--danger); font-weight: 700; }

        /* MODAL 6x7 CẤU HÌNH MỚI */
        .modal-overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.7); z-index: 1000; justify-content: center; align-items: center; backdrop-filter: blur(3px); }
        .modal-content { background: white; width: 95%; max-width: 1200px; height: 90vh; border-radius: 15px; overflow: hidden; display: flex; flex-direction: column; border: 3px solid var(--primary); }
        
        .icon-grid { 
            display: grid; 
            grid-template-columns: repeat(6, 1fr); /* 6 cột */
            gap: 10px; 
            padding: 15px; 
        }
        /* ===== POPUP CỦA TRANG 1 ===== */
.popup-bg { position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); display:none; align-items:center; justify-content:center; z-index:9999; }
.popup-box { width:90%; max-width:500px; padding:30px; border-radius:18px; text-align:center; background:linear-gradient(145deg,#f8fafc,#e2e8f0); color:#1e293b; box-shadow: 0 20px 60px rgba(0,0,0,.4); animation:pop .25s ease; position:relative; }
.popup-icon { font-size:60px; margin-bottom: 10px; }
.popup-title { font-size:28px; font-weight: 800; margin-bottom:10px; color: var(--dark); }
.popup-content { font-size:18px; font-weight: 500; line-height: 1.5; }
.popup-success .popup-icon { color:#22c55e; }
.popup-error .popup-icon { color:#ef4444; }
.popup-warning .popup-icon { color:#f59e0b; }
@keyframes pop { from{transform:scale(.8);opacity:0} to{transform:scale(1);opacity:1} }
.close-btn-popup { position:absolute; top:-15px; right:-15px; font-size:25px; font-weight:900; width:45px; height:45px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#ef4444; color:white; cursor:pointer; transition:.2s; border: none; }
.close-btn-popup:hover { transform:scale(1.1) rotate(90deg); }
        .icon-box { 
            border: 1px solid #e2e8f0; 
            padding: 10px; 
            text-align: center; 
            cursor: pointer; 
            border-radius: 8px; 
            transition: 0.2s; 
            background: white;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: space-between;
            min-height: 130px; /* Cố định độ cao để thành hàng 7 đẹp */
        }
        .icon-box:hover { border-color: var(--primary); background: #eff6ff; transform: translateY(-2px); }
        .icon-box img { width: 45px; height: 45px; object-fit: contain; margin-bottom: 5px; }
        .icon-box .it-name { 
            font-size: 12px; 
            font-weight: 700; 
            color: #1e293b;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            height: 32px;
            line-height: 1.3;
        }

        .opt-dropdown { position: absolute; top: 100%; left: 0; right: 0; z-index: 100; background: white; border: 2px solid var(--primary); max-height: 450px; overflow-y: auto; display: none; box-shadow: 0 10px 15px rgba(0,0,0,0.1); }
        .opt-item { padding: 8px 12px; cursor: pointer; border-bottom: 1px solid #eee; font-size: 0.9rem; }
        .opt-item:hover { background: #f0f7ff; }
        
        pre { background: var(--dark); color: #38bdf8; padding: 20px; border-radius: 8px; white-space: pre-wrap; border-left: 5px solid var(--primary); font-size: 1rem; }
    </style>
<link rel="stylesheet" href="/typography.css?v=20261014-1"><link rel="stylesheet" href="admin-ui.css?v=20261015-4"><script src="admin-ui.js?v=20261015-2" defer></script><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body class="admin-page" data-admin-page="taocode"><?php require_once __DIR__.'/admin_ui.php'; adminPageToolbar('Tạo Giftcode'); ?>

<div class="container">
    <div class="header-box">
        <div style="display: flex; gap: 10px;">
            <a href="giftcode.php" class="btn btn-back">⬅ TRỞ VỀ</a>
            <button type="button" class="btn btn-submit" onclick="location.reload();">🔄 LÀM MỚI</button>
        </div>
        <h2 style="margin:0; color: var(--primary); font-weight: 900;">Tạo Giftcode</h2>
        <div style="width:100px"></div>
    </div>
    
    <div id="itemList"></div>

    <div class="demo-section">
        <label>🔍 XEM VẬT PHẨM TRONG CODE:</label>
        <div id="demoGrid" class="demo-grid">
            <div style="color: #94a3b8; font-style: italic;">Chưa có vật phẩm...</div>
        </div>
    </div>

    <div style="margin-top: 25px; display: flex; gap: 20px;">
        <button type="button" class="btn btn-add" onclick="addItem()" style="flex: 1; padding: 15px;">+ THÊM VẬT PHẨM</button>
        <button type="button" class="btn btn-submit" onclick="generateJSON()" style="flex: 1; padding: 15px;">XUẤT GIFTCODE</button>
    </div>
    </div>
</div>
<div id="savePopup" class="popup-bg">
    <div class="popup-box">
        <button class="close-btn-popup" onclick="closePopup()">✖</button>
        <div id="savePopupContent"></div>
    </div>
</div>
<div class="modal-overlay" id="itemModal">
    <div class="modal-content">
        <div style="padding: 15px 20px; border-bottom: 2px solid #eee; display: flex; gap: 15px; align-items: center; background: #fff;">
            <b style="font-size: 1.1rem; white-space: nowrap;">CHỌN VẬT PHẨM</b>
            <input type="text" id="modalSearch" placeholder="Tìm tên hoặc ID..." onkeyup="filterItems()" style="flex:1; padding:10px 20px; border-radius:20px; border:2px solid var(--primary); outline:none;">
            <button class="btn btn-back" onclick="closeModal()" style="padding: 8px 15px; font-size: 13px;">ĐÓNG</button>
        </div>
        <div style="overflow-y: auto; flex: 1; background: #f8fafc;">
            <div class="icon-grid" id="modalGrid">
                <?php foreach($items as $it): ?>
                    <div class="icon-box" data-name="<?= strtolower($it['NAME']) ?>" data-id="<?= $it['id'] ?>"
                         onclick="confirmSelect(<?= $it['id'] ?>, '<?= addslashes($it['NAME']) ?>', <?= $it['icon_id'] ?>)">
                        <img src="/icon/<?= $it['icon_id'] ?>.png" loading="lazy" onerror="this.src='https://img.icons8.com/color/48/box--v1.png'">
                        <div class="it-name"><?= htmlspecialchars($it['NAME']) ?></div>
                        <code style="font-size: 11px; color: var(--primary); font-weight: bold;">ID: <?= $it['id'] ?></code>
                    </div>
                <?php endforeach; ?>
            </div>
        </div>
    </div>
</div>

<script>
    let itemCount = 0;
    let currentActiveCard = null;
    const optionData = <?= json_encode($options) ?>;
    const importantIds = [0, 6, 7, 30, 50, 77, 101, 103, 106];

    function addItem() {
        const id = itemCount++;
        const html = `
            <div class="item-card" id="card_${id}">
                <button type="button" style="position:absolute; right:15px; top:15px; color:var(--danger); border:none; background:none; cursor:pointer; font-weight:bold; font-size:1.1rem;" onclick="this.parentElement.remove(); updateDemo();">XÓA ✕</button>
                <div style="display: flex; gap: 25px;">
                    <div style="width: 280px;">
                        <label>Vật phẩm mục tiêu</label>
                        <input type="hidden" class="input_id">
                        <input type="hidden" class="input_name">
                        <input type="hidden" class="input_icon">
                        <div class="select-trigger" onclick="openModal(${id})" id="trigger_${id}">
                            <span style="color:#64748b; font-weight:600;">[ NHẤP ĐỂ CHỌN ]</span>
                        </div>
                        <label style="margin-top:15px;">Số lượng nhận</label>
                        <input type="number" class="input_quantity" min="1" placeholder="Số lượng..." oninput="updateDemo()">
                    </div>
                    <div style="flex: 1; border-left: 2px solid #f1f5f9; padding-left: 25px;">
                        <label>Chỉ số</label>
                        <div class="opt_list" id="opt_list_${id}"></div>
                        <button type="button" class="btn btn-back" style="font-size:12px; padding:6px 12px; margin-top:8px" onclick="addOption(${id})">+ Thêm chỉ số</button>
                    </div>
                </div>
            </div>`;
        document.getElementById('itemList').insertAdjacentHTML('beforeend', html);
    }

 function addOption(cardId) {
    const container = document.getElementById(`opt_list_${cardId}`);
    const html = `
        <div class="option-row">
            <div style="position:relative">
                <input type="text" class="opt-search-input" placeholder="Tìm tên chỉ số..." autocomplete="off" onfocus="showDropdown(this)" onkeyup="filterOptions(this)">
                <input type="hidden" class="opt_id">
                <input type="hidden" class="opt_template">
                <div class="opt-dropdown">
                    ${optionData.map(o => {
                        // Kiểm tra nếu ID nằm trong danh sách quan trọng
                        const isImp = importantIds.includes(parseInt(o.id));
                        const style = isImp ? 'style="color: var(--danger); font-weight: bold;"' : '';
                        return `<div class="opt-item" ${style} onclick="selectOption(this, ${o.id}, '${o.NAME.replace(/'/g, "\\'")}')">${o.NAME} (ID: ${o.id})</div>`;
                    }).join('')}
                </div>
            </div>
            <input type="number" class="opt_param" oninput="updateDemo()" placeholder="Giá trị">
            <button type="button" style="border:none; background:none; color:var(--danger); cursor:pointer; font-weight:bold; font-size:1.2rem;" onclick="this.parentElement.remove(); updateDemo();">✕</button>
        </div>`;
    container.insertAdjacentHTML('beforeend', html);
}

    function showDropdown(input) {
        document.querySelectorAll('.opt-dropdown').forEach(d => d.style.display = 'none');
        input.parentElement.querySelector('.opt-dropdown').style.display = 'block';
    }

  function selectOption(el, id, name) {
    const parent = el.closest('.option-row');
    const input = parent.querySelector('.opt-search-input');
    input.value = name;
    parent.querySelector('.opt_id').value = id;
    parent.querySelector('.opt_template').value = name;

    // Đổi màu chữ của input nếu là ID quan trọng
    if (importantIds.includes(parseInt(id))) {
        input.style.color = "var(--danger)";
        input.style.fontWeight = "bold";
    } else {
        input.style.color = "black";
        input.style.fontWeight = "normal";
    }

    el.parentElement.style.display = 'none';
    updateDemo();
}

    function updateDemo() {
        let html = '';
        document.querySelectorAll('.item-card').forEach(card => {
            const id = card.querySelector('.input_id').value;
            if(!id) return;

            const name = card.querySelector('.input_name').value;
            const qty = card.querySelector('.input_quantity').value || 0;
            const icon = card.querySelector('.input_icon').value;
            
            let optHtml = '';
            card.querySelectorAll('.option-row').forEach(row => {
                const oId = row.querySelector('.opt_id').value;
                const template = row.querySelector('.opt_template').value;
                const param = row.querySelector('.opt_param').value || '0';
                if(oId) {
                    const formattedText = template.includes('#') ? template.replace('#', param) : `${template}: ${param}`;
                    const isImp = importantIds.includes(parseInt(oId));
                    optHtml += `<div class="demo-opt-line ${isImp ? 'imp-text' : ''}">• ${formattedText}</div>`;
                }
            });

            if(optHtml === '') optHtml = '<i style="color:#94a3b8; font-size:0.8rem">Không có chỉ số</i>';

            html += `
                <div class="demo-item">
                    <img src="/icon/${icon}.png" width="45" height="45" style="object-fit:contain" onerror="this.src='https://img.icons8.com/color/48/box--v1.png'">
                    <div class="demo-info">
                        <div class="demo-name">${name}</div>
                        <div class="demo-qty">Số Lượng: <span style="color:var(--danger)">${qty}</span></div>
                        <div class="demo-options-list">${optHtml}</div>
                    </div>
                </div>`;
        });
        document.getElementById('demoGrid').innerHTML = html || '<div style="color: #94a3b8; font-style: italic;">Chưa có vật phẩm cấu hình...</div>';
    }
// --- HÀM ĐIỀU KHIỂN POPUP GIỐNG TRANG 1 ---
function showPopup(type, title, content, showBackBtn = false) {
    let popup = document.getElementById("savePopup");
    let box = popup.querySelector(".popup-box");
    let html = document.getElementById("savePopupContent");
    
    box.className = "popup-box popup-" + type;
    let icon = type === "success" ? "✅" : (type === "error" ? "❌" : "⚠️");
    
    let footerBtn = showBackBtn 
        ? `<br><br><a href="giftcode.php" class="btn btn-submit" style="background:var(--primary); width:100%">QUAY VỀ TRANG GIFTCODE</a>` 
        : '';

    html.innerHTML = `
        <div class="popup-icon">${icon}</div>
        <div class="popup-title">${title}</div>
        <div class="popup-content">${content} ${footerBtn}</div>
    `;
    popup.style.display = "flex";
}

function closePopup() { 
    document.getElementById("savePopup").style.display = "none"; 
}

// Đóng khi click ra ngoài popup
window.onclick = function(event) {
    if (event.target == document.getElementById("savePopup")) closePopup();
    if (event.target == document.getElementById("itemModal")) closeModal();
}


function generateJSON() {
    const result = [];
    const cards = document.querySelectorAll('.item-card');

    // 1. Kiểm tra xem đã bấm nút "Thêm vật phẩm" chưa
    if (cards.length === 0) {
        showPopup("warning", "NHẮC NHỞ", "Bạn chưa thêm vật phẩm nào vào danh sách!");
        return;
    }

    // Duyệt qua từng Card vật phẩm
    for (let i = 0; i < cards.length; i++) {
        const card = cards[i];
        const stt = i + 1;
        
        const itemId = card.querySelector('.input_id').value;
        const qty = card.querySelector('.input_quantity').value;
        const itemName = card.querySelector('.input_name').value || "Chưa chọn tên";

        // 2. Kiểm tra ID vật phẩm chính
        if (!itemId) {
            showPopup("error", "LỖI DỮ LIỆU", `Vật phẩm thứ <b>#${stt}</b> chưa được chọn!`);
            return;
        }

        // 3. Kiểm tra Số lượng
        if (!qty || parseInt(qty) <= 0) {
            showPopup("error", "LỖI SỐ LƯỢNG", `Vật phẩm <b>${itemName}</b> (#${stt}) chưa nhập số lượng!`);
            return;
        }

        const options = [];
        const optionRows = card.querySelectorAll('.option-row');
        
        // --- CHỐT CHẶN MỚI: KIỂM TRA PHẢI CÓ CHỈ SỐ ---
        if (optionRows.length === 0) {
            showPopup("error", "THIẾU CHỈ SỐ", `Vật phẩm <b>${itemName}</b> (#${stt}) chưa có chỉ số nào. Vui lòng bấm <b>+ Thêm chỉ số</b>!`);
            return;
        }

        // 4. Kiểm tra tính đầy đủ của từng dòng Chỉ số
        for (let j = 0; j < optionRows.length; j++) {
            const row = optionRows[j];
            const oId = row.querySelector('.opt_id').value;
            const oVal = row.querySelector('.opt_param').value;
            const rowStt = j + 1;

            if (!oId || oVal === "") {
                showPopup("error", "CHỈ SỐ TRỐNG", 
                    `Lỗi tại <b>${itemName}</b>:<br>Chỉ số dòng <b>${rowStt}</b> chưa chọn loại hoặc chưa nhập giá trị!`);
                return;
            }

            options.push({ 
                id: parseInt(oId), 
                param: parseInt(oVal) 
            });
        }

result.push({ 
    id: parseInt(itemId), 
    quantity: parseInt(qty), 
    options: options 
});
    }

    // 5. Gửi AJAX khi mọi thứ đã đầy đủ
    fetch('ajax_create_giftcode.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(result)
    })
    .then(r => r.json())
    .then(res => {
        if(res.status === 'success') {
            showPopup("success", "THÀNH CÔNG", 
                `Đã tạo Giftcode: <b style="color:var(--primary); font-size:40px; display:block; margin:10px 0;">${res.code}</b>
                <span style="font-size:16px; color:#64748b;">Hạn dùng: 2030 | Lượt: 9999</span>`, 
                true 
            );
        } else {
            showPopup("error", "LỖI SERVER", res.msg);
        }
    })
    .catch(err => showPopup("error", "LỖI", "Không thể kết nối Server!"));
}
// --- CẬP NHẬT HÀM COPY ---
function copyResult() {
    const text = document.getElementById('jsonOutput').innerText;
    navigator.clipboard.writeText(text).then(() => {
        showPopup("success", "Đã sao chép", "Mã JSON đã được lưu vào bộ nhớ tạm.");
    });
}
    function openModal(id) { currentActiveCard = id; document.getElementById('itemModal').style.display = 'flex'; }
    function closeModal() { document.getElementById('itemModal').style.display = 'none'; }
    function closeResult() { document.getElementById('resultArea').style.display = 'none'; }

    function confirmSelect(id, name, icon) {
        const trigger = document.getElementById(`trigger_${currentActiveCard}`);
        const card = trigger.closest('.item-card');
        card.querySelector('.input_id').value = id;
        card.querySelector('.input_name').value = name;
        card.querySelector('.input_icon').value = icon;
        trigger.innerHTML = `<div style="display:flex; align-items:center; gap:10px;"><img src="/icon/${icon}.png" width="30" height="30"><b>${name}</b></div>`;
        closeModal();
        updateDemo();
    }

    function filterItems() {
        const val = document.getElementById('modalSearch').value.toLowerCase();
        document.querySelectorAll('.icon-box').forEach(box => {
            box.style.display = (box.dataset.name + box.dataset.id).includes(val) ? 'flex' : 'none';
        });
    }

    function filterOptions(input) {
        const val = input.value.toLowerCase();
        input.parentElement.querySelectorAll('.opt-item').forEach(item => {
            item.style.display = item.innerText.toLowerCase().includes(val) ? 'block' : 'none';
        });
    }


    document.addEventListener('click', (e) => {
        if (!e.target.classList.contains('opt-search-input')) document.querySelectorAll('.opt-dropdown').forEach(d => d.style.display = 'none');
    });

    window.onload = addItem;
</script>
</body>
</html>