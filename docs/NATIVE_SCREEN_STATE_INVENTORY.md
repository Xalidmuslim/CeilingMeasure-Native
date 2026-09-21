# Phase 0 — Native screen/state inventory

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80
Golden master SHA-256: 354fbede167490d2481d3253507ff2829dcefec495298075487880ea2258a388

Counting convention: 22 user-facing surfaces (2 primary + 20 overlays) plus 24 explicit cross-cutting state dimensions = **46 inventory entries**. Toast and persistent bottom navigation are controls, not separate screens.


## Surfaces

| ID | Type | Surface | Entry | Entry trigger | Exit | Data/state | Web reference |
|---|---|---|---|---|---|---|---|
| S01 | Primary | Home | showHomeScreen; launcher state | Home actions or app start | showWorkspace / open overlays | orders summary + current drawing metadata | webapp/index.html:L5502-L5552; webapp/index.html:L7539-L7595 |
| S02 | Primary | Workspace | showWorkspace + render/renderRoom | Home → New/Draw or Saved → open | showHomeScreen | current order/room + drawing + estimates | webapp/index.html:L5554-L5558; webapp/index.html:L7700-L8425 |
| S03 | Overlay | UI settings | openUiSettings | settings buttons | close/done/outside | uiPrefs | webapp/index.html:L5561-L5622; webapp/index.html:L6092-L6218 |
| S04 | Overlay | Wall editor / sequential wall dock | openWall / startWallSequence | long press wall; after contour closure; tools | save/close/back/outside | edge.mm + wallSequence state | webapp/index.html:L5626-L5644; webapp/index.html:L13440-L13755 |
| S05 | Overlay | Diagonal editor | openDiag | new diagonal or long press diagonal | save/cancel/×/outside | diag.mm + auto value | webapp/index.html:L5647-L5664; webapp/index.html:L13756-L13835 |
| S06 | Overlay | Calculator number pad | openCalcKeypad | price/qty readonly fields | done/×/outside | target numeric value | webapp/index.html:L5666-L5700; webapp/index.html:L10030-L10180 |
| S07 | Overlay | Confirmation dialog | showConfirm | destructive/override actions | cancel/confirm | pending confirmAction | webapp/index.html:L5703-L5712; webapp/index.html:L6088-L6089 |
| S08 | Overlay | Vertex menu | openVertexMenu | long press vertex | close/action | selected vertex; custom A/delete | webapp/index.html:L5715-L5725; webapp/index.html:L12590-L12645 |
| S09 | Overlay | Room action sheet | openRoomActions | room long press | close/rename/duplicate/delete | roomId/name | webapp/index.html:L5728-L5740; webapp/index.html:L7760-L7862 |
| S10 | Overlay | Saved measurements | openSavedMeasurements | Saved/Home actions | close/open item | savedAt orders + query/filter | webapp/index.html:L5743-L5760; webapp/index.html:L9790-L9861 |
| S11 | Overlay | Saved action sheet | openSavedAction | saved-card long press | close/delete | saved order id | webapp/index.html:L5763-L5772; webapp/index.html:L8425-L8470 |
| S12 | Overlay | Purchase price preset | openPricePreset | purchase price action | close/apply | defaultPricing/default custom items | webapp/index.html:L5775-L5793; webapp/index.html:L7008-L7246 |
| S13 | Overlay | Client price preset | openClientPricePreset | client price action | close/apply | defaultClientPricing/meta/custom | webapp/index.html:L5795-L5813; webapp/index.html:L7008-L7246 |
| S14 | Overlay | Room sketch preview | openRoomSketchPreview | estimate room drawing action | close/outside | cloned room geometry | webapp/index.html:L5816-L5829; webapp/index.html:L8720-L8890 |
| S15 | Overlay | Client estimate | openClientEstimate | estimate action | close/outside/PDF | clientEstimateData | webapp/index.html:L5832-L5851; webapp/index.html:L8500-L9420 |
| S16 | Overlay | Internal calculation summary | openInternalCalcSummary | internal summary action | close/outside | room purchase costs | webapp/index.html:L5854-L5882; webapp/index.html:L7296-L7525 |
| S17 | Overlay | Merge orders into estimate | openClientMergeOrders | merge action | cancel/apply/outside | combinedEstimateOrderIds | webapp/index.html:L5885-L5901; webapp/index.html:L8580-L8685 |
| S18 | Overlay | Client material room chooser | openClientMaterialRoomChooser | add material | close/select room | target room | webapp/index.html:L5904-L5914; webapp/index.html:L8890-L9160 |
| S19 | Overlay | Client material editor | openClientMaterialSheet | material row action | close/save | client pricing/qty | webapp/index.html:L5917-L5927; webapp/index.html:L8890-L9300 |
| S20 | Overlay | Other client material | openClientOtherMaterialSheet | add custom | cancel/add/outside | name/unit/qty/price | webapp/index.html:L5930-L5957; webapp/index.html:L9160-L9420 |
| S21 | Overlay | Room picker | openRoomPicker | + room | close/type selection | type: Комната/Коридор/Ванная/Другое | webapp/index.html:L5960-L5973; webapp/index.html:L7863-L7910 |
| S22 | Overlay | Drawing tools menu | openDrawToolsMenu | bottom More | close/action | magnet/90/letters/auto/validation/lock/clear | webapp/index.html:L5976-L5984; webapp/index.html:L9885-L10035 |


