# Contributing to FoldPatch

You can help without writing code: report confusing setup instructions or explain what worked on your phone. Replies and fixes may take time; support for every device is not promised.

## Report a problem

Include your device model, Android/One UI version, FoldPatch version, working touch side, target mode (Left/Right/All), steps, expected result, and actual result. Screenshots or logs are optional. Remove personal information first; never include pairing codes, passwords, or unreviewed full system logs.

## Suggest a change

Documentation corrections, translations, and focused code improvements are welcome. For a large change, describe the idea first so its scope can be discussed. Controls must remain reachable on the working touch side, and existing apps should remain in their normal workspace.

Follow the [build guide](docs/BUILD.en.md). Run the checks relevant to your change; code changes should pass `bash scripts/test.sh` and the relevant builds. Changes to display layers, input, or recovery need on-device checks. State what you could not test in the pull request. Emulator success does not establish Samsung hardware support.

For display/input changes, check stop, fold, lock, lost connection, and preview cancellation as applicable. Do not change unrelated phone settings or other people's work. Keep signing keys, passwords, and personal captures out of commits. Record sources and licenses for reused code in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Contributions are submitted under this repository's Apache License 2.0.

---

# 한국어 기여 안내

개발 경험 없이 설치 과정이나 실제 사용 결과를 알려주는 것도 환영합니다. 답변과 수정에 시간이 걸릴 수 있으며 모든 기기 지원을 약속하지 않습니다.

실제로 손상된 화면을 쓰는 사람의 조작 가능성을 우선합니다. 화면 반대편에만 확인·취소 버튼을 두거나, 앱을 별도의 작업 공간으로 옮기는 변경은 현재 방향에 맞지 않습니다.

## 버그 보고

기기 모델, Android·One UI 버전, 재현 단계, 실제 터치가 되는 쪽, 왼쪽/오른쪽/전체 조작 대상, 기대한 결과와 실제 결과를 적어 주세요. 화면 녹화·로그는 선택 사항이며 개인정보를 먼저 지워 주세요. 개발자 옵션의 페어링 코드나 개인 연락처는 필요하지 않습니다.

## 개발 확인

[빌드 안내](docs/BUILD.md)에 따라 `bash scripts/test.sh`와 debug/release 빌드를 확인하세요. 화면층·입력·복구 변경은 실제 기기 확인이 필요합니다. 단위 테스트나 에뮬레이터만으로 삼성 기기 지원을 선언하지 마세요.

폴드패치를 끈 상태, 접힌 화면, 잠금, 연결 끊김, 미리보기 취소에서 정상 화면과 입력으로 돌아오는지 확인해 주세요. 확인하지 않은 항목은 PR에 명시합니다. 관련 없는 사용자 설정이나 다른 앱의 작업을 변경하지 않습니다.

공개 배포용 키, APK 서명 암호, 개인 로그·스크린샷은 커밋하지 않습니다. 다른 프로젝트에서 가져온 코드는 출처·라이선스·변경 사항을 THIRD_PARTY_NOTICES.md에 기록합니다.

기여 코드는 이 저장소의 Apache License 2.0에 따라 제출합니다.
