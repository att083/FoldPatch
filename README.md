![FoldPatch — Bridge the split. Keep using your phone.](docs/media/foldpatch-cover.png)

# FoldPatch

[English](README.md) · [한국어](README.ko.md) · [日本語](README.ja.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md)

**[Download and install the APK](docs/INSTALL.en.md)** · [Compatibility](docs/COMPATIBILITY.en.md)

**An Android app to keep a foldable phone usable with a dead center display and touch working on only one side.**

FoldPatch is open source and started with the maintainer's own damaged Galaxy Z Fold3, to help people in the same situation keep using their phone until repair is possible. [Background and related tools](docs/WHY.en.md).

## Before and after screen-area adjustment

| Before: left 50% · right 50% | After: left 44% · right 43.5% |
| --- | --- |
| [<img src="docs/media/range-before-fold3.jpg" width="340" alt="Before adjustment: the damaged center of a Fold3 hides numbers 8 and 9." />](docs/media/range-before-fold3.jpg) | [<img src="docs/media/range-after-fold3.jpg" width="340" alt="After adjustment: numbers 1 through 16 are visible across the two sides of the Fold3." />](docs/media/range-after-fold3.jpg) |

**Photographs of a Galaxy Z Fold3 · Android 15.** [See Chrome and split-screen before-and-after photos →](docs/REAL_USE.en.md)

## What it does

- **Continue the screen across the gap.** Apps get a width that fits the two usable sides combined. FoldPatch displays that single screen across the damaged strip without horizontal squashing. Use one full-screen app or your existing split-screen arrangement.
- **Control both sides from one side.** Choose left or right as the working touch side. Use it as a touchpad to click, drag, scroll, and zoom on the opposite side or the whole screen.
- **Type where touch works.** A one-sided keyboard types into fields on either side and provides basic editing tools such as copy and paste.

Adjust the usable screen widths and toolbar size/position, and optionally turn on FoldPatch automatically when you unfold.

<p>
  <a href="docs/media/settings-fold3.png"><img src="docs/media/settings-fold3.png" width="280" alt="Fold3 settings with controls on the working left side." /></a>
  <a href="docs/media/calibration-fold3.png"><img src="docs/media/calibration-fold3.png" width="280" alt="Fold3 calibration with sliders on the left to adjust both visible widths." /></a>
  <a href="docs/media/keyboard-fold3.png"><img src="docs/media/keyboard-fold3.png" width="280" alt="Fold3 with the keyboard and vertical toolbar on the left and the pointer on the right, over an input test screen." /></a>
</p>

Left to right: **settings · screen-area adjustment · keyboard and toolbar**. Actual Galaxy Z Fold3 / Android 15 captures in Korean; the keyboard background is an input test screen. Select an image to view the original.

## Can I use it on my phone?

The current scope is the **portrait inner display**. Both sides must have visible screen space, and at least one side must respond to touch. Each usable side can be set to 20–50% of the full width, measured inward from its outer edge. Set both to 50% to keep the whole screen visible and use one side as a touchpad, even on an undamaged screen.

| Environment | Testing |
| --- | --- |
| Galaxy Z Fold3 · Android 15 / One UI 7 | Physical device |
| Android 16/17 | Emulators only; no physical-device testing |
| Other foldables / Android 14 | Functionality unverified |

Android 14 is the installation minimum. See [compatibility details](docs/COMPATIBILITY.en.md) for device differences and unverified behavior. Concurrent screen-reader use is not supported.

**[Shizuku](https://shizuku.rikka.app/download/) must be installed and running.** Initial setup uses trusted Wi-Fi and wireless debugging. No root or permanently connected computer is needed. Shizuku may need to be started again after reboot.

## Get started

**Install the ready-made APK; no build tools are needed.** This early alpha is being prepared for its first GitHub release.

1. Download **`foldpatch-release.apk`** from the public release’s **Assets** and install it.
2. Open FoldPatch and follow its setup guide to choose your touch side and prepare Shizuku, permissions, and the keyboard.
3. Unfold, adjust and save both visible widths, then return to your usual apps.

You can do the early setup on the folded phone's cover screen. **[Installation, gestures, and recovery →](docs/INSTALL.en.md)**

**Using Codex, Claude Code, or another agent?** Start with the [agent installation guide](docs/AGENT_INSTALL.en.md). [AGENTS.md](AGENTS.md) is the repository entry point. You can ask:

> Read this repository's AGENTS.md and agent installation guide, then help me install FoldPatch on my phone. Do the steps you can, tell me when I need to act on the phone, and preserve my existing app data and settings.

The interface follows your OS language: Korean, English, Japanese, Simplified Chinese, or Traditional Chinese. Keyboard input supports Korean and English only. The app has no Internet permission, ads, or analytics SDK. [Privacy and permissions](PRIVACY.en.md).

## Help improve it

Reports of confusing setup steps or results on your phone are useful. Translation, documentation, and code contributions are welcome too. [Contribution guide](CONTRIBUTING.md).

[Build](docs/BUILD.en.md) · [Architecture](docs/ARCHITECTURE.en.md) · [Remaining work](docs/BACKLOG.en.md).

[Apache License 2.0](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md)
