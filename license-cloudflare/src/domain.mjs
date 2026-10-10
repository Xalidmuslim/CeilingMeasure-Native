/** Shared between Workers runtime and node:test, no npm dependencies. */
const DAY = 86_400_000;
const encoder = new TextEncoder();

export function entitlement(trialStartedMs, grants, now = Date.now()) {
  const live = grants.filter(g => !g.revoked);
  if (live.some(g => g.kind === 'lifetime')) return {kind: 'lifetime', accessUntil: null};
  let until = 0;
  for (const g of live) if (g.kind === 'monthly' && Number(g.paid_until_ms) > until) until = Number(g.paid_until_ms);
  if (until > now) return {kind: 'monthly', accessUntil: until};
  const trialUntil = Number(trialStartedMs) + 7 * DAY;
  return trialUntil > now
    ? {kind: 'trial', accessUntil: trialUntil}
    : {kind: 'expired', accessUntil: trialUntil};
}

export function base64url(bytes) {
  let binary = '';
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '');
}

export function randomRecoveryCode() {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return 'cm1_' + base64url(bytes);
}

export function validRecoveryCode(code) {
  return typeof code === 'string' && /^cm1_[a-zA-Z0-9_-]{43}$/.test(code);
}

export async function sha256Hex(value) {
  const bytes = new Uint8Array(await crypto.subtle.digest('SHA-256', encoder.encode(value)));
  return [...bytes].map(n => n.toString(16).padStart(2, '0')).join('');
}

export async function hashIp(ip, pepper) {
  const key = await crypto.subtle.importKey('raw', encoder.encode(pepper), {name: 'HMAC', hash: 'SHA-256'}, false, ['sign']);
  const hash = new Uint8Array(await crypto.subtle.sign('HMAC', key, encoder.encode(ip)));
  return [...hash].map(n => n.toString(16).padStart(2, '0')).join('');
}

let cachedPrivateKey = null;
let cachedPrivateInput = null;
async function signingKey(privateKeyDerB64) {
  if (cachedPrivateKey && privateKeyDerB64 === cachedPrivateInput) return cachedPrivateKey;
  const raw = Uint8Array.from(atob(privateKeyDerB64), c => c.charCodeAt(0));
  cachedPrivateKey = await crypto.subtle.importKey('pkcs8', raw, {name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256'}, false, ['sign']);
  cachedPrivateInput = privateKeyDerB64;
  return cachedPrivateKey;
}

export async function issueLicense({userId, deviceId, rights, privateKeyDerB64, now = Date.now()}) {
  if (!/^[a-zA-Z0-9_-]{16,128}$/.test(deviceId)) throw new Error('invalid_device');
  const leaseLimit = now + 14 * DAY;
  const claims = {
    schema: 1, issuer: 'ceilingmeasure-license', audience: 'ru.xalidmuslim.ceilingmeasure',
    user_id: userId, device_id: deviceId, kind: rights.kind,
    issued_at: now, access_until: rights.accessUntil,
    lease_until: rights.kind === 'expired' ? now : Math.min(leaseLimit, rights.accessUntil ?? leaseLimit)
  };
  const body = base64url(encoder.encode(JSON.stringify(claims)));
  const signature = await crypto.subtle.sign('RSASSA-PKCS1-v1_5',
    await signingKey(privateKeyDerB64), encoder.encode(body));
  return {token: body + '.' + base64url(new Uint8Array(signature)), claims};
}
