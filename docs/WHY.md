# 왜 폴드패치를 만들었나요?

[English](WHY.en.md) · [README](../README.ko.md)

폴드패치는 직접 쓰던 갤럭시 Z 폴드3의 가운데 화면이 보이지 않고 오른쪽 터치가 되지 않아 시작했습니다. 앱을 바꾸거나 별도 작업 공간으로 옮기지 않고, 남은 화면과 터치로 평소 쓰던 폰을 계속 쓰는 것이 목표입니다. 패널 고장을 고치거나 손상 진행을 막는 기능은 아닙니다.

## 비슷한 불편을 겪은 공개 사례

**가운데가 검게 가려지거나 터치가 고장나는 사례는 2025–2026년에도 올라오고 있습니다.** 2026-09-21에 확인한 서로 다른 사용자의 원글을 최근 순으로 정리했습니다. 댓글이 추가된 날짜가 아닌 원글 게시 시기입니다.

| 게시 시기 | 모델과 사용자가 설명한 증상 | 원문 |
| --- | --- | --- |
| 2026-09 | Z Fold6 접힘 부근의 검은 세로줄이 점점 넓어지고, 해당 부근의 터치 반응도 이상해짐 | [Samsung Members](https://r2.community.samsung.com/t5/Galaxy-Z-Flip-Galaxy-Z-Fold/Galaxy-Z-Fold6-Inner-Screen-Developing-Black-Line-at-Folding/m-p/22910072) |
| 2026-07-16 | Z Fold6 내부 화면의 검은 줄과 화면 전체 터치 불응 — 한쪽 터치가 필요한 폴드패치의 현재 대상 밖 | [Samsung Members](https://r2.community.samsung.com/t5/Galaxy-Z-Fold/Black-line-fold-6/td-p/22537487) |
| 2025-02-14 | Z Fold3 내부 화면 가운데의 검은 띠와 오른쪽 터치 불량 | [r/GalaxyFold](https://www.reddit.com/r/GalaxyFold/comments/1ipfnaz/) |
| 2025-01-06 | Pixel 9 Pro Fold 가운데의 검은 줄, 왼쪽 터치는 되지 않고 오른쪽만 작동 | [Google Pixel Community](https://support.google.com/pixelphone/thread/317231965/google-pixel-9-pro-fold-inside-screen-not-working?hl=en) |
| 2022-07-29 | Z Fold3 내부 화면 가운데의 굵은 검은 줄과 오른쪽 터치 불량 | [Samsung EU Community](https://eu.community.samsung.com/t5/galaxy-z-fold-z-flip/z-fold3-inner-screen-no-longer-working/td-p/5770195) |

이 사례들은 비슷한 불편이 최근에도 존재한다는 근거이며, 고장률·원인·폴드패치 사용자 수를 입증하는 자료는 아닙니다.

폴드패치의 현재 대상은 **양쪽 화면 일부는 보이고 한쪽 이상은 터치가 되는 경우**입니다. 위 사례의 기종들이 모두 지원된다는 뜻은 아닙니다. 실기기 작동 확인은 **Galaxy Z Fold3 / Android 15 / One UI 7**에서 했으며, 자세한 범위는 [호환성](COMPATIBILITY.md)에 정리했습니다.

## 기존 도구와 무엇이 다른가요?

2026-09-20에 확인한 각 개발자의 설명을 기준으로 한 비교입니다. 이 표를 위해 각 앱을 새로 설치해 시험한 것은 아닙니다.

| 도구 | 개발자가 설명하는 용도 | 폴드패치가 함께 해결하려는 부분 |
| --- | --- | --- |
| [Quick Cursor](https://github.com/micku7zu/QuickCursor) | 한 손으로 닿기 어려운 곳을 커서로 조작. 제스처 지원 방식은 OS·유료 기능에 따라 다름 | 커서 조작에 더해, 보이지 않는 가운데를 뺀 폭으로 기존 앱을 재배치하고 정상 쪽에 키보드 제공 |
| [Partial Screen](https://play.google.com/store/apps/details?id=ich.andre.partialscreen) | 화면 일부에서 발생하는 원치 않는 터치 차단 | 오터치 차단과 함께 가려지는 내용 및 반대쪽 입력에 대응 |
| [scrcpy](https://github.com/Genymobile/scrcpy) | 컴퓨터에서 Android 화면을 표시하고 제어. 별도 가상 화면도 지원 | 평소 쓰던 앱을 폰 내부 화면에서 이어 보고 폰만으로 조작 |

확인한 설명에서는 폴드패치가 목표로 하는 **가운데를 생략한 앱 폭 재배치 + 한쪽 터치패드 + 한쪽 키보드**의 조합을 찾지 못했습니다. 모든 제품을 조사했다거나 폴드패치가 유일하다는 뜻은 아닙니다. 필요한 기능이 커서나 터치 차단 하나라면 기존 도구가 더 간단할 수 있습니다.

## 공개하는 이유

같은 상황의 사용자가 APK를 설치해 보고, 다른 개발자가 화면·입력·복구 구현을 검토하고 기기에 맞게 개선할 수 있도록 소스와 검사 코드를 공개합니다. scrcpy 등에서 참고·수정한 코드의 출처와 라이선스는 [외부 코드 고지](../THIRD_PARTY_NOTICES.md)에 기록합니다.

현재 근거는 당사자의 Fold3 실기기 사용과 가상 기기 시험입니다. 외부 사용자의 채택, 수리비 절감액, 기기 수명 연장 효과를 측정한 실적은 아직 없습니다. [검증 범위](RELEASE_REVIEW.md)와 [다음 과제](BACKLOG.md)를 함께 공개합니다.
