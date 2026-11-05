# AWS API Gateway 연동 상태

작성: 2026-11-02.  
대상 앱: 약연 Android (`com.example.yakyeon2`).  
실서버 판정은 이날 API Gateway에 보낸 HTTP 응답만 사용한다. 단위 테스트와 백엔드 소스 대조는 실연동 성공이 아니다.

백엔드 대조 기준: `InhaUniv-Capstone-Design/Capstone-BACK-END` 브랜치 `develop`, 커밋 `b99f55bceb6517dff1d3114f53db335b7b1114a7` (2026-11-02T09:18:18Z).  
서버 내부 포트는 `application.yml`의 `server.port=8080`이고, `server.servlet.context-path`는 없다. 경로 접두어 `/api/v1`도 없다.

## 결론

앱의 기본 Base URL은 API Gateway HTTPS 주소다. 2026-11-02 20:23 KST 이후 서버가 다시 응답했다. 직전 시도(11:06–11:12 UTC)의 503은 서버가 꺼져 있던 때의 기록이다.

20:23 KST 재시도에서 `GET /terms`는 200 배열이었다. 아이디 확인, 인증번호 발송, 틀린 인증번호, 로그인 실패, 토큰 없는 프로필·알림·동의 요청도 Spring 오류 형식(`code`, `message`)으로 돌아왔다.

20:27 KST에는 인증번호 발송·확인을 호출하지 않고 다시 요청했다. 약관, 아이디 확인, 로그인 오류, 로그아웃 204, 토큰 없는 설정 API 거절은 같은 형식이었다. 계정은 만들지 않았고 JWT는 발급되지 않았다. 인증이 필요한 API의 200 본문과 앱 화면 표시는 아직 확인하지 못했다.

20:31 KST 통합 테스트에서는 서버 로그의 Mock 인증번호가 이 요청에 없어 가입·로그인·화면 표시를 진행하지 않았다. 약관 7종과 필수 4종의 `terms_version_id`는 다시 확인했다. 약품·DUR API는 `develop` 소스로 구분했고, JWT 없이 보낸 조회는 401이라 RDS 본문은 보지 못했다. 판정은 아래 「통합 테스트」절이다.

## 통합 테스트 (2026-11-02 20:31 KST)

구분: 성공은 서버 응답과 화면 표시를 모두 본 것, 부분 성공은 서버 응답만 본 것, 실패는 요청이 거절됐거나 서버에 경로가 없는 것이다. 인증번호가 없어 호출하지 않은 항목은 미실시다. 실패로 적지 않는다.

에뮬레이터 AVD `Pixel_8`은 있다. 연결된 기기는 없었다. 가입을 마칠 수 없어 에뮬레이터는 켜지 않았다.

### 가입과 로그인

| 항목 | 판정 | 근거 |
| --- | --- | --- |
| 약관 7종 조회 | 부분 성공 | `GET /terms` 200. id 1 TOS, 2 PRIVACY, 3 SENSITIVE_HEALTH, 4 SERVICE_NOTICE, 5 MEDICATION, 6 DEVICE, 7 DELEGATION. 화면 미표시 |
| 필수 4종 id | 부분 성공 | TOS 1, PRIVACY 2, SENSITIVE_HEALTH 3, SERVICE_NOTICE 4. 모두 `is_required: true`, version `1.0` |
| 아이디 중복 | 부분 성공 | `ykflow1102c`는 `available: true`. 화면 미표시 |
| 회원가입 | 미실시 | 로그에서 전달된 6자리 인증번호가 이 요청에 없다. 임의 번호와 가짜 `verification_token`은 쓰지 않았다. 앞선 `01000000000` 인증은 유효 300초가 지났다 |
| 인증 확인 `verified: true` | 미실시 | 같은 이유 |
| 로그인 JWT | 미실시 | 계정이 없다 |
| 토큰 저장, 재실행, refresh 200 | 미실시 | JWT가 없다 |
| 프로필, 알림 8개, 동의 목록의 200과 화면 | 미실시 | JWT가 없다. 토큰 없는 호출은 이전에 401 `UNAUTHENTICATED` |
| 알림 PATCH 저장 | 미실시 | JWT가 없다 |

### 약품 데이터셋 API

