# 실제 사용 전후

[English](REAL_USE.en.md) · [README](../README.ko.md)

가운데 화면이 손상된 **갤럭시 Z 폴드3 / Android 15 / One UI 7**을 직접 촬영한 사진입니다. 사진은 전달받은 파일 그대로 실었습니다. 이미지를 누르면 원본 크기로 볼 수 있습니다.

## 전체 화면 크롬

| 폴드패치 적용 전 | 폴드패치 적용 후 |
| --- | --- |
| [<img src="media/chrome-before-fold3.jpg" width="380" alt="적용 전: 전체 화면 크롬의 글과 사진이 가운데 손상 구간에 가려집니다." />](media/chrome-before-fold3.jpg) | [<img src="media/chrome-after-fold3.jpg" width="380" alt="적용 후: 크롬의 페이지 폭과 줄바꿈이 달라지고 왼쪽에 폴드패치 조작바가 보입니다." />](media/chrome-after-fold3.jpg) |

같은 웹페이지가 양쪽에 남은 화면 폭에 맞춰 다시 배치됩니다. 글줄이 바뀌는 것은 페이지를 가로로 눌러 찌그러뜨리는 대신, 앱이 사용할 폭을 줄였기 때문입니다. 검은 부분은 실제 패널 손상이라 계속 남아 있습니다.

## 왼쪽 YouTube · 오른쪽 크롬

| 폴드패치 적용 전 | 폴드패치 적용 후 |
| --- | --- |
| [<img src="media/split-before-fold3.jpg" width="380" alt="적용 전: 왼쪽에는 YouTube 영상, 오른쪽에는 크롬이 있으며 가운데 손상이 각 화면의 안쪽을 가립니다." />](media/split-before-fold3.jpg) | [<img src="media/split-after-fold3.jpg" width="380" alt="적용 후: YouTube와 크롬의 좌우 배치를 유지하고 왼쪽 위에 최소화된 폴드패치 버튼이 보입니다." />](media/split-after-fold3.jpg) |

기존의 좌우 앱 배치를 유지하면서 사용할 폭을 조정합니다. 전후 사진은 서로 다른 순간에 찍어 영상 장면과 브라우저 도구모음 상태가 다릅니다.

## 화면 범위 미리보기

| 범위 조정 전: 왼쪽 50% · 오른쪽 50% | 범위 조정 후: 왼쪽 44% · 오른쪽 43.5% |
| --- | --- |
| [<img src="media/range-before-fold3.jpg" width="380" alt="양쪽 폭을 50%로 둔 미리보기에서는 가운데 숫자 8과 9가 손상 구간에 가려집니다." />](media/range-before-fold3.jpg) | [<img src="media/range-after-fold3.jpg" width="380" alt="왼쪽 44%, 오른쪽 43.5%로 줄인 미리보기에서는 1부터 16까지의 숫자가 양쪽에 보입니다." />](media/range-after-fold3.jpg) |

두 사진 모두 폴드패치의 범위 미리보기 화면입니다. 50%·50%는 가운데를 비우지 않는 설정입니다. 폭을 줄인 사진에서는 가운데에 가려졌던 8·9번을 양쪽에서 확인할 수 있습니다. 이 값은 촬영한 기기에 맞춘 예시이며, 자신의 화면을 보면서 조정해야 합니다.

사진 속 웹페이지와 영상은 기존 앱의 실행 예시입니다. 이 사진은 화면 배치를 보여주며, 터치 조작·복구 동작과 기기별 확인 범위는 [검증 기록](RELEASE_REVIEW.md)에 정리했습니다.
