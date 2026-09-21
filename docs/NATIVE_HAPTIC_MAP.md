# Phase 0 — Native haptic map

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80

The complete golden source contains **exactly 2 navigator.vibrate call sites**.

| ID | Trigger | Source call | Duration | Debounce/re-arm rule | Web reference |
|---|---|---|---|---|---|
| H-001 | Exact 90° entry | navigator.vibrate(18) | 18 ms | Only when moveCorner90Info.errorDeg <= 0.6 and point90SnapActive was false; re-arms only after leaving exact state | webapp/index.html:L13080-L13090 |
| H-002 | Wall guide snap entry | navigator.vibrate(18) | 18 ms | Only when red-guide snap becomes active and wallGuideSnapActive was false; re-arms when guide snap is lost | webapp/index.html:L13145-L13155 |

No separate haptic exists in v184 for:
- generic corner-to-corner wall snap;
- diagonal creation or diagonal endpoint hit;
- room/card long press;
- keypad input;
- ordinary 45° wall magnet.

If a native implementation adds vibration to those actions, it is a behavioral deviation unless separately approved.
