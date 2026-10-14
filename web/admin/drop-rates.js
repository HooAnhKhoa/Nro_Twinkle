const el=id=>document.getElementById(id);
let state=null,busy=false;
const cards=[...document.querySelectorAll('[data-category]')];
const format=n=>Number(n).toLocaleString('vi-VN',{maximumFractionDigits:8});
const ruleNodes=new Map();
async function api(body){
    const response=await fetch('drop_rates.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,csrf})});
    const data=await response.json();if(!response.ok||data.error)throw Error(data.error||'Không thể lưu cấu hình');return data;
}
function inheritedChance(rule){
    const setting=state.groups[rule.category];
    return Math.min(100,Math.max(0,setting?.mode==='percent'?Number(setting.value):rule.base*Number(state.rates.drop_multiplier)*Number(setting?.value??1)));
}
function paintRule(key,fill=false){
    const node=ruleNodes.get(key),meta=rulesCatalog[key];if(!node||!state)return;
    const custom=state.regional?.[key]!=null,raw=custom?Number(state.regional[key]):inheritedChance(meta);
    const blocked=Number(state.rates.drop_multiplier)<=0||Number(state.groups[meta.category]?.value??1)<=0;
    node.chance.textContent=format(blocked?0:raw)+'%';
    node.chance.title=custom?'Tỷ lệ riêng trước bonus: '+format(raw)+'%':'Tỷ lệ mặc định sau hệ số';
    node.status.textContent=blocked?'Đang tắt bởi hệ số x0':custom?'Đã chỉnh riêng':'Theo hệ số nhóm / toàn server';
    if(fill||!node.form.dataset.dirty)node.input.value=Number(raw.toFixed(8));
    node.reset.disabled=busy||!custom;node.save.disabled=busy;
}
function setupRules(){
    for(const card of cards){
        const list=card.querySelector('.regional-rules');list.replaceChildren();
        for(const [key,meta] of Object.entries(rulesCatalog)){
            if(meta.category!==card.dataset.category)continue;
            const section=document.createElement('section');section.className='regional-rule';section.dataset.rule=key;
            const heading=document.createElement('div');heading.className='rule-heading';
            const title=document.createElement('strong');title.textContent=meta.label;
            const chance=document.createElement('span');chance.className='rule-chance';heading.append(title,chance);
            const maps=document.createElement('p');maps.className='rule-maps';maps.textContent=meta.maps;
            section.append(heading,maps);
            if(meta.note){const note=document.createElement('p');note.className='rule-note';note.textContent=meta.note;section.append(note);}
            const form=document.createElement('form');form.className='rule-form';
            const label=document.createElement('label');label.htmlFor='rule-'+key;label.textContent='Tỷ lệ riêng (%)';
            const input=document.createElement('input');input.id=label.htmlFor;input.type='number';input.min='0';input.max='100';input.step='any';input.required=true;input.inputMode='decimal';
            const saveButton=document.createElement('button');saveButton.type='submit';saveButton.textContent='Lưu';
            const reset=document.createElement('button');reset.type='button';reset.className='quiet rule-default';reset.textContent='Mặc định';reset.title='Bỏ tỷ lệ riêng và dùng lại hệ số nhóm / toàn server';
            const controls=document.createElement('div');controls.className='rule-controls';controls.append(input,saveButton,reset);form.append(label,controls);
            const status=document.createElement('small');status.className='rule-state';const feedback=document.createElement('span');feedback.className='rule-feedback';feedback.setAttribute('role','status');
            section.append(form,status,feedback);list.append(section);
            ruleNodes.set(key,{form,input,chance,status,reset,save:saveButton});
            input.oninput=()=>form.dataset.dirty='1';
            form.onsubmit=async event=>{event.preventDefault();form.dataset.dirty='1';if(await save({action:'rule',rule:key,value:Number(input.value)},feedback)){delete form.dataset.dirty;paintRule(key,true);}};
            reset.onclick=async()=>{if(await save({action:'inherit',rule:key},feedback)){delete form.dataset.dirty;paintRule(key,true);}};
        }
    }
}
function paint(fill){
    const rates=state.rates;el('globalNow').textContent='×'+format(rates.drop_multiplier);if(fill)el('globalValue').value=rates.drop_multiplier;
    const runtime=state.regional_runtime,online=runtime?.age!=null&&Number(runtime.age)>=0&&Number(runtime.age)<=20;
    el('connection').textContent=online?(Number(rates.revision)===Number(runtime.applied_revision)?'● Đã đồng bộ tỷ lệ riêng':'● Chờ đồng bộ tỷ lệ riêng'):'○ Cần server hỗ trợ tỷ lệ riêng đang chạy';
    for(const card of cards){
        const setting=state.groups[card.dataset.category];
        card.querySelector('.group-mode').textContent=setting?'×'+format(setting.value):'Chưa hỗ trợ';
        if(fill&&setting){card.querySelector('input[name=mode]').value=setting.mode;card.querySelector('input[name=value]').value=setting.value;}
    }
    for(const key of ruleNodes.keys())paintRule(key,fill);
}
async function refresh(fill=false){try{state=await api({action:'read'});paint(fill);return true;}catch(error){el('message').textContent=error.message;el('message').className='error';return false;}}
async function save(body,feedback){
    if(busy||!state)return false;busy=true;document.querySelectorAll('button').forEach(b=>b.disabled=true);
    feedback.textContent='Đang lưu…';feedback.classList.remove('error');
    try{const result=await api({...body,revision:Number(state.rates.revision)});feedback.textContent=result.message;return await refresh(false);}
    catch(error){feedback.textContent=error.message;feedback.classList.add('error');return false;}
    finally{busy=false;document.querySelectorAll('button').forEach(b=>b.disabled=false);if(state)paint(false);}
}
el('globalForm').onsubmit=event=>{event.preventDefault();save({action:'global',value:Number(el('globalValue').value)},el('message'));};
for(const card of cards){const form=card.querySelector('.group-form');if(form)form.onsubmit=event=>{event.preventDefault();save({action:'group',category:card.dataset.category,mode:form.elements.mode.value,value:Number(form.elements.value.value)},card.querySelector('.card-feedback'));};}
el('reload').onclick=()=>{if(!busy){for(const {form} of ruleNodes.values())delete form.dataset.dirty;refresh(true);el('message').textContent='';document.querySelectorAll('.card-feedback,.rule-feedback').forEach(node=>node.textContent='');}};
setupRules();refresh(true);setInterval(()=>{if(!busy)refresh(false);},5000);
