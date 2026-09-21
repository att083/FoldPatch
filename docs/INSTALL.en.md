# Install and use FoldPatch

[English](INSTALL.en.md) · [한국어](INSTALL.md) · [日本語](INSTALL.ja.md) · [简体中文](INSTALL.zh-Hans.md) · [繁體中文](INSTALL.zh-Hant.md)

[Back to README](../README.md)

FoldPatch is an early alpha. Hardware testing has used a **Galaxy Z Fold3 (SM-F926N), Android 15 / One UI 7**. Android 16 and 17 have emulator tests only. Long-duration use and every system transition have not been verified. Android 14 is the minimum installation version, not a claim of support for every device. [Compatibility and evidence](COMPATIBILITY.en.md).

## 1. Get the APK

Use the installable **`foldpatch-release.apk`**; you do not need to build the app. The first GitHub release is being prepared. Once published, get the APK from the repository’s **Releases → Assets**. “Source code (zip)” is not installable on Android. A file ending in `-unsigned.apk` is not the user release either.

Open the APK. Android may ask you to allow installation from the browser or file app that opened it. Allow that source only if you trust where you obtained the file. Existing installations can update without losing settings when both APKs use the same signing key. Development and public builds may use different keys; do not uninstall automatically to bypass a signature mismatch.

## 2. Choose your working side

Open FoldPatch. First-run setup asks which side of the **inner screen** still responds to touch. You can do this while the phone is folded, using the cover screen. On the inner screen, the starting controls are available at both edges.

The selected side is where settings, the touchpad, the toolbar, and the keyboard will be placed. It does not swap your apps or change the visible widths. When changing this setting later, confirm on the new side; an unconfirmed change returns to the previous side after 15 seconds.

## 3. Prepare Shizuku

FoldPatch needs Shizuku to access the system display and input functions it uses. Shizuku is a separate app. **No root or permanently connected computer is required.**

The setup flow skips steps already completed and helps you through the remaining ones:

1. Install Shizuku through its [official download page](https://shizuku.rikka.app/download/), then return to FoldPatch.
2. Enable Android developer options. On Samsung, open **Settings → About phone → Software information** and tap **Build number** repeatedly; unlock if asked.
3. Connect to a **trusted Wi-Fi network** and enable **Wireless debugging** in developer options.
4. In Shizuku, follow **Start via Wireless debugging → Pairing**. Open Android's **Pair device with pairing code** screen and enter that code into the Shizuku notification, not into FoldPatch. Allow Shizuku notifications if needed for this step.
5. Return to Shizuku and tap **Start**. Check that it reports running, then return to FoldPatch.
6. Allow FoldPatch in the Shizuku permission prompt. If you previously denied it, retry or use Shizuku's authorized-apps list.

Exact system labels vary. See the [official Shizuku setup guide](https://shizuku.rikka.app/guide/setup/) if a step differs on your phone. Do not share pairing codes or Wi-Fi passwords in an issue report. Public Wi-Fi is not a required recovery method.

**After a reboot:** the existing non-root Shizuku service stops. You may need to start it again. Conditional startup behavior is not a promise of automatic recovery everywhere. FoldPatch remembers your screen widths and preferences; restarting Shizuku does not mean recalibrating everything.

**During ordinary use:** FoldPatch's phone-local connection is separate from a computer's wireless ADB connection. Short Wi-Fi-loss tests passed on the Fold3, but every power/network condition has not been tested. If Shizuku stops, follow the reconnection prompt; FoldPatch cannot replace its startup process.

## 4. Allow FoldPatch and keyboard access

In FoldPatch setup:

1. Allow **display over other apps**, then return.
2. Read the accessibility explanation and consent if you want to proceed.
3. Read the keyboard-switching explanation and consent if you want to proceed.

When you turn on FoldPatch, Shizuku enables the FoldPatch accessibility service and selects its one-sided keyboard. Folding the phone or turning FoldPatch off attempts to restore the previous keyboard. You can leave setup without agreeing; FoldPatch will not be ready to use.

The public release has no Internet permission, ads, or analytics SDK, and does not save screen images or typed text. Small local lifecycle diagnostics contain technical state information. [Privacy and permission details](../PRIVACY.en.md).

## 5. Set the visible widths

Unfold in portrait orientation and unlock. Turn on FoldPatch, then open the screen-area adjustment step.

- The ruler shows the **unmodified physical screen**: each outer edge is **0%**, and the center is **50%**.
- Use the two sliders or ± buttons on your working side to match the visible width measured inward from each outer edge. Each side accepts 20–50%, in 0.5% steps.
- Open the preview. Save it only if it fits; if left unconfirmed, it returns to the previous range after 15 seconds.
- Complete the pointer/toolbar practice and return to your usual apps.

If the whole screen is visible, set **left 50% · right 50%**. The original full-width screen stays in place with no center gap, while the touchpad can control the opposite side or the whole screen. If both sides respond to touch, choose whichever side is more convenient.

FoldPatch remembers completed setup stages. If Shizuku later stops, it asks for the missing preparation rather than clearing your screen settings.

## 6. Use the touchpad

Select the working side on the toolbar for normal touch there. Select the opposite side or the whole screen to use the working side as a touchpad.

| Action | Gesture on the working side |
| --- | --- |
| Move / click | Move one finger / tap without moving. |
| Drag | Tap once, then touch again and hold while moving. Alternatively hold until the haptic cue and then move. |
| Long-press | Keep holding at the target. |
| Scroll | Move two fingers together vertically, horizontally, or diagonally. |
| Zoom | Place the pointer at the target, then spread or pinch two fingers. |
| Double-click | Tap twice quickly in the same place. |

The receiving app decides what a gesture does. Zoom requires content that supports zooming.

Tap the toolbar handle to cycle between one button, selected buttons, and all buttons; drag the handle to move it. Size, orientation, visible buttons, and pointer speed are adjustable. The toolbar remembers its stage and each side's position. Bottom-edge swipes are routed as system gestures; ordinary touchpad motion starts inside the area. System gesture support still depends on the device/OS verification limits above.

Select any input field to type using the one-sided keyboard. **Korean and English input only**, even if the app interface is Japanese or Chinese. Editing tools provide cut, copy, paste, select all, and cursor movement where the receiving field permits them. Select text before copying or cutting. Autocomplete, voice input, and clipboard history are not included.

## Recovery, turning off, and removal

| Situation | What to do |
| --- | --- |
| Reconnecting | Wait briefly. FoldPatch can reconnect if Shizuku is still running and authorized. |
| Shizuku stopped / authorization missing | Start Shizuku or authorize FoldPatch, then retry the connection check. |
| Toolbar is unreachable | Fold the phone, open FoldPatch on the cover screen, and use settings. |
| Wrong screen range | Let an unsaved preview expire. For saved changes, use Help and recovery to restore the previous saved range. |
| Want to stop | Turn off **Use FoldPatch**. Also disable **Turn on when unfolded** if you do not want it to resume. Other apps are not closed. |
| Display remains wrong after an unexpected exit | Allow a moment for recovery, then fold and open FoldPatch on the cover screen. Restart the phone if necessary. |
| Previous keyboard did not return | Select it in Samsung Settings → General management → Keyboard list and default. |

Before uninstalling, turn off FoldPatch and automatic start, and confirm normal display/keyboard behavior. Uninstalling deletes FoldPatch settings but does not uninstall Shizuku. Pairing, permissions, and the initial installation require your own confirmation; the app does not silently grant itself access.

Interface languages follow Android settings: Korean, English, Japanese, Simplified Chinese, and Traditional Chinese. App language selection does not add Japanese or Chinese typing support.
