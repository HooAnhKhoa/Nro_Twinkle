CREATE TABLE IF NOT EXISTS panel_drop_rate (
 category VARCHAR(40) NOT NULL PRIMARY KEY,
 mode VARCHAR(12) NOT NULL DEFAULT 'multiplier',
 value DECIMAL(7,3) NOT NULL DEFAULT 1,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT IGNORE INTO panel_drop_rate(category) VALUES ('dragon_ball'),('shop'),('god'),('crystal'),('food'),('porata'),('angel'),('skh'),('gold'),('capsule'),('event'),('gem'),('skill');
