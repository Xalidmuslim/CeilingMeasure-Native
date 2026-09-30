#!/usr/bin/env bash
set -euo pipefail

ROOT="$PWD"
TOOLS="$ROOT/.render-tools"
SDK="$TOOLS/android-sdk"
OUT="$ROOT/public-apk"
mkdir -p "$TOOLS" "$SDK/cmdline-tools" "$OUT"

echo "==> Installing JDK 17"
curl -fL --retry 3 --retry-delay 2   "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"   -o "$TOOLS/jdk17.tar.gz"
mkdir -p "$TOOLS/jdk17"
tar -xzf "$TOOLS/jdk17.tar.gz" -C "$TOOLS/jdk17" --strip-components=1
export JAVA_HOME="$TOOLS/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
java -version

echo "==> Installing Android SDK command line tools"
curl -fL --retry 3 --retry-delay 2   "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"   -o "$TOOLS/android-cmdline.zip"
unzip -q "$TOOLS/android-cmdline.zip" -d "$SDK/cmdline-tools"
rm -rf "$SDK/cmdline-tools/latest"
mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
export ANDROID_SDK_ROOT="$SDK"
export ANDROID_HOME="$SDK"
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

echo "==> Installing Gradle 8.9"
curl -fL --retry 3 --retry-delay 2   "https://services.gradle.org/distributions/gradle-8.9-bin.zip"   -o "$TOOLS/gradle-8.9-bin.zip"
unzip -q "$TOOLS/gradle-8.9-bin.zip" -d "$TOOLS"
export GRADLE_USER_HOME="$TOOLS/gradle-home"

echo "==> Building native v204"
"$TOOLS/gradle-8.9/bin/gradle" --no-daemon   clean lintDebug testDebugUnitTest assembleDebug assembleDebugAndroidTest

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
test -s "$APK"
test -s "$TEST_APK"

if unzip -l "$APK" | grep -q "assets/index.html"; then
  echo "FAIL: HTML asset found in native APK"
  exit 1
fi

cp "$APK" "$OUT/CeilingMeasure-native-v204.apk"
cp "$TEST_APK" "$OUT/CeilingMeasure-native-v204-androidTest.apk"
(
  cd "$OUT"
  sha256sum CeilingMeasure-native-v204.apk > CeilingMeasure-native-v204.apk.sha256
)

cat > "$OUT/index.html" <<'EOF'
<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>CeilingMeasure native v204</title>
</head>
<body>
<h1>CeilingMeasure native v204</h1>
<p><a href="CeilingMeasure-native-v204.apk">Скачать APK v204</a></p>
<p><a href="CeilingMeasure-native-v204.apk.sha256">SHA-256</a></p>
</body>
</html>
EOF

echo "==> v204 build complete"
ls -lh "$OUT"