기준은 `develop` `b99f55bc`. 이번 HTTP는 토큰 없이 보냈고 아래 조회는 모두 401 `UNAUTHENTICATED`다. 없는 경로도 토큰 없이는 401이라, 이 상태 코드만으로 구현을 증명하지 않는다. RDS에 SQL을 실행하지 않았다. 792,670건과 8,206개는 이번 요청으로 세지 못했다.

| 질문 | 소스 | Android | 실측 | 판정 |
| --- | --- | --- | --- | --- |
| 약품 검색 | `GET /medications/autocomplete?q=&limit=`. `DrugRepository.searchByName`이 `drug.product_name` ILIKE. 응답은 `drug_id`, `product_name`, `manufacturer`. 2글자 미만은 빈 배열, 50자 초과는 400 | `HomeContracts.autocompletePath`가 같은 경로 | 401. 제품명 본문 없음 | 구현은 소스에 있음. 실데이터 조회는 미실시 |
| 성분 조회 API | Controller 없음. `Drug`에 성분 컬럼 없음. 성분 코드는 DUR 쌍 결과에만 있다 | 호출 없음 | 호출하지 않음 | 미구현 |
| DUR 병용금기 조회 | 로그인 후 환자 기준 `GET /patients/{id}/interactions`, `/interactions/summary`, `/interaction-graph`, `GET /interaction-edges/{durSeq}`. `DurTabooLookup`이 `dur_taboo_item`, `dur_taboo_rule`, `dur_dataset`를 읽는다 | `CareApi`가 네 경로를 호출 | 401. 금기 본문 없음 | 구현은 소스에 있음. 실데이터 조회는 미실시 |
| 새 약과 기존 복용약의 쌍을 미리 조회 | 공개 경로 없음. `InteractionService.recheck`가 이미 저장된 약의 `item_seq`끼리 `findPairs`를 호출한다. 약 추가·수정·삭제 트랜잭션 안에서만 돈다 | 미리보기 호출 없음 | 호출하지 않음 | 미구현 |

임의 경로는 호출하지 않았다. 백엔드에 추가할 것과 앱이 붙일 화면은 다음과 같다.

| 필요한 API | 요청 | 예상 응답 | 연결 화면 | 백엔드 작업 |
| --- | --- | --- | --- | --- |
| 성분 조회 | 로그인. `drug_id` 또는 `item_seq` | 그 제품의 성분 코드와 이름 목록 | 약 검색 결과, 그래프 노드 | `drug`에 성분이 없으므로 성분 테이블을 읽고 DTO를 추가 |
| 등록 전 병용금기 쌍 | 로그인. 환자 id, 후보 `drug_id` 또는 `item_seq` | 현재 복용약과의 쌍. `dur_seq`, 두 제품, 사유 | 약 추가 전 확인 | 저장 없이 `findPairs`에 후보 품목코드를 넣는 조회. 지금은 저장 후 `recheck`만 있음 |

검색과 그래프 조회는 경로가 있으므로 새 경로를 만들지 않는다. JWT로 200 본문을 받은 뒤에 기존 `HomeApi`·`CareApi` 화면에 그대로 두면 된다.

## 1. 수정한 파일

| 파일 | 변경 |
| --- | --- |
| `app/build.gradle.kts` | 기본 `yakyeon.apiBaseUrl`을 API Gateway HTTPS로 변경 |
| `app/src/main/java/com/example/yakyeon2/api/ApiClient.java` | Base URL과 경로를 한 곳에서 잇도록 `endpoint` 추가. 끝 슬래시는 제거 |
| `app/src/test/java/com/example/yakyeon2/api/ApiContractTest.java` | 위 조합과 HTTPS 기본 주소를 확인하는 단위 테스트 1개 |
| `docs/aws-api-integration-status.md` | 이 문서 |

UI, 화면 이동, 입력 검증, API 클래스의 경로·JSON 필드는 바꾸지 않았다. Retrofit이나 OkHttp는 추가하지 않았다. 앱은 계속 `java.net.HttpURLConnection`과 `org.json`을 쓴다.

## 2. Base URL과 API 설정

주소는 `app/build.gradle.kts`의 `BuildConfig.API_BASE_URL` 한 곳만 기본값으로 둔다. `ApiConfig.BASE_URL`은 그 값을 읽기만 한다. Java 소스에 Gateway 문자열을 다시 적지 않았다.

```
https://kue200p7z3.execute-api.ap-northeast-2.amazonaws.com
```

