CREATE TABLE IF NOT EXISTS panel_drop_rate (
 category VARCHAR(40) NOT NULL PRIMARY KEY,
 mode VARCHAR(12) NOT NULL DEFAULT 'multiplier',
 value DECIMAL(7,3) NOT NULL DEFAULT 1,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT IGNORE INTO panel_drop_rate(category) VALUES ('dragon_ball'),('upgrade_stone'),('shop'),('god'),('crystal'),('food'),('porata'),('angel'),('skh'),('gold'),('capsule'),('event'),('gem'),('skill');

INSERT IGNORE INTO panel_drop_rate(category) VALUES('hung_vuong');
INSERT IGNORE INTO panel_drop_rate(category,mode,value) SELECT 'summer',mode,value FROM panel_drop_rate WHERE category='event';
INSERT IGNORE INTO panel_drop_rate(category,mode,value) SELECT 'bill_food',mode,value FROM panel_drop_rate WHERE category='event';
INSERT IGNORE INTO panel_drop_rate(category,mode,value) SELECT 'quest_items',mode,value FROM panel_drop_rate WHERE category='event';
