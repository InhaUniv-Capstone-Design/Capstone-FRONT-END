# 그래프·약통 탭 API 실연동 상태

조사일: 2026-10-30

이 문서는 그래프 탭(병용금기)과 약통 탭만 다룬다. 로그인, 회원가입, 홈, 복약 알림은 수정하지 않았다.

실제 서버와 HTTP 통신은 하지 못했다. `http://127.0.0.1:8080/terms` 연결이 즉시 거부되었다. 아래 표의 판정에 PASS는 없다.

## 1. 조사 개요

### 조사 대상

- 그래프 탭: 병용금기 요약, 그래프, 선 상세, 목록
- 약통 탭: 동의, 등록, 칸 배정, 충전, 본문 조회, 설정, 동기화, 등록 해제, 추가 충전, 배정 해제

### 사용한 백엔드 명세

약연 서버 API 명세 v0.9 (기준일 2026-10-29, Base URL `http://localhost:8080`).

- 2-4 병용금기
- 2-6 약통
- 4-11 약통 상태. `attention`은 명세에서 예정(🔜)이다. 응답에 `OPENED`가 있을 때만 "약통이 열렸습니다."를 표시하고, 그 외 값은 안내로 만들지 않는다.
- 4-12 `DEVICE` 동의

### 조사한 프론트엔드 파일

- `app/src/main/java/com/example/yakyeon2/api/ApiClient.java`
- `app/src/main/java/com/example/yakyeon2/api/ApiConfig.java`
- `app/src/main/java/com/example/yakyeon2/api/SettingsApi.java`
- `app/src/main/java/com/example/yakyeon2/api/SettingsContracts.java`
- `app/src/main/java/com/example/yakyeon2/main/MainScreens.java`
- `app/src/main/java/com/example/yakyeon2/main/SettingsScreens.java`
- `app/src/main/java/com/example/yakyeon2/MainActivity.java`
- `app/src/main/java/com/example/yakyeon2/data/GraphDocument.java`
- `app/src/main/assets/graph.html`
- `app/src/main/res/layout/screen_graph.xml`
- `app/src/main/res/layout/screen_pillbox.xml`
- `app/src/main/res/layout/screen_pillbox_search.xml`

### 서버 연결 환경

- Base URL은 `app/build.gradle.kts`의 `yakyeon.apiBaseUrl`이며 기본값은 `http://localhost:8080`이다. 앱은 `BuildConfig.API_BASE_URL`을 사용한다.
- 이번 환경에서 `http://127.0.0.1:8080`은 연결이 거부되었다. HTTP 상태 코드는 없다.
- 에뮬레이터용 `10.0.2.2`와 `adb reverse`는 서버 프로세스가 없어 확인하지 않았다.
- 테스트 계정, 환자 ID, 토큰은 만들지 않았다.

## 2. 그래프 탭 API 검증 결과

| API | 명세 일치 | 호출 코드 | 실제 HTTP 통신 | 응답 매핑 | 수정 여부 | 결과 |
|---|---|---|---|---|---|---|
| `GET /patients/{patient_id}/interactions/summary` | 일치. `total`, `flagged`, `unchecked` | 로그인 후 그래프 탭에서 호출 | 하지 못함 | 로컬 파서 테스트 통과 | 추가 | LOCAL PASS |
| `GET /patients/{patient_id}/interaction-graph` | 일치. `dur_seq`는 문자열. `groups`의 null 원소는 잘못된 응답 | 로그인 후 그래프 탭에서 호출 | 하지 못함 | 로컬 파서 테스트 통과 | 추가 | LOCAL PASS |
| `GET /interaction-edges/{dur_seq}` | 일치. 경로 값은 숫자로 바꾸지 않음 | 선 또는 목록을 누를 때 호출 | 하지 못함 | 로컬 파서 테스트 통과. 면책 문구를 대화상자에 넣음 | 추가 | LOCAL PASS |
| `GET /patients/{patient_id}/interactions` | 일치 | 로그인 후 그래프 탭에서 호출 | 하지 못함 | 로컬 파서 테스트 통과 | 추가 | LOCAL PASS |

로그인하지 않은 그래프 탭은 기존 로컬 약물 그래프를 유지한다. 그 경로는 서버 조회가 아니다.

화면 매핑(코드 기준, 실기기 미확인):

