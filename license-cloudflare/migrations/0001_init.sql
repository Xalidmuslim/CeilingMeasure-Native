-- Users are anonymous; the recovery secret is generated and shown once in the Android app.
-- Never store the recovery secret itself; only a SHA-256 digest.
CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  recovery_hash TEXT UNIQUE NOT NULL,
  trial_started_ms INTEGER NOT NULL,
  active_device_id TEXT NOT NULL,
  created_ms INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_recovery_hash ON users(recovery_hash);

CREATE TABLE IF NOT EXISTS grants (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  kind TEXT NOT NULL CHECK (kind IN ('monthly', 'lifetime')),
  source TEXT NOT NULL,
  external_ref TEXT UNIQUE NOT NULL,
  paid_until_ms INTEGER,
  revoked INTEGER NOT NULL DEFAULT 0,
  created_ms INTEGER NOT NULL,
  CHECK (
    (kind = 'lifetime' AND paid_until_ms IS NULL) OR
    (kind = 'monthly' AND paid_until_ms IS NOT NULL)
  )
);
CREATE INDEX IF NOT EXISTS idx_grants_user ON grants(user_id);

-- Salted/hmac hashed IP for coarse registration-abuse limiting; no raw IP stored.
CREATE TABLE IF NOT EXISTS registration_limits (
  ip_hash TEXT NOT NULL,
  utc_day INTEGER NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 1,
  PRIMARY KEY (ip_hash, utc_day)
);
