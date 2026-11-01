# 약연 인터페이스·API 조사 (상세설계서 7장 사전 자료)

조사일: 2026-10-31  
기준 명세: 약연 서버 API 명세서 v0.9 (기준일 2026-10-29, 개발 Base URL `http://localhost:8080`)  
이번 작업은 코드 수정이 없다. 서버 저장소와 ESP32·MQTT 저장소는 이 작업공간에서 찾지 못했다. 2부의 「구현됨」은 명세 표기이며, 서버 소스를 열어 확인한 결과가 아니다.

실서버 요청은 하지 않았다. 기존 조사 문서(`docs/api-integration-status.md`, `docs/home-api-integration-status.md`, `docs/graph-pillbox-api-integration-status.md`)도 2026-10-29~31에 `127.0.0.1:8080` 연결 거부를 기록한다. 실연동 검증 완료는 0건이다.

판정 용어는 상세설계서 작성 지시의 기준을 따른다.

| 표기 | 뜻 |
| --- | --- |
| 설계 완료 | 명세 v0.9에 경로·본문이 있다. 2부 ✅ 또는 4부 🔜 |
| 코드 구현 | 이 작업공간의 프론트 또는 확인한 웹 저장소에 해당 HTTP 호출 메서드가 있다 |
| 호출 연결 | 그 메서드를 화면 동작이 호출하고, 응답을 화면 상태와 연결한다 |
| 실연동 검증 | 실제 서버 응답으로 화면이 바뀌는 것을 확인했다. 이번 조사에서는 해당 없음 |
| 미구현 | 명세에는 있으나 확인한 코드에 호출이 없다 |
| 검증 불가 | 서버·하드웨어·외부 연계 코드 또는 실행 환경이 없어 구현 여부를 단정하지 못한다 |
| 로컬 처리 | HTTP 없이 메모리, SharedPreferences, localStorage, 데모 데이터로 동작한다 |

로그인하지 않은 Android 홈·그래프·약통은 `MedicationStore`, `InMemoryYakyeonRepository` 데모다. 그 경로를 서버 연동으로 세지 않는다.

## 1. 조사한 저장소와 파일

### 확인한 저장소

| 저장소 | 경로 | 역할 | 확인 범위 |
| --- | --- | --- | --- |
| Android | `/Users/eden/AndroidStudioProjects/yakyeon-frontend-rebuild` | 앱 | API 계층과 `MainActivity` 호출까지 대조 |
| 권한 관리 웹 (서버 연동본) | `/Users/eden/AndroidStudioProjects/yakyeon-consent-web` | QR 이후 가입·열람 동의 | `js/api.js`, `js/app.js`, `js/http.js`, `js/session.js` |
| 권한 관리 웹 (시제품) | `/Users/eden/Desktop/Capstone-FRONT-END-WEB` | 같은 화면의 로컬 시제품 | `js/app.js`. HTTP API 호출 없음 |
| 백엔드 | 없음 | Spring Boot | 명세 v0.9와 기존 docs만 사용. 소스 미확인 |
| ESP32·MQTT | 없음 | 약통 펌웨어 | 설계서 9장·명세 4-9·2-7만 사용. 소스 미확인 |

명세 v0.9 원문은 이 저장소 파일로 있지 않다. 대화에서 받은 명세와, 그 명세를 인용한 `docs/` 조사 문서를 기준으로 했다.

### Android에서 호출 경로를 확인한 파일

| 파일 | 확인한 내용 |
| --- | --- |
| `app/src/main/java/com/example/yakyeon2/api/ApiClient.java` | `HttpURLConnection`. JSON과 multipart. `401 UNAUTHENTICATED`이면 `POST /auth/refresh` 한 번 |
| `app/src/main/java/com/example/yakyeon2/api/ApiConfig.java` | `BuildConfig.API_BASE_URL` |
| `app/build.gradle.kts` | 기본값 `http://localhost:8080`. 변경은 `-Pyakyeon.apiBaseUrl` |
| `app/src/main/java/com/example/yakyeon2/api/AuthApi.java` | 가입, 로그인, 로그아웃, 비밀번호, 탈퇴, 푸시 토큰 메서드 |
| `app/src/main/java/com/example/yakyeon2/api/UserApi.java` | 프로필, 알림 설정 |
| `app/src/main/java/com/example/yakyeon2/api/SettingsApi.java` | 연동, 약관 동의, 약통 설정, 공유 요약 |
| `app/src/main/java/com/example/yakyeon2/api/HomeApi.java` | 홈, 처방, OCR, 일정, 복용 기록, 등록 중 칸 배정 |
| `app/src/main/java/com/example/yakyeon2/api/CareApi.java` | 병용금기, 약통 조회, 등록·충전·해제 메서드 |
| `app/src/main/java/com/example/yakyeon2/MainActivity.java` | 위 메서드 중 화면이 실제로 호출하는 위치 |
| `app/src/main/java/com/example/yakyeon2/data/AuthTokenStore.java` | 토큰을 `SharedPreferences`(`yakyeon_auth`)에 저장 |

Retrofit 인터페이스는 없다. 요청·응답 DTO 클래스는 없고, `AuthContracts`, `HomeContracts`, `CareContracts`, `SettingsContracts`가 `org.json`으로 읽고 쓴다.

### 웹에서 확인한 파일

