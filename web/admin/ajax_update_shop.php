<?php
session_start();
require_once '../config.php';
header('Content-Type: application/json; charset=utf-8');

if (!isset($_SESSION['user'])) {
    http_response_code(403);
    echo json_encode(['status' => 'error', 'msg' => 'Bạn chưa đăng nhập']);
    exit;
}
$stmt = $conn->prepare('SELECT is_admin FROM account WHERE username = ? LIMIT 1');
$stmt->bind_param('s', $_SESSION['user']);
$stmt->execute();
$admin = $stmt->get_result()->fetch_assoc();
$stmt->close();
if (!$admin || (int)$admin['is_admin'] !== 1) {
    http_response_code(403);
    echo json_encode(['status' => 'error', 'msg' => 'Không có quyền admin']);
    exit;
}

$data = json_decode(file_get_contents('php://input'), true);
if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !is_array($data) ||
    !hash_equals($_SESSION['shop_csrf'] ?? '', $data['csrf'] ?? '')) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'msg' => 'Yêu cầu không hợp lệ']);
    exit;
}

if (($data['action'] ?? '') === 'reload') {
    $sql = 'CREATE TABLE IF NOT EXISTS shop_reload_request (id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, loaded_at DATETIME NULL, shop_count INT NULL)';
    if (!$conn->query($sql) || !$conn->query('INSERT INTO shop_reload_request () VALUES ()')) {
        echo json_encode(['status' => 'error', 'msg' => 'Không thể gửi yêu cầu cập nhật shop']);
    } else {
        echo json_encode(['status' => 'success', 'id' => $conn->insert_id]);
    }
} elseif (($data['action'] ?? '') === 'status') {
    $id = (int)($data['id'] ?? 0);
    $stmt = $conn->prepare('SELECT loaded_at, shop_count FROM shop_reload_request WHERE id = ?');
    if (!$stmt) {
        echo json_encode(['status' => 'error', 'msg' => 'Không thể kiểm tra trạng thái']);
        exit;
    }
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $row = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    echo json_encode($row ? ['status' => 'success', 'loaded' => $row['loaded_at'] !== null, 'count' => (int)$row['shop_count']] : ['status' => 'error', 'msg' => 'Không tìm thấy yêu cầu']);
} else {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'msg' => 'Thao tác không hợp lệ']);
}
