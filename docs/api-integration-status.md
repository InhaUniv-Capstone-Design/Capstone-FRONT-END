# 로그인·회원가입·설정 API 대조 (2026-10-29)

기준은 초기 서버 API 명세 v0.9입니다. 이 문서의 이전 분류는 사용하지 않았습니다. 실서버에는 접속하지 않았습니다. 실제 서버 성공 0건, 실제 서버 실패 0건입니다.

기본 주소는 `http://localhost:8080`입니다. 에뮬레이터의 localhost는 에뮬레이터 자신이므로, 호스트의 8080을 쓰려면 `adb reverse tcp:8080 tcp:8080`이 필요합니다. 주소는 `-Pyakyeon.apiBaseUrl=http://...`로만 바꿉니다. 배포 주소는 몰라서 넣지 않았습니다. cleartext는 `localhost`, `127.0.0.1`, `10.0.2.2`만 허용합니다. API 계층은 토큰과 비밀번호를 로그로 남기지 않습니다.

## 요약

1. 대상 API 31개. 앱 화면에서 쓰지 말라고 적힌 옛 연동 API는 표 밖입니다.
2. 화면까지 연결된 호출 25개.
3. 이번에 클라이언트를 새로 둔 API 18개. 이 중 푸시 토큰 2개는 화면에서 호출하지 않습니다.
4. 기존 호출을 고친 API 1개. `POST /auth/logout`이 refresh token이 없으면 서버 성공으로 보이지 않습니다.
5. 명세와 아직 다른 API 5개. 푸시 등록, 푸시 삭제, 공유 PDF와 `medication_ids`, 약통 설정의 로컬 연결 표시, 권한 변경 화면.
6. 로컬 또는 화면만 남은 기능 6개.
7. 서버 배포 후 확인할 API 25개. 연결하지 않은 6개는 배포만으로 끝나지 않습니다.
8. `./gradlew :app:testDebugUnitTest --offline` 성공. 단위 테스트 140개, 실패 0. 실 HTTP 테스트는 0개입니다.
9. 서버 연결만 남았다고 볼 수 없습니다. 연결된 25개는 요청·파싱·실패 처리까지 코드로 닫혀 있고, PDF, FCM, 권한 변경, 복약자 선택, 약통 로컬 표시는 서버가 있어도 화면이 더 필요합니다.

## A. 전체 API 대조표

상태 표기: ✅ 구현됨, 🔜 예정, ❌ 제공하지 않음. 성공 코드가 절에 없으면 `명세 확인 필요`입니다. 클라이언트는 기존과 같이 HTTP 2xx를 성공으로 보고, 그 코드 번호를 명세에 있는 것처럼 단정하지 않습니다.

