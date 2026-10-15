(()=>{
const byId=id=>document.getElementById(id),fields=['param_from_1','param_to_1','param_from_2','param_to_2'];
const races=['Trái Đất','Namek','Xayda','Dùng chung'];let state=null,busy=false;const nodes=new Map();
const describe=t=>t.name.replace(/p([0-3])/g,(_,i)=>t[fields[Number(i)]]);
const normalize=s=>String(s).toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu,'').replace(/đ/g,'d');
async function call(body){const response=await fetch('intrinsic_templates.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,csrf})});const data=await response.json();if(!response.ok||data.error)throw Error(data.error||'Không thể tải cấu hình');return data;}
function chooseTab(catalog){byId('catalogTab').setAttribute('aria-selected',String(catalog));byId('playerTab').setAttribute('aria-selected',String(!catalog));byId('intrinsicCatalogPanel').hidden=!catalog;byId('playerIntrinsicSection').hidden=catalog;}
byId('catalogTab').onclick=()=>chooseTab(true);byId('playerTab').onclick=()=>chooseTab(false);
function createCard(entry){
    const t=entry.template,card=document.createElement('details');card.className='intrinsic-template-card';card.dataset.intrinsic=t.id;card.dataset.race=t.gender;
    const summary=document.createElement('summary'),icon=document.createElement('img');icon.className='intrinsic-template-icon';icon.alt='';icon.src='/icon/'+t.icon+'.png';icon.onerror=()=>{icon.onerror=null;icon.src='/icon/1.png';};
    const title=document.createElement('div');title.className='intrinsic-template-title';const name=document.createElement('strong'),meta=document.createElement('small');title.append(name,meta);
    const arrow=document.createElement('span');arrow.className='intrinsic-template-arrow';arrow.textContent='⌄';arrow.setAttribute('aria-hidden','true');summary.append(icon,title,arrow);card.append(summary);
    const form=document.createElement('form');form.className='intrinsic-template-body';const inputs={};
    for(let pair=1;pair<=2;pair++){
        const group=document.createElement('fieldset');group.className='intrinsic-range-pair';group.hidden=pair===2&&!/p[23]/.test(t.name);
        const legend=document.createElement('legend');legend.textContent=pair===1?'Chỉ số chính (%)':'Chỉ số thứ hai (%)';group.append(legend);
        const range=document.createElement('div');range.className='intrinsic-range-inputs';
        for(const [prefix,labelText] of [['from','Thấp nhất'],['to','Cao nhất']]){const field='param_'+prefix+'_'+pair,label=document.createElement('label'),input=document.createElement('input');label.textContent=labelText;input.type='number';input.name=field;input.min='0';input.max='32767';input.step='1';input.required=true;input.setAttribute('aria-label',labelText+' chỉ số '+pair+' nội tại #'+t.id);label.append(input);range.append(label);inputs[field]=input;}
        group.append(range);form.append(group);
    }
    const preview=document.createElement('p');preview.className='intrinsic-template-preview';form.append(preview);
    const actions=document.createElement('div');actions.className='intrinsic-template-actions';const save=document.createElement('button');save.type='submit';save.className='primary';save.textContent='Lưu nội tại';const cancel=document.createElement('button');cancel.type='button';cancel.textContent='Hủy thay đổi';actions.append(save,cancel);form.append(actions);
    const feedback=document.createElement('p');feedback.className='intrinsic-template-feedback';feedback.setAttribute('role','status');form.append(feedback);card.append(form);byId('intrinsicTemplateCards').append(card);
    const node={card,form,inputs,name,meta,preview,feedback,save,cancel,entry,dirty:false};nodes.set(t.id,node);
    const draft=()=>{const next={...node.entry.template};for(const field of fields)next[field]=inputs[field].value;return next;};
    form.oninput=()=>{node.dirty=true;card.classList.add('intrinsic-template-dirty');meta.textContent='ID '+t.id+' · '+races[t.gender]+' · Chưa lưu';preview.textContent=describe(draft());};
    cancel.onclick=()=>{node.dirty=false;paintNode(node,node.latestEntry||node.entry);feedback.textContent='';};
    form.onsubmit=async event=>{event.preventDefault();if(busy)return;const values={};for(const field of fields)values[field]=Number(inputs[field].value);await mutate({action:'save',intrinsic_id:t.id,etag:node.entry.etag,...values},feedback,node);};
    return node;
}
function paintNode(node,entry){
    node.latestEntry=entry;if(node.dirty)return;node.entry=entry;const t=entry.template;
    node.name.textContent=describe(t);node.meta.textContent='ID '+t.id+' · '+races[t.gender]+(entry.conflict?' · Các bản trùng có cấu hình khác nhau':'');node.card.classList.remove('intrinsic-template-dirty');
    for(const field of fields)node.inputs[field].value=t[field];node.preview.textContent=describe(t);node.save.disabled=busy||entry.conflict;
}
function filter(){const query=normalize(byId('intrinsicSearch').value),race=byId('intrinsicRace').value;let visible=0;for(const [id,node]of nodes){const t=node.entry.template,show=(race==='all'||String(t.gender)===race||(race!=='3'&&t.gender===3))&&normalize(t.name+' '+describe(t)+' '+id).includes(query);node.card.hidden=!show;if(show)visible++;}byId('intrinsicCount').textContent=visible+' / '+nodes.size+' nội tại · Bấm vào từng mục để chỉnh mức. Chỉ số từ 0 đến 32767.';}
function history(){
    const list=byId('intrinsicTemplateHistory');list.replaceChildren();
    for(const item of state.history){const row=document.createElement('div');row.className='intrinsic-history-row';const text=document.createElement('span');let before,after;try{before=JSON.parse(item.before_json);after=JSON.parse(item.after_json);}catch(error){continue;}
        text.textContent=item.created_at+' · #'+item.intrinsic_id+' · '+describe(before)+' → '+describe(after);
        const button=document.createElement('button');button.type='button';button.textContent=item.undone_at?'Đã hoàn tác':'Hoàn tác';button.disabled=busy||!!item.undone_at;button.onclick=()=>{if(confirm('Khôi phục mức nội tại trước lần sửa này?'))mutate({action:'undo',intrinsic_id:Number(item.intrinsic_id),audit_id:Number(item.id)},byId('intrinsicCatalogMessage'));};row.append(text,button);list.append(row);}
    if(!list.childElementCount)list.textContent='Chưa có lịch sử chỉnh cấu hình nội tại.';
}
async function refresh(reset=false){
    try{state=await call({action:'read'});const config=state.config,live=config.age!=null&&Number(config.age)>=0&&Number(config.age)<=20;byId('intrinsicSync').textContent=live?(Number(config.revision)===Number(config.applied_revision)?'● Đã đồng bộ với game':'● Chờ game đồng bộ'):'○ Cần server hỗ trợ nội tại đang chạy';
        for(const entry of state.templates){if(entry.template.id===0)continue;const node=nodes.get(entry.template.id)||createCard(entry);if(reset)node.dirty=false;paintNode(node,entry);}
        filter();history();return true;
    }catch(error){byId('intrinsicCatalogMessage').textContent=error.message;return false;}
}
async function mutate(body,feedback,node){
    if(busy||!state)return;busy=true;document.querySelectorAll('#intrinsicCatalogPanel button').forEach(button=>button.disabled=true);feedback.classList.remove('error');feedback.textContent='Đang lưu…';if(node)Object.values(node.inputs).forEach(input=>input.disabled=true);
    try{const result=await call({...body,revision:Number(state.config.revision)});if(node)node.dirty=false;feedback.textContent=result.message;await refresh();}
    catch(error){feedback.textContent=error.message;feedback.classList.add('error');}
    finally{busy=false;if(node)Object.values(node.inputs).forEach(input=>input.disabled=false);document.querySelectorAll('#intrinsicCatalogPanel button').forEach(button=>button.disabled=false);for(const n of nodes.values())n.save.disabled=n.entry.conflict;history();}
}
byId('intrinsicSearch').oninput=filter;byId('intrinsicRace').onchange=filter;byId('reloadIntrinsicCatalog').onclick=()=>{if(!busy){refresh(true);byId('intrinsicCatalogMessage').textContent='';for(const node of nodes.values())node.feedback.textContent='';}};
refresh();setInterval(()=>{if(!busy)refresh();},5000);
})();
