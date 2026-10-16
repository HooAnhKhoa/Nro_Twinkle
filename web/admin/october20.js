(()=>{
 const root=document.getElementById('october20Settings');if(!root)return;
 document.getElementById('openOctober20Settings')?.addEventListener('click',()=>{root.open=true;root.scrollIntoView({behavior:'smooth',block:'start'});});
 const form=document.getElementById('october20Form'),message=document.getElementById('october20Message');let revision=null,busy=false,loaded=false;
 async function api(body){const response=await fetch('october20_api.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,csrf:root.dataset.csrf})});const data=await response.json();if(!response.ok||data.error)throw Error(data.error||'Không kết nối được.');return data;}
 function lock(value){busy=value;for(const node of form.querySelectorAll('input,button'))node.disabled=value||(!loaded&&node.id!=='october20Reload');}
 async function load(){if(busy)return;lock(true);try{const data=await api({action:'read'});revision=Number(data.revision);for(const input of form.querySelectorAll('input'))input.value=data.settings[input.name];loaded=true;message.textContent='Cấu hình phiên bản '+revision+'.';}catch(error){message.textContent=error.message;}finally{lock(false);}}
 root.addEventListener('toggle',()=>{if(root.open&&!loaded)load();});document.getElementById('october20Reload').addEventListener('click',load);
 async function save(action){if(busy||!loaded)return;let settings={};for(const input of form.querySelectorAll('input'))settings[input.name]=Number(input.value);lock(true);try{const data=await api({action,revision,settings});busy=false;await load();message.textContent=data.message;}catch(error){message.textContent=error.message;}finally{lock(false);}}
 form.addEventListener('submit',event=>{event.preventDefault();if(form.reportValidity())save('save');});document.getElementById('october20Undo').addEventListener('click',()=>save('undo'));
})();
