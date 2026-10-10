/**
 * Minimal mobile-first administrator interface. The browser does not persist
 * the API key or store it in a cookie/URL. Every privileged request sends it
 * in X-Admin-Key over HTTPS. Never serve payment card data here.
 */
export function adminPage() {
  const page = `<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex,nofollow">
<title>Лицензии — Замер потолка</title>
<style>
:root{font-family:system-ui,-apple-system,Roboto,sans-serif;color:#21384b;background:#f6f7f8}
*{box-sizing:border-box}
body{margin:0;padding:18px 12px 40px}
main{max-width:690px;margin:auto}
h1{font-size:23px;margin:8px 0}
p{font-size:14px;color:#526778;line-height:1.55}
section{border:1px solid #dce3e7;background:white;border-radius:16px;padding:16px;margin:14px 0}
label{font-size:13px;color:#526778;display:block;margin:12px 0 6px}
input,select{font-size:16px;width:100%;border-radius:10px;border:1px solid #bac9d1;padding:12px}
button{font-size:14px;padding:11px 15px;border:0;border-radius:10px;background:#244e67;color:white;cursor:pointer;margin:6px 6px 0 0}
button.secondary{background:#e9eff2;color:#274153}
button.danger{background:#ae4343}
button:disabled{opacity:.45;cursor:default}
.entry{border-top:1px solid #e6ecef;padding:14px 0}
.entry b{display:block;font-size:15px}
.meta{font-size:12px;line-height:1.6;color:#516573;overflow-wrap:anywhere}
.note{border-left:3px solid #d1a667;padding-left:10px}
#error{min-height:24px;white-space:pre-wrap;color:#9a3131}
.status{min-height:24px;font-size:13px}
</style>
</head>
<body><main>
<h1>Замер потолка · Лицензии</h1>
<p>Панель продавца. Подтверждайте заявки <b>только после проверки реальной оплаты в lava.top</b>.</p>
<section>
<label for="key">Секретный ключ администратора</label>
<input type="password" id="key" autocomplete="off" placeholder="Ключ из Cloudflare Secrets">
<button id="fetch">Загрузить заявки</button>
<span id="error"></span>
</section>
<section>
<h2 style="font-size:18px;margin:0 0 12px">Заявки на активацию</h2>
<label for="status">Статус</label>
<select id="status"><option value="pending">Ожидают проверки</option><option value="approved">Подтверждены</option><option value="rejected">Отклонены</option></select>
<div id="items" class="status">Введите ключ администратора и загрузите заявки.</div>
</section>
<section>
<p class="note">Номер заказа и ID покупателя — не подтверждение поступления денег. Перед одобрением сверьте платёж в кабинете платформы, стоимость и вариант лицензии. Никогда не запрашивайте у клиента секретный код восстановления.</p>
</section>
</main>
<script>
'use strict';
const byId=id=>document.getElementById(id);
function label(value){const span=document.createElement('span');span.textContent=value;return span}
function displayError(message){byId('error').textContent=message||''}
async function api(path,method='GET',body=null){
 const key=byId('key').value.trim();
 if(!key)throw Error('Введите ключ администратора');
 const response=await fetch(path,{
   method,headers:{'X-Admin-Key':key,'Accept':'application/json',...(body?{'Content-Type':'application/json'}:{})},
   body:body?JSON.stringify(body):null,cache:'no-store',credentials:'omit'
 });
 const data=await response.json();
 if(!response.ok)throw Error('Ошибка '+response.status+': '+(data.error||'неизвестная'));
 return data;
}
async function refresh(){
 displayError('');
 const area=byId('items');
 area.textContent='Загружаем…';
 try{
   const data=await api('/v1/admin/payment-claims?status='+encodeURIComponent(byId('status').value));
   const claims=data.claims||[];
   area.replaceChildren();
   if(!claims.length){area.textContent='Заявок с таким статусом нет.';return}
   for(const item of claims){
     const div=document.createElement('div');
     div.className='entry';
     const title=document.createElement('b');
     title.textContent=item.plan==='monthly'?'30 дней — 299 ₽':'Навсегда — 2 990 ₽';
     div.append(title);
     const info=document.createElement('p');
     info.className='meta';
     info.textContent='Заказ: '+item.order_reference+'\nID покупателя: '+item.user_id+'\nЗаявка: '+new Date(item.created_ms).toLocaleString('ru-RU');
     info.style.whiteSpace='pre-line';
     div.append(info);
     if(item.status==='pending'){
       const approve=document.createElement('button');
       approve.textContent='Подтвердить оплату';
       approve.onclick=async()=>{
         if(!confirm('Вы действительно нашли этот платёж в кабинете lava.top и проверили сумму?'))return;
         approve.disabled=true;
         try{await api('/v1/admin/claims/approve','POST',{claim_id:item.id});await refresh()}
         catch(e){displayError(e.message);approve.disabled=false}
       };
       const reject=document.createElement('button');
       reject.textContent='Отклонить';
       reject.className='danger';
       reject.onclick=async()=>{
         if(!confirm('Отклонить заявку № '+item.order_reference+'?'))return;
         reject.disabled=true;
         try{await api('/v1/admin/claims/reject','POST',{claim_id:item.id});await refresh()}
         catch(e){displayError(e.message);reject.disabled=false}
       };
       div.append(approve,reject);
     }
     area.append(div);
   }
 }catch(e){area.textContent='';displayError(e.message)}
}
byId('fetch').addEventListener('click',refresh);
byId('status').addEventListener('change',()=>{
 if(byId('key').value.trim())refresh();
});
</script>
</body></html>`;
  return new Response(page,{
    headers:{
      'content-type':'text/html; charset=utf-8',
      'cache-control':'no-store',
      'x-frame-options':'DENY',
      'x-content-type-options':'nosniff',
      'referrer-policy':'no-referrer',
      'content-security-policy':"default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"
    }
  });
}
