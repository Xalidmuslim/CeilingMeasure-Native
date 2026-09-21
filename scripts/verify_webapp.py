#!/usr/bin/env python3
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
import hashlib,re,sys,tempfile,subprocess

path=Path(sys.argv[1] if len(sys.argv)>1 else 'webapp/index.html')
if not path.is_file():
    raise SystemExit(f'FAIL: missing {path}')
raw=path.read_text(encoding='utf-8')
expected='846042d294444bb6b5b84ef6fea1e12bef26b1f36384e047515a4e67aed46136'
actual=hashlib.sha256(path.read_bytes()).hexdigest()
if actual!=expected:
    raise SystemExit(f'FAIL: golden master changed\nexpected {expected}\nactual   {actual}')

class P(HTMLParser):
    def __init__(self): super().__init__(); self.ids=[]
    def handle_starttag(self,tag,attrs):
        d=dict(attrs)
        if 'id' in d:self.ids.append(d['id'])
p=P(); p.feed(raw)
dups=[k for k,v in Counter(p.ids).items() if v>1]
if dups: raise SystemExit('FAIL: duplicate DOM ids: '+', '.join(dups))

required_ids={
 'savedBtn','newOrder','uiSettingsBtn','wallSheet','wallDismiss','diagSheet','diagDismiss',
 'savedMeasurements','savedSearchInput','backupExport','backupImport','backupFile',
 'clientEstimatePdf','historyNav','globalUndo','globalRedo','drawToolsNav'
}
missing=sorted(required_ids-set(p.ids))
if missing: raise SystemExit('FAIL: required controls missing: '+', '.join(missing))

required_tokens=[
 'const APP_STORAGE_KEY="ceilingMeasureData"',
 'const APP_SCHEMA_VERSION=165',
 'function saveBlobFile(',
 'AndroidFileBridge.saveBase64File',
 'function exportBackup(',
 'function importBackupFile(',
 'async function downloadClientEstimatePdf(',
 'function bindSvg(',
 'function renderRoom(',
 'function cycleOrder(',
 'wall-scroll-strip',
 'width:74px!important',
 'touch-action:none!important',
 'touch-action:pan-y!important',
 'data-label-size-action',
 'data-label-rotate-kind',
 'window.ceilingHandleAndroidBack=ceilingHandleAndroidBack;',
 'closed-contour-scroll',
 'const UI_DEFAULT_PREFS={version:UI_PREFS_VERSION,font:"system",scale:1.12,weight:"normal"}'
]
for t in required_tokens:
    if t not in raw: raise SystemExit('FAIL: required contract token missing: '+t)
if 'Номер заказа' in raw: raise SystemExit('FAIL: removed order-number UI returned')
if 'http://' in raw or 'https://' in raw:
    raise SystemExit('FAIL: web asset acquired external network dependency')
if 'requestFullscreen' in raw or 'exitFullscreen' in raw:
    raise SystemExit('FAIL: native browser Fullscreen API returned')
if re.search(r'\|\|=|&&=|\?\?=', raw):
    raise SystemExit('FAIL: logical assignment is not supported by the API 30 WebView target')
if len(raw.encode('utf-8')) < 450_000:
    raise SystemExit('FAIL: web asset is unexpectedly small; likely functionality was dropped')

scripts=re.findall(r'<script(?:\s[^>]*)?>(.*?)</script>',raw,re.S|re.I)
for i,js in enumerate(scripts):
    with tempfile.NamedTemporaryFile('w',suffix='.js',encoding='utf-8',delete=False) as f:
        f.write(js); tmp=f.name
    try:
        r=subprocess.run(['node','--check',tmp],capture_output=True,text=True)
    except FileNotFoundError:
        print('WARN: Node not installed; skipped JavaScript syntax check')
        break
    if r.returncode:
        raise SystemExit(f'FAIL: JavaScript syntax error in inline script {i}:\n{r.stderr}')

print('PASS: golden webapp contract verified')
print('SHA256:',actual)
print('DOM ids:',len(p.ids),'inline scripts:',len(scripts),'bytes:',len(raw.encode('utf-8')))
