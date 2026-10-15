INSERT INTO shop(npc_id,tag_name,type_shop) SELECT 39,'SANTA_NOEL',0 WHERE NOT EXISTS(SELECT 1 FROM shop WHERE tag_name='SANTA_NOEL');
INSERT INTO tab_shop(shop_id,NAME) SELECT id,'Giáng sinh' FROM shop s WHERE tag_name='SANTA_NOEL' AND NOT EXISTS(SELECT 1 FROM tab_shop t WHERE t.shop_id=s.id);
INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec,sort_order)
SELECT t.id,649,1,1,1,20,0,0 FROM tab_shop t JOIN shop s ON s.id=t.shop_id WHERE s.tag_name='SANTA_NOEL' AND NOT EXISTS(SELECT 1 FROM item_shop i WHERE i.tab_id=t.id AND i.temp_id=649);
INSERT INTO item_shop_option(item_shop_id,option_id,param)
SELECT i.id,30,0 FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop s ON s.id=t.shop_id WHERE s.tag_name='SANTA_NOEL' AND i.temp_id=649 AND NOT EXISTS(SELECT 1 FROM item_shop_option o WHERE o.item_shop_id=i.id AND o.option_id=30);