| 기능 | API | 메서드 및 경로 | 요청 계약 | 성공 응답 계약 | 명세상 상태 | 호출 | 화면 연결 | 명세 일치 | 로컬·Mock | 검증 | 결과 | 수정 또는 미해결 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 로그인 | 로그인 | POST `/auth/login` | `login_id`, `password` 문자열. 인증 없음 | 200. `access_token`, `refresh_token`, `token_type=Bearer`, `expires_in`, `user_id`, `account_type` | ✅ 2-1-5 | 있음 | 로그인 버튼 | 일치 | 없음 | 기존 파서, 실서버 미실행 | 코드 검토 | 유지 |
| 로그인 | 토큰 갱신 | POST `/auth/refresh` | `refresh_token`. 401 `UNAUTHENTICATED`면 한 번 재시도 | 200. 로그인과 같은 세션 필드 | ✅ 2-1-6 | 있음 | 401 재시도, 앱 시작 | 일치. 본문을 읽지 못한 2xx는 세션을 지움 | 없음 | 코드 검토 | 실서버 미실행 | 그 2xx 처리는 명세에 없음. 질문으로 남김 |
| 로그인 | 로그아웃 | POST `/auth/logout` | `refresh_token` 필수. 인증 헤더 없음 | 204 | ✅ 2-1-7 | 있음 | 설정 로그아웃 | 토큰이 있을 때만 일치 | 토큰이 없으면 로컬 세션만 삭제 | 단위 테스트로 호출 여부 분기 | 통과, 실서버 미실행 | 빈 토큰은 서버 로그아웃이 아님. 네트워크 실패는 세션을 유지 |
| 로그인 | 푸시 토큰 등록 | PUT `/users/me/push-token` | `fcm_token` 필수. `notification_permitted`는 선택이라 보내지 않음 | 204 | ✅ 2-8-7 | 메서드만 | 없음 | 본문은 일치. 호출 시점이 없음 | FCM 토큰 없음 | 본문 단위 테스트 | 통과 | 가짜 토큰을 만들지 않음 |
| 로그인 | 푸시 토큰 삭제 | DELETE `/users/me/push-token` | `fcm_token` 필수 | 204 | ✅ 2-8-8 | 메서드만 | 없음 | 호출 없음 | FCM 없음 | 본문 테스트와 같음 | 통과 | 로그아웃 전에 부를 토큰이 없음 |
| 회원가입 | 약관 목록 | GET `/terms` | 본문 없음. 인증 없음 | 200 배열. `terms_version_id`, `terms_type`, `version`, `content`, `is_required` | ✅ 2-1-13 | 있음 | 가입 동의 화면 | 일치 | 가입 본문은 서버 값. 설정 문서 읽기는 앱에 넣은 원문 | 배열 파서 테스트 | 통과 | 화면에 없는 필수 종류가 오면 가입을 막음. 버전 번호를 만들지 않음 |
| 회원가입 | 인증번호 발송 | POST `/auth/phone-verifications` | `phone` 숫자 | 201. `verification_id`, `expires_in` | ✅ 2-1-2 | 있음 | 인증번호 버튼 | 일치 | 없음 | 파서 테스트 | 통과 | 화면은 `01`로 시작하는 10~11자리를 보냄. 🔜 4-4의 `010` 11자리는 아직 거절하지 않음 |
| 회원가입 | 인증번호 확인 | POST `/auth/phone-verifications/{id}/confirm` | `code` 6자리 | 200. `verified`는 항상. 성공 때만 `verification_token`, `token_expires_in` | ✅ 2-1-3 | 있음 | 가입 버튼 | 일치 | 없음 | `verified:false` 테스트 | 통과 | 틀린 번호는 오류가 아님. 가입으로 진행하지 않음 |
| 회원가입 | 아이디 중복 | GET `/auth/check-id?login_id=` | 쿼리 `login_id` | 200. `available` | ✅ 2-1-4 | 있음 | 중복 확인 버튼 | 일치 | 없음 | 경로 인코딩 테스트 | 통과 | 이 절에 오류 코드 표는 없음 |
| 회원가입 | 회원가입 | POST `/auth/signup` | 아이디, 비밀번호, 확인, `account_type`, 약관 4개, 이름, `birth_date`, `phone`, `verification_id`, `verification_token` | 201. `user_id`만 | ✅ 2-1-1, 🔜 4-4 | 있음 | 가입 버튼 | 화면 규칙에 맞춤 | 없음 | 본문 테스트 | 통과 | 가입 응답으로 로그인하지 않음. 아이디·비밀번호는 화면의 4-4 규칙을 유지. 비밀번호 상한 64자를 추가 |
| 계정 | 비밀번호 변경 | PATCH `/auth/password` | `current_password`, `new_password`, `new_password_confirm`. Bearer | 204. 모든 세션 종료 | ✅ 2-1-8 | 있음 | 설정 | 일치 | 성공 후에만 로컬 세션 삭제 | 코드 검토 | 실서버 미실행 | 구성 규칙은 2-1-8에 없어 넣지 않음. 길이만 8~64 |
| 계정 | 회원 탈퇴 | DELETE `/auth/account` | `password` | 204 | ✅ 2-1-9 | 있음 | 설정 | 일치 | 로컬 약·기록은 삭제하지 않음 | 코드 검토 | 실서버 미실행 | 명세가 로컬 데이터 삭제를 말하지 않음 |
| 계정 | 동의 이력 | GET `/users/me/consents` | 없음. Bearer. `occurred_at`은 UTC | 200 배열 | ✅ 2-1-10 | 없음 | 화면 없음 | 해당 없음 | 없음 | 미구현 | 미실행 | 화면을 새로 만들지 않음 |
| 연동 | 관리 복약자 | GET `/users/me/managed-patients` | Bearer | 200 배열. `patient_id`, `name`, `birth_date`, `has_account`, `my_role` 항상. `link_id` 등은 생략 가능 | ✅ 2-2-9, 🔜 추가 필드 | 있음 | 연동, 약통, 공유 | 일치 | 로컬 연동 상태를 성공으로 쓰지 않음 | 파서 테스트 | 통과 | `user_id`는 이 응답의 필수 필드가 아님 |
| 연동 | 보호자 목록 | GET `/patients/{id}/guardian-links` | Bearer. `status` 쿼리는 보내지 않음 | 200 배열. `link_id`, `guardian_name`, `guardian_login_id`, `status`, `permission_scope` | ✅ 2-2-8 | 있음 | 복약자 계정의 연동 화면 | 일치 | 없음 | 코드 검토 | 실서버 미실행 | 기본 목록은 ACTIVE와 PENDING |
| 연동 | 연동 해제 | DELETE `/guardian-links/{id}` | Bearer | 204 | ✅ 2-2-6 | 있음 | 각 행의 해제 | 일치 | 204 전에 로컬 해제로 보이지 않음 | 코드 검토 | 실서버 미실행 | ACTIVE만 해제 버튼. 여러 명이면 누른 행만 해제 |
| 연동 | 권한 변경 | PATCH `/guardian-links/{id}/permission` | `permission_scope` | 200. `guardian_link_id`, `permission_scope` | ✅ 2-2-7 | 없음 | 화면 없음 | 미구현 | 없음 | 미실행 | 미실행 | 권한 변경 화면을 새로 만들지 않음 |
| 연동 | 웹 동의 승인 | POST `/guardian-links/grant` | 웹 | 명세 확인 필요 | 🔜 4-1 | 없음 | 앱 화면 아님 | 앱에서 호출하지 않음 | QR은 웹 주소만 표시 | 코드에 호출 없음 | 해당 없음 | 앱 API로 만들지 않음 |
| 연동 | 공유 항목 수정 | PATCH `/guardian-links/{id}/shared-items` | 웹 | 성공 코드 명세 확인 필요 | 🔜 4-1 | 없음 | 앱 화면 아님 | 앱에서 호출하지 않음 | 없음 | 코드에 호출 없음 | 해당 없음 | 홈·약 화면의 `shared_items` 숨김은 이번 범위 밖 |
| 설정 | 프로필 조회 | GET `/users/me/profile` | Bearer | 200. 아이디, 이름, 생년월일, 가린 번호, 계정 유형 | ✅ 2-1-11 | 있음 | 프로필 | 일치 | 성공 후에만 캐시 | 기존 파서 | 실서버 미실행 | 유지 |
| 설정 | 프로필 수정 | PATCH `/users/me/profile` | `name`만 | 200. 조회와 같음 | ✅ 2-1-12 | 있음 | 이름 저장 | 일치 | 성공 후에만 캐시 | 코드 검토 | 실서버 미실행 | 전화번호·생년월일 변경은 ❌ 4-8. API를 만들지 않음 |
| 설정 | 알림 조회 | GET `/users/me/notification-settings` | Bearer | 본문은 8개 키를 고정 순서로 항상 반환. 예시는 1개 | ✅ 2-8-5 | 있음 | 알림 설정 | 본문 규칙을 따름 | 로그인 후 서버 값으로 덮음 | 1개 예시를 거절하는 테스트 | 통과 | 예시와 본문이 다름. 파서를 느슨하게 바꾸지 않음 |
| 설정 | 알림 변경 | PATCH `/users/me/notification-settings` | 바뀐 항목만 | 조회와 같은 8개 | ✅ 2-8-6 | 있음 | 토글 | 일치 | 로그아웃 중에는 로컬 저장. 서버 저장 문구 없음 | 기존 NotificationSync 테스트 | 통과 | `DEVICE`를 끄면 보호자 알림 4개도 같이 false |
| 설정 | 약통 조회 | GET `/patients/{id}/device` | Bearer | 200. 시리얼, 온라인 등 항상. `buzzer_enabled`, `led_enabled`, `schedule_synced_at`은 없으면 생략 | ✅ 2-6-6, 🔜 4-11 | 있음 | 약통 설정 | 호출 시 일치 | 로컬 연결 표시가 꺼져 있으면 이 화면으로 들어오지 않음 | 생략 필드 테스트 | 통과 | `patient_id`는 관리 복약자에서 구함. 0명 또는 여러 명이면 고르지 않음 |
| 설정 | 부저·LED | PATCH `/patients/{id}/device/settings` | 보낸 필드만 | 약통 정보 전체. HTTP 코드는 명세 확인 필요 | 🔜 4-11 | 있음 | 스위치 | 본문은 일치 | 실패하면 스위치를 서버 값으로 되돌림. 로컬 저장으로 성공 처리하지 않음 | 본문 테스트 | 통과 | 빈 2xx는 전체 약통 정보가 아니라 응답 오류 |
| 설정 | 일정 동기화 | POST `/patients/{id}/device/sync` | 본문 없음 | 202. `schedule_version` | 🔜 4-11 | 있음 | 동기화 행 | 본문 일치. 클라이언트는 2xx면 본문을 읽음 | 없음 | 코드 검토 | 실서버 미실행 | 시각은 응답에 없어 만들지 않음 |
| 설정 | 약통 해제 | DELETE `/patients/{id}/device` | Bearer | 204 | ✅ 2-6-7 | 있음 | 연결 해제 | 일치 | 204 후에만 로컬 연결 표시를 끔 | 코드 검토 | 실서버 미실행 | `DEVICE_NOT_FOUND`는 성공이 아님. 로컬 표시는 그때 지우지 않음 |
| 설정 | 동의 조회 | GET `/users/me/agreements` | Bearer | 배열 원소는 `terms_type`, `terms_version_id`, `version`, `agreed`, `changed_at`. HTTP 코드는 명세 확인 필요 | 🔜 4-12 | 있음 | 동의 화면 | 표시는 서버 값 | 조회 전 로컬 철회 표시를 쓰지 않음 | 파서 테스트 | 통과 | 404는 오류로 표시 |
| 설정 | 다시 동의 | POST `/users/me/agreements` | `terms_version_id`, `agreed` | HTTP 코드 명세 확인 필요 | 🔜 4-12 | 있음 | 철회된 항목의 다시 동의 | 본문 일치 | 성공 후 다시 조회 | 코드 검토 | 실서버 미실행 | 2xx 뒤 조회로 표시를 갱신 |
| 설정 | 동의 철회 | POST `/users/me/agreements/{termsType}/withdraw` | 본문 없음 | 204 | 🔜 4-12 | 있음 | 철회 버튼 | 일치 | 204와 재조회 전에 표시를 바꾸지 않음 | 코드 검토 | 실서버 미실행 | 약물 카드의 버튼 둘은 `MEDICATION`. `SERVICE_NOTICE`는 철회하지 않음 |
| 설정 | 공유 데이터 | GET `/patients/{id}/share-summary` | `from`, `to` 최대 90일. `include_records`, `include_period`, `include_interactions`, `include_adherence`. `medication_ids`는 생략하면 전체 | 구역을 빼면 응답에도 없음. HTTP 코드는 명세 확인 필요. PDF는 앱이 만듦 | 🔜 4-5 | 있음 | 공유 내용 보기 | 기간·포함 플래그는 일치. PDF와 약 번호는 아님 | 로컬 약 목록 PNG를 임상 공유로 열지 않음 | 90일·경로 테스트 | 통과 | 91일 이상은 요청하지 않음. 현재 약을 끄면 약 번호가 없어 요청하지 않음 |

