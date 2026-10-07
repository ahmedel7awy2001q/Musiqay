#!/usr/bin/env bash
set -euo pipefail
collect() {
  mkdir -p out/screenshots
  adb pull /sdcard/Android/data/com.musiqay.app.preview/files/screenshots/ out/screenshots/ || true
  adb logcat -d -s AndroidRuntime:E > out/device-errors.txt || true
}
trap collect EXIT
adb shell settings put system font_scale 1.3
./gradlew --no-daemon -PpreviewBuild=true :app:connectedDebugAndroidTest
