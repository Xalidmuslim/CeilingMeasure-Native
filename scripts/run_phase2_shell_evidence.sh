#!/usr/bin/env bash
set -euo pipefail

PACKAGE="ru.xalidmuslim.ceilingmeasure"
TEST_PACKAGE="ru.xalidmuslim.ceilingmeasure.test"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
OUT="build/phase2-native-evidence"

adb wait-for-device
for _ in $(seq 1 90); do
  if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)" == "1" ]]; then
    break
  fi
  sleep 2
done

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# Phase-2 screenshot evidence must contain the app itself, not transient
# system/launcher ANR overlays caused by the heavily loaded headless emulator.
# This does not alter application behavior or test assertions.
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put global anr_show_background 0 || true

adb shell rm -rf "/sdcard/Android/data/$PACKAGE/files/phase2" || true
rm -rf "$OUT"
mkdir -p "$OUT"

run_width() {
  local density="$1"
  local expected="$2"
  local log="/tmp/phase2-$expected.txt"

  echo "=== Phase 2 evidence: target width ~$expected dp, density $density ==="
  adb shell wm density "$density"
  sleep 2
  adb shell am force-stop "$PACKAGE" || true
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  sleep 1

  if adb shell dumpsys window windows 2>/dev/null | grep -Eqi 'AppErrorDialog|Application Not Responding'; then
    echo "System error dialog is visible before Phase-2 instrumentation"
    adb shell dumpsys window windows | grep -Ei 'AppErrorDialog|Application Not Responding|mCurrentFocus' || true
    exit 1
  fi

  adb shell am instrument -w -r     -e class ru.xalidmuslim.ceilingmeasure.NativePhase2ShellTest     -e phase2Width "$expected"     "$TEST_PACKAGE/$RUNNER" | tee "$log"

  grep -Eq "OK \([1-9][0-9]* tests\)" "$log"
  if grep -q "FAILURES!!!" "$log"; then
    exit 1
  fi
}

collect_evidence() {
  local remote="/sdcard/Android/data/$PACKAGE/files/phase2"
  mkdir -p "$OUT"
  adb pull "$remote" "$OUT/pulled" >/dev/null 2>&1 || true
  find "$OUT" -name '*metrics.txt' -type f -print -exec cat {} \; 2>/dev/null || true
}

cleanup() {
  local code=$?
  collect_evidence
  adb shell wm density reset >/dev/null 2>&1 || true
  adb shell settings delete global hide_error_dialogs >/dev/null 2>&1 || true
  adb shell settings delete global anr_show_background >/dev/null 2>&1 || true
  exit "$code"
}
trap cleanup EXIT
run_width 480 360
run_width 440 393
run_width 419 412
adb shell wm density reset
sleep 2

collect_evidence
COUNT="$(find "$OUT" -name '*.png' | wc -l)"
echo "Native Phase 2 screenshots: $COUNT"
[[ "$COUNT" -ge 21 ]] || {
  echo "Expected at least 21 native screenshots across 360/393/412dp"
  find "$OUT" -maxdepth 4 -type f -print || true
  exit 1
}

find "$OUT" -name '*metrics.txt' -type f -print -exec cat {} \;