### 앱에서 호출하지 않는 옛 연동

명세가 앱 화면에서 쓰지 말라고 한 API는 추가하지 않았습니다. `GET /patients/search`, `POST /patients/{id}/guardian-links`, 문자 인증 확인, 연동 동의, 보호자의 `POST /patients`, `POST /revocations/request`, `POST /revocations/confirm`.

하드웨어 전용 `GET /device/schedule`도 호출하지 않습니다.

## B. 수정 내역

- `ApiClient`: JSON 배열 응답을 읽습니다. 본문과 토큰은 로그에 없습니다.
- `AuthApi`, `AuthContracts`: 약관, 문자 인증, 아이디 확인, 회원가입, 푸시 토큰 본문. 로그아웃은 refresh token이 없으면 false를 반환하고 서버를 호출하지 않습니다.
- `SettingsApi`, `SettingsContracts`: 관리 복약자, 보호자 목록, 해제, 동의, 약통, 공유 쿼리와 파서.
- `AuthValidation`: 비밀번호 8~64자, 전화 `01` 10~11자리, 생년월일 `YYYY-MM-DD` 변환 전 검사.
- `AuthScreens`, `screen_signup_form.xml`: 인증번호 입력. 동의 본문은 `GET /terms` 결과입니다.
- `MainActivity`: 가입은 확인 후 201만 받고 로그인 화면으로 돌아갑니다. 연동·동의·약통·공유는 성공 응답 뒤에만 화면을 바꿉니다.
- `SettingsScreens`: 로컬 철회, 로컬 부저, 로컬 약통 해제, 로컬 PNG 공유를 성공 경로에서 뺐습니다.
- `ApiConfig`, `app/build.gradle.kts`: 기본 URL은 `http://localhost:8080`. Gradle 속성으로만 바꿉니다.
- `network_security_config.xml`: 에뮬레이터 주소와 루프백의 cleartext를 허용합니다. 기본 URL은 localhost입니다.
- `ApiCodes`, `ApiMessages`: 가입·인증·약통·공유·연동 오류의 빈 메시지 대체 문구. 서버 `message`가 있으면 그 문구를 먼저 씁니다.
- 테스트 `ApiContractTest`, `AuthValidationTest`. 단위 테스트가 Android stub 대신 `org.json`을 쓰도록 `testImplementation`을 추가했습니다.

