function bossLive(row){return row.age!==null&&Number(row.age)>=0&&Number(row.age)<=20;}
function bossLocation(row,compact=false){
    if(!bossLive(row))return 'Chưa cập nhật vị trí (server offline)';
    if(row.current_locations===null)return 'Server chưa gửi vị trí';
    if(!Array.isArray(row.locations)||!row.locations.length)return 'Chưa xuất hiện';
    if(compact)return [...new Set(row.locations.map(p=>p.map_name||'Map #'+p.map_id))].join(', ');
    return row.locations.map(p=>(p.map_name||'Map #'+p.map_id)+' — Khu '+p.zone_id+(Number(p.count)>1?' ('+p.count+' Boss)':'')).join('\n');
}
function bossSpawnMaps(row){return (row.spawn_maps||[]).map(p=>p.name+' (#'+p.id+')').join(', ')||'Chưa có cấu hình map';}
function bossRestHint(row){
    const seconds=Number(row.rest_seconds),custom=Number(row.customized)!==0;
    if(!Number(row.can_spawn))return (seconds===0?'0 giây là giá trị gốc không trì hoãn. ':'')+'Boss theo chuỗi/giai đoạn hoặc cơ chế riêng; không có nghĩa cứ '+seconds+' giây xuất hiện độc lập.';
    return (custom?'Đã chỉnh':'Cấu hình gốc')+': hồi '+seconds+' giây sau khi nghỉ. Đây là thời gian hồi cấu hình, không phải đếm ngược đến lần xuất hiện tiếp theo.';
}
