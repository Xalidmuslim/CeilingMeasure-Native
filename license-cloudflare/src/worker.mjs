import {entitlement, issueLicense, randomRecoveryCode, sha256Hex, hashIp, validRecoveryCode} from './domain.mjs';

const DEVICE = /^[a-zA-Z0-9_-]{16,128}$/;
const UUID = /^[a-f0-9-]{36}$/i;
const EXT_REF = /^[a-zA-Z0-9_:.-]{6,160}$/;
const SOURCE = /^[a-zA-Z0-9_-]{2,40}$/;
const DAY = 86_400_000;
const asJson = (value, status = 200) => new Response(JSON.stringify(value), {
  status,
  headers: {'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store', 'x-content-type-options': 'nosniff'}
});
const error = (name, status) => asJson({error: name}, status);

async function readJson(request) {
  if ((request.headers.get('content-length') || '0') > 8192) throw new Error('invalid_request');
  const text = await request.text();
  if (text.length > 8192) throw new Error('invalid_request');
  let data;
  try {data = JSON.parse(text)} catch {throw new Error('invalid_request')}
  if (!data || typeof data !== 'object' || Array.isArray(data)) throw new Error('invalid_request');
  return data;
}

function bearer(request) {
  const match = /^Bearer (\S+)$/.exec(request.headers.get('authorization') || '');
  return match ? match[1] : '';
}

function equalSecrets(first, second) {
  if (typeof first !== 'string' || typeof second !== 'string') return false;
  const length = Math.max(first.length, second.length);
  let mismatch = first.length ^ second.length;
  for (let i=0;i<length;i++) mismatch |= (first.charCodeAt(i) || 0) ^ (second.charCodeAt(i) || 0);
  return mismatch === 0;
}

async function userForRecovery(db, recoveryCode) {
  if (!validRecoveryCode(recoveryCode)) return null;
  const hash=await sha256Hex(recoveryCode);
  return await db.prepare('SELECT id, trial_started_ms FROM users WHERE recovery_hash = ?').bind(hash).first();
}

async function signFor(db, env, userId, trialStartedMs, deviceId) {
  if (!env.LICENSE_PRIVATE_KEY_PKCS8_B64) throw new Error('missing_license_key');
  const query=await db.prepare('SELECT kind, paid_until_ms, revoked FROM grants WHERE user_id = ?').bind(userId).all();
  const rights=entitlement(trialStartedMs,query.results||[]);
  const signed=await issueLicense({userId,deviceId,rights,privateKeyDerB64:env.LICENSE_PRIVATE_KEY_PKCS8_B64});
  return {license: signed.token, status: rights.kind, access_until: rights.accessUntil, server_time: Date.now(), user_id: userId};
}

async function register(request, env) {
  if (!env.IP_HASH_PEPPER || env.IP_HASH_PEPPER.length < 32) throw new Error('missing_ip_pepper');
  const body=await readJson(request);
  const deviceId=String(body.device_id||'');
  if (!DEVICE.test(deviceId)) return error('invalid_device',400);
  const now=Date.now();
  const utcDay=Math.floor(now/DAY);
  const ip=request.headers.get('CF-Connecting-IP') || 'unknown';
  const ipHash=await hashIp(ip,env.IP_HASH_PEPPER);
  const counter=await env.DB.prepare(
    'INSERT INTO registration_limits (ip_hash,utc_day,attempts) VALUES (?, ?, 1) ' +
    'ON CONFLICT(ip_hash,utc_day) DO UPDATE SET attempts=attempts+1 RETURNING attempts'
  ).bind(ipHash,utcDay).first();
  if (!counter || counter.attempts>5) return error('daily_trial_registration_limit',429);

  const userId=crypto.randomUUID();
  const recovery=randomRecoveryCode();
  const hash=await sha256Hex(recovery);
  await env.DB.prepare('INSERT INTO users (id,recovery_hash,trial_started_ms,active_device_id,created_ms) VALUES (?,?,?,?,?)')
    .bind(userId,hash,now,deviceId,now).run();
  const signed=await signFor(env.DB,env,userId,now,deviceId);
  return asJson({...signed,recovery_code:recovery},201);
}

