(()=>{'use strict';
const $=id=>document.getElementById(id),names={vip:'Hộp VIP',normal:'Hộp ngọc',vegetarian:'Hộp chay',milestone:'Mốc 25.000 VIP'};
let state=null,prizes=null,items=new Map(),optionNames=new Map(),busy=false,dirty=false,newSeason=false,signature=null;
async function api(body){const r=await fetch('trung_thu_top.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,csrf})});const d=await r.json();if(!r.ok)throw Error(d.error||'Không kết nối được.');return d;}
function el(tag,text){const n=document.createElement(tag);if(text!==undefined)n.textContent=text;return n;}
function local(ms){const d=new Date(Number(ms)+7*3600000);return d.toISOString().slice(0,16);}
function input(value,min,max){const n=el('input');n.type='number';n.step='1';n.min=min;n.max=max;n.required=true;n.value=value;return n;}
function rewardList(list,parent){
 const wrap=el('div');parent.append(wrap);
 function draw(){wrap.replaceChildren();list.forEach((item,index)=>{
  const row=el('div');row.className='tt-prize-row';const icon=el('img');icon.width=36;icon.height=36;icon.alt='';
  const id=input(item.id,0,32767);id.setAttribute('list','ttTopItems');id.setAttribute('aria-label','ID vật phẩm');
  const title=el('span');function identify(){const t=items.get(Number(id.value));title.textContent=t?.NAME||'Chưa tìm thấy ID';if(t){icon.src='/icon/'+t.icon_id+'.png';icon.hidden=false;}else icon.hidden=true;}
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
function rewardEditor(){const target=$('ttTopRewards');target.replaceChildren();for(const b of Object.keys(names)){
 const details=el('details');details.className='tt-reward-board';details.append(el('summary','Thưởng · '+names[b]));
 if(b==='milestone')rewardList(prizes[b],details);else prizes[b].forEach(tier=>{const block=el('details');block.append(el('summary','Hạng '+tier.from+(tier.to!==tier.from?'–'+tier.to:'')));rewardList(tier.items,block);details.append(block);});target.append(details);
}}
function status(){const s=state.season;if(!s)return 'Chưa có đợt đua top. Chọn lịch, giá hộp VIP và lưu để bắt đầu.';const now=Date.now();return s.closed_at?'Đã chốt thưởng. '+state.prizes.map(p=>names[p.board]+': '+p.claimed+'/'+p.total+' gói đã nhận').join(' · '):!Number(s.scheduled)?'Đã lưu nháp; đang dùng công tắc Trung Thu thủ công.':now<Number(s.start_ms)?'Đã hẹn giờ; tự mở lúc '+local(s.start_ms).replace('T',' '):now<Number(s.end_ms)?'Đang đua top; tự đóng lúc '+local(s.end_ms).replace('T',' '):'Đã hết giờ; chờ game chốt thưởng (game offline sẽ chốt khi chạy lại).';}
function fill(){const s=newSeason?null:state.season;prizes=structuredClone(s?.rewards||state.defaults);$('ttTopName').value=s?.name||'Trung Thu 2026';$('ttTopStart').value=s?local(s.start_ms):'';$('ttTopEnd').value=s?local(s.end_ms):'';$('ttTopBasis').value=s?.basis||'opened';$('ttTopGold').value=s?.vip_gold||0;$('ttTopGems').value=s?.vip_gems||0;$('ttTopScheduled').checked=Number(s?.scheduled)===1;rewardEditor();dirty=false;
 const locked=!!s&&(!!s.closed_at||Number(s.scheduled)===1&&Date.now()>=Number(s.start_ms));$('ttTopFields').disabled=locked;$('ttTopNew').hidden=!state.season?.closed_at||newSeason;
}
function ranks(){const target=$('ttTopRanks'),opened=[...target.children].map(n=>n.open);target.replaceChildren();for(const b of ['vip','normal','vegetarian']){
 const d=el('details');d.open=opened[['vip','normal','vegetarian'].indexOf(b)]||false;d.append(el('summary','Bảng xếp hạng · '+names[b]));const rows=state.ranks[b]||[];if(!rows.length)d.append(el('p','Chưa có điểm.'));else{const table=el('table');table.className='tt-top-table';const head=el('tr');['Hạng','Nhân vật','Điểm'].forEach(v=>head.append(el('th',v)));table.append(head);rows.forEach((r,i)=>{const tr=el('tr');[i+1,r.name||'#'+r.player_id,Number(r.score).toLocaleString('vi-VN')].forEach(v=>tr.append(el('td',v)));table.append(tr);});d.append(table);}target.append(d);
}}
async function load(reset=false){const d=await api({action:'read'});state=d;optionNames=new Map(d.options.map(o=>[Number(o.id),o.NAME]));items=new Map(d.items.map(i=>[Number(i.id),i]));const list=$('ttTopItems');if(!list.children.length)d.items.forEach(i=>{const o=el('option');o.value=i.id;o.label=i.NAME;list.append(o);});$('ttTopState').textContent=status();ranks();const next=JSON.stringify(d.season);if(reset||!dirty&&signature!==next){fill();signature=next;}const s=newSeason?null:state.season;$('ttTopFields').disabled=!!s&&(!!s.closed_at||Number(s.scheduled)===1&&Date.now()>=Number(s.start_ms));}
$('ttTopForm').oninput=()=>dirty=true;
$('ttTopForm').onsubmit=async e=>{e.preventDefault();if(busy||!state)return;busy=true;$('ttTopSave').disabled=true;try{
 const result=await api({action:'save',season_id:Number(state.season?.id||0),name:$('ttTopName').value,start:$('ttTopStart').value,end:$('ttTopEnd').value,basis:$('ttTopBasis').value,scheduled:$('ttTopScheduled').checked,vip_gold:Number($('ttTopGold').value),vip_gems:Number($('ttTopGems').value),rewards:prizes});$('ttTopMessage').textContent=result.message;dirty=false;newSeason=false;await load(true);
}catch(err){$('ttTopMessage').textContent=err.message;}finally{busy=false;$('ttTopSave').disabled=false;}};
$('ttTopReload').onclick=()=>{if(dirty&&!confirm('Bỏ thay đổi chưa lưu để tải lại?'))return;load(true).catch(e=>$('ttTopMessage').textContent=e.message);};
$('ttTopNew').onclick=()=>{newSeason=true;fill();dirty=true;};
load(true).catch(e=>$('ttTopMessage').textContent=e.message);
setInterval(()=>{if(!busy&&!dirty&&!document.hidden)load().catch(()=>{});},10000);
})();
