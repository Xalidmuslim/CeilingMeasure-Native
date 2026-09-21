# Phase 0 — Native migration risks

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80

| ID | Risk | Why it matters | Source | Mitigation / future test |
|---|---|---|---|---|
| RISK-001 | Closed Wall-mode documentation drift | Older parity prose says Wall remains available after closure; v184 quick Wall button is disabled when cycleOrder(r). | webapp/index.html:L8290-L8355; webapp/index.html:L9885-L10035 | Treat v184 as authority; matrix W-008=FAIL; Phase1 test close→switch mode→attempt Wall. |
| RISK-002 | Closure scroll contract drift | Older docs say 74px rule remains; v184 intentionally allows manual vertical scroll on closed-contour SVG. | webapp/index.html:L12680-L13020; webapp/index.html:L13980-L14163 | Implement two-state scroll contract; matrix G-006=FAIL; boundary tests before/after closure. |
| RISK-003 | Night mode stale requirement | Comment/spec mentions night mode; settings has no night control and applyUiPrefs removes ui-dark. | webapp/index.html:L5561-L5622; webapp/index.html:L6201-L6210 | Do not invent native night mode for parity; SET-004=FAIL until product decision. |
| RISK-004 | Active order not persisted | orderId initializes from db.orders[0]; selecting another order is not stored. | webapp/index.html:L6045-L6059; webapp/index.html:L7690-L7720 | Matrix O-008=FAIL; decide only after parity whether to change behavior. |
| RISK-005 | Active room not persisted | roomId initializes to first room of first order; room selection runtime-only. | webapp/index.html:L6045-L6059; webapp/index.html:L7900-L8000 | Matrix R-009=FAIL; preserve golden now. |
| RISK-006 | Tolerance comment is stale | Comment mentions 5/0.05m while executable FIELD_MEASURE_TOL is 10/0.10m. | webapp/index.html:L6000-L6006; webapp/index.html:L11100-L11710 | Port executable constant 10; numeric tests at 10 and >10. |
| RISK-007 | Layered CSS override complexity | Multiple historical CSS generations and many !important late overrides can make early-token copying wrong. | webapp/index.html:L1-L5500 | Use effective-token map + required-state screenshots, not stylesheet-first translation. |
| RISK-008 | Volatile storage fallback | When localStorage fails, web silently uses in-memory Map and warns; data then dies on close. | webapp/index.html:L6011-L6022; webapp/index.html:L13950-L13970 | Native persistent store should normally avoid this; test and surface storage failures without changing backup schema. |
| RISK-009 | Weak backup validation | Import only checks that orders is an array before replacing current DB. | webapp/index.html:L8495-L8520 | Keep compatibility but isolate parser; negative fixtures; do not partially mutate on parse failure. |
| RISK-010 | Raster PDF semantics | Web PDF is image-based and depends on canvas metrics/Arial/JPEG slicing. | webapp/index.html:L9420-L9790 | Validate rendered page appearance/content, not JavaScript object layout. |
| RISK-011 | Text metrics affect label placement | Wall labels use estimated font size/box and collision heuristics tied to view scale. | webapp/index.html:L12160-L12390 | Golden screenshot fixtures for short/adjacent walls and zoom levels. |
| RISK-012 | Pointer cancellation order | Two-pointer pinch clears single-gesture transient state before transform. | webapp/index.html:L12680-L12900 | Compose pointer tests must assert no residual wall/drag after second finger. |
| RISK-013 | Haptic overimplementation risk | Only two 18ms vibration call sites exist; broad 'snap haptic' wording can lead to extra vibrations. | webapp/index.html:L13080-L13155 | Implement exact event gates only; fake-vibrator event count tests. |
| RISK-014 | Legacy Android bridge confusion | saveBlobFile still contains AndroidFileBridge fallback although final native architecture forbids web runtime. | webapp/index.html:L8470-L8495 | Use native file APIs in future; bridge is compatibility evidence only. |
| RISK-015 | Repository architecture drift | Command text still refers to a current wrapper, but main already launches NativeHomeActivity/NativeWorkspaceActivity and CI rejects WebView. | app/src/main/AndroidManifest.xml; app/build.gradle; .github/workflows/android-apk.yml | Treat existing native code as unverified Phase1 candidate, not proof of parity; do not rewrite during Phase0. |
| RISK-016 | Locale/date differences | Money, qty and dates use ru-RU browser formatting and local Date behavior. | webapp/index.html:L6898-L6910; webapp/index.html:L8510-L8525; webapp/index.html:L9850-L9875 | Pin Russian formatting and timezone-sensitive fixture expectations in native tests. |
| RISK-017 | Solver floating-point sensitivity | Free geometry uses iterative numeric search and accepted measurement band; implementation-order changes can alter near-boundary result. | webapp/index.html:L11000-L11780 | Build golden vectors including boundary residuals before replacing solver. |

### Audit conclusion
No source-level question remains unanswered for Phase 0. Device-specific visual and native implementation parity are intentionally deferred; that is verification work, not missing source information.
