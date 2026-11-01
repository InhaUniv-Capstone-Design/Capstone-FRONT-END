# 약연 전체 정합성 진단

조사일: 2026-10-26. 이 문서는 현황 기록이다. 앱 코드, 테스트, Git 상태는 바꾸지 않았다.

## 조사 범위와 한계

확인한 자료:

- `docs/figma-frame-inventory.md`: 페이지 `최종 정리안(레이어 수정 ver)`, fileKey `AHjb9NVHC4bXliviMy58bn`, 페이지 노드 `2280:2302`. 최상위 프레임 47개.
- `docs/business-rules.md` 0절. 2026-10-22와 2026-10-25 사용자 답변. 이 절이 그 아래의 예전 충돌 문장보다 우선한다고 적혀 있다.
- `docs/screen-specifications.md`, `docs/figma-vs-spec-differences.md`, `docs/open-questions.md`, `docs/requirements-from-user.md`, `docs/common-design-rules.md`
- Android 소스: `app/src/main/java/com/example/yakyeon2`, `app/src/main/res/layout`, `app/src/test`, `app/src/androidTest`
- Git: 브랜치 `main`, `origin/main` 추적, HEAD `b1f8743` (`chore: rebuild Android frontend from scratch`)

2026-10-26 재확인에서 Figma를 직접 열었다. 상세는 아래 「Figma 직접 확인」이다. 그 전에 적힌 “Figma를 다시 열지 않았다”는 문장은 그 시점의 기록이고, 지금은 유효하지 않다.

아직 하지 않은 것:

- 단위 테스트와 계측 테스트는 실행하지 않았다. 목록은 `test-inventory.md`의 소스 `@Test`다.
- 에뮬레이터를 다시 실행하지 않았다. 구현 여부는 코드를 읽은 결과다.
- 회의록 원문은 저장소에 없다. 결정으로 쓰는 문장은 `business-rules.md` 0절뿐이다.
- 프레임 47개의 채우기 색을 하나씩 뽑지는 않았다. 홈 변수와 약통 디자인 컨텍스트만 색을 읽었다.

프레임 수와 화면 수를 구분한다. 최상위 프레임 47개 중 화면 이름 29개, 모달 이름 15개, 문서 1개, 참고 1개, 컨트롤 1개다. 화면이 아닌 주석·도형·이미지는 프레임이 아니다.

## Git

커밋된 HEAD는 프로젝트 재생성 커밋이다. 로그인, 약 등록, 홈, 그래프, 약통, 설정 목록, 테스트, `docs/`는 작업 트리에 있고 커밋되지 않았다. `git status` 기준 수정 6파일, 나머지 구현과 문서는 untracked다. 이 진단 문서 5개도 그 untracked `docs/`에 추가된다.

원격 푸시, 리셋, 커밋은 하지 않았다.

## 데이터가 있는 곳

| 저장 | 위치 | 담는 것 |
| --- | --- | --- |
| 메모리 | `MedicationStore`, `YakyeonSession` | 실행 중 약봉투, 복용 기록, 가입 초안, 약통 연결 플래그, 개발용 그래프 관계 플래그 |
| 로컬 | SharedPreferences 파일 `yakyeon_medications` | `LocalMedicationState`가 `exportState()`를 저장. 샘플·fixture id는 export에서 빠진다 |
| 로컬 | 약 이름 카탈로그 | `LocalMedicineNames` |
| 서버 | 없음 | 로그인, 중복 확인, 문자 인증, DUR, 기기 통신 호출이 없다 |

로그인 성공 경로는 없다. `AuthValidation.login`은 빈 값 검사 뒤 `NOT_CONNECTED`다. 디버그 빌드의 `개발용 홈 보기`만 `DevHomeGate`로 탭에 들어간다. 릴리스에서는 그 버튼으로 홈에 들어가지 않는다 (`DevHomeGateTest`).

설정 상세는 `MainActivity.render`가 약 등록·일정·내역이 아니면 `MainScreens.pending`을 연다. 문구는 `main_pending_message`: 상세는 다음 묶음이고 서버 데이터는 없다.

## 영역별 요약

분류 정의는 요청과 같다. 한 화면에 차이가 여러 개면 주된 분류를 적고, 나머지는 비고에 둔다. 프레임별 표는 `screen-implementation-matrix.md`다.

### 가입·로그인

