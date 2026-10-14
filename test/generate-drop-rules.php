<?php
require __DIR__.'/../web/admin/drop_rules_catalog.php';
$sql="CREATE TABLE IF NOT EXISTS panel_drop_rule (rule_key VARCHAR(80) PRIMARY KEY, category VARCHAR(40) NOT NULL, base_percent DECIMAL(12,8) NOT NULL, percent_override DECIMAL(12,8) NULL, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);\n";
$sql.="CREATE TABLE IF NOT EXISTS panel_drop_runtime (id TINYINT PRIMARY KEY, heartbeat DATETIME NULL, applied_revision BIGINT NOT NULL DEFAULT 0);\nINSERT IGNORE INTO panel_drop_runtime(id) VALUES(1);\n";
foreach(dropRules() as $key=>$rule)$sql.="INSERT INTO panel_drop_rule(rule_key,category,base_percent) VALUES('".$key."','".$rule['category']."',".sprintf('%.8F',$rule['base']).") ON DUPLICATE KEY UPDATE category=VALUES(category),base_percent=VALUES(base_percent);\n";
file_put_contents(__DIR__.'/../web/drop_rules.sql',$sql);
echo count(dropRules())." drop rules generated\n";