- 요약의 세 값을 화면에 표시한다.
- `unchecked > 0`이면 "확인하지 못한 약이 있어요"를 표시한다.
- `flagged = 0`이면 등재가 없다는 뜻이 안전하다는 뜻이 아니라는 문장을 함께 표시한다.
- `checked = false`인 노드는 점선으로 구분하고, 선택 시 검사를 마치지 못했다고 알린다.
- 등급, 위험 점수, 위험도 색은 만들지 않는다.
- `groups`는 `medication_id` 배열로만 읽고, 위험 묶음 색으로 그리지 않는다.
- 상세에는 `reason_text`, `source_name`, `source_base_date`, `precaution`, `disclaimer`를 넣는다. `disclaimer`가 없으면 응답 오류로 본다.

## 3. 약통 탭 API 검증 결과

| API | 명세 일치 | 호출 코드 | 실제 HTTP 통신 | 응답 매핑 | 수정 여부 | 결과 |
|---|---|---|---|---|---|---|
| `GET /users/me/agreements` | `terms_type = DEVICE`, `agreed` | 로그인한 동의 화면에서 호출 | 하지 못함 | 기존 동의 파서 사용 | 약통 동의 경로에 연결 | LOCAL PASS |
| `POST /users/me/agreements` | `terms_version_id`는 응답 값, `agreed: true` | 동의가 없을 때만 호출 | 하지 못함 | 기존 요청 본문 사용. 버전 9를 하드코딩하지 않음 | 약통 동의 경로에 연결 | LOCAL PASS |
| `POST /patients/{patient_id}/device` | `serial_no`, `secret`, 선택 `max_compartments` | 메서드만 있음. 화면은 호출하지 않음 | 하지 못함 | 본문 범위 테스트만 통과 | 호출 메서드 추가, 화면 미연결 | NOT IMPLEMENTED |
| `POST /prescriptions/{prescription_id}/compartment` | `compartment_no` 1–3 | 메서드만 있음 | 하지 못함 | 범위 밖은 요청을 만들지 않음 | 호출 메서드 추가, 화면 미연결 | NOT IMPLEMENTED |
| `POST /compartment-mappings/{mapping_id}/load` | `loaded_count` 1–100 | 메서드만 있음 | 하지 못함 | 범위 테스트만 통과 | 호출 메서드 추가, 화면 미연결 | NOT IMPLEMENTED |
| `GET /patients/{patient_id}/pillbox` | 명세의 device, compartments 필드 | 로그인 후 약통 탭에서 호출 | 하지 못함 | 로컬 파서 테스트 통과 | 추가 | LOCAL PASS |
| `GET /patients/{patient_id}/device` | 기존 설정 조회 | 약통 설정 화면에서 호출 | 하지 못함 | 기존 파서. 공유 거부와 미등록을 구분 | 권한·404 분기 수정 | LOCAL PASS |
| `PATCH /patients/{patient_id}/device/settings` | 바꾼 항목만 전송 | 설정 스위치에서 호출 | 하지 못함 | 응답으로 스위치를 다시 그림 | 읽기 전용이면 스위치를 숨김 | LOCAL PASS |
| `POST /patients/{patient_id}/device/sync` | 본문 없음. `202`의 `schedule_version` | 설정 화면에서 호출 | 하지 못함 | 버전을 "동기화를 요청했습니다"로 표시 | 읽기 전용이면 숨김 | LOCAL PASS |
| `DELETE /patients/{patient_id}/device` | `204` | 설정 화면에서 호출 | 하지 못함 | 성공 후 약통 탭으로 돌아가 다시 조회 | 읽기 전용이면 숨김 | LOCAL PASS |
| `POST /compartment-mappings/{mapping_id}/refill` | `added_count` 1–100 | 메서드만 있음 | 하지 못함 | 범위 테스트만 통과 | 호출 메서드 추가, 화면 미연결 | NOT IMPLEMENTED |
| `DELETE /compartment-mappings/{mapping_id}` | `204` | 메서드만 있음 | 하지 못함 | 화면 갱신 없음 | 호출 메서드 추가, 화면 미연결 | NOT IMPLEMENTED |

약통 본문 매핑(코드 기준, 실기기 미확인):

