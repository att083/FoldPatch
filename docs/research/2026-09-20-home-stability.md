# Home stability and recovery tests — 2026-09-20

Scope: the existing Galaxy Z Fold3 SM-F926N, Android 15 / One UI 7. The user explicitly excluded outdoor testing. Existing screen ranges, working side, apps and Shizuku permissions are retained. Private evidence stays under ignored `.local/home-stability/` and `.local/fold-failure/home-*`.

## Recovery defects found and changes

`ReflowArea.stop()` previously discarded its process reference before confirming process exit. After a graceful wait and SIGTERM, a suspended child could remain alive, yet the caller proceeded to release descendant system-UI surfaces. Recovery subprocess exit status and journal removal were also not checked.

Cleanup now retains the child until death is confirmed, uses bounded escalation, checks recovery exit status and journal removal, and keeps pending cleanup scheduled even after ReachPad's visible root has been removed. Failed system-UI organizer release also retains its ownership for retry. A new helper checks abandoned bounds on startup, including on the cover display. Recovery uses the current logical display dimensions rather than stale inner-display dimensions.

A real SIGSTOP test exposed an Android-specific detail: `Process.destroyForcibly()` did not kill the suspended subprocess. The [Android 15 UNIXProcess implementation](https://android.googlesource.com/platform/libcore/+/refs/heads/android15-release/ojluni/src/main/java/java/lang/UNIXProcess.java) inherits [Process.destroyForcibly()](https://android.googlesource.com/platform/libcore/+/refs/heads/android15-release/ojluni/src/main/java/java/lang/Process.java), whose default calls `destroy()` again. ReachPad therefore sends SIGKILL explicitly to its retained UNIXProcess child after the graceful deadlines. It does not search for or kill unrelated processes.

## Results

| Case | Result |
| --- | --- |
| Baseline input | Far-left/right coordinates, raw/local agreement, real Android pinch (1.6667×) and long press passed. |
| Kill only ReachPad control helper | Same Shizuku server and app survived. Replacement helper/guard appeared by the 2.4-second sample, with ReachPad keyboard selected. Both system-UI areas remained attached and input focus existed. Input test passed again afterward. |
| Suspend guard, then force-stop ReachPad | Initial portable-force-kill attempt failed; retained ownership and retry exposed the failure rather than reporting success. Test suspension was manually released before retrying the fix. |
| Same suspended-guard test with Android-specific escalation | Guard absent by the 2.7-second sample; bounds journal removed; no retained reflow/system-UI organizer or output display; Samsung keyboard restored. Both system-UI areas remained attached and input focus was present. |
| Wi-Fi interruption (about 120 seconds) | 120 one-second samples reported Wi-Fi off; all 49 user clicks were logged while both Wi-Fi and wireless debugging were off. The user confirmed pointer movement and increasing counts. Device-local observation retained one server, app, helper and guard throughout. USB debugging remained enabled. Wi-Fi/wireless debugging were restored by the temporary test app; changed desktop ADB endpoint was supplied by the user. |

The final build with startup recovery was installed, enabled and passed the input-integrity probe again. During the roughly one-minute screen-off test, only the initial transition sample retained the guard/ReachPad keyboard; subsequent samples used the Samsung keyboard and no guard. Both later organizer snapshots were clear, and the same Shizuku server survived. After unlock, the user confirmed touchpad clicks worked and the Samsung taskbar remained hidden. On the final installed build, the guard was then deliberately suspended before the user physically folded the phone and unlocked the cover screen. Cleanup completed, the Samsung keyboard returned, both original system-UI areas remained attached, input focus was present, and no organizer, guard or output display remained. The user confirmed the cover status bar and home/recent-app gestures worked. This confirms the tested suspended-guard fold path; it does not separately prove every fresh-helper recovery case. These short fault-injection tests do not establish long-term idle/doze, outdoor continuity, other OEM behavior or reboot recovery. Reboot and Shizuku cold startup remain a separate acceptance case.

## Samsung taskbar observation

After the network test the user reported Samsung's taskbar remaining visible. The system setting still read automatic hide; the taskbar remained visible even with ReachPad assistance disabled. Reapplying the same auto-hide preference through Samsung Settings (temporarily select persistent, then return to auto-hide) restored hidden behavior, including after assistance was enabled again. No production code changes Samsung taskbar preferences. The exact trigger was not established, so this is recorded as an observation needing recurrence testing, not a diagnosed ReachPad taskbar fix. Final `task_bar=1`, `taskbar_style_type=0`, and gesture-navigation mode are preserved.

## Final checks and cleanup

The process-shutdown unit tests cover graceful exit, termination, forced termination, timeout, interruption and an already-dead or absent process. The Android-specific force-kill adapter was exercised by the device suspension tests above. The final source passed the full local test suite, locale/release-manifest checks, and debug/release builds; its debug APK is installed.

The temporary network-test app and its test-only permission were removed, and device-local observer files were deleted. Wi-Fi and wireless debugging were restored. The phone was left folded with normal cover-screen input. Comparing application preferences before and after testing found only the expected assistance-active flag change between the initial unfolded and final folded state; saved screen ranges and user controls were retained. Raw logs and screenshots remain private and are excluded from the public source package.
