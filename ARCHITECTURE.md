# Architecture

Baseline: main 6411551c1d73c37c81da1e5cab85fa619b923f3e. Existing native app: Java, Android Views, custom Canvas, AndroidX Activity; min API 29, target/compile API 35, JDK 17, AGP 8.7.0, Gradle 8.9. Keep the native foundation and the v184-compatible storage facade. Kotlin/Compose/Room are preferences for a new foundation, not a reason to replace this implementation.

| System | Owner | Boundary |
|---|---|---|
| UI/navigation | NativeHomeActivity, NativeWorkspaceActivity, native dialogs | Display and user actions; no calculation formulas |
| Drawing | CeilingDrawingView | Transient render/gesture drafts and Canvas; commit at gesture end |
| Geometry | NativeGeometry | Graph topology, sketch snapshot and factual mode metadata |
| Solver | NativeMeasurementSolver, NativeMeasurementValidator | Measured constraints and explicit failure results |
| Calculation | NativeEstimateEngine | Room metrics, project metrics, quantities and totals |
| Number placement | NativeLabelEngine | Stable semantic ID in, visual placement out |
| Rooms | NativeOrderRoomLogic, NativeProjectStore | Stable IDs and canonical round trips |
| Persistence | NativeProjectStore, NativeDataContract | Canonical JSON in private Android preferences; compatible backup/import |
| PDF | NativeClientEstimatePdf | Android PdfDocument, native font rendering, pagination |
| Settings/design | NativeUiPrefs, NativeTypography, NativeVisualTokens | Display preferences separate from measurement data |

Changes in this work add focused gesture/history policy helpers and storage recovery rather than a second domain model. The old web files remain reference fixtures; the Android source set does not include them.

## State and lifecycle
Project/room models are persistent. Pointer drafts, haptic transitions and label selection are transient. Viewport state persists only at gesture completion. Room identity and active selection survive Activity recreation without introducing active IDs into canonical exported v184 JSON. Storage recovery maintains a last-known-good copy and preserves corrupt source bytes for diagnosis/import recovery.

## Verification boundary
JVM tests cover canonical data, identity, quantity and placement logic. Instrumented tests execute Canvas MotionEvents, native dialogs, Android lifecycle/insets and PdfDocument. GitHub Actions provides KVM emulators; local builds provide rapid compiler/lint/JVM feedback. Figma documents native visual components and is not a runtime dependency.