- 칸은 `compartment_no` 오름차순이다. 응답에 없는 칸을 `EMPTY`로 만들지 않는다.
- `EMPTY`, `PENDING_LOAD`, `ACTIVE`를 각각 빈 칸, 충전 전, 충전 완료로 표시한다.
- `loaded_count`, `remaining_count`가 null이면 0으로 보여 주지 않는다.
- `low_stock = true`이면 약봉지 부족을 표시한다.
- `pouch_present = null`은 약봉지가 없다는 뜻이 아니다. `false`일 때만 미감지 문구를 보여 준다.
- `alert_active = null`은 알림이 꺼졌다는 뜻이 아니다. `true`일 때만 알림 문구를 보여 준다.
- `attention = OPENED`이면 기존 문구 "약통이 열렸습니다."를 표시한다. null이면 그 안내를 숨긴다.
- 개수는 약봉지 수이다. 서버 약 목록에는 하루 복용 횟수가 없으므로 그 숫자를 만들지 않는다.

약통 등록은 QR의 시리얼과 비밀값이 필요하다. 검색 화면의 예시 기기 `TRX-0328`로는 `POST /device`를 호출하지 않는다. 로그인한 검색 화면에서는 그 행을 숨긴다. 로그인하지 않은 기존 데모 흐름은 그대로 둔다.

## 4. 권한 및 예외 처리 결과

| 상황 | 백엔드 응답 | 기존 처리 | 수정 내용 | 검증 결과 |
|---|---|---|---|---|
| 보호자, `DUR_INFO` 없음 | `403 SHARE_NOT_GRANTED` | 그래프가 서버를 호출하지 않음 | `shared_items`에 항목이 있는데 키가 없으면 호출하지 않음. 필드가 없으면 호출하고, `403`이면 "공유되지 않음" | 코드 분기만 확인. HTTP 미검증 |
| 보호자, `DEVICE_EVENT` 없음 | `403 SHARE_NOT_GRANTED` | 약통 본문을 호출하지 않음 | 그래프와 같은 규칙. 약통 탭과 설정 화면에 "공유되지 않음" | 코드 분기만 확인. HTTP 미검증 |
| 복약자 본인 | 공유 항목 검사 없음 | 해당 없음 | `my_role = SELF`에는 `DUR_INFO`, `DEVICE_EVENT` 사전 차단을 하지 않음 | 로컬 규칙 테스트 통과 |
| 보호자 `READ_ONLY` | 조회는 허용, 쓰기는 `403 INSUFFICIENT_PERMISSION` | 설정 스위치를 막지 않음 | 조회는 호출. 등록 동의, 스위치, 동기화, 해제를 숨기거나 막음 | 로컬 규칙 테스트 통과. HTTP 미검증 |
| 보호자 `READ_WRITE` | 쓰기 허용 | 구분 없음 | `permission_scope = READ_WRITE`만 쓰기 허용 | 로컬 규칙 테스트 통과 |
| `401 UNAUTHENTICATED` | 토큰 재발급 후 한 번 재시도 | `ApiClient`에 이미 있음 | 그래프·약통에서 토큰을 하드코딩하지 않음 | 기존 클라이언트 유지. 이번 HTTP 미검증 |
| `404 DEVICE_NOT_FOUND` | 등록된 약통 없음 | 설정에서 일반 실패 대화상자 | 약통 탭은 등록 전 화면, 설정은 미등록 안내 | 코드 분기만 확인 |
| `409` 약통·배정 코드 | 명세의 code | 문구 없음 | `code`별 문구를 추가 | 문구 테스트 통과. 해당 쓰기 화면은 없음 |
| JSON이 아닌 5xx | 서버 오류 | `ApiClient`가 예외로 변환 | 그래프·약통은 그 예외를 서버 오류 문구로 표시 | 기존 파서 동작. 이번 HTTP 미검증 |
| 환자 0명 또는 여러 명 | 앱이 한 명을 고르지 않음 | 설정에 이미 있음 | 그래프·약통도 같은 안내를 보여주고 데이터 API는 호출하지 않음 | 기존 환자 결정 규칙 재사용 |

`shared_items`가 응답에 없으면 서버가 아직 공유 항목을 강제하지 않는 것으로 보고 조회를 호출한다. 있는 목록에서 키가 빠졌을 때만 미리 "공유되지 않음"으로 둔다.

