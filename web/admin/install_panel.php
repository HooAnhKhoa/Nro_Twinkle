<?php
// CLI only. Installs additive panel tables without modifying player data.
if(PHP_SAPI!=='cli'){http_response_code(404);exit;}
mysqli_report(MYSQLI_REPORT_ERROR|MYSQLI_REPORT_STRICT);
require __DIR__.'/../config.php';
foreach(['panel_schema.sql','boss_web.sql','server_rates.sql'] as $file)foreach(explode(';',file_get_contents(__DIR__.'/../'.$file)) as $sql)if(trim($sql)!=='')$conn->query($sql);
if(!$conn->query("SHOW COLUMNS FROM boss_web_config LIKE 'head_id'")->num_rows)$conn->query('ALTER TABLE boss_web_config ADD head_id INT NOT NULL DEFAULT 0');
if(!$conn->query("SHOW COLUMNS FROM boss_web_config LIKE 'current_locations'")->num_rows)$conn->query('ALTER TABLE boss_web_config ADD current_locations TEXT NULL');
echo "Panel schema installed; existing player data preserved.\n";
