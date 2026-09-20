# CeilingMeasure-Native

Репозиторий для безопасной упаковки приложения «Замер потолков» в Android APK без переписывания веб-приложения.

## Эталон
- `webapp/index.html` — единственный golden master UI и бизнес-логики.
- SHA-256: `7fade548ce909eb3bea6998c244fd90feb74b060a066d19c6a6c50c02c3f05d8`
- Размер: 517968 байт.
- Эталон нельзя рефакторить, сокращать, пересобирать «по смыслу» или заменять нативной реализацией.

## Перед работой Astra Max
1. Полностью прочитать `ASTRA_MAX_APK_COMMAND.txt`.
2. Полностью прочитать `FEATURE_CONTRACT.md`.
3. Выполнить `python scripts/verify_webapp.py webapp/index.html`.
4. Не начинать Android-миграцию, если проверка golden master не прошла.

## Текущий статус
Подготовительный этап завершён: эталонный webapp, функциональный контракт, контроль SHA и GitHub Actions-проверка находятся в репозитории. Android wrapper и APK ещё не создавались.

Следующий этап выполняется Astra Max строго по `ASTRA_MAX_APK_COMMAND.txt`.
