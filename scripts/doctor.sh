#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/android-env.sh"
java -version
echo "Android SDK: $ANDROID_HOME"
for item in platforms/android-36/android.jar build-tools/35.0.0/aapt2 platform-tools/adb emulator/emulator; do
  if [[ -f "$ANDROID_HOME/$item" ]]; then
    echo "OK      $item"
  else
    echo "MISSING $item" >&2
    exit 1
  fi
done
if [[ -f "$ANDROID_HOME/system-images/android-34/pleos_car_emulator/arm64-v8a/system.img" ||
      -f "$ANDROID_HOME/system-images/android-34/android-automotive/arm64-v8a/devices.xml" ]]; then
  echo 'OK      Pleos Connect ARM64 image'
else
  echo 'MISSING Pleos image — docs/DEVELOPMENT.md의 에뮬레이터 설치 절차를 확인하세요.'
fi
emulator -list-avds
adb devices -l
