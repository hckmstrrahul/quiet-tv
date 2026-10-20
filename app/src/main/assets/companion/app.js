'use strict';
const $ = s => document.querySelector(s);
let token = sessionStorage.getItem('quiet-tv-token') || '', state = null, busy = false, confirmAction = null, toastTimer;
const esc = text => String(text).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function toast(text) { $('#toast').textContent = text; $('#toast').hidden = false; clearTimeout(toastTimer); toastTimer = setTimeout(() => $('#toast').hidden = true, 3200); }
function paired(value) { $('#pairing').hidden = value; $('#dashboard').hidden = !value; $('#connection').textContent = value ? 'Connected to your TV' : 'Local companion'; $('#connection').classList.toggle('connected', value); }
async function request(path, body, mime = 'application/json') {
 const controller = new AbortController(), timer = setTimeout(() => controller.abort(), 10000);
 try {
  const headers = {}; if (token) headers.Authorization = 'Bearer ' + token;
  if (body !== undefined) headers['Content-Type'] = mime;
  const r = await fetch(path, {method:body === undefined ? 'GET':'POST', headers, body:body === undefined ? undefined : (mime === 'application/json' ? JSON.stringify(body) : body), signal:controller.signal, cache:'no-store'});
  const data = await r.json();
  if (!r.ok) { if (r.status === 401 && path !== '/pair') { token = ''; sessionStorage.removeItem('quiet-tv-token'); paired(false); } throw Error(data.error || 'Could not save this change.'); }
  return data;
 } catch(e) { if (e instanceof TypeError || e.name === 'AbortError') throw Error('TV disconnected. Open Connect device on the TV to reconnect.'); throw e; }
 finally { clearTimeout(timer); }
}
async function mutate(action) {
 if (busy) return; busy = true;
 try { state = await request('/api/action', action); render(); toast('Saved on your TV'); }
 catch(e) { toast(e.message); } finally { busy = false; }
}
$('#pair-form').addEventListener('submit', async e => {
 e.preventDefault(); const button = e.submitter; button.disabled = true; $('#pair-error').textContent = '';
 try { const result = await request('/pair', $('#code').value.replace(/\s/g,''), 'text/plain'); token = result.token; sessionStorage.setItem('quiet-tv-token', token); state = await request('/api/state'); paired(true); render(); $('#code').value = ''; }
 catch(e) { $('#pair-error').textContent = e.message; } finally { button.disabled = false; }
});
function render() {
 if (!state) return;
 $('#app-count').textContent = state.apps.length + ' TV apps'; $('#version').textContent = 'Quiet TV ' + state.appVersion;
 $('#accent').value = state.accent; $('#columns').value = state.columns;
 $('#photo-status').textContent = state.photo ? 'Photo background active on your TV.' : 'JPEG, PNG or WebP · up to 4 MB';
 const query = $('#search').value.toLocaleLowerCase();
 const visible = state.apps.filter(a => a.label.toLocaleLowerCase().includes(query));
 $('#app-list').innerHTML = visible.length ? visible.map(a => {
  const i = state.apps.indexOf(a), p = esc(a.package), name = esc(a.label);
  return `<article class="app-card" data-package="${p}"><div class="app-icon" aria-hidden="true">${esc(a.label.slice(0,1))}</div><div><div class="app-label">${name}</div><div class="app-kind">TV app</div></div><div class="app-controls"><button class="icon-button" data-move="-1" aria-label="Move ${name} earlier" ${i===0?'disabled':''}>↑</button><button class="icon-button" data-move="1" aria-label="Move ${name} later" ${i===state.apps.length-1?'disabled':''}>↓</button></div></article>`;
 }).join('') : '<div class="empty">No apps found. Try another search.</div>';
 renderWallpapers();
 $('#preview-tiles').style.gridTemplateColumns = `repeat(${state.columns},1fr)`;
 $('#preview-tiles').innerHTML = Array.from({length:state.columns},()=>'<div></div>').join('');
 $('#preview-tiles').querySelectorAll('div').forEach(el=>el.style.borderColor=state.accent);
}
$('#app-list').addEventListener('click', e => {
 const card=e.target.closest('[data-package]'); if(!card) return;
 const pkg=card.dataset.package;
 const move=e.target.closest('[data-move]'); if(move) mutate({action:'move',package:pkg,direction:Number(move.dataset.move)});
});
$('#search').addEventListener('input',render);
function confirm(title,message,action){$('#confirm-title').textContent=title;$('#confirm-message').textContent=message;confirmAction=action;$('#confirm-dialog').showModal();}
$('#cancel-confirm').addEventListener('click',()=>$('#confirm-dialog').close());
$('#confirm-form').addEventListener('submit',e=>{e.preventDefault();$('#confirm-dialog').close();if(confirmAction)confirmAction();});
document.querySelectorAll('[data-tab]').forEach(button=>button.addEventListener('click',()=>{
 document.querySelectorAll('[data-tab]').forEach(b=>{const active=b===button;b.classList.toggle('active',active);b.setAttribute('aria-selected',String(active));});
 document.querySelectorAll('.tab-panel').forEach(p=>p.hidden=p.id!=='tab-'+button.dataset.tab);
}));
$('#accent').addEventListener('change',e=>mutate({action:'appearance',accent:e.target.value}));
$('#columns').addEventListener('change',e=>mutate({action:'appearance',columns:Number(e.target.value)}));
for(const [id,key,colors] of [['accent-swatches','accent',[['Sage','#AADBCB'],['Ice','#94C8FF'],['Lavender','#C9B7F5'],['Amber','#EACA92']]]]){
 for(const [name,color] of colors){const b=document.createElement('button');b.textContent=name;b.addEventListener('click',()=>mutate({action:'appearance',[key]:color}));$('#'+id).appendChild(b);}
}
$('#wallpaper-file').addEventListener('change',async e=>{
 const file=e.target.files[0];if(!file)return;
 if(file.size>4*1024*1024){toast('Choose an image under 4 MB.');e.target.value='';return;}
 toast('Sending your photo…');
 try{state=await request('/api/wallpaper',file,file.type||'application/octet-stream');render();toast('Your new background is on the TV');}catch(e){toast(e.message);}finally{e.target.value='';}
});
$('#clear-wallpaper').addEventListener('click',()=>mutate({action:'clear-wallpaper'}));
$('#backup').addEventListener('click',async()=>{try{const data=await request('/api/backup'),url=URL.createObjectURL(new Blob([JSON.stringify(data,null,2)],{type:'application/json'})),a=document.createElement('a');a.href=url;a.download='quiet-tv-layout.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}catch(e){toast(e.message);}});
$('#restore-file').addEventListener('change',async e=>{const file=e.target.files[0];if(!file)return;try{if(file.size>65536)throw Error('Choose a Quiet TV layout file under 64 KB.');const data=JSON.parse(await file.text());confirm('Restore this layout?','Your current colors and ordering will be replaced. The gallery background will be restored.',async()=>{try{state=await request('/api/restore',data);render();toast('Layout restored on your TV');}catch(e){toast(e.message);}});}catch(e){toast(e.message);}finally{e.target.value='';}});
$('#refresh').addEventListener('click',async()=>{try{state=await request('/api/state');render();toast('Up to date');}catch(e){toast(e.message);}});
$('#forget').addEventListener('click',()=>{token='';sessionStorage.removeItem('quiet-tv-token');paired(false);});
if(token) request('/api/state').then(data=>{state=data;paired(true);render();}).catch(e=>{$('#pair-error').textContent=e.message;paired(false);});

const wallpaperUrls=new Map();let loadingWallpapers=false;
function renderWallpapers(){
 const options=[{id:'shuffle',title:'Shuffle all · 10s'},...(state.wallpapers||[])];
 $('#wallpaper-grid').innerHTML=options.map(p=>{
  const selected=!state.photo&&(p.id==='shuffle'?state.wallpaperShuffle:!state.wallpaperShuffle&&state.wallpaperId===p.id);
  const image=wallpaperUrls.get(p.id==='shuffle'?state.wallpapers[0].id:p.id);
  return `<button class="wallpaper-choice" data-backdrop="${esc(p.id)}" aria-pressed="${selected}"><span class="wallpaper-picture">${image?'<img src="'+image+'" alt="">':''}${p.id==='shuffle'?'<span class="shuffle-symbol">⤨</span>':''}${selected?'<span class="wallpaper-check">✓</span>':''}</span><span>${esc(p.title)}</span></button>`;
 }).join('');
 const preview=wallpaperUrls.get(state.wallpaperId);$('#preview').style.backgroundImage=preview?`linear-gradient(#0005,#0008),url(${preview})`:'';
 loadWallpaperThumbs();
}
async function loadWallpaperThumbs(){
 if(loadingWallpapers||!state)return;loadingWallpapers=true;
 const session=token;
 try{
  for(const p of state.wallpapers||[]){
   if(wallpaperUrls.has(p.id))continue;
   const r=await fetch('/api/backgrounds/'+encodeURIComponent(p.id),{headers:{Authorization:'Bearer '+session},signal:AbortSignal.timeout(10000)});
   if(!r.ok)break;const blob=await r.blob();if(token!==session)break;
   wallpaperUrls.set(p.id,URL.createObjectURL(blob));renderWallpapers();
  }
 }catch(_){}finally{loadingWallpapers=false;}
}
$('#wallpaper-grid').addEventListener('click',e=>{const b=e.target.closest('[data-backdrop]');if(b)mutate({action:'backdrop',id:b.dataset.backdrop});});
