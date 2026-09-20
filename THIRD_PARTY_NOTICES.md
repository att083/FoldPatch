# Third-party notices

FoldPatch was previously named ReachPad. The root `LICENSE` covers this
project's own code; the license copies in `licenses/` cover the third-party
components described below.

## scrcpy

ShellBridge's ActivityThread initialization and virtual display flag selection were adapted from Genymobile scrcpy's Workarounds.java, FakeContext.java, and NewDisplayCapture.java, licensed under Apache License 2.0.

Source: https://github.com/Genymobile/scrcpy

Reference commit: 19c1261d2e2cbf2b5e6a71a8b64cc1dd3ede06ac. The provider registration also follows its ActivityManager and ContentProvider wrapper call signatures.

```text
Copyright (C) 2018 Genymobile
Copyright (C) 2018-2026 Romain Vimont
```

The code here is modified for a local, UID-restricted Binder bridge and an on-device renderer. It is not scrcpy itself. The upstream license is included in licenses/scrcpy-Apache-2.0.txt.

## Shizuku API 13.1.5

FoldPatch links the api, provider, aidl and shared client libraries from
https://github.com/RikkaApps/Shizuku-API under the MIT License.
See `licenses/Shizuku-API-MIT.txt` for the license and `libs/README.md` for
artifact URLs and SHA-256 hashes. The separate Shizuku manager is not bundled.

## Material Design icons

The control icon vector paths in `res/drawable/ic_*.xml` (excluding the
FoldPatch `ic_launcher.xml` resource wrapper) are derived from Google's Material
Design icons, licensed under Apache License 2.0.
Source: https://github.com/google/material-design-icons
License: `licenses/material-icons-Apache-2.0.txt`.

## Temporary FoldPatch brand mark

`res/drawable-nodpi/foldpatch_mark.png` was generated for this project with OpenAI Image Generation. It is a temporary brand asset, not a Material Design icon. Its source brief and replacement procedure are in `docs/BRAND.md`.
