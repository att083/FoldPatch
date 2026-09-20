# Fold transition: missing cover System UI — 2026-09-20

Scope: Galaxy Z Fold3 SM-F926N, Android 15 / One UI 7. The user folded while ReachPad assistance was active, then woke/unlocked the cover display. The status bar disappeared and bottom navigation stopped working. A live failure was captured before any restart or setting change.

## Confirmed failure state

ReachPad had stopped its presentation. The output display and reflow guard were gone; no pending bounds journal remained. Display 0 had the normal cover geometry (840 × 2289 logical, 832 × 2268 physical). This was not a leftover inner-display resolution override.

SurfaceFlinger instead had the live `OneHanded:15:15` (StatusBar) and `OneHanded:17:17` (NotificationShade) trees in its **Offscreen Hierarchy**, with no parent. WindowManager still considered NotificationShade focused, but InputDispatcher reported `NO_WINDOW` and no focused input window. Thus an invisible system panel could block normal interaction.

A bounded shell-side recovery re-registered and unregistered the unoccupied feature-3 display-area organizer, with no transforms or bounds changes. WMS recreated those two surfaces under their current real parent. Input focus returned to `OK`; the shade and status bar became visible. The user confirmed that home/recent-app gestures worked again. No reboot, System UI restart, app-data deletion, or display-size reset was used.

## Teardown change

Previously, `NativeScreen.stop()` unregistered `SystemUiFit` (feature-3 descendants), then waited for `ReflowArea` (feature-4 ancestor) to restore and unregister. The guard can also independently restore during folding. This allowed descendant replacement to precede ancestor replacement across separate processes and fold sync transactions.

[Android's WindowContainer implementation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/services/core/java/com/android/server/wm/WindowContainer.java) recreates the surface and reparents children when an organizer is removed; operations may use a container sync transaction during transitions. The observed detached areas are exactly the transformed feature-3 areas that are direct children of the reflow ancestor. Other feature-3 areas behind stable intermediate containers remained attached. This supports a stale-parent transaction ordering failure; the exact historical transaction interleaving was not traced.

The stop path now removes ReachPad's output, waits for the reflow guard to restore/release the ancestor, and only then releases the status/shade organizer. New descendant surfaces therefore use the replacement ancestor, rather than one about to be removed. Existing app tasks, input mapping, healthy-region settings and gestures are unchanged.

## Validation

- The original broken-state snapshot detects both offscreen system areas and missing input focus.
- Targeted recovery returned both areas and focus; the user confirmed navigation recovery.
- Java gesture/geometry/Hangul/range tests, six-locale checks and release-surface checks pass.
- The new debug APK was installed; a fresh control-helper process ran the changed code. The device history recorded three unfold/assistance-ready → fold/assistance-stop → cover-off/on cycles afterward.
- Two post-cycle snapshots of the awake cover screen found both system areas under the real display, valid input focus, and no retained organizer, reflow guard or output display. No cleanup errors were reported in the helper log.
- The user confirmed the status bar and home/recent-app navigation now work after folding and unlocking. This confirms the tested sequence, not every possible intermittent transition.
- Both debug and unsigned release builds completed. Existing compiler deprecation/annotation warnings remain.

Raw screenshots, window/input dumps and device identifiers stay in ignored `.local/fold-failure/`, not in release materials. A successful fold test does not prove every OEM transition or simultaneous process-kill sequence safe.
