<?php
session_start();
session_destroy();

// Kiểm tra nếu người dùng nhấn "Đăng nhập lại" (relogin)
if (isset($_GET['action']) && $_GET['action'] == 'relogin') {
    // Quay lại chính trang panel.php để hiện form login
    header("Location: /dang-nhap.php");
} else {
    // Nếu là Đăng xuất bình thường, về trang chủ
    header("Location: /index.php");
}
exit;
?>
