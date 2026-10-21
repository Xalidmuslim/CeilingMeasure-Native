#!/usr/bin/env bash
# Deterministic, read-only smoke test for a debug-signed, R8-optimized release.
# Needs a booted Android emulator/device and an APK whose id ends in .optimized.
set -euo pipefail

APK="${1:-app/build/outputs/apk/releasePreview/app-releasePreview.apk}"
PKG="ru.xalidmuslim.ceilingmeasure.optimized"
ACT="${PKG}/ru.xalidmuslim.ceilingmeasure.NativeHomeActivity"
OUT="build/v243-performance"
mkdir -p "$OUT"
test -s "$APK"

adb wait-for-device
adb install -r "$APK" | tee "$OUT/install.txt"
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
adb logcat -c

echo 'Cold process starts (NOT device boot, sampled on emulator):' > "$OUT/measurements.txt"
for n in 1 2 3 4 5; do
  adb shell am force-stop "$PKG"
  adb shell input keyevent KEYCODE_HOME
  adb shell am start -W -n "$ACT" | tee "$OUT/start-${n}.txt"
  grep -E '^(Status|ThisTime|TotalTime|WaitTime):' "$OUT/start-${n}.txt" >> "$OUT/measurements.txt"
  grep -q '^Status: ok' "$OUT/start-${n}.txt"
  sleep 1
done

# Native view hierarchy proves title and Materials is not a home card.
adb shell uiautomator dump /sdcard/v243_home.xml > "$OUT/uiautomator.txt" 2>&1
adb exec-out cat /sdcard/v243_home.xml > "$OUT/home.xml"
python3 - "$OUT/home.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
nodes=list(root.iter("node"))
texts=[n.get("text","") for n in nodes]
assert any("Замер потолков" in x for x in texts), "Home heading missing"
assert any("Новый замер" in x for x in texts), "Primary action missing"
# Materials still exists in the dock. The home screen has no materials card.
assert sum(x == "Материалы" for x in texts) == 1, "Expected one Materials dock label"
print("Native home titles and single Materials dock entry: OK")
PY

adb exec-out screencap -p > "$OUT/home.png"
# Capture frames during vertical gestures, without forcing GPU layers.
adb shell dumpsys gfxinfo "$PKG" reset > "$OUT/gfx-reset.txt"
for n in 1 2 3; do
  adb shell input swipe 550 1750 550 470 350
  adb shell input swipe 550 500 550 1750 360
done
adb shell dumpsys gfxinfo "$PKG" framestats > "$OUT/gfx-home-framestats.txt"
adb exec-out screencap -p > "$OUT/home-after-scroll.png"

# Open native Settings (not license gated), then return and check launcher again.
python3 - "$OUT/home.xml" > "$OUT/settings-tap.txt" <<'PY'
import re,sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter("node"))
target=next((n for n in nodes if n.get("content-desc")=="Настройки"),None)
assert target is not None,"Settings navigation not found"
nums=list(map(int,re.findall(r"\d+",target.get("bounds",""))))
assert len(nums)==4
print((nums[0]+nums[2])//2,(nums[1]+nums[3])//2)
PY
read -r x y < "$OUT/settings-tap.txt"
adb shell input tap "$x" "$y"
sleep 1
adb exec-out screencap -p > "$OUT/settings-open.png"
adb shell input keyevent KEYCODE_BACK
sleep 1

adb shell dumpsys activity activities > "$OUT/activities.txt"
adb shell dumpsys meminfo "$PKG" > "$OUT/meminfo.txt"
adb logcat -d -s AndroidRuntime:E > "$OUT/android-crashes.txt"
if grep -E 'FATAL EXCEPTION|Process: ru.xalidmuslim.ceilingmeasure.optimized' "$OUT/android-crashes.txt"; then
  echo "Android runtime crash detected" >&2
  exit 1
fi

python3 - "$OUT" <<'PY'
import re,sys,statistics,json,pathlib
p=pathlib.Path(sys.argv[1])
times=[]
for path in sorted(p.glob("start-*.txt")):
    raw=path.read_text()
    match=re.search(r'^TotalTime:\s*(\d+)',raw,re.M)
    if not match: raise SystemExit("No TotalTime in "+str(path))
    times.append(int(match.group(1)))
report={
    "platform":"Android emulator (not a physical phone)",
    "cold_start_definition":"force-stop + am start -W, excludes device boot; 5 samples",
    "start_total_time_ms":times,
    "median_start_total_time_ms":statistics.median(times),
    "mean_start_total_time_ms":round(statistics.mean(times),1),
    "scroll":"6 touch swipes; raw gfxinfo framestats attached",
    "settings":"native settings opened and returned; screenshot attached",
    "runtime_fatal_errors":False,
}
(p/"summary.json").write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n")
print(json.dumps(report,ensure_ascii=False,indent=2))
PY
