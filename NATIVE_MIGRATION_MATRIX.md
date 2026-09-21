# NATIVE_MIGRATION_MATRIX — «Замер потолков»

## Статусы

- TODO — ещё не перенесено/не проверено
- AUDITED — web-реализация найдена и задокументирована
- IMPLEMENTING — native реализация в работе
- PASS — native поведение подтверждено тестом/сравнением
- FAIL — обнаружено расхождение
- UNVERIFIED — автоматическая/ручная проверка ещё не выполнена

Astra обязана заполнять столбцы «Web reference», «Native reference», «Test evidence» конкретными путями, функциями, test names, screenshots, commit SHA. Нельзя ставить PASS без evidence.

| ID | Область | Требование parity | Web reference | Native reference | Test evidence | Status |
|---|---|---|---|---|---|---|
| P-001 | Golden master | `webapp/index.html` не меняется; SHA = `354fbede167490d2481d3253507ff2829dcefec495298075487880ea2258a388` | TODO | N/A | SHA check | TODO |
| P-002 | Audit | Полный inventory экранов/состояний/модалок | TODO | N/A | Audit report | TODO |
| P-003 | Audit | Полный список web event handlers/gestures | TODO | N/A | Audit report | TODO |
| P-004 | Audit | Полный data schema + migrations | TODO | N/A | Schema report | TODO |
| P-005 | Audit | Формулы/коэффициенты/округление извлечены из web | TODO | N/A | Formula report | TODO |
| P-006 | Audit | Visual tokens: размеры, spacing, radius, shadow, typography, colors | TODO | N/A | Token report | TODO |
| O-001 | Заказ | Создание заказа | TODO | TODO | E2E | TODO |
| O-002 | Заказ | Удаление заказа | TODO | TODO | E2E | TODO |
| O-003 | Заказ | Имя клиента = заголовок заказа | TODO | TODO | E2E | TODO |
| O-004 | Заказ | Телефон | TODO | TODO | E2E | TODO |
| O-005 | Заказ | Адрес | TODO | TODO | E2E | TODO |
| O-006 | Заказ | Нет функции номера заказа | TODO | TODO | Screenshot/state | TODO |
| O-007 | Заказ | Автосохранение | TODO | TODO | Relaunch test | TODO |
| O-008 | Заказ | Текущий заказ восстанавливается после relaunch | TODO | TODO | Relaunch test | TODO |
| R-001 | Комнаты | Добавить комнату | TODO | TODO | E2E | TODO |
| R-002 | Комнаты | Выбрать комнату | TODO | TODO | E2E | TODO |
| R-003 | Комнаты | Long press меню комнаты | TODO | TODO | Gesture test | TODO |
| R-004 | Комнаты | Переименовать | TODO | TODO | E2E | TODO |
| R-005 | Комнаты | Дублировать | TODO | TODO | E2E | TODO |
| R-006 | Комнаты | Удалить | TODO | TODO | E2E | TODO |
| R-007 | Комнаты | Индикатор завершённости parity | TODO | TODO | Screenshot | TODO |
| R-008 | Комнаты | Один чертёж на комнату | TODO | TODO | E2E | TODO |
| R-009 | Комнаты | Активная комната сохраняется | TODO | TODO | Relaunch test | TODO |
| R-010 | Комнаты | Горизонтальная компоновка карточек совпадает | TODO | TODO | Screenshot diff | TODO |
| D-001 | Чертёж | Режим Стена | TODO | TODO | E2E | TODO |
| D-002 | Чертёж | Режим Точки | TODO | TODO | E2E | TODO |
| D-003 | Чертёж | Режим Диагональ | TODO | TODO | E2E | TODO |
| D-004 | Чертёж | Режим Цифры | TODO | TODO | E2E | TODO |
| D-005 | Чертёж | Режим по умолчанию совпадает | TODO | TODO | Startup state | TODO |
| W-001 | Стены | Первая стена создаётся как в web | TODO | TODO | Gesture test | TODO |
| W-002 | Стены | Последующие стены только от допустимых концов/углов | TODO | TODO | Gesture test | TODO |
| W-003 | Стены | Одна стена за один gesture | TODO | TODO | Gesture test | TODO |
| W-004 | Стены | Точки создаются только по web-правилам | TODO | TODO | Gesture test | TODO |
| W-005 | Стены | Замыкание контура | TODO | TODO | E2E | TODO |
| W-006 | Стены | После замыкания новые стены запрещены | TODO | TODO | Negative test | TODO |
| W-007 | Стены | После замыкания нет хвоста | TODO | TODO | Geometry assert | TODO |
| W-008 | Стены | В закрытом контуре режим Стена остаётся доступен для редактирования | TODO | TODO | E2E | TODO |
| W-009 | Стены | Long press стены/размера открывает редактирование | TODO | TODO | Gesture test | TODO |
| W-010 | Стены | Undo | TODO | TODO | State test | TODO |
| W-011 | Стены | Redo | TODO | TODO | State test | TODO |
| W-012 | Стены | Fit view | TODO | TODO | Viewport assert | TODO |
| K-001 | Клавиатура | Custom numeric keypad | TODO | TODO | Screenshot/E2E | TODO |
| K-002 | Клавиатура | Системная клавиатура не используется для размеров | TODO | TODO | UI test | TODO |
| K-003 | Клавиатура | Начальное `0` | TODO | TODO | State test | TODO |
| K-004 | Клавиатура | Decimal comma | TODO | TODO | Input test | TODO |
| K-005 | Клавиатура | Backspace/delete | TODO | TODO | Input test | TODO |
| K-006 | Клавиатура | Явный `×` закрывает | TODO | TODO | E2E | TODO |
| K-007 | Клавиатура | Tap outside закрывает | TODO | TODO | Gesture test | TODO |
| K-008 | Клавиатура | Некорректный ввод не портит данные | TODO | TODO | Negative test | TODO |
| G-001 | Жесты | Pinch zoom двумя пальцами | TODO | TODO | Gesture test | TODO |
| G-002 | Жесты | Pan двумя пальцами | TODO | TODO | Gesture test | TODO |
| G-003 | Жесты | Single-touch drawing не конфликтует с pinch/pan | TODO | TODO | Gesture test | TODO |
| G-004 | Жесты | В Wall mode вертикальный scroll над drawing заблокирован | TODO | TODO | Gesture test | TODO |
| G-005 | Жесты | Правая полоса 74 px разрешает page scroll | TODO | TODO | Boundary test | TODO |
| G-006 | Жесты | Правило scroll сохраняется после closure | TODO | TODO | E2E | TODO |
| G-007 | Жесты | Нет системного text-selection/context-menu на drawing | TODO | TODO | Long-press test | TODO |
| S-001 | Snap | Magnifier parity | TODO | TODO | Screenshot/gesture | TODO |
| S-002 | Snap | Красная направляющая | TODO | TODO | Screenshot | TODO |
| S-003 | Snap | Притягивание к направляющей | TODO | TODO | Geometry test | TODO |
| S-004 | Snap | 90° detection | TODO | TODO | Geometry test | TODO |
| S-005 | Snap | 90° marker parity | TODO | TODO | Screenshot | TODO |
| S-006 | Snap | Haptic на 90° | TODO | TODO | Haptic test/manual | TODO |
| S-007 | Snap | Haptic на snapping | TODO | TODO | Haptic test/manual | TODO |
| S-008 | Snap | Пороги snapping перенесены из web | TODO | TODO | Unit tests | TODO |
| T-001 | Точки | Создание/редактирование по web-правилам | TODO | TODO | E2E | TODO |
| T-002 | Точки | Hit areas совпадают | TODO | TODO | Gesture boundary test | TODO |
| T-003 | Точки | Magnifier/snapping в режиме Точки | TODO | TODO | E2E | TODO |
| T-004 | Точки | State persistence | TODO | TODO | Relaunch test | TODO |
| X-001 | Диагонали | Создание диагонали | TODO | TODO | E2E | TODO |
| X-002 | Диагонали | Автодиагональ/расчёт | TODO | TODO | Formula test | TODO |
| X-003 | Диагонали | Ручной ввод | TODO | TODO | E2E | TODO |
| X-004 | Диагонали | Long press edit | TODO | TODO | Gesture test | TODO |
| X-005 | Диагонали | Delete | TODO | TODO | E2E | TODO |
| X-006 | Диагонали | Явный `×` в вводе | TODO | TODO | UI test | TODO |
| X-007 | Диагонали | Значение внутри контура | TODO | TODO | Screenshot diff | TODO |
| X-008 | Диагонали | Нет лишней рамки вокруг числа | TODO | TODO | Screenshot diff | TODO |
| N-001 | Цифры | Independent X/Y | TODO | TODO | State test | TODO |
| N-002 | Цифры | Drag label | TODO | TODO | Gesture test | TODO |
| N-003 | Цифры | Rotation 0–360° | TODO | TODO | Gesture/state test | TODO |
| N-004 | Цифры | Size/scale `−/+` | TODO | TODO | UI/state test | TODO |
| N-005 | Цифры | Reset size если есть | TODO | TODO | UI/state test | TODO |
| N-006 | Цифры | Pose сохраняется после redraw | TODO | TODO | State test | TODO |
| N-007 | Цифры | Pose сохраняется после zoom | TODO | TODO | State test | TODO |
| N-008 | Цифры | Pose сохраняется после room switch | TODO | TODO | State test | TODO |
| N-009 | Цифры | Pose сохраняется после relaunch | TODO | TODO | Relaunch test | TODO |
| N-010 | Цифры | Нет shrink при drag | TODO | TODO | Visual/state test | TODO |
| N-011 | Цифры | Screen-size compensation при zoom | TODO | TODO | Visual test | TODO |
| L-001 | Labels | Подписи снаружи контура, кроме диагоналей | TODO | TODO | Screenshot/geometry | TODO |
| L-002 | Labels | Hide letters поведение parity | TODO | TODO | State test | TODO |
| L-003 | Labels | Add angle A, если функция есть | TODO | TODO | Audit + E2E | TODO |
| C-001 | Расчёт | Площадь комнаты | TODO | TODO | Unit/E2E | TODO |
| C-002 | Расчёт | Периметр комнаты | TODO | TODO | Unit/E2E | TODO |
| C-003 | Расчёт | 4×500 => 25 м² | TODO | TODO | Mandatory unit test | TODO |
| C-004 | Расчёт | 4×500 => 20 м | TODO | TODO | Mandatory unit test | TODO |
| C-005 | Расчёт | Формат/округление совпадает | TODO | TODO | Golden vectors | TODO |
| C-006 | Расчёт | Общий итог по комнатам | TODO | TODO | E2E | TODO |
| M-001 | Материалы | Полотно/квадратура | TODO | TODO | Calculation test | TODO |
| M-002 | Материалы | Багет | TODO | TODO | Calculation test | TODO |
| M-003 | Материалы | Вставка | TODO | TODO | Calculation test | TODO |
| M-004 | Материалы | Светильники | TODO | TODO | Calculation test | TODO |
| M-005 | Материалы | Карниз | TODO | TODO | Calculation test | TODO |
| M-006 | Материалы | Углы | TODO | TODO | Calculation test | TODO |
| M-007 | Материалы | Трубы | TODO | TODO | Calculation test | TODO |
| M-008 | Материалы | Световые линии | TODO | TODO | Calculation test | TODO |
| M-009 | Материалы | Теневой профиль | TODO | TODO | Calculation test | TODO |
| M-010 | Материалы | Керамогранит | TODO | TODO | Calculation test | TODO |
| M-011 | Материалы | Custom materials | TODO | TODO | E2E | TODO |
| M-012 | Материалы | Дюбель = 6 шт/м | TODO | TODO | Mandatory unit test | TODO |
| M-013 | Материалы | Текущая комната / все комнаты | TODO | TODO | E2E | TODO |
| M-014 | Материалы | Ручной ввод цен | TODO | TODO | E2E | TODO |
| M-015 | Материалы | Default presets | TODO | TODO | Audit + test | TODO |
| M-016 | Материалы | Selected-only positions | TODO | TODO | E2E | TODO |
| SV-001 | Saved | Автосохранение | TODO | TODO | Relaunch test | TODO |
| SV-002 | Saved | Список saved measurements | TODO | TODO | E2E | TODO |
| SV-003 | Saved | Search name | TODO | TODO | Search test | TODO |
| SV-004 | Saved | Search phone | TODO | TODO | Search test | TODO |
| SV-005 | Saved | Search address | TODO | TODO | Search test | TODO |
| SV-006 | Saved | Recent/all если есть | TODO | TODO | Audit + E2E | TODO |
| SV-007 | Saved | Delete via action menu | TODO | TODO | E2E | TODO |
| SV-008 | Saved | Scroll gesture from card | TODO | TODO | Gesture test | TODO |
| SV-009 | Saved | Bottom nav visible | TODO | TODO | Screenshot | TODO |
| SV-010 | Saved | Last card not covered | TODO | TODO | Screenshot/layout assert | TODO |
| SV-011 | Saved | Open restores full order state | TODO | TODO | Deep state test | TODO |
| B-001 | Backup | Export JSON | TODO | TODO | File test | TODO |
| B-002 | Backup | Import JSON | TODO | TODO | File test | TODO |
| B-003 | Backup | Native accepts web backup | TODO | TODO | Cross-version fixture | TODO |
| B-004 | Backup | Order/rooms/drawings/prices/settings restored | TODO | TODO | Deep compare | TODO |
| B-005 | Backup | Invalid file handled safely | TODO | TODO | Negative test | TODO |
| PDF-001 | PDF | Client estimate screen parity | TODO | TODO | Screenshot diff | TODO |
| PDF-002 | PDF | Только выбранные позиции | TODO | TODO | Content assert | TODO |
| PDF-003 | PDF | Компактная premium-light верстка | TODO | TODO | PDF visual diff | TODO |
| PDF-004 | PDF | Итоги совпадают | TODO | TODO | Content assert | TODO |
| PDF-005 | PDF | Valid `%PDF-` | TODO | TODO | File prefix test | TODO |
| UI-001 | UI | Основная композиция экранов совпадает | TODO | TODO | Screenshot diff | TODO |
| UI-002 | UI | Typography parity | TODO | TODO | Screenshot/token assert | TODO |
| UI-003 | UI | Colors parity | TODO | TODO | Token assert | TODO |
| UI-004 | UI | Spacing/radius/shadows parity | TODO | TODO | Screenshot diff | TODO |
| UI-005 | UI | Expand/collapse parity | TODO | TODO | E2E | TODO |
| UI-006 | UI | Modals/bottom sheets parity | TODO | TODO | Screenshot/E2E | TODO |
| UI-007 | UI | Lower navigation parity | TODO | TODO | Screenshot/E2E | TODO |
| UI-008 | UI | Safe areas on Android | TODO | TODO | Device screenshots | TODO |
| UI-009 | UI | No content under status/nav bars | TODO | TODO | Device screenshots | TODO |
| A-001 | Animation | Open/close durations/easing parity | TODO | TODO | Timing/screen recording | TODO |
| A-002 | Animation | Active-state transitions parity | TODO | TODO | Visual test | TODO |
| SET-001 | Settings | Font family | TODO | TODO | State test | TODO |
| SET-002 | Settings | Font size | TODO | TODO | State test | TODO |
| SET-003 | Settings | Font weight | TODO | TODO | State test | TODO |
| SET-004 | Settings | Night mode | TODO | TODO | Screenshot/state | TODO |
| SET-005 | Settings | Settings persist | TODO | TODO | Relaunch test | TODO |
| I-001 | Icon | Используется `branding/app_icon_master_1024.webp` | TODO | TODO | APK resource check | TODO |
| I-002 | Icon | Adaptive/legacy resources без redesign | TODO | TODO | Launcher screenshot | TODO |
| QA-001 | QA | Cold launch | TODO | TODO | Instrumentation | TODO |
| QA-002 | QA | No crash through full smoke flow | TODO | TODO | Instrumentation | TODO |
| QA-003 | QA | No regression in prior PASS phases | TODO | TODO | Full suite | TODO |
| QA-004 | QA | Все дополнительные web-функции добавлены в matrix | TODO | TODO | Audit diff | TODO |
| QA-005 | QA | Side-by-side screenshots по обязательным states | TODO | TODO | Artifacts | TODO |
| QA-006 | QA | Final native APK не содержит WebView/HTML runtime для UI | TODO | TODO | APK/code inspection | TODO |

## Обязательные Phase Gates

### Phase 0 — Audit only
PASS только если:
- golden master SHA подтверждён;
- заполнены Web reference для всей таблицы;
- добавлены новые строки для всех функций, найденных сверх baseline;
- data/storage/formula/gesture/haptic/visual maps приложены;
- native code не менялся.

### Phase 1+ — Implementation
Каждая фаза может перейти в PASS только если:
- implementation complete;
- tests green;
- screenshot/behavior parity green;
- matrix evidence заполнено;
- все ранее PASS строки остаются PASS.

## Правило завершения

Нельзя писать «нативная версия готова», если существует хотя бы одна строка FAIL или UNVERIFIED среди обязательных функций.
TODO допускается только для ещё не начатой будущей фазы.
