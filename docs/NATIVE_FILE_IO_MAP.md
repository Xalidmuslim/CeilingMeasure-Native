# Phase 0 — Native file I/O map

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80

| Flow | Format | Exact behavior | Filename/output | Web reference |
|---|---|---|---|---|
| Backup export | JSON | {app:'ceilingMeasure',version:165,exportedAt:ISO,db} | zamery_backup_YYYY-MM-DD.json | webapp/index.html:L8470-L8505 |
| Backup import | JSON | accept raw.db or raw; require orders array; confirmation then replace DB | user-selected .json | webapp/index.html:L8495-L8520 |
| Browser file save | Blob URL + temporary <a download> | fallback when AndroidFileBridge absent | same requested name | webapp/index.html:L8470-L8495 |
| Legacy Android save bridge | base64 data via AndroidFileBridge.saveBase64File(name,mime,base64) | bridge path is legacy web wrapper behavior | same requested name | webapp/index.html:L8470-L8495 |
| Client PDF | offline raster-image PDF 1.4 | canvas 794 wide, page 1123, scale 1.55; JPEG .92 slices; A4 points 595×842 | smeta_<safe-client>_<DD-MM-YYYY>.pdf | webapp/index.html:L9420-L9790 |
| PDF magic | %PDF-1.4 + %OFFLINE | custom object/xref builder | application/pdf | webapp/index.html:L9420-L9475 |
| PDF content filter | only lines with qty>0, then rendered PDF filters price>0 | clientEstimateData / renderClientEstimateCanvasOffline | N/A | webapp/index.html:L8510-L8720; webapp/index.html:L9475-L9790 |
| Filename sanitization | forbidden \ / : * ? " < > \| → _; whitespace → _; max 50 chars | estimateFileSafeName | fallback klient | webapp/index.html:L8510-L8525 |

### Native parity requirements
- Backup compatibility is semantic and byte-structure aware: preserve field names/types and tolerate both wrapped and raw DB files.
- Native PDF does not need to reproduce the JavaScript PDF object builder internally, but must reproduce visible content, selected-position filtering, totals, pagination intent and a valid %PDF- prefix.
- The web AndroidFileBridge branch is **legacy wrapper-only** and must not survive as a runtime WebView dependency in the final native application.
