# AI installation guide: install FoldPatch on a user's phone

[한국어](AGENT_INSTALL.md) · [README](../README.md)

Use this guide when someone asks an agent to “install this repository on my phone.” Complete the work that your tools and the user's authorization allow; hand off only actions that need the user's device access or decision. This guide serves people who want to use the Android app; contributing code or setting up a development environment is not a prerequisite. An installation request does not authorize publishing the repository or contacting other people.

## 1. Establish the current situation

Read [compatibility](COMPATIBILITY.en.md) and [installation/recovery](INSTALL.en.md). Use available device information before asking technical questions. Establish the phone/model/OS, whether FoldPatch is already installed, and whether your environment can reach that phone. Ask which inner-screen side responds to touch if this is not already known.

Android 15/API 35 is required. On Android 14 or older, ask the user to update Android first; do not install an older FoldPatch release as a workaround. Physical-device results cover Galaxy Z Fold3 / Android 15; Android 16/17 are emulator-only. Shizuku must be installed, running and authorized. The user can begin setup on the cover screen. Screen calibration later requires the unlocked, portrait inner display.

| Situation | Next action |
| --- | --- |
| A signed public release is available | Download and verify its APK and checksum; avoid building from source for an ordinary installation. |
| Working from an unpublished maintainer checkout | Use a supplied local release candidate only after checking its version, checksum and signing provenance. Do not describe it as a public download. |
| No installable APK is available | Explain that fact. If the user wants a local development build, follow [Build](BUILD.en.md); explain its debug/test entry points and different signing key. Otherwise finish with the precise missing release requirement. |
| No phone connection/tools | Provide the verified phone-download link and guide the browser installer. Do not claim to have installed remotely. |
| Existing app or partial setup | Preserve data and completed steps. Inspect the version and live setup status; do not start over. |

## 2. Choose and verify an APK

Use the repository supplied by the user or its verified Git remote. Check the releases list: an alpha prerelease may not appear under GitHub's `latest` release. Choose a real published tag with **both** `foldpatch-release.apk` and `foldpatch-release.apk.sha256`. Do not invent a URL, substitute another repository or use an unsigned Actions artifact.

If GitHub CLI is available, these are Bash examples after replacing the two values with the verified repository and chosen tag. Browser/API equivalents are fine; GitHub CLI is not required.

```bash
FP_REPO='OWNER/REPOSITORY'
gh release list --repo "$FP_REPO"
FP_TAG='EXISTING_RELEASE_TAG'
gh release view "$FP_TAG" --repo "$FP_REPO" --json tagName,isPrerelease,assets,url
```

Download into a new directory so old APKs cannot be mistaken for the selected release:

```bash
FP_DOWNLOAD_DIR="$(mktemp -d)"
gh release download "$FP_TAG" --repo "$FP_REPO" \
  --pattern foldpatch-release.apk --pattern foldpatch-release.apk.sha256 \
  --dir "$FP_DOWNLOAD_DIR"
```

Inspect the checksum file first: it should contain one SHA-256 entry for the APK's basename, with no absolute path or parent-directory path. Then check it:

```bash
(cd "$FP_DOWNLOAD_DIR" && sha256sum --check foldpatch-release.apk.sha256)
```

A checksum detects a mismatch; it is not independent proof of the publisher's identity. When SDK tools are available, inspect the package/version with `aapt2 dump badging` and the signer with `apksigner verify --verbose --print-certs`. Compare the signer with the selected release's published record, and check update compatibility with an existing installation. Never request a distribution private key for installation.

## 3. Install through an available path

**On the phone:** open the verified release asset in the browser, download the APK, open it and follow Android's installer. If Android asks to allow this installation source, explain what it enables and let the user decide. Manufacturer restrictions can differ; do not tell the user to broadly disable device protection to force installation.

**Through an authorized computer connection:** use Android SDK platform-tools. Inspect `adb devices -l`, choose the intended phone, and use `-s` on every device command. If more than one phone/emulator is present or its identity is unclear, resolve that before sending installation or input commands. In WSL/remote environments, a device visible to the host may not be reachable from the agent; reuse a working authorized path without resetting shared ADB servers.

For wireless computer ADB, follow the [official Android guide](https://developer.android.com/tools/adb#connect-to-a-device-over-wi-fi). The pairing port and connection port are different and can change. Have the user approve debugging and enter the pairing code locally when possible. Do not retain codes in scripts, logs or issues. Pairing the computer does **not** pair or start Shizuku.

After selecting the device, examples below use Bash variables. An empty/missing selection must not fall back to an arbitrary device.

```bash
FP_DEVICE='SELECTED_DEVICE_SERIAL'
adb -s "$FP_DEVICE" shell getprop ro.product.model
adb -s "$FP_DEVICE" shell getprop ro.build.version.sdk
adb -s "$FP_DEVICE" shell pm path dev.foldpatch
```

If the package already exists, check its installed version before proceeding. Turn off FoldPatch through its settings and allow the normal display/keyboard to return before updating an active installation. When it is safe to install the selected APK:

```bash
adb -s "$FP_DEVICE" install --no-incremental -r "$FP_DOWNLOAD_DIR/foldpatch-release.apk"
adb -s "$FP_DEVICE" shell am start -n dev.foldpatch/.NativeActivity
```

`-r` preserves app data for a compatible update. A signature mismatch or version downgrade is a reason to find a compatible APK and explain the problem, not to uninstall, clear data, change package IDs or force a downgrade. `scripts/run-device.sh` installs the **debug** APK, so do not use it for the public release. Do not use legacy activities or debug bootstrap/probe components for ordinary setup.

## 4. Help the user finish on the phone

Use the app's resumable setup flow. For full labels and recovery steps, follow [Install and use FoldPatch](INSTALL.en.md); do not duplicate them with shell settings writes.

| Agent can do | User needs to do |
| --- | --- |
| Find the official Shizuku download/setup instructions and explain the current missing step | Install/start Shizuku, approve wireless debugging/pairing and FoldPatch authorization; enter pairing codes in Shizuku's own flow |
| Explain overlay, accessibility and keyboard access before the relevant step | Make the permission/consent decisions in Android and FoldPatch |
| Open or describe screen-area adjustment | Identify the working touch side and the physically visible widths; use sliders/±, preview and save |
| Explain pointer movement, clicking, scrolling and keyboard use | Try them with real fingers and confirm they are usable on the damaged display |

Never choose 50% + 50% simply because an ADB screenshot shows a complete image: panel damage is not visible in an ordinary screenshot. The unmodified calibration ruler starts at 0% on **each physical outer edge**. Each side accepts 20–50%; 50% + 50% keeps the original full-width screen. Preserve an existing saved range unless the user wants to change it.

If Shizuku stops, distinguish restarting it from reconnecting to a running service. A non-root reboot stops Shizuku; restoring its connection does not require clearing FoldPatch settings. Do not require public Wi-Fi as the recovery method. A lost computer ADB connection alone does not establish that the phone-local app connection stopped.

## 5. Verify only this installation

Confirm the installed package/version and that FoldPatch opens. Complete or identify the remaining setup step. If the phone is ready, ask the user to check a normal app, pointer click/scroll and keyboard input, then folding or turning off FoldPatch for normal-screen recovery. Do not force a reboot, stop Shizuku, disable networking or run development probes merely to finish an install.

End with a short report: **version/source installed; Shizuku and setup status; what was actually checked; the next action, if any**. Distinguish “APK downloaded,” “APK installed,” and “ready and physically checked.” If you cannot observe the phone, say which result the user confirmed. Keep pairing codes, personal screens and unreviewed system logs out of that report.
