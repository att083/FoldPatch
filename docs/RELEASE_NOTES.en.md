# v0.1.0-alpha.4 — first public alpha

[한국어](RELEASE_NOTES.md) · [Install and recover](INSTALL.en.md) · [Validation](RELEASE_REVIEW.en.md)

FoldPatch keeps existing apps usable around an unreadable center strip, with a touchpad and keyboard on the side that still responds. This is an early alpha, not a panel repair or universal foldable-device support claim.

## Included

- App-width reflow across two usable regions, without horizontally squeezing the content or moving apps into a separate workspace.
- Either working touch side; opposite-side or whole-screen cursor control with click, drag, hold, two-finger scrolling and pinch zoom.
- Korean/English keyboard on the working side, key-press haptics and basic editing tools.
- Adjustable pointer speed/appearance and toolbar size/orientation/buttons; three toolbar stages with saved state and position.
- Physical-screen calibration at 20–50% per side, timed preview rollback and previous-range recovery. At 50% + 50%, keep the original screen and use the touchpad without display reflow.
- Guided Shizuku/permission setup, reconnect handling and recovery when folded or stopped.
- Korean, English, Japanese, Simplified Chinese and Traditional Chinese interface; integrated Android 15/16/17 display paths.

## Before installing

The application ID is `dev.foldpatch`, finalized before the first public release. Earlier development builds under the former ID are separate apps; they do not transfer settings or permissions automatically.

**Shizuku must be installed, running and authorized.** A non-root Shizuku service stops at reboot and may need manual startup. No root or permanently connected computer is needed. Read the [setup guide](INSTALL.en.md).

Physical-device checks cover **Samsung Galaxy Z Fold3 / Android 15 / One UI 7**. **Android 16 and 17 have emulator results only.** Android 14 is the installation minimum; other models are unverified. See [compatibility and known limits](COMPATIBILITY.en.md).

Landscape and concurrent screen-reader use are not supported. Keyboard input is Korean/English only. Protected content, calls, extended battery/thermal behavior and all system transitions have not been fully tested. Notifications can be scaled on the older Samsung display path.

## Release files

The [release](https://github.com/att083/FoldPatch/releases/tag/v0.1.0-alpha.4) includes:

| File | Use |
| --- | --- |
| `foldpatch-release.apk` | Signed, installable app |
| `foldpatch-release.apk.sha256` | SHA-256 checksum of that APK |
| `foldpatch-source.zip` | Reviewed source, tests and build instructions; not an installable app |

Development builds may have a different signing key. Do not uninstall an existing app automatically to bypass a signature mismatch; uninstalling loses its settings. Normal updates require the same distribution key.
