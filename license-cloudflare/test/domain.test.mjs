import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
import {entitlement, issueLicense, randomRecoveryCode, sha256Hex, validRecoveryCode} from '../src/domain.mjs';

if (!globalThis.crypto) globalThis.crypto = webcrypto;
const DAY = 86_400_000;
const now = Date.parse('2026-10-11T12:00:00Z');

test('7 days for account, no local reset', () => {
  assert.equal(entitlement(now - 6*DAY, [], now).kind, 'trial');
  assert.equal(entitlement(now - 7*DAY, [], now).kind, 'expired');
});
test('paid monthly and perpetual precedence, revoked grants', () => {
  const expired = now - 9*DAY;
  const monthly = {kind:'monthly',paid_until_ms:now + DAY,revoked:0};
  assert.equal(entitlement(expired,[monthly],now).kind, 'monthly');
  assert.equal(entitlement(expired,[monthly,{kind:'lifetime',revoked:0}],now).kind, 'lifetime');
  assert.equal(entitlement(expired,[{...monthly,revoked:1}],now).kind, 'expired');
});
test('recovery code is uniformly random, unique and SHA hashed', async () => {
  const a=randomRecoveryCode(), b=randomRecoveryCode();
  assert.equal(validRecoveryCode(a),true);
  assert.notEqual(a,b);
  assert.equal((await sha256Hex(a)).length,64);
  assert.equal(validRecoveryCode('nope'),false);
});
test('Worker RSA signature interoperates with Android SHA256withRSA', async () => {
  const pair = await crypto.subtle.generateKey(
    {name:'RSASSA-PKCS1-v1_5',modulusLength:2048,publicExponent:new Uint8Array([1,0,1]),hash:'SHA-256'},
    true,['sign','verify']);
  const privateDer = await crypto.subtle.exportKey('pkcs8',pair.privateKey);
  const privB64=Buffer.from(privateDer).toString('base64');
  const deviceId='device_1234567890123456';
  const trial=await issueLicense({userId:'test',deviceId,rights:{kind:'trial',accessUntil:now+DAY},privateKeyDerB64:privB64,now});
  const [body,signature]=trial.token.split('.');
  const verified=await crypto.subtle.verify('RSASSA-PKCS1-v1_5',pair.publicKey,
    Buffer.from(signature,'base64url'),Buffer.from(body,'utf8'));
  assert.equal(verified,true);
  assert.equal(trial.claims.lease_until,now+DAY);
  const paid=await issueLicense({userId:'test',deviceId,rights:{kind:'lifetime',accessUntil:null},privateKeyDerB64:privB64,now});
  assert.equal(paid.claims.lease_until,now+14*DAY);
});
