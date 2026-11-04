# CADS — enforceable release gate (CeilingMeasure pilot)

**State on 2026-11-04:** staged in draft PR #44. Until merged and protected by GitHub branch rules, these requirements are **proposed controls**, not enforced across repositories or production.

## Verified blockers

- `main` branch is currently **not protected**, GitHub API reported `protected=false`.
- Repository rulesets collection was **empty**.
- The new CADS Dependency Review workflow failed with `Dependency review is not supported on this repository`: repository **Dependency Graph is disabled**. Enable it in [Settings → Security & analysis](https://github.com/Xalidmuslim/CeilingMeasure-Native/settings/security_analysis), then rerun PR checks. Do not mark this as green or use `continue-on-error`.
- Existing complete Android instrumentation had **8 failures out of 116**. Resolve root causes; do not delete tests.
- The feature-v270 preview Release publish attempt previously failed with **403 Resource not accessible by integration**. GitHub Actions integration must have the correct allowed permissions before publishing. This does not block a locally retained test artifact, but no APK is production-approved.
- No production signing certificate, paid-licensing production environment, or physical-device acceptance tests are verified.
- Crash reporting (Firebase Crashlytics/Sentry) is **not connected**; requires separate account configuration, a privacy review and integration tests.

## Scope added in PR #44

- `.github/workflows/commercial-quality-gate.yml`: unit tests, lint, separate preview build, full Android instrumentation.
- `.github/workflows/ceiling-v270-auto-ceiling.yml`: full Android-15 instrumentation required inside the build job before the *preview* GitHub Release job can run. PR events may run CI but may not publish. This remains a **preview** APK signed with the Android debug key and a separate `.smooth270` app ID.
- `.github/workflows/cads-codeql.yml`: Java-only static security analysis via CodeQL. For Kotlin code, switch to proper compiled analysis.
- `.github/workflows/cads-dependency-review.yml`: fails on newly introduced high/critical dependency vulnerabilities, **when Dependency Graph is enabled**.
- `.github/dependabot.yml`: opens Gradle/Actions update PRs. Does not auto-merge changes.
- `scripts/test_cads_release_policy.py`: guards against bypassing the full suite in the active preview publishing workflow.
- `scripts/commercial_readiness_check.py`: static preflight; in production mode intentionally refuses unconfigured signing/licensing. Passing it is not product certification.

## Turn status checks into mandatory GitHub rules

> Connector tools available to this assistant cannot change branch protection rules. A repository administrator must perform this once in GitHub settings.

1. Enable **Dependency Graph** in repository Settings → Security & analysis. Check that the dependency-review PR workflow can run.
2. Open **Settings → Rules → Rulesets** (or **Branches → Add branch protection rule**) for the target **integration/production** branch; activate **Require a pull request before merging** and **Require status checks to pass**. Disallow force pushes and deletions; enforce for administrators to prevent accidental bypass.
3. For the **feature/v270-auto-ceiling-installation** target branch, select required checks when they are visible and run on that branch: `Preview checks and full Android regression`, `test-build-preview`, `CADS CodeQL security`, and `CADS Dependency Review`. Keep check names unique. Select the GitHub Actions source if available.
4. For `main`, only require checks **after CADS workflows have actually landed and run there**; otherwise a non-existent required check will block legitimate merges. Protect `main` from direct pushes immediately and migrate the CADS workflow via a tested PR.
5. Test policy enforcement: open a disposable PR with a deliberately failing unit test; verify merge is blocked. Remove that deliberately failing test in the same PR, rerun, then close the test PR. Confirm no bypass.
6. Configure a separate protected environment named `production` with required reviewer(s) and secrets scoped to this environment before introducing a paid publishing workflow. **Never** make debug-key-signed previews eligible for paid distribution.

Because the current CI contains known failures, requiring checks will block feature merges until those failures are fixed. This is intentional.

## CADS minimum acceptance for each future commercial repository

- Protected integration and production branches, PR-only merges, mandatory CI with unique check names.
- Relevant language/framework unit tests, integration tests, UI/end-to-end tests on critical flows, and explicit regression fixtures for prior bugs.
- Static code security analysis, dependencies/secret checks; threat model and authorization review for sensitive workloads.
- Signed release tied to the reviewed source commit; build reproducibility, artifact hash and rollback plan.
- Backup/restore and migration tests with **representative non-personal test data**.
- At least one real test device/target runtime; measured performance where promised; no unverified FPS guarantees.
- Error monitoring and incident handling after release, with privacy safeguards.
- Named owner responsible for operational support and escalation. A green CI alone is not proof of compliance or security.

When copying CADS to another project, **adapt checks to that project's stack**. Do not copy Android/Java commands into Flutter, Kotlin/Compose, server or web repositories. Do not change production code, customer data, keys or existing release identity simply to introduce CADS.

## Production release decision

Commercial release = **blocked** until all required automated checks **plus human-signoff** for real-device compatibility, legal/payment obligations, production signing and operational readiness. Preview build = *test artifact only*.
