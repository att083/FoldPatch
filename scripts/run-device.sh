#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
REACHPAD_DEVICE="${1:?Usage: bash scripts/run-device.sh DEVICE_IP:CONNECT_PORT}"
if [[ "$REACHPAD_DEVICE" == *:* ]]; then "$REACHPAD_ADB" connect "$REACHPAD_DEVICE"; fi
"$REACHPAD_ADB" -s "$REACHPAD_DEVICE" install --no-incremental -r "$REACHPAD_ROOT/build/foldpatch-debug.apk"
"$REACHPAD_ADB" -s "$REACHPAD_DEVICE" shell am start -n dev.reachpad/.NativeActivity
printf 'Installed. Use FoldPatch settings with Shizuku running on the phone.\n'
