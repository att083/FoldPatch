# Why FoldPatch exists

[한국어](WHY.md) · [README](../README.md)

FoldPatch started when the maintainer's Galaxy Z Fold3 developed an unreadable center and lost touch on the right. The aim is to keep ordinary apps usable on the phone's remaining screen, without moving them into a separate workspace. It does not repair the panel or stop damage from spreading.

## Reports of a similar problem

**Reports of black areas at the crease and failed touch input continue into 2025–2026.** These independent user reports were checked on 2026-09-21 and are listed newest first. Dates refer to the original posts, not later replies.

| Posted | Model and reported symptoms | Original report |
| --- | --- | --- |
| 2026-09 | Z Fold6: a widening black line along the crease and abnormal touch response around it | [Samsung Members](https://r2.community.samsung.com/t5/Galaxy-Z-Flip-Galaxy-Z-Fold/Galaxy-Z-Fold6-Inner-Screen-Developing-Black-Line-at-Folding/m-p/22910072) |
| 2026-07-16 | Z Fold6: black line on the inner display and no touch response anywhere — outside FoldPatch's current scope, which requires a working touch side | [Samsung Members](https://r2.community.samsung.com/t5/Galaxy-Z-Fold/Black-line-fold-6/td-p/22537487) |
| 2025-02-14 | Z Fold3: black bar down the center of the inner display; right side unresponsive | [r/GalaxyFold](https://www.reddit.com/r/GalaxyFold/comments/1ipfnaz/) |
| 2025-01-06 | Pixel 9 Pro Fold: black line down the middle; only the right side responds to touch | [Google Pixel Community](https://support.google.com/pixelphone/thread/317231965/google-pixel-9-pro-fold-inside-screen-not-working?hl=en) |
| 2022-07-29 | Z Fold3: thick black line in the inner display's center; right-side touch failure | [Samsung EU Community](https://eu.community.samsung.com/t5/galaxy-z-fold-z-flip/z-fold3-inner-screen-no-longer-working/td-p/5770195) |

These reports show that similar difficulties are still being reported. They do not establish failure rates, hardware causes, or adoption of FoldPatch.

FoldPatch currently targets phones with **visible space on both sides and working touch on at least one side**. The models above are not a list of supported devices. Physical-device testing is limited to **Galaxy Z Fold3 / Android 15 / One UI 7**; see [compatibility](COMPATIBILITY.en.md) for details.

## How existing tools compare

This comparison uses developer documentation checked on 2026-09-20, not fresh device tests of each tool.

| Tool | Documented purpose | What FoldPatch combines with that need |
| --- | --- | --- |
| [Quick Cursor](https://github.com/micku7zu/QuickCursor) | Cursor control for one-handed reach; gesture options depend on OS and paid features | Reflowing ordinary apps to a width that excludes the dead center, plus a keyboard on the working side |
| [Partial Screen](https://play.google.com/store/apps/details?id=ich.andre.partialscreen) | Blocking unwanted touches in selected screen areas | Keeping obscured content readable and the opposite side operable as well as blocking bad input |
| [scrcpy](https://github.com/Genymobile/scrcpy) | Displaying and controlling Android from a computer, with optional virtual displays | Continuing existing apps across the phone's usable regions and operating them on the phone itself |

The documentation reviewed did not describe FoldPatch's combination of **app-width reflow around a dead center, a one-sided touchpad, and a one-sided keyboard**. This is not an exhaustive market survey or a claim of uniqueness. An existing tool may be simpler if only cursor control or touch blocking is needed.

## Why publish the implementation?

People with the same problem can try an installable APK. Developers can inspect the display, input and recovery code and adapt it to more devices. Reused or adapted code, including work from scrcpy, is credited in [third-party notices](../THIRD_PARTY_NOTICES.md).

Current evidence comes from the maintainer's Fold3 and emulator tests. External adoption, repair-cost savings and longer device life have not been measured. The [validation record](RELEASE_REVIEW.en.md) and [next work](BACKLOG.en.md) state what is known and what remains open.
