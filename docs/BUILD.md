# 빌드·서명·공개 준비

[English](BUILD.en.md)

## 도구

Linux 또는 macOS의 Bash 환경에서 다음을 준비합니다.

- JDK 17 (`javac`, `java`, `jar`, `keytool`)
- Python 3
- Android SDK: `platforms;android-36`, `build-tools;35.0.0`, `platform-tools`
- SHA-256 도구 `sha256sum` (macOS는 coreutils 설치)

SDK 경로는 `ANDROID_SDK_ROOT`, 이어서 `ANDROID_HOME`, 기본 `~/Android/Sdk` 순으로 찾습니다. Java는 `REACHPAD_JAVA` 또는 `JAVA_HOME`을 지정할 수 있으며, 지정하지 않으면 PATH의 Java를 사용합니다. Gradle·Android Studio는 필수가 아닙니다. 현재 프로젝트는 Android SDK 명령 도구로 빌드합니다.

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
# JAVA_HOME은 설치한 JDK 17 디렉터리를 지정하거나 Java 17을 PATH에 둡니다.
sdkmanager 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
bash scripts/test.sh
bash scripts/build.sh debug
bash scripts/build.sh release
```

빌드 도중 네트워크로 의존성을 내려받지 않습니다. Shizuku API 13.1.5의 고정 JAR를 포함하며, 빌드 전에 `libs/SHA256SUMS`를 검사합니다. 출처·라이선스는 `libs/README.md`에 있습니다. 컴파일 시 Shizuku의 compile-only annotation 관련 경고와 Java 8 source/target 경고가 발생할 수 있습니다. APK 생성 실패를 뜻하지는 않습니다.

## 산출물과 차이

| 빌드 | 파일 | 특성 |
| --- | --- | --- |
| debug | `build/foldpatch-debug.apk` | 로컬 개발 키로 서명, 디버깅·시험 activity·ADB 등록 provider 활성 |
| release | `build/foldpatch-release-unsigned.apk` | 디버깅 비활성, 시험 activity·ADB provider 제거, 아직 설치 불가 |
| release 서명 후 | `build/foldpatch-release.apk` | 지정한 배포 키로 서명, 설치 가능 |

사용자에게 보이는 이름은 FoldPatch(한국어: 폴드패치)입니다. 기존 설치의 권한·설정·키보드 연결을 유지하기 위해 패키지 ID는 `dev.reachpad`, 최소 Android 14/API 34, 현재 target API 34입니다. 이것은 GitHub 초기 시험 배포 설정이며 Play Store 요건을 만족한다는 뜻은 아닙니다. 다음 공개 버전마다 AndroidManifest.xml의 versionCode를 올리고 versionName을 변경합니다.

브랜딩 변경은 설치 식별자 변경이 아닙니다. 기존 Java 패키지·서비스 이름·환경변수 `REACHPAD_*`·서명 키 별칭·복구 파일명은 호환성을 위해 유지합니다. 과거 연구 기록의 ReachPad 표기는 당시 이름입니다. 아이콘 교체는 [브랜드 가이드](BRAND.md)를 참고하세요.

## 배포 서명

Android는 같은 앱의 업데이트에 서명 일관성이 필요합니다. 공개 배포에는 개발 키를 사용하지 않습니다. 최초 배포 키는 이후 업데이트에도 보관해야 하며, 공개 저장소에 넣지 않습니다. [Android 공식 앱 서명 안내](https://developer.android.com/studio/publish/app-signing)

배포 관리자가 안전한 위치에서 키를 만들고 암호 파일을 준비합니다. 아래 예시는 **새 프로젝트의 첫 키를 만들 때만** 사용합니다. 기존 키가 있다면 재생성하지 마세요.

```bash
mkdir -p .local/signing
chmod 700 .local/signing
keytool -genkeypair -keystore .local/signing/release.keystore \
  -alias reachpad -keyalg RSA -keysize 3072 -validity 10000
```

암호는 터미널 기록이나 명령 문자열에 넣지 않고 입력합니다. 서명 스크립트에는 내용이 아닌 파일 경로를 전달합니다. 별도 키 암호라면 REACHPAD_KEY_PASS_FILE도 지정합니다.

```bash
export REACHPAD_KEYSTORE="$PWD/.local/signing/release.keystore"
export REACHPAD_KEY_ALIAS=reachpad
export REACHPAD_STORE_PASS_FILE="$PWD/.local/signing/store-password.txt"
bash scripts/sign-release.sh
```

`build/foldpatch-release.apk.sha256`도 생성됩니다. 서명 키와 암호를 별도의 안전한 장소에 백업한 뒤 첫 공개를 진행하세요. 키를 바꾸면 기존 사용자의 일반 업데이트가 막힐 수 있습니다. unsigned APK는 GitHub의 사용자 설치 파일로 올리지 않습니다.

## 설치 시험

```bash
bash scripts/run-device.sh DEVICE_SERIAL_OR_IP:PORT
```

이 스크립트는 debug APK를 설치하고 설정을 엽니다. release APK는 `adb install -r build/foldpatch-release.apk`로 설치할 수 있지만, 이미 개발 키로 서명된 동일 앱이 설치된 기기에서는 서명 불일치가 발생합니다. 기존 설정을 지우는 삭제/재설치를 자동으로 하지 않습니다. 새 설치 시험은 별도 기기·에뮬레이터에서 진행하세요. 에뮬레이터는 초기 설정 화면 확인용이며 삼성 화면 재배치 검증을 대신하지 못합니다.

## 공개할 소스 묶음

```bash
python3 scripts/prepare-public.py
```

허용 목록에 있는 소스·리소스·라이브러리·테스트·안내와 README에 쓰는 표지·실기기 스크린샷만 `build/github-ready/`에 복사하고 `build/foldpatch-source.zip`을 생성합니다. Markdown 링크와 HTML 이미지·링크가 공개 묶음 안의 파일을 가리키는지도 확인합니다.

소개글 초안·촬영안·이미지 생성/편집 지시문은 비공개 `.local/`에 보관합니다. 개인 로그, 그 밖의 기기 캡처, 내부 검토·신청 준비 자료, 로컬 경로가 포함된 과거 조사 기록, 서명 키, 암호, 빌드 중간 파일도 공개 대상이 아닙니다.

첫 GitHub 공개는 `build/github-ready/`의 파일로 새 저장소를 구성합니다. 원본 작업 폴더와 `.git` 이력에는 과거 내부 자료가 남아 있으므로 그대로 업로드하지 않습니다. 이 스크립트는 GitHub 저장소 생성이나 업로드를 하지 않습니다.

첫 공개는 prerelease로 게시하고 `docs/RELEASE_NOTES.md`의 검증 범위를 반영합니다. 공개 묶음에서 다시 빌드하고 파일 목록을 확인하는 것이 마지막 로컬 점검입니다.

좌우 터치 입력 통합 시험용 `tests/device/TouchSideProbeActivity.java`는 debug 빌드에만 포함됩니다. synthetic pad 입력을 실제 Binder 입력 전달까지 확인하며, 물리 디지타이저나 접근성의 하드웨어 입력 캡처를 대신 검증하지는 않습니다.

문자열 리소스는 AAPT2에서 `R.java`를 생성한 뒤 Java 소스와 함께 컴파일합니다. debug 전용 `LocalizationProbeActivity`는 언어 선택·문자열 포맷·좁은 조작바 배치를 검사하며 release에는 포함하지 않습니다.