화면 골격은 있다. `AuthScreens`가 `screen_login`, `screen_signup_type`, `screen_signup_form`, `dialog_signup_consent`, `dialog_caregiver_notice`를 연다.

0절과 맞는 부분: 유형은 `다음`으로 이동, 미선택 시 짧은 안내, 대리인은 QR 대기 없이 안내 후 입력, 아이디·비밀번호 형식 검사, 필수 누락은 항목을 짚는 짧은 문구, 동의 체크가 모두 있어야 진행.

없는 부분: 로그인 성공, 회원 저장, 문자 인증, 아이디 중복의 실제 조회, 가입 성공 팝업, 생년월일 달력. 실패 문구 `회원정보가 일치하지 않습니다`는 로그인 성공 자체가 없어 그 팝업에 도달하지 않는다. 분류는 서버 의존과 누락이 함께 있다.

### 홈

`MainScreens.home`, `screen_home.xml`. 복용 칸은 아침·점심·저녁·취침 네 개다. 0절이 네 칸을 정한다. Figma `2280:4170`은 아침·점심·저녁 예시만 보인다. 이건 Figma와 달라도 0절을 따른 구현으로 본다.

개수와 병용금기 건수는 `MedicationStore`에서 계산한다. 홈 복용 개수는 샘플 약봉투를 포함한다. 그래프 노드는 샘플을 빼는 별도 규칙이다. 인사 이름은 Figma의 `김약연`을 쓰지 않는다.

디버그에서만 개발용 패널이 있다. 목록은 `mock-data-scenarios.md`.

### 약 등록·일정·내역

구현된 화면: 목록 `screen_med_list`, 동의 대화상자 `screen_med_consent`, 약봉투 `screen_med_envelope`, 수기 `screen_med_form`, 칸 선택 `screen_med_slot`, 알람 `screen_med_alarm`, 일정 `screen_schedule`, 내역 `screen_history`, 약 이름 검색 `dialog_medicine_name`.

로컬 등록, 칸 배정, 4시간 간격, 취침 예외, 3시간 안 누락→지연, 내역 필터(전체·완료·지연·누락), 기본 2주 범위는 코드와 단위 테스트에 있다. 촬영·OCR·서버 저장은 없다. 카메라 타일은 있고 인식 결과는 만들지 않는다.

저장된 알람 수정 `updateAlarmMinutes`는 수동 기록이 아닌 예정 회차 기록을 지운 뒤 다시 만든다. `applySavedEdit`도 수동 기록이 없는 그 약의 기록을 지운 뒤 다시 펼친다. 예정만 지우고 이미 지난 미기록 회차를 유지하는 방안은 코드에 없다. 회의 문장으로 그 방안이 확정됐다는 기록은 `business-rules.md`에 없다. 분류는 동작 차이와 회의 결정 확인 필요가 겹친다.

### 그래프

`MainScreens.graph`, `DrugGraphView`, assets의 Cytoscape. 복용 중이며 샘플이 아닌 약만 노드다. 확인된 DUR 쌍은 비어 있다. 개발용 플래그가 켜지고 노드가 둘 이상일 때만 첫 두 노드를 잇고 `개발용`으로 표시한다. Figma 예시 쌍 약1+약7, 약2+약6은 데이터로 쓰지 않는다. 상세 팝업은 그 개발용 쌍에만 뜬다. 출처·기준일·진단 아님 문장은 넣지 않는다. 서버 DUR이 없다.

### 약통

`MainScreens.pillbox`, `screen_pillbox.xml`. 칸 3개, 실제 슬롯 인덱스, 빈 칸과 사용 칸 구분, `약통 n칸 사용 중`. 연결 표시는 `pillboxConnected()` 플래그다. Wi-Fi 검색이 아니다. 기기 찾기 `2280:2993`, 연결 동의 `2280:5315`, 연결 전 팝업 `2280:3492`, 설정 상세 `2280:3420`, 불안정 배지 `2280:4064`는 없다. `약통 설정`은 아직 연결되지 않았다는 안내만 연다. `정상`, `확인 필요`, `약통이 열렸습니다.`는 없다. 0절이 Bluetooth 문구를 덮는다.

등록 흐름의 `약통에 넣기`는 유지된다. 연결 플래그가 없으면 `연결된 약통이 없어요`, 세 칸이 차면 `사용가능한 약통이 없어요` 쪽으로 간다. 이 문구는 약통 탭의 슬롯 탭 문구와 다르다.

