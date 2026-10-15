INSERT IGNORE INTO item_template(id,TYPE,gender,NAME,description,level,icon_id,part,is_up_to_up,power_require,gold,gem,head,body,leg)
SELECT 2000,75,3,'Vé Thành Cổ','Dùng 1 lần vào Thành Cổ 1 khi sự kiện Giỗ Tổ đang bật.',0,icon_id,0,1,0,0,0,-1,-1,-1 FROM item_template WHERE id=1787;
INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec,sort_order)
SELECT tab.id,2000,1,1,1,5,0,-1 FROM tab_shop tab JOIN shop s ON s.id=tab.shop_id
WHERE s.tag_name='SHOP_CHI_CHI' AND NOT EXISTS(SELECT 1 FROM item_shop i WHERE i.tab_id=tab.id AND i.temp_id=2000)
ORDER BY tab.id LIMIT 1;
