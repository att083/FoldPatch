# Why FoldPatch exists

[한국어](WHY.md) · [README](../README.md)

FoldPatch started when the maintainer's Galaxy Z Fold3 developed an unreadable center and lost touch on the right. The aim is to keep ordinary apps usable on the phone's remaining screen, without moving them into a separate workspace. It does not repair the panel or stop damage from spreading.

## Reports of a similar problem

These are three independent public user reports. They establish that others describe this difficulty, not its failure rate, hardware cause, or adoption of FoldPatch.

| Posted | Model and reported symptoms | Original report |
| --- | --- | --- |
| 2022-01-31 | Fold3: thick black line near the crease when unfolded; right side unresponsive | [Samsung EU Community](https://eu.community.samsung.com/t5/galaxy-z-fold-z-flip/samsung-galaxy-fold-3-black-line-in-crease/td-p/4726218/page/3) |
| 2022 | Z Fold3: black line at the crease and touch failure on one side | [Samsung Members](https://r2.community.samsung.com/t5/Samsung-Care/Samsung-Z-Fold-3-Black-Line-on-the-Crease/td-p/11432325) |
| 2022-07-29 | Z Fold3: thick black line in the inner display's center; right-side touch failure | [Samsung EU Community](https://eu.community.samsung.com/t5/galaxy-z-fold-z-flip/z-fold3-inner-screen-no-longer-working/td-p/5770195) |

FoldPatch currently targets phones with **visible space on both sides and working touch on at least one side**. It does not claim to solve a completely unreadable inner display or loss of touch on both sides.

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
