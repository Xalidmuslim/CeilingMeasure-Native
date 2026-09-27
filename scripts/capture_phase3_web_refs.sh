#!/usr/bin/env bash
set -euo pipefail

EXPECTED="354fbede167490d2481d3253507ff2829dcefec495298075487880ea2258a388"
ACTUAL="$(sha256sum webapp/index.html | awk '{print $1}')"
[[ "$ACTUAL" == "$EXPECTED" ]] || {
  echo "Golden SHA mismatch: $ACTUAL"
  exit 1
}
python3 scripts/verify_webapp.py webapp/index.html

CHROME=""
for candidate in google-chrome google-chrome-stable chromium chromium-browser; do
  if command -v "$candidate" >/dev/null 2>&1; then
    CHROME="$(command -v "$candidate")"
    break
  fi
done
[[ -n "$CHROME" ]] || {
  echo "A Chromium/Chrome binary is required for Phase 3 reference screenshots"
  exit 1
}

ROOT="$(pwd)"
OUT="$ROOT/build/phase3-web-reference"
TMP="$OUT/tmp"
rm -rf "$OUT"
mkdir -p "$TMP"
GOLDEN_URI="file://$ROOT/webapp/index.html"

make_wrapper() {
  local state="$1"
  local width="$2"
  local height="$3"
  local file="$TMP/$state-$width.html"
  cat > "$file" <<EOF
<!doctype html>
<meta charset="utf-8">
<style>
html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#fff}
iframe{position:fixed;left:0;top:0;width:${width}px;height:${height}px;border:0}
</style>
<iframe id="f" src="$GOLDEN_URI"></iframe>
<script>
const state="$state";
const f=document.getElementById('f');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
let handled=false;
f.addEventListener('load',async()=>{
  if(handled)return;
  handled=true;
  await sleep(320);
  const d=f.contentDocument;
  const w=f.contentWindow;

  const click=selector=>d.querySelector(selector)?.click();
  const setInput=(selector,value)=>{
    const el=d.querySelector(selector);
    if(!el)return;
    el.value=value;
    el.dispatchEvent(new Event('input',{bubbles:true}));
  };
  const fresh=()=>{ click('#homeNewOrder'); };
  const fill=()=>{
    setInput('[data-ok="name"]','Квартира 189');
    setInput('[data-ok="phone"]','+7 900 000-00-00');
    setInput('[data-ok="address"]','Нальчик');
  };
  const addRoom=type=>{
    click('#addRoom');
    click('[data-room-type="'+type+'"]');
  };
  const severalRooms=()=>{
    addRoom('Коридор');
    addRoom('Ванная');
    addRoom('Другое');
  };
  const selectIndex=i=>{
    const list=d.querySelectorAll('[data-r]');
    list[i]?.click();
  };
  const makeReady=()=>{
    const r=w.curRoom?.();
    if(!r)return;
    const p=[
      {id:'p_phase3_a',x:100,y:100},
      {id:'p_phase3_b',x:300,y:100},
      {id:'p_phase3_c',x:300,y:300},
      {id:'p_phase3_d',x:100,y:300}
    ];
    r.points=p;
    r.edges=[
      {id:'e_phase3_ab',a:p[0].id,b:p[1].id,mm:'500'},
      {id:'e_phase3_bc',a:p[1].id,b:p[2].id,mm:'500'},
      {id:'e_phase3_cd',a:p[2].id,b:p[3].id,mm:'500'},
      {id:'e_phase3_da',a:p[3].id,b:p[0].id,mm:'500'}
    ];
    r.diagList=[];
    w.save?.();
    w.renderRooms?.();
    w.renderRoom?.();
  };
  const openActions=i=>{
    const list=d.querySelectorAll('[data-r]');
    const id=list[i]?.dataset?.r;
    if(id)w.openRoomActions?.(id);
  };

  fresh();
  await sleep(100);

  if(state==='01_fresh_order'){
  }else if(state==='02_order_fields'){
    fill();
  }else if(state==='03_four_rooms'){
    fill(); severalRooms();
  }else if(state==='04_active_non_first'){
    fill(); severalRooms(); selectIndex(2);
  }else if(state==='05_room_ready'){
    fill(); severalRooms(); selectIndex(2); makeReady();
  }else if(state==='06_room_picker'){
    fill(); severalRooms(); click('#addRoom');
  }else if(state==='07_room_action_sheet'){
    fill(); severalRooms(); openActions(1);
  }else if(state==='08_room_rename_keyboard'){
    fill(); severalRooms(); openActions(1);
    await sleep(80);
    click('[data-alpha-action="clear"]');
    click('[data-alpha-key="з"]');
    click('[data-alpha-key="а"]');
    click('[data-alpha-key="л"]');
  }else if(state==='09_order_collapsed'){
    fill(); severalRooms();
    w.setCollapseBlockState?.('order',true,{animate:false});
  }else if(state==='10_rooms_collapsed'){
    fill(); severalRooms();
    w.setCollapseBlockState?.('rooms',true,{animate:false});
  }else if(state==='11_delete_order_confirm'){
    fill(); severalRooms(); click('#delOrder');
  }else if(state==='12_delete_room_confirm'){
    fill(); severalRooms(); openActions(1);
    await sleep(80);
    click('#roomDeleteFromMenu');
  }

  await sleep(300);
});
</script>
EOF
}

capture() {
  local state="$1"
  local width="$2"
  local height="$3"
  make_wrapper "$state" "$width" "$height"
  local file="$TMP/$state-$width.html"
  "$CHROME" \
    --headless=new \
    --no-sandbox \
    --disable-gpu \
    --hide-scrollbars \
    --allow-file-access-from-files \
    --force-device-scale-factor=1 \
    --run-all-compositor-stages-before-draw \
    --virtual-time-budget=4500 \
    --window-size="$width,$height" \
    --screenshot="$OUT/web-$width-x-$height-$state.png" \
    "file://$file" >/tmp/phase3-chrome.log 2>&1
  test -s "$OUT/web-$width-x-$height-$state.png"
}

states=(
  01_fresh_order
  02_order_fields
  03_four_rooms
  04_active_non_first
  05_room_ready
  06_room_picker
  07_room_action_sheet
  08_room_rename_keyboard
  09_order_collapsed
  10_rooms_collapsed
  11_delete_order_confirm
  12_delete_room_confirm
)

for width in 360 393 412; do
  for state in "${states[@]}"; do
    capture "$state" "$width" 852
  done
done

rm -rf "$TMP"
printf 'golden_sha=%s\n' "$ACTUAL" > "$OUT/reference_manifest.txt"
printf 'chrome=%s\n' "$("$CHROME" --version)" >> "$OUT/reference_manifest.txt"
printf 'widths=360,393,412\n' >> "$OUT/reference_manifest.txt"
printf 'states=%s\n' "${#states[@]}" >> "$OUT/reference_manifest.txt"
printf 'screenshots=%s\n' "$(find "$OUT" -maxdepth 1 -name '*.png' | wc -l)" >> "$OUT/reference_manifest.txt"

COUNT="$(find "$OUT" -maxdepth 1 -name '*.png' | wc -l)"
[[ "$COUNT" -ge 36 ]] || {
  echo "Expected 36 Phase-3 golden screenshots, got $COUNT"
  exit 1
}
echo "Phase 3 web-reference screenshots: $COUNT"
