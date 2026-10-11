#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
mock_root="$(mktemp -d)"
trap 'rm -rf "$mock_root"' EXIT

cat > "$mock_root/gradle" <<'MOCK_GRADLE'
#!/usr/bin/env bash
exit 0
MOCK_GRADLE

cat > "$mock_root/adb" <<'MOCK_ADB'
#!/usr/bin/env bash
set -euo pipefail
case "$*" in
  'wait-for-device') exit 0 ;;
  'shell getprop sys.boot_completed') echo 1 ;;
  'shell pm path android') echo package:/system/framework/framework-res.apk ;;
  'install -r '*) echo Success ;;
  'shell pm list instrumentation')
    echo "instrumentation:$FAKE_TARGET_PACKAGE.test/androidx.test.runner.AndroidJUnitRunner (target=$FAKE_TARGET_PACKAGE)" ;;
  'shell am instrument -w -r '*)
    if [[ "$*" == "shell am instrument -w -r $FAKE_TARGET_PACKAGE.test/androidx.test.runner.AndroidJUnitRunner" ]]; then
      echo 'OK (2 tests)'
    else
      echo "Wrong test runner selected: $*" >&2
      exit 1
    fi ;;
  *) echo "Unexpected adb call: $*" >&2; exit 2 ;;
esac
MOCK_ADB
chmod +x "$mock_root/adb" "$mock_root/gradle"

for target in ru.xalidmuslim.ceilingmeasure ru.xalidmuslim.ceilingmeasure.preview; do
  echo "Checking instrumented test selection for: $target"
  (
    cd "$repo_root"
    FAKE_TARGET_PACKAGE="$target" PATH="$mock_root:$PATH" \
      bash scripts/run_android_smoke.sh > "$mock_root/result.log" 2>&1
  ) || {
    cat "$mock_root/result.log" >&2
    exit 1
  }
  grep -q 'OK (2 tests)' "$mock_root/result.log"
done

echo 'Instrumentation launcher regression checks passed.'