끝 슬래시는 없다. 앱 경로는 `/terms`, `/auth/login`처럼 `/`로 시작한다. 조합 예:

`https://kue200p7z3.execute-api.ap-northeast-2.amazonaws.com/terms`

`-Pyakyeon.apiBaseUrl=` 또는 `gradle.properties`의 같은 키는 기존처럼 이 기본값을 덮어쓴다. 실서비스 기본값으로 `http://localhost:8080`, `http://127.0.0.1:8080`, `http://15.164.17.62:8080`은 넣지 않았다. 그 주소로 덮어쓰면 `endpointAppendsTheApiPathOnce`가 실패한다.

통신 설정은 기존과 같다.

| 항목 | 동작 |
| --- | --- |
| 스킴 | HTTPS. cleartext 허용은 `network_security_config.xml`의 `localhost`, `127.0.0.1`, `10.0.2.2`뿐이라 Gateway에는 적용되지 않는다 |
| Accept | `application/json` |
| JSON 본문 | `Content-Type: application/json; charset=utf-8` |
| 인증 헤더 | 인증 API만 `Authorization: Bearer <access_token>`. 토큰 종류가 `Bearer`가 아니면 세션을 저장하지 않는다 |
| 토큰 저장 | `AuthTokenStore` (`yakyeon_auth`). `expires_in`초로 만료 시각을 저장하고, 60초 여유로 `hasUsableAccess`를 본다 |
| 만료 | 앱 시작 때 access가 쓸 수 없으면 refresh token으로 `POST /auth/refresh`를 한 번 시도한다. 인증 API가 `401`이고 코드가 `UNAUTHENTICATED`이면 갱신 한 번 뒤 재요청한다. `INVALID_REFRESH_TOKEN`, `REFRESH_TOKEN_REUSED`만 세션을 지운다 |
| 5xx·단절 | 세션을 지우지 않는다. Gateway 503은 `code`가 없어 서버 오류로 분류된다 |
| 로그 | API 계층은 토큰과 비밀번호를 로그로 남기지 않는다 |

`develop`의 `SecurityConfig`에서 JWT 없이 허용하는 경로는 다음뿐이다.

- `POST /auth/signup`, `/auth/login`, `/auth/refresh`, `/auth/logout`
- `GET /auth/check-id`
- `POST /auth/phone-verifications`, `/auth/phone-verifications/*/confirm`
- `GET /terms`
- `POST /revocations/request`, `/revocations/confirm` (앱은 호출하지 않음)
- `POST /device/heartbeat`, `/device/signals` (기기용. 앱 사용자 JWT와 별개)

그 외는 인증이 필요하다. 프로필, 알림 설정, 동의, 관리 복약자, 약통이 여기 해당한다.

## 3. 실제 서버 요청

재시도는 2026-11-02 20:23 KST 이후다. SQL, SSH, EC2 `8080` 직접 호출은 하지 않았다. 가입으로 계정을 만들지 않았다.

앞선 시도(11:06–11:12 UTC)는 같은 주소가 모두 Gateway 503 `{"message":"Service Unavailable"}`이었다. 아래는 서버를 다시 켠 뒤의 결과다.

| 요청 | HTTP | 본문 |
| --- | --- | --- |
| `GET /terms` | 200 | 배열 7건. 필드는 `terms_version_id`, `terms_type`, `version`, `content`, `is_required` |
| `GET /auth/check-id?login_id=ykprobe1102a` | 200 | `{"available":true}` |
| `GET /auth/check-id?login_id=yakyeon-aws-probe-not-created` | 200 | `{"available":false}`. 하이픈이 있어 가입 규칙(영문·숫자 6~20자)에 맞지 않는다. 계정이 있어서가 아니다 |
| `POST /auth/login` 없는 아이디 | 401 | `INVALID_CREDENTIALS` |
| `POST /auth/login` 빈 본문 | 400 | `VALIDATION_ERROR`. `password: 비밀번호를 입력해주세요.` |
| `POST /auth/phone-verifications` `01000000000` | 201 | `{"verification_id":1,"expires_in":300}` |
| `POST /auth/phone-verifications/1/confirm` 코드 `000000` | 200 | `{"verified":false}`. `verification_token` 없음 |
| `POST /auth/signup` 위 번호, 가짜 `verification_token` | 400 | `VERIFICATION_NOT_COMPLETED`. 이후 `ykprobe1102a`는 여전히 `available:true` |
| `POST /auth/refresh` 가짜 refresh | 401 | `INVALID_REFRESH_TOKEN` |
| `GET /users/me/profile` 토큰 없음 | 401 | `UNAUTHENTICATED` |
| `GET /users/me/profile` `Bearer not-a-real-jwt` | 401 | `UNAUTHENTICATED` |
| `GET /users/me/notification-settings` 토큰 없음 | 401 | `UNAUTHENTICATED` |
| `GET /users/me/agreements` 토큰 없음 | 401 | `UNAUTHENTICATED` |
| `GET /no-such-yakyeon-path` 토큰 없음 | 401 | `UNAUTHENTICATED`. 없는 경로와 인증 필요 경로가 토큰 없이는 같은 401이다 |