홈, 그래프, 약 등록, 복약 기록 화면은 수정하지 않았습니다. 보호자의 `shared_items`와 `READ_ONLY`에 맞춰 그 화면의 버튼을 숨기는 일은 설정 조회 결과에 의존하지만 이번 범위 밖입니다.

## C. 검증 결과

| 검증 | 결과 |
| --- | --- |
| `./gradlew :app:testDebugUnitTest --offline` | 성공. 140개, 실패 0, 오류 0, 건너뜀 0 |
| 모의 JSON 파싱·요청 본문·90일 제한·알림 8개 규칙·로그아웃 분기 | 성공. 실서버 응답이 아님 |
| 실 HTTP 요청 | 미실행. 성공 0, 실패 0 |
| 기기 UI 조작 | 미실행. 단위 테스트는 화면 클릭을 하지 않음 |

첫 컴파일은 문자 리터럴 오류로 실패했고 고쳤습니다. `org.json` stub 때문에 계약 테스트 11개가 실패한 뒤, 테스트 전용 라이브러리를 넣고 다시 통과했습니다.

## D. 미해결

백엔드 확인이 필요합니다.

- `GET /patients/{id}/share-summary`, `PATCH /patients/{id}/device/settings`, `GET /users/me/agreements`, `POST /users/me/agreements`의 성공 HTTP 코드가 절에 없습니다.
- 알림 조회 본문은 8개 키를 항상 준다고 하고, 예시는 1개입니다. 앱은 8개가 아니면 응답 오류로 봅니다.
- 갱신 응답이 2xx인데 세션 필드를 읽을 수 없으면 앱이 세션을 지웁니다. 명세에 이 경우가 없습니다.
- 탈퇴 뒤 이 기기의 약·복약 기록을 지울지 명세에 없습니다. 지우지 않습니다.
- 약통 조회가 `DEVICE_NOT_FOUND`일 때 로컬 연결 표시를 지워도 되는지 명세에 없습니다. 지우지 않습니다.
- 휴대폰은 지금 `01X` 10~11자리이고, 🔜 4-4는 나중에 `010` 11자리입니다.
- 약물 카드의 철회 버튼 둘은 명세가 둘 다 `MEDICATION`이라고 합니다. 버튼 문구와 다릅니다.
- 공유에서 약을 일부만 보내려면 `medication_ids`가 필요한데, 이 화면에 서버 약 번호 선택이 없습니다.
- 보호자가 복약자가 여럿이면 약통·공유에서 한 명을 고르지 않습니다. 선택 화면은 명세의 별도 화면이라 이번에 만들지 않았습니다.
- 배포 서버의 베이스 URL.

