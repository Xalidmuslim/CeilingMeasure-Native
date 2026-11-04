# «Замер потолка» — Commercial Release Readiness v1.0

Status: **BLOCKED FOR SALE** as of 2026-11-04. This document is a release decision log, not a claim that the app was tested on a physical device.

## Baseline and scope

- Source: `Xalidmuslim/CeilingMeasure-Native`, `feature/v270-auto-ceiling-installation`, PR #43, commit `4f9d524f4c004ff6976ee2647b2a3f7e5631745a` at inspection.
- Native Android **Java / View / Canvas** application. Keep existing architecture; do not switch to Compose, Flutter, WebView, or a new app ID.
- Preserve saved clients, orders, rooms, drawings, measurements, settings, price presets, PDFs, import/export, navigation and approved animations.
- Changes here add only release checks and documentation. No geometry, measurement or UI behavior is changed.

## Verified evidence and outstanding issues

1. **Preview CI:** workflow run `41424193163`, job `test-build-preview` succeeded. This confirms an isolated, debug-key-signed `.smooth270` optimized preview was built in that run; it is not proof of a production release or on-device performance.
2. **Regression failures:** workflow run `41424193151`, job `verify`, executed **116 Android tests, 8 failures**. Recorded examples include `ClassCastException` (View cast to ViewGroup), workspace UI assertions and a wall-input blink assertion. Full suite must pass before paid release.
3. **Old CI gate:** run `41423989325` failed at `Verify native-only source` while executing legacy hard-coded code-marker checks. Investigate the exact outdated predicate rather than removing functionality or weakening tests.
4. **Publishing permissions:** in run `41424193163` the `publish-v270` job failed with HTTP **403 Resource not accessible by integration**. This is a permission issue, not evidence that APK compilation failed. Correct GitHub Actions integration permissions separately; preview artifact remains an alternative, not a production release.
5. **Signing:** `app/build.gradle` signs `releasePreview` with `signingConfigs.debug` and gives it `.smooth270`. There is **no explicitly configured production signingConfig** in the inspected `release` block. Do not distribute preview under a commercial label.
6. **Licensing:** `NativeLicenseClient.canEdit()` permits editing when the license service is not configured; `CEILING_LICENSE_URL` and `CEILING_LICENSE_PUBLIC_KEY_B64` default to empty. Paid entitlement enforcement is **not demonstrated**.
7. **Data:** manifest enables backup, and the inspected backup XML excludes `native_license_access.xml` while including other app data. Test that backups actually restore drawings/prices, and review privacy, retention and recovery policies before accepting paid users.

## Release contract

### Preview gate — mandatory for every PR

- `python3 -m unittest discover -s scripts -p test_commercial_readiness_check.py -v`
- `python3 scripts/commercial_readiness_check.py --mode preview`
- `gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleReleasePreview`
- `python3 scripts/commercial_readiness_check.py --mode preview --apk app/build/outputs/apk/releasePreview/app-releasePreview.apk`
- **All** Android instrumentation regressions using `bash scripts/run_android_smoke.sh`, not only the curated passing subsets.
- Retain failing test evidence. Never change the expected result to make a broken test pass.

### Commercial release gate — mandatory before accepting payments

- All preview gate checks green on the release commit; resolve all eight known instrumentation failures.
- Release signed by a dedicated **production** key. Verify APK signature certificate, package ID, versionCode, and install/upgrade compatibility; maintain secure key custody. Never use the Android debug key for paid releases.
- Licensing service deployed with HTTPS, signature verification and tests for trial expiry, renewals, rollback/clock skew, unavailable server and recovery. If replacing server licenses with a different payment system, document and test its entitlements before changing `canEdit()`.
- Real-device verification on an agreed Android device matrix: cold start, lengthy drawing, two-finger gestures, keyboard, navigation, PDF, backup/import/export, offline behavior, permissions, and version upgrade **without data loss**.
- Measure frame times and jank with Perfetto / Macrobenchmark. A 120 Hz display setting does not prove 120 FPS.
- Test clean install and upgrade **from the current paid production package and signing identity** using representative anonymous test records. Do not migrate previews into production blindly, because application IDs differ.
- Security/privacy review: app permissions, backup destination, sensitive license data, dependency vulnerabilities, data deletion, and support incident process.
- Release notes, reproducible build evidence, SHA-256, rollback/support procedure, and a named maintainer recorded.
- Publish only from the reviewed release commit. A GitHub Release failure must not be bypassed by renaming an unverified preview as a sale-ready APK.

### Strict preflight

`python3 scripts/commercial_readiness_check.py --mode production --apk <path-to-production-apk>`

This intentionally fails with the current Gradle and license configuration. Passing the static preflight **alone** is still not certification; signing and actual device tests are separate gates.

## Decisions

- Leave geometry, calculations, customer storage, stable UI and animations untouched in this PR.
- Keep preview APK freely installable only for testing, with a separate package ID.
- Do not market the current preview as a finished, licensed product.
- Next engineering work: reproduce/fix the eight Android instrumentation failures, then address production signing and license-server readiness. Each fix must have a failing regression test before the change and a passing test afterward.
