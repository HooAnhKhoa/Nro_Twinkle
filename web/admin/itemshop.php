<?php
session_start();
require_once "../config.php";

// ===== CHECK ADMIN =====
if(!isset($_SESSION['user'])){ header("Location: /admin/panel.php"); exit; }
$userEsc = $conn->real_escape_string($_SESSION['user']);
$q = $conn->query("SELECT is_admin FROM account WHERE username='$userEsc'");
$check = $q->fetch_assoc();
if(!$check || $check['is_admin'] != 1){ echo "Không có quyền!"; exit; }
?>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản Lý Cửa Hàng</title>
    <link rel="icon" type="image/png" href="/img/icon.jpg">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        body {
            margin: 0; font-family: 'Segoe UI'; font-size: 1.1em;
            background: linear-gradient(135deg, #e0f2fe, #bae6fd);
            min-height: 100vh;
        }
        .container { max-width: 1300px; margin: auto; padding: 15px; }

        /* ===== HEADER BOX (Giữ nguyên cấu trúc gốc) ===== */
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
            padding: 8px 15px; border: none; border-radius: 10px; color: white;
            font-weight: 600; cursor: pointer; font-size: 1.1em;
            position: relative; overflow: hidden; transition: all 0.2s ease;
        }
        .home { background: #3b82f6; }
        .refresh { background: #22c55e; }

        .menu-buttons button:hover {
            transform: translateY(-2px) scale(1.05);
            box-shadow: 0 8px 20px rgba(0,0,0,0.25);
        }
        .menu-buttons button:active {
            transform: scale(0.95);
        }

        /* ===== SHOP SELECTION AREA ===== */
        .shop-selection {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 25px;
            margin-top: 10px;
        }

        .shop-item {
            background: white;
            border-radius: 20px;
            padding: 50px 20px;
            text-align: center;
            text-decoration: none;
            color: #1e3a8a;
            box-shadow: 0 10px 25px rgba(0,0,0,0.1);
            transition: all 0.3s ease;
            display: flex;
            flex-direction: column;
            align-items: center;
            border: 4px solid transparent;
        }

        .shop-item:hover {
            transform: translateY(-8px);
            border-color: #2563eb;
            background: #f8fafc;
            box-shadow: 0 15px 35px rgba(0,0,0,0.15);
        }

        .icon-container {
            display: flex;
            gap: 15px;
            margin-bottom: 20px;
            justify-content: center;
            flex-wrap: wrap;
        }

        .icon-container img {
            width: 70px;
            height: 70px;
            object-fit: contain;
            image-rendering: pixelated;
        }

        .shop-item h2 {
            margin: 15px 0 5px 0;
            font-size: 1.7em;
            font-weight: 800;
        }

        .shop-item span {
            color: #64748b;
            font-size: 1.1em;
        }

        .header-right img { width: 500px; height: 200px; object-fit: contain; }
    </style>
<link rel="stylesheet" href="/typography.css?v=20261014-1"></head>
<body>
<div class="container">
    <div class="header-box">
        <div class="header-left">
            <img src="/img/account.gif">
            <div class="header-content">
                <h1>Shop</h1> 
                <div class="menu-buttons">
                    <button class="home" onclick="location.href='/admin/panel.php'">🏠 Go Panel</button>
                    <button class="refresh" onclick="location.reload()">🔄 Làm Mới</button>
                </div>
            </div>
        </div>
        <div class="header-right">
            <img src="/img/vip.jpg">
        </div>
    </div>

    <div class="shop-selection">
        
        <a href="ngoc.php" class="shop-item">
            <div class="icon-container">
                <img src="/img/930.png" onerror="this.src='/icon/930.png'">
                <img src="/img/932.png" onerror="this.src='/icon/932.png'">
            </div>
            <h2>Shop Vàng, Ngọc</h2>
            <span>Chỉnh sửa vật phẩm bán bằng vàng và ngọc</span>
        </a>

        <a href="vang.php" class="shop-item">
            <div class="icon-container">
                <img src="/icon/4028.png">
                <img src="/icon/14116.png">
                <img src="/icon/15485.png">
                <img src="/icon/7223.png">
            </div>
            <h2>Shop Thỏi Vàng</h2>
            <span>Vật Phẩm Đặc Biệt & Thỏi Vàng</span>
        </a>

    </div>
</div>
</body>
</html>