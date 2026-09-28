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
  let d=f.contentDocument;
  let w=f.contentWindow;

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
  const reloadGolden=async()=>{
    await new Promise(resolve=>{
      f.addEventListener('load',resolve,{once:true});
      w.location.reload();
    });
    await sleep(420);
    d=f.contentDocument;
    w=f.contentWindow;
  };
  const makeReadyAt=async i=>{
    const raw=w.localStorage.getItem('ceilingMeasureData');
    if(!raw)return;
    const data=JSON.parse(raw);
    const order=data.orders?.[0];
    const room=order?.rooms?.[i];
    if(!room)return;
    const p=[
      {id:'p_phase3_a',x:100,y:100},
      {id:'p_phase3_b',x:300,y:100},
      {id:'p_phase3_c',x:300,y:300},
      {id:'p_phase3_d',x:100,y:300}
    ];
    room.points=p;
    room.edges=[
      {id:'e_phase3_ab',a:p[0].id,b:p[1].id,mm:'500'},
      {id:'e_phase3_bc',a:p[1].id,b:p[2].id,mm:'500'},
      {id:'e_phase3_cd',a:p[2].id,b:p[3].id,mm:'500'},
      {id:'e_phase3_da',a:p[3].id,b:p[0].id,mm:'500'}
    ];
    room.diagList=[];
    w.localStorage.setItem('ceilingMeasureData',JSON.stringify(data));
    await reloadGolden();
    click('#homeDrawing');
    await sleep(180);
    selectIndex(i);
    await sleep(120);
  };
  const longPressRoom=async i=>{
    const b=d.querySelectorAll('[data-r]')[i];
    if(!b)return;
    const EventCtor=w.PointerEvent||w.MouseEvent;
    b.dispatchEvent(new EventCtor('pointerdown',{
      bubbles:true,
      cancelable:true,
      pointerId:91,
      pointerType:'touch',
      isPrimary:true,
      button:0,
      buttons:1,
      clientX:12,
      clientY:12
    }));
    await sleep(380);
    b.dispatchEvent(new EventCtor('pointerup',{
      bubbles:true,
      cancelable:true,
      pointerId:91,
      pointerType:'touch',
      isPrimary:true,
      button:0,
      buttons:0,
      clientX:12,
      clientY:12
    }));
    await sleep(160);
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
    fill(); severalRooms(); await makeReadyAt(2);
  }else if(state==='06_room_picker'){
    fill(); severalRooms(); click('#addRoom');
  }else if(state==='07_room_action_sheet'){
    fill(); severalRooms(); await longPressRoom(1);
  }else if(state==='08_room_rename_keyboard'){
    fill(); severalRooms(); await longPressRoom(1);
    click('[data-alpha-action="clear"]');
    click('[data-alpha-key="з"]');
    click('[data-alpha-key="а"]');
    click('[data-alpha-key="л"]');
  }else if(state==='09_order_collapsed'){
    fill(); severalRooms();
    click('[data-collapse-toggle="order"]');
    await sleep(220);
  }else if(state==='10_rooms_collapsed'){
    fill(); severalRooms();
    click('[data-collapse-toggle="rooms"]');
    await sleep(220);
  }else if(state==='11_delete_order_confirm'){
    fill(); severalRooms(); click('#delOrder');
  }else if(state==='12_delete_room_confirm'){
    fill(); severalRooms(); await longPressRoom(1);
    click('#roomDeleteFromMenu');
  }

  await sleep(300);

  const required={
    '05_room_ready':()=>!!d.querySelector('.room-ready'),
    '06_room_picker':()=>d.querySelector('#roomPicker')?.getAttribute('aria-hidden')==='false',
    '07_room_action_sheet':()=>d.querySelector('#roomActionSheet')?.getAttribute('aria-hidden')==='false',
    '08_room_rename_keyboard':()=>d.querySelector('#roomActionSheet')?.getAttribute('aria-hidden')==='false'
      && d.querySelector('#roomRenameValue')?.value==='Зал',
    '09_order_collapsed':()=>d.querySelector('[data-collapse-block="order"]')?.classList.contains('collapsed'),
    '10_rooms_collapsed':()=>d.querySelector('[data-collapse-block="rooms"]')?.classList.contains('collapsed'),
    '11_delete_order_confirm':()=>d.querySelector('#miniConfirm')?.getAttribute('aria-hidden')==='false'
      && d.querySelector('#miniConfirmTitle')?.textContent?.includes('Удалить заказ?'),
    '12_delete_room_confirm':()=>d.querySelector('#miniConfirm')?.getAttribute('aria-hidden')==='false'
      && d.querySelector('#miniConfirmTitle')?.textContent?.includes('Удалить комнату')
  };
  const check=required[state];
  if(check&&!check()){
    document.documentElement.dataset.phase3ReferenceError='state-not-realized-'+state;
  }
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
  local dom="$OUT/web-$width-x-$height-$state-dom.txt"
  "$CHROME" \
    --headless=new \
    --no-sandbox \
    --disable-gpu \
    --allow-file-access-from-files \
    --force-device-scale-factor=1 \
    --virtual-time-budget=4500 \
    --window-size="$width,$height" \
    --dump-dom "file://$file" > "$dom" 2>/tmp/phase3-chrome-dom.log
  if grep -q 'data-phase3-reference-error=' "$dom"; then
    echo "Phase-3 golden state was not realized: $state at $width px"
    grep -o 'data-phase3-reference-error="[^"]*"' "$dom" || true
    exit 1
  fi
  rm -f "$dom"
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