약관 7건:

| terms_version_id | terms_type | version | is_required |
| --- | --- | --- | --- |
| 1 | TOS | 1.0 | true |
| 2 | PRIVACY | 1.0 | true |
| 3 | SENSITIVE_HEALTH | 1.0 | true |
| 4 | SERVICE_NOTICE | 1.0 | true |
| 5 | MEDICATION | 1.0 | false |
| 6 | DEVICE | 1.0 | false |
| 7 | DELEGATION | 1.0 | false |

가입 화면이 필수로 보는 4종(TOS, PRIVACY, SERVICE_NOTICE, SENSITIVE_HEALTH)이 있고, 나머지 3종은 `is_required`가 false다. 앱은 이때 가입을 막지 않는다. 약 등록이 찾는 `MEDICATION` 버전은 5다.

### 인증번호를 제외한 재시도

2026-11-02 20:27 KST. `POST /auth/phone-verifications`와 confirm은 호출하지 않았다. 가입 본문에 인증번호나 `verification_token`을 넣지 않았다.

| 요청 | HTTP | 본문 |
| --- | --- | --- |
| `GET /terms` | 200 | 위와 같은 7건 |
| `GET /auth/check-id?login_id=ykprobe1102a` | 200 | `{"available":true}` |
| `GET /auth/check-id?login_id=ykprobe1102b` | 200 | `{"available":true}` |
| `POST /auth/login` `ykprobe1102a`와 임의 비밀번호 | 401 | `INVALID_CREDENTIALS` |
| `POST /auth/login` `{}` | 400 | `VALIDATION_ERROR`. 이번 본문은 `login_id: 아이디를 입력해주세요.` 빠진 필드 문구는 하나다 |
| `POST /auth/refresh` 가짜 토큰 | 401 | `INVALID_REFRESH_TOKEN` |
| `POST /auth/logout` 존재하지 않는 refresh | 204 | 본문 없음. 맞는 토큰만 폐기하는 구현이라 다른 세션은 건드리지 않는다 |
| `POST /auth/signup` `{}` | 400 | `VALIDATION_ERROR`. `name: 이름을 입력해주세요.` 계정은 만들어지지 않음 |
| `GET /users/me/profile` 토큰 없음, 가짜 Bearer | 401 | `UNAUTHENTICATED` |
| `PATCH /users/me/profile` 토큰 없음 | 401 | `UNAUTHENTICATED` |
| `GET`·`PATCH /users/me/notification-settings` 토큰 없음 | 401 | `UNAUTHENTICATED` |
| `GET /users/me/agreements` 토큰 없음 | 401 | `UNAUTHENTICATED` |
| `GET /users/me/managed-patients` 토큰 없음 | 401 | `UNAUTHENTICATED` |
| `PATCH /auth/password` 토큰 없음 | 401 | `UNAUTHENTICATED` |
| `DELETE /auth/account` 토큰 없음 | 401 | `UNAUTHENTICATED` |

## 4. 우선 API 대조와 실측

요청·응답 칸은 `develop` 소스와 이번 HTTP를 같이 본 것이다. 200 본문을 받지 못한 항목은 연동 완료가 아니다.

