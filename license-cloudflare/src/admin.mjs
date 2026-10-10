/**
 * Mobile-only seller fallback: buyers use native app; owner can grant
 * licences in the native Owner Mode or through this browser page.
 *
 * No password, bank-card data, transfer details or tokens are ever persisted.
 */
export function adminPage() {
  const html=`<!doctype html>
<html lang="ru"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex,nofollow">
<title>Владелец · Замер потолка</title>
<style>
*{box-sizing:border-box}
body{margin:0;padding:16px 10px 42px;background:#f6f8f9;color:#253c4c;font:15px/1.45 system-ui,-apple-system,Roboto,sans-serif}
main{max-width:570px;margin:0 auto}
h1{font-size:23px;line-height:1.2;margin:4px 0}
h2{font-size:17px}
section{border-radius:15px;border:1px solid #dce4e9;background:white;margin:15px 0;padding:16px}
label{display:block;color:#526976;font-size:13px;margin:12px 0 5px}
input,select{width:100%;font:16px system-ui;border:1px solid #bacbd3;padding:12px;border-radius:9px}
button{border:0;border-radius:9px;background:#26566f;color:white;padding:12px 16px;font:14px system-ui;margin:10px 0 0;cursor:pointer}
button:disabled{opacity:.5;cursor:default}
p,small{color:#617580}
.warning{background:#fffaf2;padding:10px;border-left:3px solid #b78c3a}
.status{font-size:14px;white-space:pre-wrap;overflow-wrap:anywhere}
</style></head><body><main>
<h1>Замер потолка · Владелец</h1>
<p>Управление лицензиями с телефона после личной проверки банковского перевода.</p>
<section>
<label for="key">Секретный ключ владельца (не сохраняется)</label>
<input id="key" type="password" autocomplete="off" placeholder="ADMIN_API_KEY из Cloudflare">
<label for="customer">ID покупателя из WhatsApp</label>
<input id="customer" type="text" autocomplete="off" spellcheck="false" placeholder="UUID">
<button id="lookup">Проверить лицензию</button>
<p class="status" id="existing"></p>
</section>
<section>
<h2>Открыть доступ</h2>
<label for="plan">Вариант лицензии</label>
<select id="plan">
<option value="monthly">30 дней — без автопродления</option>
<option value="lifetime">Навсегда</option>
</select>
<label for="ref">Уникальный номер полученного перевода</label>
<input id="ref" type="text" autocomplete="off" spellcheck="false" placeholder="Например transfer_20261011_01">
<button id="grant">Активировать доступ</button>
<p class="status" id="result"></p>
<p class="warning">Сначала проверьте поступление денег в приложении банка. Скриншот покупателя не является доказательством. Не просите у него секретный код восстановления.</p>
</section>
</main><script>
'use strict';
const el=id=>document.getElementById(id);
function validation(){
 const id=el('customer').value.trim(),key=el('key').value;
 if(!/^[a-f0-9-]{36}$/i.test(id))throw Error('Неправильный ID покупателя');
 if(key.length<32)throw Error('Введите секретный ключ владельца');
 return {id,key};
}
async function api(path,method,body){
 const data=validation(),headers={'X-Admin-Key':data.key,'Accept':'application/json'};
 if(body)headers['Content-Type']='application/json';
 const resp=await fetch(path,{method,headers,body:body?JSON.stringify(body):null,credentials:'omit',cache:'no-store'});
 const json=await resp.json();
 if(!resp.ok)throw Error('Ошибка '+resp.status+': '+(json.error||'неизвестная'));
 return json;
}
function humanStatus(value){
 const terms={trial:'Пробный период',monthly:'Оплачен доступ на 30 дней',lifetime:'Бессрочно',expired:'Лицензия истекла'};
 let s=terms[value.status]||'Неизвестный статус';
 if(value.access_until&&value.status!=='lifetime')s+=' · до '+new Date(value.access_until).toLocaleString('ru-RU');
 return s;
}
el('lookup').onclick=async()=>{
 el('existing').textContent='Проверяем…';
 el('lookup').disabled=true;
 try{
  const id=validation().id;
  const out=await api('/v1/admin/customer?user_id='+encodeURIComponent(id),'GET');
  el('existing').textContent=humanStatus(out);
 }catch(e){el('existing').textContent=e.message}
 finally{el('lookup').disabled=false}
};
el('grant').onclick=async()=>{
 const output=el('result');output.textContent='';
 let id,key,ref;
 try{
  ({id,key}=validation());
  ref=el('ref').value.trim();
  if(!/^[A-Za-z0-9][A-Za-z0-9_.:-]{5,127}$/.test(ref))throw Error('Нужен уникальный номер перевода (6–128 символов)');
 }catch(e){output.textContent=e.message;return}
 const plan=el('plan').value;
 if(!confirm('Вы лично проверили поступление денег на банковский счёт? Открыть клиенту '+(plan==='monthly'?'30 дней':'бессрочный доступ')+'?'))return;
 el('grant').disabled=true;
 try{
  const payload={user_id:id,kind:plan,external_ref:'bank_'+ref};
  if(plan==='monthly')payload.period_days=30;
  const result=await api('/v1/admin/grants','POST',payload);
  output.textContent='Готово. Лицензия выдана. Попросите покупателя нажать «Обновить лицензию».';
  el('ref').value='';
  el('lookup').click();
 }catch(e){output.textContent=e.message}
 finally{el('grant').disabled=false}
};
</script></body></html>`;
  return new Response(html,{
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