### 설정

목록 `screen_settings`만 있다. 대리인 계정일 때만 연동 행이 추가된다. 프로필, 연동, 알림, 공유, 법적 안내를 누르면 `screen_pending`이다. Figma의 해당 하위 화면과 모달은 누락이다. 행을 뺀 것은 아니다.

### 하단 탭

순서 홈·그래프·약통·설정. 아이콘은 집, 차트, 원 안 X, 사람. 선택색 `#12735E`, 비선택 `#8C9491`. 높이는 `wrap_content`와 최소 68dp다. 이전 에뮬레이터에서 179px(68dp)로 측정됐다. 그 측정은 이번 조사에서 다시 하지 않았다. Figma 그래프 라벨만 Bold인 점은 코드가 네 라벨을 같은 10sp medium으로 둔다.

## 보존할 구현

Figma와 다르지만 0절 또는 코드 주석상 유지 후보인 것:

- 홈 네 칸, 내역 필터의 `완료`, Wi-Fi만 언급하고 Bluetooth UI를 만들지 않은 것, 그래프에 가짜 DUR을 넣지 않은 것, 약통 하드웨어 상태를 완료처럼 그리지 않은 것, 개발용 예시와 그래프 검증을 나눈 것, 헤더 볼드.

삭제하거나 이름으로 합치지 않는다. 개발용 버튼 10개는 전부 유지 대상이다. 불필요 여부는 판단하지 않았다.

## 가장 큰 차이 10개

1. 로그인·가입이 서버 없이 끝나지 않는다. 디버그 홈 보기만 탭에 들어간다.
2. 설정 다섯 갈래(프로필, 연동, 알림, 공유, 법적 안내)가 안내 문구 화면이다.
3. 약통 기기 찾기, 연결 동의, 연결 전 팝업, 설정 상세, 불안정 배지가 없다. 약통 탭은 로컬 슬롯 카드다.
4. 그래프 엣지는 식약처 DUR이 아니다. 개발용 한 쌍만 있다.
5. 촬영은 약봉투를 인식하지 않는다. 약은 이름 검색과 수기다.
6. 문자 인증, 아이디 중복, 회원 저장이 없다.
7. 홈은 Figma의 3칸 예시가 아니라 0절의 4칸이다. 샘플 환자명·예시 병용 쌍을 쓰지 않는다.
8. 의료진 공유 화면이 없다. 0절은 PNG, Figma 버튼은 PDF다. 0절을 우선하면 PDF 버튼은 따르지 않는다.
9. 홈은 `screen_home.xml`의 comment 아이콘으로 내역에 간다. 그래프·약통·설정은 공유 헤더의 글자 `내역`이다. Figma 약통 `2280:3972`는 comment-alt다. 0절도 comment-alt다.
10. 저장된 약의 알람·내용 수정이 수동 기록이 없는 회차 기록을 지우고 다시 만든다. 유지 규칙은 문서에 확정 문장으로 없다.

## 먼저 손댈 후보와 이유

코드를 고치지 않았다. 순서는 위험과 확인 필요를 나눈 제안이다.

1. 문서 드리프트 정리. `open-questions.md`는 0절이 이미 답한 질문도 질문으로 남겨 둔다. 구현 전에 어떤 문장이 우선인지 팀이 같은 표를 보게 하는 일이 코드 변경보다 안전하다.
2. 설정 상세는 플레이스홀더라 기존 등록 흐름을 깨지 않고 화면을 더할 수 있다. 다만 알림 스위치와 공유 형식은 0절과 Figma가 겹치므로, 화면을 그리기 전에 `decision-log.md`의 확인 질문을 닫는 편이 안전하다.
3. 약통 하드웨어 화면은 0절의 순서(검색 → 동의 → 약통)가 있다. UI만 만들 수 있으나 연결됐다고 표시하면 안 된다. 가짜 `TRX-0328`은 넣지 않는 쪽이 현재 구현 원칙과 같다.
4. 그래프 DUR, 로그인, OCR, 문자는 서버 계약이 생기기 전에는 완료로 보이지 않게 두는 편이 맞다.
5. 개발용 버튼과 기존 테스트는 삭제 후보가 아니다.

## Figma 직접 확인

