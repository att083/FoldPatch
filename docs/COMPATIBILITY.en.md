# Compatibility

[한국어・technical details](COMPATIBILITY.md) · [README](../README.md) · [Install and recover](INSTALL.en.md)

**Published alpha.5:** compile/target API 37, minimum API 35 (Android 15). Its update, input and folding/unfolding were checked on the Fold3 / Android 15; see the separate [validation](RELEASE_REVIEW.en.md#published-alpha5--target-api-37). The table below records alpha.4 and earlier development results.

**Android 14 is not supported.** Reflow caused the OS process to terminate on the API 34 AOSP emulator. From alpha.5, the APK requires Android 15 or newer. The published alpha.4 allowed installation on Android 14, but that does not mean it is supported.

Current release: **v0.1.0-alpha.5**, checked 2026-09-21. **Physical-device testing is limited to Samsung Galaxy Z Fold3 / Android 15 / One UI 7. Android 16 and 17 have emulator results only.**

## Requirements

- Android 15 or newer is required, not a guarantee of compatibility.
- The portrait inner display must have visible space on both sides and at least one working touch side.
- Each visible side can use 20–50% of the screen width. At 50% + 50%, the original screen is retained and the touchpad works without display reflow.
- Shizuku must be installed, running and authorized. Root and a permanently connected computer are not required. A non-root Shizuku service stops at reboot and may require manual startup.
- Concurrent screen-reader use is not supported. Keyboard input supports Korean and English only.

## What has been tested

| Environment | Evidence | Still unverified |
| --- | --- | --- |
| Galaxy Z Fold3 SM-F926N · Android 15 / One UI 7 | App reflow, touchpad and keyboard, folding recovery, short Wi-Fi-loss and screen-off checks; updated settings and range UI | Extended outdoor/idle use, every system transition and long-term battery/thermal behavior |
| Google Android 15 / API 35 emulator | Wireless Shizuku setup, onboarding, reflow, asymmetric widths and recovery after process/area-ownership loss | Samsung-specific menus, hardware touch and physical folding |
| Google Android 16 / API 36 emulator | Setup and reflow, pointer/scroll/pinch/hold input, keyboard editing, reconnect and recovery | One UI 8 hardware; the later 50% + 50% change was not separately retested here |
| Google Android 17 / API 37 emulator | Integrated alternate reflow path, input, keyboard editing, process/ownership recovery, 50% + 50% transitions and signed APK installation/update and browser-to-installer flow | One UI 9 hardware, physical folding and bottom-gesture/taskbar behavior |
| Other foldables / Android 15+ | Unverified devices | Actual operation on those devices |

The 50% + 50% setting was checked on the Fold3 and the Android 17 emulator. The Fold3 range screen saved 50% and restored the previous range. Injected touchpad test events checked endpoint coordinates, zoom and long-press on both environments; these automated streams do not replace physical-finger testing.

The Android 16 image was Google APIs x86_64 API 36 revision 7; Android 17 was API 37.0 revision 6. The virtual inner display was 1768 × 2208 at 420 dpi. These results do not cover every OS patch or manufacturer build.

## System behavior and limitations

- On the tested older Samsung display structure, app content is reflowed while notification/status layers are uniformly scaled. On the alternate Android 17 structure, app and notification/status widths are reflowed together.
- Android 17 emulator navigation/taskbar windows still request the physical display width. Large damaged areas and manufacturer-specific bottom gestures need additional device testing.
- Protected video/security screens, incoming calls and every system dialog have not been fully verified.
- Landscape use is not supported. FoldPatch disengages on a cover screen, lock, screen-off or landscape transition.
- Reconnecting to an already-running Shizuku is implemented. Restarting Shizuku after reboot is a separate requirement. Short indoor Wi-Fi-loss success is not an all-day connectivity guarantee.
- Samsung taskbar auto-hide once remained visible and was restored by reapplying the setting. Its cause is not confirmed.
- Moving a system picture-in-picture window partly off-screen can be normal behavior; additional clipping caused by FoldPatch has not been established.

## OS-specific implementation

Android 16 removed the old display-buffer constructor. FoldPatch now uses the common constructor plus `update()` path. Android 17 assigns an area used by the older reflow implementation to the system. FoldPatch keeps that ownership and selects a verified common-parent path when the display tree supports it. It checks actual structure and ownership instead of assuming support from the OS number. The integrated paths were exercised on Android 15, 16 and 17 emulators.

Historical investigation notes describe earlier failed prototypes as well as later fixes. For the current result, use this page and the [current validation summary](RELEASE_REVIEW.en.md). [Architecture](ARCHITECTURE.en.md) explains the implementation; [technical evidence in Korean](COMPATIBILITY.md) includes AOSP references.

## Reporting another device

Include the model, Android/One UI version, FoldPatch version, working touch side, steps and whether folding or turning off FoldPatch restores normal operation. Share only reviewed diagnostics. A successful emulator run does not establish support on a Samsung device.
