<?php
function adminPageToolbar(string $title): void {
    $safe=htmlspecialchars($title,ENT_QUOTES,'UTF-8');
    echo '<div class="admin-page-topbar"><div class="admin-breadcrumb"><span>Quản trị</span><span class="crumb-divider">/</span><strong>'.$safe.'</strong></div><a class="admin-back" href="panel.php" data-admin-back>← Tổng quan</a></div>';
}
