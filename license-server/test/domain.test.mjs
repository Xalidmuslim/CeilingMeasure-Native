import test from 'node:test';
import assert from 'node:assert/strict';
import {generateKeyPairSync} from 'node:crypto';
import {entitlement,issueLicense,verifyLicense,publicKeyDerBase64} from '../src/domain.mjs';

const { privateKey, publicKey } = generateKeyPairSync('rsa', {modulusLength: 2048});
const priv = privateKey.export({type: 'pkcs8',format:'pem'});
const pub = publicKey.export({type: 'spki',format:'pem'});
const now = new Date('2026-10-11T12:00:00Z'), t = now.getTime(), d = 86400000;
const deviceId = 'device_1234567890123456';

test('trial lasts seven days and does not reset', () => {
  assert.equal(entitlement(new Date(t-6*d),[],now).kind,'trial');
  assert.equal(entitlement(new Date(t-7*d),[],now).kind,'expired');
});
test('paid and perpetual entitlements', () => {
  const trial = new Date(t-10*d);
  const paid = {kind:'monthly',paid_until:new Date(t+d),revoked:false};
  assert.equal(entitlement(trial,[paid],now).kind,'monthly');
  assert.equal(entitlement(trial,[paid,{kind:'lifetime',revoked:false}],now).kind,'lifetime');
  assert.equal(entitlement(trial,[{...paid,revoked:true}],now).kind,'expired');
});
test('signature, device binding, offline expiry, tampering', () => {
  const issued = issueLicense({userId:'user',deviceId,entitlement:{kind:'monthly',accessUntil:t+30*d},privateKeyPem:priv,now});
  assert.equal(verifyLicense(issued.token,pub,{deviceId,now:t}),true);
  assert.equal(verifyLicense(issued.token,pub,{deviceId:'different_device_identifier',now:t}),false);
  assert.equal(verifyLicense(issued.token,pub,{deviceId,now:t+15*d}),false);
  assert.equal(verifyLicense(issued.token.slice(0,-3)+'aaa',pub,{deviceId,now:t}),false);
  assert.ok(publicKeyDerBase64(priv).length > 300);
});
test('trial signed license cannot outlive its trial',()=>{
  const l = issueLicense({userId:'user',deviceId,entitlement:{kind:'trial',accessUntil:t+d},privateKeyPem:priv,now});
  assert.equal(verifyLicense(l.token,pub,{deviceId,now:t+2*d}),false);
});
