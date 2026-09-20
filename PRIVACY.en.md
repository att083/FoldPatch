# Privacy and permissions

[한국어](PRIVACY.md) · [README](README.md)

This notice applies to the public release build of FoldPatch v0.1.0-alpha.4.

## Processing on your phone

- Setup progress and consent to accessibility and keyboard switching are saved inside the app. FoldPatch does not receive or store Shizuku pairing codes or Wi-Fi passwords.
- The working touch side, visible widths, toolbar position/size, pointer speed and related preferences are saved locally.
- The previous keyboard identifier is saved so FoldPatch can attempt to restore it. Typed text is not saved.
- The accessibility service handles display overlays, touch-coordinate conversion and touchpad input. It does not request permission to retrieve accessibility nodes or their text.
- The keyboard sends the keys you press to the selected input field. It has no learning, typing history or upload function.
- The Shizuku helper composes display layers and changes app bounds and input coordinates. It does not create screenshot or recording files.
- Recovery after an unexpected exit can require a temporary shell-only file containing screen dimensions. It contains no app content or typed text.

## Network and diagnostics

FoldPatch has no Internet permission, advertising or analytics SDK. It does not upload screen contents, key input or usage statistics. Links to Shizuku open an external browser; that website's policy applies. Shizuku is a separate application.

Small local diagnostics record timestamps and fixed names for connection, fold and lock-state changes. The app retains two files of about 16 KB each. They contain no screen contents, input text, network names or addresses and are removed when the app is uninstalled.

Connection and display errors can also appear in Android system logs. Development builds include input test screens that can log their sample text. **Those test screens are not registered in the public release build.**

## Why these permissions are needed

| Permission or component | Purpose |
| --- | --- |
| Shizuku | Privileged display layout, touch injection, accessibility activation and keyboard selection |
| Display over other apps | Checking FoldPatch's overlay authorization |
| Accessibility service | Displaying overlays alongside system gestures and processing touch coordinates and touchpad input |
| Input method | Providing the keyboard on your selected side |
| Notifications / foreground service | Showing running status and returning to settings |
| Boot receiver | Waiting to reconnect if automatic startup is enabled; it cannot start Shizuku itself |

Setup explains system access before requesting Shizuku authorization. Accessibility activation and keyboard switching receive separate explanations and consent. Installing, pairing and approving system permissions require your actions.

Turning off FoldPatch or folding the phone removes its display arrangement and attempts to restore the previous keyboard. Android's accessibility authorization may remain enabled, but FoldPatch does not collect touchpad input while turned off. To revoke access, disable it in Android accessibility settings and Shizuku's authorized-apps list.

Uninstalling removes FoldPatch's internal settings. Before sharing diagnostics, remove notifications, account names, typed text, IP addresses and pairing codes. Full system logs and personal screenshots are not required for a public issue report.
