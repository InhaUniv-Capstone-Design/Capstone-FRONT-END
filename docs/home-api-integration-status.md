# 홈 탭 API 연동 현황

명세 버전: 약연 서버 API 명세서 v0.9  
기준일: 2026-10-31  
실제 서버: `http://127.0.0.1:8080`, `http://localhost:8080` 모두 연결 거부. HTTP 응답으로 화면이 바뀌는 것까지 확인한 항목은 없다.

판정은 호출 코드가 있다는 이유만으로 완료로 두지 않았다.

## 1) 작업 개요

조사한 구조는 Activity + 화면 빌더다. ViewModel은 없다. `ApiClient`가 `HttpURLConnection`으로 호출하고, `401 UNAUTHENTICATED`이면 `POST /auth/refresh`를 한 번만 다시 시도한다. 로그인 세션이 있으면 홈·복약 기록·약물 목록을 서버 응답으로 그린다. 세션이 없으면 기존 `MedicationStore` 데모를 그대로 둔다.

수정 범위는 홈 탭과 홈에서 들어가는 기록, 일정, 약물 목록, 등록, 복용 시각, 등록 중 칸 배정, 그리고 그 호출에 필요한 공통 파서와 오류 코드다. 그래프 탭, 약통 탭, 설정 탭, 로그인 화면의 동작은 바꾸지 않았다. 공통 클라이언트에는 OCR용 multipart와 오류 코드, 소수 파서만 더했다.

병용금기 요약은 이미 있던 `CareApi.summary`를 재사용한다. 칸 배정 호출은 이미 있던 `CareApi.assignCompartment`를 등록 성공 뒤에만 쓴다. 약통 등록, 충전, 부저, LED는 호출하지 않는다.

## 2) API별 연동 현황

실제 HTTP 열은 모두 미확인이다. 서버가 없어 요청을 보내지 못했다.

