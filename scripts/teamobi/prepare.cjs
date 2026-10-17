// Build a canonical-only catalog and one-time migration plan from a read-only DB export.
const fs=require('fs'),path=require('path');
const dir=process.argv[2]||'build/teamobi-import';
const read=n=>JSON.parse(fs.readFileSync(path.join(dir,n+'.json'),'utf8').replace(/^\uFEFF/,''));
const source=read('ItemTemplates'),parts=read('Parts'),old=read('current');
if(old.item_template.length!==2001||old.item_template.find(x=>+x.id===2000)?.NAME!=='Vé Thành Cổ')throw Error('Expected the backed-up legacy catalog; do not rebuild from an already-migrated database.');
const norm=s=>String(s).normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/gi,'d').toLowerCase().replace(/cai trang|\bct\b/g,'').replace(/[^a-z0-9]/g,'');
const overrides={758:758,1536:1732,1537:1863,1748:1748,1758:1936,1759:1938,1760:1939,1765:1813,1766:1814,1767:1815,1768:1816,1769:1817,1770:1818,1771:1819,1774:1732,1775:1863,1776:1823,1777:1824,1779:1797,1789:1830,1792:2040,1793:2040,1796:1890,1802:1862,1805:993,1808:1020,1809:1586,1810:1586,1811:1586,1819:1884,1824:1935,1825:1835,1840:1840,1841:1697,1842:1685,1843:1731,1844:1557,1845:1567,1846:1632,1847:1775,1848:1777,1849:1865,1850:1866,1851:1018,1863:1996,1900:1913,2000:1876};
const byName=new Map(),byIcon=new Map();for(const a of source){const n=norm(a.name);if(!byName.has(n))byName.set(n,[]);byName.get(n).push(a);const k=a.type+':'+a.iconID;if(!byIcon.has(k))byIcon.set(k,[]);byIcon.get(k).push(a);}
const itemMap={},report=[];
for(const o of old.item_template){const id=+o.id,x=source[id];let target;
 if(Object.hasOwn(overrides,id))target=overrides[id];
 else if(!o.NAME || x&&norm(x.name)===norm(o.NAME))target=id;
 else {let match=byName.get(norm(o.NAME))||[];if(match.length!==1)match=byIcon.get(o.TYPE+':'+o.icon_id)||[];if(x&&x.iconID===+o.icon_id&&x.type===+o.TYPE)target=id;else if(match.length===1)target=match[0].id;else throw Error('Unresolved legacy item '+id+' '+o.NAME);}
 if(!source[target]||source[target].id!==target)throw Error('Not a canonical ID '+target);
 itemMap[id]=target;if(id!==target)report.push({old:id,new:target,oldName:o.NAME,newName:source[target].name});
}
const partKey=x=>JSON.stringify([x.type,x.pi.map(p=>[p.id,p.dx,p.dy])]);
const partIndex=new Map(parts.map((x,i)=>[partKey(x),i]));
const partMap={};const partFallback=[];
for(const o of old.part){const id=+o.id,details=JSON.parse(o.DATA),value={type:+o.TYPE,pi:details.map(p=>({id:p[0],dx:p[1],dy:p[2]}))};let target=parts[id]&&partKey(parts[id])===partKey(value)?id:partIndex.get(partKey(value));
 if(target===undefined){const ids=new Set(value.pi.map(p=>p.id).filter(n=>n!==2955));let best=-1,score=0;parts.forEach((p,i)=>{if(p.type!==value.type)return;const hits=p.pi.filter(q=>ids.has(q.id)).length;if(hits>score){best=i;score=hits;}});target=best>=0?best:(parts[id]?.type===value.type?id:value.type);partFallback.push({old:id,new:target,sharedFrames:score});}
 partMap[id]=target;
}
// Known old outfit definitions can identify costumes omitted from ItemTemplates JSON.
const outfits=new Map();for(const o of old.item_template)if(+o.TYPE===5&&+o.head>=0){const to=itemMap[o.id];if(source[to].type===5&&source[to].iconID===+o.icon_id){outfits.set(to,{head:partMap[o.head],body:partMap[o.body],leg:partMap[o.leg],method:'existing-frame-map'});}}
const groups=[];for(let i=0;i<parts.length-2;i++)if(parts[i].type===0&&parts[i+1].type===1&&parts[i+2].type===2){const ids=parts.slice(i,i+3).flatMap(p=>p.pi.map(x=>x.id)).filter(x=>x!==2955);groups.push({head:i,body:i+1,leg:i+2,min:Math.min(...ids),max:Math.max(...ids)});}
const unresolved=[];
for(const a of source.filter(x=>x.type===5||x.type===18)){
 if(outfits.has(a.id))continue;
 const exact=groups.filter(g=>parts[g.head].pi.some(p=>p.id===a.iconID));
 const nearby=groups.filter(g=>g.max<a.iconID&&a.iconID-g.max<=4).sort((x,y)=>y.max-x.max);
 const interval=groups.filter(g=>g.min<=a.iconID&&a.iconID<=g.max+15).sort((x,y)=>y.min-x.min);
 const match=exact.length===1?exact[0]:nearby[0]||interval[0];
 if(match)outfits.set(a.id,{head:match.head,body:match.body,leg:match.leg,method:exact.length===1?'head-frame':'adjacent-icon'});
 else unresolved.push({id:a.id,name:a.name,icon:a.iconID});
}
outfits.set(1593,{head:1467,body:1474,leg:1475,method:'shared-head-body-variant'});
const items=source.map(a=>({id:a.id,TYPE:a.type,gender:a.gender,NAME:a.name,description:a.description,level:a.level,icon_id:a.iconID,part:a.part,is_up_to_up:([6,7,8,12,13,14,15,16,22,27,29,30,31,33,35,37].includes(a.type)?1:+a.isUpToUp),power_require:a.strRequire,gold:0,gem:0,head:outfits.get(a.id)?.head??-1,body:outfits.get(a.id)?.body??-1,leg:outfits.get(a.id)?.leg??-1}));
for(const o of old.item_template){const target=items[itemMap[o.id]];if(target&&+o.id===itemMap[o.id]){target.gold=+o.gold;target.gem=+o.gem;}}
const flagRows=read('flag-rows'),flags=new Map(flagRows.map(x=>[+x.id,x])),portedFlags=[];
for(const o of old.item_template){const target=items[itemMap[o.id]],flag=flags.get(+o.part);if(+o.TYPE===11&&target?.TYPE===11&&+o.icon_id===target.icon_id&&flag&&target.part>=0&&target.part!==+o.part){portedFlags.push({...flag,id:target.part,NAME:target.NAME});}}
// Recovered original animation definitions also belong in fresh canonical migrations.
const restoredPath='config/teamobi-restored-flags.json';
if(fs.existsSync(restoredPath))for(const flag of JSON.parse(fs.readFileSync(restoredPath,'utf8'))){
 const item=items[flag.item_id];
 if(item&&flag.original_part!==undefined&&item.part===flag.original_part)item.part=flag.flag_id;
 if(!item||item.TYPE!==11||item.part!==flag.flag_id||item.icon_id!==flag.icon_id)throw Error('Recovered flag does not match canonical item '+flag.item_id);
 if(!portedFlags.some(x=>+x.id===flag.flag_id))portedFlags.push({id:flag.flag_id,icon_data:flag.frames.join(','),NAME:flag.name,gold:-1,gem:-1,icon_id:flag.icon_id});
}
const imageIds=new Set(read('images').map(x=>x.id));const existing=new Set(fs.readdirSync('data/icon/x4').filter(x=>/^\d+\.png$/.test(x)).map(x=>+x.slice(0,-4)));
const missingIcons=[...new Set([...source.map(x=>x.iconID),...parts.flatMap(p=>p.pi.map(x=>x.id))])].filter(x=>!imageIds.has(x)&&!existing.has(x));
const plan={portedFlags,baselineItemsSha256:require('crypto').createHash('sha256').update(JSON.stringify(old.item_template)).digest('hex'),baselinePartsSha256:require('crypto').createHash('sha256').update(JSON.stringify(old.part)).digest('hex'),version:'teamobi-canonical-20261017',itemMap,partMap,items,parts:parts.map((p,id)=>({id,TYPE:p.type,DATA:JSON.stringify(p.pi.map(x=>[x.id,x.dx,x.dy]))})),itemChanges:report,partFallback,unresolvedOutfits:unresolved,outfitMatches:Object.fromEntries(outfits),missingIcons,missingItemIcons:source.filter(x=>missingIcons.includes(x.iconID)).map(x=>({id:x.id,name:x.name,icon:x.iconID}))};
fs.writeFileSync(path.join(dir,'plan.json'),JSON.stringify(plan,null,2));
console.log(JSON.stringify({items:items.length,parts:parts.length,changedItems:report.length,partFallback:partFallback.length,unresolvedCostumes:unresolved.length,missingIcons:missingIcons.length,maxDescription:Math.max(...items.map(x=>[...x.description].length)),nappa:items[2075]},null,2));
