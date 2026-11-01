# 작업 기준선

기록 시각: 2026-10-26. 앱 코드는 수정하지 않았다. `git reset`, `git clean`, 강제 checkout, 커밋, 푸시는 하지 않았다.

이 파일은 조사 시작 직후의 `git status`를 남긴 것이다. 이 파일을 만든 뒤에는 `docs/` 안의 문서만 추가되거나 고쳐졌다.

## 프로젝트

| 항목 | 값 |
| --- | --- |
| 경로 | `/Users/eden/AndroidStudioProjects/yakyeon-frontend-rebuild` |
| applicationId | `com.example.yakyeon2` (`app/build.gradle.kts`) |
| namespace | `com.example.yakyeon2` |
| 브랜치 | `main` |
| 추적 | `origin/main`과 같음 (`## main...origin/main`) |
| HEAD | `b1f8743e0bcd636c42688c762de01871d33931b6` |
| HEAD 메시지 | `chore: rebuild Android frontend from scratch` |
| 작성 | deEdenKim, 2026-10-22 18:12:56 +0900 |
| 스테이징 | 없음 |

HEAD에 들어 있는 것은 재생성 커밋이다. 로그인 이후 화면, 테스트, `docs/`는 작업 트리에만 있다.

## 조사 시작 시점의 수정 파일

`git diff --stat`: 6 files changed, 954 insertions, 14 deletions.

- `app/src/main/java/com/example/yakyeon2/MainActivity.java`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`

## 조사 시작 시점의 untracked

`git status --short` 기준. 디렉터리는 그 안의 파일을 포함한다.

- `.idea/`
- `app/src/androidTest/java/com/example/yakyeon2/GraphWebRenderTest.java`
- `app/src/main/assets/`
- `app/src/main/java/com/example/yakyeon2/auth/`
- `app/src/main/java/com/example/yakyeon2/data/`
- `app/src/main/java/com/example/yakyeon2/main/`
- `app/src/main/java/com/example/yakyeon2/navigation/`
- `app/src/main/java/com/example/yakyeon2/ui/`
- `app/src/main/res/color/`
- `app/src/main/res/drawable-nodpi/`
- `app/src/main/res/drawable/` 아래 아이콘·배경 다수. 상태 출력에 개별 파일로 나열됨
- `app/src/main/res/layout/` 아래 `dialog_*`, `item_*`, `screen_*`, `view_*`
- `app/src/main/res/menu/`
- `app/src/main/res/raw/`
- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/test/java/com/example/yakyeon2/auth/`
- `app/src/test/java/com/example/yakyeon2/data/`
- `app/src/test/java/com/example/yakyeon2/navigation/`
- `docs/`

개별 drawable·layout 파일 이름은 같은 시점의 `git status --short` 출력과 같다. 여기 적지 않은 untracked 앱 파일은 없다.
