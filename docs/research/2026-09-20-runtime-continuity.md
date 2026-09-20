# Same-boot continuity investigation

Scope: keep ReachPad operating after initial Shizuku startup while Wi-Fi is unavailable. Offline cold boot is outside this investigation. No production app source or saved screen geometry was changed.

## Device and preconditions

- Galaxy Z Fold3 SM-F926N, Android 15 / One UI 7, ReachPad 0.1.0-alpha.3.
- Shizuku 13.6.0.r1086.2650830c; WRITE_SECURE_SETTINGS already granted.
- Developer options, USB debugging and wireless debugging initially enabled. Device unplugged, unfolded, awake and unlocked. USB debugging is a setting here, not a requirement to keep a cable connected.
- Shizuku boot receiver enabled by default in the matching release; package inspection found no component override disabling it. An actual reboot was not tested.
- No explicit device-idle allowlist entries found for ReachPad or Shizuku. This alone does not establish Samsung's separate sleeping-app or battery settings.

## Method and observations

A temporary, separate diagnostic APK ran under an ordinary app UID. Its temporarily granted WRITE_SECURE_SETTINGS permission restored wireless debugging independently of the ADB connection. It targeted API 28 solely to use the legacy Wi-Fi toggle API for this local test; this is not a proposed production implementation. A local shell observer sampled process identity and display-area organizer ownership about every two seconds. App-local logs separately recorded Wi-Fi and debugging states.

| Test | Observed result |
| --- | --- |
| Wireless debugging off for about 40 seconds, USB debugging on | 17 offline samples retained the same Shizuku server, ReachPad app, control helper and reflow guard. Display-area organizer remained present. |
| Wi-Fi off for about 40 seconds, USB debugging on | Wi-Fi became disabled and Android also disabled wireless debugging. Original server, app, helper and guard survived; 22 samples with wireless debugging off retained all processes and organizer ownership. |
| Deliberate termination of ReachPad's control helper, Shizuku left running | New helper and reflow guard appeared without opening ReachPad settings. Recovery was observed by the eight-second check; notification returned to active. |
| Wi-Fi off again after the helper rebound, about 40 seconds | Fresh helper and guard, original Shizuku server and app remained alive in all 23 wireless-debugging-off samples; organizer remained present. |

The guard's lease expires after ten seconds without updates, so its uninterrupted survival during the offline intervals is evidence of continued control-loop activity, beyond just the app process existing. These tests did not record human touch delivery during the offline interval, prolonged idle/doze, switching to another access point, a cellular data session, reboot, or both USB and wireless debugging being disabled. The earlier outdoor failure is **not reproduced or explained** by these results.

The initially running helper was older than the Shizuku server. Its startup provenance was not established, so Wi-Fi was tested again after the deliberate helper restart to cover a fresh application-initiated binding. That test also retained the same processes and display organizer.

Wi-Fi restoration did not immediately restore wireless debugging: the first attempt was reset by Android while Wi-Fi was coming up. The diagnostic app's delayed retry restored it. This restoration was performed by the test harness, **not evidence that Shizuku or ReachPad automatically restarts on returning home**. Desktop ADB ports changed, which is separate from the phone-local app connection.

## What the sources establish

- Android 15's [AdbService](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android15-release/services/core/java/com/android/server/adb/AdbService.java) stops adbd in `stopAdbd()` only when both USB and Wi-Fi ADB are disabled. This explains why retaining USB debugging can matter. It is not an OEM-wide process-survival guarantee.
- The [Shizuku setup guide](https://shizuku.rikka.app/guide/setup/) recommends retaining USB debugging/developer options and allowing background activity when diagnosing unexpected stops.
- [Shizuku 13.6 release notes](https://github.com/RikkaApps/Shizuku/releases/tag/v13.6.0) add non-root startup on Android 13+ on a trusted WLAN. The matching [BootCompleteReceiver](https://raw.githubusercontent.com/RikkaApps/Shizuku/v13.6.0/manager/src/main/java/moe/shizuku/manager/receiver/BootCompleteReceiver.kt) responds to boot broadcasts, requires the appropriate prior launch mode and permission, enables ADB settings, and briefly discovers the local TLS endpoint. This is conditional boot startup; the inspected receiver does not watch arbitrary later Wi-Fi reconnections. Do not promise restart every time the user returns home.

## Implementation recommendations at the time of this investigation

**Later update:** recommendations 2–4 below were implemented in the subsequent [input and recovery work](2026-09-20-input-and-recovery.md). They are retained here as investigation history, not an outstanding implementation list. Extended real-use validation remains open.

1. **Keep the existing local control architecture.** Network loss should not trigger helper teardown. Guide users through retaining USB debugging and checking relevant Samsung background restrictions; do not require public-WLAN debugging.
2. **Harden reconnect state.** `Bridge.java` has a `binding` flag without an expiry and no explicit server-death reset. Add a bounded bind wait, generation-aware callbacks, death handling and paced retry. These are code risks, not established causes of the past outdoor failure. The ordinary helper-death path recovered in this test.
3. **Make loss and recovery visible accurately.** In `NativeService.refresh`, the null-helper branch returns without refreshing the notification. Distinguish helper reconnecting, Shizuku stopped and permission revoked; keep a reachable recovery entry point on the healthy side. Do not imply that restarting ReachPad alone can restart a dead privileged server without a bootstrap path.
4. **Record a small private lifecycle history.** Store timestamps and states for server/helper death, bind attempts, power/fold events and engine readiness. Exclude screen content, app lists, network names/addresses and input text. This allows diagnosis of a real outdoor failure.
5. **Test ordinary use after those changes.** Screen-off/idle, fold/unfold, longer Wi-Fi-free use, network changes, and recovery after server restart are separate acceptance cases. Reboot should have a clear conditional recovery explanation rather than a guarantee of unattended restart everywhere.

Cleanup: the temporary diagnostic APK (including its granted permission) and on-device observer/log files were removed. Wi-Fi, USB debugging and wireless debugging were restored to their original enabled states. ReachPad's screen range and healthy-side preference were preserved. Local raw diagnostic evidence remains under the ignored `.local/continuity-probe/` directory.

Conclusion: these measurements support same-boot use without Wi-Fi under the tested settings. They do not establish universal continuity or identify the user's previous failure. Prioritize reconnect handling, truthful status and lifecycle diagnostics before changing the display engine or claiming the issue solved.
