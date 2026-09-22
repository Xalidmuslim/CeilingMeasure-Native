#!/usr/bin/env bash
set -euo pipefail

EXPECTED="354fbede167490d2481d3253507ff2829dcefec495298075487880ea2258a388"
ACTUAL="$(sha256sum webapp/index.html | awk '{print $1}')"
[[ "$ACTUAL" == "$EXPECTED" ]] || {
  echo "Golden SHA mismatch: $ACTUAL"
  exit 1
}
python3 scripts/verify_webapp.py

CHROME=""
for candidate in google-chrome google-chrome-stable chromium chromium-browser; do
  if command -v "$candidate" >/dev/null 2>&1; then
    CHROME="$(command -v "$candidate")"
    break
  fi
done
[[ -n "$CHROME" ]] || {
  echo "A Chromium/Chrome binary is required for Phase 2 reference screenshots"
  exit 1
}

ROOT="$(pwd)"
OUT="$ROOT/build/phase2-web-reference"
TMP="$OUT/tmp"
rm -rf "$OUT"
mkdir -p "$TMP"

GOLDEN_URI="file://$ROOT/webapp/index.html"

make_wrapper() {
  local state="$1"
  local file="$TMP/$state.html"
  cat > "$file" <<EOF
<!doctype html>
<meta charset="utf-8">
<style>
html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#fff}
iframe{position:fixed;inset:0;width:100%;height:100%;border:0}
</style>
<iframe id="f" src="$GOLDEN_URI"></iframe>
<script>
const state="$state";
const f=document.getElementById('f');
let handled=false;
f.addEventListener('load',()=>{
  if(handled)return;
  handled=true;
  setTimeout(()=>{
    const d=f.contentDocument;
    function custom(){
      d.getElementById('homeSettingsBtn')?.click();
      setTimeout(()=>{
        d.querySelector('[data-ui-font="serif"]')?.click();
        const slider=d.getElementById('uiSizeSlider');
        if(slider){slider.value='124';slider.dispatchEvent(new Event('input',{bubbles:true}));}
        d.querySelector('[data-ui-weight="bold"]')?.click();
      },120);
    }
    if(state==='workspace_top'){
      d.getElementById('homeNewOrder')?.click();
    }else if(state==='workspace_bottom'){
      d.getElementById('homeNewOrder')?.click();
      setTimeout(()=>{const sc=d.getElementById('appScroll');if(sc)sc.scrollTop=sc.scrollHeight;},220);
    }else if(state==='settings_default'){
      d.getElementById('homeSettingsBtn')?.click();
    }else if(state==='settings_custom'){
      custom();
    }else if(state==='home_custom'){
      custom();
      setTimeout(()=>d.getElementById('uiSettingsDone')?.click(),360);
    }else if(state==='workspace_custom'){
      custom();
      setTimeout(()=>{
        d.getElementById('uiSettingsDone')?.click();
        d.getElementById('homeNewOrder')?.click();
      },360);
    }
  },350);
});
</script>
EOF
}

capture() {
  local state="$1"
  local width="$2"
  local height="$3"
  make_wrapper "$state"
  "$CHROME" --headless=new --no-sandbox --disable-gpu --hide-scrollbars --allow-file-access-from-files --force-device-scale-factor=1 --run-all-compositor-stages-before-draw --virtual-time-budget=3000 --window-size="$width,$height" --screenshot="$OUT/web-$width-x-$height-x-$state.png" "file://$TMP/$state.html" >/tmp/phase2-chrome.log 2>&1
  test -s "$OUT/web-$width-x-$height-x-$state.png"
}

for width in 360 393 412; do
  capture home "$width" 852
  capture workspace_top "$width" 852
  capture workspace_bottom "$width" 852
done

capture settings_default 393 852
capture settings_custom 393 852
capture home_custom 393 852
capture workspace_custom 393 852

rm -rf "$TMP"
printf 'golden_sha=%s\n' "$ACTUAL" > "$OUT/reference_manifest.txt"
printf 'chrome=%s\n' "$("$CHROME" --version)" >> "$OUT/reference_manifest.txt"
printf 'screenshots=%s\n' "$(find "$OUT" -maxdepth 1 -name '*.png' | wc -l)" >> "$OUT/reference_manifest.txt"

COUNT="$(find "$OUT" -maxdepth 1 -name '*.png' | wc -l)"
[[ "$COUNT" -ge 13 ]] || {
  echo "Expected at least 13 reference screenshots, got $COUNT"
  exit 1
}