서버가 있어야 확인되는 것: 위 표의 화면 연결 25개 전부의 실제 상태 코드, 오류 본문, 세션 만료 재시도.

## E. 서버 배포 후 체크리스트

아래는 명세에 있는 동작입니다. 명세에 없는 항목은 필수로 적지 않았습니다.

- 로그인 성공과 `INVALID_CREDENTIALS`.
- 회원가입: 약관 조회, 인증번호, 틀린 번호의 `verified:false`, 중복 아이디, 201 뒤 자동 로그인이 아닌지.
- 401 후 갱신 한 번, 갱신 재사용 `REFRESH_TOKEN_REUSED`면 모든 세션 종료.
- refresh token이 있을 때 로그아웃 204. 네트워크 실패 때 세션이 유지되는지.
- 프로필 이름만 수정. 전화번호·생년월일은 읽기 전용.
- 비밀번호 변경 204 뒤 로그인 화면. 탈퇴 204.
- 알림 8개 조회와 변경. `DEVICE`를 끄면 보호자 항목 4개가 같이 가는지.
- 동의 조회, `MEDICATION`·`TOS`·`PRIVACY`·`SENSITIVE_HEALTH`·`DEVICE` 철회 204, 다시 동의. `SERVICE_NOTICE`는 철회 요청을 보내지 않는지.
- 복약자 계정은 보호자 목록, 보호자 계정은 관리 복약자. ACTIVE 한 건 해제 204.
- 약통이 등록된 단일 복약자에서 조회, 부저·LED, 동기화 202, 해제 204. 없는 약통은 성공처럼 보이지 않는지.
- 공유 90일 이내 조회. 91일은 요청 전에 막히는지. `SHARE_NOT_GRANTED` 표시.
- 푸시 토큰은 FCM 토큰이 생기기 전에는 호출하지 않습니다. 채널 ID `INTERACTION`, `INTAKE`, `DEVICE`, `STOCK`은 아직 만들지 않았습니다.