| 기능 | 메서드와 경로 | 요청 | 성공 응답 | JWT | 실측 | 연동 |
| --- | --- | --- | --- | --- | --- | --- |
| 약관 조회 | `GET /terms` | 없음 | 200 배열. 5개 필드 | 불필요 | 200, 7건 | 성공. 가입 4종 필수, MEDICATION는 5번·선택 |
| 인증번호 발송 | `POST /auth/phone-verifications` | `phone` | 201. `verification_id`, `expires_in` | 불필요 | 201. id 1, 300초 | 응답 형식은 성공. 문자 도달은 미확인 |
| 인증번호 확인 | `POST /auth/phone-verifications/{id}/confirm` | `code` | 200. `verified`. 성공 시에만 토큰 | 불필요 | 틀린 코드 200 `verified:false` | 실패 형식은 성공. 맞는 코드와 토큰은 미확인 |
| 아이디 중복 | `GET /auth/check-id?login_id=` | 쿼리 | 200. `available` | 불필요 | 규칙에 맞는 미사용 아이디 true, 형식 오류 false | 성공 |
| 회원가입 | `POST /auth/signup` | 아이디, 비밀번호, 유형, 약관 4개, 이름, 생년월일, 전화, 인증 id·토큰 | 201. `user_id` | 불필요 | 가짜 토큰 400 `VERIFICATION_NOT_COMPLETED`. 계정 없음 | 실패 형식만 확인. 201 미확인 |
| 로그인 | `POST /auth/login` | `login_id`, `password` | 200. 토큰 6필드 | 불필요 | 없는 계정 401 `INVALID_CREDENTIALS`. 빈 본문 400 `VALIDATION_ERROR` | 오류 형식은 성공. JWT 발급 미확인 |
| 토큰 갱신 | `POST /auth/refresh` | `refresh_token` | 200. 로그인과 같은 6필드 | 불필요 | 가짜 토큰 401 `INVALID_REFRESH_TOKEN` | 오류 형식은 성공. 갱신 200 미확인 |
| 프로필 조회 | `GET /users/me/profile` | 없음 | 200. 6필드 | 필요 | 토큰 없음·가짜 Bearer 모두 401 `UNAUTHENTICATED` | 인증 거절만 확인. 200 미확인 |
| 프로필 수정 | `PATCH /users/me/profile` | `name`만 | 200 | 필요 | 미호출 | 미검증 |
| 알림 설정 조회 | `GET /users/me/notification-settings` | 없음 | 200. `settings` 8개 | 필요 | 토큰 없음 401 `UNAUTHENTICATED` | 인증 거절만 확인. 8개 본문 미확인 |
| 알림 설정 변경 | `PATCH /users/me/notification-settings` | 바뀐 항목만 | 200. 8개 | 필요 | 미호출 | 미검증 |
| 동의 조회 | `GET /users/me/agreements` | 없음 | 200 배열 | 필요 | 토큰 없음 401 `UNAUTHENTICATED`. 404 아님 | 배포됨. 200 본문 미확인 |
| 다시 동의 | `POST /users/me/agreements` | `terms_version_id`, `agreed` | 200 | 필요 | 미호출 | 미검증 |
| 동의 철회 | `POST /users/me/agreements/{termsType}/withdraw` | 없음 | 204 | 필요 | 미호출 | 미검증 |

앱이 보내는 가입 `account_type`은 `AccountRoles`가 `PATIENT`/`GUARDIAN`으로 바꾼 값이다. 화면의 본인/보호자 구분과 같다. 전화는 앱이 `010` 11자리만 보내고, 서버 정규식 `^01[016789][0-9]{7,8}$`의 부분집합이다.

알림 8개 키는 양쪽이 같다. `DOSE_REMINDER`, `MISSED`, `END_SOON`, `DEVICE`, `GUARDIAN_OPEN`, `GUARDIAN_TAKEN`, `GUARDIAN_MISSED`, `GUARDIAN_DEVICE`.

`docs/api-integration-master-status.md`(2026-11-01)는 동의 API를 서버 미구현으로 적었다. 이번 `GET /users/me/agreements`는 404가 아니라 401 `UNAUTHENTICATED`다. 경로는 배포되어 있다. 배열 본문은 로그인 후에만 볼 수 있다.

## 5. 성공한 기능과 실패한 기능

실서버에서 응답까지 확인한 것:

