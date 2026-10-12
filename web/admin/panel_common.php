<?php
if (session_status() !== PHP_SESSION_ACTIVE) session_start();
require_once __DIR__.'/../config.php';
$auth=$conn->prepare('SELECT id,is_admin FROM account WHERE username=? LIMIT 1');
$username=$_SESSION['user']??'';
$auth->bind_param('s',$username); $auth->execute(); $panelAdmin=$auth->get_result()->fetch_assoc();
if (!$panelAdmin || (int)$panelAdmin['is_admin']!==1) {
    if ($_SERVER['REQUEST_METHOD']==='POST') {http_response_code(403);header('Content-Type: application/json');echo json_encode(['error'=>'Không có quyền quản trị']);}
    else {header('Location: /dang-nhap.php');} exit;
}
if (empty($_SESSION['panel_csrf'])) $_SESSION['panel_csrf']=bin2hex(random_bytes(32));
function panelReply($data,$code=200) {http_response_code($code);header('Content-Type: application/json; charset=utf-8');echo json_encode($data,JSON_UNESCAPED_UNICODE);exit;}
function panelInput() {
    $data=json_decode(file_get_contents('php://input'),true);
    if (!is_array($data)||!is_string($data['csrf']??null)||!hash_equals($_SESSION['panel_csrf'],$data['csrf'])) panelReply(['error'=>'Phiên thao tác không hợp lệ. Tải lại panel.'],403);
    return $data;
}
function panelSchema() {
    $sql=file_get_contents(__DIR__.'/../panel_schema.sql');
    foreach(explode(';',$sql) as $query) if(trim($query)!=='') $GLOBALS['conn']->query($query);
}
function panelEscape($value) {return htmlspecialchars((string)$value,ENT_QUOTES,'UTF-8');}
