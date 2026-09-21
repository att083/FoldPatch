#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
mkdir -p "$REACHPAD_ROOT/build/tests"
javac -encoding UTF-8 -cp "$REACHPAD_ANDROID" -d "$REACHPAD_ROOT/build/tests" \
  "$REACHPAD_ROOT"/src/dev/foldpatch/{HangulComposer,PointerGeometry,TouchpadScroll,RangePreview,TouchSide,TrackpadGesture,ProcessShutdown,SetupFlow,ReflowPolicy}.java "$REACHPAD_ROOT"/tests/*.java
for test in HangulComposer PointerGeometry TouchpadScroll RangePreview TouchSide TrackpadGesture ProcessShutdown SetupFlow ReflowPolicy; do
    java -cp "$REACHPAD_ROOT/build/tests:$REACHPAD_ANDROID" "dev.foldpatch.${test}Test"
done
python3 "$REACHPAD_ROOT/scripts/check-release.py"

python3 "$REACHPAD_ROOT/scripts/check-locales.py"
