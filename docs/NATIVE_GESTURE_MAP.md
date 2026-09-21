# Phase 0 — Native gesture map

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80

| Gesture/rule | Threshold | Scope | Behavior | Web reference |
|---|---|---|---|---|
| Long press | 330 ms | vertices, edges, diagonals, rooms, saved measurements | LONG_PRESS_MS=330; movement cancels before fire | webapp/index.html:L6060-L6064 |
| Room long-press movement cancel | 22 px | room chips | distance >22 cancels room action timer | webapp/index.html:L7910-L7995 |
| Saved-card long-press movement cancel | 12 px | Saved list | distance >12 converts to scroll/no action | webapp/index.html:L9790-L9820 |
| General pointer movement | 9 px | drawing SVG | distance >9 marks moved and cancels long press | webapp/index.html:L12900-L12940 |
| Pinch start | 2 active pointers | drawing SVG | cancels single interaction; midpoint anchored scale/pan | webapp/index.html:L12680-L12900 |
| Pinch scale | .45…12 | drawing view | scale clamp | webapp/index.html:L13355-L13385 |
| Open Wall scroll | right HTML strip 74 px | wall-draw-mode | SVG owns one finger; strip uses touch-action:pan-y | webapp/index.html:L13980-L14035 |
| Closed-contour Wall scroll | dy >8 px and \|dy\| ≥ .72\|dx\| | closed contour SVG | manual appScroll delta ×1.35; deliberate exception to open-strip rule | webapp/index.html:L12920-L12975 |
| Non-Wall element scroll | dy >11 px and \|dy\| ≥ .78\|dx\| | other modes where elementScrollAllowed | manual fallback; drawing gestures otherwise retained | webapp/index.html:L12975-L13020 |
| Vertex hit | 34 px default; 48 px in move/diag | drawing SVG | threshold divided by view scale | webapp/index.html:L12530-L12580; webapp/index.html:L12680-L12800 |
| Edge hit | 80 px default; 82 px pointer fallback | drawing SVG | nearest segment | webapp/index.html:L12545-L12590; webapp/index.html:L12680-L12800 |
| Wall live corner snap | 22 px | new wall endpoint | nearest open vertex; along-segment projection uses 22/scale | webapp/index.html:L13115-L13145 |
| Wall final endpoint snap | 36 px | pointer up | nearest vertex; degree>=2 target rejected | webapp/index.html:L13260-L13340 |
| Wall guide snap | 26 screen px | red stop guide | snapWallToGuide | webapp/index.html:L12390-L12430 |
| Move 90° candidate | 30 screen px | moving degree-2 vertex | threshold=30/max(.45,scale) | webapp/index.html:L10480-L10540 |
| Exact 90° state | error ≤0.6° | moving degree-2 vertex | green 90 marker + haptic gate | webapp/index.html:L10480-L10525 |
| Wall angle magnet | 45° steps; diff <0.24 rad | new wall | snapAngle | webapp/index.html:L13355-L13385 |
| Grid | 10 / 5 / 2 units | normal / scale≥4 / scale≥8 | roundGrid | webapp/index.html:L13355-L13375 |
| Precision wall grid | 2 / 1 / 0.5 units | normal / scale≥4 / scale≥8 | roundPrecisionGrid | webapp/index.html:L13355-L13375 |
| First-wall minimum gesture | >18/view.scale world units | empty drawing | short tap creates no orphan point | webapp/index.html:L13280-L13355 |
| Magnifier | 180×108; grid 5; base view×2.8 | wall and point-move only | fit scale; wall final clamp 1.1…7.1, move 1.05…6.2 | webapp/index.html:L12430-L12535 |
| Label rotate | 0…360° normalized to 0.1° | selected edge/diag label | rotation ring; direct live transform | webapp/index.html:L11940-L12160; webapp/index.html:L12800-L12900 |
| Label size | .55…2.5; UI step .1 | selected edge/diag label | minus/reset/plus; screen-size counter-scaling | webapp/index.html:L11940-L12160; webapp/index.html:L12720-L12800 |
| Selection/context guard | suppressed except true editable inputs | document | contextmenu/selectstart/dragstart guards | webapp/index.html:L13940-L14110 |

### Critical closure rule
The v184 source has two different Wall scroll contracts:
- **open contour:** one-finger drawing owns the SVG; page scroll entry is the **74 px** right HTML strip;
- **closed contour:** the SVG has class closed-contour-scroll and vertical one-finger movement can manually scroll the page from the drawing itself.

Older parity prose saying the 74 px rule is unchanged after closure is therefore stale and is marked FAIL in the migration matrix.

### Multi-touch invariant
The second pointer cancels long-press/draw/diag/vertex-drag/label transient state before pinch begins. A native pointer-input implementation must reproduce that cancellation order or it can accidentally leave extra walls or edits after a pinch.