| 파일 | 확인한 내용 |
| --- | --- |
| `yakyeon-consent-web/js/config.js` | `API_BASE_URL = "http://127.0.0.1:8080"` |
| `yakyeon-consent-web/js/http.js` | `fetch`. Bearer. 401이면 refresh 후 한 번 재시도 |
| `yakyeon-consent-web/js/session.js` | 토큰은 모듈 메모리. `localStorage` 없음 |
| `yakyeon-consent-web/js/api.js` | 가입·로그인·grant·shared-items·해제·목록 |
| `yakyeon-consent-web/js/app.js` | 위 함수를 화면 버튼에서 호출 |
| `Capstone-FRONT-END-WEB/js/app.js` | `localStorage` 키 `yakyeon.patient.accounts`, `yakyeon.patient.session`. `fetch` 없음 |

## 2. 개수

| 항목 | 개수 |
| --- | --- |
| 명세에 경로가 있는 API | 73 (2부 64, 4부 신규 9) |
| 명세가 만들지 않기로 한 기능 | 1 (프로필 휴대폰·생년월일 변경) |
| 명세가 삭제했다고 한 API | 공유 이력 `share-logs` 2개. 목록에 넣지 않음 |
| Android 화면 호출 연결 | 46 |
| Android 메서드만 있고 화면 미연결 | 7 |
| Android 호출 코드 없음 | 위 53개를 뺀 나머지. 하드웨어 2개와 웹 전용·옛 연동 포함 |
| 웹(`yakyeon-consent-web`) 화면 호출 연결 | 13 (`POST /auth/refresh`는 `http.js`가 401에서 호출) |
| 시제품 웹 HTTP 호출 | 0 |
| 실연동 검증 | 0 |
| MQTT·ESP32·DUR 수집·FCM 발송 코드 | 검증 불가 |

## 3. 전체 API 목록과 호출 대조

상태 열의 「명세」는 v0.9 표시다. 「Android」「Web」은 이번 코드 대조다. 실연동 열은 전부 검증 불가라 표에서 반복하지 않는다.

관련 DB는 상세설계서 8장 표 이름과의 대응 후보이며, 서버 코드로 확인하지 않았다. 4장과 8장의 테이블 이름이 다른 항목은 7절에 둔다.

### 3-1. 인증·계정 (명세 2-1)

| 기능 | Method | Endpoint | 인증 | 명세 | Android | Web | 관련 요구사항 | 관련 DB 후보 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 회원가입 | POST | `/auth/signup` | 없음 | ✅ 설계 완료 | 호출 연결 `AuthApi.signup` ← `MainActivity` | 호출 연결 `signup` ← `app.js` | FR-AUTH-001, FR-AUTH-003 | `app_user`, `patient`, `user_agreement`, `agreement_log` |
| 인증번호 발송 | POST | `/auth/phone-verifications` | 없음 | ✅ | 호출 연결 `AuthApi.sendPhoneCode` | 호출 연결 `requestPhoneVerification` | FR-AUTH-001 | `phone_verification` |
| 인증번호 확인 | POST | `/auth/phone-verifications/{verificationId}/confirm` | 없음 | ✅ | 호출 연결 `AuthApi.confirmPhone` | 호출 연결 `confirmPhoneVerification` | FR-AUTH-001 | `phone_verification` |
| 아이디 중복 | GET | `/auth/check-id` | 없음 | ✅ | 호출 연결 `AuthApi.checkLoginId` | 호출 연결 `checkLoginId` | FR-AUTH-001 | `app_user` |
| 로그인 | POST | `/auth/login` | 없음 | ✅ | 호출 연결 `AuthApi.login` | 호출 연결 `login`. `account_type`이 `PATIENT`가 아니면 세션을 지우고 `PATIENT_ONLY` | FR-AUTH-002 | `app_user`, `auth_token` |
| 토큰 갱신 | POST | `/auth/refresh` | 없음. 본문에 refresh | ✅ | 호출 연결. 앱 시작과 401 재시도 `ApiClient.refreshSession` | 호출 연결. `http.js` `runRefresh` | FR-AUTH-002 | `auth_token` |
| 로그아웃 | POST | `/auth/logout` | 없음. 본문에 refresh | ✅ | 호출 연결 `AuthApi.logout`. refresh가 없으면 서버를 호출하지 않고 로컬 세션만 삭제 | 호출 연결 `logout`. refresh가 없으면 메모리만 삭제 | FR-AUTH-002 | `auth_token` |
| 비밀번호 변경 | PATCH | `/auth/password` | Bearer | ✅ | 호출 연결 `AuthApi.changePassword` | 호출 없음 | FR-MY-001 | `app_user`, `auth_token` |
| 회원 탈퇴 | DELETE | `/auth/account` | Bearer | ✅ | 호출 연결 `AuthApi.deleteAccount` | 호출 없음 | FR-MY-002 | `withdraw_user` 설계 함수. 테이블은 8장 계정 영역 |
| 동의 이력 | GET | `/users/me/consents` | Bearer | ✅ | 미구현. 화면 없음 | 호출 없음 | FR-AUTH-005 | `consent_log` |
| 프로필 조회 | GET | `/users/me/profile` | Bearer | ✅ | 호출 연결 `UserApi.profile` | 호출 없음 | FR-MY-001 | `app_user` |
| 이름 수정 | PATCH | `/users/me/profile` | Bearer | ✅ | 호출 연결 `UserApi.updateName`. `name`만 | 호출 없음 | FR-MY-001 | `app_user`, `patient` |
| 약관 목록 | GET | `/terms` | 없음 | ✅ | 호출 연결 `AuthApi.terms`, `HomeApi.medicationTermsVersion` | 호출 연결 `getTerms` | FR-AUTH-003 | `terms_version` |

푸시 토큰은 알림 절에 둔다.

### 3-2. 보호자 연동

