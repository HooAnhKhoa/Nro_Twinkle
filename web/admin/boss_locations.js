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
function bossDuration(ms){
    let seconds=Math.max(0,Math.ceil(ms/1000));
    const days=Math.floor(seconds/86400);seconds%=86400;
    return (days?days+' ngày ':'')+[Math.floor(seconds/3600),Math.floor(seconds%3600/60),seconds%60].map(n=>String(n).padStart(2,'0')).join(':');
}
function bossTimerLabels(row,now){
    if(!bossLive(row))return [['Thời gian','Server chưa kết nối']];
    const timings=Array.isArray(row.timings)?row.timings:[];
    if(!timings.length)return [['Thời gian','Chờ bản server mới gửi dữ liệu']];
    const spawn=timings.filter(t=>t.spawn_at!=null).map(t=>Number(t.spawn_at));
    const appeared=timings.filter(t=>t.appeared_at!=null).map(t=>Number(t.appeared_at));
    const expires=timings.filter(t=>t.expires_at!=null).map(t=>Number(t.expires_at));
    return [
        ['Hồi cấu hình',bossDuration(Number(row.rest_seconds)*1000)],
        ['Xuất hiện tiếp',spawn.length?(Math.min(...spawn)<=now?'Đang chờ vào map':bossDuration(Math.min(...spawn)-now)):(Number(row.online_count)>0?'Boss đang trên map':'Theo chuỗi / điều kiện riêng')],
        ['Đã tồn tại',appeared.length?bossDuration(now-Math.min(...appeared)):'Chưa xuất hiện'],
        ['Còn tồn tại',expires.length?bossDuration(Math.min(...expires)-now):(row.lifetime_seconds==null?'Theo cơ chế gốc':Number(row.lifetime_seconds)===0?'Không giới hạn':'Chưa xuất hiện')]
    ];
}
