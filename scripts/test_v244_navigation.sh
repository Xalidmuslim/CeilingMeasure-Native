#!/usr/bin/env bash
set -euo pipefail

PREVIEW_APK="${1:-build/release/app-releasePreview.apk}"
DEBUG_APK="${2:-build/debug/app-debug.apk}"
TEST_APK="${3:-build/androidTest/app-debug-androidTest.apk}"
OUT="build/v246-evidence"
mkdir -p "$OUT"
adb wait-for-device
adb install -r "$DEBUG_APK"
adb install -r "$TEST_APK"

# Focused instrumentation: exact saved project opens its drawing, no wrong fallback,
# and Materials dialog is attached immediately with no home-only fade.
TEST_RUNNER="ru.xalidmuslim.ceilingmeasure.preview.test/androidx.test.runner.AndroidJUnitRunner"
adb shell pm list instrumentation | tee "$OUT/instrumentation.txt"
adb shell am instrument -w -r \
  -e class ru.xalidmuslim.ceilingmeasure.NativeHomeDirectNavigationTest \
  "$TEST_RUNNER" | tee "$OUT/focused-tests.txt"
grep -Eq 'OK \([3-9] tests\)' "$OUT/focused-tests.txt"

# Settings from both Home entries: one fade layer, no translation, and
# Android Back must return to the exact same Home + scroll offset.
adb shell am instrument -w -r \
  -e class ru.xalidmuslim.ceilingmeasure.NativeHomeSettingsMotionTest \
  "$TEST_RUNNER" | tee "$OUT/home-settings-tests.txt"
grep -Eq 'OK \(2 tests\)' "$OUT/home-settings-tests.txt"


# R8-optimized release-preview: verify home->Materials actual UI route on Android.
adb install -r "$PREVIEW_APK"
PKG="ru.xalidmuslim.ceilingmeasure.optimized"
ACT="$PKG/ru.xalidmuslim.ceilingmeasure.NativeHomeActivity"
adb shell am force-stop "$PKG"
adb shell am start -W -n "$ACT" | tee "$OUT/optimized-home-start.txt"
adb shell uiautomator dump /sdcard/v244-home.xml >/dev/null 2>&1
adb exec-out cat /sdcard/v244-home.xml > "$OUT/home.xml"
adb exec-out screencap -p > "$OUT/home.png"
python3 - "$OUT/home.xml" > "$OUT/materials-tap.txt" <<'PY'
import re, sys, xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter("node"))
candidates=[n for n in nodes if n.get("content-desc")=="Материалы"]
assert len(candidates)==1, "Materials must only be present in bottom navigation"
vals=list(map(int,re.findall(r"\d+",candidates[0].get("bounds",""))))
assert len(vals)==4
print((vals[0]+vals[2])//2,(vals[1]+vals[3])//2)
PY
read -r x y < "$OUT/materials-tap.txt"
adb shell input tap "$x" "$y"
sleep 1
adb shell uiautomator dump /sdcard/v244-materials.xml >/dev/null 2>&1
adb exec-out cat /sdcard/v244-materials.xml > "$OUT/materials.xml"
adb exec-out screencap -p > "$OUT/materials.png"
python3 - "$OUT/materials.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
strings=[n.get("text","") for n in ET.parse(sys.argv[1]).getroot().iter("node")]
assert any("Закупочный прайс" in x for x in strings), "Materials dialog was not opened"
assert not any(x=="Комнаты" for x in strings), "Underlying editor shown instead of Materials"
print("Optimized home->Materials route: PASS")
PY
# End-to-end Android Back: actual foreground window must be the original Home,
# not the intermediate drawing editor hidden under the full-screen price dialog.
adb shell input keyevent KEYCODE_BACK
sleep 1
adb shell uiautomator dump /sdcard/v244-return-home.xml >/dev/null 2>&1
adb exec-out cat /sdcard/v244-return-home.xml > "$OUT/after-back.xml"
adb exec-out screencap -p > "$OUT/after-back.png"
python3 - "$OUT/after-back.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
texts = [node.get("text", "") for node in nodes]
assert any("Новый замер" in text for text in texts), (
    "After Back expected Home primary action, not the hidden drawing editor")
assert any("Последние замеры" in text for text in texts), (
    "After Back expected Home project list")
assert not any("Закупочный прайс" in text for text in texts), (
    "Materials dialog still visible after Android Back")
print("Optimized Materials->Android Back->Home: PASS")
PY
# Measure and inspect Home -> Settings -> Back on the real R8-optimized APK.
adb shell dumpsys gfxinfo "$PKG" reset > "$OUT/settings-gfx-reset.txt"
adb shell uiautomator dump /sdcard/v246-home-settings.xml >/dev/null 2>&1
adb exec-out cat /sdcard/v246-home-settings.xml > "$OUT/home-before-settings.xml"
python3 - "$OUT/home-before-settings.xml" > "$OUT/settings-tap.txt" <<'PY'
import re, sys, xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
buttons = [n for n in nodes if n.get("content-desc") == "Настройки"]
assert buttons, "Settings navigation not available on Home"
bounds = list(map(int, re.findall(r"\d+", buttons[-1].get("bounds", ""))))
assert len(bounds) == 4, "Settings navigation has invalid bounds"
print((bounds[0]+bounds[2])//2, (bounds[1]+bounds[3])//2)
PY
read -r x y < "$OUT/settings-tap.txt"
adb shell input tap "$x" "$y"
sleep .4
adb shell uiautomator dump /sdcard/v246-settings.xml >/dev/null 2>&1
adb exec-out cat /sdcard/v246-settings.xml > "$OUT/settings.xml"
adb exec-out screencap -p > "$OUT/settings-open.png"
python3 - "$OUT/settings.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
texts = [n.get("text", "") for n in
         ET.parse(sys.argv[1]).getroot().iter("node")]
assert "Размер текста" in texts, "Settings dialog did not render correctly"
assert "Жирность" in texts, "Settings preferences disappeared"
print("Optimized Home -> Settings: PASS")
PY
adb shell input keyevent KEYCODE_BACK
sleep .5
adb shell uiautomator dump /sdcard/v246-home-after-settings.xml >/dev/null 2>&1
adb exec-out cat /sdcard/v246-home-after-settings.xml > "$OUT/home-after-settings.xml"
adb exec-out screencap -p > "$OUT/home-after-settings.png"
python3 - "$OUT/home-after-settings.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
texts = [n.get("text", "") for n in
         ET.parse(sys.argv[1]).getroot().iter("node")]
assert "Последние замеры" in texts, "Settings Back did not restore Home"
assert "Новый замер" in texts, "Home primary action missing after Settings Back"
assert "Размер текста" not in texts, "Settings dialog remained after Back"
print("Optimized Settings -> Android Back -> Home: PASS")
PY
adb shell dumpsys gfxinfo "$PKG" framestats > "$OUT/settings-gfx-framestats.txt"
adb shell dumpsys activity activities > "$OUT/after-back-activities.txt"
adb logcat -d -s AndroidRuntime:E > "$OUT/crashes.txt"
if grep -E "FATAL EXCEPTION|Process: $PKG" "$OUT/crashes.txt"; then
  echo "App crashed" >&2; exit 1
fi
echo 'Focused UI routing checks complete'
