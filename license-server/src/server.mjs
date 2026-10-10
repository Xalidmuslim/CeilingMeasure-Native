import {createHash, createHmac, randomBytes, randomInt, randomUUID, timingSafeEqual} from 'node:crypto';
import http from 'node:http';
import {Pool} from 'pg';
import nodemailer from 'nodemailer';
import {entitlement, issueLicense} from './domain.mjs';

const need = key => {
  const value = process.env[key];
  if (!value) throw new Error('Missing environment variable: ' + key);
  return value;
};
const db = new Pool({connectionString:need('DATABASE_URL'),max:5});
const privateKeyPem = Buffer.from(need('LICENSE_PRIVATE_KEY_B64'),'base64').toString('utf8');
const pepper = need('OTP_PEPPER');
const adminKey = need('ADMIN_API_KEY');
if (adminKey.length < 32 || pepper.length < 32 || !privateKeyPem.includes('PRIVATE KEY')) {
  throw new Error('Invalid production signing/authentication configuration');
}
const mail = nodemailer.createTransport({
  host:need('SMTP_HOST'),
  port:Number(process.env.SMTP_PORT || 587),
  secure:process.env.SMTP_SECURE === 'true',
  requireTLS:true,
  auth:{user:need('SMTP_USER'),pass:need('SMTP_PASS')}
});
const from=need('SMTP_FROM');
const SESSION_MS=30*86400000;
const DEVICE=/^[A-Za-z0-9_-]{16,128}$/;
const EMAIL=/^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const sha=s=>createHash('sha256').update(s).digest('hex');
const otpHash=(email,code)=>createHmac('sha256',pepper).update(email+'|'+code).digest('hex');
const safeEqual=(a,b)=>typeof a==='string' && typeof b==='string' &&
  a.length===b.length && timingSafeEqual(Buffer.from(a),Buffer.from(b));
const normalizeEmail=s=>String(s||'').trim().toLowerCase();
const reply=(res,status,data)=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(JSON.stringify(data));};
const fail=(res,status,reason)=>reply(res,status,{error:reason});
const readJson=async req=>{
  let size=0, chunks=[];
  for await (const chunk of req){size+=chunk.length;if(size>8192)throw new Error('too_large');chunks.push(chunk);}
  let data;
  try{data=JSON.parse(Buffer.concat(chunks).toString('utf8'))}catch{throw new Error('invalid_json')}
  if(!data || typeof data!=='object' || Array.isArray(data))throw new Error('invalid_json');
  return data;
};
async function userByToken(req){
  const match=/^Bearer (\S+)$/.exec(req.headers.authorization||'');
  if(!match)return null;
  const result=await db.query(`SELECT s.user_id, s.device_id, u.email, u.trial_started_at
    FROM sessions s JOIN users u ON u.id=s.user_id
    WHERE s.token_hash=$1 AND s.expires_at>NOW()`,[sha(match[1])]);
  return result.rows[0]||null;
}

