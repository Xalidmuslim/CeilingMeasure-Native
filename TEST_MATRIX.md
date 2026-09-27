# Acceptance test matrix

Initial evidence: main 6411551c1d73c37c81da1e5cab85fa619b923f3e; Android APK workflow 36314763707 succeeded. This is baseline evidence only, not proof of the new contract. Statuses below are intentionally pending until fresh execution.

| Area | Required assertions | Test/evidence | Current status |
|---|---|---|---|
| Native boundary | Native Activities/Canvas, no WebView/HTML/INTERNET | CI native source and APK gate | baseline pass |
| Scale | 500 square = 25 m² / 20 m | NativeAppSmokeTest + new domain regression | pending |
| Shapes | rectangle, L, concave/complex, short/long walls, invalid crossing | geometry/solver regression suite | pending |
| Constraints | one/multiple diagonals, edited length, underdetermined/conflicting data | NativeFactualGeometryTest + regressions | pending |
| Sketch/factual | switching preserves original positions and measurements | factual geometry tests | pending |
| Numbers | stable wall ID, distinct values, source-half/free space, no swaps | NativeLabelEngine regression suite | pending |
| Manual labels | drag/zoom/rotation preserve size and identity | pose tests + gesture smoke | pending |
| Wall gestures | first line, endpoint-only, one wall, no extra corner, auto close | NativeAppSmokeTest + gesture acceptance | pending |
| Gesture priority | long press in 4 modes, cancel, pinch/pan, no single zoom | gesture policy + instrumented tests | pending |
| Scroll | open Wall capture, right strip, closed empty scroll | gesture acceptance | pending |
| Precision | grid loupe, X/Y snap, 90°, bounded haptic | Canvas smoke + code/visual evidence | pending |
| Keypad | 0 below 8, replace initial 0, ×/outside/back, no IME | native dialog tests | pending |
| Rooms | open, rename/delete confirmation, seven compact cards, isolation | room unit tests + Phase3 tests | pending |
| Persistence | roundtrip, process recreation, failed read/write recovery | store unit tests + lifecycle acceptance | pending |
| History | wall/point/dimension/diagonal/label undo/redo, materials retained | drawing history regression suite | pending |
| Clear | confirmed, only active room | user-journey acceptance | pending |
| Materials | automatic insert, override, 6 dowels/m, custom prices | estimate unit tests + acceptance | pending |
| PDF | Cyrillic, long names, many rooms/lines, pagination, no empty categories | PdfDocument acceptance + rendered artifact | pending |
| Insets/navigation | overlay-first back; widths 360/393/412 dp, API35 | Phase2/Phase3 + acceptance | pending |
| Full journey | create, draw, sizes, solve, pinch, edit, rooms, estimate, PDF, reload | NativeContractJourneyTest | pending |
| Quality gates | lint, unit suite, assembly, APK signature, instrumentation | exact-head CI | pending |

Physical-device ergonomics and real vibration intensity cannot be inferred from emulator assertions; they must be distinguished from automated coverage in delivery notes.
