# Native design system

Direction: a quiet, matte field tool. Existing native component bounds and insets are preserved unless a contract requirement demands a targeted change.

| Token | Value |
|---|---|
| Space | 4, 8, 12, 16, 24, 32 dp |
| Type | Android sans; numeric 16–18 sp, body 14–16 sp, captions 12 sp, titles 20–24 sp |
| Text | #1e3144; secondary #708294 |
| Background / surface | #f2f5f8 / #ffffff; matte, no decorative glow |
| Borders | #d8e2ea, 1 dp |
| Accent | #2f8fbe; light fill #edf6fb |
| Success / warning / error | #2f9a68 / #d58a24 / #c84c54 |
| Radius | controls 8–11 dp; cards 13 dp; sheets 16 dp |
| Icons | 20–24 dp within at least 48 dp important touch target |
| Compact room cards | compact visual width with scrolling list; do not shrink text illegibly |
| Elevation | zero on normal cards, subtle modal separation only |
| Motion | overlay 115 ms; collapse 145 ms; no bounce or geometry morphing |

Production tokens: NativeVisualTokens / NativeTypography. Figma file: https://www.figma.com/design/HTlkheIttFM80MmrUa1Q6t. The file is design documentation; native production screens and actual emulator evidence remain authoritative.
