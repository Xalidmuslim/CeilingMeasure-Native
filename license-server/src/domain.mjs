import { createPrivateKey, createPublicKey, sign, verify } from 'node:crypto';

const DAY = 86400000;
const MAX_OFFLINE_DAYS = 14;

export function entitlement(trialStarted, grants, now = new Date()) {
  const current = now.getTime();
  const active = grants.filter(g => !g.revoked);
  if (active.some(g => g.kind === 'lifetime')) return { kind: 'lifetime', accessUntil: null };
  const latest = active.filter(g => g.kind === 'monthly').reduce((max, g) => Math.max(max, new Date(g.paid_until).getTime()), 0);
  if (latest > current) return { kind: 'monthly', accessUntil: latest };
  const trialEnd = new Date(trialStarted).getTime() + 7 * DAY;
  return trialEnd > current
    ? { kind: 'trial', accessUntil: trialEnd }
    : { kind: 'expired', accessUntil: trialEnd };
}

export function issueLicense({ userId, deviceId, entitlement: rights, privateKeyPem, now = new Date() }) {
  if (!/^[A-Za-z0-9_-]{16,128}$/.test(deviceId)) throw new Error('Invalid device ID');
  const time = now.getTime();
  const payload = {
    schema: 1,
    issuer: 'ceilingmeasure-license',
    audience: 'ru.xalidmuslim.ceilingmeasure',
    user_id: userId,
    device_id: deviceId,
    kind: rights.kind,
    issued_at: time,
    access_until: rights.accessUntil,
    lease_until: rights.kind === 'expired' ? time :
      Math.min(rights.accessUntil ?? time + MAX_OFFLINE_DAYS * DAY, time + MAX_OFFLINE_DAYS * DAY)
  };
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url');
  const signature = sign('RSA-SHA256', Buffer.from(body, 'utf8'), createPrivateKey(privateKeyPem)).toString('base64url');
  return { token: body + '.' + signature, payload };
}

export function verifyLicense(token, publicKeyPem, { deviceId, now = Date.now() } = {}) {
  try {
    const parts = token.split('.');
    if (parts.length !== 2) return false;
    if (!verify('RSA-SHA256', Buffer.from(parts[0], 'utf8'), createPublicKey(publicKeyPem), Buffer.from(parts[1], 'base64url'))) return false;
    const p = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
    return p.schema === 1 && p.issuer === 'ceilingmeasure-license' &&
      p.audience === 'ru.xalidmuslim.ceilingmeasure' &&
      (!deviceId || p.device_id === deviceId) && p.kind !== 'expired' &&
      Number.isSafeInteger(p.issued_at) && Number.isSafeInteger(p.lease_until) &&
      p.issued_at <= now + 300000 && p.lease_until > now &&
      (p.access_until === null || p.access_until > now);
  } catch { return false; }
}

export function publicKeyDerBase64(privateKeyPem) {
  return createPublicKey(createPrivateKey(privateKeyPem)).export({type: 'spki', format: 'der'}).toString('base64');
}
