import {entitlement, issueLicense, randomRecoveryCode, sha256Hex, hashIp, validRecoveryCode} from './domain.mjs';
import {adminPage} from './admin.mjs';

const DEVICE = /^[a-zA-Z0-9_-]{16,128}$/;
const UUID = /^[a-f0-9-]{36}$/i;
const REF = /^[A-Za-z0-9][A-Za-z0-9_.:-]{5,127}$/;
const DAY = 86_400_000;

const asJson = (value, status = 200) => new Response(JSON.stringify(value), {
  status,
  headers: {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store',
    'x-content-type-options': 'nosniff'
  }
});
const error = (reason, status) => asJson({error:reason}, status);

async function readJson(request) {
  if (Number(request.headers.get('content-length') || '0') > 8192) throw Error('invalid_json');
  const raw=await request.text();
  if (raw.length>8192) throw Error('invalid_json');
  try {
    const parsed=JSON.parse(raw);
    if (parsed && typeof parsed==='object' && !Array.isArray(parsed)) return parsed;
  } catch {}
  throw Error('invalid_json');
}
function bearer(request) {
  const match=/^Bearer (\S+)$/.exec(request.headers.get('authorization') || '');
  return match ? match[1] : '';
}
function safeEqual(a,b) {
  if (typeof a!=='string' || typeof b!=='string') return false;
  let mismatch=a.length^b.length;
  for (let i=0;i<Math.max(a.length,b.length);i++) mismatch |= (a.charCodeAt(i)||0)^(b.charCodeAt(i)||0);
  return mismatch===0;
}
function authorized(request,env) {
  return typeof env.ADMIN_API_KEY==='string' && env.ADMIN_API_KEY.length>=32 &&
    safeEqual(request.headers.get('x-admin-key'),env.ADMIN_API_KEY);
}
async function customer(db,recoveryCode) {
  if (!validRecoveryCode(recoveryCode)) return null;
  return await db.prepare('SELECT id,trial_started_ms,active_device_id FROM users WHERE recovery_hash=?')
    .bind(await sha256Hex(recoveryCode)).first();
}
async function signedLicense(env,userId,trialStartedMs,deviceId) {
  if (!env.LICENSE_PRIVATE_KEY_PKCS8_B64) throw Error('missing_license_signing_key');
  const grants=await env.DB.prepare('SELECT kind,paid_until_ms,revoked FROM grants WHERE user_id=?')
    .bind(userId).all();
  const rights=entitlement(trialStartedMs,grants.results||[]);
  const signed=await issueLicense({userId,deviceId,rights,privateKeyDerB64:env.LICENSE_PRIVATE_KEY_PKCS8_B64});
  return {
    license:signed.token, status:rights.kind,
    access_until:rights.accessUntil,server_time:Date.now(),user_id:userId
  };
}
async function register(request,env) {
  if (!env.IP_HASH_PEPPER || env.IP_HASH_PEPPER.length<32) throw Error('missing_registration_pepper');
  const input=await readJson(request);
  const deviceId=String(input.device_id||'');
  if(!DEVICE.test(deviceId))return error('invalid_device',400);
  const now=Date.now();
  const utcDay=Math.floor(now/DAY);
  const ip=request.headers.get('CF-Connecting-IP')||'unknown';
  const ipHash=await hashIp(ip,env.IP_HASH_PEPPER);
  const allowed=await env.DB.prepare(
    'INSERT INTO registration_limits (ip_hash,utc_day,attempts) VALUES (?,?,1) '+
    'ON CONFLICT(ip_hash,utc_day) DO UPDATE SET attempts=attempts+1 RETURNING attempts'
  ).bind(ipHash,utcDay).first();
  if (!allowed || allowed.attempts>5) return error('trial_limit_reached',429);
  const id=crypto.randomUUID();
  const code=randomRecoveryCode();
  await env.DB.prepare(
    'INSERT INTO users (id,recovery_hash,trial_started_ms,active_device_id,created_ms) VALUES (?,?,?,?,?)'
  ).bind(id,await sha256Hex(code),now,deviceId,now).run();
  return asJson({...await signedLicense(env,id,now,deviceId),recovery_code:code},201);
}
async function renew(request,env,explicitRecovery) {
  const input=await readJson(request);
  const deviceId=String(input.device_id||'');
  if(!DEVICE.test(deviceId))return error('invalid_device',400);
  const code=explicitRecovery ? input.recovery_code : bearer(request);
  const user=await customer(env.DB,code);
  if(!user)return error('invalid_recovery_code',401);
  await env.DB.prepare('UPDATE users SET active_device_id=? WHERE id=?').bind(deviceId,user.id).run();
  return asJson(await signedLicense(env,user.id,user.trial_started_ms,deviceId));
}
function validWhatsAppNumber(v) {
  const digits=String(v||'').trim();
  return /^[1-9][0-9]{7,14}$/.test(digits) ? digits : null;
}
function safePrice(value,fallback) {
  const price=Number(value);
  return Number.isSafeInteger(price) && price>=50 && price<=1_000_000 ? price : fallback;
}
function catalogue(env) {
  return asJson({
    currency:'RUB',
    payment_method:'manual_transfer',
    requires_manual_verification:true,
    recurring:false,
    whatsapp_number:validWhatsAppNumber(env.WHATSAPP_NUMBER),
    // Calls provide a fallback for users whose Russian ISP blocks WhatsApp.
    call_number:validWhatsAppNumber(env.SUPPORT_CALL_NUMBER || env.WHATSAPP_NUMBER),
    plans:{
      monthly:{name:'30 дней',days:30,price_rub:safePrice(env.MONTHLY_PRICE_RUB,299)},
      lifetime:{name:'Навсегда',days:null,price_rub:safePrice(env.LIFETIME_PRICE_RUB,2990)}
    }
  });
}
async function viewCustomer(request,env) {
  if(!authorized(request,env))return error('forbidden',403);
  const id=new URL(request.url).searchParams.get('user_id')||'';
  if(!UUID.test(id))return error('invalid_user_id',400);
  const user=await env.DB.prepare('SELECT id,trial_started_ms FROM users WHERE id=?').bind(id).first();
  if(!user)return error('user_not_found',404);
  const rows=await env.DB.prepare(
    'SELECT kind,source,external_ref,paid_until_ms,revoked,created_ms FROM grants WHERE user_id=? ORDER BY created_ms DESC LIMIT 50'
  ).bind(id).all();
  const rights=entitlement(user.trial_started_ms,rows.results||[]);
  return asJson({
    user_id:user.id,trial_started_ms:user.trial_started_ms,
    status:rights.kind,access_until:rights.accessUntil,grants:rows.results||[]
  });
}
async function manualGrant(request,env) {
  if(!authorized(request,env))return error('forbidden',403);
  const body=await readJson(request);
  const id=String(body.user_id||'');
  const kind=String(body.kind||'');
  const ref=String(body.external_ref||'').trim();
  if(!UUID.test(id)||!['monthly','lifetime'].includes(kind)||!REF.test(ref))return error('invalid_grant',400);
  if(kind==='monthly' && body.period_days!==30)return error('requires_30_day_period',400);
  if(kind==='lifetime' && body.period_days!=null)return error('lifetime_has_no_period',400);
  const user=await env.DB.prepare('SELECT id,trial_started_ms FROM users WHERE id=?').bind(id).first();
  if(!user)return error('user_not_found',404);
  // Idempotency check: never extend a paid month twice with the same transfer reference.
  const existing=await env.DB.prepare('SELECT id FROM grants WHERE external_ref=?').bind(ref).first();
  if(existing)return error('already_activated',409);
  const now=Date.now();
  let until=null;
  if(kind==='monthly'){
    const r=await env.DB.prepare(
      "SELECT MAX(paid_until_ms) AS until_ms FROM grants WHERE user_id=? AND kind='monthly' AND revoked=0"
    ).bind(id).first();
    // Preserve remaining trial days if the buyer pays before the free period ends.
    until=Math.max(now,Number(user.trial_started_ms)+7*DAY,Number(r?.until_ms||0))+30*DAY;
  }
  const grantId=crypto.randomUUID();
  try{
    await env.DB.prepare(
      'INSERT INTO grants (id,user_id,kind,source,external_ref,paid_until_ms,created_ms) VALUES (?,?,?,?,?,?,?)'
    ).bind(grantId,id,kind,'manual_transfer',ref,until,now).run();
    return asJson({grant_id:grantId,user_id:id,kind,access_until:until},201);
  }catch(e){
    if(/UNIQUE/i.test(String(e.message||'')))return error('already_activated',409);
    throw e;
  }
}
async function revoke(request,env) {
  if(!authorized(request,env))return error('forbidden',403);
  const input=await readJson(request);
  const ref=String(input.external_ref||'');
  if(!REF.test(ref))return error('invalid_reference',400);
  const result=await env.DB.prepare('UPDATE grants SET revoked=1 WHERE external_ref=? AND revoked=0')
    .bind(ref).run();
  return asJson({revoked:(result.meta?.changes||0)>0});
}

export default {
  async fetch(request,env) {
    try {
      const path=new URL(request.url).pathname,method=request.method;
      if(!env.DB)throw Error('missing_D1_database');
      if(method==='GET' && path==='/health') {
        await env.DB.prepare('SELECT 1 AS ok').first();
        return asJson({status:'ok'});
      }
      if(method==='GET' && path==='/v1/catalog')return catalogue(env);
      if(method==='GET' && path==='/admin')return adminPage();
      if(method==='POST' && path==='/v1/auth/register')return await register(request,env);
      if(method==='POST' && path==='/v1/auth/recover')return await renew(request,env,true);
      if(method==='POST' && path==='/v1/license')return await renew(request,env,false);
      if(method==='GET' && path==='/v1/admin/customer')return await viewCustomer(request,env);
      if(method==='POST' && path==='/v1/admin/grants')return await manualGrant(request,env);
      if(method==='POST' && path==='/v1/admin/revoke')return await revoke(request,env);
      return error('not_found',404);
    } catch(e){
      if(e.message==='invalid_json')return error('invalid_json',400);
      console.error('License server:',String(e.message||'').slice(0,100));
      return error('internal_error',500);
    }
  }
};
