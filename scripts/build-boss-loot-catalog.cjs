// Read-only item inventory for the panel. Never executes reward code or estimates probabilities.
const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'..'),base=path.join(root,'src/nro/models/boss');
function files(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?files(path.join(dir,e.name)):[path.join(dir,e.name)]);}
function clean(s){return s.replace(/\/\*[\s\S]*?\*\/|\/\/[^\r\n]*/g,'');}
// Balanced delimiters; commas inside nested calls are not argument separators.
function split(s){let depth=0,start=0,out=[];for(let i=0;i<s.length;i++){if('([{'.includes(s[i]))depth++;if(')]}'.includes(s[i]))depth--;if(s[i]===','&&!depth){out.push(s.slice(start,i).trim());start=i+1;}}out.push(s.slice(start).trim());return out;}
function block(s,p){let d=0;for(let i=p;i<s.length;i++){if(s[i]==='{')d++;if(s[i]==='}'&&--d===0)return s.slice(p+1,i);}return '';}
function paren(s,p){let d=0;for(let i=p;i<s.length;i++){if(s[i]==='(')d++;if(s[i]===')'&&--d===0)return s.slice(p+1,i);}return '';}
const classes=new Map();
for(const file of files(base).filter(f=>f.endsWith('.java'))){const source=clean(fs.readFileSync(file,'utf8'));const pkg=source.match(/package\s+([\w.]+);/),cls=source.match(/public\s+(?:abstract\s+)?class\s+(\w+)(?:\s+extends\s+(\w+))?/);if(pkg&&cls)classes.set(pkg[1]+'.'+cls[1],{source,file,parent:cls[2],name:cls[1]});}
function method(s,name){const m=new RegExp('(?:public|private|protected)\\s+(?:synchronized\\s+|static\\s+|final\\s+)*(?:void|boolean|int)\\s+'+name+'\\s*\\([^)]*\\)\\s*\\{').exec(s);return m?block(s,m.index+m[0].length-1):null;}
function resolve(expr,s,before,seen=new Set()){
 expr=expr.trim().replace(/\((?:short|int)\)\s*/g,'');if(/^\d+$/.test(expr))return [+expr];
 if(seen.has(expr))return null;seen=new Set(seen).add(expr);
 if(expr.includes('?')){const q=expr.indexOf('?'),colon=expr.lastIndexOf(':');const a=resolve(expr.slice(q+1,colon),s,before,seen),b=resolve(expr.slice(colon+1),s,before,seen);return a&&b?[...a,...b]:null;}
 if(/^\{[\s\S]*\}$/.test(expr)||/^new (?:int|short)\[\]\s*\{/.test(expr)){const values=expr.slice(expr.indexOf('{')+1,expr.lastIndexOf('}')).replace(/[{}]/g,'').split(',').map(v=>v.trim()).filter(Boolean);return values.every(v=>/^\d+$/.test(v))?values.map(Number):null;}
 const range=expr.match(/^Util.nextInt\((\d+)\s*,\s*(\d+)\)$/);if(range&&+range[2]-range[1]<2000)return Array.from({length:+range[2]-range[1]+1},(_,i)=>+range[1]+i);
 const variable=expr.match(/^(\w+)(?:\[[\s\S]*\])?$/);if(variable){const name=variable[1],re=new RegExp('\\b'+name+'\\s*=\\s*([^;]+);','g');let m,last;while((m=re.exec(s))&&m.index<before)last=m;if(last)return resolve(last[1],s,last.index,seen);}
 return null;
}
const out={version:1,description:'Các vật phẩm có thể rơi theo nhánh gốc; không phải tất cả cùng rơi.',bosses:{}};
for(const [key,c] of classes){
 if(!c.parent)continue;let owner=c,reward=method(c.source,'reward'),guard=0;
 while(reward===null&&owner.parent&&guard++<10){owner=[...classes.values()].find(x=>x.name===owner.parent)||{};reward=method(owner.source||'','reward');}
 const bodies=[reward||'',method(c.source,'afk')||'',method(c.source,'giftBox')||''];
 const visited=new Set(['reward','afk','giftBox']);
 for(let n=0;n<bodies.length;n++)for(const call of bodies[n].matchAll(/(?<![\w.])(?:this\.)?(\w+)\s*\(/g)){
   const name=call[1];if(visited.has(name))continue;visited.add(name);
   const helper=method(owner.source||'',name);if(helper!==null)bodies.push(helper);
 }
 let ids=new Set(),unresolved=[];
 for(const body of bodies){for(const m of body.matchAll(/new\s+ItemMap\s*\(/g)){const args=split(paren(body,m.index+m[0].length-1));const values=resolve(args[1]||'',body,m.index);if(values)values.forEach(id=>ids.add(id));else unresolved.push(args[1]);}
 if(body.includes('randDoTLBoss(')||body.includes('randDoTL('))for(let i=555;i<=567;i++)ids.add(i);
 if(body.includes('EventDrops.rewardStamp('))ids.add(1558);
 }
 out.bosses[key]={ids:[...ids].sort((a,b)=>a-b),complete:!unresolved.length,unresolved:[...new Set(unresolved)],note:'Ngẫu nhiên / theo điều kiện gốc.'};
}
// Event configuration and nested class identity cannot be inferred from a literal ItemMap argument.
const props=fs.readFileSync(path.join(root,'config/trung-thu-2026.properties'),'utf8');
for(const [kind,keys]of Object.entries({Monkey:['monkey-tail'],Rabbit:['carrot'],Gogeta:['gogeta-costume','moon-board'],Omega:['omega-costume','moon-board']})){
 const ids=keys.map(k=>Number(props.match(new RegExp('^item\\.'+k+'=(\\d+)','m'))[1]));
 out.bosses['nro.models.boss.event_trung_thu.MoonBoss$'+kind]={ids,complete:true,note:'Chỉ khi sự kiện Trung Thu bật và đủ điều kiện nhận thưởng; tỷ lệ lấy từ mục Rơi đồ → Trung Thu.'};
}
out.bosses['nro.models.boss.event_noel.OngGiaNoel'].note='Phát quà theo chu kỳ 60 giây khi có người chơi ở gần; cần bật sự kiện Noel.';
for(const name of ['SonTinh','ThuyTinh'])out.bosses['nro.models.boss.event_hung_vuong.'+name].note='Ấn theo tỷ lệ Giỗ Tổ. Cải trang được trao trễ khi boss này ở trạng thái thưởng sau trận Sơn Tinh / Thủy Tinh; cần bật sự kiện.';
out.bosses['nro.models.boss.Broly.SuperBroly'].note='Thưởng đệ tử nếu người hạ chưa có đệ tử; không rơi vật phẩm gốc xuống đất.';
fs.writeFileSync(path.join(root,'web/boss_native_drops.json'),JSON.stringify(out,null,2)+'\n');
console.log(JSON.stringify(Object.entries(out.bosses).filter(([k,v])=>!v.complete).map(([key,v])=>({key,unresolved:v.unresolved})),null,2));
