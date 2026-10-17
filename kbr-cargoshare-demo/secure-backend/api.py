"""Server-side phone authentication and fail-closed driver verification groundwork.
No photos, passports or driver licence numbers are accepted or stored here.
Deploy ONLY with PostgreSQL hosted in Russia and an HTTPS reverse proxy.
"""
from __future__ import annotations
import hashlib, hmac, os, secrets, time, uuid
from dataclasses import dataclass
import requests
from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from sqlalchemy import Column, String, Float, Integer, Boolean, ForeignKey, create_engine, select, func
from sqlalchemy.orm import declarative_base, sessionmaker, Session

Base=declarative_base()
class User(Base):
    __tablename__="users"
    id=Column(String(36),primary_key=True)
    phone=Column(String(12),nullable=False,unique=True)
    driver_status=Column(String(20),nullable=False,default="NOT_STARTED")
    verified_until=Column(Float)
class Otp(Base):
    __tablename__="otp_requests"
    id=Column(String(36),primary_key=True)
    phone=Column(String(12),nullable=False,index=True)
    ip_hash=Column(String(64),nullable=False,index=True)
    code_hash=Column(String(64),nullable=False)
    created=Column(Float,nullable=False)
    expires=Column(Float,nullable=False)
    attempts=Column(Integer,nullable=False,default=0)
    used=Column(Boolean,nullable=False,default=False)
class AuthSession(Base):
    __tablename__="sessions"
    token_hash=Column(String(64),primary_key=True)
    user_id=Column(String(36),ForeignKey("users.id"),nullable=False)
    expires=Column(Float,nullable=False)
    revoked=Column(Boolean,nullable=False,default=False)

@dataclass(frozen=True)
class Config:
    database_url:str
    secret:str
    sms_login:str=""
    sms_key:str=""
    sms_sign:str=""
    environment:str="development"
    @classmethod
    def env(cls):
        result=cls(os.getenv("DATABASE_URL",""),os.getenv("APP_SECRET",""),
                   os.getenv("SMSAERO_LOGIN",""),os.getenv("SMSAERO_API_KEY",""),
                   os.getenv("SMSAERO_SIGN",""),os.getenv("APP_ENV","production"))
        if not result.database_url or len(result.secret)<32:
            raise RuntimeError("Set DATABASE_URL and APP_SECRET (min 32 chars)")
        return result
class PhoneIn(BaseModel):
    phone:str=Field(pattern=r"^\+7\d{10}$")
class VerifyIn(PhoneIn):
    code:str=Field(pattern=r"^\d{6}$")
class SmsAero:
    def __init__(self,config:Config): self.config=config
    def send(self,phone:str,code:str)->bool:
        c=self.config
        if not(c.sms_login and c.sms_key and c.sms_sign):return False
        try:
            response=requests.post("https://gate.smsaero.ru/v2/sms/send",
                auth=(c.sms_login,c.sms_key),
                json={"number":phone[1:],"sign":c.sms_sign,
                      "text":f"ГрузШер КБР: код входа {code}. Никому его не сообщайте."},
                headers={"Accept":"application/json"},timeout=8)
            response.raise_for_status()
            return response.json().get("success") is True
        except (requests.RequestException,ValueError): return False

