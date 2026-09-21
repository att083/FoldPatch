# Build, sign and prepare a release

[한국어](BUILD.md) · [Contributing](../CONTRIBUTING.md)

## Prerequisites

Use Bash on Linux or macOS with JDK 17 (`javac`, `java`, `jar`, `keytool`), Python 3, Android SDK platform 37.0, build-tools 35.0.0 and platform-tools. `sha256sum` is required; on macOS it is available through coreutils. Gradle and Android Studio are not required.

The scripts find the SDK through `ANDROID_SDK_ROOT`, then `ANDROID_HOME`, then `~/Android/Sdk`. Set `JAVA_HOME` or `REACHPAD_JAVA` to JDK 17, or put that JDK on PATH.

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
sdkmanager 'platforms;android-37.0' 'build-tools;35.0.0' 'platform-tools'
bash scripts/test.sh
bash scripts/build.sh debug
bash scripts/build.sh release
```

After the SDK is installed, the build does not download application dependencies. Fixed Shizuku API 13.1.5 JARs are included and checked against `libs/SHA256SUMS`; see [dependency sources](../libs/README.md). Existing compile-only annotation and Java 8 source/target warnings do not indicate a failed APK build.

| Output | Purpose |
| --- | --- |
| `build/foldpatch-debug.apk` | Locally signed development build with debugging and test entry points |
| `build/foldpatch-release-unsigned.apk` | Non-debuggable release build; not installable until signed |
| `build/foldpatch-release.apk` | Installable release after signing with the distribution key |

The application ID is `dev.foldpatch`. It was changed before the first public release; pre-publication development installs under the former ID are separate apps and cannot update in place. The next candidate (0.1.0-alpha.5) requires minimum API 35 (Android 15) and uses target/compile API 37. The published alpha.4 still targets API 34. Target SDK compliance alone does not establish Play Store eligibility or device compatibility. Increase `versionCode` and update `versionName` for each public update.

## Validation

`scripts/test.sh` runs nine host test groups, release-manifest/dependency checks and checks for missing or inconsistent localized strings. GitHub Actions is configured to run these checks, build an unsigned APK and validate the public export. CI artifacts are build evidence, not the installable user release. Neither host tests nor CI establish physical-device compatibility.

Display/input/recovery changes also need targeted device checks. Debug-only probes in `tests/device/` are excluded from the release build. Synthetic events test input routing, not a damaged digitizer or finger feel. Report untested conditions in a pull request.

## Distribution signing

The distribution key must stay private and remain the same for normal updates. Keep a separate secure backup of the key and its password before publishing. Do not regenerate an existing release key. See [Android's signing guide](https://developer.android.com/studio/publish/app-signing).

For a new project's first key only:

```bash
mkdir -p .local/signing
chmod 700 .local/signing
keytool -genkeypair -keystore .local/signing/release.keystore \
  -alias reachpad -keyalg RSA -keysize 3072 -validity 10000
```

Enter the password interactively, then provide a private password file to the signing script. Do not put its contents in a command line or commit it.

```bash
export REACHPAD_KEYSTORE="$PWD/.local/signing/release.keystore"
export REACHPAD_KEY_ALIAS=reachpad
export REACHPAD_STORE_PASS_FILE="$PWD/.local/signing/store-password.txt"
bash scripts/sign-release.sh
```

If the key has a different password, also set `REACHPAD_KEY_PASS_FILE`. Signing creates `build/foldpatch-release.apk.sha256`. Public releases attach this checksum and the signed APK; an unsigned CI artifact is not a substitute.

## Install locally

```bash
bash scripts/run-device.sh DEVICE_SERIAL_OR_IP:PORT
```

This installs the debug build and opens settings. A signed release can be installed with `adb install -r build/foldpatch-release.apk`. A development build signed with a different key cannot be updated this way. Do not automatically uninstall someone's existing app to bypass a signature mismatch; use a separate test device/emulator for fresh installation.

## Public source export

```bash
python3 scripts/prepare-public.py
```

This creates `build/github-ready/` and `build/foldpatch-source.zip` from an explicit allowlist. It includes source, resources, dependencies, tests, workflows, documentation and the final README images. Local Markdown and HTML links are checked against the exported files.

Signing material, personal logs/captures, internal promotion drafts, image prompts and application-preparation notes are excluded. For the first publication, initialize the public repository from the exported tree; the development repository's history contains internal materials and is not the publication source. The export script does not create a GitHub repository or upload files.

Associate each release with a source commit/tag, use the corresponding signed APK and checksum, and state the [tested environments](COMPATIBILITY.en.md). Branding compatibility details are in the [brand guide](BRAND.md).
