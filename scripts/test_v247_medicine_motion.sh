#!/usr/bin/env bash
# Runtime QA for Medicine Smooth Motion adaptation on an Android 15 emulator.
# Executes after the v246 navigation regression suite already installed the
# optimized APK, debug APK, and instrumentation APK.
set -euo pipefail
OUT="build/v247-performance"
PKG="ru.xalidmuslim.ceilingmeasure.optimized"
HOME_COMPONENT="$PKG/ru.xalidmuslim.ceilingmeasure.NativeHomeActivity"
WORKSPACE="$PKG/ru.xalidmuslim.ceilingmeasure.NativeWorkspaceActivity"
RUNNER="ru.xalidmuslim.ceilingmeasure.preview.test/androidx.test.runner.AndroidJUnitRunner"
mkdir -p "$OUT"

# 1. Synthetic and real logo: no white corners, artwork preserved, no layers.
adb shell am instrument -w -r \
    -e class ru.xalidmuslim.ceilingmeasure.NativeHomeIconTransparencyTest \
    "$RUNNER" | tee "$OUT/icon-tests.txt"
grep -Eq 'OK \(3 tests\)' "$OUT/icon-tests.txt"

# 2. Native motion constants, fixed transform and no forced GPU layer.
adb shell am instrument -w -r \
    -e class ru.xalidmuslim.ceilingmeasure.NativeMedicineMotionPolicyTest \
    "$RUNNER" | tee "$OUT/motion-tests.txt"
grep -Eq 'OK \(2 tests\)' "$OUT/motion-tests.txt"

# 3. Android's reduced/disabled animations must not dim Home on first frame.
adb shell settings put global animator_duration_scale 0
adb shell am instrument -w -r \
    -e class 'ru.xalidmuslim.ceilingmeasure.NativeMedicineMotionPolicyTest#respectsSystemReducedMotionWhenAnimatorsDisabled' \
    "$RUNNER" | tee "$OUT/reduced-motion.txt"
adb shell settings put global animator_duration_scale 1
grep -Eq 'OK \(1 test\)' "$OUT/reduced-motion.txt"

# 4. Five actual process-cold starts. Device boot is not included.
adb shell input keyevent KEYCODE_HOME
for n in 1 2 3 4 5; do
    adb shell am force-stop "$PKG"
    adb shell am start -W -n "$HOME_COMPONENT" | tee "$OUT/cold-start-${n}.txt"
    grep -q '^Status: ok' "$OUT/cold-start-${n}.txt"
    sleep .4
done
adb exec-out screencap -p > "$OUT/home.png"
adb shell uiautomator dump /sdcard/v247-home.xml >/dev/null 2>&1
adb exec-out cat /sdcard/v247-home.xml > "$OUT/home.xml"

# 5. Scroll the long real native editor; the Home only displays 3 recent rows.
adb shell am start -W -n "$WORKSPACE" --es native_launch_action drawing \
    | tee "$OUT/open-editor.txt"
adb shell uiautomator dump /sdcard/v247-editor.xml >/dev/null 2>&1
adb exec-out cat /sdcard/v247-editor.xml > "$OUT/editor.xml"
python3 - "$OUT/editor.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
texts=[n.get("text","") for n in ET.parse(sys.argv[1]).getroot().iter("node")]
assert any("Чертёж" in x or "Комнаты" in x for x in texts), "Drawing editor unavailable"
print("Native editor opened for scroll check")
PY
adb shell dumpsys gfxinfo "$PKG" reset > "$OUT/gfx-reset.txt"
for n in 1 2 3 4; do
    adb shell input swipe 70 1650 70 450 420
    adb shell input swipe 70 460 70 1650 440
done
sleep .6
adb shell dumpsys gfxinfo "$PKG" framestats > "$OUT/gfx-editor-framestats.txt"
adb exec-out screencap -p > "$OUT/editor-after-scroll.png"
adb shell input keyevent KEYCODE_BACK
sleep .3

# 6. Return to Home with no extra Activity and capture motion diagnostics.
adb shell dumpsys activity activities > "$OUT/activity-stacks.txt"
adb shell dumpsys meminfo "$PKG" > "$OUT/memory.txt"
adb logcat -d -s AndroidRuntime:E > "$OUT/runtime-errors.txt"
if grep -E 'FATAL EXCEPTION|Process: ru.xalidmuslim.ceilingmeasure.optimized' "$OUT/runtime-errors.txt"; then
  echo "Runtime crash detected" >&2
  exit 1
fi

python3 - "$OUT" <<'PY'
import pathlib,re,statistics,json,sys
p=pathlib.Path(sys.argv[1])
start=[]
for f in sorted(p.glob("cold-start-*.txt")):
    m=re.search(r'^TotalTime:\s*(\d+)',f.read_text(),re.M)
    if not m: raise SystemExit("Missing cold-start timing: "+str(f))
    start.append(int(m.group(1)))
raw=(p/"gfx-editor-framestats.txt").read_text()
rows=[]
in_profile=False
for line in raw.splitlines():
    if line.startswith("---PROFILEDATA---"):
        in_profile=not in_profile
        continue
    if not in_profile or not line.startswith("0,"): continue
    fields=line.split(",")
    if len(fields)<14: continue
    try:
        intended=int(fields[1]); completed=int(fields[13])
        if completed>=intended and intended>0:
            rows.append(round((completed-intended)/1e6,3))
    except (ValueError,IndexError): continue
report={
    "device":"Android API 35 emulator (not Xiaomi 13 Ultra)",
    "cold_start":"force-stop + am start -W; device boot excluded",
    "cold_start_total_time_ms":start,
    "cold_start_median_ms":statistics.median(start),
    "cold_start_mean_ms":round(statistics.mean(start),1),
    "editor_vertical_swipes":8,
    "editor_frame_samples":len(rows),
    "editor_frame_median_ms":statistics.median(rows) if rows else None,
    "editor_frames_above_16_67ms":sum(x>16.67 for x in rows),
    "runtime_crashes":0,
    "notes":"Profiling an emulator is not physical-phone smoothness proof. Original functionality remains covered by existing tests."
}
(p/"summary.json").write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n")
print(json.dumps(report,ensure_ascii=False,indent=2))
PY
