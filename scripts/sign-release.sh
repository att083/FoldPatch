#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
: "${REACHPAD_KEYSTORE:?Set the path to your release keystore}"
: "${REACHPAD_KEY_ALIAS:?Set the release key alias}"
: "${REACHPAD_STORE_PASS_FILE:?Set the path to the keystore password file}"
REACHPAD_KEY_PASS_ARGS=()
# apksigner consumes successive lines when the same password file is used twice.
# With a shared store/key password, omit --key-pass so it reuses the store password.
if [[ -n "${REACHPAD_KEY_PASS_FILE:-}" && "$REACHPAD_KEY_PASS_FILE" != "$REACHPAD_STORE_PASS_FILE" ]]; then
    REACHPAD_KEY_PASS_ARGS=(--key-pass "file:$REACHPAD_KEY_PASS_FILE")
fi
REACHPAD_INPUT="$REACHPAD_ROOT/build/foldpatch-release-unsigned.apk"
REACHPAD_OUTPUT="$REACHPAD_ROOT/build/foldpatch-release.apk"
[[ -f "$REACHPAD_INPUT" ]] || { echo 'Run bash scripts/build.sh release first.' >&2; exit 1; }
"$REACHPAD_TOOLS/apksigner" sign --ks "$REACHPAD_KEYSTORE" --ks-key-alias "$REACHPAD_KEY_ALIAS" --ks-pass "file:$REACHPAD_STORE_PASS_FILE" "${REACHPAD_KEY_PASS_ARGS[@]}" --out "$REACHPAD_OUTPUT" "$REACHPAD_INPUT"
"$REACHPAD_TOOLS/apksigner" verify --verbose --print-certs "$REACHPAD_OUTPUT"
(cd "$REACHPAD_ROOT/build" && sha256sum foldpatch-release.apk > foldpatch-release.apk.sha256)
echo "Signed $REACHPAD_OUTPUT"
