#!/usr/bin/env bash
set -euo pipefail

gradle --no-daemon assembleDebugAndroidTest

adb wait-for-device
echo "Waiting for Android boot and package manager..."

ready=0
for _ in $(seq 1 90); do
  boot="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  if [[ "$boot" == "1" ]] && adb shell pm path android >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done

if [[ "$ready" != "1" ]]; then
  echo "Android package manager did not become ready"
  adb devices -l || true
  adb shell getprop sys.boot_completed || true
  exit 1
fi

install_with_retry() {
  local apk="$1"
  local attempt

  for attempt in 1 2 3; do
    echo "Installing $apk (attempt $attempt/3)"
    if adb install -r "$apk"; then
      return 0
    fi

    echo "Install failed; restarting adb before retry"
    adb kill-server || true
    adb start-server
    adb wait-for-device
    sleep 5
  done

  echo "Failed to install $apk after 3 attempts"
  adb devices -l || true
  return 1
}

install_with_retry app/build/outputs/apk/debug/app-debug.apk
install_with_retry app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# Resolve the installed instrumentation component instead of assuming a fixed
# applicationId. Debug variants can add suffixes such as ".preview".
installed_instrumentation="$(adb shell pm list instrumentation | tr -d '\r')"
instrumentation_pattern='^instrumentation:([^[:space:]]+)[[:space:]]+\(target=([^)]+)\)

grep -Eq "OK \([1-9][0-9]* tests\)" /tmp/instrumentation.txt

if grep -q "FAILURES!!!" /tmp/instrumentation.txt; then
  exit 1
fi

target_pattern='^ru[.]xalidmuslim[.]ceilingmeasure([.][A-Za-z0-9_]+)*

grep -Eq "OK \([1-9][0-9]* tests\)" /tmp/instrumentation.txt

if grep -q "FAILURES!!!" /tmp/instrumentation.txt; then
  exit 1
fi

runner_components=()
while IFS= read -r line; do
  if [[ "$line" =~ $instrumentation_pattern ]]; then
    component="${BASH_REMATCH[1]}"
    target="${BASH_REMATCH[2]}"
    if [[ "$target" =~ $target_pattern && "$component" == "${target}.test/androidx.test.runner.AndroidJUnitRunner" ]]; then
      runner_components+=("$component")
    fi
  fi
done <<< "$installed_instrumentation"

if [[ "${#runner_components[@]}" -ne 1 ]]; then
  echo "Expected exactly one CeilingMeasure AndroidJUnitRunner; found ${#runner_components[@]}" >&2
  printf '%s\n' "$installed_instrumentation" >&2
  exit 1
fi

adb shell am instrument -w -r "${runner_components[0]}" | tee /tmp/instrumentation.txt

grep -Eq "OK \([1-9][0-9]* tests\)" /tmp/instrumentation.txt

if grep -q "FAILURES!!!" /tmp/instrumentation.txt; then
  exit 1
fi
