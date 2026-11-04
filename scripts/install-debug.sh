#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/android-env.sh"
serial="${ANDROID_SERIAL:-$(adb get-serialno)}"
if [[ "$serial" != emulator-* ]]; then
  echo '실행 중인 에뮬레이터를 선택하세요. 예: ANDROID_SERIAL=emulator-5554 ./scripts/install-debug.sh' >&2
  exit 1
fi
cd "$PLEOS_ROOT"
./gradlew :app:assembleDebug
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" shell am start -n com.example.chabi/.MainActivity
