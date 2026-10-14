<?php
require_once 'panel_common.php';
$message='';
if($_SERVER['REQUEST_METHOD']==='POST') {
    if(!is_string($_POST['csrf']??null)||!hash_equals($_SESSION['panel_csrf'],$_POST['csrf'])){http_response_code(403);exit('Phiên thao tác không hợp lệ');}
    if(($_POST['confirmation']??'')!=='RESET'){$message='Nhập RESET để xác nhận.';}
    else try {
        $socket=@fsockopen('127.0.0.1',14445,$errno,$errstr,.4);
        if($socket){fclose($socket);throw new RuntimeException('Server đang chạy. Bảo trì và dừng server trước khi reset.');}
        $online=(int)$conn->query('SELECT COUNT(*) AS n FROM account WHERE last_time_login>last_time_logout')->fetch_assoc()['n'];
        if($online)throw new RuntimeException('Vẫn có tài khoản đánh dấu online. Kiểm tra server trước khi reset.');
        $tables=['account','clan','player','history_transaction','super_rank'];$prefix='panel_backup_'.date('Ymd_His').'_';
        $engines=$conn->query("SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('account','clan','player','history_transaction','super_rank')")->fetch_all(MYSQLI_ASSOC);
        if(count($engines)!==count($tables))throw new RuntimeException('Thiếu bảng dữ liệu; không thực hiện reset.');
        foreach($engines as $table)if(strcasecmp($table['ENGINE'],'InnoDB')!==0)throw new RuntimeException('Reset yêu cầu các bảng InnoDB để đảm bảo hoàn tác khi lỗi.');
        foreach($tables as $table)$conn->query("CREATE TABLE `$prefix$table` LIKE `$table`");
        $conn->begin_transaction();
        foreach($tables as $table)$conn->query("INSERT INTO `$prefix$table` SELECT * FROM `$table`");
        foreach($tables as $table)$conn->query('DELETE FROM `'.$table.'`'.($table==='account'?' WHERE is_admin != 1':''));$conn->commit();
        $_SESSION['reset_flash']='Đã reset. Bản sao lưu trong database: '.$prefix.'*. Giữ lại tài khoản admin.';header('Location: reset.php');exit;
    }catch(Throwable $e){$conn->rollback();$message=$e instanceof RuntimeException?$e->getMessage():'Reset không thành công. Không tiếp tục nếu thiếu bảng hoặc sao lưu lỗi.';}
}
if(isset($_SESSION['reset_flash'])){$message=$_SESSION['reset_flash'];unset($_SESSION['reset_flash']);}
?><!doctype html><html lang="vi"><head><meta charset="utf-8"><link rel="stylesheet" href="panel.css"><style>body{padding:22px;background:white}input{padding:10px;border:1px solid #b9cedd;margin:10px}</style><link rel="stylesheet" href="/typography.css?v=20261014-1"></head><body><h1>🔄 Reset dữ liệu</h1><p class="hint">Xóa nhân vật, clan, giao dịch, xếp hạng và tài khoản thường. Mỗi bảng được sao lưu nguyên trạng trong database trước khi xóa. Server phải dừng và không còn tài khoản online.</p><p class="error"><?=panelEscape($message)?></p><form method="post"><input type="hidden" name="csrf" value="<?=panelEscape($_SESSION['panel_csrf'])?>"><label>Nhập RESET <input name="confirmation" required pattern="RESET" autocomplete="off"></label><button class="primary" onclick="return confirm('Bạn xác nhận sao lưu và reset dữ liệu game?')">Sao lưu và Reset</button></form><p class="hint">Hoàn tác bằng bản sao lưu phải thực hiện khi server dừng. Hướng dẫn nằm trong tài liệu triển khai của dự án.</p></body></html>
