# 서버 배포 후 통합 테스트

문서 버전: 2026-11-01.  
이 목록은 실제 서버가 응답한 뒤에만 체크한다. Mock과 JVM 테스트로 채우지 않는다.

## 주소

- Android: `gradle.properties` 또는 `-Pyakyeon.apiBaseUrl=` 로 배포 URL. 코드 기본값은 `http://localhost:8080`. 경로 접두어 `/api/v1`은 없다.
- 웹: `yakyeon-consent-web/js/config.js`의 `API_BASE_URL`. 기본값은 `http://127.0.0.1:8080`.
- 평문 HTTP는 디버그 네트워크 설정에서만 허용되는지 확인한다.

## 로그인과 인증

- 회원가입 `010` 11자리. `011`은 앱이 보내기 전에 막는지, 서버는 더 넓어도 앱이 subset만 보내는지
- 로그인 200의 6필드 저장
- 만료된 access로 요청 두 개를 동시에 보냈을 때 refresh가 한 번인지
- `401 INVALID_REFRESH_TOKEN`과 `401 REFRESH_TOKEN_REUSED` 뒤에 로그인 화면으로 가는지
- 서버 5xx와 비행기 모드에서 세션이 남는지
- 로그아웃 204
- 탈퇴: 성공 204 뒤에 앱을 다시 열어도 이전 약·토큰이 없는지. 실패하면 데이터가 남는지
- 푸시 토큰이 없는 지금 빌드에서는 탈퇴가 `DELETE /users/me/push-token` 없이 `DELETE /auth/account`만 호출하는지

## 약 등록

- `GET /terms`에 `MEDICATION`이 없으면 등록이 막히는지
- 동의 API가 아직 404이면 약을 저장하지 않는지
- 동의 API가 생긴 뒤 `403 CONSENT_REQUIRED`를 성공으로 보이지 않는지
- OCR은 Mock 3약이다. 화면 문구가 실제 판독 완료처럼 보이지 않는지
- 처방일이 OCR에 있으면 그 값, 없으면 가장 이른 시작일이 추정값으로 가는지
- `BEDTIME`이 포함된 초안은 서버로 나가지 않는지

## 복약 일정과 홈

- 슬롯 `NONE`/`PENDING`/`DONE`/`MISSED`
- `next_dose`가 없으면 null
- `next_dose.status`는 `PENDING`
- 알 수 없는 `timing_slot`은 무시
- 복용 완료 뒤 홈과 기록 탭이 다시 읽히는지
- 빈 일정

## 그래프

- `dur_seq`가 문자열로 상세 경로에 들어가는지
- `groups`가 `[]`일 때와 `[[1,3]]`일 때
- `summary.total`이 노드 수보다 작을 수 있는지
- `unchecked`, `flagged`, edges, interactions 개수가 어긋나면 화면이 정상 그래프로 보이지 않는지
- 보호자 `DUR_INFO` 없음. 지금은 404일 수 있다. 구현 뒤 403은 "공유되지 않음"인지
- 상세 API는 환자 번호 없이 로그인만으로 열리는지

## 약통

- 시리얼·비밀값 등록. 비밀값이 로그에 없는지
- 없는 약통 `404 DEVICE_NOT_FOUND`만 로컬 연결 표시를 끄는지. 401과 5xx는 끄지 않는지
- 배정 직후 `PENDING_LOAD`와 null 수량
- 충전과 추가 충전에서 `loaded_count`와 `remaining_count`가 다른지
- 칸 해제 204 뒤 다음 조회가 `EMPTY`인지
- 보호자 `DEVICE_EVENT` 없음은 403이고 성공으로 보이지 않는지
- sync 202는 접수 문구인지. `schedule_in_sync`가 생기기 전에는 완료로 보이지 않는지
- `attention`이 없는 응답도 약통 화면이 열리는지

## 알림

- GET과 PATCH가 8개인지
- 순서가 바뀌어도 키로 스위치가 맞는지
- 8개 미만은 저장 완료로 보이지 않는지

## 보호자와 웹

- 보호자 앱이 복약자 선택 없이 첫 행을 쓰는지
- 앱 QR은 웹 주소만 있는지. 약통 비밀값과 다른지
- 웹 로그인, 열람 허용, 범위 변경. 서버에 grant와 shared-items가 없으면 404인지
- `medication_ids`가 요청에 없는지
- 웹 로그아웃과 토큰이 localStorage에 남지 않는지
- 공유 요약 API가 생긴 뒤 앱 PDF가 200 JSON으로 만들어지는지

## IoT

- 202 접수 후 기기가 일정을 받았는지는 `schedule_in_sync`로만 판단
- 필드가 false로 남으면 "기기가 다음에 연결될 때 반영돼요"
- 약통 개방 이벤트를 앱이 복약 증거처럼 말하지 않는지
