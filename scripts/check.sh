#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/android-env.sh"
cd "$PLEOS_ROOT"
exec ./gradlew verifyArchitecture :core:test :data-local:testDebugUnitTest :handoff-zxing:test \
  :app:testDebugUnitTest :app:assembleDebug :app:lintDebug \
  :data-local:lintDebug :route-map-api:lintDebug :route-map-maplibre:lintDebug --console=plain "$@"