| API | 호출 코드 | 응답 파서 | 화면 연결 | 실제 HTTP 확인 | 테스트 결과 | 최종 판정 |
|---|---|---|---|---|---|---|
| `GET /patients/{id}/home-summary` | 있음 | 있음 | 진행률, 아침·점심·저녁, 다음 복약 | 미확인 | 파서 테스트 통과 | LOCAL PASS |
| `GET /patients/{id}/medications?status=ACTIVE` | 있음 | 있음 | 홈의 복용 중 개수 = 배열 길이 | 미확인 | 파서 테스트 통과 | LOCAL PASS |
| `GET /patients/{id}/interactions/summary` | 기존 `CareApi.summary` | 기존 파서 | 홈 카드는 `flagged`. `unchecked > 0`이면 안전하다고 쓰지 않음. 누르면 그래프 탭 | 미확인 | 기존 파서 유지 | LOCAL PASS |
| `GET /patients/{id}/doses?date=` | 있음 | 있음 | 오늘 남은 복약은 `PENDING`만, 일정 화면은 고른 날짜 | 미확인 | null `taken_at` 테스트 통과 | LOCAL PASS |
| `POST /doses/{id}/taken` | 있음 | 일정 한 건 | 일정 화면 "먹었어요" / "복용 시각 수정". 응답 전 화면을 성공으로 바꾸지 않음 | 미확인 | `taken_at` 본문 테스트 통과 | LOCAL PASS |
| `POST /doses/{id}/not-taken` | 있음. 본문 없음 | 일정 한 건 | 일정 화면 "안 먹었어요" | 미확인 | 응답 파서는 일정과 동일 | LOCAL PASS |
| `GET /patients/{id}/dose-history` | 있음 | 일정과 동일 | 기록 화면. 전체는 `status` 생략, 완료는 `NORMAL`, 지연, 누락. 기본은 쿼리 생략(서버 14일). 직접 고르면 최대 90일 | 미확인 | 경로 테스트 통과 | LOCAL PASS |
| `GET /patients/{id}/medications` | 있음 | 있음 | 홈 개수는 `ACTIVE`만 사용. 목록 화면은 처방 목록을 사용 | 미확인 | 소수·null 필드 테스트 통과 | LOCAL PASS |
| `GET /patients/{id}/prescriptions` | 있음. `groupByHospital`는 보내지 않음 | 있음 | 로그인 약물 목록 | 미확인 | 안의 Medication 파서만 테스트 | PARTIAL |
| `GET /medications/autocomplete` | 있음 | 있음 | 로그인 중 약 이름 검색 | 미확인 | 경로만 테스트. 2글자 미만은 호출하지 않고 빈 목록. 50자 초과는 호출하지 않음 | PARTIAL |
| `POST /prescriptions/ocr` | multipart `image` | 있음 | 로그인 중 촬영 버튼이 사진을 고름 | 미확인 | 빈 `recognized` 테스트 통과. 실제 업로드 없음 | LOCAL PASS |
| `POST /patients/{id}/prescriptions` | 있음 | `prescription_id`, `medication_ids` | 등록 마지막 저장 | 미확인 | 본문 테스트 통과. `BEDTIME`이 있으면 호출하지 않음 | LOCAL PASS |
| `PATCH /medications/{id}` | 있음 | Medication | 없음. 명세 4-10대로 이 화면은 약 내용을 수정하지 않음 | 미확인 | 본문 빌더는 등록과 같은 코드. PATCH 전용 테스트 없음 | NOT IMPLEMENTED |
| `DELETE /medications/{id}` | 있음. `204`면 예외 없음 | 본문 없음 | 로그인 목록에서 약을 길게 눌러 삭제 | 미확인 | HTTP 없음 | LOCAL PASS |
| `POST /medications/{id}/end` | 있음 | `status=ENDED`가 아니면 성공으로 보지 않음 | 로그인 목록에서 약을 눌러 종료 | 미확인 | HTTP 없음 | LOCAL PASS |
| `POST /prescriptions/{id}/end` | 있음 | `ended_at`, `ended_medication_count` | 약봉지 종료 버튼 | 미확인 | 파서 테스트 없음 | PARTIAL |
| `GET /prescriptions/{id}/schedule-settings` | 있음 | 있음 | 복용 시각 수정 버튼 | 미확인 | 파서 테스트 없음 | PARTIAL |
| `PUT /prescriptions/{id}/schedule-settings` | 있음 | 조회와 같음 | 등록 직후, 그리고 목록의 시각 수정 | 미확인 | 요청 본문 테스트 통과. `BEDTIME`은 거절 | LOCAL PASS |
| `GET /users/me/agreements` | 기존 `SettingsApi` | 기존 파서 | 등록 전 동의. `MEDICATION`이 이미 동의면 통과 | 미확인 | 서버 404 여부는 모름 | BACKEND PENDING |
| `POST /users/me/agreements` | 기존 `SettingsApi` | 기존 | 미동의일 때만. `terms_version_id`는 `GET /terms`의 `MEDICATION` | 미확인 | 버전 9를 코드에 넣지 않음. 샘플 JSON 테스트만 통과 | BACKEND PENDING |
| `GET /terms` | 기존과 같이 비로그인 호출 | `MEDICATION`만 찾음 | 동의 저장 전에만 | 미확인 | 항목이 없으면 등록하지 않음 | BACKEND PENDING |
| `POST /prescriptions/{id}/compartment` | 기존 `CareApi` | 호출 성공만 사용. 칸 상태를 만들지 않음 | 등록 중 칸을 고른 경우에만. 번호는 화면 인덱스 + 1 | 미확인 | 칸 1 매핑 테스트 통과 | LOCAL PASS |

`POST /users/me/agreements/MEDICATION/withdraw`는 설정 쪽에 이미 있다. 이번 홈 등록 흐름에서는 호출하지 않는다.

## 3) 화면별 구현 현황

