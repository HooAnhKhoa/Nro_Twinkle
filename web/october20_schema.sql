CREATE TABLE IF NOT EXISTS october20_config (
 id TINYINT PRIMARY KEY, revision BIGINT NOT NULL DEFAULT 1, settings TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO october20_config(id,settings) VALUES(1,'{"normal.roses":20,"normal.paper":10,"normal.chocolate":5,"normal.bow":1,"normal.gold":1000000,"special.roses":50,"special.paper":20,"special.chocolate":10,"special.bow":3,"special.gold":3000000,"normal.accessory":2,"special.accessory":20,"normal.stones":60,"special.stones":50,"permanent":1,"normal.days":3,"special.days":7,"normal.quantity":3,"special.quantity":10,"normal.points":1,"special.points":5,"redeem.points":100,"redeem.days":7,"sd.min":15,"sd.max":20,"sdcm.min":15,"sdcm.max":20,"cm.min":5,"cm.max":7,"cute.min":5,"cute.max":7}');
CREATE TABLE IF NOT EXISTS october20_player (
 player_id INT PRIMARY KEY, points BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS october20_exchange (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, player_id INT NOT NULL, operation VARCHAR(32) NOT NULL,
 points_delta INT NOT NULL, config_revision BIGINT NOT NULL, rewards TEXT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS october20_config_audit (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, admin_id INT NOT NULL, revision BIGINT NOT NULL,
 before_settings TEXT NOT NULL, after_settings TEXT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
