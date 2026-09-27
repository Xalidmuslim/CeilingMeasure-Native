# Native Measurement Contract Implementation Plan

> **For agentic workers:** use superpowers:executing-plans to implement task-by-task, with regression evidence and a fresh whole-branch review. User explicitly requested continuous autonomous execution.

**Goal:** deliver a verified installable native measurement APK satisfying SYSTEM_CONTRACT.md.
**Architecture:** preserve Java Android Views/Canvas, canonical JSON and solver boundaries. Add focused helpers for gesture policy and drawing operation history. Harden storage without an unrequested data migration.
**Tech Stack:** existing Android API29–35, Java17, Gradle8.9, AndroidX Activity, JUnit, AndroidJUnitRunner, PdfDocument, GitHub Actions.
**Spec:** SYSTEM_CONTRACT.md; current 44-section user request.

## Global Constraints
- No WebView, HTML runtime, web implementation, or hybrid shell.
- 500 × 500 square = 25 m² and 20 m; displayed unit мм.
- Preserve stable semantic IDs, measurements, sketch and factual separation, canonical imports.
- Do not rewrite unaffected working modules; no hidden failing or unrun gates.
- Every committed gesture saves once; transient drag state does not persist every frame.

## Review Focus
- Corrupt stored JSON must retain the source and last valid project, not erase all rooms.
- Cancelling or adding a second finger during point/label drag must not commit a partial mutation.
- A wall number must remain tied to its original edge through short edges, sharp wedges and zoom.
- Undo after a material price edit must not undo that unrelated price.
- Long Cyrillic names and many room/material rows must paginate without clipping.

### Task 1: Baseline and contracts
**Files:** SYSTEM_CONTRACT.md, ARCHITECTURE.md, TEST_MATRIX.md, docs rules, DESIGN_SYSTEM.md, CHANGELOG.md; Gradle version catalog.
**Interfaces:** current main graph and storage types retained.
- [ ] Run baseline lint/JVM/assemble; confirm existing main CI independently.
- [ ] Commit accurate contracts and catalog; create branch/PR for fresh CI.

### Task 2: Storage and lifecycle
**Files:** NativeProjectStore.java, NativeWorkspaceActivity.java, NativeProjectStoreTest.java, new lifecycle tests.
**Interfaces:** saveCurrent(ProjectData), loadCurrent(), canonical backup format unchanged; recovery keys are private preferences.
- [ ] Test corrupt-current recovery, corrupt-source preservation, failed canonical write retains last good data; run RED.
- [ ] Add last-known-good storage and explicit error state without replacing valid DB on exception.
- [ ] Test and retain active room/project on Activity recreation separately from canonical export.
- [ ] Run store and existing full JVM suite; commit.

### Task 3: Calculation and geometry acceptance
**Files:** NativeGeometry.java, NativeMeasurementSolver.java, NativeEstimateEngine.java, domain tests, NativeFactualGeometryTest.java.
**Interfaces:** existing Result and Metrics types; no cosmetic rewrite.
- [ ] Add tests for square 25/20, rectangle, L, short/long edges, crossings, missing/edited constraints, multiple diagonals and identity roundtrip.
- [ ] Run tests first and fix only demonstrated violations; retain old solver behavior where compatible.
- [ ] Test insert manual override and 6 dowels/m; run suite and commit.

### Task 4: Placement and drawing history
**Files:** NativeLabelEngine.java, NativeDrawingHistory.java, NativeWorkspaceActivity.java, placement/history tests.
**Interfaces:** layout(RoomData,float,float) returns placements keyed by original IDs; history applies drawing fields to current room while retaining materials.
- [ ] Add RED tests for source-half placement, acute/concave obstacles, font invariance while dragging and diagonal inside/alignment.
- [ ] Implement candidate scoring and occupied bounds; never mutate semantic IDs/values.
- [ ] Add RED tests for operation undo/redo preserving materials, per-room isolation and drawing additions/deletions.
- [ ] Replace UI snapshot stacks with named drawing-operation history; run suite and commit.

### Task 5: Gesture policy and complete journey
**Files:** NativeGestureEngine.java, CeilingDrawingView.java, NativeWorkspaceActivity.java, NativeContractJourneyTest.java.
**Interfaces:** shared long-press/scroll/pinch policy; drawing view retains native event dispatch.
- [ ] Add tests for 4-mode long press, open/closed scroll, pinch centroid, partial-lift suppression, cancel rollback, snapping, closure and existing target editing.
- [ ] Reproduce failures before local corrections; ensure no gesture is disabled to fix another.
- [ ] Execute room/material/PDF/recreation journey through native screens; commit.

### Task 6: Native visual/PDF delivery
**Files:** NativeVisualTokens.java, native dialog/activity layouts, NativeClientEstimatePdf.java, instrumented PDF tests, Figma components, README/TEST_MATRIX/CHANGELOG.
- [ ] Create Figma tokens/components matching native controls; inspect composition.
- [ ] Replace decorative gradients with matte fills without changing graph/solver; keep touch targets/insets.
- [ ] Test Cyrillic/long rows/multipage PDF and render PDF evidence; fix proven issues.
- [ ] Run lint, all JVM, native-only and APK verification, all instrumented suites at API30/API35 and width evidence.
- [ ] Fresh branch review, targeted regression fixes, final exact-head CI; publish artifact and accurate evidence.
