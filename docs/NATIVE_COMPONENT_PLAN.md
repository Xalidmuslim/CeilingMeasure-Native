# Phase 0 — Native component plan (planning only)

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80

**No implementation is authorized by this document. Phase 1 has not started.**

| Native component | Planned responsibility | Source authority | Acceptance strategy |
|---|---|---|---|
| App navigation shell (Kotlin + Compose) | Home/workspace, bottom history navigation, modal routing, Android Back | NATIVE_SCREEN_STATE_INVENTORY.md | state-machine tests + screenshots |
| OrderRepository | orders, rooms, completion state, migration-compatible persistence | NATIVE_DATA_SCHEMA.md | import golden backup + deep compare |
| UiPrefsRepository | font family/scale/weight + collapse state | NATIVE_DATA_SCHEMA.md / NATIVE_VISUAL_TOKENS.md | relaunch tests |
| DrawingState | points, edges, diagonals, modes, selection, view | schema + gesture map | deterministic state tests |
| CeilingDrawingSurface (custom native Canvas/pointer layer) | exact plan rendering, hit testing, magnifier, labels, guides, pan/zoom | NATIVE_GESTURE_MAP.md / visual tokens | gesture vectors + pixel screenshots |
| GeometryEngine | cycle detection, repair, free/orthogonal solver, auto diagonals | NATIVE_FORMULA_MAP.md | golden numeric vectors |
| MeasurementInput | wall/diag custom pads and sequential wall flow | screen inventory + gesture map | E2E input sequence |
| HapticController | exactly the audited haptic gates | NATIVE_HAPTIC_MAP.md | fake-vibrator event assertions |
| EstimateEngine | room/order quantities, purchase/client prices, custom items, combined orders | formula/data maps | unit tests with fixtures |
| Estimate UI (Compose) | client estimate + internal calculation + presets/material editors | screen/visual maps | screenshot + E2E |
| BackupCodec | JSON export/import compatible with web schemaVersion 165 | NATIVE_FILE_IO_MAP.md | cross-version fixtures |
| NativePdfExporter | premium-light PDF with selected positive-price lines | file I/O + visual tokens | PDF prefix/content + rendered-page diff |
| ValidationEngine | errors/warnings and geometry lock gate | source roomValidation | test matrix for open/missing/invalid/valid |
| MigrationAdapters | v14/v13 cm, old mm, drawings, pricing v59, UI prefs, later flags | data schema | fixture-by-version migration tests |

### Architecture constraints
- Final UI must be fully native Android. Existing Android Views/Java components may be retained and refined when they can achieve verified parity; Jetpack Compose is optional, not a requirement. Do not rewrite a working native screen merely to change toolkit. The drawing plan remains a dedicated native drawing layer.
- No WebView, HTML, JavaScript engine or web asset as production UI/runtime.
- The golden HTML remains test/reference material only.
- Native references in NATIVE_MIGRATION_MATRIX.md remain TODO during Phase 0 even though the repository already contains Java native classes; those classes are not accepted as parity evidence until a later phase verifies them against the golden maps.
- Preserve backup compatibility before any storage-schema cleanup.
- Do not “improve” thresholds, formulas, copy, layout, motion or haptics during parity migration.


### Toolkit decision after Phase 1

Exact parity and regression control have priority over a framework rewrite. The repository already contains a fully native Android Views/Java shell and drawing surface. For Phase 2+, retain and correct those components when practical. A Compose migration is allowed only if it solves a verified parity blocker and is separately justified by tests/screenshots. WebView/HTML/JS remains forbidden as production UI/runtime.