| 화면 | 연결된 API | 구현한 기능 | 미구현 | 비고 |
|---|---|---|---|---|
| 홈 | home-summary, ACTIVE medications, interactions/summary, 오늘 doses | 진행률 `done/total`, 슬롯 `NONE/PENDING/DONE/MISSED`, 남은 복약 `PENDING`, 병용금기 `flagged` | 알림 팝업의 별도 "복약 완료" | 그 팝업 화면이 없어 일정 화면의 회차 단위 호출만 연결 |
| 취침 칸 | home-summary의 `slots` | 칸은 유지. 응답에 `BEDTIME`이 없으면 "취침 · 서버 미지원" | 서버 전송 | 저녁에 합치지 않음 |
| 일정 | doses, taken, not-taken | 날짜별 조회, 시각을 보내 기록, 안 먹었어요 | 응답 전 성공 표시는 하지 않음 | 성공 후 홈과 일정을 다시 조회 |
| 기록 | dose-history | 전체·완료(`NORMAL`)·지연·누락, 90일 이내 기간 | 1년 조회 | 명세 최대는 90일 |
| 약물 목록 | prescriptions, medication end, delete, prescription end, schedule get/put | 종료 구분, 약 종료, 길게 눌러 삭제, 약봉지 종료, 시각 수정 | 약 내용 PATCH, 등록 후 칸 변경 | 칸 변경 버튼은 안내만 하고 API를 호출하지 않음 |
| 등록 | agreements, terms, autocomplete, ocr, prescriptions, schedule, compartment | 동의 확인 후 등록. OCR은 초안만 채움. 빈 인식은 오류로 보지 않음 | PRN 전용 입력 칸 | OCR이 `is_prn`이면 시간대 없이 확인 가능. 시간대를 고르면 일반 약으로 바뀜 |
| 그래프·약통·설정 | 변경 없음 | 홈의 병용금기 카드만 그래프 탭으로 이동 | 그래프 상세, 약통 충전 | 이번 범위 밖 |

로그인하지 않은 홈, 목록, 일정, 기록은 기존 로컬 데모다.

## 4) 권한 및 오류 처리

`SELF`는 공유 항목을 미리 막지 않는다. 보호자는 `shared_items`가 응답에 있을 때만 검사한다. 필드가 없으면 서버 응답을 기다린다. 없는 필드를 만들어 권한을 판단하지 않는다.

| 구분 | 처리 |
|---|---|
| SELF | 공유 항목 사전 검사 없음. 쓰기는 허용 |
| 보호자 + `shared_items`에 항목 있음 | 그 항목이 없으면 호출하지 않고 "공유되지 않음" |
| 보호자 + `shared_items` 없음 | 호출하고 서버의 `403`을 표시 |
| READ_ONLY | `writesAllowed`가 거짓. 등록, 기록, 종료, 삭제, 시각 수정을 열지 않음 |
| READ_WRITE | 쓰기 호출 |
| MED_INFO | 약 목록, 등록, OCR |
| DOSE_RECORD | 홈 요약, 일정, 기록, 먹었어요, 안 먹었어요, 복용 시각 |
| DUR_INFO | 홈 병용금기 건수 |
| DEVICE_EVENT | 등록 중 칸 배정 |

화면은 `message`가 아니라 `code`로 기존 `ApiMessages`에 연결한다. 추가한 코드는 `DOSE_NOT_FOUND`, `DOSE_EXPIRED`, `DOSE_NOT_DECIDED`, `DOSE_RECORD_FAILED`, `INVALID_TAKEN_AT`, `INVALID_MEDICATION`, `INVALID_SCHEDULE`, `INVALID_QUERY`, `INVALID_SEARCH_QUERY`, `INVALID_IMAGE`, `MEDICATION_NOT_FOUND`, `MEDICATION_ALREADY_ENDED`, `MEDICATION_NOT_STARTED`, `DRUG_NOT_FOUND`, `OCR_UNAVAILABLE`, `SCHEDULE_CONFLICT`다.

`401 UNAUTHENTICATED`는 기존 refresh 한 번이다. JSON이 아닌 5xx는 기존 `ApiException.http`로 남고 앱을 종료하지 않는다. 동의 API가 `404`이면 등록 화면으로 넘어가지 않는다.

구역마다 실패를 따로 둔다. 병용금기만 실패해도 진행률을 0으로 만들지 않는다. 약 개수 호출이 실패하면 0개로 보이지 않는다.

## 5) 백엔드 확인이 필요한 사항

