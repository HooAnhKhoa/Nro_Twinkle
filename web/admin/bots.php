<?php
require_once __DIR__.'/panel_common.php';
require_once __DIR__.'/bot_panel_lib.php';
if($_SERVER['REQUEST_METHOD']==='POST'){
    $data=panelInput();$action=$data['action']??'';
    try{
        $paths=require __DIR__.'/../game_paths.php';$root=realpath($paths['game_root']??'');
        if(!$root||!is_dir($root)||!is_file($root.'/Config.properties'))throw new RuntimeException('Chưa cấu hình đúng thư mục game.');
        if($action==='read')panelReply(botPanelRead($root,$conn));
        if(!in_array($action,['save','undo'],true))panelReply(['error'=>'Thao tác không hợp lệ.'],422);
        $result=botPanelChange($root,$action,$data,(int)$panelAdmin['id']);panelReply($result+botPanelRead($root,$conn));
    }catch(InvalidArgumentException $e){panelReply(['error'=>$e->getMessage()],422);}
    catch(RuntimeException $e){panelReply(['error'=>$e->getMessage()],409);}
    catch(Throwable $e){error_log($e->getMessage());panelReply(['error'=>'Không thực hiện được. Cấu hình cũ được giữ nếu chưa ghi thành công.'],500);}
}
?><!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Bot tự động</title><link rel="stylesheet" href="/typography.css?v=20261014-2"><link rel="stylesheet" href="admin-ui.css?v=20261015-5"><link rel="stylesheet" href="bots.css?v=1"><script src="admin-ui.js" defer></script></head><body class="admin-page"><?php require_once __DIR__.'/admin_ui.php';adminPageToolbar('Bot tự động'); ?>
<main class="bots-page"><header class="bots-heading"><div><h1>Bot tự động</h1><p>Luyện quái, nhặt đồ và săn boss · Tối đa 3 bot thử nghiệm</p></div><button id="refreshBots" type="button">↻ Làm mới</button></header>
<div class="bot-sync" id="sync" role="status">Đang kết nối…</div>
<section class="bot-metrics" aria-label="Tình trạng bot"><article><span>Đang chạy</span><strong id="running">—</strong></article><article><span>Luyện quái</span><strong id="farmCount">—</strong></article><article><span>Săn boss</span><strong id="huntCount">—</strong></article><article><span>Lỗi ghi nhận</span><strong id="errorCount">—</strong></article></section>
<section class="bot-controls"><div class="bot-section-title"><h2>Cấu hình nhóm bot</h2><label class="bot-enabled"><input id="enabled" type="checkbox"> Bật nhóm bot</label></div>
<form id="botForm"><div class="bot-types"><article class="bot-type farm"><div><h3>Luyện quái</h3><p>Tự đánh, nhặt đồ của mình và đổi map luyện.</p></div><div class="bot-stepper"><button type="button" data-field="farm" data-step="-1" aria-label="Bớt bot luyện quái">−</button><input id="farm" type="number" min="0" max="3" step="1" value="2" aria-label="Số bot luyện quái"><button type="button" data-field="farm" data-step="1" aria-label="Thêm bot luyện quái">+</button></div></article>
<article class="bot-type hunt"><div><h3>Săn boss</h3><p>Tìm boss đang xuất hiện, tự chuyển map và khu.</p></div><div class="bot-stepper"><button type="button" data-field="hunt" data-step="-1" aria-label="Bớt bot săn boss">−</button><input id="hunt" type="number" min="0" max="3" step="1" value="1" aria-label="Số bot săn boss"><button type="button" data-field="hunt" data-step="1" aria-label="Thêm bot săn boss">+</button></div></article></div>
<div class="bot-actions"><span id="total">Đã chọn 3 / 3 bot</span><div><button id="undoBots" type="button" disabled>Hoàn tác</button><button id="stopBots" type="button" class="danger">Tắt tất cả</button><button id="saveBots" type="submit" class="primary">Áp dụng</button></div></div></form>
<p class="bot-note">Thay đổi số lượng sẽ tạo lại nhóm bot thử nghiệm. Đồ nhặt và tiến trình bot được làm mới; dữ liệu người chơi không thay đổi. Nếu chưa có boss phù hợp, bot săn boss sẽ luyện quái trong lúc chờ.</p></section>
<p id="message" role="status" aria-live="polite"></p><section class="bot-list-section"><div class="bot-section-title"><h2>Bot đang hoạt động</h2><span id="updated">Tự cập nhật mỗi 5 giây</span></div><div id="botList" class="bot-list"><p class="bot-empty">Đang tải danh sách…</p></div></section>
</main><script>const botCsrf=<?=json_encode($_SESSION['panel_csrf'])?>;</script><script src="bots.js?v=1"></script></body></html>
