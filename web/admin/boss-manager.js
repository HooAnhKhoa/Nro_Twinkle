const byId=id=>document.getElementById(id),number=n=>Number(n).toLocaleString('vi-VN');
let bossRows=[],chosen=null,infoDirty=false,dropDirty=false,infoBusy=false,dropBusy=false,clockOffset=0;
let dropConfig=null,optionCatalog=[],dropLoadVersion=0,pickerVersion=0,pickerTimer=null,lastCardFocus=null;
const modal=byId('bossModal');
async function bossApi(body){const response=await fetch('boss.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,csrf:bossCsrf})});const data=await response.json();if(!response.ok||data.error)throw Error(data.error||'Không thể thực hiện thao tác.');return data;}
function notify(text,error=false){byId('message').textContent=text||'';byId('message').className='boss-message'+(error?' error':'');}
function node(tag,text,cls){const e=document.createElement(tag);if(text!=null)e.textContent=text;if(cls)e.className=cls;return e;}
function icon(src){const image=node('img');image.src=src;image.alt='';image.onerror=()=>{image.onerror=null;image.src='/icon/1.png';};return image;}
function renderBosses(){
 const query=byId('search').value.trim().toLowerCase(),filter=byId('stateFilter').value;
 const rows=bossRows.filter(r=>{
  const match=(r.name+' '+bossLocation(r)+' '+bossSpawnMaps(r)+' '+r.hp).toLowerCase().includes(query)||query!==''&&String(r.hp).includes(query.replace(/[.,\s]/g,''));
  return match&&(filter==='all'||filter==='off'&&!Number(r.enabled)||filter==='live'&&bossLive(r)&&Number(r.online_count)>0||filter==='waiting'&&bossLive(r)&&Number(r.enabled)&&Number(r.online_count)===0);
 });
 byId('count').textContent=rows.length+' / '+bossRows.length+' boss';byId('empty').hidden=rows.length>0;byId('cards').replaceChildren();
 for(const r of rows){
  const card=node('button',null,'boss-tile'+(!Number(r.enabled)?' is-off':''));card.type='button';card.dataset.key=r.boss_key;
  const live=bossLive(r)&&Number(r.online_count)>0,status=!Number(r.enabled)?'Đang tắt':!bossLive(r)?'Chưa kết nối':live?r.online_count+' trên map':'Chờ xuất hiện';
  card.append(node('span',status,'boss-status'+(!Number(r.enabled)?' off':live?'':' waiting')));
  const portrait=node('div',null,'boss-card-image');const img=icon(r.image);img.alt=r.name;portrait.append(img);card.append(portrait,node('strong',r.name),node('span','HP '+number(r.hp),'boss-card-hp'));
  const place=node('span','📍 '+bossLocation(r,true),'boss-card-place');place.title=bossLocation(r);card.append(place);card.onclick=()=>openBoss(r,card);byId('cards').append(card);
 }
}
function fillInfo(){if(!chosen)return;byId('hp').value=number(chosen.hp);byId('damage').value=number(chosen.damage);byId('rest').value=chosen.rest_seconds;byId('lifetime').value=chosen.lifetime_seconds??'';byId('enabled').checked=!!Number(chosen.enabled);}
function showTab(name){const drops=name==='drops';byId('infoPanel').hidden=drops;byId('dropsPanel').hidden=!drops;byId('infoTab').setAttribute('aria-selected',String(!drops));byId('dropsTab').setAttribute('aria-selected',String(drops));if(drops&&!dropConfig&&!dropBusy)loadDrops();}
async function openBoss(row,card){
 if((infoDirty||dropDirty)&&chosen?.boss_key!==row.boss_key&&!confirm('Bỏ các thay đổi chưa lưu?'))return;
 if(chosen?.boss_key!==row.boss_key){dropLoadVersion++;dropConfig=null;dropDirty=false;byId('dropRows').replaceChildren();byId('itemSearch').value='';byId('itemResults').hidden=true;}
 chosen=row;infoDirty=false;fillInfo();byId('spawnDelay').value=0;notify('');lastCardFocus=card;updateDetails();showTab(startBossDrops?'drops':'info');if(!modal.open)modal.showModal();
}
function closeBoss(){if(infoBusy||dropBusy){notify('Đang xử lý, hãy chờ hoàn tất.');return;}if((infoDirty||dropDirty)&&!confirm('Đóng và bỏ các thay đổi chưa lưu?'))return;modal.close();infoDirty=dropDirty=false;dropLoadVersion++;dropConfig=null;const current=[...byId('cards').children].find(c=>c.dataset.key===chosen?.boss_key);(current||lastCardFocus)?.focus();}
function updateTimers(){if(!chosen||!modal.open)return;byId('timers').replaceChildren();for(const [title,value] of bossTimerLabels(chosen,Date.now()+clockOffset)){const line=node('div');line.append(node('span',title),node('strong',value));byId('timers').append(line);}}
function updateDetails(){
 if(!chosen)return;byId('bossTitle').textContent=chosen.name;byId('bossPortrait').src=chosen.image;byId('bossState').textContent=Number(chosen.enabled)?chosen.online_count+' đang xuất hiện · '+(bossLive(chosen)?'Đã kết nối server':'Chưa kết nối server'):'Boss đang tắt';
 byId('locationNow').textContent='📍 '+bossLocation(chosen);byId('restHint').textContent=bossRestHint(chosen);byId('detail').textContent='Map xuất hiện: '+bossSpawnMaps(chosen);updateTimers();
 byId('spawn').textContent=Number(byId('spawnDelay').value)>0?'Hẹn xuất hiện':'Gọi ngay';byId('spawn').disabled=infoBusy||infoDirty||!bossLive(chosen)||!Number(chosen.can_spawn)||!Number(chosen.enabled)||Number(chosen.online_count)>0;
 byId('saveInfo').disabled=infoBusy||!chosen;byId('periodicHint').hidden=!chosen.boss_key.endsWith('.OngGiaNoel');
}
async function refreshBosses(){
 try{const data=await bossApi({action:'list'});bossRows=data.rows;clockOffset=data.server_now-Date.now();byId('pageMessage').textContent='';
  if(chosen){const fresh=bossRows.find(r=>r.boss_key===chosen.boss_key);if(fresh){chosen=fresh;if(!infoDirty&&!infoBusy)fillInfo();updateDetails();}}renderBosses();
 }catch(error){byId('pageMessage').textContent=error.message;}
}
byId('search').oninput=renderBosses;byId('stateFilter').onchange=renderBosses;byId('closeModal').onclick=closeBoss;modal.addEventListener('cancel',event=>{event.preventDefault();closeBoss();});
byId('infoTab').onclick=()=>showTab('info');byId('dropsTab').onclick=()=>showTab('drops');byId('infoForm').oninput=event=>{if(event.target.id!=='spawnDelay')infoDirty=true;updateDetails();};byId('cancelInfo').onclick=()=>{infoDirty=false;fillInfo();updateDetails();notify('Đã bỏ thay đổi thông tin.');};
byId('infoForm').onsubmit=async event=>{
 event.preventDefault();if(!chosen||infoBusy)return;infoBusy=true;updateDetails();
 try{const data=await bossApi({action:'save',key:chosen.boss_key,hp:Number(byId('hp').value.replace(/[.,\s]/g,'')),damage:Number(byId('damage').value.replace(/[.,\s]/g,'')),rest:Number(byId('rest').value),lifetime:byId('lifetime').value.trim()===''?null:Number(byId('lifetime').value),enabled:byId('enabled').checked});infoDirty=false;notify(data.message);await refreshBosses();}catch(error){notify(error.message,true);}finally{infoBusy=false;updateDetails();}
};
byId('spawn').onclick=async()=>{
 if(!chosen||infoBusy)return;const delay=Number(byId('spawnDelay').value);if(!Number.isInteger(delay)||delay<0||delay>604800){notify('Thời gian hẹn: 0–604.800 giây.',true);return;}infoBusy=true;updateDetails();
 try{const command=await bossApi({action:'spawn',key:chosen.boss_key,delay});notify('Đang chờ server nhận lệnh…');let done=false;for(let n=0;n<14;n++){await new Promise(resolve=>setTimeout(resolve,1500));const data=await bossApi({action:'status',id:command.id});if(data.command?.completed_at){notify(data.command.result);done=true;break;}}if(!done)notify('Lệnh vẫn đang chờ server phản hồi.');await refreshBosses();}catch(error){notify(error.message,true);}finally{infoBusy=false;updateDetails();}
};
function updateDropHint(){
 const mode=byId('dropMode').value;byId('dropModeHint').textContent=({original:'Dùng toàn bộ bảng rơi trong code gốc. Các dòng tùy chỉnh bên dưới được giữ làm bản nháp.',add:'Giữ đồ gốc và rơi thêm theo các dòng bên dưới.',replace:'Chỉ rơi các dòng bên dưới, thay vật phẩm gốc. Bảng trống sẽ không rơi đồ.',off:'Tắt vật phẩm rơi của boss. Bảng bên dưới vẫn được giữ để bật lại.'})[mode];
}
async function loadDrops(){
 if(!chosen||dropBusy)return;const version=++dropLoadVersion,key=chosen.boss_key;dropBusy=true;byId('saveDrops').disabled=true;byId('dropSync').textContent='Đang tải…';
 try{const [data,catalog]=await Promise.all([bossApi({action:'drop_get',key}),optionCatalog.length?Promise.resolve({options:optionCatalog}):bossApi({action:'drop_catalog'})]);if(version!==dropLoadVersion||key!==chosen?.boss_key)return;optionCatalog=catalog.options;dropConfig=data.config;dropDirty=false;byId('dropMode').value=dropConfig.mode;renderDrops();updateDropHint();showDropSync();}catch(error){if(version===dropLoadVersion)notify(error.message,true);}finally{dropBusy=false;byId('saveDrops').disabled=!dropConfig;}
}
function showDropSync(){if(!dropConfig)return;byId('dropSync').textContent=Number(dropConfig.revision)===0?'Đang dùng cơ chế gốc':Number(dropConfig.applied_revision)===Number(dropConfig.revision)?'Server đã nhận · bản '+dropConfig.revision:'Đã lưu · chờ server nhận';}
function markDrops(){dropDirty=true;byId('dropSync').textContent='Có thay đổi chưa lưu';}
function numericField(title,value,min,max,onchange){const label=node('label',title),input=node('input');input.type='number';input.min=min;input.max=max;input.step=title==='Tỷ lệ %'?'0.0001':'1';input.required=true;input.value=value;input.oninput=()=>{onchange(Number(input.value));markDrops();};label.append(input);return label;}
function optionsEditor(entry,container){
 container.replaceChildren();container.parentElement.querySelector('summary').textContent='Chỉ số vật phẩm ('+entry.options.length+')';for(const [index,option] of entry.options.entries()){
  const line=node('div',null,'boss-option-row'),select=node('select');select.setAttribute('aria-label','Loại chỉ số');for(const available of optionCatalog){const o=new Option('#'+available.id+' · '+available.name,available.id);select.append(o);}select.value=option.id;
  select.onchange=()=>{option.id=Number(select.value);markDrops();};const input=node('input');input.type='number';input.min=0;input.max=32767;input.step=1;input.required=true;input.value=option.param;input.setAttribute('aria-label','Giá trị chỉ số');input.oninput=()=>{option.param=Number(input.value);markDrops();};
  const remove=node('button','×','boss-remove');remove.type='button';remove.setAttribute('aria-label','Xóa chỉ số');remove.onclick=()=>{entry.options.splice(index,1);markDrops();renderDrops();};line.append(select,input,remove);container.append(line);
 }
 const add=node('button','+ Thêm chỉ số','boss-add-option');add.type='button';add.disabled=entry.options.length>=15;add.onclick=()=>{const available=optionCatalog.find(o=>!entry.options.some(p=>p.id===Number(o.id)));if(!available)return;entry.options.push({id:Number(available.id),param:0});markDrops();optionsEditor(entry,container);container.parentElement.open=true;};container.append(add);
}
function renderDrops(){
 byId('dropRows').replaceChildren();const entries=dropConfig?.items||[];byId('dropEmpty').hidden=entries.length>0;
 for(const [index,entry] of entries.entries()){
  const row=node('article',null,'boss-drop-row'),main=node('div',null,'boss-drop-main'),identity=node('div',null,'boss-drop-identity'),name=node('div');name.append(node('strong',entry.name||'Vật phẩm #'+entry.id),node('small','ID '+entry.id));identity.append(icon('/icon/'+(entry.icon_id||1)+'.png'),name);main.append(identity,numericField('Tỷ lệ %',entry.chance,0,100,v=>entry.chance=v),numericField('Ít nhất',entry.min,1,99999,v=>entry.min=v),numericField('Nhiều nhất',entry.max,1,99999,v=>entry.max=v));
  const remove=node('button','×','boss-remove');remove.type='button';remove.setAttribute('aria-label','Xóa '+entry.name);remove.onclick=()=>{dropConfig.items.splice(index,1);markDrops();renderDrops();};main.append(remove);const details=node('details'),summary=node('summary','Chỉ số vật phẩm ('+entry.options.length+')'),options=node('div');details.append(summary,options);optionsEditor(entry,options);row.append(main,details);byId('dropRows').append(row);
 }
}
byId('dropMode').onchange=()=>{markDrops();updateDropHint();};byId('reloadDrops').onclick=()=>{if(dropDirty&&!confirm('Bỏ thay đổi bảng rơi chưa lưu và tải lại?'))return;dropConfig=null;notify('');loadDrops();};
byId('itemSearch').oninput=()=>{
 clearTimeout(pickerTimer);const version=++pickerVersion,query=byId('itemSearch').value.trim();if(!query){byId('itemResults').hidden=true;return;}
 pickerTimer=setTimeout(async()=>{try{const data=await bossApi({action:'drop_search',query});if(version!==pickerVersion)return;const results=byId('itemResults');results.replaceChildren();for(const item of data.items){const button=node('button');button.type='button';button.append(icon('/icon/'+item.icon_id+'.png'),node('span',item.name),node('small','#'+item.id));button.onclick=()=>{if(!dropConfig){notify('Chưa tải xong bảng rơi.',true);return;}if(dropConfig.items.length>=30){notify('Tối đa 30 dòng vật phẩm.',true);return;}dropConfig.items.push({id:Number(item.id),name:item.name,icon_id:Number(item.icon_id),min:1,max:1,chance:100,options:[]});markDrops();renderDrops();byId('itemSearch').value='';results.hidden=true;};results.append(button);}if(!data.items.length)results.append(node('p','Không tìm thấy vật phẩm.','boss-help'));results.hidden=false;}catch(error){notify(error.message,true);}},220);
};
byId('dropForm').onsubmit=async event=>{
 event.preventDefault();if(!chosen||!dropConfig||dropBusy)return;dropBusy=true;byId('saveDrops').disabled=true;
 try{const data=await bossApi({action:'drop_save',key:chosen.boss_key,revision:Number(dropConfig.revision),mode:byId('dropMode').value,items:dropConfig.items.map(({id,min,max,chance,options})=>({id,min,max,chance,options}))});dropConfig=data.config;dropDirty=false;notify(data.message);showDropSync();}catch(error){notify(error.message,true);}finally{dropBusy=false;byId('saveDrops').disabled=false;}
};
setInterval(async()=>{if(!modal.open||!dropConfig||dropDirty||dropBusy||byId('dropsPanel').hidden)return;try{const key=chosen.boss_key;const data=await bossApi({action:'drop_get',key});if(!modal.open||chosen?.boss_key!==key||!dropConfig||dropDirty||dropBusy)return;if(Number(data.config.revision)===Number(dropConfig.revision)){dropConfig.applied_revision=data.config.applied_revision;showDropSync();}else byId('dropSync').textContent='Có bản mới ở phiên khác · bấm Tải lại';}catch(error){}},5000);
refreshBosses();setInterval(refreshBosses,5000);setInterval(updateTimers,1000);

window.addEventListener('beforeunload',event=>{if(infoDirty||dropDirty){event.preventDefault();event.returnValue='';}});