def create_app(config:Config|None=None,sender=None):
    c=config or Config.env()
    if len(c.secret)<32:raise ValueError("APP_SECRET too short")
    if c.environment=="production" and not c.database_url.startswith("postgresql+"):
        raise RuntimeError("Production requires PostgreSQL, not local SQLite")
    engine=create_engine(c.database_url,pool_pre_ping=True)
    Base.metadata.create_all(engine)
    SessionMaker=sessionmaker(bind=engine,expire_on_commit=False)
    api=FastAPI(title="GruzSher authenticated API",version="1.0.0")
    api.state.sms=sender if sender is not None else SmsAero(c)
    def mac(value:str)->str:
        return hmac.new(c.secret.encode(),value.encode(),hashlib.sha256).hexdigest()
    def database():
        with SessionMaker() as db:yield db
    def bad_code():return HTTPException(401,"Неверный или просроченный код")
    @api.middleware("http")
    async def private_responses(request:Request,call_next):
        response=await call_next(request)
        response.headers["Cache-Control"]="no-store"
        response.headers["X-Content-Type-Options"]="nosniff"
        response.headers["Referrer-Policy"]="no-referrer"
        return response
    @api.get("/health")
    def health():return {"ok":True}
    @api.post("/v1/auth/otp/start")
    def send_code(body:PhoneIn,request:Request,db:Session=Depends(database)):
        if sender is None and not(c.sms_login and c.sms_key and c.sms_sign):
            raise HTTPException(503,"SMS_PROVIDER_NOT_CONFIGURED")
        now=time.time()
        ip_key=mac("ip:"+str(request.client.host if request.client else "unknown"))
        recent=db.scalar(select(Otp).where(Otp.phone==body.phone).order_by(Otp.created.desc()).limit(1))
        if recent and now-recent.created<60:raise HTTPException(429,"Повторите через минуту")
        phone_count=db.scalar(select(func.count()).select_from(Otp).where(Otp.phone==body.phone,Otp.created>now-86400))
        ip_count=db.scalar(select(func.count()).select_from(Otp).where(Otp.ip_hash==ip_key,Otp.created>now-86400))
        if phone_count>=5 or ip_count>=30:raise HTTPException(429,"Лимит SMS превышен")
        code=f"{secrets.randbelow(1000000):06d}"
        record=Otp(id=str(uuid.uuid4()),phone=body.phone,ip_hash=ip_key,
            code_hash=mac(body.phone+":"+code),created=now,expires=now+300,attempts=0,used=False)
        db.add(record)
        db.commit()
        if not api.state.sms.send(body.phone,code):
            record.used=True
            db.commit()
            raise HTTPException(503,"SMS сейчас недоступны")
        return {"result":"SENT","expires_in_seconds":300}
    @api.post("/v1/auth/otp/verify")
    def verify_code(body:VerifyIn,db:Session=Depends(database)):
        now=time.time()
        otp=db.scalar(select(Otp).where(Otp.phone==body.phone).order_by(Otp.created.desc()).limit(1))
        if otp is None or otp.used or otp.expires<now or otp.attempts>=5:raise bad_code()
        otp.attempts+=1
        if not hmac.compare_digest(otp.code_hash,mac(body.phone+":"+body.code)):
            db.commit()
            raise bad_code()
        otp.used=True
        user=db.scalar(select(User).where(User.phone==body.phone))
        if user is None:
            user=User(id=str(uuid.uuid4()),phone=body.phone,driver_status="NOT_STARTED")
            db.add(user)
            db.flush()
        token=secrets.token_urlsafe(32)
        db.add(AuthSession(token_hash=mac("token:"+token),user_id=user.id,expires=now+86400,revoked=False))
        db.commit()
        return {"access_token":token,"expires_in_seconds":86400}
    bearer=HTTPBearer(auto_error=False)
    def authenticated(credentials:HTTPAuthorizationCredentials|None=Depends(bearer),
                      db:Session=Depends(database))->User:
        if credentials is None or credentials.scheme.lower()!="bearer":
            raise HTTPException(401,"Необходим вход")
        auth=db.get(AuthSession,mac("token:"+credentials.credentials))
        if auth is None or auth.revoked or auth.expires<time.time():
            raise HTTPException(401,"Сеанс истёк")
        user=db.get(User,auth.user_id)
        if user is None:raise HTTPException(401,"Сеанс истёк")
        return user
    def permitted(user:User)->bool:
        return bool(user.driver_status=="VERIFIED" and
                    user.verified_until and user.verified_until>time.time())
    @api.get("/v1/me")
    def me(user:User=Depends(authenticated)):
        return {"user_id":user.id,"phone_masked":"+7******"+user.phone[-4:],
                "driver_status":user.driver_status,"can_rent_real_vehicle":permitted(user)}
    @api.get("/v1/bookings/eligibility")
    def eligibility(user:User=Depends(authenticated)):
        return {"allowed":permitted(user),"requires_verification":not permitted(user)}
    @api.post("/v1/driver-verifications/start")
    def start_verification(user:User=Depends(authenticated)):
        # Deliberately fail closed until a licensed KYC provider is contracted,
        # consent workflow is approved and a verified integration is implemented.
        raise HTTPException(503,"KYC_PROVIDER_NOT_CONFIGURED")
    return api
