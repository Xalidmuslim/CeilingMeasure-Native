# Phase 0 — Native data/storage schema

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80
Golden SHA-256: 354fbede167490d2481d3253507ff2829dcefec495298075487880ea2258a388

Persistence is split between:
1. application DB key **ceilingMeasureData** (schemaVersion **165**); and
2. UI preference key **ceilingMeasureUiPrefsV1** (UI prefs version **2**).

Counting convention: fixed, source-defined persisted path slots are counted once by schema type; dynamic array lengths and custom material instances are not multiplied. With that convention the golden source defines **173 canonical persisted field slots**.


## Canonical persisted schema

| Group | Field slots | Paths |
|---|---|---|
| Root DB | 15 | db.schemaVersion, db.orders, db.defaultPricing, db.defaultClientPricing, db.hiddenDefaultPurchaseKeys, db.hiddenDefaultClientKeys, db.defaultClientStandardMeta, db.defaultPurchaseCustomItems, db.defaultClientCustomItems, db.uiCollapse, db.internalAllRoomsMigratedV157, db.defaultClientPricingMigratedV146, db.hintsCollapsedMigratedV73, db.otherRoomNamesMigratedV66, db.autoLettersMigratedV64 |
| Default purchase pricing | 9 | db.defaultPricing.canvas, db.defaultPricing.baguette, db.defaultPricing.insert, db.defaultPricing.light, db.defaultPricing.corner, db.defaultPricing.cornice, db.defaultPricing.dowel, db.defaultPricing.lightline, db.defaultPricing.pipe |
| Default client pricing | 9 | db.defaultClientPricing.canvas, db.defaultClientPricing.insert, db.defaultClientPricing.light, db.defaultClientPricing.cornice, db.defaultClientPricing.corner, db.defaultClientPricing.pipe, db.defaultClientPricing.lightline, db.defaultClientPricing.shadow, db.defaultClientPricing.porcelain |
| Default client metadata | 18 | db.defaultClientStandardMeta.canvas.name, db.defaultClientStandardMeta.canvas.unit, db.defaultClientStandardMeta.insert.name, db.defaultClientStandardMeta.insert.unit, db.defaultClientStandardMeta.light.name, db.defaultClientStandardMeta.light.unit, db.defaultClientStandardMeta.cornice.name, db.defaultClientStandardMeta.cornice.unit, db.defaultClientStandardMeta.corner.name, db.defaultClientStandardMeta.corner.unit, db.defaultClientStandardMeta.pipe.name, db.defaultClientStandardMeta.pipe.unit, db.defaultClientStandardMeta.lightline.name, db.defaultClientStandardMeta.lightline.unit, db.defaultClientStandardMeta.shadow.name, db.defaultClientStandardMeta.shadow.unit, db.defaultClientStandardMeta.porcelain.name, db.defaultClientStandardMeta.porcelain.unit |
| Order | 10 | order.id, order.name, order.phone, order.address, order.status, order.pricing, order.pricingMigratedV59, order.rooms, order.savedAt, order.combinedEstimateOrderIds |
| Legacy order pricing map | 3 | order.pricing.canvas, order.pricing.baguette, order.pricing.insert |
| Room | 22 | room.id, room.name, room.points, room.edges, room.diagList, room.notes, room.pricing, room.clientPricing, room.clientEstimateQty, room.clientEstimateOverride, room.clientCustomItems, room.customItems, room.hiddenInternalKeys, room.extraQty, room.geometryLocked, room.validationExpanded, room.extrasExpanded, room.showAutoLetters, room.point90Magnet, room.hintCollapsed, room.view, room.savedAt |
| Room purchase pricing | 9 | room.pricing.canvas, room.pricing.baguette, room.pricing.insert, room.pricing.light, room.pricing.corner, room.pricing.cornice, room.pricing.dowel, room.pricing.lightline, room.pricing.pipe |
| Room client pricing | 11 | room.clientPricing.baguette, room.clientPricing.dowel, room.clientPricing.canvas, room.clientPricing.insert, room.clientPricing.light, room.clientPricing.cornice, room.clientPricing.corner, room.clientPricing.pipe, room.clientPricing.lightline, room.clientPricing.shadow, room.clientPricing.porcelain |
| Client estimate qty | 8 | room.clientEstimateQty.insert, room.clientEstimateQty.light, room.clientEstimateQty.cornice, room.clientEstimateQty.corner, room.clientEstimateQty.pipe, room.clientEstimateQty.lightline, room.clientEstimateQty.shadow, room.clientEstimateQty.porcelain |
| Client estimate override | 8 | room.clientEstimateOverride.insert, room.clientEstimateOverride.light, room.clientEstimateOverride.cornice, room.clientEstimateOverride.corner, room.clientEstimateOverride.pipe, room.clientEstimateOverride.lightline, room.clientEstimateOverride.shadow, room.clientEstimateOverride.porcelain |
| Extra qty | 5 | room.extraQty.light, room.extraQty.corner, room.extraQty.cornice, room.extraQty.lightline, room.extraQty.pipe |
| View | 3 | room.view.scale, room.view.panX, room.view.panY |
| Point | 4 | point.id, point.x, point.y, point.customLabel |
| Edge | 5 | edge.id, edge.a, edge.b, edge.mm, edge.labelPose |
| Diagonal | 5 | diag.id, diag.a, diag.b, diag.mm, diag.labelPose |
| Label pose type | 5 | labelPose.manual, labelPose.x, labelPose.y, labelPose.angle, labelPose.size |
| Room purchase custom item | 6 | purchaseCustom.id, purchaseCustom.sourceId, purchaseCustom.name, purchaseCustom.unit, purchaseCustom.qty, purchaseCustom.price |
| Room client custom item | 6 | clientCustom.id, clientCustom.sourcePresetId, clientCustom.name, clientCustom.unit, clientCustom.qty, clientCustom.price |
| Default custom item type | 4 | defaultCustom.id, defaultCustom.name, defaultCustom.unit, defaultCustom.price |
| Separate UI prefs storage | 4 | uiPrefs.version, uiPrefs.font, uiPrefs.scale, uiPrefs.weight |
| Known uiCollapse keys | 4 | db.uiCollapse.order, db.uiCollapse.rooms, db.uiCollapse.drawing, db.uiCollapse.estimate |


