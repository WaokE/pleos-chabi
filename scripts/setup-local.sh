#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/android-env.sh"
if [[ ! -f "$PLEOS_ROOT/local.properties" ]]; then
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$PLEOS_ROOT/local.properties"
fi
java -version
echo "Android SDK: $ANDROID_HOME"
echo "준비 완료. ./gradlew :app:assembleDebug :app:lintDebug"
