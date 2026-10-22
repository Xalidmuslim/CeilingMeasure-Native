# CeilingMeasure Native — preserved Medicine Smooth Motion baseline

The tested Android performance baseline is **commit `0af9cd4dec1e45576507793123dfb59100425b77`**, preserved on branch
`golden/ceiling-medicine-smooth-v247-2026-10-23`.

## Mandatory constraints for future UI editing

- **Do not change the optimized startup, image-preload scheduling, project-list caching, animation easing/timing, R8 and shrinking, GPU-layer behavior, back navigation, or editor scroll implementation** without an explicit new request to optimize them.
- For changes to text, colors, cards, icons or layout, modify only the required view/asset. Do not "refactor" the performance files opportunistically.
- Preserve the native Android Java architecture, app ID and customer data. Never point a preview build at the production app data directory.
- If altering the *header logo* matte only, it is acceptable to adjust the isolated background-pixel predicate or matte-removal routine in `NativeHomePreloader`; leave its executor, threading, caching, saved-project loading and bitmap decoding **unchanged**.
- Keep `NativeMotion`, `NativeWorkspaceActivity`, `NativeProjectStore`, `NativeHomeActivity`, `NativePremiumHomeUi` UI structure, Gradle build types/R8 and existing animation resources unchanged for icon-only fixes.
- Run the existing build, unit tests, targeted Android instrumentation tests and R8-optimized APK verification after relevant changes.
- Do not change the `Prophetic-Medicine-Native` reference repository.

**Rollback:** use `golden/ceiling-medicine-smooth-v247-2026-10-23`. Always develop changes on an isolated branch.
