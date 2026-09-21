# Working with FoldPatch

FoldPatch is an Android app for a damaged foldable inner display. An ordinary installation request means installing the app on the user's phone, not deploying a website or setting up a development environment.

## Choose the relevant guide

- **Install, update or recover a user's phone:** read [Agent installation guide](docs/AGENT_INSTALL.en.md) ([한국어](docs/AGENT_INSTALL.md)), then the relevant parts of [installation and recovery](docs/INSTALL.en.md).
- **Change the app:** read [Contributing](CONTRIBUTING.md), [Build](docs/BUILD.en.md) and, for display/input changes, [Architecture](docs/ARCHITECTURE.en.md).
- **Check support claims:** use [Compatibility](docs/COMPATIBILITY.en.md) and [Current validation](docs/RELEASE_REVIEW.en.md). Older research notes preserve earlier states.

## Installing for someone

Prefer the signed release APK from the repository the user identified. Check the actual release and assets; a source ZIP or unsigned CI APK is not an installable release. If no public APK is available, say so and follow the guide's local-candidate or development-build branch. Never invent a download URL.

Use existing authorization to carry out routine download, inspection and installation steps. Ask only for missing information or a phone action the user must perform. Select the intended ADB device explicitly. Preserve existing app data, screen widths, permissions and signing continuity; do not uninstall, clear data or downgrade to work around an installation failure.

Shizuku setup and phone permission prompts require the user's participation. Computer ADB pairing is separate from Shizuku pairing. Do not bypass the in-app consent/setup flow with settings writes or development entry points. Do not infer the physical damage or working touch side from a screenshot.

Report separately what was downloaded, installed, configured and physically verified. A successful APK install does not mean FoldPatch is ready to use. Give instructions in the user's language and keep technical details focused on the next step.

## Editing this repository

- Preserve the user's existing apps/workspace and keep controls on the working touch side. Left/Right/All refer to screen regions, not individual apps.
- The application ID is `dev.foldpatch`; normal startup is `.NativeActivity`. Do not change these identifiers as a branding cleanup.
- The build uses Bash, JDK 17 and the Android SDK, not Gradle. Follow the build guide for versions and paths.
- For code changes, run `bash scripts/test.sh` and the relevant build. Display/input/recovery changes need targeted device checks; state what could not be tested.
- For documentation-only changes, check links, exported files and formatting with `python3 scripts/prepare-public.py`. Do not install an APK or repeat unrelated device tests.
- New public documents must be added to the allowlist in `scripts/prepare-public.py`. Keep English/Korean links consistent. Preserve others' edits.
- Keep signing material, pairing codes, personal captures/logs and private working notes out of source exports and commits. An installation request does not authorize publishing releases or contacting other people.
