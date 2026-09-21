# CeilingMeasure-Native

Цель репозитория — создать полноценное нативное Android-приложение «Замер потолков» с точной функциональной и визуальной parity относительно утверждённой web-версии.

## Golden master

- `webapp/index.html` — неизменяемый эталон поведения, UI, жестов, формул и данных.
- APP_BUILD_VERSION: `184-gesture-fix`
- SHA-256: `354fbede167490d2481d3253507ff2829dcefec495298075487880ea2258a388`
- Эталон используется для аудита, сравнения и регрессии.
- Финальная native-версия не должна использовать WebView/HTML/JS как рабочий UI.

## Native migration documents

1. `NATIVE_PARITY_SPEC.md` — главный контракт точной нативной копии.
2. `NATIVE_MIGRATION_MATRIX.md` — функция → web reference → native implementation → test → status.
3. `ASTRA_MAX_NATIVE_MIGRATION_COMMAND.txt` — текущая команда Astra Max. На первом запуске только Phase 0 audit.
4. `FEATURE_CONTRACT.md` — legacy-контракт WebView-оболочки и функциональный baseline.
5. `ASTRA_MAX_APK_COMMAND.txt` — legacy-команда старой WebView-упаковки; для native migration не использовать.

## Правило миграции

Не делать «похожее приложение» и не переносить всё одним rewrite.

Сначала Astra Max обязана полностью изучить golden master, заполнить audit-документы и matrix, не меняя native-код. Только после отдельного подтверждения пользователя начинается Phase 1 реализации.

## Phase sequence

- Phase 0 — audit only
- Phase 1 — data model / persistence / compatibility
- Phase 2 — native shell / visual tokens / navigation
- Phase 3 — orders / rooms
- Phase 4 — drawing engine
- Phase 5 — wall / points / closure / undo-redo
- Phase 6 — zoom / pan / scroll / magnifier / snapping / haptics
- Phase 7 — diagonals
- Phase 8 — dimension labels / Numbers
- Phase 9 — calculations / materials
- Phase 10 — saved measurements
- Phase 11 — backup / restore
- Phase 12 — PDF / client estimate
- Phase 13 — settings / night mode
- Phase 14 — full parity regression
- Phase 15 — release packaging after user acceptance

## Current action for Astra Max

Open and execute exactly:

`ASTRA_MAX_NATIVE_MIGRATION_COMMAND.txt`

Do not start Phase 1 until the Phase 0 audit report is reviewed and accepted.
