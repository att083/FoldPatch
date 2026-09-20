# Android 17 대체 재배치 경로 실험

확인일: 2026-09-20.

> 이 문서는 최초 실험 시점의 기록이다. 후속 작업에서 대체 경로를 정식 소스에 통합했다. 현재 적용 상태와 검증 범위는 [호환성 문서](../COMPATIBILITY.md)를 따른다.

## 결론과 적용 상태

Android 17 가상 기기에서 시스템이 관리 중인 화면 영역의 소유권을 유지하면서, 기존 앱의 폭을 줄이고 가운데 손상 구간을 건너뛰어 표시하는 대체 경로를 확인했다. 별도 실험 APK에서 실제 재배치·입력·한쪽 키보드가 동작했다.

**정식 코드에 통합한 지원 완료 상태는 아니다.** 실험 코드는 로컬 `.local/android17/prototype/`에 격리했다. 기존 APK와 실제 사용자 폰에는 적용하지 않았다. 이 결과는 Samsung One UI 9 지원 확인을 대신하지 않는다.

## 환경과 변경

- Google APIs x86_64 API 37.0 revision 6, 빌드 CE2A.260420.019 / 15611780.
- 가상 화면 1768 × 2208, 420 dpi. 기본 범위 왼쪽 45%, 오른쪽 45%로 유효 폭은 1592 px.
- 앞선 최초 준비 시험에서 무선 페어링·Shizuku 권한을 확인한 가상 기기를 사용했다. 이번 실험에서는 공식 컴퓨터 시작 경로로 Shizuku를 실행했다.
- 기존 경로가 사용하는 feature 4는 시스템 소유여서 사용하지 않았다. 비어 있는 feature 3을 확보한 뒤 `OneHanded:0:23` 영역 하나의 bounds만 변경했다. 다른 OneHanded 영역의 bounds는 변경하지 않았다.
- 해당 영역 안에 앱과 상태 표시줄·알림창이 함께 있어, 이번 구조에서는 이들이 모두 줄어든 폭에 맞춰 다시 배치됐다. 실험판은 기존 `SystemUiFit` 변환을 수행하지 않는다.
- 기존 display 0에서 앱을 유지한다. 물리 화면 폭을 영구 변경하거나 별도 가상 데스크톱으로 앱을 이동하지 않는다.
- 기존 복구 프로세스와 정리 경로를 사용하되 복구 기록 이름은 실험 전용으로 분리했다.

## 확인한 결과

| 항목 | 결과와 범위 |
| --- | --- |
| 실제 앱 폭 재배치 | 1768 → 1592 px. 번호 1–16이 양쪽에 모두 표시되고 원형 표식의 비율 유지 |
| 상태 표시줄·알림창 | 앱과 함께 1592 px로 다시 배치되는 윈도 정보와 실제 화면 확인 |
| 시스템 소유권 | feature 4와 feature 10의 시스템 조직자 등록 유지 |
| 왼쪽 정상 터치 + 전체 조작 | 양쪽 클릭, 가로·세로 이동을 포함한 스크롤 시험 통과 |
| 오른쪽 정상 터치 + 전체 조작 | 같은 클릭·스크롤 시험 통과 |
| 입력 좌표 | 화면 양 끝의 수신 좌표와 raw 좌표 확인 통과 |
| 확대·길게 누르기 | 대상 앱의 실제 ScaleGestureDetector 배율 1.3519331, long press 1회 확인 |
| 한쪽 키보드 | 양쪽 설정 각각에서 선택·복사·잘라내기·붙여넣기·이모지 이동·한글 조합 시험 통과 |
| 홈·최근 앱 | 표시 화면 확인. 진입은 ADB 키 이벤트로 실행했으므로 하단 손가락 제스처 검증은 아님 |
| 화면 꺼짐·켜짐 | 꺼지면 재배치 해제·복구 기록 제거, 켜고 잠금 해제하면 재배치 재개 |
| 앱 강제 종료 | 재배치 해제·복구 기록 제거. 시스템의 기존 조직자 등록 유지 |

입력 시험은 실제 터치패드 처리 함수에 합성 동작을 전달하고 Binder 입력 주입 및 대상 앱 수신까지 확인했다. 실제 손가락·손상된 패널·햅틱·체감 지연 검증을 대신하지 않는다. 소프트웨어 키보드 시험은 가상 하드웨어 키보드가 연결돼 있어도 화면 키보드가 표시되도록 시험 설정을 켰다.

복구 시험은 가상 기기가 종료돼 한 차례 중단됐고, 다시 부팅해 Shizuku와 앱 연결을 준비한 뒤 전체 순서를 재실행해 통과했다. 첫 최근 앱 카드 시험의 자동 단정문은 `dumpsys window windows`에 없는 `mCurrentFocus`를 찾는 도구 오류가 있었으므로 그 단정문을 앱 결함이나 통과 증거로 사용하지 않았다. 복구 결과는 별도의 재실행 기록으로 판단했다.

## 정식 반영 전에 남은 일

1. OS 번호만으로 선택하지 않고 실제 화면 영역 구조·소유권을 확인해 기존 경로와 새 경로를 선택한다.
2. 실행 도중 다른 시스템 기능이 영역을 가져가는 경우 감지·중단·복구를 처리한다. 이번 실험판은 시작 전 점유 검사만으로 일반적인 충돌 안전성을 입증하지 않는다.
3. 기존 복구 기록과 새 경로를 구분하고 비정상 종료·재연결까지 검증한다.
4. Android 15·16 기존 경로의 회귀 확인 후 하나의 APK로 통합한다.
5. 삼성의 화면 구조, 실제 접기·잠금·하단 제스처, 시스템 대화상자·보호 영상과 넓은 손상 범위를 확인한다. 가상 기기의 내비게이션/작업표시줄은 여전히 물리 폭 1768 px를 요청하므로 별도 처리가 필요한지 확인해야 한다.

## 근거와 재현 자료

공식 구현: [DisplayAreaPolicy](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/wm/DisplayAreaPolicy.java), [WindowManagerPolicy](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/policy/WindowManagerPolicy.java), [DisplayAreaOrganizerController](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/wm/DisplayAreaOrganizerController.java), [TopLevelZoomOutDisplayAreaOrganizer](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/libs/WindowManager/Shell/src/com/android/wm/shell/appzoomout/TopLevelZoomOutDisplayAreaOrganizer.kt).

로컬 실험 산출물은 공개 배포에 포함하지 않는다:

```text
.local/android17/build-prototype.py          실험용 소스 복사 및 변경
.local/android17/prototype/                 격리된 소스와 실험 APK
.local/android17/area-probe.log              독립 영역 확보·폭 변경·반납
.local/android17/prototype-active-*.txt      실제 재배치 상태
.local/android17/prototype-number-screen.png
.local/android17/prototype-notifications.png
.local/android17/right-*.log                오른쪽 정상 터치 시험
.local/android17/recovery-results.txt       화면 끄기·켜기·강제 종료 복구
.local/compatibility/emulator-5564/         입력·키보드 시험 로그
```
