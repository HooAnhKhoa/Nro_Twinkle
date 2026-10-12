<?php
session_start();
include "../config.php";

if (empty($_SESSION['user'])) {
    header('Location: /dang-nhap.php');
    exit;
}
$authStmt = $conn->prepare('SELECT is_admin FROM account WHERE username = ? LIMIT 1');
$authStmt->bind_param('s', $_SESSION['user']);
$authStmt->execute();
$authAccount = $authStmt->get_result()->fetch_assoc();
$authStmt->close();
if (!$authAccount) {
    unset($_SESSION['user'], $_SESSION['username'], $_SESSION['user_id'], $_SESSION['id']);
    header('Location: /dang-nhap.php');
    exit;
}
if ((int)$authAccount['is_admin'] !== 1) {
    header('Location: /forum.php');
    exit;
}

$isLogin = false;
$isAdmin = false;
$user = "";
$balance = 0;
$error = "";
$showPopup = false;
$popupMsg = "";
$statusPopup = "success"; 

// Kiểm tra thông báo từ Session (Chống lặp khi F5)
if (isset($_SESSION['success_flash'])) {
    $showPopup = true;
    $popupMsg = $_SESSION['success_flash'];
    unset($_SESSION['success_flash']); // Xóa ngay sau khi nhận để F5 không hiện lại
}

$p_name = "Còn Zin";
$p_sm = "0";
$p_avatar = "../img/td.jpg"; 

function getAvatar($gender) {
    return match((int)$gender){
        0 => "../img/td.jpg",
        1 => "../img/nm.jpg",
        2 => "../img/xd.jpg",
        default => "../img/td.jpg"
    };
}

// Lấy thông tin User & Admin
if(isset($_SESSION['user'])){
    $user = $_SESSION['user'];
    $isLogin = true;
    
    $sql = "SELECT a.id as acc_id, a.is_admin, a.vnd, p.name, p.gender, p.data_point 
            FROM account a 
            LEFT JOIN player p ON a.id = p.account_id 
            WHERE a.username = '$user'";
            
    $q = $conn->query($sql);
    $data = $q->fetch_assoc();
    
    if($data) {
        if($data['is_admin'] == 1) $isAdmin = true;
        $balance = $data['vnd'];
        if(!empty($data['name'])){
            $p_name = htmlspecialchars($data['name']);
            $p_avatar = getAvatar($data['gender']); 

            if(!empty($data['data_point'])){
                $point_arr = json_decode($data['data_point'], true);
                if(is_array($point_arr) && isset($point_arr[1])){
                    $p_sm = number_format($point_arr[1]);
                }
            }
        }
    }
}

