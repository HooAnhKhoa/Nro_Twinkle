let overviewData=null,goldExpanded=false,overviewBusy=false;
function renderGold(){const body=el('goldTop');body.replaceChildren();const rows=overviewData?.gold_top||[];el('goldCount').textContent=rows.length?'Top '+rows.length:'Chưa có dữ liệu';el('toggleGold').hidden=rows.length<=5;el('toggleGold').textContent=goldExpanded?'Thu gọn ↑':'Xem top 20 →';if(!rows.length){const row=body.insertRow();const cell=row.insertCell();cell.colSpan=4;cell.textContent='Chưa có nhân vật sở hữu thỏi vàng trong dữ liệu đã lưu.';return;}for(const [index,player] of rows.slice(0,goldExpanded?20:5).entries()){const row=body.insertRow(),rank=document.createElement('span');rank.className='rank-number'+(index<3?' top':'');rank.textContent=index+1;row.insertCell().append(rank);const identity=document.createElement('div');identity.className='player-identity';const image=document.createElement('img');image.src='../img/'+(['td.jpg','nm.jpg','xd.jpg'][Number(player.gender)]||'td.jpg');image.alt='';identity.append(image,document.createTextNode(player.name));row.insertCell().append(identity);row.insertCell().textContent=['Trái Đất','Namek','Xayda'][Number(player.gender)]||'—';const quantity=row.insertCell();quantity.className='align-right';quantity.textContent=fmt(player.gold_bars);}}
function renderActivity(){const list=el('activityList');list.replaceChildren();for(const activity of overviewData?.activity||[]){const row=document.createElement('div');row.className='activity-row'+(activity.completed_at?'':' pending');const time=document.createElement('span');time.className='activity-time';time.textContent=activity.time.replace(/^\d{4}-(\d{2})-(\d{2}) /,'$2/$1 ');const type=document.createElement('span');type.className='activity-type';type.textContent=({maintenance:'Bảo trì',kick:'Kick',gift:'Tặng VNĐ',undo:'Hoàn tác'})[activity.action]||activity.action;const text=document.createElement('span');text.textContent=activity.result||'Lệnh đang chờ server xử lý';row.append(time,type,text);list.append(row);}if(!list.childElementCount){const empty=document.createElement('p');empty.className='empty-state';empty.textContent='Chưa có hoạt động quản trị được ghi nhận.';list.append(empty);}}
async function refreshOverview(){if(overviewBusy)return;overviewBusy=true;try{overviewData=await api({action:'overview'});el('totalPlayers').textContent=fmt(overviewData.counts.players);el('totalAccounts').textContent=fmt(overviewData.counts.accounts);el('totalGiftcodes').textContent=fmt(overviewData.counts.giftcodes);el('goldUpdated').textContent='Đọc dữ liệu lúc '+overviewData.updated_at.slice(11);renderGold();renderActivity();}catch(error){el('goldTop').replaceChildren();const row=el('goldTop').insertRow();const cell=row.insertCell();cell.colSpan=4;cell.textContent='Không tải được bảng xếp hạng. Hãy thử làm mới.';el('pageMessage').textContent=error.message;}finally{overviewBusy=false;}}
document.addEventListener('panel-status',event=>{const {runtime,live}=event.detail;document.body.classList.toggle('server-live',live&&!Number(runtime.maintenance));el('runtimeHint').textContent=live?(Number(runtime.maintenance)?'Đang bảo trì':'Server đang hoạt động'):'Server chưa kết nối';el('lastUpdate').textContent='Cập nhật '+new Date().toLocaleTimeString('vi-VN');const ratio=live&&Number(runtime.ram_max)>0?Math.min(100,100*Number(runtime.ram_used)/Number(runtime.ram_max)):0;el('ramMeter').style.width=ratio+'%';});
el('toggleGold').onclick=()=>{goldExpanded=!goldExpanded;renderGold();};el('refreshDashboard').onclick=async()=>{el('refreshDashboard').disabled=true;try{if(!el('dashboardPage').hidden&&currentDashboardPage){el('dashboardFrame').src=currentDashboardPage;}await Promise.all([refreshStatus(),refreshOverview()]);}finally{el('refreshDashboard').disabled=false;}};
function closeMenu(){document.body.classList.remove('menu-open');el('menuToggle').setAttribute('aria-expanded','false');}
el('menuToggle').onclick=()=>{const open=document.body.classList.toggle('menu-open');el('menuToggle').setAttribute('aria-expanded',String(open));};el('overviewNav').onclick=()=>showDashboardOverview();el('activityNav').onclick=()=>showDashboardOverview(true);document.querySelectorAll('[data-target]').forEach(button=>button.addEventListener('click',closeMenu));document.addEventListener('keydown',event=>{if(event.key==='Escape')closeMenu();});document.querySelector('.dashboard-main').addEventListener('click',event=>{if(!event.target.closest('#menuToggle'))closeMenu();});
refreshOverview();setInterval(refreshOverview,30000);