## Migrations

| Migration | Behavior | Web reference |
|---|---|---|
| Legacy key selection | ceilingMeasureData → ceilingTouchV125…V15 → v14/v13 → v12/v11/v10/v9/v8/v6/v5 | webapp/index.html:L6006-L6057 |
| Legacy cm → project units | v14/v13 wall/diag numeric value ×10; older mm set not multiplied | webapp/index.html:L6625-L6646 |
| Legacy drawings | active legacy drawing copied to room root when root empty; drawings/activeDrawingId deleted | webapp/index.html:L6436-L6456 |
| Legacy geometry representation | points array + walls/diags/closed converted to ID-based points/edges/diagList | webapp/index.html:L6483-L6545 |
| Pricing v59 | order-level canvas/baguette/insert copied into every room then room pricing becomes authoritative | webapp/index.html:L6787-L6865 |
| Auto letters v64 | showAutoLetters=false once | webapp/index.html:L6613-L6624 |
| Room names v66 | Другая → Другое including numbered suffix | webapp/index.html:L6595-L6612 |
| Hints v73 | hintCollapsed=true once | webapp/index.html:L6583-L6594 |
| Objects v98 | objects/objectAutoQty/lightAutoLayout removed; manual calculator quantities retained | webapp/index.html:L6483-L6545 |
| Default client pricing v146 | seed first legacy room clientPricing then persist root defaults | webapp/index.html:L6558-L6582 |
| Internal expanded v157 | extrasExpanded=true across rooms once | webapp/index.html:L6546-L6557 |
| UI prefs v182 behavior | UI prefs version 2; old 100% default migrates to 112%; georgia/arial aliases map | webapp/index.html:L6092-L6127 |


### Serialization rules
- persistDbRaw removes legacy order.number and order.savedName before every DB write, writes db.schemaVersion=165 and serializes the complete db object.
- localStorage failure does not throw into UI: storage switches to an in-memory Map. That fallback is **not durable across process/page closure**.
- current orderId and roomId are runtime variables, not persisted fields. Relaunch selects db.orders[0] and that order's first room.
- global drawing mode and global wall-angle magnet are runtime-only; room.point90Magnet, labelPose, view and room flags are persisted.
- undo/redo, active vertex, active modal/edit IDs, pointer state, wall-sequence state and selected label are transient.

### Compatibility constraints
- Backup import accepts either {db: ...} or a raw DB object and only requires an orders array before replacing current data.
- Native storage may use Room/DataStore/files internally, but export/import and migration semantics must preserve this schema contract until an explicit post-parity migration is separately approved.