| 기능 | Method | Endpoint | 인증 | 명세 | Android | Web | 관련 요구사항 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 복약자 검색 | GET | `/patients/search` | Bearer | ✅ 옛 방식. 앱 화면에서 쓰지 말 것 | 호출 없음 | 호출 없음 | 설계 4-3의 문자 연동. QR 흐름에서는 사용하지 않음 | 관련 DB 후보 `patient` |
| 연동 요청 | POST | `/patients/{patientId}/guardian-links` | Bearer | ✅ 옛 방식 | 호출 없음 | 호출 없음 | 위와 같음 | `guardian_link`, `link_invite` |
| 위임 인증 발송 | POST | `/guardian-links/{linkId}/verification` | Bearer | ✅ 옛 방식 | 호출 없음 | 호출 없음 | 위와 같음 | `phone_verification` |
| 위임 인증 확인 | POST | `/verifications/{verificationId}/confirm` | Bearer | ✅ 옛 방식 | 호출 없음 | 호출 없음 | 위와 같음 | 가입용 confirm과 경로가 다름 |
| 위임 동의 | POST | `/guardian-links/{linkId}/consent` | Bearer | ✅ 옛 방식. 성공 시 기본 `READ_ONLY` | 호출 없음 | 호출 없음 | 위와 같음 | v0.9 QR 동의는 `READ_WRITE` |
| 연동 해제 | DELETE | `/guardian-links/{linkId}` | Bearer | ✅ | 호출 연결 `SettingsApi.unlink` | 호출 연결 `unlink` | FR-AUTH-005 | |
| 권한 변경 | PATCH | `/guardian-links/{linkId}/permission` | Bearer | ✅ | 미구현. 화면 없음 | 호출 없음 | FR-AUTH-005 | 웹에서 낮추는 화면은 명세상 필수 아님 |
| 보호자 목록 | GET | `/patients/{patientId}/guardian-links` | Bearer. 복약자 본인 | ✅ | 호출 연결 `SettingsApi.guardianLinks` | 호출 연결 `listGuardianLinks` | FR-AUTH-005 | 4-1 추가 필드 `relationship`, `shared_items`는 🔜 |
| 관리 복약자 | GET | `/users/me/managed-patients` | Bearer | ✅ | 호출 연결 `SettingsApi.managedPatients`. 0명·여러 명이면 그래프·약통·공유에서 한 명을 고르지 않음 | 호출 연결 `listManagedPatients` | FR-MULTI-001 | 선택 화면은 없음 |
| 복약자 등록 | POST | `/patients` | Bearer | ✅. 보호자 직접 등록은 옛 방식 | 호출 없음 | 호출 없음 | 설계에 이 화면 없음 | |
| 복약자 조회 | GET | `/patients/{patientId}` | Bearer | ✅ | 호출 없음 | 호출 없음 | FR-AUTH-005 | 보호자 열람은 🔜 `PERSONAL_INFO` |
| 철회 인증 요청 | POST | `/revocations/request` | 없음 | ✅ 옛 방식 | 호출 없음 | 호출 없음 | 설계 4-4 | 응답이 가입 여부를 숨김 |
| 철회 확인 | POST | `/revocations/confirm` | 없음 | ✅ 옛 방식 | 호출 없음 | 호출 없음 | 설계 4-4 | |
| 열람 허용 | POST | `/guardian-links/grant` | Bearer. `PATIENT` | 🔜 4-1 | 호출 없음. 앱은 QR에 웹 주소만 표시 `SettingsScreens` | 호출 연결 `grantLink` | FR-AUTH-004 | 서버에 없으면 404. 실연동 검증 불가 |
| 열람 범위 변경 | PATCH | `/guardian-links/{linkId}/shared-items` | Bearer. 복약자 | 🔜 4-1 | 호출 없음 | 호출 연결 `updateSharedItems` | FR-AUTH-005 | |

관련 DB 후보: `guardian_link`, `link_invite`, `consent_log`, `patient`. 서버 코드 미확인.

### 3-3. 약물·처방 (명세 2-3)

| 기능 | Method | Endpoint | 인증 | 명세 | Android | Web | 관련 요구사항 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 약 목록 | GET | `/patients/{patientId}/medications` | Bearer | ✅ | 호출 연결 `HomeApi.medications`. 홈 개수는 `status=ACTIVE` | 해당 없음 | FR-LIST-001, FR-HOME-001 |
| 약 수정 | PATCH | `/medications/{medicationId}` | Bearer | ✅. 4-10은 화면에서 쓰지 않음 | 메서드만 `HomeApi.updateMedication`. `MainActivity` 호출 없음 | 해당 없음 | FR-LIST-002 |
| 약 삭제 | DELETE | `/medications/{medicationId}` | Bearer | ✅ | 호출 연결 `HomeApi.deleteMedication` | 해당 없음 | FR-LIST-002 |
| 약 종료 | POST | `/medications/{medicationId}/end` | Bearer | ✅ | 호출 연결 `HomeApi.endMedication` | 해당 없음 | FR-LIST-002 |
| 처방 종료 | POST | `/prescriptions/{prescriptionId}/end` | Bearer | ✅ | 호출 연결 `HomeApi.endPrescription` | 해당 없음 | FR-LIST-002 |
| 자동완성 | GET | `/medications/autocomplete` | Bearer | ✅ | 호출 연결 `HomeApi.autocomplete`. 2글자 미만·50자 초과는 호출하지 않음 | 해당 없음 | FR-ADD-002 |
| 처방 등록 | POST | `/patients/{patientId}/prescriptions` | Bearer | ✅ | 호출 연결 `HomeApi.createPrescription`. `BEDTIME`이 있으면 호출하지 않음 | 해당 없음 | FR-ADD-001, FR-ADD-002, FR-DUR-001 |
| OCR | POST | `/prescriptions/ocr` | Bearer | ✅ Mock | 호출 연결 `HomeApi.ocr`. multipart `image` | 해당 없음 | FR-ADD-001 |
| 처방 목록 | GET | `/patients/{patientId}/prescriptions` | Bearer | ✅ | 호출 연결 `HomeApi.prescriptions`. `groupByHospital`는 보내지 않음 | 해당 없음 | FR-LIST-001 |

