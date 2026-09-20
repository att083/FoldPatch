# App languages

FoldPatch follows Android’s language preferences automatically. The app interface supports Korean, English, Japanese, Simplified Chinese and Traditional Chinese. If none of the preferred languages is supported, the interface falls back to English. Android’s app-language setting can override the system preference; FoldPatch does not add its own language picker.

**This is interface localization, not new keyboard input support.** The FoldPatch keyboard still supports Korean and English only. Japanese/Chinese composition, conversion candidates and other input languages are outside this release. Keyboard action labels follow the interface language; the character layouts and Hangul composer are unchanged.

## Resources

- `res/values/strings.xml`: complete English fallback.
- `res/values-en/strings.xml`: identical explicit English resources, so English takes priority over another supported language in an ordered locale list.
- `res/values-ko`, `res/values-ja`, `res/values-b+zh+Hans`, `res/values-b+zh+Hant`: complete translations.
- `res/xml/locales_config.xml`: languages exposed to Android.

The current native settings, setup/permission explanations, calibration, toolbar, notifications, recovery messages and accessibility labels use Android string resources. Dynamic messages use positional format arguments, allowing each language to order a sentence naturally. Physical left/right coordinates do not change with the display language. Experimental legacy virtual-display screens are not part of the localized product flow.

Follow [Android’s resource localization guidance](https://developer.android.com/guide/topics/resources/localization) when adding translations. Keep default and explicit English resources identical. Do not translate preference keys, action identifiers, component names or keyboard character rows.

## Verification

`bash scripts/test.sh` includes `scripts/check-locales.py`: key parity, format argument compatibility, literal percent handling, default/English parity and missing native UI resources.

The debug-only `LocalizationProbeActivity` verifies Android resource selection (including Chinese region/script variants and English before Korean), fallback for unsupported languages, formatted messages and compact toolbar layouts. It is absent from the public release manifest and DEX.

Verified on SM-F926N / Android 15 / One UI 7:

- 11 runtime locale cases passed, including `en,ko-KR`, `zh-CN`, `zh-SG`, `zh-TW`, `zh-HK`, explicit Chinese scripts and unsupported French fallback.
- 1,804 string resolutions/format checks and 176 toolbar layouts passed. Layout cases cover a 20% working width, horizontal/vertical bars, expanded/collapsed states and 60/75/100/160% sizes.
- Inspected English settings, side selection, raw calibration and preview countdown; Japanese settings/help; Simplified Chinese keyboard settings; Traditional Chinese home settings.
- Checked localized keyboard action labels with an app-language override. Hangul/English character layouts and the composer remain unchanged.
- Existing automated input/geometry/recovery tests and debug/release builds passed. Both debug test activities are absent from the release DEX.
- Restored “follow system language” after testing and verified that existing user preferences were preserved.

Translation wording has not had an independent native-speaker review. Extreme font scaling and every settings page at the minimum usable width have not been visually checked.
