#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
REACHPAD_OUT="$REACHPAD_ROOT/build/native-probe"
mkdir -p "$REACHPAD_OUT/classes" "$REACHPAD_OUT/dex"
javac -encoding UTF-8 -source 8 -target 8 -cp "$REACHPAD_SDK/platforms/android-36/android.jar" -d "$REACHPAD_OUT/classes" "$REACHPAD_ROOT"/experiments/native-screen/*.java
jar cf "$REACHPAD_OUT/classes.jar" -C "$REACHPAD_OUT/classes" .
"$REACHPAD_SDK/build-tools/35.0.0/d8" --min-api 30 --lib "$REACHPAD_SDK/platforms/android-36/android.jar" --output "$REACHPAD_OUT/dex" "$REACHPAD_OUT/classes.jar"
jar cf "$REACHPAD_OUT/native-probe.jar" -C "$REACHPAD_OUT/dex" classes.dex
printf 'Built %s\n' "$REACHPAD_OUT/native-probe.jar"