관련 DB 후보: `prescription`, `patient_medication`, `drug`, `medication_timing`. OCR 원본 이미지는 명세가 저장하지 않는다고 한다.

### 3-4. 병용금기 (명세 2-4)

| 기능 | Method | Endpoint | 인증 | 명세 | Android | 관련 요구사항 |
| --- | --- | --- | --- | --- | --- | --- |
| 등재 목록 | GET | `/patients/{patientId}/interactions` | Bearer | ✅ | 호출 연결 `CareApi.interactions` | FR-DUR-001, FR-GRAPH-001 |
| 요약 | GET | `/patients/{patientId}/interactions/summary` | Bearer | ✅ | 호출 연결 `CareApi.summary`. 홈과 그래프 | FR-DUR-001, FR-HOME-001 |
| 그래프 | GET | `/patients/{patientId}/interaction-graph` | Bearer | ✅ | 호출 연결 `CareApi.graph` | FR-GRAPH-001 |
| 엣지 상세 | GET | `/interaction-edges/{durSeq}` | Bearer | ✅ | 호출 연결 `CareApi.edge`. 경로 값은 문자열 그대로 | FR-GRAPH-002 |

웹은 호출하지 않는다. 보호자 `DUR_INFO` 없으면 `403 SHARE_NOT_GRANTED`는 4-1 예정이다. 관련 DB 후보: `dur_taboo_rule`, `dur_taboo_item`, `interaction_check`, `interaction_finding`.

### 3-5. 복용 일정·기록 (명세 2-5)

| 기능 | Method | Endpoint | 인증 | 명세 | Android | 관련 요구사항 |
| --- | --- | --- | --- | --- | --- | --- |
| 일정 | GET | `/patients/{patientId}/doses` | Bearer | ✅ | 호출 연결 `HomeApi.doses` | FR-SCHEDULE-001, FR-HOME-001 |
| 기록 | GET | `/patients/{patientId}/dose-history` | Bearer | ✅ | 호출 연결 `HomeApi.doseHistory`. 앱은 90일을 넘기면 요청하지 않음 | FR-ALARM-001, FR-PILLBOX-006 |
| 홈 요약 | GET | `/patients/{patientId}/home-summary` | Bearer | ✅. 슬롯 3개. `BEDTIME`은 🔜 4-2 | 호출 연결 `HomeApi.homeSummary` | FR-HOME-001 |
| 먹었어요 | POST | `/doses/{doseId}/taken` | Bearer | ✅ | 호출 연결 `HomeApi.markTaken`. 일정 화면. 알림 팝업 전용 화면은 없음 | FR-SCHEDULE-001, FR-PILLBOX-004 |
| 안 먹었어요 | POST | `/doses/{doseId}/not-taken` | Bearer | ✅ | 호출 연결 `HomeApi.markNotTaken` | FR-SCHEDULE-001 |
| 시각 조회 | GET | `/prescriptions/{prescriptionId}/schedule-settings` | Bearer | ✅ | 호출 연결 `HomeApi.schedule` | FR-SCHEDULE-002 |
| 시각 저장 | PUT | `/prescriptions/{prescriptionId}/schedule-settings` | Bearer | ✅. 지금 규칙은 아침·점심·저녁, 간격 4시간, 최댓값 20:59 | 호출 연결 `HomeApi.saveSchedule` | FR-SCHEDULE-002 |

웹은 호출하지 않는다. 예정은 서버가 7일치 생성. 클라이언트가 예정을 만들지 않는다. 관련 DB 후보: `schedule_setting`, `scheduled_dose`, `dose_correction_log`.

### 3-6. 약통, 앱이 호출 (명세 2-6, 4-11)

