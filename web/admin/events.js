(()=>{'use strict';
let state=null,busy=false;
const $=s=>document.querySelector(s),cards=[...document.querySelectorAll('[data-event]')];
async function api(body){const response=await fetch('events.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,csrf})});const data=await response.json();if(!response.ok)throw Error(data.error||'Không thể kết nối.');return data;}
function lock(){cards.forEach(card=>card.querySelector('button').disabled=busy||!state);document.querySelectorAll('[data-undo]').forEach(button=>button.disabled=busy);$('#reloadEvents').disabled=busy;}
function render(){const values=new Map(state.events.map(row=>[row.event_key,Number(row.enabled)===1]));let count=0;
cards.forEach(card=>{const on=values.get(card.dataset.event)===true;count+=Number(on);card.classList.toggle('is-on',on);card.querySelector('.event-state').textContent=on?'Đang bật':'Đang tắt';card.querySelector('button').setAttribute('aria-checked',String(on));});
$('#eventCount').textContent=`${count}/${cards.length} hoạt động đang bật`;
const config=state.config,online=config.age!==null&&Number(config.age)<=12;
$('#eventSync').textContent=!online?'Game chưa kết nối bản hỗ trợ EV':Number(config.revision)===Number(config.applied_revision)?'✓ Game đã nhận cấu hình':'◷ Đang chờ game nhận cấu hình…';
$('#eventSync').classList.toggle('synced',online&&Number(config.revision)===Number(config.applied_revision));
const latest=new Map();state.history.forEach(row=>{if(!latest.has(row.event_key))latest.set(row.event_key,row.id);});
const history=$('#eventHistory');history.replaceChildren();
if(!state.history.length)history.textContent='Chưa có thay đổi.';
state.history.forEach(row=>{const line=document.createElement('div');line.className='event-history-row';const text=document.createElement('span');text.textContent=`${row.created_at} · ${eventCatalog[row.event_key]?.name||row.event_key}: ${Number(row.before_enabled)?'Bật':'Tắt'} → ${Number(row.after_enabled)?'Bật':'Tắt'}${row.undone_at?' · Đã hoàn tác':''}`;line.append(text);
if(!row.undone_at&&latest.get(row.event_key)===row.id&&values.get(row.event_key)===(Number(row.after_enabled)===1)){const button=document.createElement('button');button.type='button';button.textContent='Hoàn tác';button.dataset.undo=row.id;button.onclick=()=>change({action:'undo',event_key:row.event_key,audit_id:Number(row.id)});line.append(button);}history.append(line);});lock();}
async function load(){state=await api({action:'read'});render();}
async function change(body){if(busy||!state)return;busy=true;lock();try{const result=await api({...body,revision:Number(state.config.revision)});$('#eventMessage').textContent=result.message;await load();}catch(error){$('#eventMessage').textContent=error.message;try{await load();}catch{state=null;}}finally{busy=false;lock();}}
cards.forEach(card=>card.querySelector('button').onclick=()=>change({action:'set',event_key:card.dataset.event,enabled:card.querySelector('button').getAttribute('aria-checked')!=='true'}));
$('#reloadEvents').onclick=()=>{if(!busy)load().catch(error=>{$('#eventMessage').textContent=error.message;state=null;lock();});};
load().catch(error=>{$('#eventMessage').textContent=error.message;lock();});
setInterval(()=>{if(!busy&&!document.hidden)load().catch(()=>{$('#eventSync').textContent='Mất kết nối web. Tải lại để kiểm tra.';state=null;lock();});},5000);
})();