// Menu destinations share the dashboard shell; operational confirmations remain dialogs.
history.scrollRestoration='manual';
const dashboardLinks=[...document.querySelectorAll('[data-target]')].filter(b=>b.dataset.target.includes('.php'));
const pageRoutes=new Map();
for(const button of dashboardLinks){if(!pageRoutes.has(button.dataset.target))pageRoutes.set(button.dataset.target,button.textContent.trim());}
const pageFiles=new Set([...pageRoutes.keys()].map(t=>t.split('?')[0]).concat(['vang.php','ngoc.php','taocode.php']));
let currentDashboardPage='';
function safeDashboardTarget(target){
    try{const url=new URL(target,location.href);const file=url.pathname.split('/').pop();
        return url.origin===location.origin&&url.pathname==='/admin/'+file&&(pageFiles.has(file)||file==='boss_drops.php')?(file==='boss_drops.php'?'boss.php':file)+url.search:'';
    }catch(error){return '';}
}
function markDashboardMenu(target,activity=false){
    const file=target.split('?')[0],family=['vang.php','ngoc.php'].includes(file)?'itemshop.php':file==='taocode.php'?'giftcode.php':file;
    document.querySelectorAll('.sidebar .active').forEach(b=>{b.classList.remove('active');b.removeAttribute('aria-current');});
    const button=target?dashboardLinks.find(b=>b.closest('.sidebar')&&(b.dataset.target===target||(!pageRoutes.has(target)&&b.dataset.target.split('?')[0]===family))):el(activity?'activityNav':'overviewNav');
    if(button){button.classList.add('active');button.setAttribute('aria-current','page');const group=button.closest('details');if(group)group.open=true;}
}
function setDashboardAddress(hash,replace=false){if(location.hash!==hash)history[replace?'replaceState':'pushState'](null,'',hash);}
function showDashboardOverview(activity=false,updateAddress=true){
    closeMenu();el('overviewContent').hidden=false;el('dashboardPage').hidden=true;
    el('dashboardPageTitle').textContent='Tổng quan máy chủ';el('dashboardPageHint').textContent='Theo dõi hoạt động và quản lý game';
    document.title='Tổng quan • NRO Twinkle';markDashboardMenu('',activity);
    if(updateAddress)setDashboardAddress(activity?'#activity':'#overview');
    if(activity)el('activitySection').scrollIntoView({behavior:'smooth'});else window.scrollTo({top:0});
}
window.showDashboardOverview=showDashboardOverview;
window.openDashboardPage=function(target,label,updateAddress=true){
    target=safeDashboardTarget(target);if(!target)return;
    closeMenu();if(el('dialog').open)closeDialog();
    el('overviewContent').hidden=true;el('dashboardPage').hidden=false;
    label=pageRoutes.get(target)||label||'Quản trị';el('dashboardPageTitle').textContent=label;
    el('dashboardPageHint').textContent='Quản lý và chỉnh sửa dữ liệu máy chủ';document.title=label+' • NRO Twinkle';
    markDashboardMenu(target);if(updateAddress)setDashboardAddress('#page='+encodeURIComponent(target));
    if(currentDashboardPage!==target){currentDashboardPage=target;el('pageLoading').textContent='Đang tải trang…';el('pageLoading').hidden=false;el('dashboardFrame').hidden=true;el('dashboardFrame').src=target;}
    window.scrollTo({top:0});
};
el('dashboardFrame').addEventListener('load',()=>{
    const frame=el('dashboardFrame');
    try{
        const doc=frame.contentDocument,target=safeDashboardTarget(frame.contentWindow.location.href);
        if(!doc||!target)return;
        currentDashboardPage=target;doc.body.classList.add('admin-embedded');
        const label=pageRoutes.get(target)||doc.querySelector('.admin-breadcrumb strong')?.textContent||doc.title;
        frame.title=label;if(!el('dashboardPage').hidden){el('dashboardPageTitle').textContent=label;document.title=label+' • NRO Twinkle';}
        if(!el('dashboardPage').hidden){markDashboardMenu(target);setDashboardAddress('#page='+encodeURIComponent(target),true);}
        el('pageLoading').hidden=true;frame.hidden=false;if(!el('dashboardPage').hidden)window.scrollTo({top:0});

        if(!el('dashboardPage').hidden)requestAnimationFrame(()=>window.scrollTo({top:0,behavior:'instant'}));
    }catch(error){el('pageLoading').textContent='Không tải được trang. Bấm lại mục menu để thử lại.';currentDashboardPage='';}
});
function restoreDashboardRoute(){
    if(location.hash.startsWith('#page=')){try{const target=safeDashboardTarget(decodeURIComponent(location.hash.slice(6)));if(target){window.openDashboardPage(target,pageRoutes.get(target),false);return;}}catch(error){}}
    showDashboardOverview(location.hash==='#activity',false);
}
window.addEventListener('hashchange',restoreDashboardRoute);restoreDashboardRoute();
