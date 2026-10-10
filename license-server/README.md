# CeilingMeasure native v225 — private license service (pilot)

This is the first-phase **license server**, not a payment processor. No card details pass through the server. Payment confirmation is currently **manual** via an authenticated admin API. The server must be hosted with TLS on a public domain before licenses can be issued to real clients.

## What exists
- Email sign-in with a 6-digit, 10-minute one-time code.
- 7-day trial, started at successful email verification and stored in PostgreSQL.
- Manually issued monthly (fixed expiry) and lifetime access grants.
- 14-day signed offline lease (less if the trial or subscription ends earlier).
- RSA-SHA256 signed license claims, tied to one application installation/device identifier.
- Idempotency via external payment reference; manual revocation.
- Reinstall and new-device access via signing into the same email account.

## Configure locally / on a hosting service
Requires Node.js >=20, PostgreSQL, SMTP with real account, HTTPS reverse proxy, and a secure secret store.

1. Set `DATABASE_URL` and execute `license-server/sql/schema.sql` once against the PostgreSQL database (`psql "$DATABASE_URL" -f sql/schema.sql`).
2. Generate your *own* RSA private key, which MUST never be committed:
   ```bash
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out private.pem
   openssl pkey -in private.pem -pubout -outform DER | base64 | tr -d '\\n'
   base64 < private.pem | tr -d '\\n'
   ```
   The first base64 value is the **public** SPKI key to embed in the Android build; the second is the **private** PEM encoded as base64 for `LICENSE_PRIVATE_KEY_B64` on the server.
3. Set `OTP_PEPPER` and `ADMIN_API_KEY` to independent random secrets at least 32 characters each. Set SMTP variables and `PORT`. See `.env.example` (the Node process **does not automatically load** the file).
4. Execute `npm install`, `npm test`, `npm start` in `license-server/`.
5. Serve only through HTTPS and restrict the admin API by firewall or VPN/IP allowlist. Enable request throttling at the reverse proxy (especially `/v1/auth/request`), monitoring and PostgreSQL backups.

## Android build
The Android project defaults to **legacy/free access** if the license service has not been configured; it MUST NOT accidentally disable the original v224 for existing users.

For a controlled, license-enabled release, build with BOTH:
```bash
gradle assembleRelease \\
  -PceilingLicenseUrl=https://licenses.example.com \\
  -PceilingLicensePublicKeyB64=PUBLIC_BASE64_SPKI
```
(The URLs/keys above are placeholders.) The two values may also be set using the `CEILING_LICENSE_URL` and `CEILING_LICENSE_PUBLIC_KEY_B64` environment variables. Neither value is secret. The server's PRIVATE key must **never** be stored in an APK.

## API
- `POST /v1/auth/request` JSON: `{"email":"client@example.com"}` — send code.
- `POST /v1/auth/verify` JSON: `{"email":"client@example.com","code":"123456","device_id":"32_random_alphanumeric_chars"}` — open account/return session.
- `GET /v1/license` with `Authorization: Bearer <session_token>` — refresh signed offline license.
- `POST /v1/admin/grants` with `X-Admin-Key: <ADMIN_API_KEY>` — admin-only, JSON for monthly:
  `{"email":"client@example.com","kind":"monthly","source":"manual","external_ref":"receipt_000001","paid_until":"2026-12-01T00:00:00Z"}`.
- Lifetime grant: same fields with `"kind":"lifetime"`, omitting `paid_until`.
- `POST /v1/admin/revoke`: `{"external_ref":"receipt_000001"}` with the same admin header.

**Never** grant an entitlement based on a screenshot or client-side "payment success" flag alone. When a compliant payment provider is selected, validate its signed webhooks or authoritative API server-side and use its unique payment IDs as `external_ref`. Recurring auto-charge is not integrated.

## Limitations and required work before paid launch
- The host, DB, TLS domain, SMTP service, and real payment provider are **not provisioned** by this repository.
- The GitHub Actions debug APK uses empty server configuration and is a **non-monetized preview**.
- Device IDs are app-generated identifiers, not hardware attestation. The code deters casual reuse but is not tamper-proof.
- A revoked paid grant may remain usable offline until the last signed lease expires (up to 14 days).
- 7-day trials are unique per email account, not necessarily per person; rate limiting and fraud controls are needed.
- Existing customers of free v224 require an explicit migration/grandfathering policy.
- For compliance, choose a lawful payment service, define seller and tax handling, and publish user terms/privacy information before activation.
