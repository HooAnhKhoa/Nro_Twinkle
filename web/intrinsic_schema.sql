CREATE TABLE IF NOT EXISTS panel_intrinsic_template_config (
 id TINYINT NOT NULL PRIMARY KEY,
 revision BIGINT NOT NULL DEFAULT 1,
 applied_revision BIGINT NOT NULL DEFAULT 0,
 heartbeat DATETIME NULL
);
INSERT IGNORE INTO panel_intrinsic_template_config(id) VALUES(1);
CREATE TABLE IF NOT EXISTS panel_intrinsic_template_audit (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 intrinsic_id INT NOT NULL,
 admin_id INT NOT NULL,
 copies INT NOT NULL,
 before_json TEXT NOT NULL,
 after_json TEXT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 undone_at DATETIME NULL
);