## 5. 발견된 문제점

### 그래프 탭이 서버를 호출하지 않음

- 위치: `MainScreens.graph`
- 원인: 저장된 로컬 약과 개발용 관계만 그렸다.
- 영향: 요약, 미검사 약, 등재 상세, 면책 문구가 화면에 없었다. `flagged = 0`을 안전으로 읽을 근거도 없었다.
- 수정: 로그인한 그래프 탭은 요약, 그래프, 목록을 호출한다. 실패하면 이전 그래프를 남기지 않는다.

### 약통 탭이 로컬 연결 플래그만 사용함

- 위치: `MainScreens.pillbox`, `MedicationStore.pillboxConnected`
- 원인: 칸과 "약통이 열렸습니다"가 서버 `pillbox` 응답이 아니었다.
- 영향: 충전 전, 약봉지 수, 부족, 미보고 센서 값을 구분할 수 없었다.
- 수정: 로그인한 약통 탭은 `GET /pillbox` 결과를 칸에 표시한다. 로그인하지 않은 데모는 그대로 둔다.

### 설정 화면이 로컬 연결 플래그에 막힘

- 위치: `SettingsScreens.pillbox`, `MainActivity.render`
- 원인: 서버 장치 조회가 되어도 로컬 플래그가 꺼져 있으면 설정 본문 대신 미연결 화면으로 돌아갔다.
- 영향: 등록된 약통의 설정이 보이지 않을 수 있었다.
- 수정: 로그인 상태에서는 서버 장치 조회로 설정 화면을 연다. `READ_ONLY`는 변경 버튼을 숨긴다.

### 동의 후 로컬 연결로 등록을 대신함

- 위치: `SettingsScreens.pillboxConsent`
- 원인: 동의 체크만으로 `connectPillbox()`를 호출했다. `DEVICE` 동의 API와 시리얼·비밀값이 없었다.
- 영향: 약통이 등록된 것처럼 보일 수 있었다.
- 수정: 로그인 상태에서는 동의 API만 호출하고, QR 시리얼과 비밀값이 필요하다는 안내를 보여 준다. 예시 시리얼로 `POST /device`를 호출하지 않는다.

### 쓰기 API 화면이 없음

- 위치: 약통 등록, 칸 배정, 최초 충전, 추가 충전, 배정 해제
- 원인: QR 스캐너와 약봉지 개수 입력이 없다. 처방 ID와 배정 ID도 이 탭 화면에 없다.
- 영향: 해당 작업은 앱에서 아직 할 수 없다.
- 수정: 요청 본문과 범위 검사만 추가했다. 없는 식별자나 개수를 넣어 호출하지는 않았다.

## 6. 실제 서버 연동 결과

실제 서버 통신을 하지 못했다.

- 서버 연결 여부: 실패. `http://127.0.0.1:8080` 연결 거부.
- Base URL: `http://localhost:8080`
- 실제 요청한 API: 없음
- HTTP 응답 코드: 없음
- 응답 데이터 확인 여부: 확인하지 못함
- UI 반영 여부: 실제 응답이 화면에 그려지는지는 확인하지 못함
- 미검증 항목 및 이유: 서버 프로세스가 없어 인증, 환자 결정, 그래프 네 API, 약통 조회와 설정 쓰기, 권한 코드의 실제 본문을 보지 못했다. 에뮬레이터와 실기기 화면도 실행하지 못했다.

성공 JSON을 만들어 서버 응답처럼 넣지 않았다.

## 7. 수정된 파일 목록

