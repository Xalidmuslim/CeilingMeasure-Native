# Phase 0 — Web function and event map

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80
Golden master: webapp/index.html, 14163 lines / 527995 UTF-8 bytes.

Inventory contains **346 named function declarations**, **200 explicit event-binding lines** and **41 timer/animation scheduling lines**. Inline anonymous callbacks are represented by their binding line so they are not silently omitted.


## Named functions

| Line | Domain | Function |
|---|---|---|
| 6014 | storage/settings | storageGet |
| 6018 | storage/settings | storageSet |
| 6023 | storage/settings | persistDbRaw |
| 6064 | storage/settings | smoothOpenOverlay |
| 6072 | storage/settings | smoothCloseOverlay |
| 6088 | storage/settings | showConfirm |
| 6089 | storage/settings | hideConfirm |
| 6090 | storage/settings | showToast |
| 6102 | storage/settings | clampUiScale |
| 6130 | storage/settings | uiTextElements |
| 6136 | storage/settings | captureUiTypography |
| 6164 | storage/settings | applyUiTypography |
| 6182 | storage/settings | refreshUiSettingsControls |
| 6201 | storage/settings | saveUiPrefs |
| 6202 | storage/settings | applyUiPrefs |
| 6210 | storage/settings | setUiPref |
| 6211 | storage/settings | setUiScaleFromSlider |
| 6215 | storage/settings | openUiSettings |
| 6216 | storage/settings | closeUiSettings |
| 6217 | storage/settings | resetUiSettings |
| 6218 | storage/settings | startUiTypographyObserver |
| 6259 | schema/migrations/collapse | ensureHiddenDefaultPurchaseKeys |
| 6266 | schema/migrations/collapse | ensureHiddenDefaultClientKeys |
| 6274 | schema/migrations/collapse | ensureDefaultClientStandardMeta |
| 6287 | schema/migrations/collapse | clientStandardMaterialMeta |
| 6298 | schema/migrations/collapse | effectiveDefaultPricing |
| 6306 | schema/migrations/collapse | effectiveDefaultClientPricing |
| 6315 | schema/migrations/collapse | ensureDefaultPricing |
| 6320 | schema/migrations/collapse | ensureDefaultClientPricing |
| 6325 | schema/migrations/collapse | newRoomPricing |
| 6331 | schema/migrations/collapse | newRoomClientPricing |
| 6337 | schema/migrations/collapse | normalizeMaterialUnit |
| 6340 | schema/migrations/collapse | ensureDefaultPurchaseCustomItems |
| 6350 | schema/migrations/collapse | ensureDefaultClientCustomItems |
| 6360 | schema/migrations/collapse | newRoomPurchaseCustomItems |
| 6365 | schema/migrations/collapse | newRoomClientCustomItems |
| 6370 | schema/migrations/collapse | ensureRoomCustomItems |
| 6384 | schema/migrations/collapse | ensureRoomHiddenInternalKeys |
| 6393 | schema/migrations/collapse | applyDefaultPurchaseCustomToRoom |
| 6414 | schema/migrations/collapse | applyDefaultClientCustomToRoom |
| 6436 | schema/migrations/collapse | flattenLegacyDrawings |
| 6457 | schema/migrations/collapse | makeRoom |
| 6482 | schema/migrations/collapse | pointId |
| 6482 | schema/migrations/collapse | edgeId |
| 6482 | schema/migrations/collapse | diagId |
| 6483 | schema/migrations/collapse | normRoom |
| 6546 | schema/migrations/collapse | migrateInternalAllRoomsV157 |
| 6558 | schema/migrations/collapse | migrateDefaultClientPricingV146 |
| 6583 | schema/migrations/collapse | migrateHintsCollapsedV73 |
| 6595 | schema/migrations/collapse | migrateOtherRoomNamesV66 |
| 6613 | schema/migrations/collapse | migrateHideAutoLettersV64 |
| 6625 | schema/migrations/collapse | migrateLegacyUnitsToMm |
| 6647 | schema/migrations/collapse | ensureCollapseState |
| 6651 | schema/migrations/collapse | isUiCollapsed |
| 6653 | schema/migrations/collapse | updateCollapseButton |
| 6661 | schema/migrations/collapse | collapseBodyHeight |
| 6667 | schema/migrations/collapse | setCollapseVisualState |
| 6688 | schema/migrations/collapse | initializeCollapseBlock |
| 6696 | schema/migrations/collapse | setCollapseBlockState |
| 6760 | schema/migrations/collapse | normalizeExpandedCollapseBodies |
| 6773 | schema/migrations/collapse | toggleCollapseBlock |
| 6774 | schema/migrations/collapse | bindCollapseBlocks |
| 6782 | schema/migrations/collapse | save |
| 6787 | schema/migrations/collapse | ensurePricing |
| 6796 | schema/migrations/collapse | ensureRoomPricing |
| 6807 | schema/migrations/collapse | ensureClientPricing |
| 6826 | schema/migrations/collapse | ensureRoomExtras |
| 6836 | schema/migrations/collapse | migratePricingToRooms |
| 6866 | validation/pricing | wallInputToUnits |
| 6886 | validation/pricing | fmtWallMeters |
| 6891 | validation/pricing | updateWallMeterHint |
| 6898 | validation/pricing | fmtPerimeterM |
| 6903 | validation/pricing | fmtMoney |
| 6907 | validation/pricing | fmtQty |
| 6911 | validation/pricing | roomValidation |
| 6928 | validation/pricing | validationPanelHtml |
| 6938 | validation/pricing | drawingValidationState |
| 6945 | validation/pricing | openDrawingValidation |
| 6983 | validation/pricing | toggleGeometryLock |
| 6994 | validation/pricing | duplicateRoom |
| 7008 | validation/pricing | defaultPriceRowsHtml |
| 7032 | validation/pricing | hiddenDefaultPriceRowsHtml |
| 7048 | validation/pricing | bindDefaultPriceInputs |
| 7057 | validation/pricing | customDefaultPriceRowsHtml |
| 7072 | validation/pricing | bindDefaultPriceDeleteButtons |
| 7138 | validation/pricing | renderPurchasePricePreset |
| 7148 | validation/pricing | renderClientPricePreset |
| 7158 | validation/pricing | applyPricePreset |
| 7173 | validation/pricing | applyClientPricePreset |
| 7199 | validation/pricing | syncPricePresetBodyState |
| 7205 | validation/pricing | openPricePreset |
| 7213 | validation/pricing | closePricePreset |
| 7219 | validation/pricing | openClientPricePreset |
| 7235 | validation/pricing | closeClientPricePreset |
| 7242 | validation/pricing | roomReady |
| 7247 | validation/pricing | roomCost |
| 7296 | validation/pricing | orderCost |
| 7336 | validation/pricing | sortedRoomCostItems |
| 7347 | validation/pricing | setActiveRoomFromSummary |
| 7363 | validation/pricing | updateInternalCalcControls |
| 7369 | validation/pricing | bindInternalCalcControls |
| 7387 | validation/pricing | roomSummaryMeta |
| 7392 | validation/pricing | priceRoomRowHtml |
| 7401 | validation/pricing | internalRoomCalcLines |
| 7425 | validation/pricing | internalCalcSummaryHtml |
| 7492 | validation/pricing | refreshInternalCalcSummary |
| 7523 | validation/pricing | closeInternalCalcSummary |
| 7529 | orders/rooms/render | bindPriceSummaryActions |
| 7533 | orders/rooms/render | curOrder |
| 7534 | orders/rooms/render | curRoom |
| 7535 | orders/rooms/render | newOrder |
| 7539 | orders/rooms/render | ensureOrder |
| 7541 | orders/rooms/render | refreshHomeScreen |
| 7551 | orders/rooms/render | showHomeScreen |
| 7558 | orders/rooms/render | showWorkspace |
| 7559 | orders/rooms/render | openHomeDrawing |
| 7565 | orders/rooms/render | syncFocusDrawingViewport |
| 7583 | orders/rooms/render | scheduleFocusDrawingViewport |
| 7589 | orders/rooms/render | setDrawingFocus |
| 7621 | orders/rooms/render | toggleDrawingFocus |
| 7623 | orders/rooms/render | ensureHistoryNavVisible |
| 7631 | orders/rooms/render | overlayBackAvailable |
| 7632 | orders/rooms/render | updateHistoryNav |
| 7633 | orders/rooms/render | globalBackAction |
| 7650 | orders/rooms/render | ceilingHandleAndroidBack |
| 7691 | orders/rooms/render | push |
| 7698 | orders/rooms/render | restore |
| 7699 | orders/rooms/render | undoDo |
| 7700 | orders/rooms/render | redoDo |
| 7702 | orders/rooms/render | renderOrders |
| 7708 | orders/rooms/render | render |
| 7752 | orders/rooms/render | requestDeleteRoom |
| 7772 | orders/rooms/render | openRoomActions |
| 7780 | orders/rooms/render | closeRoomActions |
| 7792 | orders/rooms/render | buildRoomAlphaKeyboard |
| 7850 | orders/rooms/render | saveRoomRename |
| 7863 | orders/rooms/render | nextRoomTypeName |
| 7884 | orders/rooms/render | openRoomPicker |
| 7889 | orders/rooms/render | closeRoomPicker |
| 7893 | orders/rooms/render | addRoomByType |
| 7911 | orders/rooms/render | renderRooms |
| 8025 | orders/rooms/render | internalEditableRoomCardHtml |
| 8155 | orders/rooms/render | internalPurchaseAllRoomsHtml |
| 8188 | orders/rooms/render | bindInternalPurchaseAllRooms |
| 8249 | orders/rooms/render | refreshInternalPurchaseAllRooms |
| 8275 | orders/rooms/render | clientMainEstimateHtml |
| 8306 | orders/rooms/render | renderRoom |
| 8404 | orders/rooms/render | hintTitle |
| 8405 | orders/rooms/render | hintText |
| 8413 | orders/rooms/render | statusText |
| 8431 | file/estimate/PDF/saved | openSavedAction |
| 8444 | file/estimate/PDF/saved | closeSavedAction |
| 8448 | file/estimate/PDF/saved | deleteSavedMeasurement |
| 8472 | file/estimate/PDF/saved | saveBlobFile |
| 8492 | file/estimate/PDF/saved | exportBackup |
| 8499 | file/estimate/PDF/saved | importBackupFile |
| 8517 | file/estimate/PDF/saved | estimateDateText |
| 8521 | file/estimate/PDF/saved | estimateFileSafeName |
| 8536 | file/estimate/PDF/saved | clientEstimateSourceQuantities |
| 8549 | file/estimate/PDF/saved | estimateRoomLines |
| 8587 | file/estimate/PDF/saved | validMergedEstimateOrders |
| 8601 | file/estimate/PDF/saved | estimateOrderLabel |
| 8606 | file/estimate/PDF/saved | findOrderRoom |
| 8610 | file/estimate/PDF/saved | renderClientMergeOrdersList |
| 8650 | file/estimate/PDF/saved | openClientMergeOrders |
| 8659 | file/estimate/PDF/saved | closeClientMergeOrders |
| 8663 | file/estimate/PDF/saved | applyClientMergeOrders |
| 8676 | file/estimate/PDF/saved | clientEstimateData |
| 8719 | file/estimate/PDF/saved | fittedPreviewRoom |
| 8742 | file/estimate/PDF/saved | openRoomSketchPreview |
| 8761 | file/estimate/PDF/saved | closeRoomSketchPreview |
| 8767 | file/estimate/PDF/saved | clientEstimateBodyHtml |
| 8864 | file/estimate/PDF/saved | bindClientEstimatePriceInputs |
| 8905 | file/estimate/PDF/saved | refreshClientEstimate |
| 8924 | file/estimate/PDF/saved | closeClientEstimate |
| 8938 | file/estimate/PDF/saved | clientMaterialSelectableRooms |
| 8963 | file/estimate/PDF/saved | clientMaterialTargetRooms |
| 8976 | file/estimate/PDF/saved | clientMaterialTargetRoom |
| 8979 | file/estimate/PDF/saved | renderClientMaterialRoomChooser |
| 9030 | file/estimate/PDF/saved | openClientMaterialRoomChooser |
| 9052 | file/estimate/PDF/saved | closeClientMaterialRoomChooser |
| 9058 | file/estimate/PDF/saved | clientMaterialDefaultQty |
| 9065 | file/estimate/PDF/saved | renderClientMaterialList |
| 9133 | file/estimate/PDF/saved | openClientMaterialSheet |
| 9142 | file/estimate/PDF/saved | closeClientMaterialSheet |
| 9146 | file/estimate/PDF/saved | addClientEstimateMaterial |
| 9191 | file/estimate/PDF/saved | addClientPresetMaterialToEstimate |
| 9234 | file/estimate/PDF/saved | buildClientOtherAlphaKeyboard |
| 9262 | file/estimate/PDF/saved | openOtherMaterialSheet |
| 9316 | file/estimate/PDF/saved | closeClientOtherMaterialSheet |
| 9324 | file/estimate/PDF/saved | addClientOtherMaterial |
| 9423 | file/estimate/PDF/saved | bytesFromDataUrl |
| 9429 | file/estimate/PDF/saved | asciiBytes |
| 9430 | file/estimate/PDF/saved | concatByteParts |
| 9434 | file/estimate/PDF/saved | buildImagePdfBlob |
| 9467 | file/estimate/PDF/saved | pdfRoundRect |
| 9473 | file/estimate/PDF/saved | pdfFont |
| 9476 | file/estimate/PDF/saved | pdfWrapLines |
| 9491 | file/estimate/PDF/saved | renderClientEstimateCanvasOffline |
| 9769 | file/estimate/PDF/saved | downloadClientEstimatePdf |
| 9792 | file/estimate/PDF/saved | bindSavedItemActions |
| 9821 | file/estimate/PDF/saved | renderSavedMeasurementsList |
| 9845 | file/estimate/PDF/saved | openSavedMeasurements |
| 9852 | file/estimate/PDF/saved | closeSavedMeasurements |
| 9857 | file/estimate/PDF/saved | formatSavedAt |
| 9862 | file/estimate/PDF/saved | commitExplicitSaveMeasurement |
| 9875 | file/estimate/PDF/saved | explicitSaveMeasurement |
| 9893 | file/estimate/PDF/saved | renderDrawToolsMenu |
| 10023 | tools/keypad/geometry helpers | openDrawToolsMenu |
| 10030 | tools/keypad/geometry helpers | closeDrawToolsMenu |
| 10046 | tools/keypad/geometry helpers | calcInputLabel |
| 10051 | tools/keypad/geometry helpers | formatCalcPadDisplay |
| 10054 | tools/keypad/geometry helpers | normalizeCalcPadValue |
| 10058 | tools/keypad/geometry helpers | applyCalcPadValue |
| 10127 | tools/keypad/geometry helpers | openCalcKeypad |
| 10188 | tools/keypad/geometry helpers | closeCalcKeypad |
| 10199 | tools/keypad/geometry helpers | pressCalcKey |
| 10221 | tools/keypad/geometry helpers | bindRoomUI |
| 10323 | tools/keypad/geometry helpers | updatePricingUI |
| 10370 | tools/keypad/geometry helpers | pointById |
| 10371 | tools/keypad/geometry helpers | pointIndex |
| 10372 | tools/keypad/geometry helpers | pointLabel |
| 10373 | tools/keypad/geometry helpers | edgesAt |
| 10374 | tools/keypad/geometry helpers | vertexDegree |
| 10375 | tools/keypad/geometry helpers | edgeOther |
| 10376 | tools/keypad/geometry helpers | edgeLabel |
| 10377 | tools/keypad/geometry helpers | diagLabel |
| 10380 | tools/keypad/geometry helpers | rectsOverlap |
| 10383 | tools/keypad/geometry helpers | pointSegmentDistance |
| 10390 | tools/keypad/geometry helpers | pickDiagLabelPlacement |
| 10449 | tools/keypad/geometry helpers | diagFanPreferredT |
| 10477 | tools/keypad/geometry helpers | unitVec |
| 10481 | tools/keypad/geometry helpers | moveCorner90Info |
| 10521 | tools/keypad/geometry helpers | nearestMove90Candidate |
| 10533 | tools/keypad/geometry helpers | cyclePolygonPoints |
| 10537 | tools/keypad/geometry helpers | drawingCenter |
| 10546 | solver/calculation | pointInPolygon |
| 10557 | solver/calculation | edgeOutsideChoice |
| 10578 | solver/calculation | edgeOutwardNormal |
| 10584 | solver/calculation | outwardPointUnit |
| 10605 | solver/calculation | edgeBetween |
| 10606 | solver/calculation | straightContinuationEdge |
| 10617 | solver/calculation | extendStraightWall |
| 10633 | solver/calculation | diagBetween |
| 10634 | solver/calculation | deleteEdge |
| 10640 | solver/calculation | deleteVertex |
| 10649 | solver/calculation | openChainOrder |
| 10687 | solver/calculation | segmentProjectionInfo |
| 10694 | solver/calculation | repairOvershotClosedContour |
| 10734 | solver/calculation | repairNearlyClosedContour |
| 10803 | solver/calculation | cycleOrder |
| 10818 | solver/calculation | inferOrthogonalSketchAxis |
| 10857 | solver/calculation | repairTinyUnmeasuredClosureEdge |
| 10900 | solver/calculation | autoGeometryConstraints |
| 10912 | solver/calculation | median |
| 10919 | solver/calculation | circleIntersectionCandidates |
| 10939 | solver/calculation | fitPointToSolvedAnchors |
| 10966 | solver/calculation | polygonTurnSigns |
| 10977 | solver/calculation | segmentsCrossProper |
| 10983 | solver/calculation | polygonSelfCrossings |
| 10998 | solver/calculation | buildArchitecturalGuide |
| 11091 | solver/calculation | anchorErrorStats |
| 11100 | solver/calculation | closestFeasibleCandidate |
| 11130 | solver/calculation | fitPointCandidate |
| 11157 | solver/calculation | tryTrilateratedGeometryFromBase |
| 11296 | solver/calculation | tryTrilateratedGeometry |
| 11375 | solver/calculation | solveFreeGeometry |
| 11594 | solver/calculation | solveOrthogonalGeometry |
| 11749 | solver/calculation | solveAutoGeometry |
| 11756 | solver/calculation | applyGeometrySolution |
| 11784 | solver/calculation | applyAutoGeometry |
| 11785 | solver/calculation | applyOrthogonalGeometry |
| 11787 | solver/calculation | exactRectAreaRaw |
| 11819 | solver/calculation | roomCalc |
| 11897 | solver/calculation | autoDiagonal |
| 11905 | solver/calculation | generateAutoDiagonals |
| 11949 | labels/SVG/snap | normalizedLabelAngle |
| 11956 | labels/SVG/snap | normalizedLabelSize |
| 11961 | labels/SVG/snap | labelPose |
| 11971 | labels/SVG/snap | labelItemByKind |
| 11976 | labels/SVG/snap | selectedLabelIs |
| 11979 | labels/SVG/snap | labelGroupSvg |
| 11985 | labels/SVG/snap | labelSelectionSvg |
| 12012 | labels/SVG/snap | syncLabelScreenScale |
| 12023 | labels/SVG/snap | liveLabelTransform |
| 12041 | labels/SVG/snap | pickWallLabelPlacement |
| 12118 | labels/SVG/snap | svgScene |
| 12134 | labels/SVG/snap | svgGeometry |
| 12280 | labels/SVG/snap | wallGuideOverlaySvg |
| 12299 | labels/SVG/snap | bestGuideEdgeForVertex |
| 12314 | labels/SVG/snap | wallStopGuide |
| 12327 | labels/SVG/snap | consider |
| 12397 | labels/SVG/snap | snapWallToGuide |
| 12419 | labels/SVG/snap | drawMagnifierScene |
| 12585 | labels/SVG/snap | hideDrawMagnifier |
| 12592 | labels/SVG/snap | nearestVertex |
| 12597 | labels/SVG/snap | pointSegmentDistance |
| 12604 | labels/SVG/snap | nearestEdge |
| 12612 | labels/SVG/snap | svgData |
| 12622 | labels/SVG/snap | openVertexMenu |
| 12629 | labels/SVG/snap | closeVertexMenu |
| 12633 | labels/SVG/snap | startVertexLongPress |
| 12641 | labels/SVG/snap | deleteDiag |
| 12647 | labels/SVG/snap | startDiagLongPress |
| 12656 | labels/SVG/snap | handleDiagTap |
| 12662 | labels/SVG/snap | startEdgeLongPress |
| 12672 | labels/SVG/snap | cancelVertexLongPress |
| 12676 | labels/SVG/snap | handleEdgeTap |
| 12682 | pointer gestures/viewport | bindSvg |
| 12686 | pointer gestures/viewport | base |
| 12692 | pointer gestures/viewport | world |
| 12695 | pointer gestures/viewport | clearSingleForMulti |
| 13381 | pointer gestures/viewport | roundGrid |
| 13382 | pointer gestures/viewport | roundPrecisionGrid |
| 13387 | pointer gestures/viewport | snapAngle |
| 13388 | pointer gestures/viewport | startPinch |
| 13389 | pointer gestures/viewport | applyPinch |
| 13390 | pointer gestures/viewport | updateWorld |
| 13398 | pointer gestures/viewport | zoomBy |
| 13399 | pointer gestures/viewport | fitView |
| 13400 | pointer gestures/viewport | redrawOnly |
| 13409 | pointer gestures/viewport | armKeypadGuard |
| 13412 | pointer gestures/viewport | keypadIsGuarded |
| 13415 | pointer gestures/viewport | bindMiniKeypads |
| 13454 | wall/diag editors/global guards | orderedWallsForSequence |
| 13499 | wall/diag editors/global guards | nextUnmeasuredWall |
| 13511 | wall/diag editors/global guards | firstUnmeasuredWall |
| 13514 | wall/diag editors/global guards | wallSequencePosition |
| 13519 | wall/diag editors/global guards | previousWallInSequence |
| 13526 | wall/diag editors/global guards | commitCurrentWallValueSilently |
| 13541 | wall/diag editors/global guards | goPreviousWallInSequence |
| 13553 | wall/diag editors/global guards | armWallSequenceBrowserBack |
| 13560 | wall/diag editors/global guards | disarmWallSequenceBrowserBack |
| 13567 | wall/diag editors/global guards | syncWallDockLayout |
| 13579 | wall/diag editors/global guards | startWallSequence |
| 13593 | wall/diag editors/global guards | openWall |
| 13623 | wall/diag editors/global guards | dismissWallInput |
| 13632 | wall/diag editors/global guards | closeWall |
| 13645 | wall/diag editors/global guards | saveWall |
| 13735 | wall/diag editors/global guards | openDiag |
| 13746 | wall/diag editors/global guards | closeDiag |
| 13747 | wall/diag editors/global guards | saveDiag |
| 13752 | wall/diag editors/global guards | useAuto |
| 13999 | wall/diag editors/global guards | isNativeEditableTextTarget |
| 14103 | wall/diag editors/global guards | lockFromTarget |
| 14110 | wall/diag editors/global guards | unlock |


