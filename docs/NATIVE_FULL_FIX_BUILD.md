# Native full-app fix build

Base: 0d8e7a7d425dbc5be23bd72991d2705ee7728480

Scope:
- native factual geometry preserves the overall sketch shape while factual wall sizes remain authoritative;
- calculated diagonals are never treated as factual measurements;
- closed-contour canvas hands vertical drag/fling to the native ancestor ScrollView;
- the legacy 74dp manual scroll strip is removed after contour closure;
- compact top actions and estimate rows are constrained to remain inside their cards at larger text sizes.

This branch is isolated from main.
