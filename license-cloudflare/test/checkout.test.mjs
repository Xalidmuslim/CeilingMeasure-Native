import test from 'node:test';
import assert from 'node:assert/strict';
import worker from '../src/worker.mjs';

const url = 'https://ceilingmeasure-license.example.workers.dev';

test('catalogue only exposes genuine external checkout links', async () => {
  const env = {
    DB: {},
    MONTHLY_PRICE_RUB: '299',
    LIFETIME_PRICE_RUB: '2990',
    PAYMENT_MONTH_URL: 'https://lava.top/ru/products/sample?currency=RUB',
    PAYMENT_LIFETIME_URL: 'https://evil.example/products/fraud'
  };
  const res = await worker.fetch(new Request(url+'/v1/catalog'),env);
  assert.equal(res.status,200);
  const body=await res.json();
  assert.equal(body.currency,'RUB');
  assert.equal(body.manual_activation,true);
  assert.equal(body.plans.monthly.price_rub,299);
  assert.ok(body.plans.monthly.checkout_url.startsWith('https://lava.top/'));
  assert.equal(body.plans.lifetime.checkout_url,null);
});

test('buyer cannot approve a payment from client-side claim alone', async () => {
  const env={DB:{}};
  const res=await worker.fetch(new Request(url+'/v1/admin/claims/approve',{
    method:'POST',headers:{'Content-Type':'application/json'},
    body:JSON.stringify({claim_id:'e39fed5a-8f61-4bd1-9440-fc0312d9eace'})
  }),env);
  assert.equal(res.status,403);
});

test('buyer proof request requires a registered account recovery secret',async () => {
  const env={DB:{}};
  const res=await worker.fetch(new Request(url+'/v1/payment-claims',{
    method:'POST',headers:{'Content-Type':'application/json'},
    body:JSON.stringify({plan:'monthly',order_reference:'receipt_000001'})
  }),env);
  assert.equal(res.status,401);
});

test('seller dashboard has restrictive security headers',async ()=>{
  const res=await worker.fetch(new Request(url+'/admin'),{DB:{}});
  assert.equal(res.status,200);
  assert.equal(res.headers.get('x-frame-options'),'DENY');
  assert.match(await res.text(),/Подтверждайте заявки/);
});
