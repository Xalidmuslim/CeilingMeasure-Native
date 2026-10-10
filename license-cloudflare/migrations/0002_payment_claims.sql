-- Payment proofs are untrusted reports. No license is issued until seller confirms
-- the actual settlement in lava.top and explicitly approves the claim.
CREATE TABLE IF NOT EXISTS payment_claims (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  plan TEXT NOT NULL CHECK(plan IN ('monthly', 'lifetime')),
  order_reference TEXT NOT NULL UNIQUE,
  status TEXT NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','approved','rejected')),
  created_ms INTEGER NOT NULL,
  reviewed_ms INTEGER
);
CREATE INDEX IF NOT EXISTS payment_claims_pending_idx ON payment_claims(status,created_ms);
CREATE INDEX IF NOT EXISTS payment_claims_user_idx ON payment_claims(user_id,created_ms);