| 기능 | Method | Endpoint | 인증 | 명세 | Android | 관련 요구사항 |
| --- | --- | --- | --- | --- | --- | --- |
| 칸 배정 | POST | `/prescriptions/{prescriptionId}/compartment` | Bearer | ✅ | 호출 연결. 등록 마지막에서만 `HomeApi.assignCompartment` → `CareApi.assignCompartment`. 등록 후 칸 변경 화면은 호출하지 않음 | FR-PILLBOX-003 |
| 충전 | POST | `/compartment-mappings/{mappingId}/load` | Bearer | ✅ | 메서드만 `CareApi.loadCompartment` | FR-PILLBOX-003 |
| 추가 충전 | POST | `/compartment-mappings/{mappingId}/refill` | Bearer | ✅ | 메서드만 `CareApi.refillCompartment` | 설계 3장에 별도 ID 없음. 약통 잔량 |
| 배정 해제 | DELETE | `/compartment-mappings/{mappingId}` | Bearer | ✅ | 메서드만 `CareApi.releaseCompartment` | FR-PILLBOX-003 |
| 약통 등록 | POST | `/patients/{patientId}/device` | Bearer | ✅. 동의 없으면 🔜 `403 CONSENT_REQUIRED` | 메서드만 `CareApi.registerDevice`. 화면은 예시 기기 `TRX-0328`로 호출하지 않음. QR 형식은 명세에 없음 | FR-PILLBOX-002 |
| 약통 조회 | GET | `/patients/{patientId}/device` | Bearer | ✅. `buzzer_enabled` 등은 🔜 4-11 추가 필드 | 호출 연결 `SettingsApi.device` | FR-PILLBOX-001, FR-PILLBOX-002 |
| 약통 해제 | DELETE | `/patients/{patientId}/device` | Bearer | ✅ | 호출 연결 `SettingsApi.disconnectDevice` | FR-PILLBOX-002 |
| 약통 화면 | GET | `/patients/{patientId}/pillbox` | Bearer | ✅. `attention`은 🔜 | 호출 연결 `CareApi.pillbox`. `OPENED`일 때만 열림 문구 | FR-PILLBOX-001, FR-PILLBOX-005 |
| 부저·LED | PATCH | `/patients/{patientId}/device/settings` | Bearer | 🔜 4-11 | 호출 연결 `SettingsApi.updateDevice` | FR-PILLBOX-002 |
| 일정 동기화 요청 | POST | `/patients/{patientId}/device/sync` | Bearer | 🔜 4-11 | 호출 연결 `SettingsApi.syncDevice`. 응답 버전은 요청 완료. 기기 반영 완료로 보지 않음 | FR-PILLBOX-002, FR-SCHEDULE-002 |

BLE 검색, Wi-Fi 비밀번호 전달 코드는 Android에 없다. 관련 DB 후보: `device`, `compartment_mapping`.

### 3-7. 약통, 하드웨어가 호출 (명세 2-7, 4-9)

프론트는 호출하지 않는다. 펌웨어 저장소는 없다.

| 기능 | Method | Endpoint | 인증 | 명세 | 코드 | 관련 요구사항 |
| --- | --- | --- | --- | --- | --- | --- |
| 생존 신호 | POST | `/device/heartbeat` | `X-Device-Serial`, `X-Device-Secret` | ✅ | 검증 불가 | FR-PILLBOX-002, FR-PILLBOX-004 |
| 개방·버튼 | POST | `/device/signals` | 같은 헤더 | ✅ | 검증 불가 | FR-PILLBOX-004, FR-PILLBOX-005 |
| 일정 수신 | GET | `/device/schedule` | 같은 헤더 | 🔜 4-9 | 검증 불가 | FR-SCHEDULE-002, FR-PILLBOX-004 |

관련 DB 후보: `device_signal`, `intake_event`, `device_compartment_state`, `device_status_report`, `scheduled_dose`.

### 3-8. 알림 (명세 2-8, 4-6, 4-7)

| 기능 | Method | Endpoint | 인증 | 명세 | Android | 관련 요구사항 |
| --- | --- | --- | --- | --- | --- | --- |
| 알림 목록 | GET | `/users/me/notifications` | Bearer | ✅ | 미구현. 경로 문자열 없음 | FR-ALARM-001은 복용 내역이며 이 목록과 다름. 알림함은 FR-ALARM-002 |
| 안 읽은 수 | GET | `/users/me/notifications/unread-count` | Bearer | ✅ | 미구현 | FR-ALARM-002 |
| 읽음 | POST | `/notifications/{notificationId}/read` | Bearer | ✅ | 미구현 | FR-ALARM-002 |
| 모두 읽음 | POST | `/users/me/notifications/read-all` | Bearer | ✅ | 미구현 | 명세에만 있음 |
| 설정 조회 | GET | `/users/me/notification-settings` | Bearer | ✅ | 호출 연결 `UserApi.notificationSettings`. 8개가 아니면 오류 | FR-ALARM-003 |
| 설정 변경 | PATCH | `/users/me/notification-settings` | Bearer | ✅ | 호출 연결 `UserApi.updateNotificationSettings`. `DEVICE`를 끄면 보호자 4개도 같은 요청에 `false` | FR-ALARM-003 |
| 푸시 토큰 등록 | PUT | `/users/me/push-token` | Bearer | ✅ | 메서드만 `AuthApi.registerPushToken`. FCM 토큰을 만들지 않아 화면이 호출하지 않음 | FR-ALARM-002 |
| 푸시 토큰 삭제 | DELETE | `/users/me/push-token` | Bearer | ✅ | 메서드만 `AuthApi.deletePushToken` | FR-AUTH-002, FR-ALARM-002 |

Android 알림 채널 `INTERACTION`, `INTAKE`, `DEVICE`, `STOCK` 생성 코드는 확인하지 못했다. Firebase 의존성 호출도 확인하지 못했다. FCM 발송은 서버 측이며 명세는 Mock까지이고 실기기 발송은 🔜이다. 검증 불가.

관련 DB 후보: `notification`, `notification_delivery`, `notification_setting`, `push_token`.

### 3-9. 동의 철회·공유 (명세 4-5, 4-12)

| 기능 | Method | Endpoint | 인증 | 명세 | Android | 관련 요구사항 |
| --- | --- | --- | --- | --- | --- | --- |
| 동의 상태 | GET | `/users/me/agreements` | Bearer | 🔜 | 호출 연결 `SettingsApi.agreements`. 약 등록 전과 약통 동의, 설정 화면 | FR-AUTH-003, FR-AUTH-006 |
| 동의 저장 | POST | `/users/me/agreements` | Bearer | 🔜 | 호출 연결 `SettingsApi.agree`. 버전 번호는 `/terms` 응답 값. 9를 코드에 넣지 않음 | FR-AUTH-003 |
| 동의 철회 | POST | `/users/me/agreements/{termsType}/withdraw` | Bearer | 🔜 | 호출 연결 `SettingsApi.withdraw`. `SERVICE_NOTICE`는 호출하지 않음. 약물 카드 버튼 둘은 `MEDICATION` | FR-AUTH-006 |
| 공유 요약 | GET | `/patients/{patientId}/share-summary` | Bearer | 🔜 | 호출 연결 `SettingsApi.shareSummary`. PDF는 `ShareSummaryExporter`가 앱에서 생성. 서버 PNG API는 없음 | FR-SHARE-001 |