async function handler(req,res) {
  try {
    if(req.method==='GET' && req.url==='/health') {
      await db.query('SELECT 1');
      return reply(res,200,{status:'ok'});
    }
    if(req.method==='POST' && req.url==='/v1/auth/request'){
      const body=await readJson(req);
      const email=normalizeEmail(body.email);
      if(!EMAIL.test(email)||email.length>254)return fail(res,400,'invalid_email');
      const code=String(randomInt(0,1000000)).padStart(6,'0');
      const client=await db.connect();
      try{
        await client.query('BEGIN');
        const current=await client.query('SELECT requested_at FROM otp_challenges WHERE email=$1 FOR UPDATE',[email]);
        if(current.rows[0] && Date.now()-new Date(current.rows[0].requested_at).getTime()<60000){
          await client.query('ROLLBACK');
          return fail(res,429,'try_again_later');
        }
        await client.query(`INSERT INTO otp_challenges (email,code_hash,attempts,requested_at,expires_at)
          VALUES ($1,$2,0,NOW(),NOW()+INTERVAL '10 minutes')
          ON CONFLICT(email) DO UPDATE SET code_hash=EXCLUDED.code_hash,attempts=0,requested_at=NOW(),expires_at=EXCLUDED.expires_at`,
          [email,otpHash(email,code)]);
        // The SMTP send is completed before committing so failed deliveries don't start a trial.
        await mail.sendMail({from,to:email,subject:'Код входа — Замер потолка',
          text:'Код подтверждения: '+code+'\nОн действует 10 минут. Не сообщайте код посторонним.'});
        await client.query('COMMIT');
        return reply(res,200,{sent:true,expires_in_seconds:600});
      }catch(e){await client.query('ROLLBACK').catch(()=>{});throw e;}finally{client.release();}
    }
    if(req.method==='POST' && req.url==='/v1/auth/verify'){
      const body=await readJson(req);
      const email=normalizeEmail(body.email),code=String(body.code||''),deviceId=String(body.device_id||'');
      if(!EMAIL.test(email)||!/^[0-9]{6}$/.test(code)||!DEVICE.test(deviceId))return fail(res,400,'invalid_request');
      const client=await db.connect();
      try{
        await client.query('BEGIN');
        const challenge=await client.query('SELECT * FROM otp_challenges WHERE email=$1 FOR UPDATE',[email]);
        const row=challenge.rows[0];
        if(!row || new Date(row.expires_at).getTime()<=Date.now() || row.attempts>=5){
          await client.query('ROLLBACK');return fail(res,401,'invalid_or_expired_code');
        }
        await client.query('UPDATE otp_challenges SET attempts=attempts+1 WHERE email=$1',[email]);
        if(!safeEqual(row.code_hash,otpHash(email,code))){
          await client.query('COMMIT');return fail(res,401,'invalid_or_expired_code');
        }
        await client.query('DELETE FROM otp_challenges WHERE email=$1',[email]);
        const id=randomUUID();
        const users=await client.query(`INSERT INTO users (id,email) VALUES ($1,$2)
          ON CONFLICT(email) DO UPDATE SET email=EXCLUDED.email
          RETURNING id,trial_started_at`,[id,email]);
        const userId=users.rows[0].id;
        const token=randomBytes(32).toString('base64url');
        await client.query(`INSERT INTO sessions (token_hash,user_id,device_id,expires_at)
          VALUES ($1,$2,$3,NOW()+INTERVAL '30 days')`,[sha(token),userId,deviceId]);
        await client.query('COMMIT');
        return reply(res,200,{session_token:token,expires_in_seconds:SESSION_MS/1000,user_id:userId});
      }catch(e){await client.query('ROLLBACK').catch(()=>{});throw e;}finally{client.release();}
    }
    if(req.method==='GET' && req.url==='/v1/license'){
      const auth=await userByToken(req);
      if(!auth)return fail(res,401,'login_required');
      const grants=await db.query('SELECT kind,paid_until,revoked FROM grants WHERE user_id=$1',[auth.user_id]);
      const rights=entitlement(auth.trial_started_at,grants.rows);
      const license=issueLicense({userId:auth.user_id,deviceId:auth.device_id,entitlement:rights,privateKeyPem});
      return reply(res,200,{license:license.token,status:rights.kind,access_until:rights.accessUntil,server_time:Date.now()});
    }
    if(req.url==='/v1/admin/grants' && req.method==='POST'){
      if(!safeEqual(req.headers['x-admin-key'],adminKey))return fail(res,403,'forbidden');
      const b=await readJson(req);
      const email=normalizeEmail(b.email),kind=b.kind,ref=String(b.external_ref||''),source=String(b.source||'');
      const paidUntil=b.paid_until;
      if(!EMAIL.test(email)||!['monthly','lifetime'].includes(kind)||
         !/^[A-Za-z0-9_:.-]{6,160}$/.test(ref)||
         !/^[A-Za-z0-9_-]{2,40}$/.test(source)||
         (kind==='monthly' && (!paidUntil||Number.isNaN(new Date(paidUntil).getTime())||new Date(paidUntil).getTime()<=Date.now()))||
         (kind==='lifetime' && paidUntil!=null))return fail(res,400,'invalid_grant');
      const found=await db.query('SELECT id FROM users WHERE email=$1',[email]);
      if(!found.rowCount)return fail(res,404,'user_not_registered');
      const grant=await db.query(`INSERT INTO grants (id,user_id,kind,source,external_ref,paid_until)
        VALUES ($1,$2,$3,$4,$5,$6) ON CONFLICT(external_ref) DO NOTHING RETURNING id`,
        [randomUUID(),found.rows[0].id,kind,source,ref,kind==='monthly'?new Date(paidUntil):null]);
      if(!grant.rowCount)return fail(res,409,'duplicate_transaction');
      return reply(res,201,{grant_id:grant.rows[0].id});
    }
    if(req.url==='/v1/admin/revoke' && req.method==='POST'){
      if(!safeEqual(req.headers['x-admin-key'],adminKey))return fail(res,403,'forbidden');
      const b=await readJson(req),ref=String(b.external_ref||'');
      if(!/^[A-Za-z0-9_:.-]{6,160}$/.test(ref))return fail(res,400,'invalid_reference');
      const updated=await db.query('UPDATE grants SET revoked=TRUE WHERE external_ref=$1 AND revoked=FALSE RETURNING id',[ref]);
      return reply(res,200,{revoked:!!updated.rowCount});
    }
    return fail(res,404,'not_found');
  } catch(err){
    if(err.message==='too_large')return fail(res,413,'too_large');
    if(err.message==='invalid_json')return fail(res,400,'invalid_json');
    console.error('Request failed:',err.code||err.message);
    return fail(res,500,'internal_error');
  }
}

const port=Number(process.env.PORT||8080);
const server=http.createServer(handler);
server.requestTimeout=15000;
server.headersTimeout=10000;
server.listen(port,()=>console.log('License server listening on port '+port));