연 파일: `AHjb9NVHC4bXliviMy58bn`. 도구는 Figma MCP `get_metadata`, 홈 `get_variable_defs`, 약통 `get_design_context`. 세션 `2159fb73-712f-4144-a82f-08620bec1e25`. 계정 핸들 EDEN.

페이지 세 개 중 최신은 `2280:2302` `최종 정리안(레이어 수정 ver)`뿐이다. `0:1` Page 1과 `2220:2` Page 2는 읽지 않았고 기준으로 쓰지 않는다.

페이지 직속 자식 57개. 인벤토리의 화면 29, 모달 15, 문서·참고·컨트롤 각 1은 ID까지 페이지에 있다. 인벤토리에 없던 장식·이미지가 3개 더 있다. 화면이 아니다. ID 대조와 문구는 `screen-implementation-matrix.md`.

이전에 진단 표에 적었던 로그인 `2280:2303`, 홈 `2280:2762`, 약봉투 `2280:4386` 같은 번호는 이 페이지에 없다. 그 표는 잘못된 ID였고, 행렬 문서를 이번 조회로 갈아 끼웠다. 구현 여부 판단은 코드 파일을 본 뒤에 남겼다.

약통 프레임에서 확인한 값: 바깥 패딩 18, 내용 위 16 아래 24, 카드 패딩 20, 모서리 14, 그림자 y 4 blur 16, 탭 높이 68, 탭 위 여백 8, 아이콘 28, 라벨 10. 제목은 Regular 20이다. 요구사항의 헤더 볼드가 이 Regular를 덮는다. 3칸은 `확인 필요`와 `약통이 열렸습니다.`다. 코드는 그 상태를 그리지 않는다.

그래프 `2280:4093` 목록은 `약1 + 약7`, `약2 + 약6`이다. 코드의 등재 데이터가 아니다.

## 다음 작업 순서

앱 코드는 이 단계에서 바꾸지 않는다.

1. 기준은 이 문서, `work-baseline.md`, `screen-implementation-matrix.md`, `decision-log.md`의 0절 닫힌 항목이다. Page 1·Page 2와 폐기된 프레임 번호는 쓰지 않는다.
2. 첫 코드 변경은 이미 결정된 헤더다. 그래프·약통·설정의 글자 `내역`을 comment 아이콘으로 바꾼다. 이동 대상은 내역 그대로다. 홈 아이콘과 개발용 버튼은 건드리지 않는다.
3. 그 다음 누락 화면은 설정 상세다. 알림은 0절의 스위치와 `알림 설정 · 02`를 같이 두고, 공유 버튼은 PNG다. PDF 문구와 `전체` 초기값을 따르지 않는다.
4. 약통 기기 UI는 검색, 동의, 약통 순서로 빈 상태에 가깝게 둔다. `TRX-0328`, 동기화 시각, `연결됨`을 검색 결과 없이 채우지 않는다. `정상`과 열림 문구도 기기 이벤트가 생기기 전에는 그리지 않는다.
5. 로그인 성공, 문자, 중복 조회, OCR, DUR 쌍은 서버 계약 전이다.
6. `applySavedEdit`가 수동 기록이 없는 기록을 지우는 동작은 0절 문장만으로 닫히지 않았다. 그 코드를 먼저 고치지 않는다.

위험:

- 미커밋 트리가 앱 대부분이다. reset이나 clean은 구현을 지운다.
- Figma 예시 문장(김약연, 아스피린+와파린, TRX-0328, PDF, 블루투스)을 그대로 넣으면 0절과 충돌한다.
- 하단 탭 글자 굵기만 바꿔도 이전에 라벨 높이가 0이 된 적이 있다. 그래프 Bold는 그 회귀를 확인한 뒤에만 바꾼다.
- 테스트는 아직 실행하지 않았다. 삭제하지 않는다.

## 근거 파일

- 네비게이션: `MainActivity.java`, `AppNavigator.java`, `MedScreen.java`, `RegistrationChrome.java`, `DevHomeGate.java`
- 화면: `AuthScreens.java`, `MainScreens.java`, `MedScreens.java`, `RecordScreens.java`
- 데이터: `MedicationStore.java`, `LocalMedicationState.java`, `DrugRelations.java`, `DevFixtures.java`
- 레이아웃: `app/src/main/res/layout/screen_*.xml`, `dialog_*.xml`, `screen_pending.xml`
