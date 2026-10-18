(()=>{'use strict';
const $=id=>document.getElementById(id),names={roses:'Hộp Quà 20/10 (ngọc)',cards:'Thiệp Chúc VIP',gifts:'Hộp Quà 20/10',rose_bonus:'Top 1 Hộp ngọc · 21.000 điểm',card_bonus:'Top 1 Thiệp VIP · 41.250 điểm'};
let state=null,prizes=null,items=new Map(),optionNames=new Map(),busy=false,dirty=false,newSeason=false,signature=null,missing={};
async function api(body){const r=await fetch('october20_top.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,csrf})});const d=await r.json();if(!r.ok)throw Error(d.error||'Không kết nối được.');return d;}
function el(tag,text){const n=document.createElement(tag);if(text!==undefined)n.textContent=text;return n;}
function local(ms){const d=new Date(Number(ms)+7*3600000);return d.toISOString().slice(0,16);}
function input(value,min,max){const n=el('input');n.type='number';n.step='1';n.min=min;n.max=max;n.required=true;n.value=value;return n;}
function rewardList(list,parent){
 const wrap=el('div');parent.append(wrap);
 function draw(){wrap.replaceChildren();list.forEach((item,index)=>{
  const row=el('div');row.className='tt-prize-row';const icon=el('img');icon.width=36;icon.height=36;icon.alt='';
  const id=input(item.id,0,32767);id.setAttribute('list','octTopItems');id.setAttribute('aria-label','ID vật phẩm');
  const title=el('span');function identify(){const t=items.get(Number(id.value));title.textContent=(t?.NAME||'Chưa tìm thấy ID')+(missing[id.value]?' · Chưa đủ hình: '+missing[id.value]:'');if(t){icon.src='/icon/'+t.icon_id+'.png';icon.hidden=false;}else icon.hidden=true;}
  identify();id.oninput=()=>{item.id=Number(id.value);identify();};
  const qty=input(item.quantity,1,99999);qty.setAttribute('aria-label','Số lượng');qty.oninput=()=>item.quantity=Number(qty.value);
  const days=input(item.days,0,3650);days.setAttribute('aria-label','Số ngày, 0 vĩnh viễn');days.oninput=()=>item.days=Number(days.value);
  const remove=el('button','×');remove.type='button';remove.setAttribute('aria-label','Xóa vật phẩm');remove.onclick=()=>{list.splice(index,1);dirty=true;draw();};
  row.append(icon,id,title,el('span','×'),qty,el('span','Ngày (0 = VV)'),days,remove);wrap.append(row);
  const stats=el('details');stats.style.margin='0 12px 12px';stats.append(el('summary','Chỉ số ('+item.options.length+')'));
  item.options.forEach((option,oi)=>{const line=el('div');line.style.cssText='display:flex;gap:8px;margin:8px 0;align-items:center';const select=el('select');select.setAttribute('aria-label','Loại chỉ số');for(const [oid,name] of optionNames){if(oid===93)continue;const o=el('option',name);o.value=oid;o.selected=oid===option.id;select.append(o);}select.onchange=()=>option.id=Number(select.value);const value=input(option.param,0,1000000000);value.setAttribute('aria-label','Giá trị chỉ số');value.oninput=()=>option.param=Number(value.value);const del=el('button','×');del.type='button';del.onclick=()=>{item.options.splice(oi,1);dirty=true;draw();};line.append(select,value,del);stats.append(line);});
  const addStat=el('button','+ Chỉ số');addStat.type='button';addStat.onclick=()=>{if(item.options.length>=20)return;item.options.push({id:50,param:0});dirty=true;draw();};stats.append(addStat);wrap.append(stats);
 });}
 draw();const add=el('button','+ Vật phẩm');add.type='button';add.onclick=()=>{list.push({id:987,quantity:1,days:0,options:[]});dirty=true;draw();};parent.append(add);
}
function rewardEditor(){const target=$('octTopRewards');target.replaceChildren();for(const b of Object.keys(names)){
 const details=el('details');details.className='tt-reward-board';details.append(el('summary','Thưởng · '+names[b]));
 if(b==='rose_bonus'||b==='card_bonus')rewardList(prizes[b],details);else prizes[b].forEach(tier=>{const block=el('details');block.append(el('summary','Hạng '+tier.from+(tier.to!==tier.from?'–'+tier.to:'')));rewardList(tier.items,block);details.append(block);});target.append(details);
}}
function status(){const s=state.season;if(!s)return 'Chưa có đợt 20/10. Chọn lịch, kiểm tra quà và lưu.';const now=Date.now();const stage=!Number(s.scheduled)?'Đang lưu nháp':now<Number(s.start_ms)?'Chưa đến giờ bắt đầu':now>=Number(s.end_ms)?'Sự kiện đã kết thúc':Number(s.paused)?'Đang tạm tắt':s.closed_at?'Đã chốt top; shop còn mở đến giờ tắt':'Đang mở';return stage+' · Chốt điểm '+local(s.points_end_ms).replace('T',' ')+' · '+state.prizes.map(p=>names[p.board]+': '+p.claimed+'/'+p.total+' gói đã nhận').join(' · ');}
function fill(){const s=newSeason?null:state.season;prizes=structuredClone(s?.rewards||state.defaults);$('octTopName').value=s?.name||'Phụ nữ Việt Nam 20/10/2026';$('octTopStart').value=s?local(s.start_ms):local(Date.now()+5*60000);$('octTopPointsEnd').value=s?local(s.points_end_ms):'2026-10-29T23:00';$('octTopEnd').value=s?local(s.end_ms):'2026-10-30T00:00';$('octTopRose').value=s?.rose_gems||20;$('octTopCard').value=s?.card_gems||50;$('octTopGold').value=s?.gift_gold||10000000;$('octTopScheduled').checked=Number(s?.scheduled)===1;rewardEditor();dirty=false;
 const locked=!!s&&(!!s.closed_at||Number(s.scheduled)===1&&Date.now()>=Number(s.start_ms));$('octTopFields').disabled=locked;$('octTopNew').hidden=!state.season?.closed_at||Date.now()<Number(state.season?.end_ms)||newSeason;
}
function ranks(){const target=$('octTopRanks'),opened=[...target.children].map(n=>n.open);target.replaceChildren();for(const b of ['roses','cards','gifts']){
 const d=el('details');d.open=opened[['roses','cards','gifts'].indexOf(b)]||false;d.append(el('summary','Bảng xếp hạng · '+names[b]));const rows=state.ranks[b]||[];if(!rows.length)d.append(el('p','Chưa có điểm.'));else{const table=el('table');table.className='tt-top-table';const head=el('tr');['Hạng','Nhân vật','Điểm'].forEach(v=>head.append(el('th',v)));table.append(head);rows.forEach((r,i)=>{const tr=el('tr');[i+1,r.name||'#'+r.player_id,Number(r.score).toLocaleString('vi-VN')].forEach(v=>tr.append(el('td',v)));table.append(tr);});d.append(table);}target.append(d);
}}
async function load(reset=false){const d=await api({action:'read'});state=d;missing=d.missing||{};optionNames=new Map(d.options.map(o=>[Number(o.id),o.NAME]));items=new Map(d.items.map(i=>[Number(i.id),i]));const list=$('octTopItems');if(!list.children.length)d.items.filter(i=>!missing[i.id]).forEach(i=>{const o=el('option');o.value=i.id;o.label=i.NAME;list.append(o);});$('octTopState').textContent=status();$('octTopPause').hidden=!Number(d.season?.scheduled)||Date.now()>=Number(d.season?.end_ms);$('octTopPause').textContent=Number(d.season?.paused)?'Mở lại theo lịch':'Tạm tắt sự kiện';ranks();const next=JSON.stringify(d.season);if(reset||!dirty&&signature!==next){fill();signature=next;}const s=newSeason?null:state.season;$('octTopFields').disabled=!!s&&(!!s.closed_at||Number(s.scheduled)===1&&Date.now()>=Number(s.start_ms));}
$('octTopForm').oninput=()=>dirty=true;
$('octTopForm').onsubmit=async e=>{e.preventDefault();if(busy||!state)return;busy=true;$('octTopSave').disabled=true;try{
 const result=await api({action:'save',season_id:Number(state.season?.id||0),name:$('octTopName').value,start:$('octTopStart').value,end:$('octTopEnd').value,points_end:$('octTopPointsEnd').value,scheduled:$('octTopScheduled').checked,gift_gold:Number($('octTopGold').value),rose_gems:Number($('octTopRose').value),card_gems:Number($('octTopCard').value),rewards:prizes});$('octTopMessage').textContent=result.message;dirty=false;newSeason=false;await load(true);
}catch(err){$('octTopMessage').textContent=err.message;}finally{busy=false;$('octTopSave').disabled=false;}};
$('octTopReload').onclick=()=>{if(dirty&&!confirm('Bỏ thay đổi chưa lưu để tải lại?'))return;load(true).catch(e=>$('octTopMessage').textContent=e.message);};
$('octTopPause').onclick=async()=>{if(busy||!state?.season)return;busy=true;try{const r=await api({action:'pause',season_id:Number(state.season.id),paused:!Number(state.season.paused)});$('octTopMessage').textContent=r.message;await load(true);}catch(e){$('octTopMessage').textContent=e.message;}finally{busy=false;}};
$('octTopNew').onclick=()=>{newSeason=true;fill();dirty=true;};
load(true).catch(e=>$('octTopMessage').textContent=e.message);
setInterval(()=>{if(!busy&&!dirty&&!document.hidden)load().catch(()=>{});},10000);
})();
