# Maestro — Android E2E

These tests exercise the existing native Android app on an isolated Android emulator.

## Covered (initial smoke suite)
- Launch, home title and primary home cards.
- Create a new order, enter workspace and check its order/client/rooms sections.

**Not covered yet:** geometry editing, diagonals, calculations, backup/restore, all settings, PDF export, device FPS, visual QA. These require deterministic fixture data and dedicated flows.

## Local command
Build the app (Gradle 8.9 + JDK 17), install the debug APK on a running Android emulator, then run:

```sh
maestro test .maestro
```

GitHub Actions: `.github/workflows/maestro-e2e.yml` (manual or pull requests).
Test launch uses `clearState`: **emulator app data is erased**, never run the destructive flows against a personal phone with real measurements.

Only merge after the emulator E2E workflow passes. This suite supplements existing Gradle tests; it cannot establish 120 FPS or verify all buttons.
