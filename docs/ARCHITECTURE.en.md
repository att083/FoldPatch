# Display, input and recovery

[한국어](ARCHITECTURE.md) · [Build](BUILD.en.md) · [Compatibility](COMPATIBILITY.en.md)

The normal path is `NativeActivity → NativeService → Shizuku ShellBridge → NativeScreen`. Existing apps stay in their original workspace.

## Display

FoldPatch gives existing apps a layout width equal to the combined visible regions. For a 1768 px screen with 40% usable on each side, the app area is about 1414 px wide. It composes that content into a GPU buffer and displays its two parts at 1:1 size on either side of the gap. It does not change the physical display resolution, density or input viewport.

`ReflowPolicy` selects an area based on the actual display tree and ownership. The older path transforms notification/status layers separately; the Android 17 common-parent path reflows their width with app content. A competing system organizer is not intentionally replaced. Ownership can still change between checking and registration; there is no atomic platform API to remove that race entirely.

`FrameMirror` uses a BLAST buffer without CPU readback or video encoding. The visible overlays do not clone app input windows. Buffers and the output display are released on stop, folding and connection loss. Protected content is not guaranteed to display.

At 50% + 50%, `NativeScreen` creates only the transparent pointer overlay. It does not take display-area ownership, resize apps, create mirror buffers or scale system UI. Switching between full width and a gap first restores the previous display changes.

## Input and keyboard

`TouchSide` stores the working side and rolls back an unconfirmed side change after 15 seconds. Left/Right/All are screen regions, independent of app windows. Touch, keyboard and toolbar positions are mapped back into the original display coordinates; injected events are not processed again as physical touch. Input starting in the damaged gap is discarded.

`TrackpadGesture` recognizes pointer movement, click/double-click, long-press, drag, two-finger scrolling and pinch. Scrolling produces a touch stream at the pointer; pinch produces a two-pointer stream. Pointer changes and cancellation preserve Android's event ordering. The receiving app determines which gestures have an effect.

`ReachKeyboard` is an Android input method sized and placed within the selected working side. It retains the previous keyboard identifier for restoration. Recovery cannot guarantee an immediate keyboard switch after every process-failure combination; installation guidance includes manual recovery.

## Recovery

`ReflowGuard` records original/applied bounds and the selected path, uses a file lock to prevent overlapping controllers, and watches the parent heartbeat. Process loss triggers restoration. `AreaOrganizer` reports added areas and ownership loss. If another organizer takes ownership, FoldPatch preserves the recovery record and waits for that area to become available instead of modifying its new owner's state.

- An app Binder disconnect releases display changes and attempts keyboard restoration.
- A helper disconnect retries connection to a still-running, authorized Shizuku.
- A display-control timeout can trigger a bounded helper restart; it cannot start Shizuku itself.
- Cover display, lock, screen-off and landscape transitions disengage FoldPatch.
- An unsaved range preview expires after 15 seconds; unconfirmed settings are recovered on restart.

Unfolding-based automatic startup is separate from Shizuku startup. This is not a promise that a force-stopped app will restart automatically.

## Privilege boundary and experiments

Binder calls check the FoldPatch UID and accept a limited set of operations. The debug ADB registration provider requires DUMP permission and a shell UID; it is not registered in release builds. [Privacy and permissions](../PRIVACY.en.md) describes local state and diagnostics.

Older VirtualDisplay/GL experiments (`MainActivity`, `ReachService`, `ControlPanel`, and `experiments/`) remain as engineering references and are not the normal startup path. Release builds block external entry to `MainActivity` and exclude test activities from the manifest. Earlier experimental performance results are not measurements of the current display path.
