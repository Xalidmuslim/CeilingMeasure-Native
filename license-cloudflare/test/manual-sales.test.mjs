import test from 'node:test';
import assert from 'node:assert/strict';
import worker from '../src/worker.mjs';

const origin='https://ceilingmeasure-license.example.workers.dev';
const buyer='11111111-2222-4333-8444-555555555555';
const key='a'.repeat(64);
const day=86400000;

class TestDb {
  constructor() {
    this.users=new Map([[buyer,{id:buyer,trial_started_ms:Date.now()-9*day}]]);
    this.grants=new Map();
  }
  prepare(sql){
    const db=this;
    let params=[];
    return {
      bind(...args){params=args;return this;},
      async first(){
        if(sql.startsWith('SELECT id FROM users'))return db.users.get(params[0])||null;
        if(sql.startsWith('SELECT id FROM grants WHERE external_ref'))return db.grants.get(params[0])||null;
        if(sql.includes('MAX(paid_until_ms)')) {
          const rows=[...db.grants.values()].filter(g=>g.user_id===params[0] && g.kind==='monthly' && !g.revoked);
          const mx=rows.length?Math.max(...rows.map(g=>g.paid_until_ms)):null;
          return {until_ms:mx};
        }
        if(sql.startsWith('SELECT id,trial_started_ms FROM users'))return db.users.get(params[0])||null;
        if(sql.startsWith('SELECT 1 AS ok'))return {ok:1};
        throw Error('Unhandled first() query: '+sql);
      },
      async all(){
        if(sql.startsWith('SELECT kind,source,external_ref')){
          return {results:[...db.grants.values()].filter(g=>g.user_id===params[0])};
        }
        throw Error('Unhandled all() query '+sql);
      },
      async run(){
        if(sql.startsWith('INSERT INTO grants')){
          const [id,userId,kind,source,ref,until,created]=params;
          if(db.grants.has(ref))throw Error('UNIQUE constraint');
          db.grants.set(ref,{id,user_id:userId,kind,source,external_ref:ref,paid_until_ms:until,created_ms:created,revoked:0});
          return {success:true};
        }
        if(sql.startsWith('UPDATE grants SET revoked')){
          const grant=db.grants.get(params[0]);
          if(grant && !grant.revoked){grant.revoked=1;return {meta:{changes:1}};}
          return {meta:{changes:0}};
        }
        throw Error('Unhandled run() query '+sql);
      }
    };
  }
}
const post=(path,body,admin)=>{
  const headers={'Content-Type':'application/json'};
  if(admin)headers['X-Admin-Key']=admin;
  return new Request(origin+path,{method:'POST',headers,body:JSON.stringify(body)});
};

test('manual RUB catalogue shows WhatsApp and fallback number but not bank details',async()=>{
  const env={DB:new TestDb(),WHATSAPP_NUMBER:'79001234567',MONTHLY_PRICE_RUB:'299',LIFETIME_PRICE_RUB:'2990'};
  const res=await worker.fetch(new Request(origin+'/v1/catalog'),env);
  assert.equal(res.status,200);
  const data=await res.json();
  assert.equal(data.payment_method,'manual_transfer');
  assert.equal(data.recurring,false);
  assert.equal(data.whatsapp_number,'79001234567');
  assert.equal(data.call_number,'79001234567');
  assert.equal(data.plans.monthly.days,30);
  assert.equal(data.plans.lifetime.price_rub,2990);
  assert.equal(data.plans.monthly.checkout_url,undefined);
});
test('missing or malicious phone never produces an untrusted wa.me address',async()=>{
  const env={DB:new TestDb(),WHATSAPP_NUMBER:'https://evil.example/123'};
  const body=await (await worker.fetch(new Request(origin+'/v1/catalog'),env)).json();
  assert.equal(body.whatsapp_number,null);
  assert.equal(body.call_number,null);
});
test('any buyer cannot issue a paid license',async()=>{
  const env={DB:new TestDb(),ADMIN_API_KEY:key};
  const body={user_id:buyer,kind:'monthly',external_ref:'bank_test001',period_days:30};
  assert.equal((await worker.fetch(post('/v1/admin/grants',body,''),env)).status,403);
  assert.equal((await worker.fetch(post('/v1/admin/grants',body,'wrongkey'),env)).status,403);
});
test('owner confirms bank transfer, extends another 30 days, prevents duplicate receipt',async()=>{
  const env={DB:new TestDb(),ADMIN_API_KEY:key};
  const first={user_id:buyer,kind:'monthly',external_ref:'bank_test001',period_days:30};
  const a=await worker.fetch(post('/v1/admin/grants',first,key),env);
  assert.equal(a.status,201);
  const firstBody=await a.json();
  assert.equal(firstBody.kind,'monthly');
  assert.ok(firstBody.access_until>Date.now()+29*day);
  assert.equal((await worker.fetch(post('/v1/admin/grants',first,key),env)).status,409);
  const next={...first,external_ref:'bank_test002'};
  const b=await worker.fetch(post('/v1/admin/grants',next,key),env);
  assert.equal(b.status,201);
  const secondBody=await b.json();
  assert.equal(secondBody.access_until,firstBody.access_until+30*day);
  const life={user_id:buyer,kind:'lifetime',external_ref:'bank_test003'};
  const c=await worker.fetch(post('/v1/admin/grants',life,key),env);
  assert.equal(c.status,201);
  assert.equal((await c.json()).access_until,null);
});
test('owner can check license state, other people cannot',async()=>{
  const env={DB:new TestDb(),ADMIN_API_KEY:key};
  const u=origin+'/v1/admin/customer?user_id='+buyer;
  assert.equal((await worker.fetch(new Request(u),env)).status,403);
  const req=new Request(u,{headers:{'X-Admin-Key':key}});
  const verified=await worker.fetch(req,env);
  assert.equal(verified.status,200);
  assert.equal((await verified.json()).status,'expired');
});
