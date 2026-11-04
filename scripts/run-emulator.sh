#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/android-env.sh"
exec emulator -avd "${PLEOS_AVD:-Pleos_Connect_2_1_API_34}" \
  -no-snapshot -memory 4096 -gpu host "$@"
