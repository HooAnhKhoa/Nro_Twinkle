'use strict';
(() => {
const data=JSON.parse(document.getElementById('catalogData').textContent);
let items=data.items, page=1, current=null, editing=false, saving=false, opener=null;
const $=id=>document.getElementById(id), form=$('itemForm'), size=48;
const types={0:'Áo',1:'Quần',2:'Găng',3:'Giày',4:'Rada',5:'Cải trang',6:'Đậu thần',7:'Sách kỹ năng',8:'Đồ nhiệm vụ',9:'Vàng',10:'Ngọc',11:'Vật phẩm đặc biệt',12:'Ngọc Rồng',13:'Bùa',14:'Đá nâng cấp',15:'Nguyên liệu',16:'Bình nước phép',17:'Phụ kiện',18:'Pet',23:'Thú cưỡi',24:'Thú cưỡi',19:'Bông tai',22:'Vệ tinh',25:'Gói vật phẩm',27:'Vật phẩm sự kiện',28:'Cờ',29:'Vật phẩm hỗ trợ',30:'Sao pha lê',31:'Bánh Trung Thu',32:'Giáp tập luyện',33:'Thẻ Rada',34:'Hồng ngọc',35:'Sách tuyệt kỹ',36:'Danh hiệu',37:'Sách kỹ năng',38:'Hiệu ứng'};
const planets=['Trái Đất','Namếc','Xayda','Dùng chung'];
const number=new Intl.NumberFormat('vi-VN');
const fold=s=>String(s).normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toLowerCase();
const typeName=n=>types[n]||('Loại '+n);
const shortPower=n=>n>=1e9?number.format(Math.round(n/1e8)/10)+' tỷ':n>=1e6?number.format(Math.round(n/1e5)/10)+' tr':number.format(n);
const node=(tag,cls,text)=>{const el=document.createElement(tag);if(cls)el.className=cls;if(text!==undefined)el.textContent=text;return el;};
function picture(img,id) {
    img.hidden=false;img.onerror=()=>{img.hidden=true;img.parentElement.classList.add('missing-picture');img.parentElement.setAttribute('aria-label','Chưa có ảnh');};
    img.parentElement.classList.remove('missing-picture');img.parentElement.removeAttribute('aria-label');img.src='/icon/'+id+'.png';
}
function options() {
    const values=[...new Set(items.map(i=>i.TYPE))].sort((a,b)=>a-b);
    for(const v of values){$('typeFilter').add(new Option(typeName(v),v));form.elements.TYPE.add(new Option(typeName(v)+' ('+v+')',v));}
}
function render() {
    const query=fold($('search').value.trim()),type=$('typeFilter').value,gender=$('genderFilter').value;
    const list=items.filter(i=>(!query||fold(i.NAME+' '+i.description+' '+i.id).includes(query))&&(type===''||i.TYPE===Number(type))&&(gender===''||i.gender===Number(gender)));
    const sort=$('sort').value;list.sort((a,b)=>sort==='name'?a.NAME.localeCompare(b.NAME,'vi')||a.id-b.id:sort==='power'?a.power_require-b.power_require||a.id-b.id:a.id-b.id);
    const pages=Math.max(1,Math.ceil(list.length/size));page=Math.min(page,pages);
    $('count').textContent=number.format(list.length)+' vật phẩm';$('empty').hidden=list.length>0;
    $('grid').replaceChildren();
    for(const item of list.slice((page-1)*size,page*size)){
        const card=node('button','item-card'+(current&&current.id===item.id?' selected':''));card.type='button';card.dataset.id=item.id;card.setAttribute('aria-label','Xem '+item.NAME);
        const main=node('span','card-main'),pic=node('span','item-picture'),img=node('img');img.alt='';img.loading='lazy';pic.append(img);picture(img,item.icon_id);
        const copy=node('span','card-copy');copy.append(node('span','card-name',item.NAME),node('span','card-description',item.description));
        const badges=node('span','badges');badges.append(node('span','badge',typeName(item.TYPE)),node('span','badge planet-'+item.gender,planets[item.gender]||'Dùng chung'));copy.append(badges);main.append(pic,copy);
        const footer=node('span','card-footer');footer.append(node('strong','', 'ID '+item.id),node('span','',(item.power_require?shortPower(item.power_require)+' SM · ':'')+'Cấp '+item.level));card.append(main,footer);card.onclick=()=>open(item,card);$('grid').append(card);
    }
    $('pagination').replaceChildren();
    if(pages>1){
        const prev=node('button','','← Trước'),next=node('button','','Sau →');prev.type=next.type='button';prev.disabled=page===1;next.disabled=page===pages;
        const change=n=>{page+=n;render();window.scrollTo({top:0});$('search').focus({preventScroll:true});};
        prev.onclick=()=>change(-1);next.onclick=()=>change(1);$('pagination').append(prev,node('span','',page+' / '+pages),next);
    }
}
function facts(item) {
    $('facts').replaceChildren();
    const entries=[['ID vật phẩm',item.id],['Loại',typeName(item.TYPE)],['Hành tinh',planets[item.gender]||'Dùng chung'],['Sức mạnh yêu cầu',number.format(item.power_require)],['Cấp',item.level],['Xếp chồng',item.is_up_to_up?'Có':'Không'],['ID ảnh',item.icon_id]];
    for(const [label,value] of entries){const row=node('div');row.append(node('dt','',label),node('dd','',String(value)));$('facts').append(row);}
}
function values() {
    const result={};
    for(const key of ['TYPE','gender','NAME','description','level','icon_id','part','is_up_to_up','power_require','head','body','leg']){
        const el=form.elements[key];result[key]=el.type==='checkbox'?(el.checked?1:0):el.type==='number'||el.tagName==='SELECT'?Number(el.value):el.value;
    }
    return result;
}
function dirty(){if(!current||!editing)return false;const v=values();return Object.keys(v).some(k=>v[k]!==current[k]);}
function showMessage(text,kind=''){ $('message').textContent=text;$('message').className=kind; }
function mode(edit) {
    editing=edit;$('facts').hidden=edit;$('editFields').hidden=!edit;$('edit').hidden=edit;$('save').hidden=!edit;$('cancel').textContent=edit?'Hủy chỉnh sửa':'Đóng';
}
function fill(item) {
    $('itemTitle').textContent=item.NAME;$('itemDescription').textContent=item.description;picture($('detailIcon'),item.icon_id);facts(item);
    for(const [key,value] of Object.entries(item)){const input=form.elements.namedItem(key);if(input){if(input.type==='checkbox')input.checked=Boolean(value);else input.value=value;}}
    form.querySelector('details').open=false;
}
function open(item,card) {
    opener=card;current=item;fill(item);mode(false);showMessage('');$('overlay').hidden=false;document.body.style.overflow='hidden';$('close').focus();
}
function close() {
    if(saving)return;
    if(dirty()&&!confirm('Bỏ các thay đổi chưa lưu?'))return;
    $('overlay').hidden=true;document.body.style.overflow='';mode(false);current=null;render();
    const target=opener&&document.querySelector('[data-id="'+opener.dataset.id+'"]');if(target)target.focus({preventScroll:true});else $('search').focus({preventScroll:true});
}
$('edit').onclick=()=>{mode(true);showMessage('');form.elements.NAME.focus();};
$('cancel').onclick=()=>{if(editing){if(dirty()&&!confirm('Bỏ các thay đổi chưa lưu?'))return;fill(current);mode(false);showMessage('');$('edit').focus();}else close();};
$('close').onclick=close;$('overlay').onclick=e=>{if(e.target===$('overlay'))close();};
document.addEventListener('keydown',e=>{
    if($('overlay').hidden)return;
    if(e.key==='Escape'){e.preventDefault();close();}
    if(e.key==='Tab'){
        const els=[...$('drawer').querySelectorAll('button,input,textarea,select,summary,[tabindex="0"]')].filter(el=>!el.disabled&&el.getClientRects().length);
        const first=els[0],last=els[els.length-1];if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus();}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}
    }
});
window.addEventListener('beforeunload',e=>{if(dirty()){e.preventDefault();e.returnValue='';}});
form.onsubmit=async e=>{
    e.preventDefault();if(saving||!editing||!form.reportValidity())return;
    saving=true;editFields.inert=true;for(const b of $('drawer').querySelectorAll('button'))b.disabled=true;
    showMessage('Đang lưu…');
    try{
        const response=await fetch('item.php',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...values(),id:current.id,revision:current.revision,csrf:data.csrf})});
        const result=await response.json();if(!response.ok||!result.item)throw Error(result.error||'Không lưu được vật phẩm.');
        items=items.map(i=>i.id===result.item.id?result.item:i);current=result.item;fill(current);mode(false);render();showMessage('Đã lưu thay đổi.','success');
    }catch(error){showMessage(error.message||'Mất kết nối. Vui lòng thử lại.','error');}
    finally{saving=false;editFields.inert=false;for(const b of $('drawer').querySelectorAll('button'))b.disabled=false;}
};
for(const id of ['search','typeFilter','genderFilter','sort'])$(id).addEventListener(id==='search'?'input':'change',()=>{page=1;render();});
options();render();
})();