웹은 이 네 API를 호출하지 않는다. 관련 DB 후보: `user_agreement`, `agreement_log`, `terms_version`. 공유 이력 테이블은 명세가 만들지 않는다고 확정했다. 접근 기록은 설계서의 `access_audit_log` 후보만 남는다.

### 3-10. 제공하지 않음

| 기능 | 명세 | 코드 |
| --- | --- | --- |
| 휴대폰 번호 변경 | ❌ 4-8 | 호출 없음. 프로필은 읽기 전용 표시 |
| 생년월일 변경 | ❌ 4-8 | 호출 없음 |
| 서버 PNG 생성 | 4-5에서 삭제. PDF는 앱 | 서버 export API 없음 |
| 공유 이력 목록 | 4-5에서 삭제 | 호출 없음 |
| QR 발급 API | 4-1. QR은 웹 주소만. 서버 API 없음 | 앱은 고정 이미지 `R.drawable.care_qr_code`와 웹 열기 |

## 4. MQTT와 외부 연계

| 연계 | 설계서 | 명세 v0.9 | 확인한 코드 | 판정 |
| --- | --- | --- | --- | --- |
| Android ↔ Spring Boot | REST | REST JSON, 개발 `http://localhost:8080` | `ApiClient` | 호출 연결. 실연동 검증 불가 |
| 웹 ↔ Spring Boot | REST | 4-1 CORS `https://inhauniv-capstone-design.github.io` | consent-web은 `http://127.0.0.1:8080`로 `fetch` | 호출 연결. 실연동 검증 불가. 시제품 웹은 localStorage |
| Spring Boot ↔ PostgreSQL | SQL | 명세는 테이블을 API마다 적지 않음 | 서버 소스 없음 | 검증 불가 |
| DUR 공공데이터 | 서버 주기 수집 | 앱은 결과 조회 API만 호출. 수집 API는 명세에 없음 | 서버 소스 없음 | 검증 불가. 앱의 조회는 호출 연결 |
| OCR | 외부 엔진, 원본 파기 | 현재 Mock. 실연동 시 빈 배열 또는 `503 OCR_UNAVAILABLE` | 앱은 multipart 전송 | 호출 연결. 엔진 연동은 검증 불가 |
| FCM | 서버 발송 | 토큰 등록 API는 ✅. 실발송은 🔜. 푸시 본문은 title, body, `data.notification_id`만 | 앱은 토큰 메서드만. 채널 미확인 | 검증 불가 |
| ESP32 ↔ 브로커 | MQTT `yakyeon/{serial}/...`, ack, LWT | 4-9는 기기 통신을 HTTPS로 확정. MQTT 변경은 하드웨어 입고 후 별도 명세 | 펌웨어 없음 | 검증 불가. 설계와 명세가 다름 |
| BLE·Wi-Fi 프로비저닝 | 최초 등록 시 BLE로 Wi-Fi 전달 | 등록 본문은 라벨 QR의 `serial_no`, `secret`. BLE는 찾기·연결 표시용이라고만 함. Wi-Fi 비밀번호 필드 없음 | Android에 Bluetooth API 호출 없음 | 미구현. QR 원문 형식은 명세에 없음 |
| 문자 인증 | 휴대폰 인증 | 개발 환경은 콘솔 `[MOCK SMS]` | 앱·웹은 발송 API를 호출. 실제 문자 수신은 검증 불가 | 호출 연결. 외부 연동 검증 불가 |

설계서 9.3의 MQTT 메시지(`event`, `ack`, `schedule`, `synced`, `config`, `state`, `status`, `online`)에 대응하는 펌웨어·브로커 설정 파일은 찾지 못했다. 명세가 이미 받은 기기 API는 `POST /device/heartbeat`, `POST /device/signals`이고, 일정은 `GET /device/schedule`이다. ack 큐, 이벤트 ID 중복 제거, 로컬 재전송은 서버·펌웨어 코드 없이 확인하지 못했다.

## 5. 요구사항 ID와 API

설계서 3장 ID를 기준으로 한다. API가 없으면 「추가 API 설계 필요」 또는 「명세에 대체 경로」로 적는다.

