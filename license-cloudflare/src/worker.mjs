import {entitlement, issueLicense, randomRecoveryCode, sha256Hex, hashIp, validRecoveryCode} from './domain.mjs';
import {adminPage} from './admin.mjs';

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
  if (!sellerAuthorized(request,env)) return error('forbidden',403);
  const data=await readJson(request);
  const userId=String(data.user_id||'');
  const kind=String(data.kind||'');
  const source=String(data.source||'');
  const externalRef=String(data.external_ref||'');
  const fixed30 = kind==='monthly' && data.period_days===30 && data.paid_until == null;
  const explicitEnd = kind==='monthly' && !fixed30 ? Date.parse(String(data.paid_until||'')) : null;
  if (!UUID.test(userId) || !['monthly','lifetime'].includes(kind) ||
      !SOURCE.test(source) || !EXT_REF.test(externalRef) ||
      (kind==='monthly' && !fixed30 && (!Number.isFinite(explicitEnd) || explicitEnd<=Date.now())) ||
      (kind==='monthly' && data.period_days!=null && !fixed30) ||
      (kind==='lifetime' && (data.paid_until!=null || data.period_days!=null)))
    return error('invalid_grant',400);
  const user=await env.DB.prepare('SELECT id FROM users WHERE id=?').bind(userId).first();
  if (!user) return error('user_not_registered',404);
  let until = kind==='monthly' ? explicitEnd : null;
  if (fixed30) {
    const existing=await env.DB.prepare(
      "SELECT MAX(paid_until_ms) AS valid_until FROM grants WHERE user_id=? AND kind='monthly' AND revoked=0"
    ).bind(userId).first();
    until=Math.max(Date.now(), Number(existing?.valid_until||0))+30*DAY;
  }
  const grantId=crypto.randomUUID();
  try {
    const result=await env.DB.prepare(
      'INSERT INTO grants (id,user_id,kind,source,external_ref,paid_until_ms,created_ms) VALUES (?,?,?,?,?,?,?)'
    ).bind(grantId,userId,kind,source,externalRef,until,Date.now()).run();
    if (!result.success) return error('grant_failed',500);
    return asJson({grant_id:grantId,kind,user_id:userId,access_until:until},201);
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

const PAYMENT_ORDER = /^[A-Za-z0-9][A-Za-z0-9:_./-]{5,127}$/;

function sellerAuthorized(request, env) {
  return typeof env.ADMIN_API_KEY === 'string'
    && env.ADMIN_API_KEY.length >= 32
    && equalSecrets(request.headers.get('x-admin-key'), env.ADMIN_API_KEY);
}
function trustedCheckoutLink(value) {
  try {
    if (!value) return null;
    const url = new URL(value);
    const host = url.hostname.toLowerCase();
    if (url.protocol !== 'https:') return null;
    if (!(host === 'lava.top' || host.endsWith('.lava.top') ||
          host === 'boosty.to' || host.endsWith('.boosty.to'))) return null;
    if (url.username || url.password) return null;
    return url.toString();
  } catch { return null; }
}
function priceRub(value, fallback) {
  const n = Number(value);
  return Number.isSafeInteger(n) && n >= 50 && n <= 1000000 ? n : fallback;
}
function catalogue(env) {
  const monthlyUrl = trustedCheckoutLink(env.PAYMENT_MONTH_URL);
  const lifetimeUrl = trustedCheckoutLink(env.PAYMENT_LIFETIME_URL);
  return asJson({
    currency: 'RUB',
    provider: 'external_checkout',
    manual_activation: true,
    needs_seller_confirmation: true,
    plans: {
      monthly: {name: '30 дней',days: 30,price_rub: priceRub(env.MONTHLY_PRICE_RUB,299), checkout_url:monthlyUrl},
      lifetime: {name: 'Навсегда',days: null,price_rub: priceRub(env.LIFETIME_PRICE_RUB,2990), checkout_url:lifetimeUrl}
    }
  });
}
async function createPaymentClaim(request,env) {
  const recoveryCode = bearer(request);
  const user = await userForRecovery(env.DB,recoveryCode);
  if (!user) return error('invalid_recovery_code',401);
  const input = await readJson(request);
  const plan = String(input.plan || '');
  const ref = String(input.order_reference || '').trim();
  if (!['monthly','lifetime'].includes(plan) || !PAYMENT_ORDER.test(ref))
    return error('invalid_payment_claim',400);
  const existing = await env.DB.prepare('SELECT id, user_id, plan, status FROM payment_claims WHERE order_reference=?').bind(ref).first();
  if (existing) {
    if (existing.user_id === user.id && existing.plan === plan)
      return asJson({claim_id: existing.id, status: existing.status});
    return error('order_reference_already_used',409);
  }
  const pending = await env.DB.prepare("SELECT COUNT(*) AS n FROM payment_claims WHERE user_id=? AND status='pending'").bind(user.id).first();
  if (Number(pending?.n || 0)>=5) return error('too_many_pending_claims',429);
  const id=crypto.randomUUID();
  try {
    await env.DB.prepare('INSERT INTO payment_claims (id,user_id,plan,order_reference,status,created_ms) VALUES (?,?,?,?,?,?)')
      .bind(id,user.id,plan,ref,'pending',Date.now()).run();
  } catch (e) {
    if (String(e.message||'').includes('UNIQUE')) return error('order_reference_already_used',409);
    throw e;
  }
  return asJson({claim_id:id,status:'pending',message:'awaiting_manual_review'},201);
}
async function listPaymentClaims(request,env) {
  if (!sellerAuthorized(request,env)) return error('forbidden',403);
  const status=new URL(request.url).searchParams.get('status') || 'pending';
  if (!['pending','approved','rejected'].includes(status)) return error('invalid_status',400);
  const rows = await env.DB.prepare(
    'SELECT id,user_id,plan,order_reference,status,created_ms,reviewed_ms FROM payment_claims WHERE status=? ORDER BY created_ms DESC LIMIT 100'
  ).bind(status).all();
  return asJson({status,claims:rows.results || []});
}
async function reviewPaymentClaim(request,env,decision) {
  if (!sellerAuthorized(request,env)) return error('forbidden',403);
  const body=await readJson(request);
  const id=String(body.claim_id || '');
  if (!UUID.test(id)) return error('invalid_claim',400);
  const claim=await env.DB.prepare('SELECT * FROM payment_claims WHERE id=?').bind(id).first();
  if (!claim) return error('claim_not_found',404);
  if (claim.status!=='pending') return error('claim_already_reviewed',409);
  const now=Date.now();
  if (decision==='reject') {
    const update=await env.DB.prepare("UPDATE payment_claims SET status='rejected',reviewed_ms=? WHERE id=? AND status='pending'")
      .bind(now,id).run();
    return asJson({status: (update.meta?.changes||0)>0 ? 'rejected':'unchanged'});
  }
  // Only the seller may approve, and only AFTER separately verifying the
  // matching payment and amount in the official payment-provider account.
  const externalRef = 'manual_lava:' + claim.order_reference;
  let paidUntil=null;
  if (claim.plan==='monthly') {
    const current=await env.DB.prepare(
      "SELECT MAX(paid_until_ms) AS valid_until FROM grants WHERE user_id=? AND kind='monthly' AND revoked=0"
    ).bind(claim.user_id).first();
    paidUntil=Math.max(now,Number(current?.valid_until||0))+30*DAY;
  }
  // Cloudflare D1 executes batch statements as a single transaction.
  try {
    const changes=await env.DB.batch([
      env.DB.prepare("UPDATE payment_claims SET status='approved',reviewed_ms=? WHERE id=? AND status='pending'").bind(now,id),
      env.DB.prepare('INSERT INTO grants (id,user_id,kind,source,external_ref,paid_until_ms,created_ms) VALUES (?,?,?,?,?,?,?)')
        .bind(crypto.randomUUID(),claim.user_id,claim.plan,'manual_lava',externalRef,paidUntil,now)
    ]);
    if (!changes[0].meta?.changes) return error('claim_already_reviewed',409);
    return asJson({status:'approved',user_id:claim.user_id,kind:claim.plan,access_until:paidUntil});
  } catch(e) {
    if (String(e.message||'').includes('UNIQUE')) return error('duplicate_payment_reference',409);
    throw e;
  }
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
      if(method==='GET' && url.pathname==='/admin')return adminPage();
      if(method==='GET' && url.pathname==='/v1/catalog')return catalogue(env);
      if(method==='POST' && url.pathname==='/v1/payment-claims')return await createPaymentClaim(request,env);
      if(method==='GET' && url.pathname==='/v1/admin/payment-claims')return await listPaymentClaims(request,env);
      if(method==='POST' && url.pathname==='/v1/admin/claims/approve')return await reviewPaymentClaim(request,env,'approve');
      if(method==='POST' && url.pathname==='/v1/admin/claims/reject')return await reviewPaymentClaim(request,env,'reject');
      return error('not_found',404);
    } catch(e) {
      if (e.message==='invalid_request')return error('invalid_request',400);
      console.error('License server failure:', String(e.message||'').slice(0,120));
      return error('internal_error',500);
    }
  }
};
