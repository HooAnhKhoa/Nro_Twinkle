CREATE TABLE IF NOT EXISTS panel_event_runtime (
 id TINYINT PRIMARY KEY, revision BIGINT NOT NULL DEFAULT 1,
 applied_revision BIGINT NOT NULL DEFAULT 0, heartbeat DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO panel_event_runtime(id) VALUES(1);
CREATE TABLE IF NOT EXISTS panel_event_config (
 event_key VARCHAR(40) PRIMARY KEY, enabled TINYINT NOT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO panel_event_config(event_key,enabled) VALUES
 ('broly',1),('hung_vuong',1),('halloween',0),('christmas',0),
 ('tet',0),('trung_thu',0),('summer',1),('mob_rewards',1);
CREATE TABLE IF NOT EXISTS panel_event_audit (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, event_key VARCHAR(40) NOT NULL,
 admin_id INT NOT NULL, before_enabled TINYINT NOT NULL, after_enabled TINYINT NOT NULL,
 revision BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 undone_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
