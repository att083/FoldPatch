#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
REACHPAD_VARIANT="${1:-debug}"
case "$REACHPAD_VARIANT" in debug|release) ;; *) echo 'Usage: bash scripts/build.sh [debug|release]' >&2; exit 2;; esac
for tool in javac jar keytool python3 sha256sum; do command -v "$tool" >/dev/null || { echo "Missing tool: $tool" >&2; exit 1; }; done
[[ -f "$REACHPAD_ANDROID" && -x "$REACHPAD_TOOLS/aapt2" ]] || { echo 'Install Android SDK platform 36 and build-tools 35.0.0; set ANDROID_SDK_ROOT.' >&2; exit 1; }
(cd "$REACHPAD_ROOT/libs" && sha256sum --check SHA256SUMS)
REACHPAD_OUT="$REACHPAD_ROOT/build/$REACHPAD_VARIANT"
mkdir -p "$REACHPAD_OUT/classes" "$REACHPAD_OUT/dex" "$REACHPAD_OUT/generated" "$REACHPAD_ROOT/.local"
find "$REACHPAD_OUT/classes" "$REACHPAD_OUT/dex" "$REACHPAD_OUT/generated" -type f -delete
python3 "$REACHPAD_ROOT/scripts/prepare-manifest.py" "$REACHPAD_VARIANT" "$REACHPAD_OUT/AndroidManifest.xml"
"$REACHPAD_TOOLS/aapt2" compile --dir "$REACHPAD_ROOT/res" -o "$REACHPAD_OUT/resources.zip"
mkdir -p "$REACHPAD_OUT/assets/licenses"
cp "$REACHPAD_ROOT/LICENSE" "$REACHPAD_ROOT/THIRD_PARTY_NOTICES.md" "$REACHPAD_OUT/assets/"
cp "$REACHPAD_ROOT"/licenses/*.txt "$REACHPAD_OUT/assets/licenses/"
"$REACHPAD_TOOLS/aapt2" link --java "$REACHPAD_OUT/generated" -A "$REACHPAD_OUT/assets" -o "$REACHPAD_OUT/unsigned.apk" --manifest "$REACHPAD_OUT/AndroidManifest.xml" -I "$REACHPAD_ANDROID" "$REACHPAD_OUT/resources.zip"
find "$REACHPAD_ROOT/src" "$REACHPAD_OUT/generated" -name '*.java' > "$REACHPAD_OUT/sources.list"
if [[ "$REACHPAD_VARIANT" == debug ]]; then find "$REACHPAD_ROOT/tests/device" -name '*.java' >> "$REACHPAD_OUT/sources.list"; fi
javac -encoding UTF-8 -source 8 -target 8 -classpath "$REACHPAD_ANDROID:$REACHPAD_ROOT/libs/*" -d "$REACHPAD_OUT/classes" @"$REACHPAD_OUT/sources.list"
jar cf "$REACHPAD_OUT/classes.jar" -C "$REACHPAD_OUT/classes" .
"$REACHPAD_TOOLS/d8" --min-api 34 --lib "$REACHPAD_ANDROID" --output "$REACHPAD_OUT/dex" "$REACHPAD_OUT/classes.jar" "$REACHPAD_ROOT"/libs/*.jar
(cd "$REACHPAD_OUT/dex" && jar uf ../unsigned.apk classes.dex)
"$REACHPAD_TOOLS/zipalign" -f 4 "$REACHPAD_OUT/unsigned.apk" "$REACHPAD_OUT/aligned.apk"
if [[ "$REACHPAD_VARIANT" == debug ]]; then
    if [[ ! -f "$REACHPAD_ROOT/.local/debug.keystore" ]]; then
        keytool -genkeypair -keystore "$REACHPAD_ROOT/.local/debug.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=FoldPatch Debug' -keyalg RSA -validity 10000 >/dev/null 2>&1
    fi
    "$REACHPAD_TOOLS/apksigner" sign --ks "$REACHPAD_ROOT/.local/debug.keystore" --ks-pass pass:android --out "$REACHPAD_ROOT/build/foldpatch-debug.apk" "$REACHPAD_OUT/aligned.apk"
    "$REACHPAD_TOOLS/apksigner" verify "$REACHPAD_ROOT/build/foldpatch-debug.apk"
    echo "Built $REACHPAD_ROOT/build/foldpatch-debug.apk"
else
    # No implicit debug signing or key creation for a public release.
    cp "$REACHPAD_OUT/aligned.apk" "$REACHPAD_ROOT/build/foldpatch-release-unsigned.apk"
    echo "Built $REACHPAD_ROOT/build/foldpatch-release-unsigned.apk (not installable until signed)"
fi
