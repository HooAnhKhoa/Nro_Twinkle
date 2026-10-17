INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec,sort_order)
SELECT tab.id,1876,1,1,1,5,0,-1 FROM tab_shop tab JOIN shop s ON s.id=tab.shop_id
WHERE s.tag_name='SHOP_CHI_CHI' AND NOT EXISTS(SELECT 1 FROM item_shop i WHERE i.tab_id=tab.id AND i.temp_id=1876)
ORDER BY tab.id LIMIT 1;
