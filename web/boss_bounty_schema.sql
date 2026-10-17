CREATE TABLE IF NOT EXISTS boss_bounty_receipt (
 encounter_key CHAR(36) PRIMARY KEY, account_id INT NOT NULL, player_id INT NOT NULL,
 boss_key VARCHAR(160) NOT NULL, boss_name VARCHAR(200) NOT NULL,
 map_id INT NOT NULL, zone_id INT NOT NULL, hp BIGINT NOT NULL, damage BIGINT NOT NULL,
 tier TINYINT NOT NULL, vnd INT NOT NULL, balance_before BIGINT NOT NULL, balance_after BIGINT NOT NULL,
 ground_items TEXT NOT NULL, gems INT NOT NULL DEFAULT 0, gems_before INT NOT NULL DEFAULT 0, gems_after INT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX account_time(account_id,created_at), INDEX player_time(player_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