## State dimensions

| ID | State | Values | Authority | Web reference |
|---|---|---|---|---|
| ST01 | Navigation state | home vs workspace | body.app-home / showHomeScreen / showWorkspace | webapp/index.html:L7539-L7595 |
| ST02 | Order collapse | expanded/collapsed | db.uiCollapse.order | webapp/index.html:L6647-L6780; webapp/index.html:L7700-L7760 |
| ST03 | Rooms collapse | expanded/collapsed | db.uiCollapse.rooms | webapp/index.html:L6647-L6780; webapp/index.html:L7700-L7760 |
| ST04 | Drawing collapse | expanded/collapsed | db.uiCollapse.drawing | webapp/index.html:L6647-L6780; webapp/index.html:L8290-L8425 |
| ST05 | Estimate collapse | expanded/collapsed | db.uiCollapse.estimate | webapp/index.html:L6647-L6780; webapp/index.html:L8290-L8425 |
| ST06 | Drawing mode | draw / move / diag / labels | mode; default draw | webapp/index.html:L6048-L6061; webapp/index.html:L8290-L8355 |
| ST07 | Contour topology | empty / open / closed | cycleOrder(r) | webapp/index.html:L10200-L10470; webapp/index.html:L12680-L13425 |
| ST08 | Geometry lock | unlocked / locked | room.geometryLocked | webapp/index.html:L6945-L7007; webapp/index.html:L9885-L10035 |
| ST09 | Wall angle magnet | off / on | global magnet | webapp/index.html:L6048-L6061; webapp/index.html:L9885-L10035 |
| ST10 | Point 90° magnet | off / on | room.point90Magnet | webapp/index.html:L6457-L6545; webapp/index.html:L9885-L10035 |
| ST11 | Auto letters | hidden / shown | room.showAutoLetters; default false | webapp/index.html:L6457-L6545; webapp/index.html:L9885-L10035 |
| ST12 | Drawing hint | collapsed / expanded | room.hintCollapsed | webapp/index.html:L6457-L6545; webapp/index.html:L8290-L8425 |
| ST13 | Drawing focus | normal / focus | drawingFocusMode | webapp/index.html:L6048-L6061; webapp/index.html:L7595-L7690 |
| ST14 | Wall sequence | off / sequential entry | wallSequenceMode | webapp/index.html:L6048-L6061; webapp/index.html:L13440-L13755 |
| ST15 | Wall editor | closed / one edge active | wallEditId | webapp/index.html:L6048-L6061; webapp/index.html:L13440-L13755 |
| ST16 | Diagonal editor | closed / one diag active | diagEditId | webapp/index.html:L6048-L6061; webapp/index.html:L13756-L13835 |
| ST17 | Label editing | none / edge / diagonal selected | selectedLabel + drag/rotate | webapp/index.html:L11940-L12160; webapp/index.html:L12680-L13360 |
| ST18 | Pointer transform | single / pinch-two-pointer | pointers + pinch + multiTouch | webapp/index.html:L12680-L13360 |
| ST19 | Vertex dragging | inactive / active | dragVertexId | webapp/index.html:L12680-L13360 |
| ST20 | Saved filter | recent / all / search | savedFilterMode + query | webapp/index.html:L9790-L9861 |
| ST21 | Internal purchase | hidden / open | internalPurchaseOpen | webapp/index.html:L8000-L8425 |
| ST22 | Price manage mode | purchase/client manage off/on | purchasePriceManageMode/clientPriceManageMode | webapp/index.html:L7000-L7246 |
| ST23 | Validation | ok / warn / error | roomValidation() | webapp/index.html:L6910-L7007 |
| ST24 | Completion | working / saved | order.savedAt + room.savedAt | webapp/index.html:L9790-L9895 |


### Navigation invariants
- Home and workspace are distinct source states; Home does not replace workspace data.
- Bottom history navigation is kept visible across overlays; Android back is routed through ceilingHandleAndroidBack.
- Wall-sequence browser-back is special: Back goes to the previous wall before closing the sequence.
- Saved measurements contain only orders with savedAt; auto-saved working orders are not automatically shown there until explicit completion.

### Source discrepancies captured
- Closed contour disables the quick Wall button; this contradicts the older parity sentence that Wall remains re-enterable after closure.
- Closed-contour Wall mode has a deliberate manual one-finger vertical scroll path over the SVG, unlike the open-contour 74 px strip rule.
- Settings UI has font, size and weight only; night mode is not user-facing in v184.
