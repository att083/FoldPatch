# Pointer coordinates, gestures and control recovery — 2026-09-20

Scope: Galaxy Z Fold3 SM-F926N, Android 15 / One UI 7, portrait inner display. The physical display is 1768 × 2208; this test used left edge 787, right start 981, logical app width 1574. User settings and existing tasks are retained. No publication or production signing performed.

## Cause and implementation

The former presentation mirrored live window layers. Android created corresponding input-window clones. On the right side, raw coordinates could differ from local coordinates plus the view location by the 194 px gap. Buttons using local hit-testing could work while seek bars using raw coordinates were displaced. In split screen, some cloned touchable regions also ended at logical x=1574 although the visible right viewport extended to physical x=1768. This is a shared input/composition problem, not a video-app-specific offset.

The existing app area still reflows to the healthy logical width. `FrameMirror` composes the original display into a BLAST GPU buffer on a private, output-only virtual display. No activity is launched or moved there. Only buffer pixels are split across the physical healthy regions. Physical display 0 has the original app input windows, without the old live app-window clones.

Accessibility captures actual touchscreen events while assistance is active. Trackpad events are generated in original logical coordinates. Direct touches, keyboard and toolbar input are rebuilt in that same coordinate space; injected events bypass the hardware filter. This also preserves raw/local coordinate consistency. The right-side toolbar is placed in logical coordinates and appears at its saved physical position through the renderer.

The notification shade keeps the existing constant scale. Its extra bottom background samples the two outer corners, avoiding vertical streaks from stretched disclosure text. It has no separate input clone on display 0: the original shade window owns its touch region and keyboard focus. An intermediate DROP_INPUT shade clone was rejected because token-based key lookup could select it and discard Back events. Final code removes it.

## Gestures

- One-finger movement, tap and double-tap.
- Hold still until haptic feedback, then drag; continue holding for the app's long press.
- Tap, then press again within 300 ms and drag.
- Two fingers moving together: scroll in any direction, including diagonal and reversal.
- Two fingers spreading/pinching: a real two-pointer input stream centered at the pointer; orientation changes during an active pinch are forwarded too.
- Pointer addition, release, cancellation and third-finger interruption terminate streams coherently.

App support determines the gesture's effect. Symmetric pinching needs room around the pointer; move away from the extreme screen edge for zoom. There are no configurable three/four-finger desktop shortcuts. Screen-reader coexistence is not supported; activation is detected and assistance is released rather than capturing incompatible input.

## Recovery

- Bind requests expire after 5 seconds; attempts have generations and paced retries.
- Shizuku arrival/death and helper death/disconnection reset binding state.
- A screen-control request stalled for more than 10 seconds can restart ReachPad's own helper, at most once per minute. Late results from the old helper are discarded.
- Status distinguishes Shizuku stopped, permission needed and reconnection in progress.
- A private two-file history stores only fixed lifecycle names and timestamps, approximately 16 KB per file. Screen-ready/stopped events distinguish a bound helper from a working presentation. No screen content, entered text or network identifiers are logged there.

This does not start a dead Shizuku server or promise recovery after an offline reboot. Prior short Wi-Fi-off tests remain documented separately in [runtime continuity](2026-09-20-runtime-continuity.md); the original outdoor incident is still not reproduced or explained.

## Evidence

- Pure-Java tests cover pointer bounds, asymmetric regions, healthy-side changes, range rollback, Hangul and gesture sequencing. Locale and release-surface checks pass.
- Device injection test confirmed original raw/local coordinate consistency at both edges and delivery of two-finger streams.
- In the user's original split-screen video player, pointer x=1200 on the seek bar yielded about 0:47 of a 3:06 video, and x=1400 yielded 1:38. Before the fix those positions had sought about 1:37 and 2:29 respectively. Native seek padding/quantization can produce small differences; the dead-gap-sized error is removed.
- The in-app miniplayer close button at physical (1694, 1665) was clicked through the actual Pad driver and the miniplayer closed. This covers the previously unreachable far-right region.
- The user explicitly confirmed that the seek-bar pointer position and tap-then-press drag now work with physical fingers.
- Healthy-right whole-trackpad device test passed: taps=3, diagonal scroll dx=70, dy=100. Healthy-left setting was restored.
- On the final APK, another forced helper termination recovered the screen in about 2.1 seconds; the previous app process and Shizuku server survived, and ReachPad’s keyboard remained selected.
- Deliberate helper termination kept the same Shizuku server and app process. The new helper connected about 0.48 s after recorded death; the assisted display subsequently returned, with one output display rather than leaked duplicates.

- Android `ScaleGestureDetector` recognized 1.6667× zoom and `GestureDetector` delivered a real long-press callback in the final paced device test; raw/local and both-edge checks passed in the same run. Synthetic MOVE events must be spaced across frames: a burst in one millisecond is coalesced by Android and cannot exercise a sustained pinch.
- Notification and quick-settings panels render without stretched bottom text; Back closes the shade after removal of the redundant input clone.

 Raw screenshots, UI dumps and process diagnostics remain only under ignored `.local/input-repair/`; they are not public release materials.

## Open-source choice and limits

Existing Shizuku and scrcpy-derived shell initialization remain attributed in `THIRD_PARTY_NOTICES.md`. [scrcpy's control documentation](https://github.com/Genymobile/scrcpy/blob/master/doc/control.md#pinch-to-zoom-rotate-and-tilt-simulation) describes simulated multiple fingers. ReachPad needs recognition of actual local pad fingers and its own healthy-region mapping, so this change implements a small standalone gesture kernel instead of adding the desktop control stack. No new third-party dependency or copied gesture code was added.

The additional GPU composition can affect latency, power and protected content. No CPU readback, video encoding or network transport is used, but this is not a measured zero-latency or battery improvement. Sustained outdoor use, doze, all applications, protected video, every OS/device and landscape remain unverified. Android system PiP edge stashing is normal behavior. Additional clipping caused by ReachPad was suspected but has not been established by an assistance-on/off comparison; it is not a confirmed defect. This is distinct from the video app’s in-app miniplayer and the repaired input-coordinate mismatch. The APK is still an alpha.