| 파일 경로 | 변경 내용 | 변경 이유 |
|---|---|---|
| `app/src/main/java/com/example/yakyeon2/api/CareApi.java` | 그래프·약통 호출 추가 | 명세 경로를 한곳에서 호출 |
| `app/src/main/java/com/example/yakyeon2/api/CareContracts.java` | 응답 파서, 요청 범위, 공유·쓰기 규칙 | 화면이 JSON을 직접 읽지 않도록 함 |
| `app/src/main/java/com/example/yakyeon2/api/JsonReads.java` | null 가능한 숫자 읽기 | 약봉지 수가 null일 수 있음 |
| `app/src/main/java/com/example/yakyeon2/api/ApiCodes.java` | 병용금기·약통 오류 코드 | `code`로 분기 |
| `app/src/main/java/com/example/yakyeon2/api/ApiMessages.java` | 코드별 문구 | `message`가 없어도 코드를 구분 |
| `app/src/main/java/com/example/yakyeon2/MainActivity.java` | 로그인 탭 로드, 상세 조회, 동의, 설정 권한 | 실패 시 이전 데이터를 성공으로 남기지 않음 |
| `app/src/main/java/com/example/yakyeon2/main/MainScreens.java` | 서버 그래프·약통 화면 | 응답 필드를 기존 레이아웃에 표시 |
| `app/src/main/java/com/example/yakyeon2/main/SettingsScreens.java` | 로그인 설정, 읽기 전용, 예시 기기 숨김, 동의 | 로컬 연결 플래그와 서버 등록을 분리 |
| `app/src/main/assets/graph.html` | `checked = false` 점선 | 미검사 약을 검사 완료처럼 보이지 않게 함 |
| `app/src/main/res/values/strings.xml` | 안내 문구 | 안전 오해, 공유 거부, 충전 전, 부족 |
| `app/src/main/res/layout/screen_pillbox_search.xml` | 개수·안내 텍스트 id | 로그인 시 예시 기기를 숨김 |
| `app/src/test/java/com/example/yakyeon2/api/CareContractTest.java` | 파서·범위·권한 테스트 | 서버 없이 매핑을 고정 |

`GraphDocument.json`과 로그인하지 않은 그래프·약통 데모는 바꾸지 않았다.

## 8. 테스트 결과

| 테스트 | 실행 여부 | 결과 | 비고 |
|---|---|---|---|
| `./gradlew :app:testDebugUnitTest` | 실행 | 통과. 146건, 실패 0 | 기존 테스트 포함 |
| `CareContractTest` | 실행 | 통과 | 요약, `dur_seq` 문자열, 그룹 null, 약통 null, 1–3칸, 1–100개, 공유·쓰기 |
| 오류 코드 문구 | 실행 | 통과 | `CareContractTest`의 fallback 확인 |
| 그래프·약통 UI 매핑 | 실행하지 않음 | 미검증 | JVM 테스트가 레이아웃을 그리지 않음 |
| 권한별 실제 화면 | 실행하지 않음 | 미검증 | 서버와 계정이 없음 |
| `assembleDebug` | 실행 | 통과 | 디버그 APK 생성 |
| 실제 HTTP | 실행 | 실패 | `127.0.0.1:8080` 연결 거부 |
| 에뮬레이터·실기기 UI | 실행하지 않음 | 미검증 | 서버 응답을 화면에 넣을 수 없었음 |

## 9. 최종 종합 판정

전체 연동을 하나의 PASS로 보지 않는다. 실제 서버 응답이 화면에 반영되는 것까지 확인한 API는 없다.

| 항목 | 판정 |
|---|---|
| 병용금기 요약 | LOCAL PASS |
| 병용금기 그래프 | LOCAL PASS |
| 그래프 선 상세 | LOCAL PASS |
| 병용금기 목록 | LOCAL PASS |
| 그래프 탭 화면의 실제 서버 반영 | FAIL. 서버에 연결하지 못함 |
| `DEVICE` 동의 조회·저장 | LOCAL PASS |
| 약통 등록 | NOT IMPLEMENTED |
| 칸 배정 | NOT IMPLEMENTED |
| 최초 충전 | NOT IMPLEMENTED |
| 약통 본문 조회 | LOCAL PASS |
| 약통 설정 조회 | LOCAL PASS |
| 설정 변경 | LOCAL PASS |
| 일정 동기화 요청 | LOCAL PASS. 하드웨어 반영 완료는 확인하지 않음 |
| 약통 등록 해제 | LOCAL PASS |
| 추가 충전 | NOT IMPLEMENTED |
| 배정 해제 | NOT IMPLEMENTED |
| 약통 탭 화면의 실제 서버 반영 | FAIL. 서버에 연결하지 못함 |
| `attention`의 `OPENED` 외 하드웨어 이벤트 | BACKEND PENDING. 명세 4-11이 예정 |
| 공유 거부·읽기 전용 분기 | LOCAL PASS |

로그인하지 않은 그래프와 약통은 계속 로컬 데모이다. 그 화면을 서버 연동 성공으로 보지 않는다.
