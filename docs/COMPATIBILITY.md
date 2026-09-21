# Android·Galaxy 호환성

[English](COMPATIBILITY.en.md)

**공개 alpha.5:** compile/target API 37, 최소 Android 15/API 35입니다. Fold3 / Android 15에서 이 버전의 업데이트·입력·접기와 펼치기를 확인했습니다. [검증 기록](RELEASE_REVIEW.md#공개-alpha5--target-api-37)을 따로 확인하세요. 아래는 alpha.4 및 이전 개발 단계의 결과입니다.

**Android 14는 지원하지 않습니다.** API 34 AOSP 가상 기기의 재배치 과정에서 OS 프로세스가 종료됐습니다. alpha.5부터 APK도 Android 15 이상에서만 설치됩니다. 공개 alpha.4는 Android 14에 설치할 수 있었지만 지원을 의미하지 않습니다.

**실기기 검증은 삼성 Galaxy Z Fold3 / Android 15 / One UI 7에서 진행했다. Android 16·17은 가상 기기에서만 확인했으며 실기기 테스트는 아직 진행하지 못했다.**

확인일: 2026-09-21. 지원 목표는 손상된 Galaxy Fold의 Android 15부터 최신 정식 Android 17까지다. 최소 사용 조건은 Android 15이며, 설치 가능 버전과 실제 기능 검증 범위는 다르다. `targetSdkVersion`은 지원하는 최대 Android 버전이 아니다.

## 기기와 OS를 구분해서 확인한다

[Galaxy Z Fold7 공식 발표](https://news.samsung.com/global/samsung-galaxy-z-fold7-raising-the-bar-for-smartphones)는 Android 16·One UI 8, [Galaxy Z Fold8 공식 사양 비교](https://www.samsung.com/ie/support/mobile-devices/what-is-the-difference-between-the-galaxy-z-fold8-ultra-and-z-fold8/)는 Android 17·One UI 9를 명시한다. 기기 이름만으로 현재 설치된 OS를 판단하면 안 된다. 지역·통신사·업데이트 상태도 기록한다.

| 환경 | 확인 범위 | 아직 확인하지 않은 범위 |
| --- | --- | --- |
| Fold3 SM-F926N / Android 15 / One UI 7 | 기존 실기기에서 화면 재배치·입력·키보드·접기 복구·집 안 연결 유지 확인 | 장시간·외출 등 남은 과제는 별도 기록 참조 |
| Google Android 15 / API 35 가상 기기 | 실제 무선 페어링부터 최초 준비·범위 저장·완료·재연결. 통합본 재배치·비대칭 범위·강제 종료/소유권 상실 복구 | 삼성 최초 설치 UI·물리적 접힘 |
| Google Android 16 / API 36 가상 기기 | 새 설치 안내·무선 페어링·권한·범위 저장·클릭·스크롤·핀치·길게 누르기·키보드 편집·종료 복구·재연결. 통합본 재배치·비대칭 범위·강제 종료/소유권 상실 복구 | One UI 8 실기기·물리적 접힘·장시간 사용 |
| Google Android 17 / API 37 가상 기기 | **통합 APK의 대체 재배치·입력·키보드 확인.** 비대칭 범위·복구 프로세스 강제 종료·소유권 상실 후 복구 확인 | One UI 9 실기기·물리적 접힘·하단 제스처·장시간 사용 |
| 다른 Fold 모델·Android 15 이상 | 해당 기기 미검증 | 해당 기기에서의 기능 확인 |

Google 가상 기기의 결과를 삼성 실기기 지원 인증으로 표시하지 않는다. 현재 실기기 검증 모델은 Fold3 하나다. 최신 버전 지원을 목표로 버전별 문제를 고치되, 검증하지 않은 기기를 ‘모두 정상 동작’으로 안내하지 않는다.

**50% + 50% 설정:** Fold3 실기기와 Android 17 가상 기기에서 전체 폭의 입력 좌표·확대·길게 누르기를 확인했다. Fold3에서는 범위 설정 화면으로 50% 저장과 기존 범위 복귀를 확인했고, Android 17에서는 화면 영역을 변경하지 않는 것과 손상 구간이 있는 설정으로의 전환·복구도 확인했다. 같은 가상 기기에서 배포 서명 APK의 새 설치·첫 실행·업데이트를 확인했다. 이후 Chrome에서 로컬 시험 서버의 APK를 내려받아 설치 출처 허용·설치·첫 실행까지 확인했다. 실제 GitHub 다운로드 및 삼성 새 설치 검증은 아니다. Android 16에서는 이 추가 설정을 별도로 재시험하지 않았다.

## Android 16에서 발견하고 수정한 문제

기존 앱은 화면 출력 버퍼를 만들 때 `BLASTBufferQueue(String, SurfaceControl, int, int, int)` 생성자를 호출했다. Android 16에서 이 생성자가 제거되어 Shizuku 연결과 권한 준비는 성공해도 폴드패치 시작이 실패했다.

`FrameMirror`를 공통 생성자 `BLASTBufferQueue(String, boolean)`와 `update(SurfaceControl, int, int, int)`의 두 호출로 바꿨다. Android 15의 이전 생성자도 내부적으로 같은 순서로 동작한다. 버전 번호만으로 분기하지 않고, 기존 동작과 같은 공통 호출을 사용한다. 생성 후 실패하는 경우 기존 정리 경로가 버퍼를 해제한다.

근거: AOSP의 [Android 15 구현](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/graphics/java/android/graphics/BLASTBufferQueue.java)과 [Android 16 구현](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/graphics/java/android/graphics/BLASTBufferQueue.java). 수정 후 Android 16에서 실제 재배치와 입력 전달을 확인했다.

변경 후 Android 15 가상 기기에서도 실제 `NativeScreen`의 시작 → 표시 확인 → 상태 확인 → 종료를 실행했다. 1768 px 화면의 앱 폭을 1592 px로 바꾼 뒤 원래 영역으로 복원했고 복구 기록이 제거됨을 확인했다. Android 15의 전체 최초 준비 흐름은 앞선 검증이며, 이 마지막 확인은 변경한 출력 경로의 회귀 시험이다.

## Android 17에서 발견한 문제와 통합한 해결 경로

Shizuku의 페어링·실행·허용은 성공한다. 실패 지점은 `ReflowGuard.acquire()`가 화면 폭을 바꾸기 위한 영역을 확보할 때다. 기존에 사용하던 feature 4의 `WindowedMagnification:0:31`이 시스템 소유의 `(organized)` 상태여서, 기존 소유권을 빼앗지 않는 보호 검사에 의해 시작을 중단한다.

앱을 강제 종료한 후에도 같았으며, 가상 기기를 재부팅하고 부팅 완료 후 폴드패치나 UI 자동화 도구를 실행하기 전에 다시 확인해 같은 상태임을 확인했다. 이때 폴드패치 프로세스가 없고 활성화한 접근성 서비스도 없었다. 시험 도구가 남긴 일시적 소유권으로 설명되는 현상이 아니다.

AOSP Android 17의 [AppZoomOutController](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/libs/WindowManager/Shell/src/com/android/wm/shell/appzoomout/AppZoomOutController.java)는 초기화 시 [TopLevelZoomOutDisplayAreaOrganizer](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/libs/WindowManager/Shell/src/com/android/wm/shell/appzoomout/TopLevelZoomOutDisplayAreaOrganizer.kt)를 등록한다. 이 조직자가 사용하는 `FEATURE_TOP_LEVEL_ZOOM` 값이 기존 앱이 사용한 4다. 가상 기기의 관찰과 이 공식 구현이 일치한다.

또한 [DisplayAreaPolicy](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/wm/DisplayAreaPolicy.java)의 화면층 구성이 바뀌었다. 시험 환경에는 `SystemUiFit`이 별도로 변환하던 `OneHanded:15:15`·`OneHanded:17:17` 영역이 없다. 소유권 검사만 제거해서 해결할 문제가 아니다.

후속 [대체 경로 실험](research/2026-09-20-android17-feasibility.md)에서 시스템 소유권을 유지한 재배치와 상태 표시줄·알림창 처리를 확인했다. 좌우 정상 터치 설정과 키보드 시험도 통과했다. 이후 `ReflowPolicy`·`AreaOrganizer`를 추가해 정식 코드에 통합했다. 확인한 공통 부모 구조에서는 feature 3을 사용해 앱·상태바·알림창의 폭을 함께 바꾸며 feature 4·10의 시스템 소유권은 유지한다. 기존 구조에서는 기존 경로를 사용한다. 시작 전 점유 검사, 실행 중 소유권 상실 감지, 경로별 복구 기록을 포함한다. 모든 Android 17·One UI 9 기기에서 동작한다는 뜻은 아니다. 시스템 조직자를 강제로 교체하거나 OS 기능을 꺼서 통과한 것으로 처리하지 않았다.

화면 처리 통합 당시의 회귀 시험은 Android 15·16·17 가상 기기에서 진행했다. 이후 디자인 업데이트를 Fold3 실기기에 설치해 설정·화면 범위 조정·키보드와 조작바 표시 및 일부 입력 동작을 확인했다. [README의 실기기 캡처](../README.ko.md#무엇을-할-수-있나요)를 참고한다. 이는 장시간 사용·모든 접힘/잠금 전환·프로세스 강제 종료 복구 시험을 전부 재실행했다는 뜻은 아니다. Android 16·17 실기기 테스트는 여전히 진행하지 못했다.

Android 17 가상 기기에서는 앱·상태 표시줄·알림창과 달리 내비게이션/작업표시줄이 물리 화면 폭을 계속 요청한다. 넓은 손상 범위와 하단 제스처·제조사별 작업표시줄의 배치는 별도 확인이 필요하다. 가상 기기 테스트 결과를 모든 시스템 화면의 정상 동작 보장으로 안내하지 않는다.

## 이번 시험의 조건과 한계

- Android 16 이미지는 Google APIs x86_64 API 36 revision 7, Android 17은 API 37.0 revision 6을 사용한다. 가상 내부 화면은 1768 × 2208 / 420 dpi다. 최신 Android의 모든 QPR·보안 패치·제조사 빌드를 시험했다는 뜻은 아니다.
- APK 설치는 시험용 ADB로 진행하고, Shizuku 페어링·시작과 앱 권한 허용은 실제 UI로 진행한다. 브라우저 다운로드부터 패키지 설치까지는 별도 시험 대상이다.
- AOSP 가상 기기의 확대 인식 최소 간격은 32 mm다. 기존 자동 시험의 400 px 동작은 420 dpi에서 이 간격에 못 미쳤다. 시험 제스처를 정상 터치 영역 안에서 충분히 벌어지도록 수정한 뒤, 대상 앱의 실제 `ScaleGestureDetector` 확대 결과를 확인했다. 앱의 핀치 인식 로직을 바꾸거나 검증 조건을 제거하지 않았다.
- 가상 기기에는 하드웨어 키보드가 연결된 것으로 표시된다. 키보드 시험에서는 ‘하드웨어 키보드 사용 중에도 화면 키보드 표시’를 켰다. 물리 키보드가 없는 휴대전화 환경과 시험 조건을 맞추기 위한 설정이다.
- 입력 시험은 합성된 터치패드 동작을 실제 Binder 입력 전달과 대상 앱 수신까지 확인한다. 손상된 실제 터치 패널, 실제 손가락 감각·진동, 삼성의 접힘 전환을 대신하지 않는다.
- Android 16에서 Shizuku 페어링 성공 로그가 남고 시작도 가능했지만 알림이 ‘진행 중’으로 남는 장면이 있었다. 실제 Shizuku 실행과 폴드패치 연결을 확인해서 진행했다. 이 외부 앱 알림 현상의 일반적 재현 조건은 확정하지 않았다.

## 새로운 One UI 버전에서 필요한 확인

화면을 변경하는 기능은 일반 앱 화면 API만으로 구현되지 않는다. Shizuku와 Android 내부 화면·입력 인터페이스에 의존하므로 OS·One UI 업데이트마다 다음 항목을 확인해야 한다.

1. Shizuku 시작·권한 허용·제어 프로세스 연결.
2. 기존 앱 내용의 폭 재배치와 가운데 구간 제외, 미리보기 취소·저장.
3. 왼쪽·오른쪽 정상 터치 선택, 화면 양 끝 좌표, 스크롤·드래그·핀치·길게 누르기.
4. 정상 터치 쪽 키보드와 기존 키보드 복귀.
5. 알림창·홈·최근 앱, 잠금·화면 꺼짐, 내부·외부 화면 전환.
6. 연결 또는 프로세스 종료 시 원래 화면 복구와 재연결.

Android의 공식 [16 전체 앱 변경점](https://developer.android.com/about/versions/16/behavior-changes-all)과 [17 전체 앱 변경점](https://developer.android.com/about/versions/17/behavior-changes-all)도 함께 검토한다. 현재 앱의 SDK target을 유지한다고 내부 API 변경을 피할 수 있는 것은 아니다.