| 번호 | 관련 API | 확인이 필요한 내용 | 필요한 이유 | 질문 또는 요청 |
|---|---|---|---|---|
| 1 | `GET /patients/{id}/home-summary` | 본문은 슬롯 상태가 `NONE/PENDING/DONE/MISSED`이고 `next_dose.status`는 `PENDING`이다. 예시 JSON은 슬롯 `ACTIVE`, next `NORMAL`이다 | 앱은 본문을 따른다. 예시대로 오면 요약 전체를 잘못된 응답으로 본다 | 예시가 본문과 다른지, 서버가 어떤 값을 보내는지 |
| 2 | 시간대 | `BEDTIME`을 보내면 `400 VALIDATION_ERROR` | 앱은 취침을 보내지도, 저녁에 합치지도 않는다 | 4-2에서 취침이 추가되면 슬롯 이름과 개수를 알려 달라 |
| 3 | `GET/POST /users/me/agreements`, `GET /terms` | `MEDICATION` 동의가 구현됐는지 | 없으면 404일 수 있고, 그때 약 등록을 성공으로 처리하면 안 된다 | 구현 여부와 `terms_type=MEDICATION`의 `terms_version_id`가 `/terms`에 있는지 |
| 4 | `POST /patients/{id}/prescriptions` | `403 CONSENT_REQUIRED`를 실제로 주는지 | 명세에 구현 확인이 필요하다고 되어 있다 | 동의 없이 등록하면 어떤 `code`가 오는지 |
| 5 | `POST /prescriptions/ocr` | Mock인지, `503 OCR_UNAVAILABLE`을 쓰는지 | 앱은 인식 결과를 실제 판독이라고 표시하지 않는다 | 빈 `recognized`와 503의 현재 동작 |
| 6 | 보호자 환자 응답 | `shared_items`가 항상 오는지 | 없으면 앱이 미리 막지 않고 서버에 맡긴다 | 필드 유무와 `MED_INFO`, `DOSE_RECORD`, `DUR_INFO`, `DEVICE_EVENT` 값 |
| 7 | `PATCH /medications/{id}` | 화면에서 쓰지 않는 것이 맞는지 | 명세 4-10은 화면이 이 API를 쓰지 않는다고 한다 | 약 내용 수정 화면이 필요한지 |
| 8 | 처방일 | 화면에 처방일 칸이 없다 | OCR 처방일이 있으면 그 값을 쓰고, 없으면 약 시작일 중 가장 이른 날을 `prescribed_date`로 보낸다 | 이 매핑이 맞는지 |

## 6) 프론트엔드 추가 작업

- 약 내용 `PATCH` 화면은 없다.
- 등록이 끝난 뒤의 칸 변경은 호출하지 않는다. 등록 중에만 `compartment`를 보낸다.
- 알림 팝업의 "복약 완료" 전용 화면은 코드에 없다. 일정 행의 먹었어요가 회차 전체를 한 번 호출한다.
- 취침은 서버가 `BEDTIME`을 받기 전까지 보낼 수 없다. 칸 문구는 남겨 두었다.
- 동의 API는 호출은 붙어 있으나 서버가 없어 구현 여부를 확인하지 못했다. 버전 번호는 고정하지 않았다.
- 에뮬레이터와 실기기에서 화면 반영은 확인하지 못했다.

## 7) 테스트 결과

실행한 명령:

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`
- `curl` `http://127.0.0.1:8080/terms`, `http://localhost:8080/terms`

단위 테스트 155건, 실패 0, 건너뜀 0. 그중 `HomeContractTest` 9건은 홈 파서, null, 0.5정, 빈 OCR, 취침 요청 거절, 기록 기간 90일, 동의 버전 추출이다.

`assembleDebug`는 성공했다.

실제 HTTP는 연결 거부로 하지 못했다. 에뮬레이터와 실기기 확인은 하지 않았다. 성공 JSON을 서버 응답인 것처럼 넣지 않았다.

## 8) 최종 판정

| 대상 | 판정 |
|---|---|
| 홈 요약, 복용 중 개수, 병용금기 건수, 오늘 남은 복약 | LOCAL PASS |
| 먹었어요, 안 먹었어요, 기록 필터 | LOCAL PASS |
| 약물 등록 본문, 복용 시각 저장, 등록 중 칸 배정, OCR 파서 | LOCAL PASS |
| 처방 목록, 처방 종료, 시각 조회, 자동완성 응답 | PARTIAL |
| 약 내용 PATCH 화면 | NOT IMPLEMENTED |
| MEDICATION 동의 API의 서버 구현 | BACKEND PENDING |
| 취침 시간대 서버 저장 | BACKEND PENDING |
| 실제 서버 요청과 화면 반영 | 해당 없음. PASS 없음 |
