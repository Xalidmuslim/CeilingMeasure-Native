# Phase 0 — Native visual tokens

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80

The file contains years of layered CSS overrides. Native parity must use the **effective late cascade**, not an early declaration copied in isolation.

| Token/domain | Golden value | Authority | Web reference |
|---|---|---|---|
| Core background | --bg #f3f6fa; later app background --app-bg #f2f5f8 | CSS root layers | webapp/index.html:L12-L13; webapp/index.html:L328-L338 |
| Core card | --card #ffffff; --app-surface #ffffff | CSS root | webapp/index.html:L328-L338; webapp/index.html:L4037-L4043 |
| Core text | --text #223448; --text-strong #1f3143; --premium-text #1e3144 | CSS root | webapp/index.html:L12-L13; webapp/index.html:L328-L338; webapp/index.html:L2768-L2781 |
| Muted text | --muted #748598; --text-soft #708195; --premium-muted #708294 | CSS root | webapp/index.html:L12-L13; webapp/index.html:L328-L338; webapp/index.html:L2768-L2781 |
| Accent | --blue #2b88bf; --accent #2f91c4; --app-accent #2f95c2; premium #2f8fbe | CSS root | webapp/index.html:L12-L13; webapp/index.html:L328-L338; webapp/index.html:L2768-L2781; webapp/index.html:L4037-L4043 |
| Semantic ok/warn/error | #2f9a68 / #d58a24 / #c84c54; legacy red #cf4747 | CSS root | webapp/index.html:L12-L13; webapp/index.html:L4037-L4043 |
| Card radii | dominant 8–18 px; --radius 18; compact 16; premium 22/16 | CSS layers | webapp/index.html:L328-L338; webapp/index.html:L2768-L2781; webapp/index.html:L3190-L3202 |
| Pill radius | 999 px | chips/tabs/badges | webapp/index.html CSS: multiple selectors |
| Base shadow | 0 8px 26px rgba(25,51,78,.08) | --shadow | webapp/index.html:L12-L13 |
| Card shadow | 0 6px 22px rgba(25,46,68,.06) | --shadow-card | webapp/index.html:L328-L338 |
| Premium shadow | 0 14px 36px rgba(25,49,73,.08) | --premium-shadow | webapp/index.html:L2768-L2781 |
| Typography family | system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif | --ui-font-family / UI_FONT_MAP system | webapp/index.html:L3134-L3137; webapp/index.html:L6092-L6101 |
| Typography default scale | 1.12 (112%) | UI_DEFAULT_PREFS | webapp/index.html:L6092-L6127 |
| Typography scale range | .85…1.30 | slider 85..130 + clamp | webapp/index.html:L5577-L5603; webapp/index.html:L6102-L6105 |
| Typography weights | normal +0; medium +100; bold +200 over measured source | UI_WEIGHT_MAP | webapp/index.html:L6092-L6101 |
| Compact spacing | --app-compact-gap 7 px; common gaps 4–12 px | CSS | webapp/index.html:L3190-L3202 |
| Overlay motion | 115 ms JS close delay | OVERLAY_MOTION_MS | webapp/index.html:L6063-L6086 |
| Collapse motion | 145 ms JS; CSS motion panel 145 ms | COLLAPSE_MOTION_MS / --motion-panel | webapp/index.html:L6652-L6687; webapp/index.html:L3918-L3940 |
| Motion easing | cubic-bezier(.22,.78,.24,1) authoritative late layer | --motion-ease | webapp/index.html:L3918-L3940 |
| Safe area | env(safe-area-inset-top/bottom) in nav, sheets, focus | CSS safe-area rules | webapp/index.html:L133-L150; webapp/index.html:L4090-L4180; webapp/index.html:L5035-L5130 |
| Drawing canvas | viewBox 760×560; magnifier 180×108 | SVG markup/functions | webapp/index.html:L8290-L8355; webapp/index.html:L12390-L12535 |
| Wall scroll strip | 74 px physical HTML overlay on right | late v174 CSS | webapp/index.html:L13980-L14035 |

### Effective-style warnings
- Many late rules deliberately set transition:none!important for common controls while overlays and a few structural elements keep short motion. Do not globally animate every control in native.
- The source contains multiple generations of root tokens. Later app/premium tokens override the visual result in many screens; screenshot parity remains the final authority.
- Font size is user-scalable at runtime and applied to dynamically inserted text through a MutationObserver in web. Native must apply the same setting to newly created composables/screens without requiring restart.
- Night mode is not an effective v184 token/state: applyUiPrefs explicitly removes ui-dark.