## Event bindings

| Line | Domain | Binding |
|---|---|---|
| 6777 | schema/migrations/collapse | btn.onclick=e=>{e.preventDefault();e.stopPropagation();toggleCollapseBlock(btn.dataset.collapseToggle)} |
| 7052 | validation/pricing | input.onpointerdown=e=>{e.preventDefault();e.stopPropagation()}; |
| 7053 | validation/pricing | input.onclick=e=>{e.preventDefault();e.stopPropagation();openCalcKeypad(input)}; |
| 7054 | validation/pricing | input.onfocus=e=>e.target.blur() |
| 7073 | validation/pricing | $$("[data-default-standard-hide]",root).forEach(btn=>btn.onclick=e=>{ |
| 7084 | validation/pricing | $$("[data-default-client-standard-hide]",root).forEach(btn=>btn.onclick=e=>{ |
| 7095 | validation/pricing | $$("[data-default-standard-restore]",root).forEach(btn=>btn.onclick=e=>{ |
| 7103 | validation/pricing | $$("[data-default-client-standard-restore]",root).forEach(btn=>btn.onclick=e=>{ |
| 7113 | validation/pricing | $$("[data-default-client-standard-edit]",root).forEach(btn=>btn.onclick=e=>{ |
| 7118 | validation/pricing | $$("[data-default-custom-edit]",root).forEach(btn=>btn.onclick=e=>{ |
| 7122 | validation/pricing | $$("[data-default-client-custom-edit]",root).forEach(btn=>btn.onclick=e=>{ |
| 7127 | validation/pricing | $$("[data-default-custom-delete]",root).forEach(btn=>btn.onclick=e=>{ |
| 7132 | validation/pricing | $$("[data-default-client-custom-delete]",root).forEach(btn=>btn.onclick=e=>{ |
| 7371 | validation/pricing | btn.onclick=e=>{ |
| 7378 | validation/pricing | btn.onclick=e=>{ |
| 7503 | validation/pricing | head.onclick=e=>{ |
| 7706 | orders/rooms/render | $$(".order",box).forEach(el=>el.onclick=()=>{orderId=el.dataset.id;roomId=curOrder()?.rooms?.[0]?.id\|\|null;activeVertexId=null;undo=[];redo=[];render()}) |
| 7747 | orders/rooms/render | $$("[data-ok]",$("#main")).forEach(el=>el.oninput=()=>{o[el.dataset.ok]=el.value;save();if(el.dataset.ok==="name"){const title=$(".order-title",$("#main"));if(title)title.textContent=String(o.name\|\|"").trim()\|\| |
| 7748 | orders/rooms/render | $("#delOrder").onclick=()=>showConfirm("Удалить заказ?","Карточка заказчика и все комнаты будут удалены.",()=>{db.orders=db.orders.filter(x=>x.id!==o.id);orderId=db.orders[0]?.id\|\|null;roomId=db.orders[0]?.room |
| 7817 | orders/rooms/render | kb.addEventListener("pointerdown",e=>{ |
| 7823 | orders/rooms/render | kb.addEventListener("click",e=>{ |
| 7927 | orders/rooms/render | b.onpointerdown=e=>{ |
| 7966 | orders/rooms/render | b.onpointermove=e=>{ |
| 7972 | orders/rooms/render | b.onpointerup=e=>{ |
| 8001 | orders/rooms/render | b.onpointercancel=e=>{ |
| 8008 | orders/rooms/render | b.oncontextmenu=e=>{ |
| 8014 | orders/rooms/render | $("#addRoom").onclick=e=>{ |
| 8192 | orders/rooms/render | input.onpointerdown=e=>{e.preventDefault();e.stopPropagation()}; |
| 8193 | orders/rooms/render | input.onclick=e=>{e.preventDefault();e.stopPropagation();openCalcKeypad(input)}; |
| 8194 | orders/rooms/render | input.onfocus=e=>e.target.blur() |
| 8197 | orders/rooms/render | $$("[data-internal-extras-toggle]",root).forEach(btn=>btn.onclick=e=>{ |
| 8218 | orders/rooms/render | $$("[data-internal-standard-delete]",root).forEach(btn=>btn.onclick=e=>{ |
| 8228 | orders/rooms/render | $$("[data-internal-standard-restore]",root).forEach(btn=>btn.onclick=e=>{ |
| 8236 | orders/rooms/render | $$("[data-internal-custom-delete]",root).forEach(btn=>btn.onclick=e=>{ |
| 8243 | orders/rooms/render | $$("[data-internal-add-material]",root).forEach(btn=>btn.onclick=e=>{ |
| 8636 | file/estimate/PDF/saved | btn.onclick=e=>{ |
| 8866 | file/estimate/PDF/saved | input.onpointerdown=e=>{e.preventDefault();e.stopPropagation()}; |
| 8867 | file/estimate/PDF/saved | input.onclick=e=>{e.preventDefault();e.stopPropagation();openCalcKeypad(input)}; |
| 8868 | file/estimate/PDF/saved | input.onfocus=e=>e.target.blur?.() |
| 8871 | file/estimate/PDF/saved | btn.onclick=e=>{ |
| 8880 | file/estimate/PDF/saved | btn.onclick=e=>{ |
| 8899 | file/estimate/PDF/saved | btn.onclick=e=>{ |
| 9010 | file/estimate/PDF/saved | if(all)all.onclick=e=>{ |
| 9020 | file/estimate/PDF/saved | btn.onclick=e=>{ |
| 9117 | file/estimate/PDF/saved | $$('[data-client-material-key]',box).forEach(btn=>btn.onclick=e=>{ |
| 9128 | file/estimate/PDF/saved | $$('[data-client-preset-material]',box).forEach(btn=>btn.onclick=e=>{ |
| 9250 | file/estimate/PDF/saved | kb.addEventListener("pointerdown",e=>{const b=e.target.closest("button");if(!b)return;e.preventDefault();e.stopPropagation()}); |
| 9251 | file/estimate/PDF/saved | kb.addEventListener("click",e=>{ |
| 9797 | file/estimate/PDF/saved | b.onpointerdown=e=>{ |
| 9805 | file/estimate/PDF/saved | b.onpointermove=e=>{ |
| 9809 | file/estimate/PDF/saved | b.onpointerup=e=>{ |
| 9817 | file/estimate/PDF/saved | b.onpointercancel=()=>{clearTimer();longDone=false;moved=true}; |
| 9818 | file/estimate/PDF/saved | b.oncontextmenu=e=>e.preventDefault() |
| 9918 | tools/keypad/geometry helpers | btn.onclick=()=>{ |
| 10224 | tools/keypad/geometry helpers | if($("#explicitSaveBtn"))$("#explicitSaveBtn").onclick=explicitSaveMeasurement; |
| 10225 | tools/keypad/geometry helpers | if($("#drawingCheckBtn"))$("#drawingCheckBtn").onclick=openDrawingValidation; |
| 10226 | tools/keypad/geometry helpers | if($("#drawingFocusBtn"))$("#drawingFocusBtn").onclick=()=>toggleDrawingFocus(); |
| 10227 | tools/keypad/geometry helpers | if($("#pricePresetBtn"))$("#pricePresetBtn").onclick=openPricePreset; |
| 10229 | tools/keypad/geometry helpers | if($("#mainClientAddMaterial"))$("#mainClientAddMaterial").onclick=openClientMaterialRoomChooser; |
| 10230 | tools/keypad/geometry helpers | if($("#mainClientPricePreset"))$("#mainClientPricePreset").onclick=openClientPricePreset; |
| 10231 | tools/keypad/geometry helpers | if($("#mainClientMergeOrders"))$("#mainClientMergeOrders").onclick=openClientMergeOrders; |
| 10232 | tools/keypad/geometry helpers | if($("#mainClientPdf"))$("#mainClientPdf").onclick=downloadClientEstimatePdf; |
| 10234 | tools/keypad/geometry helpers | if($("#openInternalPurchase"))$("#openInternalPurchase").onclick=e=>{ |
| 10242 | tools/keypad/geometry helpers | if($("#backToClientMain"))$("#backToClientMain").onclick=e=>{ |
| 10254 | tools/keypad/geometry helpers | if($("#notes"))$("#notes").oninput=e=>{r.notes=e.target.value;persistDbRaw()}; |
| 10256 | tools/keypad/geometry helpers | if($("#hintToggle"))$("#hintToggle").onclick=e=>{ |
| 10263 | tools/keypad/geometry helpers | $$("[data-quick-mode]").forEach(b=>b.onclick=()=>{ |
| 10275 | tools/keypad/geometry helpers | if($("#extrasToggle"))$("#extrasToggle").onclick=e=>{ |
| 10299 | tools/keypad/geometry helpers | input.onpointerdown=e=>{ |
| 10303 | tools/keypad/geometry helpers | input.onclick=e=>{ |
| 10308 | tools/keypad/geometry helpers | input.onfocus=e=>e.target.blur() |
| 10312 | tools/keypad/geometry helpers | $$("[data-internal-custom-delete]").forEach(btn=>btn.onclick=e=>{ |
| 10319 | tools/keypad/geometry helpers | $("#zoomIn").onclick=()=>zoomBy(1.16);$("#zoomOut").onclick=()=>zoomBy(1/1.16);$("#fitView").onclick=fitView; |
| 12710 | pointer gestures/viewport | svg.onpointerdown=e=>{ |
| 12930 | pointer gestures/viewport | svg.onpointermove=e=>{ |
| 13164 | pointer gestures/viewport | svg.onpointerup=e=>{ |
| 13361 | pointer gestures/viewport | svg.onpointercancel=e=>{ |
| 13421 | pointer gestures/viewport | btn.onpointerdown=e=>{ |
| 13425 | pointer gestures/viewport | btn.onpointerup=e=>e.stopPropagation(); |
| 13426 | pointer gestures/viewport | btn.onclick=e=>{ |
| 13669 | wall/diag editors/global guards | $("#wallCancel").onclick=()=>{ |
| 13676 | wall/diag editors/global guards | $("#wallSave").onclick=saveWall; |
| 13678 | wall/diag editors/global guards | $("#wallDecimalBtn").onclick=e=>{ |
| 13688 | wall/diag editors/global guards | $("#wallDeleteFromKeypad").onpointerdown=e=>{ |
| 13691 | wall/diag editors/global guards | $("#wallDeleteFromKeypad").onclick=e=>{ |
| 13704 | wall/diag editors/global guards | $("#wallDismiss").onclick=e=>{ |
| 13709 | wall/diag editors/global guards | $("#wallSheet").onclick=e=>{ |
| 13713 | wall/diag editors/global guards | $("#wallValue").onkeydown=e=>{if(e.key==="Enter")saveWall()}; |
| 13715 | wall/diag editors/global guards | window.addEventListener("popstate",()=>{ |
| 13756 | wall/diag editors/global guards | $("#diagDeleteFromKeypad").onpointerdown=e=>{e.stopPropagation()}; |
| 13757 | wall/diag editors/global guards | $("#diagDeleteFromKeypad").onclick=e=>{ |
| 13771 | wall/diag editors/global guards | window.addEventListener("resize",()=>{ |
| 13775 | wall/diag editors/global guards | window.visualViewport.addEventListener("resize",()=>{ |
| 13780 | wall/diag editors/global guards | $("#diagDismiss").onpointerdown=e=>{e.preventDefault();e.stopPropagation()};$("#diagDismiss").onclick=e=>{e.preventDefault();e.stopPropagation();closeDiag()}; |
| 13781 | wall/diag editors/global guards | $("#diagCancel").onclick=closeDiag;$("#diagSave").onclick=saveDiag;$("#diagUseAuto").onclick=useAuto;$("#diagSheet").onclick=e=>{if(e.target===$("#diagSheet"))closeDiag()};$("#diagValue").onkeydown=e=>{if(e.key |
| 13784 | wall/diag editors/global guards | $("#calcKeypadClose").onclick=e=>{e.preventDefault();e.stopPropagation();closeCalcKeypad()}; |
| 13785 | wall/diag editors/global guards | $("#calcKeypadDone").onclick=e=>{e.preventDefault();e.stopPropagation();closeCalcKeypad()}; |
| 13786 | wall/diag editors/global guards | $("#calcKeypad").onclick=e=>{if(e.target===$("#calcKeypad"))closeCalcKeypad()}; |
| 13788 | wall/diag editors/global guards | btn.onpointerdown=e=>{e.preventDefault();e.stopPropagation()}; |
| 13789 | wall/diag editors/global guards | btn.onclick=e=>{ |
| 13795 | wall/diag editors/global guards | $("#calcDecimalKey").onpointerdown=e=>{e.preventDefault();e.stopPropagation()}; |
| 13796 | wall/diag editors/global guards | $("#calcDecimalKey").onclick=e=>{ |
| 13802 | wall/diag editors/global guards | $("#roomActionClose").onclick=closeRoomActions; |
| 13803 | wall/diag editors/global guards | $("#roomActionSheet").onclick=e=>{if(e.target===$("#roomActionSheet"))closeRoomActions()}; |
| 13804 | wall/diag editors/global guards | $("#roomRenameSave").onclick=saveRoomRename; |
| 13805 | wall/diag editors/global guards | $("#roomRenameValue").onfocus=e=>e.target.blur(); |
| 13806 | wall/diag editors/global guards | $("#roomDuplicateFromMenu").onclick=()=>{const id=roomActionId;if(id)duplicateRoom(id)}; |
| 13807 | wall/diag editors/global guards | $("#roomDeleteFromMenu").onclick=()=>{ |
| 13813 | wall/diag editors/global guards | $("#miniConfirmCancel").onpointerdown=e=>e.stopPropagation(); |
| 13814 | wall/diag editors/global guards | $("#miniConfirmCancel").onclick=e=>{e.preventDefault();e.stopPropagation();hideConfirm()}; |
| 13815 | wall/diag editors/global guards | $("#miniConfirmOk").onpointerdown=e=>e.stopPropagation(); |
| 13816 | wall/diag editors/global guards | $("#miniConfirmOk").onclick=e=>{ |
| 13823 | wall/diag editors/global guards | $("#miniConfirm").onclick=e=>{if(e.target===$("#miniConfirm"))hideConfirm()}; |
| 13826 | wall/diag editors/global guards | $("#vertexAddA").onclick=()=>{ |
| 13845 | wall/diag editors/global guards | $("#vertexMenuClose").onclick=closeVertexMenu; |
| 13846 | wall/diag editors/global guards | $("#vertexMenu").onclick=e=>{if(e.target===$("#vertexMenu"))closeVertexMenu()}; |
| 13847 | wall/diag editors/global guards | $("#vertexDelete").onclick=()=>{ |
| 13855 | wall/diag editors/global guards | $("#uiSettingsBtn").onclick=openUiSettings; |
| 13856 | wall/diag editors/global guards | $("#uiSettingsClose").onclick=closeUiSettings; |
| 13857 | wall/diag editors/global guards | $("#uiSettingsDone").onclick=closeUiSettings; |
| 13858 | wall/diag editors/global guards | $("#uiSettingsReset").onclick=resetUiSettings; |
| 13859 | wall/diag editors/global guards | $("#uiSettingsSheet").onclick=e=>{if(e.target===$("#uiSettingsSheet"))closeUiSettings()}; |
| 13860 | wall/diag editors/global guards | $$('[data-ui-font]').forEach(btn=>btn.onclick=e=>{e.preventDefault();e.stopPropagation();setUiPref('font',btn.dataset.uiFont)}); |
| 13861 | wall/diag editors/global guards | $("#uiSizeSlider").oninput=e=>{ |
| 13865 | wall/diag editors/global guards | $$('[data-ui-weight]').forEach(btn=>btn.onclick=e=>{e.preventDefault();e.stopPropagation();setUiPref('weight',btn.dataset.uiWeight)}); |
| 13867 | wall/diag editors/global guards | $("#savedBtn").onclick=openSavedMeasurements; |
| 13868 | wall/diag editors/global guards | $("#savedClose").onclick=closeSavedMeasurements; |
| 13869 | wall/diag editors/global guards | $("#backupExport").onclick=exportBackup; |
| 13870 | wall/diag editors/global guards | $("#backupImport").onclick=()=>$("#backupFile").click(); |
| 13871 | wall/diag editors/global guards | $("#backupFile").onchange=e=>{const f=e.target.files?.[0];if(f)importBackupFile(f);e.target.value=""}; |
| 13872 | wall/diag editors/global guards | $("#pricePresetClose").onclick=closePricePreset; |
| 13873 | wall/diag editors/global guards | $("#pricePresetSheet").onclick=e=>{if(e.target===$("#pricePresetSheet"))closePricePreset()}; |
| 13874 | wall/diag editors/global guards | $("#pricePresetApplyRoom").onclick=()=>applyPricePreset("room"); |
| 13875 | wall/diag editors/global guards | $("#pricePresetApplyAll").onclick=()=>applyPricePreset("all"); |
| 13876 | wall/diag editors/global guards | $("#purchasePricePresetManage").onclick=()=>{ |
| 13880 | wall/diag editors/global guards | $("#purchasePricePresetAdd").onclick=()=>openOtherMaterialSheet("purchaseDefault"); |
| 13881 | wall/diag editors/global guards | $("#clientPricePresetBtn").onclick=openClientPricePreset; |
| 13882 | wall/diag editors/global guards | $("#clientPricePresetClose").onclick=closeClientPricePreset; |
| 13883 | wall/diag editors/global guards | $("#clientPricePresetSheet").onclick=e=>{if(e.target===$("#clientPricePresetSheet"))closeClientPricePreset()}; |
| 13884 | wall/diag editors/global guards | $("#clientPricePresetApplyRoom").onclick=()=>applyClientPricePreset("room"); |
| 13885 | wall/diag editors/global guards | $("#clientPricePresetApplyAll").onclick=()=>applyClientPricePreset("all"); |
| 13886 | wall/diag editors/global guards | $("#clientPricePresetManage").onclick=()=>{ |
| 13890 | wall/diag editors/global guards | $("#clientPricePresetAdd").onclick=()=>openOtherMaterialSheet("clientDefault"); |
| 13891 | wall/diag editors/global guards | $("#roomSketchPreviewClose").onclick=closeRoomSketchPreview; |
| 13892 | wall/diag editors/global guards | $("#roomSketchPreviewSheet").onclick=e=>{if(e.target===$("#roomSketchPreviewSheet"))closeRoomSketchPreview()}; |
| 13893 | wall/diag editors/global guards | $("#clientEstimateClose").onclick=closeClientEstimate; |
| 13894 | wall/diag editors/global guards | $("#clientMergeOrders").onclick=openClientMergeOrders; |
| 13895 | wall/diag editors/global guards | $("#clientMergeOrdersClose").onclick=closeClientMergeOrders; |
| 13896 | wall/diag editors/global guards | $("#clientMergeOrdersCancel").onclick=closeClientMergeOrders; |
| 13897 | wall/diag editors/global guards | $("#clientMergeOrdersApply").onclick=applyClientMergeOrders; |
| 13898 | wall/diag editors/global guards | $("#clientMergeOrdersSheet").onclick=e=>{if(e.target===$("#clientMergeOrdersSheet"))closeClientMergeOrders()}; |
| 13899 | wall/diag editors/global guards | $("#clientAddMaterial").onclick=openClientMaterialRoomChooser; |
| 13900 | wall/diag editors/global guards | $("#clientMaterialRoomClose").onclick=closeClientMaterialRoomChooser; |
| 13901 | wall/diag editors/global guards | $("#clientMaterialRoomSheet").onclick=e=>{if(e.target===$("#clientMaterialRoomSheet"))closeClientMaterialRoomChooser()}; |
| 13902 | wall/diag editors/global guards | $("#clientMaterialClose").onclick=closeClientMaterialSheet; |
| 13903 | wall/diag editors/global guards | $("#clientMaterialSheet").onclick=e=>{if(e.target===$("#clientMaterialSheet"))closeClientMaterialSheet()}; |
| 13904 | wall/diag editors/global guards | $("#clientOtherMaterialClose").onclick=closeClientOtherMaterialSheet; |
| 13905 | wall/diag editors/global guards | $("#clientOtherMaterialCancel").onclick=closeClientOtherMaterialSheet; |
| 13906 | wall/diag editors/global guards | $("#clientOtherMaterialAdd").onclick=addClientOtherMaterial; |
| 13907 | wall/diag editors/global guards | $("#clientOtherMaterialSheet").onclick=e=>{if(e.target===$("#clientOtherMaterialSheet"))closeClientOtherMaterialSheet()}; |
| 13908 | wall/diag editors/global guards | $$('[data-client-other-unit]').forEach(btn=>btn.onclick=e=>{e.preventDefault();e.stopPropagation();clientOtherMaterialUnit=btn.dataset.clientOtherUnit\|\|"шт";$$('[data-client-other-unit]').forEach(b=>b.classList |
| 13909 | wall/diag editors/global guards | $("#clientEstimatePdf").onclick=downloadClientEstimatePdf; |
| 13910 | wall/diag editors/global guards | $("#clientEstimateSheet").onclick=e=>{if(e.target===$("#clientEstimateSheet"))closeClientEstimate()}; |
| 13911 | wall/diag editors/global guards | $("#internalCalcClose").onclick=closeInternalCalcSummary; |
| 13912 | wall/diag editors/global guards | $("#internalCalcSheet").onclick=e=>{if(e.target===$("#internalCalcSheet"))closeInternalCalcSummary()}; |
| 13916 | wall/diag editors/global guards | $("#savedActionClose").onclick=closeSavedAction; |
| 13917 | wall/diag editors/global guards | $("#savedActionSheet").onclick=e=>{if(e.target===$("#savedActionSheet"))closeSavedAction()}; |
| 13918 | wall/diag editors/global guards | $("#savedDeleteAction").onclick=()=>{ |
| 13923 | wall/diag editors/global guards | $("#savedMeasurements").onclick=e=>{if(e.target===$("#savedMeasurements"))closeSavedMeasurements()}; |
| 13924 | wall/diag editors/global guards | if($("#savedSearchInput"))$("#savedSearchInput").oninput=renderSavedMeasurementsList; |
| 13925 | wall/diag editors/global guards | $$('[data-saved-filter]').forEach(btn=>btn.onclick=e=>{ |
| 13932 | wall/diag editors/global guards | $("#newOrder").onclick=newOrder;$("#search").oninput=renderOrders; |
| 13933 | wall/diag editors/global guards | $("#homeBtn").onclick=showHomeScreen; |
| 13934 | wall/diag editors/global guards | $("#homeSettingsBtn").onclick=openUiSettings; |
| 13935 | wall/diag editors/global guards | $("#homeNewOrder").onclick=()=>{showWorkspace();newOrder()}; |
| 13936 | wall/diag editors/global guards | $("#homeSaved").onclick=openSavedMeasurements; |
| 13937 | wall/diag editors/global guards | $("#homeDrawing").onclick=openHomeDrawing; |
| 13939 | wall/diag editors/global guards | $("#roomPickerClose").onclick=e=>{e.preventDefault();e.stopPropagation();closeRoomPicker()}; |
| 13940 | wall/diag editors/global guards | $("#roomPicker").onclick=e=>{if(e.target===$("#roomPicker"))closeRoomPicker()}; |
| 13941 | wall/diag editors/global guards | $$("[data-room-type]").forEach(btn=>btn.onclick=e=>{ |
| 13947 | wall/diag editors/global guards | $("#drawToolsNav").onclick=e=>{ |
| 13955 | wall/diag editors/global guards | $("#drawToolsClose").onclick=e=>{e.preventDefault();e.stopPropagation();closeDrawToolsMenu()}; |
| 13956 | wall/diag editors/global guards | $("#drawToolsMenu").onclick=e=>{if(e.target===$("#drawToolsMenu"))closeDrawToolsMenu()}; |
| 13957 | wall/diag editors/global guards | $("#globalUndo").onclick=globalBackAction;$("#globalRedo").onclick=redoDo; |
| 13964 | wall/diag editors/global guards | document.addEventListener("contextmenu",e=>{ |
| 13982 | wall/diag editors/global guards | document.addEventListener("selectstart",e=>{ |
| 13990 | wall/diag editors/global guards | document.addEventListener("dragstart",e=>{ |
| 14005 | wall/diag editors/global guards | document.addEventListener("contextmenu",e=>{ |
| 14011 | wall/diag editors/global guards | document.addEventListener("selectstart",e=>{ |
| 14017 | wall/diag editors/global guards | document.addEventListener("dragstart",e=>{ |
| 14023 | wall/diag editors/global guards | document.addEventListener("selectionchange",()=>{ |
| 14117 | wall/diag editors/global guards | document.addEventListener('pointerdown',e=>{ |
| 14126 | wall/diag editors/global guards | document.addEventListener('pointerup',e=>unlock(e.pointerId),{capture:true,passive:true}); |
| 14127 | wall/diag editors/global guards | document.addEventListener('pointercancel',e=>unlock(e.pointerId),{capture:true,passive:true}); |
| 14130 | wall/diag editors/global guards | document.addEventListener('touchstart',e=>{ |
| 14140 | wall/diag editors/global guards | document.addEventListener('touchmove',e=>{ |
| 14145 | wall/diag editors/global guards | document.addEventListener('touchend',()=>{ if(!pointerId) locked=false; },{capture:true,passive:true}); |
| 14146 | wall/diag editors/global guards | document.addEventListener('touchcancel',()=>{ locked=false; pointerId=null; },{capture:true,passive:true}); |
| 14150 | wall/diag editors/global guards | scroller.addEventListener('scroll',()=>{ |
| 14158 | wall/diag editors/global guards | window.addEventListener('blur',()=>{locked=false;pointerId=null},{passive:true}); |


## Timers / animation scheduling

| Line | Source |
|---|---|
| 6082 | el._overlayCloseTimer=setTimeout(()=>{ |
| 6090 | function showToast(text){const t=$("#appToast");t.textContent=text;t.classList.add("show");clearTimeout(toastTimer);toastTimer=setTimeout(()=>t.classList.remove("show"),1800)} |
| 6225 | uiTypographyFrame=requestAnimationFrame(()=>{for(const root of roots)applyUiTypography(root);refreshUiSettingsControls()}) |
| 6973 | requestAnimationFrame(()=>{ |
| 7210 | requestAnimationFrame(syncPricePresetBodyState); |
| 7216 | setTimeout(syncPricePresetBodyState,100); |
| 7228 | requestAnimationFrame(()=>{ |
| 7238 | setTimeout(syncPricePresetBodyState,100); |
| 7519 | requestAnimationFrame(()=>target.scrollIntoView({block:"start",behavior:"smooth"})) |
| 7562 | requestAnimationFrame(()=>document.querySelector('[data-collapse-block="drawing"]')?.scrollIntoView({block:"start",behavior:"smooth"})) |
| 7584 | requestAnimationFrame(()=>{ |
| 7585 | requestAnimationFrame(syncFocusDrawingViewport) |
| 7941 | timer=setTimeout(()=>{ |
| 7962 | requestAnimationFrame(()=>openRoomActions(id)) |
| 8440 | requestAnimationFrame(()=>{ |
| 8489 | setTimeout(()=>URL.revokeObjectURL(url),1800) |
| 9184 | requestAnimationFrame(()=>{ |
| 9222 | requestAnimationFrame(()=>{ |
| 9310 | requestAnimationFrame(()=>{ |
| 9366 | requestAnimationFrame(()=>{ |
| 9377 | requestAnimationFrame(()=>{ |
| 9396 | requestAnimationFrame(()=>{ |
| 9418 | requestAnimationFrame(()=>{ |
| 9803 | timer=setTimeout(()=>{timer=null;if(!moved){longDone=true;openSavedAction(id)}},LONG_PRESS_MS) |
| 9957 | requestAnimationFrame(()=>{ |
| 9968 | requestAnimationFrame(()=>startWallSequence()); |
| 9993 | requestAnimationFrame(()=>{ |
| 11774 | setTimeout(()=>showToast(solved.message),60) |
| 11777 | setTimeout(()=>showToast(left |
| 11781 | setTimeout(()=>showToast(solved.message),60) |
| 12635 | longPressTimer=setTimeout(()=>{ |
| 12649 | longPressTimer=setTimeout(()=>{ |
| 12664 | longPressTimer=setTimeout(()=>{ |
| 13316 | if(closedNow)setTimeout(()=>showToast("Контур замкнут автоматически"),0) |
| 13336 | if(closedNow)setTimeout(()=>showToast("Контур замкнут автоматически"),0) |
| 13353 | setTimeout(()=>{ |
| 13570 | requestAnimationFrame(()=>{ |
| 13642 | requestAnimationFrame(()=>normalizeExpandedCollapseBodies($("#roomArea")\|\|document)); |
| 13659 | setTimeout(()=>{ |
| 13960 | setTimeout(()=>{bindMiniKeypads();applyUiPrefs();if(!storagePersistent)showToast("Постоянное хранилище недоступно: данные сохраняются только до закрытия")},0); |
| 14155 | requestAnimationFrame(()=>{restoring=false}); |


### Audit rule
The native rewrite must preserve observable behavior, not JavaScript structure. A future native helper does not need the same name, but every user-visible behavior referenced here must remain traceable through NATIVE_MIGRATION_MATRIX.md.
