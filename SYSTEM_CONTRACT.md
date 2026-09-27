# System contract — native ceiling measurement

Status: binding product requirements, 2026-09-27. This document records the user's current instructions; it does not claim every acceptance test has passed. See TEST_MATRIX.md for evidence.

## Change authority
Only an explicit user instruction may change this contract. Preserve first, modify second. Every change identifies its module, adds a regression test for a defect, and runs the critical suite. UI work must not rewrite Geometry/Solver/Calculation, identity, or persistence. Older phase documents are historical where they conflict with this contract. The v184 web file stays an immutable reference, never an APK runtime asset.

## Native boundary and architecture
Android-only UI and Canvas; no WebView, HTML runtime, hybrid shell, network dependency, or JavaScript drawing. Preserve the existing Java Android Views implementation and canonical schema instead of replacing working modules to adopt a preferred language/framework. Separate drawing, measurement solver, gesture policy, calculation, label placement, rooms, estimates, persistence, PDF and settings. Native Android back closes overlays first, then nested screens.

## Identity and storage
Stable string IDs identify project, room, vertex, wall, diagonal and custom material; array positions are never identity. A wall dimension belongs to its wall ID for its lifetime. Automatic placement changes presentation only. Each room owns geometry, dimensions, view state and materials. Every completed model operation is saved. Drag previews do not write storage every frame. Restart, recreation, rotation, room switching and PDF navigation must preserve data. A parse/write failure must never silently overwrite valid data with an empty database. Preserve canonical imports, exports and unknown compatible fields.

## Drawing and gestures
Initial mode when opening a room: Стена. First gesture seeds an empty contour; subsequent walls start at an existing open endpoint. One gesture adds one wall; straight continuation does not add a false corner. Snapping to the first point closes a valid contour automatically. Closed contours prohibit ordinary wall additions but allow mode switching and editing. Reject degenerate and self-intersecting contours for area calculation.

Priority: two-finger zoom/pan > object long press or object drag > empty-space page scroll. Open Wall mode owns single-finger canvas gestures, with a separate right scroll strip. After closure a vertical drag in empty canvas can scroll. One finger never zooms. Pinch is centered on the gesture centroid, scale clamped to 0.45–12, and changes only the viewport. After one finger lifts from a pinch, drawing stays suppressed until all fingers lift. Long press uses a shared policy in all four modes, edits wall/dimension or edits/deletes a diagonal; a short tap never deletes. Long press room offers rename/delete, with deletion confirmation.

## Measurement and calculation
The UI says мм. Intentional business scale: 100 entered units = 1 metre; raw square units / 10000 = square metres. Four walls of 500 in a square MUST give 25 m² and 20 m. Do not correct the displayed unit convention. Calculation stays outside UI. Sum room results into project results; missing measurements must be reported, not invented.

Sketch geometry and factual geometry are distinct. Preserve the sketch when solving or switching modes. Measured diagonals constrain the solver; automatically derived diagonals are not independent evidence. Insufficient or conflicting measurements produce an actionable message and no destructive application. Measurements and wall IDs survive solving and undo.

## Labels and precision
Wall numbers and vertex letters go outside; diagonal numbers go inside along their diagonal, without a frame. Hide letters hides letters in every label but leaves dimensions. Add angle А assigns only А. Automatic placement evaluates free space and obstacles, avoids narrowing wedges, prefers the wall's source half, never changes wallId or swaps values, and avoids overlapping labels/walls/diagonals. Manual drag, rotation and font sizing persist; dragging alone never shrinks a number.

Compact magnifier with grid/crosshair in Wall, Points and Diagonal. X/Y guides are red in every direction; snap and 90° transitions give one short haptic, not continuous vibration. Custom measurement keypad starts at 0, has 0 below 8, backspace, confirm, × and outside dismissal. It must leave the selected object visible and suppress the Android keyboard.

## Rooms, history and destructive actions
Horizontal compact room cards; room switching never mixes drawings. Undo/redo represents drawing operations, including walls, points, measurements, diagonals and label poses; unrelated material changes are not reverted. Clear requires confirmation and affects the current room only. Deleting a room requires confirmation. Bottom navigation respects system insets and never covers content.

## Materials, estimate and PDF
Per-room materials and project totals: canvas, insert, lights, cornice, corners, pipes, light lines, shadow profile, porcelain and custom items. Insert is automatically derived from room perimeter with initial price 0 ₽, with a persistent manual override. Dowels default to ceil(6 × applicable perimeter/profile metres). Automatic and manual quantities are distinguishable; long press edits quantity. Extras collapse. Prices are manual.

Estimate shows project title once, then room names and selected lines, quantity, price and total. Native offline PDF contains only used/selected positions, supports Cyrillic, wrapping and multiple pages, light colors and thin rules. Export downloads a PDF, without forcing sharing.

## UX, verification and delivery
Matte neutral surfaces, graphite text, one quiet accent, fine borders, modest radii, no decorative gradients/glass/heavy shadows. Readable text and important touch targets at least 48 dp. Functional animations 115–145 ms; geometry does not animate. Canvas receives the main working area.

Mandatory gates: lint, JVM tests, native-only checks, APK assembly/verification, Android instrumented tests and complete user journey. Report exact commit and CI evidence. A failing or unrun gate is not a pass. A mockup, TODO, source-only package or previously built APK is not the requested completed product.
