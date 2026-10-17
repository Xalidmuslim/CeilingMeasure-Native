import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import pytest
from fastapi.testclient import TestClient
from api import Config,create_app

class Sender:
    def __init__(self):self.codes=[]
    def send(self,phone,code):
        self.codes.append((phone,code))
        return True

def setup(tmp_path):
    sender=Sender()
    app=create_app(Config(f"sqlite:///{tmp_path/'auth.db'}","secret-safe-for-tests-only-"+("X"*40)),sender)
    return TestClient(app),sender

def test_real_otp_but_never_fake_verified(tmp_path):
    c,s=setup(tmp_path)
    phone="+79001234567"
    assert c.post("/v1/auth/otp/start",json={"phone":phone}).status_code==200
    code=s.codes[-1][1]
    assert len(code)==6 and code.isdigit()
    response=c.post("/v1/auth/otp/verify",json={"phone":phone,"code":code})
    assert response.status_code==200,response.text
    token=response.json()["access_token"]
    assert len(token)>40
    auth={"Authorization":"Bearer "+token}
    me=c.get("/v1/me",headers=auth).json()
    assert me["driver_status"]=="NOT_STARTED"
    assert me["can_rent_real_vehicle"] is False
    assert c.get("/v1/bookings/eligibility",headers=auth).json()["allowed"] is False
    assert c.post("/v1/driver-verifications/start",headers=auth).status_code==503
    assert c.post("/v1/auth/otp/verify",json={"phone":phone,"code":code}).status_code==401

def test_login_limits_and_untrusted_requests(tmp_path):
    c,s=setup(tmp_path)
    assert c.get("/v1/me").status_code==401
    assert c.get("/v1/me",headers={"Authorization":"Bearer malicious"}).status_code==401
    for phone in ["+799","79001234567","+18005551212"]:
        assert c.post("/v1/auth/otp/start",json={"phone":phone}).status_code==422
    phone="+79009998877"
    assert c.post("/v1/auth/otp/start",json={"phone":phone}).status_code==200
    assert c.post("/v1/auth/otp/start",json={"phone":phone}).status_code==429
    bad="111111" if s.codes[-1][1]!="111111" else "222222"
    for _ in range(5):
        assert c.post("/v1/auth/otp/verify",json={"phone":phone,"code":bad}).status_code==401
    assert c.post("/v1/auth/otp/verify",json={"phone":phone,"code":s.codes[-1][1]}).status_code==401

def test_production_rejects_sqlite_and_weak_secrets(tmp_path):
    with pytest.raises(RuntimeError):
        create_app(Config("sqlite:///foo.db","Q"*40,environment="production"))
    with pytest.raises(ValueError):
        create_app(Config("sqlite:///foo.db","weak",environment="development"))
