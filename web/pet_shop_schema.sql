INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec,sort_order)
SELECT tab.id,1939,1,1,1,30,0,0 FROM tab_shop tab JOIN shop s ON s.id=tab.shop_id
WHERE s.tag_name='SHOP_CHI_CHI'
AND EXISTS(SELECT 1 FROM item_template WHERE id=1939)
AND NOT EXISTS(SELECT 1 FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id WHERE sh.tag_name='SHOP_CHI_CHI' AND i.temp_id=1939)
ORDER BY tab.id LIMIT 1;