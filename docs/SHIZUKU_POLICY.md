# Shizuku integration and distribution review

Reviewed: 2026-09-20. Scope: the first-run setup flow and the current GitHub APK distribution plan. This is a source-based engineering assessment, not an approval from Google, Shizuku, Samsung, or a legal clearance for every jurisdiction.

## Findings and decisions

- **Official install guidance is an intended integration path.** The [Shizuku API guide](https://github.com/RikkaApps/Shizuku-API#requirements) directs dependent apps to guide users to install Shizuku and links its official download page. FoldPatch opens that page in a browser. It does not bundle, modify, silently install, or redistribute the manager APK.
- **Keep library notices.** The linked [Shizuku API is MIT licensed](https://github.com/RikkaApps/Shizuku-API/blob/master/LICENSE). Its copyright and permission notice are included in `licenses/Shizuku-API-MIT.txt`, the source export, and APK assets. The separate manager has its own [Apache 2.0 and identity restrictions](https://github.com/RikkaApps/Shizuku#license). FoldPatch keeps its own name, application ID and icon, and does not claim affiliation. Its manifest requests an existing Shizuku permission with `uses-permission`; it does not define a Shizuku-owned permission with `permission`.
- **Use explicit user actions.** Installation, wireless-debugging approval, pairing and Shizuku authorization stay under user control. FoldPatch neither reads nor stores pairing codes. Access to Android system functions is described before the Shizuku authorization prompt. Accessibility activation and keyboard switching receive separate explanations and affirmative consent before FoldPatch is turned on.
- **Do not sell setup as ordinary permission-free installation.** Shizuku requires elevated ADB privileges and a separate startup procedure. The [official startup guide](https://shizuku.rikka.app/guide/setup/) describes wireless debugging and restart limitations. Setup recommends trusted Wi-Fi; it does not make public Wi-Fi a recovery requirement or ask users to disable device protection features. OEM menus and restrictions can differ. Successful use on one Fold3 is not general compatibility certification.

## Google Play is a separate, unresolved decision

Shizuku integration by itself is not evidence that the complete app will pass review. The [AccessibilityService policy](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en) requires an appropriate declaration and review, and non-accessibility-tools need a separate prominent disclosure and affirmative consent. FoldPatch targets damaged hardware; this does not establish eligibility for the disability-focused `isAccessibilityTool` exemption. The app does not declare that flag.

The [Device and Network Abuse / foreground-service policy](https://support.google.com/googleplay/android-developer/answer/16559646?hl=en) also applies. User permission alone does not authorize bypassing platform security controls. The current `ShellBridge.enable_pointer_accessibility` writes the system accessibility setting through Shizuku instead of the normal Android accessibility approval UI; the helper also selects an input method and uses privileged display/input interfaces. **Those paths need a dedicated Play compliance review and potentially architectural changes before a store submission.** Adding disclosure is not a certification that these paths comply. Secure-screen behavior and foreground-service declarations must also be reviewed for any store release.

For the current GitHub plan, no prohibition on official Shizuku install guidance or use of the MIT API was found in the cited documents. That limited conclusion does not assert that every privileged behavior or every future distribution channel is approved. Do not describe this build as Play-approved or officially endorsed by Shizuku.

## Setup behavior implemented from this review

1. Choose the working side, including an entry point on both physical sides of an unfolded screen.
2. Explain the dependency and detect missing installation, stopped Shizuku, missing authorization, or helper connection separately.
3. Guide developer options, trusted Wi-Fi/wireless debugging, and pairing/startup; provide the official guide for OEM differences. Instructions resume after returning from another app.
4. Open the system overlay permission UI. Explain accessibility activation and keyboard switching on separate consent screens.
5. Turn on FoldPatch explicitly, save a reversible screen-range preview, practice locally, and offer optional notifications/automatic unfolding behavior.
6. Recheck live connection and overlay permission even after completion. Keep previously confirmed side, ranges and consent; existing installations are migrated without re-running the tutorial.

Development verification: targeted host setup-state scenarios cover interrupted setup, migration, revoked permissions and reconnection. A subsequent [clean Android 15 emulator test](research/2026-09-20-first-run-validation.md) exercised actual OS permission dialogs, Shizuku wireless pairing/startup, disclosures, range preview, practice, completion and recovery. APKs were provisioned through ADB; browser sideloading, clean Samsung setup and novice usability remain unverified. The existing user device was not reset or changed for this test.