- 약관 7건 조회. 가입 화면이 요구하는 4개 필수 약관이 있다.
- 형식에 맞는 미사용 아이디는 `available:true`.
- 인증번호 발송 201, 틀린 번호는 `verified:false`.
- 로그인 실패와 빈 본문은 `INVALID_CREDENTIALS`, `VALIDATION_ERROR`.
- 가짜 refresh는 `INVALID_REFRESH_TOKEN`. 앱은 이 코드에서 세션을 지운다.
- 토큰이 없거나 가짜 Bearer면 프로필·알림·동의는 `UNAUTHENTICATED`. 앱은 이 코드에서 갱신을 한 번 시도한다.

아직 못 한 것:

- 맞는 인증번호, `verification_token`, 회원가입 201.
- 로그인 200과 JWT 6필드 저장.
- 그 토큰으로 프로필·알림 8개·동의 배열을 읽어 화면에 표시하는 것.

20:27 재시도 뒤에도 `ykprobe1102a`는 `available:true`다. 인증번호 API는 다시 호출하지 않았다. 앞선 `01000000000` 인증 1건은 그대로다.

로그아웃 204는 그 refresh가 서버에 없을 때의 응답이다. 로그인 200은 아니다.

기기와 에뮬레이터는 실행하지 않았다. 이번 재시도에서 앱 코드는 바꾸지 않았다.

## 6. 백엔드에서 필요한 일

1. 이번 재시도에서 Gateway는 Spring JSON을 돌려줬다. 503 복구는 끝났다.
2. 가입을 끝까지 보려면 서버 로그의 인증번호가 필요하다. HTTP 응답에는 코드가 없다.
3. `develop`에 아직 없는 경로. 앱은 이미 호출한다. 토큰 없는 요청은 없는 경로도 401이라, 아래는 이번 HTTP로 404를 확인하지 못했다. 소스 기준이다.
   - `PATCH /patients/{id}/device/settings`
   - `POST /patients/{id}/device/sync`
   - `GET /patients/{id}/share-summary`
   - `POST /guardian-links/grant`
   - `PATCH /guardian-links/{id}/shared-items`
4. 약통 MQTT 클라이언트는 이 `develop` 트리에 없다. 기기 수신은 `POST /device/heartbeat`, `POST /device/signals`이다.

## 7. 아직 Mock인 기능

이번 HTTP로는 Mock 결과를 받지 못했다. 아래는 `develop` 소스와 이번 작업 지시의 배포 설명이다.

| 기능 | 소스 | 이번 실측 |
| --- | --- | --- |
| SMS | `MockSmsGateway`. `sms.provider`가 없거나 `mock`이면 문자를 보내지 않고 로그에만 남긴다. 저장소 `application.yml`은 `sms.provider`를 지정하지 않는다 | `01000000000` 발송은 201. 응답만으로는 실제 발송인지 Mock인지 구분되지 않는다. 로그는 보지 않음 |
| OCR | `MockPrescriptionOcrClient`는 `ocr.provider=mock`일 때만 동작하고, 이미지와 관계없이 같은 3개 약을 반환한다 | 미호출 |
| 푸시 | `MockPushSender`는 `push.provider=mock`일 때만 동작한다. 휴대폰 알림을 보내지 않는다 | 미호출. 앱은 FCM 토큰이 생기기 전에는 `PUT /users/me/push-token`을 호출하지 않는다 |
| 약통 MQTT | 소스에 MQTT 연동 없음 | 미호출 |

Mock이 켜진 응답이 나중에 오더라도 문자 발송, OCR 판독, 푸시 도착, 약통 연동이 된 것으로 보지 않는다.

## 8. 다음 작업

1. 가입 201과 로그인 JWT는 서버 로그의 6자리 인증번호가 이 대화에 들어와야 진행한다. 임의 번호는 쓰지 않는다. 아이디 `ykflow1102c`는 20:31 직후 `available:true`였다. `ykprobe1102a`, `ykprobe1102b`는 20:27 직후 `available:true`였다.
2. 로그인 200의 토큰 6필드를 저장한 다음 `GET /users/me/profile`, `GET /users/me/notification-settings`, `GET /users/me/agreements`가 200인지 본다.
3. 그 응답으로 앱 화면에 값이 나오는지는 에뮬레이터 또는 기기에서 확인한다.
4. 권한 관리 웹 `yakyeon-consent-web/js/config.js`의 `API_BASE_URL`은 아직 `http://127.0.0.1:8080`이다.
5. 약통 설정·동기화, 공유 요약, 웹 동의 `grant`/`shared-items`는 서버에 경로가 생긴 뒤 JWT로 실측한다.
