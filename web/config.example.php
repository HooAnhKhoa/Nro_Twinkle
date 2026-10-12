<?php
// ===== DATABASE CONFIG =====
define("DB_HOST", "localhost");
define("DB_USER", "root");
define("DB_PASS", "");
define("DB_NAME", "srcnrofree");

// ===== CONNECT (chỉ tạo 1 lần) =====
if (!isset($conn)) {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

    if ($conn->connect_error) {
        die("Kết nối database thất bại: " . $conn->connect_error);
    }

    $conn->set_charset("utf8mb4"); // chuẩn hơn utf8
}
?>