| 요구사항 | 연결 API | 빈 곳 |
| --- | --- | --- |
| FR-AUTH-001 | signup, check-id, phone-verifications, confirm | 아이디·비밀번호 규칙이 화면(6~20, 구성 문자)과 현재 서버(4~30, 8~64)가 다름. 맞추는 변경은 🔜 4-4 |
| FR-AUTH-002 | login, refresh, logout | 반복 실패 잠금 횟수·시간은 설계서도 「확정 필요」. 명세 오류는 `INVALID_CREDENTIALS`만. 잠금 API 필드 없음. 토큰 저장은 설계의 Keystore가 아니라 SharedPreferences |
| FR-AUTH-003 | `/terms`, agreements 3개 | agreements는 🔜. 약물·약통 동의 종류 `MEDICATION`, `DEVICE` |
| FR-AUTH-004 | `POST /guardian-links/grant` 🔜. QR 발급 API 없음 | 설계서의 「QR 발급 API」는 명세에 없다. 웹 주소만 |
| FR-AUTH-005 | DELETE link, GET guardian-links, PATCH permission, PATCH shared-items | permission 화면 호출 없음. shared-items는 웹만 |
| FR-AUTH-006 | agreements withdraw | 🔜 |
| FR-AUTH-007 온보딩 | 없음 | 설계서도 MVP 화면 없음. 추가 API 불필요 |
| FR-HOME-001 | home-summary, medications, interactions/summary, doses | 취침 슬롯은 🔜 |
| FR-ALARM-001 | dose-history | 알림 목록 API와 다른 화면 |
| FR-ALARM-002 | notification-settings, push-token, 목록 API | 목록·읽음·FCM 수신은 앱 미연결. 정각 푸시는 명세가 보내지 않음. 약통 LED·부저 |
| FR-ALARM-003 | notification-settings | 호출 연결. 실발송 검증 불가 |
| FR-LIST-001 | prescriptions, medications | 호출 연결 |
| FR-LIST-002 | delete, end, prescription end | PATCH는 메서드만. 명세 4-10과 맞음 |
| FR-ADD-001 | ocr, prescriptions | OCR은 Mock |
| FR-ADD-002 | autocomplete, prescriptions | 호출 연결 |
| FR-SCHEDULE-001 | doses, taken, not-taken | 회차 모호 선택 전용 API는 없음. 클라이언트가 `dose_id`를 정해 `taken`을 호출 |
| FR-SCHEDULE-002 | schedule-settings | 약통 반영은 sync 요청과 `GET /device/schedule`. MQTT schedule 토픽은 코드 없음 |
| FR-DUR-001 | interactions, summary. 등록 시 서버가 자동 검사 | 수집 배치 API는 명세에 없음. 서버 내부 작업으로 봄 |
| FR-GRAPH-001 | interaction-graph, interactions | 호출 연결 |
| FR-GRAPH-002 | interaction-edges | `dur_seq` 타입 불일치. 7절 |
| FR-PILLBOX-001 | pillbox, device | 호출 연결. `attention`은 예정 필드 |
| FR-PILLBOX-002 | device POST는 메서드만. settings, sync, delete는 호출 연결 | BLE·QR 파싱 미구현. QR 형식 추가 설계 필요 |
| FR-PILLBOX-003 | compartment는 등록 중만 호출. load·refill·해제는 메서드만 | 충전 화면 없음 |
| FR-PILLBOX-004 | signals는 하드웨어. 앱 수동은 taken | 판정은 서버. 펌웨어 검증 불가 |
| FR-PILLBOX-005 | signals `IR_TRIGGER`, pillbox `attention` | 개방만으로 복용 완료가 아닌 규칙은 명세와 같음. 실기기 검증 불가 |
| FR-PILLBOX-006 | dose-history, taken으로 당일 정정 | `corrected` 필드를 일정 응답에서 읽음 |
| FR-SHARE-001 | share-summary | 설계는 PNG, 명세는 앱 PDF. 서버 이미지 API 없음 |
| FR-MY-001 | profile, password | 휴대폰·생년월일 변경 API는 ❌ |
| FR-MY-002 | DELETE /auth/account | 호출 연결. 로컬 약 기록 삭제 여부는 명세에 없음 |
| FR-MY-003 | terms, agreements | 호출 연결 |
| FR-MULTI-001 | managed-patients | 목록은 호출. 여러 명 전환 화면은 없음. 설계도 MVP는 1명 |

## 6. API별 구현·연동·검증 요약

| 구분 | 설계 완료 | 프론트 코드 구현 | 화면 호출 연결 | 실연동 검증 |
| --- | --- | --- | --- | --- |
| 인증·가입 (2-1, 푸시 제외) | 13 | 12 (`consents` 제외) | 12 | 0 |
| 옛 보호자 연동·철회 | 9 | 0 | 0 | 0 |
| 유지할 연동 조회·해제·권한 | 4 | 3 (`permission` 제외) | 3 | 0 |
| QR grant, shared-items | 2 🔜 | 웹만 | 웹만 | 0 |
| 약물·처방 | 9 | 9 | 8 (`PATCH` 제외) | 0 |
| 병용금기 | 4 | 4 | 4 | 0 |
| 일정·기록 | 7 | 7 | 7 | 0 |
| 약통 앱 API (2-6) | 8 | 8 | 4 (pillbox, device GET/DELETE, compartment). load, refill, release, register는 화면 없음 | 0 |
| 약통 설정 🔜 | 2 | 2 | 2 | 0 |
| 하드웨어 | 3 | 0 | 0 | 검증 불가 |
| 알림 목록·읽음 | 4 | 0 | 0 | 0 |
| 알림 설정 | 2 | 2 | 2 | 0 |
| 푸시 토큰 | 2 | 2 | 0 | 0 |
| 약관 동의 🔜 | 3 | 3 | 3 | 0 |
| 공유 요약 🔜 | 1 | 1 | 1 | 0 |

## 7. 명세와 코드, 설계서의 불일치

