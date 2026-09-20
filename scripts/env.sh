#!/usr/bin/env bash
# Shared tool discovery; works with a standard SDK install and Java 17 on PATH.
REACHPAD_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REACHPAD_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
if [[ -n "${REACHPAD_JAVA:-${JAVA_HOME:-}}" ]]; then
    export JAVA_HOME="${REACHPAD_JAVA:-$JAVA_HOME}"
    export PATH="$JAVA_HOME/bin:$PATH"
fi
REACHPAD_TOOLS="$REACHPAD_SDK/build-tools/35.0.0"
REACHPAD_ANDROID="$REACHPAD_SDK/platforms/android-36/android.jar"
REACHPAD_ADB="$REACHPAD_SDK/platform-tools/adb"
