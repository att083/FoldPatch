# Next work

[한국어](BACKLOG.md) · [Current validation](RELEASE_REVIEW.en.md)

The next work is about reliability and installation. Hardware evidence currently covers a Galaxy Z Fold3 / Android 15. These are priorities, not promised delivery dates or claims of support for additional devices.

| Priority | Work | Evidence needed to close it |
| --- | --- | --- |
| Before alpha.5 publication | Final change review and release preparation | Fold3 core checks completed; verify the source, signed APK and checksum selected for publication |
| 1. OS and One UI compatibility | Investigate reported display-tree, taskbar, gesture and input differences on other devices | Record model/OS/build, reproduction steps and targeted before/after checks; update the compatibility table only with observed results |
| 2. Connection and recovery | Longer Wi-Fi/mobile-data/idle use; repeated fold/lock transitions; Shizuku stop/restart and reboot | Confirm continued control or recovery of normal display, system gestures and keyboard; distinguish a live Shizuku reconnect from restarting a stopped Shizuku |
| 3. First installation | A fresh Samsung installation and a person's attempt using only the guide; review translations and small/large UI settings | Record where setup blocks or confuses, improve that step, then repeat it without developer intervention |

Short indoor connection tests, emulator onboarding and automated recovery checks have passed. They do not resolve the original outdoor interruption, long idle conditions or independent-user setup. Samsung taskbar auto-hide once needed its setting reapplied; the cause is still unconfirmed.

## Release preparation

Prepared locally: signed APK and checksum, a buildable source export, bilingual core documentation, issue templates and a GitHub Actions workflow. The [validation summary](RELEASE_REVIEW.en.md) distinguishes locally executed checks from checks still pending on GitHub.

Before public release: confirm a separate secure signing-key backup, publish the reviewed source and matching APK under the same version tag, and check the real download links and hosted CI result. Store distribution is outside the current release plan.

## Later work

- Measure latency, battery use and thermals of the current display-buffer implementation. Old measurements do not describe the current renderer.
- Exercise calls, protected content, system dialogs and keyboard transitions where relevant failures are reported.
- Improve Korean/English symbols, emoji and field-specific keyboard behavior. Japanese/Chinese input and landscape support are deferred.
- Check suspected extra picture-in-picture clipping against normal Android behavior before calling it a defect; partially hiding a PiP window at an edge can be intentional.

Already implemented: reflow of existing apps, either working touch side, opposite-side/whole-screen touchpad, drag/pinch/scroll/hold, pointer and toolbar options, one-sided keyboard/editing/haptics, multilingual UI, range preview/rollback, reconnect and fold recovery. Implementation is not universal-device validation.