1. 토큰 저장. 설계 4-2는 Android Keystore다. 코드는 `AuthTokenStore`가 `SharedPreferences` 파일 `yakyeon_auth`에 access, refresh를 넣는다.
2. 통신 방식. 설계 9.3은 MQTT다. 명세 4-9는 기기 통신을 HTTPS로 확정하고 MQTT는 후속이다. 펌웨어는 없어 어느 쪽이 구현됐는지 검증 불가.
3. 약통 등록. 설계는 BLE 검색 후 Wi-Fi 전달이다. 명세는 라벨 QR의 시리얼·비밀값이다. 앱은 BLE도 QR 파싱도 없고, 등록 POST를 화면에서 호출하지 않는다.
4. 공유 산출물. 설계 FR-SHARE-001은 서버 PNG다. 명세 4-5는 `GET .../share-summary`를 앱이 PDF로 만든다. 앱에 `ShareSummaryExporter`가 있다. 서버 PNG API는 없다.
5. 연동 권한. 설계 4-4는 웹에서 「조회만 / 등록·수정」을 고른다. v0.9는 필수 3개 동의하면 `READ_WRITE`이고, 권한 화면은 필수가 아니다. 옛 `POST .../consent` 성공 기본값은 `READ_ONLY`라 두 계약이 공존한다.
6. QR. 설계는 서버가 QR을 발급한다. 명세는 웹 주소만 담고 발급 API가 없다. 앱 이미지는 리소스 `care_qr_code`다.
7. 아이디·비밀번호. 화면과 🔜 4-4는 아이디 6~20 영문·숫자, 비밀번호 영문·숫자·특수문자 8자 이상. 2-1-1 현재 서버는 아이디 4~30, `#` 시작 금지, 비밀번호 8~64이고 구성 규칙이 없다. 앱 `AuthValidation`은 화면 규칙과 비밀번호 상한 64자를 쓴다(`docs/api-integration-status.md`).
8. 취침. 앱 칸은 4개다. 서버는 `MORNING`, `LUNCH`, `DINNER`만 받는다. `BEDTIME`은 앱이 보내지 않는다.
9. `dur_seq`. 목록·그래프 응답 타입은 string, 상세 경로 변수 표기는 number. 앱은 문자열을 경로에 넣는다.
10. 홈 요약 예시. 본문은 슬롯 `NONE/PENDING/DONE/MISSED`, `next_dose.status`는 `PENDING`. 예시 JSON은 슬롯 `ACTIVE`, next `NORMAL`. 앱은 본문을 따른다.
11. 칸 배정·충전 예시. 세 API 예시가 같은 JSON이고 `valid_to`가 `valid_from`보다 앞선다. 본문과 예시가 다르다. `docs/backend-review-requests-graph-pillbox.md`.
12. 그래프 `groups` 예시가 `[null]`이다. 타입은 약 번호 배열의 배열. 앱은 null 묶음을 잘못된 응답으로 본다.
13. 알림 조회 본문은 설정 8개를 항상 준다고 하고 예시는 1개다. 앱은 8개가 아니면 오류로 본다.
14. 설계의 복용 내역과 명세의 알림함. 앱의 복용 내역은 `dose-history`다. `GET /users/me/notifications`는 호출하지 않는다.
15. 정각 알림. 설계 FR-ALARM-002와 v0.9 4-7은 앱 푸시에 정각이 없다. 정각은 약통이다. 설계 일부 문장과 맞춰 두되, 앱에 정각 푸시 호출을 넣지 않는다.
16. 테이블 이름. 4장은 `care_link`, `pillbox`, `dose_event`, `consent_text_version`을 쓰고 8장은 `guardian_link`, `device`, `intake_event`, `terms_version`을 쓴다. 이번 문서의 DB 열은 8장 후보만 적었고 확정하지 않는다.
17. 시제품 웹과 연동 웹. `Capstone-FRONT-END-WEB`은 localStorage다. `yakyeon-consent-web`은 메모리 세션과 HTTP다. 명세가 말하는 「브라우저에만 남습니다」 문구는 시제품용이고, 연동 후에는 빼야 한다고 4-1이 적는다. consent-web은 localStorage를 쓰지 않는다.
18. `docs/graph-pillbox-api-integration-status.md`(10-30)는 칸 배정을 화면 미연결로 적었다. 10-31 홈 연동 이후 `MainActivity`가 등록 성공 뒤 `HomeApi.assignCompartment`를 호출한다. 충전·추가 충전·배정 해제·약통 등록 POST는 여전히 화면 미연결이다.

## 8. 추가 확인

백엔드 저장소가 없어 아래는 미확인이다.

- 2부 64개가 현재 서버 브랜치에 있는지, 4부 9개가 아직 404인지.
- `shared_items`, `SHARE_NOT_GRANTED`, `CONSENT_REQUIRED`, `attention`, device settings, device sync의 배포 여부.
- 가입 잠금 횟수와 시간.
- 약통 QR 원문에서 `serial_no`와 `secret`을 나누는 형식.
- `schedule_synced_at`이 동기화 요청 시각인지, 기기가 일정을 받은 시각인지.
- 배정 직후 수량 필드가 null인지 0인지.
- DUR 수집 주기와 OCR 업체가 Mock인지.
- FCM 프로젝트 연동 여부.
- MQTT를 이미 버렸는지, HTTPS와 병행하는지.
- 설계서 4장 테이블 이름과 8장 중 어느 쪽이 현재 스키마인지.
- 배포 Base URL. 개발 값만 확인했다.
- `GET /patients/{id}/share-summary`, `PATCH .../device/settings`, `GET/POST /users/me/agreements`의 성공 HTTP 코드. 절에 없는 항목이 있다. 앱은 2xx와 본문 필드로 판단한다.

프론트에서 화면이 더 필요한 호출은 약통 등록, 충전, 추가 충전, 배정 해제, 알림 목록, 푸시 토큰, 권한 변경, 복약자가 여럿일 때의 선택이다. 이것들을 이번 조사에서 구현하지 않았다.
