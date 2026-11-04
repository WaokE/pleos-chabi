#!/usr/bin/env bash
# Shared by the helper scripts; does not change the user's shell configuration.
PLEOS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${ANDROID_HOME:-}" && -f "$PLEOS_ROOT/local.properties" ]]; then
  ANDROID_HOME="$(sed -n 's/^sdk.dir=//p' "$PLEOS_ROOT/local.properties" | head -1)"
fi
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [[ ! -d "$ANDROID_HOME/platform-tools" ]]; then
  echo "Android SDK를 찾을 수 없습니다. ANDROID_HOME을 설정하세요." >&2
  return 1
fi
if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi
export PATH="${JAVA_HOME:+$JAVA_HOME/bin:}$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