// Xử lý RESET DỮ LIỆU (Cải tiến với Redirect)
if(isset($_POST['exec_reset']) && $isAdmin) {
    $conn->query("DELETE FROM account WHERE is_admin != 1");
    $conn->query("DELETE FROM clan");
    $conn->query("DELETE FROM player");
    $conn->query("DELETE FROM history_transaction");
    $conn->query("DELETE FROM super_rank");
    
    // Lưu thông báo vào session và chuyển hướng để tránh lặp khi F5
    $_SESSION['success_flash'] = "Hệ thống đã dọn dẹp toàn bộ dữ liệu thành công! Chỉ giữ lại tài khoản Admin.";
    header("Location: " . $_SERVER['PHP_SELF']);
    exit;
}
?>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Panel Web </title>
    <link rel="icon" type="image/jpg" href="../img/icon.jpg">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
    
    <style>
        :root {
            --blue: #3b82f6; --blue2: #2563eb;
            --green: #22c55e; --green2: #16a34a;
            --red: #f43f5e; --red2: #e11d48;
            --card-dark: linear-gradient(160deg, #4a4a4a, #2b2b2b 65%, #1b1b1b);
            --bg-gradient: linear-gradient(135deg, #bae6fd, #60a5fa);
        }

        * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
        body { font-family: 'Plus Jakarta Sans', sans-serif; background: var(--bg-gradient); min-height: 100vh; display: flex; justify-content: center; padding: 20px 0; font-size: 19.2px; }

        /* --- POPUP MODERN STYLES --- */
        .modal-overlay {
            position: fixed; inset: 0; background: rgba(0,0,0,0.8);
            display: none; justify-content: center; align-items: center; z-index: 9999;
            backdrop-filter: blur(8px);
        }
        .modal-card {
            background: #fff; color: #1e293b; width: 90%; max-width: 420px;
            padding: 35px; border-radius: 30px; text-align: center;
            box-shadow: 0 25px 60px rgba(0,0,0,0.6);
            transform: scale(0.7); opacity: 0; transition: 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            position: relative;
        }
        .modal-overlay.active { display: flex; }
        .modal-overlay.active .modal-card { transform: scale(1); opacity: 1; }
        
        .modal-icon { font-size: 70px; margin-bottom: 20px; }
        .status-success .modal-icon { color: var(--green); }
        .status-error .modal-icon { color: var(--red); }
        .status-warning .modal-icon { color: #f59e0b; }

        .modal-title { font-weight: 900; font-size: 26px; margin-bottom: 12px; color: #1e3a8a; }
        .modal-desc { font-size: 16px; line-height: 1.6; margin-bottom: 30px; color: #64748b; font-weight: 500; }
        
        .modal-btn-group { display: flex; gap: 15px; justify-content: center; }
        .btn-modal { 
            padding: 14px 28px; border: none; border-radius: 15px; 
            font-weight: 800; cursor: pointer; transition: 0.2s; font-size: 15px; text-transform: uppercase;
        }
        .btn-confirm-red { background: var(--red); color: white; box-shadow: 0 4px 15px rgba(244, 63, 94, 0.4); }
        .btn-confirm-green { background: var(--green); color: white; width: 100%; box-shadow: 0 4px 15px rgba(34, 197, 94, 0.4); }
        .btn-cancel { background: #f1f5f9; color: #475569; }
        .btn-modal:hover { transform: translateY(-3px); filter: brightness(1.1); }
        .btn-modal:active { transform: scale(0.95); }

        /* --- CẤU TRÚC CHÍNH --- */
        .main-wrapper {
            width: 100%; max-width: 720px; background: var(--card-dark); border-radius: 28px; padding: 22px;
            box-shadow: 0 15px 45px rgba(0,0,0,.4); border: 1px solid rgba(255,255,255,0.12); color: white; margin: 0 12px; position: relative;
        }
        .panel-blur { filter: blur(12px); pointer-events: none; }
        .header { text-align: center; margin-bottom: 25px; }
        .logo-text { font-size: 33.6px; font-weight: 900; letter-spacing: 1px; margin-bottom: 15px;}

        .user-info-bar {
            margin-top: 10px; background: rgba(0,0,0,0.35); padding: 15px; border-radius: 20px;
            display: flex; align-items: center; gap: 15px; position: relative;
        }
        .user-avatar-circle { width: 70px; height: 70px; border-radius: 50%; border: 2px solid var(--blue); overflow: hidden; background: #000; flex-shrink: 0; }
        .user-avatar-circle img { width: 100%; height: 100%; object-fit: contain; }
        .user-details { flex-grow: 1; display: flex; flex-direction: column; gap: 2px; }
        .info-row { display: flex; align-items: center; gap: 5px; font-size: 16px; font-weight: 700; }
        .btn-logout-mini { position: absolute; top: 10px; right: 10px; padding: 5px 10px; border-radius: 10px; background: rgba(244, 63, 94, 0.15); color: #fda4af; text-decoration: none; font-size: 11px; font-weight: 800; transition: 0.2s; }
        
        .section-box { background: rgba(0,0,0,0.2); border-radius: 22px; padding: 18px; margin-bottom: 20px; border: 1px solid rgba(255,255,255,0.05); }
        .section-title { font-size: 14px; font-weight: 900; text-transform: uppercase; letter-spacing: 2px; margin: -8px auto 18px auto; display: flex; align-items: center; justify-content: center; gap: 12px; width: fit-content; padding: 8px 25px; background: linear-gradient(180deg, rgba(255,255,255,0.08) 0%, rgba(255,255,255,0.02) 100%); border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 50px; }

        .grid-menu { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; }
        .grid-3 { grid-template-columns: repeat(3, 1fr); }

        .item-card {
            padding: 15px 5px; text-align: center; text-decoration: none; color: white;
            display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; cursor: pointer; 
            position: relative; overflow: hidden; transition: 0.2s; background: transparent !important;
            border: none; min-height: 80px; filter: url(#fracture_optimized); will-change: transform;
        }

        .item-card:nth-child(3n+1) { clip-path: polygon(25% 0%, 75% 0%, 100% 50%, 75% 100%, 25% 100%, 0% 50%); }
        .item-card:nth-child(3n+2) { clip-path: polygon(50% 0%, 90% 20%, 100% 60%, 75% 100%, 25% 100%, 0% 60%, 10% 20%); }
        .item-card:nth-child(3n+3) { clip-path: polygon(30% 0%, 70% 0%, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0% 70%, 0% 30%); }

        .admin-row-end { grid-column: span 3; display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; margin-top: 5px; }
        .item-card-half { clip-path: polygon(10% 0%, 90% 0%, 100% 50%, 90% 100%, 10% 100%, 0% 50%) !important; }

        .item-card::before { content: ""; position: absolute; inset: 0; background: var(--shard-color); z-index: 0; transition: opacity 0.2s; }
        .item-card i { position: relative; z-index: 5; font-size: 24px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5)); }
        
        /* CẬP NHẬT CHỮ DỄ NHÌN HƠN */
        .item-card span { 
            position: relative; 
            z-index: 5; 
            font-size: 12px; /* Tăng size chữ */
            font-weight: 900; 
            text-transform: uppercase; 
            letter-spacing: 0.8px; 
            text-shadow: 2px 2px 4px rgba(0,0,0,0.9); /* Bóng đổ đậm hơn */
            background: rgba(0,0,0,0.3); /* Nền chữ tối hơn để nổi bật */
            padding: 4px 8px; 
            border-radius: 6px;
            display: inline-block;
        }

        .shards { position: absolute; inset: 0; z-index: 1; pointer-events: none; }
        .shard { position: absolute; inset: 0; background: var(--shard-color); transition: transform 0.3s ease-out; }
        .shard-1 { clip-path: polygon(0 0, 100% 0, 50% 100%); }
        .shard-2 { clip-path: polygon(50% 0, 100% 100%, 0 100%); filter: brightness(0.85); }
        
        .item-card:hover:not(.locked) { transform: translateY(-5px) scale(1.05); }
        .item-card:hover:not(.locked) .shard-1 { transform: translateY(-4px); }
        .item-card:hover:not(.locked) .shard-2 { transform: translateY(4px); }
        
        .bg-blue { --shard-color: linear-gradient(135deg, var(--blue), var(--blue2)); }
        .bg-green { --shard-color: linear-gradient(135deg, var(--green), var(--green2)); }
        .bg-red { --shard-color: linear-gradient(135deg, var(--red), var(--red2)); }
        .locked { opacity: 0.3; filter: grayscale(1) !important; pointer-events: none; }
    </style>
</head>
<body>

<div class="modal-overlay" id="popupModal">
    <div class="modal-card" id="modalCard">
        <div class="modal-icon" id="popupIcon"></div>
        <div class="modal-title" id="popupTitle"></div>
        <div id="popupDesc" class="modal-desc"></div>
        <div class="modal-btn-group" id="modalBtnGroup">
            </div>
    </div>
</div>

<svg width="0" height="0" style="position:absolute; visibility: hidden;">
  <filter id="fracture_optimized">
    <feTurbulence type="turbulence" baseFrequency="0.06" numOctaves="1" result="noise"/>
    <feDisplacementMap in="SourceGraphic" in2="noise" scale="3" />
  </filter>
</svg>

<div class="main-wrapper">
    <?php if(!$isLogin): ?>
    <div style="position: absolute; inset: 0; display: flex; justify-content: center; align-items: center; z-index: 1000; padding: 20px;">
        <div style="background: white; padding: 30px; border-radius: 24px; width: 90%; max-width: 340px; color: #1e293b; text-align: center; box-shadow: 0 10px 30px rgba(0,0,0,0.5);">
            <h2 style="font-weight: 900; margin-bottom: 10px;">ĐĂNG NHẬP</h2>
            <?php if($error) echo "<p style='color:red; font-size:14px; margin-bottom:10px;'>$error</p>"; ?>
            <form method="POST">
                <input type="text" name="username" placeholder="Tên tài khoản" style="width:100%; padding:12px; margin:8px 0; border-radius:12px; border:1.5px solid #e2e8f0;" required>
                <input type="password" name="password" placeholder="Mật khẩu" style="width:100%; padding:12px; margin:8px 0; border-radius:12px; border:1.5px solid #e2e8f0;" required>
                <button name="login" style="width:100%; padding:14px; border:none; border-radius:12px; background:var(--blue2); color:white; font-weight:800; cursor:pointer;">ĐĂNG NHẬP NGAY</button>
            </form>
        </div>
    </div>
    <?php endif; ?>

    <div class="<?php echo !$isLogin ? 'panel-blur' : ''; ?>">
        <div class="header">
            <div class="logo-text">
                <span style="color:var(--blue)">CON CHÓ,</span> 
                <span style="color:var(--red)">THẬT</span> 
                <span style="color:var(--green)">ĐÁNG YÊU</span>
            </div>
            
            <?php if($isLogin): ?>
            <div class="user-info-bar">
                <a href="logout.php" class="btn-logout-mini"><i class="fa-solid fa-power-off"></i> THOÁT</a>
                <div class="user-avatar-circle"><img src="<?php echo $p_avatar; ?>" alt="Avatar"></div>
                <div class="user-details">
                    <div class="info-row" style="color: var(--blue);">Nvật: <?php echo $p_name; ?></div>
                    <div class="info-row" style="color: var(--red);">Smạnh: <?php echo $p_sm; ?></div>
                    <div class="info-row" style="color: var(--green);">Số dư: <?php echo number_format($balance); ?> VNĐ</div>
                </div>
            </div>
            <?php endif; ?>
        </div>

        <div class="section-box">
            <div class="section-title">⚡ Đặc Quyền Player</div>
            <div class="grid-menu">
                <div class="item-card bg-blue" onclick="showSavingsNotice()" style="grid-column: span 2; flex-direction: row; justify-content: center; gap: 12px; clip-path: polygon(5% 0%, 95% 0%, 100% 50%, 95% 100%, 5% 100%, 0% 50%);">
                    <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                    <i class="fa-solid fa-piggy-bank"></i><span>GỬI TIẾT KIỆM</span>
                </div>
                <a href="../bxh01.php" class="item-card bg-red">
                    <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                    <i class="fa-solid fa-crown"></i><span>BXH Đua Top</span>
                </a>
                <a href="../bxh02.php" class="item-card bg-green">
                    <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                    <i class="fa-solid fa-medal"></i><span>BXH Tài Nguyên</span>
                </a>
            </div>
        </div>

        <div class="section-box">
            <div class="section-title">📊 Đặc Quyền ADMIN</div>
            <div class="grid-menu grid-3">
                <div class="item-card bg-red <?php echo !$isAdmin?'locked':''; ?>" onclick="handleGo('account.php')">
                    <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                    <i class="fa-solid fa-user-shield"></i><span>TÀI KHOẢN</span>
                </div>
                <div class="item-card bg-red <?php echo !$isAdmin?'locked':''; ?>" onclick="handleGo('player.php')">
                    <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                    <i class="fa-solid fa-id-badge"></i><span>NHÂN VẬT</span>
                </div>
                <div class="item-card bg-green <?php echo !$isAdmin?'locked':''; ?>" onclick="handleGo('giftcode.php')">
                    <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                    <i class="fa-solid fa-ticket"></i><span>GIFTCODE</span>
                </div>
                <div class="item-card bg-red <?php echo !$isAdmin?'locked':''; ?>" onclick="handleGo('item.php')">
                    <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                    <i class="fa-solid fa-box-open"></i><span>VẬT PHẨM</span>
                </div>
                <div class="item-card bg-green <?php echo !$isAdmin?'locked':''; ?>" onclick="handleGo('itemshop.php')">
                    <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                    <i class="fa-solid fa-shop"></i><span>CỬA HÀNG</span>
                </div>
                <div class="item-card bg-green <?php echo !$isAdmin?'locked':''; ?>" onclick="handleGo('map.php')">
                    <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                    <i class="fa-solid fa-map-location-dot"></i><span>BẢN ĐỒ</span>
                </div>
                
                <div class="admin-row-end">
                    <div class="item-card bg-blue item-card-half <?php echo !$isAdmin?'locked':''; ?>" onclick="handleGo('giaodich.php')">
                        <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                        <i class="fa-solid fa-handshake"></i><span>LỊCH SỬ</span>
                    </div>
                    
                    <form id="resetForm" method="POST" style="display:none;"><input type="hidden" name="exec_reset" value="1"></form>
                    <div class="item-card bg-blue item-card-half <?php echo !$isAdmin?'locked':''; ?>" onclick="confirmReset()">
                        <div class="shards"><div class="shard shard-1"></div><div class="shard shard-2"></div></div>
                        <i class="fa-solid fa-rotate"></i><span>RESET</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const modal = document.getElementById('popupModal');
    const modalBox = document.getElementById('modalCard');
    const popupIcon = document.getElementById('popupIcon');
    const popupTitle = document.getElementById('popupTitle');
    const popupDesc = document.getElementById('popupDesc');
    const btnGroup = document.getElementById('modalBtnGroup');

    // Hàm gọi Modal chung
    function showUniversalModal({status, title, desc, buttons}) {
        modalBox.className = "modal-card status-" + status;
        popupTitle.innerText = title;
        popupDesc.innerHTML = desc;
        
        let iconHTML = '';
        if(status === 'success') iconHTML = '<i class="fa-solid fa-circle-check"></i>';
        if(status === 'error') iconHTML = '<i class="fa-solid fa-circle-xmark"></i>';
        if(status === 'warning') iconHTML = '<i class="fa-solid fa-triangle-exclamation"></i>';
        popupIcon.innerHTML = iconHTML;

        btnGroup.innerHTML = '';
        buttons.forEach(btn => {
            const b = document.createElement('button');
            b.className = 'btn-modal ' + (btn.class || '');
            b.innerText = btn.text;
            b.onclick = () => {
                if(btn.onclick) btn.onclick();
                closePopup();
            };
            btnGroup.appendChild(b);
        });

        modal.classList.add('active');
    }

    function closePopup() { modal.classList.remove('active'); }
    function handleGo(url) { window.location.href = url; }

    function showSavingsNotice() {
        showUniversalModal({
            status: 'warning',
            title: 'THÔNG BÁO',
            desc: 'Game kh&ocirc;ng c&oacute; ch&#7913;c n&#259;ng n&#7841;p ti&#7873;n.<br>Trang n&agrave;y t&#7841;o ra &#273;&#7875; h&#7885;c t&#7853;p, free m&#7885;i v&#7853;t ph&#7849;m.',
            buttons: [{ text: 'ĐÃ HIỂU', class: 'btn-confirm-green' }]
        });
    }

    // Logic Reset
    function confirmReset() {
        showUniversalModal({
            status: 'warning',
            title: 'XÁC NHẬN RESET',
            desc: 'Xóa: Account, Clan, Player, Lịch Sử Giao Dịch, Super Rank...<br>Hành động này <b>KHÔNG THỂ</b> hoàn tác!',
            buttons: [
                { text: 'QUAY LẠI', class: 'btn-cancel' },
                { 
                    text: 'XÁC NHẬN XÓA', 
                    class: 'btn-confirm-red', 
                    onclick: () => document.getElementById('resetForm').submit() 
                }
            ]
        });
    }

    // Hiển thị thông báo từ PHP (Chỉ hiện 1 lần nhờ logic unset ở đầu file)
    <?php if($showPopup): ?>
    window.onload = () => {
        showUniversalModal({
            status: '<?php echo $statusPopup; ?>',
            title: 'THÀNH CÔNG',
            desc: '<?php echo $popupMsg; ?>',
            buttons: [{ text: 'TUYỆT VỜI', class: 'btn-confirm-green' }]
        });
    }
    <?php endif; ?>
</script>

</body>
</html>