async function recoverOrRefresh(request, env, allowRecovery) {
  const body=await readJson(request);
  const deviceId=String(body.device_id||'');
  if (!DEVICE.test(deviceId)) return error('invalid_device',400);
  const recoveryCode=allowRecovery ? body.recovery_code : bearer(request);
  const user=await userForRecovery(env.DB,recoveryCode);
  if (!user) return error('invalid_recovery_code',401);
  // Rebind the account to the requesting installation: new phone restores the same trial/purchase.
  await env.DB.prepare('UPDATE users SET active_device_id=? WHERE id=?').bind(deviceId,user.id).run();
  return asJson(await signFor(env.DB,env,user.id,user.trial_started_ms,deviceId));
}

async function adminGrant(request,env) {
  if (!env.ADMIN_API_KEY || env.ADMIN_API_KEY.length<32 ||
      !equalSecrets(request.headers.get('x-admin-key'), env.ADMIN_API_KEY)) return error('forbidden',403);
  const data=await readJson(request);
  const userId=String(data.user_id||'');
  const kind=String(data.kind||'');
  const source=String(data.source||'');
  const externalRef=String(data.external_ref||'');
  const until=kind==='monthly' ? Date.parse(String(data.paid_until||'')) : null;
  if (!UUID.test(userId) || !['monthly','lifetime'].includes(kind) ||
      !SOURCE.test(source) || !EXT_REF.test(externalRef) ||
      (kind==='monthly' && (!Number.isFinite(until) || until<=Date.now())) ||
      (kind==='lifetime' && data.paid_until != null)) return error('invalid_grant',400);
  const user=await env.DB.prepare('SELECT id FROM users WHERE id=?').bind(userId).first();
  if (!user) return error('user_not_found',404);
  const id=crypto.randomUUID();
  try {
    const result=await env.DB.prepare(
      'INSERT INTO grants (id,user_id,kind,source,external_ref,paid_until_ms,created_ms) VALUES (?,?,?,?,?,?,?)'
    ).bind(id,userId,kind,source,externalRef,until,Date.now()).run();
    if (!result.success) return error('grant_failed',500);
    return asJson({grant_id:id},201);
  } catch(e) {
    if (String(e.message||'').includes('UNIQUE')) return error('duplicate_payment_reference',409);
    throw e;
  }
}

async function adminRevoke(request,env) {
  if (!env.ADMIN_API_KEY || env.ADMIN_API_KEY.length<32 ||
      !equalSecrets(request.headers.get('x-admin-key'), env.ADMIN_API_KEY)) return error('forbidden',403);
  const data=await readJson(request);
  const reference=String(data.external_ref||'');
  if(!EXT_REF.test(reference))return error('invalid_reference',400);
  const result=await env.DB.prepare('UPDATE grants SET revoked=1 WHERE external_ref=? AND revoked=0')
    .bind(reference).run();
  return asJson({revoked: (result.meta?.changes||0)>0});
}

export default {
  async fetch(request, env) {
    try {
      const url=new URL(request.url);
      const method=request.method;
      if (!env.DB) throw new Error('missing_d1_binding');
      if (method==='GET' && url.pathname==='/health') {
        await env.DB.prepare('SELECT 1 AS ok').first();
        return asJson({status:'ok'});
      }
      if(method==='POST' && url.pathname==='/v1/auth/register')return await register(request,env);
      if(method==='POST' && url.pathname==='/v1/auth/recover')return await recoverOrRefresh(request,env,true);
      if(method==='POST' && url.pathname==='/v1/license')return await recoverOrRefresh(request,env,false);
      if(method==='POST' && url.pathname==='/v1/admin/grants')return await adminGrant(request,env);
      if(method==='POST' && url.pathname==='/v1/admin/revoke')return await adminRevoke(request,env);
      return error('not_found',404);
    } catch(e) {
      if (e.message==='invalid_request')return error('invalid_request',400);
      console.error('License server failure:', String(e.message||'').slice(0,120));
      return error('internal_error',500);
    }
  }
};
