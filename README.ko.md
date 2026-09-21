![FoldPatch — 갈라진 화면을 이어, 계속 쓰도록.](docs/media/foldpatch-cover.png)

# FoldPatch · 폴드패치

[English](README.md) · [한국어](README.ko.md) · [日本語](README.ja.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md)

**[APK 다운로드](https://github.com/att083/FoldPatch/releases/download/v0.1.0-alpha.4/foldpatch-release.apk)** · [설치 안내](docs/INSTALL.md) · [호환성](docs/COMPATIBILITY.md)

**가운데 화면이 깨지고 한쪽 터치만 남은 폴드폰을 계속 쓰도록 돕는 Android 앱입니다.**

직접 쓰던 갤럭시 Z 폴드3가 고장 나면서 만든 오픈소스 앱입니다. 당장 수리하기 어려워도, 같은 상황에 있는 사람이 남은 화면으로 폰을 계속 사용할 수 있기를 바랍니다. [비슷한 고장 사례와 기존 도구 비교](docs/WHY.md).

## 화면 범위 조정 전후

| 조정 전: 왼쪽 50% · 오른쪽 50% | 조정 후: 왼쪽 44% · 오른쪽 43.5% |
| --- | --- |
| [<img src="docs/media/range-before-fold3.jpg" width="340" alt="범위 조정 전: 가운데 손상 구간에 숫자 8과 9가 가려진 폴드3." />](docs/media/range-before-fold3.jpg) | [<img src="docs/media/range-after-fold3.jpg" width="340" alt="범위 조정 후: 1부터 16까지의 숫자가 양쪽 화면에 보이는 폴드3." />](docs/media/range-after-fold3.jpg) |

**갤럭시 Z 폴드3 · Android 15 실물 촬영.** [크롬·멀티윈도우 사용 전후 보기 →](docs/REAL_USE.md)

## 무엇을 할 수 있나요?

- **화면 이어보기:** 앱의 폭을 양쪽 정상 영역에 맞추고, 하나의 앱 화면을 가운데 손상 구간을 건너 이어 보여줍니다. 가로로 찌그러뜨리지 않으며, 전체 화면과 기존 분할 화면을 사용할 수 있습니다.
- **한쪽에서 전체 조작:** 터치가 되는 쪽을 왼쪽·오른쪽 중 선택합니다. 그쪽을 터치패드로 써서 반대쪽이나 전체 화면을 클릭·드래그·스크롤·확대할 수 있습니다.
- **한쪽 키보드:** 입력칸이 어느 쪽에 있어도 정상 터치 쪽에서 입력하고 복사·붙여넣기 등 기본 편집 도구를 사용합니다.

보이는 화면 범위와 조작바 크기·위치를 조절하고, 펼칠 때 폴드패치가 자동으로 켜지도록 설정할 수 있습니다.

<p>
  <a href="docs/media/settings-fold3.png"><img src="docs/media/settings-fold3.png" width="280" alt="폴드3 설정 화면. 정상 터치 쪽인 왼쪽에 조작 항목이 배치됩니다." /></a>
  <a href="docs/media/calibration-fold3.png"><img src="docs/media/calibration-fold3.png" width="280" alt="폴드3 화면 범위 맞추기. 왼쪽 슬라이더로 양쪽의 보이는 폭을 조절합니다." /></a>
  <a href="docs/media/keyboard-fold3.png"><img src="docs/media/keyboard-fold3.png" width="280" alt="폴드3의 왼쪽 키보드와 세로 조작바, 오른쪽 포인터. 배경은 입력 시험 화면입니다." /></a>
</p>

왼쪽부터 **설정 · 화면 범위 맞추기 · 키보드와 조작바**. 갤럭시 Z 폴드3 / Android 15 실기기 캡처이며, 키보드 배경은 입력 시험 화면입니다. 이미지를 누르면 원본을 볼 수 있습니다.

## 내 폰에서 쓸 수 있나요?

현재는 **세로로 펼친 내부 화면**을 대상으로 합니다. 양쪽에 보이는 영역이 남아 있고, 한쪽 이상은 터치가 되어야 합니다. 각 바깥 끝에서 전체 폭의 20~50%를 사용하도록 설정할 수 있습니다. 양쪽을 50%로 맞추면 화면 전체를 그대로 쓰면서 한쪽 터치패드로 조작할 수 있습니다.

| 환경 | 확인 상태 |
| --- | --- |
| 갤럭시 Z 폴드3 · Android 15 / One UI 7 | 실기기 확인 |
| Android 16·17 | 가상 기기만 확인, 실기기 미검증 |
| 다른 폴드 기기·Android 15 이상 | 기능 미검증 |

설치 최소 버전은 Android 15입니다. 기기별 차이와 아직 확인하지 못한 동작은 [호환성 안내](docs/COMPATIBILITY.md)를 확인하세요. 화면 읽기 기능과의 동시 사용은 지원하지 않습니다.

**[Shizuku](https://shizuku.rikka.app/download/) 설치·실행이 필요합니다.** 처음 준비할 때 신뢰하는 Wi-Fi와 무선 디버깅을 사용하며, 루팅이나 상시 PC 연결은 필요하지 않습니다. 재부팅하면 Shizuku를 다시 시작해야 할 수 있습니다.

## 시작하기

**설치용 APK를 제공합니다. 직접 빌드할 필요가 없습니다.** 현재 초기 시험판입니다.

1. **[foldpatch-release.apk](https://github.com/att083/FoldPatch/releases/download/v0.1.0-alpha.4/foldpatch-release.apk)**를 받아 설치합니다.
2. 앱을 열고 **사용 준비** 안내에 따라 조작할 쪽과 Shizuku·권한·키보드를 준비합니다.
3. 폰을 펼쳐 양쪽의 보이는 폭을 맞추고 저장한 뒤, 쓰던 앱으로 돌아갑니다.

초기 준비는 폰을 접어 바깥 화면에서 해도 됩니다. **[설치·조작·복구 안내 →](docs/INSTALL.md)**

**AI에게 폰 설치를 맡기려면** Codex·Claude Code 등에 [AI 설치 안내서](docs/AGENT_INSTALL.md)를 읽게 해 주세요. 아래 문장을 복사해 요청하면 됩니다.

> 이 저장소의 `docs/AGENT_INSTALL.md`를 읽고 내 폰에 폴드패치를 설치해 줘. 할 수 있는 단계는 직접 진행하고, 내가 폰에서 해야 할 때 알려 줘. 기존 앱 데이터와 설정은 유지해 줘.

앱은 OS 언어에 따라 한국어·영어·일본어·중국어 간체/번체로 표시됩니다. 키보드 입력은 한글·영문만 지원합니다. 인터넷 권한, 광고, 분석 SDK는 없습니다. [개인정보·권한 안내](PRIVACY.md).

## 함께 개선하기

설치 중 막힌 단계나 내 기기에서의 사용 결과를 알려주는 것도 도움이 됩니다. 번역·문서·코드 개선도 환영합니다. [기여 안내](CONTRIBUTING.md).

[빌드](docs/BUILD.md) · [기술 구조](docs/ARCHITECTURE.md) · [남은 과제](docs/BACKLOG.md)

[Apache License 2.0](LICENSE) · [외부 코드·라이브러리 고지](THIRD_PARTY_NOTICES.md)
