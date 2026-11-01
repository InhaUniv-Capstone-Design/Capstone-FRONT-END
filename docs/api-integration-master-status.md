# API 통합 현황

문서 버전: 2026-11-01.  
기준: 백엔드 담당자가 코드를 확인하고 확정한 답변. 배포일은 없다.  
검증 구분: 로컬 검증은 JVM·Node 테스트다. 서버 검증은 아직 없다.

## 상태 뜻

| 상태 | 의미 |
| --- | --- |
| 구현 완료 | 프론트 코드에 호출 또는 파서가 있다 |
| 로컬 검증 완료 | 서버 없이 테스트가 통과했다 |
| 서버 검증 완료 | 이번 작업에서 해당 없음 |
| 서버 미구현 | 백엔드가 아직 제공하지 않는다 |
| 프론트 수정 필요 | 이번 개정에서 반영한 항목은 아래 수정 표 |
| 배포 후 검증 | 서버 주소 설정 뒤에 확인할 항목 |

## 확정 계약과 프론트

| API | 백엔드 | 성공 코드 | Android | 웹 | 로컬 검증 | 서버 검증 |
| --- | --- | --- | --- | --- | --- | --- |
| POST /auth/login, /auth/refresh | 구현 | 200. 토큰 6필드 | `SessionResponse` | `acceptSession` | 통과 | 없음 |
| POST /auth/refresh 동시 호출 | 구현. 재사용 시 재로그인 | 갱신 1회 | `RefreshGate` | `refreshTask` | 통과 | 없음 |
| DELETE /auth/account | 구현 | 204 | 푸시 토큰이 있으면 먼저 삭제. 성공 뒤에만 로컬 삭제 | 해당 화면 없음 | 로컬 삭제 단위 테스트 | 없음 |
| GET /terms | 구현. MEDICATION 시드 없음 | 200 배열 | 호출 | 호출 | 기존 테스트 | 없음 |
| GET/POST /users/me/agreements, withdraw | 미구현 | 200 배열, 200 한 건, 204 | 화면이 호출. 404는 미배포로 표시. 동의 없으면 약 등록 안 함 | 미사용 | 파서 테스트 | 없음 |
| GET/PATCH /users/me/notification-settings | 구현. 항상 8개 | 200 | 8개 아니면 거절. 키로 식별 | 해당 없음 | 기존 테스트 | 없음 |
| GET /users/me/managed-patients | 구현. 정렬 보장 | 200 | 보호자는 첫 복약자 | 호출 | 통과 | 없음 |
| 병용금기 summary, graph, interactions | 구현. 보호자는 향후 DUR_INFO | 200 | string `dur_seq`, groups 2개 이상, 건수 일치 검사 | 해당 없음 | 통과 | 없음 |
| GET /interaction-edges/{dur_seq} | 구현. 로그인만 | 200 | 경로 변수 string | 해당 없음 | 통과 | 없음 |
| GET device, GET pillbox | 구현. 없으면 DEVICE_NOT_FOUND | 200 | 404 해당 코드만 로컬 연결 해제 | 해당 없음 | 파서 테스트 | 없음 |
| POST /patients/{id}/device | 구현 | 201 | 시리얼·비밀값 직접 입력 | 해당 없음 | 본문 테스트 | 없음 |
| POST compartment, load, refill, DELETE mapping | 구현 | 201 또는 204 | 상태·수량 분리. 204는 본문 파싱 안 함 | 해당 없음 | 기존 테스트 | 없음 |
| PATCH device/settings, POST device/sync | 미구현 | 200 약통 구조, 202 접수 | 화면 호출. 202를 기기 완료로 표시하지 않음. `schedule_in_sync` 없으면 null | 해당 없음 | 파서 | 없음 |
| GET home-summary, doses | 구현. BEDTIME 거절 | 200 | 슬롯은 이름으로 구분. BEDTIME 미전송 | 해당 없음 | 기존 테스트 | 없음 |
| POST prescriptions | 구현. 동의 검사는 아직 201 | 201 | 프론트는 동의 없으면 등록하지 않음 | 해당 없음 | 기존 테스트 | 없음 |
| POST /prescriptions/ocr | Mock | 200 고정 3약 | 실제 판독 완료로 쓰지 않음 | 해당 없음 | 문구 유지 | 없음 |
| GET share-summary | 미구현 | 200 JSON | `medication_ids` 없음 | 해당 없음 | 쿼리 테스트 | 없음 |
| POST /guardian-links/grant, PATCH shared-items | 미구현 | 200 예정 | 앱은 grant를 호출하지 않음. QR은 웹 주소 | `yakyeon-consent-web`이 호출 | 웹 테스트 | 없음 |
| DELETE /guardian-links/{id} | 구현 | 204 | 호출 | 호출 | 204 테스트 | 없음 |

## 알림 8개 순서

`DOSE_REMINDER`, `MISSED`, `END_SOON`, `DEVICE`, `GUARDIAN_OPEN`, `GUARDIAN_TAKEN`, `GUARDIAN_MISSED`, `GUARDIAN_DEVICE`.

화면 순서가 아니라 `setting_key`로 맞춘다. 8개가 아니면 정상 응답으로 보지 않는다.

## 세션

| 상황 | 처리 |
| --- | --- |
| 200이고 6필드 | 저장 |
| 200인데 필드 누락 | 세션 유지. 응답 오류 |
| 401 INVALID_REFRESH_TOKEN | 세션 삭제 |
| 401 REFRESH_TOKEN_REUSED | 세션 삭제 후 로그인 |
| 5xx, 네트워크, 파싱 오류 | 세션 유지 |
| 동시 401 | 갱신 요청 1개. 나머지는 결과를 기다림 |

## 휴대폰

앱과 웹 모두 `010`으로 시작하는 숫자 11자리만 허용한다. 하이픈과 공백은 검사 전에 뺀다.

## 향후 서버 순서

1. 약관 동의 관리와 MEDICATION 시드
2. 약통 설정. `schedule_in_sync`, `attention`
3. 그 다음 웹 동의와 열람 범위. `shared_items`
4. 취침 슬롯은 약통 설정과 복약 이력 공유 이후

5초 간격 재조회는 `schedule_in_sync`가 생기기 전에는 돌리지 않는다. 필드가 없으면 오류도 아니고 true도 아니다.

## 웹 두 벌

| 프로젝트 | 위치 | 저장 | HTTP |
| --- | --- | --- | --- |
| 권한 관리 웹 | `/Users/eden/AndroidStudioProjects/yakyeon-consent-web` | 메모리. localStorage 없음 | 있다 |
| 시제품 | `/Users/eden/Desktop/Capstone-FRONT-END-WEB` | localStorage | 없음. 이번 수정 없음 |

웹 QR은 앱이 만드는 보호자 연동 주소다. 약통 `serial_no`/`secret`과 같은 데이터가 아니다.
