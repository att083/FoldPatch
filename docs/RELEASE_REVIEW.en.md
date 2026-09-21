# Current validation

[한국어](RELEASE_REVIEW.md) · [Compatibility](COMPATIBILITY.en.md) · [Historical development log (Korean)](research/2026-09-20-development-history.md)

Scope: **v0.1.0-alpha.4**, updated 2026-09-21. This is the first public alpha. **Physical-device testing covers Samsung Galaxy Z Fold3 / Android 15 / One UI 7. Android 16 and 17 have emulator results only.**

The application ID was finalized as `dev.foldpatch` before publication on 2026-09-21. The renamed development APK was installed on the Fold3 with the maintainer’s screen/control settings transferred and new permissions approved in the setup flow. The maintainer confirmed touchpad click/scroll, keyboard input, cover-screen recovery and automatic return after unfolding. The old development app was then removed. This was a settings migration, not an independent novice onboarding test. Earlier emulator/browser-installation evidence below predates the ID change.

## Observed results

| Area | Evidence | Limits |
| --- | --- | --- |
| Fold3 operation | Existing-app reflow, click/scroll/drag, one-sided keyboard, recents/notifications and cover-screen recovery after folding | Accumulated development checks, not a complete final-version rerun of every feature |
| Latest 50% setting | Fold3 range UI saved 50% per side and restored the previous range; Fold3/API 37 automated input-path checks covered endpoints, zoom and long-press | Synthetic streams do not establish physical-finger feel or digitizer behavior |
| Android 15/16/17 emulators | Integrated reflow, asymmetric widths, forced-process-exit and area-ownership recovery; input and keyboard editing on API 36/37 | Not Samsung hardware, physical folding or latest One UI validation |
| Onboarding | Android 15/16 wireless Shizuku pairing, denied/retried authorization, range saving and progress after restart | Fresh Samsung installation and independent novice-user testing remain open |
| Connection continuity | Fold3 clicks during about 120 seconds without Wi-Fi/wireless debugging; control returned after about one minute of screen-off | USB debugging remained enabled; no claim of all-day/outdoor continuity or reboot autostart |
| Earlier public APK candidate | Distribution-signature verification; fresh installation/launch and setup progress retained across an update on Android 17 emulator | The existing Samsung phone used a development-signed update to retain its data |
| Earlier browser installation | Android 17 emulator: Chrome download → allow installation source → Android installer → Open → first setup screen; verified download hash, version and non-debuggable flag | Served from a local HTTP test server, not the eventual GitHub HTTPS release or a Samsung device |

An early Android 17 path failed. The integrated alternative subsequently passed the tests above while preserving system ownership. Historical statements about that failed path are not the current result. Android 17 bottom gestures/taskbar behavior and manufacturer display structures still need additional device checks.

## Source and artifacts

- Extracted the public source ZIP into a separate directory and passed all nine host test groups, release checks, 221 string keys across six resource sets, and the release build.
- Rebuilt manifest, DEX, resources and license-file contents match the signed candidate. This excludes signature and ZIP metadata; it is not a claim of byte-identical APK files.
- The release is non-debuggable and excludes development test entry points. Checks cover the absence of Internet permission, fixed dependency hashes and license files.
- GitHub Actions is configured for the same host checks, unsigned build and public-export/document-link validation. Workflow lint and execution of the same commands from the exported source passed locally. **[GitHub-hosted validation passed](https://github.com/att083/FoldPatch/actions/runs/35564666849)** on 2026-09-21. The GitHub-built unsigned APK has the same entry contents as the locally built candidate.
- Signing-certificate SHA-256: `c069f52970619a240dab85e4e519ae59c82cd03ca1a5ccfb0ac6e3fb161c0e48`.

## Still open

Fresh Samsung setup without migrated settings and an independent novice's attempt still need confirmation. Android 16/17 hardware, long-term thermals/battery, protected content, calls and every system transition remain unverified. Landscape and concurrent screen-reader use are not supported.

Detailed Korean records cover [onboarding](research/2026-09-20-first-run-validation.md), [input/recovery](research/2026-09-20-input-and-recovery.md), [fold recovery](research/2026-09-20-fold-system-ui-restore.md) and [indoor connection checks](research/2026-09-20-home-stability.md). See [next work](BACKLOG.en.md) for priorities.
