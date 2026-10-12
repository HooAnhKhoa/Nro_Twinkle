<?php
session_start();
require_once "../config.php";

// Check Admin
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

if($_SERVER['REQUEST_METHOD'] === 'POST'){
    $data = json_decode(file_get_contents("php://input"), true);
    if(!$data) {
        echo json_encode(["status" => "error", "msg" => "Dữ liệu trống"]);
        exit;
    }

    // 1. Tạo mã ngẫu nhiên 6 ký tự (Chữ in hoa và số)
    $characters = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    $randomCode = '';
    for ($i = 0; $i < 6; $i++) {
        $randomCode .= $characters[rand(0, strlen($characters) - 1)];
    }

    // 2. Lấy ID tự tăng tiếp theo
    $res = $conn->query("SELECT MAX(id) as max_id FROM giftcode");
    $row = $res->fetch_assoc();
    $newId = ($row['max_id'] ?? 0) + 1;

    // 3. Chuẩn bị dữ liệu
    $code = $conn->real_escape_string($randomCode);
    $count_left = 9999;
    $detail = $conn->real_escape_string(json_encode($data, JSON_UNESCAPED_UNICODE));
    $expired = '2030-01-01 00:00:00';

    // 4. Insert trực tiếp
    $sql = "INSERT INTO giftcode (id, code, count_left, detail, expired) VALUES ($newId, '$code', $count_left, '$detail', '$expired')";

    if($conn->query($sql)){
        echo json_encode(["status" => "success", "code" => $code]);
    } else {
        echo json_encode(["status" => "error", "msg" => "Lỗi: " . $conn->error]);
    }
    exit;
}