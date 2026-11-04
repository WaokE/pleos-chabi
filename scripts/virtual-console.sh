#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/android-env.sh"
serial="${ANDROID_SERIAL:-$(adb get-serialno)}"
if [[ "$serial" != emulator-* ]]; then
  echo '실행 중인 Pleos 에뮬레이터를 ANDROID_SERIAL로 지정하세요.' >&2
  exit 1
fi
adb -s "$serial" forward tcp:8088 tcp:8088
echo 'Pleos Connect Virtual Console: http://127.0.0.1:8088/'
