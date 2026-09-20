# Golden-master contract: «Замер потолков»

## Non-negotiable architecture
The file `webapp/index.html` is the behavioral and visual golden master. The Android APK must WRAP this app, not port or rewrite it. Do not rebuild the UI in Compose/XML and do not translate JavaScript business logic to Kotlin/Java.

The SHA-256 of the approved asset is:
`7fade548ce909eb3bea6998c244fd90feb74b060a066d19c6a6c50c02c3f05d8`

Any change to this file requires an explicit, separate product change and a new approved hash. A build must fail if the asset hash differs.

## Functions that must remain
1. Customer/order: create/delete, customer name as order title, phone, address. No order-number feature.
2. Rooms: add, select, long-press menu, rename, duplicate, delete, completed indicator, one drawing per room.
3. Drawing modes: Wall / Points / Diagonal / Numbers.
4. Wall drawing: first wall from blank space; later walls only from existing corners; closed contour blocks new walls and cannot leave an extra tail after closure.
5. Gestures: two-finger pan/zoom; undo/redo; fit view; magnifier/snapping/alignment; no page vertical swipe over the drawing in Wall mode except the dedicated 74 px right strip.
6. Custom wall-size keypad: decimal comma, edit/delete, X close, tap outside closes.
7. Diagonals: automatic calculated value, manual input, edit/delete, X close.
8. Dimension labels: independent drag, 360° rotation, independent size change; pose persists after redraw/zoom/save.
9. Calculation rule already used by the web app, including the project-specific scale where a 500×500 square corresponds to 25 m² and 20 m perimeter. Do not reinterpret displayed units.
10. Purchase/client calculations: canvas, baguette, insert, lights, cornice, corners, pipes, light lines, shadow profile, porcelain, custom materials; dowels 6 pcs/m automatically; current room/all rooms; default price presets; merged orders where present.
11. Client estimate: same layout/content and offline PDF generation.
12. Persistence: current localStorage schema/migrations, automatic saving, saved measurements, search by name/phone/address, recent/all, delete via action menu.
13. Backup: export JSON and restore from file.
14. Saved-measurements window must scroll vertically; bottom navigation remains visible and must not cover the last card.
15. UI settings: current font family/size/weight/night-mode behavior.
16. No external web dependency: the app must work offline after APK installation.

## Android wrapper requirements
- Load the web asset with `WebViewAssetLoader` from `https://appassets.androidplatform.net/assets/index.html`.
- Enable JavaScript and DOM storage. Keep WebView page zoom disabled; the app owns its own pinch/zoom gestures.
- Do not add native touch listeners that alter the web gesture contract.
- Implement `WebChromeClient.onShowFileChooser` so JSON backup restore works.
- Expose exactly `AndroidFileBridge.saveBase64File(name, mime, base64)` and save generated JSON/PDF bytes to Downloads using Android storage APIs. The web app already falls back to ordinary browser Blob downloads outside Android.
- No remote fonts, scripts, CSS, analytics, ads, network APIs, or CDN dependencies.
- Do not alter the web-app CSS, layout, text, geometry, timing constants, formulas, storage key, schema migration, or event handlers while packaging.

## Required regression gates
Before producing the APK:
- run `scripts/verify_webapp.py app/src/main/assets/index.html`;
- run JavaScript syntax checks;
- Android lint + unit tests + debug build;
- instrumentation smoke test on an Android emulator/WebView;
- inspect WebView console and fail on uncaught JavaScript errors;
- only upload the APK artifact if every mandatory gate is green.

## Mandatory smoke scenarios
1. Cold launch with empty storage and relaunch with populated storage.
2. Create customer, fill name/phone/address, restart Activity, verify persistence.
3. Add/rename/duplicate/delete rooms and switch between them.
4. Draw and close a simple rectangle, enter wall sizes, confirm area/perimeter; verify no line can continue past closed contour.
5. Undo/redo and fit view.
6. Create/edit/delete diagonal; use calculated diagonal and manual diagonal.
7. Move/rotate/resize dimension labels, redraw/zoom/relaunch and verify the saved pose.
8. Verify Wall mode blocks vertical page swipe across the drawing and allows it only from the right 74 px strip.
9. Open Saved measurements, swipe starting on cards, search, and verify the fixed bottom nav never covers the last card.
10. Export backup JSON to Downloads, restore it through file chooser, compare restored order/room count and values.
11. Fill purchase/client price data including custom material and automatic dowels; verify totals before/after relaunch.
12. Generate PDF and confirm a non-empty valid `%PDF-` file appears in Downloads.
13. Toggle UI settings/night mode and verify persistence.

No test may be deleted, weakened, skipped, or replaced by a claim that the feature “should work”. If a scenario cannot be automated, report it explicitly as unverified and do not label the build complete.