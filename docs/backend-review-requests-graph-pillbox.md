# 그래프·약통 탭 백엔드 확인 요청

작성일: 2026-10-30

대상은 그래프 탭(병용금기)과 약통 탭이다. 프론트엔드 코드는 수정하지 않았다.

기준 문서:

- 약연 서버 API 명세 v0.9 (기준일 2026-10-29). 2부는 구현된 API, 4부는 예정 API로 구분한다.
- `docs/graph-pillbox-api-integration-status.md`
- 앱의 `CareApi`, `CareContracts`, `MainActivity`, `MainScreens`, `SettingsScreens`

`http://127.0.0.1:8080` 은 이 검토 시점에도 연결이 거부되었다. 서버 구현이 명세와 같다고 보지 않는다.

## 1) 전체 검토 요약

| 구분 | 개수 |
|---|---|
| 백엔드 담당자에게 확인이 필요한 사항 | 7 |
| 백엔드 코드 수정이 확인된 사항 | 0 |
| 명세 보완이 필요한 사항 | 4 |
| 실제 서버 연동 후에만 확인할 수 있는 사항 | 6 |
| 프론트엔드에서 추가 구현이 필요한 사항 | 6 |

한 항목은 한 구분에만 넣었다. 명세 예시와 본문이 어긋나는 것은 서버 결함으로 적지 않았다. 서버를 보지 못했기 때문이다.

이미 명세에 있어서 다시 묻지 않은 것:

- `unchecked > 0` 이면 등재 없음을 안전으로 보여 주지 않는다.
- `checked = false` 는 약물 DB와 매칭되지 않아 검사하지 못한 약이다. 별도의 사유 코드는 명세에 없다.
- `disclaimer` 는 상세 응답에 항상 있고, 화면에 함께 표시한다.
- 한 칸에 약봉지 하나, 한 약봉지는 한 칸이다. 칸 번호와 `max_compartments` 는 1~3이고, 칸 수를 생략하면 3이다.
- `DEVICE` 는 약통 등록 약관이다. `DEVICE_EVENT` 는 보호자 열람 항목이다.
- `READ_ONLY` 는 조회만 되고, 쓰기는 `403 INSUFFICIENT_PERMISSION` 이다.
- `pouch_present`, `alert_active` 가 null 이면 보고가 없는 것이다. 없음이나 꺼짐이 아니다.
- `attention` 은 예정 필드이고, 값은 `OPENED` 또는 null 이다.
- `POST /device/sync` 의 `202` 는 일정 버전을 올려 다음 기기 연결 때 받게 하는 요청이다. 그 응답만으로 하드웨어 반영 완료가 아니다.
- 오류 본문은 `{ "code", "message" }` 이고, 화면 분기는 `code` 로 한다.
- 액세스 토큰 만료는 `401 UNAUTHENTICATED` 이고, `POST /auth/refresh` 로 한 번 갱신한다. refresh token 은 1회용이다.

## 2) 백엔드 담당자에게 수정 요청이 필요한 사항

확인된 서버 결함은 없다.

명세 예시가 본문과 다른 곳은 4)에 두었다. 그 예시대로 서버가 응답하는지는 서버에 붙기 전에는 알 수 없다. 7개 질문에 대한 답에서 서버 수정이 필요해지면 그때 수정 요청으로 올리면 된다.

## 3) 백엔드 담당자에게 확인 질문이 필요한 사항

| 번호 | 구분 | 관련 API | 확인이 필요한 내용 | 질문 문장 | 확인이 필요한 이유 |
|---|---|---|---|---|---|
| 1 | 그래프 | `GET /interaction-edges/{dur_seq}` | 경로 변수의 타입 | `dur_seq` 는 목록과 그래프에서 string 입니다. 상세 API의 경로 변수 타입은 number 로 적혀 있습니다. 서버는 이 값을 문자열 경로로 받나요, 정수로 받나요? 숫자만 가능한가요, 64비트 정수를 넘는 값이나 앞에 0이 있는 값이 있나요? 받는 타입에 맞춰 명세의 경로 타입을 string 또는 number 중 하나로 맞춰 주세요. | 앱은 문자열을 그대로 경로에 넣습니다. 서버가 정수만 받으면 상세 조회가 실패합니다. |
| 2 | 그래프 | `GET /patients/{patient_id}/interaction-graph` | `groups` 예시의 null | 필드 타입은 `List<List<Long>>` 인데, 예시의 `groups` 는 `[null]` 입니다. 실제 응답에 null 묶음이나 null 원소가 들어가나요? 예시가 잘못된 것이면 예시만 고쳐 주세요. | 앱은 null 묶음을 잘못된 응답으로 보고 그래프를 그리지 않습니다. 예시대로 null 이 오면 화면이 오류가 됩니다. |
| 3 | 그래프 | 요약, 그래프, 목록 | 세 API의 개수 관계 | `summary.total`(오늘 복용 중인 약 수)과 그래프 `nodes`(끝나지 않은 모든 약)는 같은 집합인가요? `summary.unchecked` 는 `checked = false` 인 노드 수와 항상 같나요? `summary.flagged`(약 쌍 수)는 `edges` 개수, `interactions` 개수와 항상 같나요? 같은 약 쌍에 사유가 두 개면 `flagged` 는 1이고 목록은 2건인가요? | 화면은 요약 숫자와 목록을 함께 보여 줍니다. 기준이 다르면 서로 다른 숫자가 동시에 보입니다. |
| 4 | 약통 | 칸 배정, 충전, 추가 충전 | 수량 계산 | `POST /prescriptions/{prescription_id}/compartment` 직후 `status` 는 `PENDING_LOAD` 인가요? 그때 `loaded_count` 와 `remaining_count` 는 null 인가요, 0 인가요, 다른 값인가요? `POST .../load` 성공 시 `remaining_count` 는 방금 보낸 `loaded_count` 와 같나요? `POST .../refill` 의 `added_count` 는 `remaining_count` 에만 더해지나요, `loaded_count` 에도 더해지나요? 배정·충전·추가 충전 예시가 모두 같은 JSON(`status` ACTIVE, `loaded_count` 5, `remaining_count` 4, `valid_to` 가 `valid_from` 보다 앞섬)이라, 맞는 예시로 바꿔 주세요. | 본문은 배정 직후를 충전 전이라고 하고, 충전한 수가 남은 수의 시작값이라고 합니다. 예시는 그 문장과 다릅니다. 앱은 응답 숫자를 그대로 보여 줄 예정이라, 어느 쪽이 맞는 계약인지 필요합니다. |
| 5 | 약통 | `POST /patients/{patient_id}/device/sync`, `GET /patients/{patient_id}/device` | 동기화 시각의 의미 | `202` 의 `schedule_version` 은 서버가 버전을 올렸다는 뜻으로 이해했습니다. `schedule_synced_at` 은 이 POST 시각인가요, 기기가 일정을 받아 간 시각인가요? 앱이 하드웨어 반영 완료를 알 수 있는 필드가 있나요? 없으면 없다고 적어 주세요. | 앱은 동기화 요청과 기기 반영 완료를 같은 성공으로 보여 주면 안 됩니다. |
| 6 | 권한 | `GET /users/me/managed-patients`, 4-1, 4-11, 4-12 | 예정 API의 현재 배포 여부 | 지금 앱이 호출할 서버에 다음이 들어가 있나요? ① `managed-patients` 의 `shared_items` ② 보호자 호출의 `403 SHARE_NOT_GRANTED` ③ `PATCH /patients/{patient_id}/device/settings` ④ `POST /patients/{patient_id}/device/sync` ⑤ `pillbox` 의 `attention` ⑥ `POST /patients/{patient_id}/device` 의 `403 CONSENT_REQUIRED`. `shared_items` 필드가 없을 때와 빈 배열일 때 서버 동작이 다른가요? 필드가 아직 없으면, 앱이 조회를 호출하고 `403` 으로 판단해도 되나요? | 2부 `managed-patients` 와 `GET /device` 에는 이 필드가 없습니다. 4부는 예정입니다. 없는 API를 앱이 호출하면 설정 화면이 실패합니다. |
| 7 | 권한 | 약통 쓰기 API | `DEVICE_EVENT` 범위 | 4-1은 보호자의 `DEVICE_EVENT` 가 필요한 약통 API를 "조회·등록·칸 배정"이라고만 적습니다. 다음도 같은 항목이 필요한가요? `POST .../load`, `POST .../refill`, `DELETE /compartment-mappings/{mapping_id}`, `DELETE /patients/{patient_id}/device`, `POST .../device/sync`. `PATCH .../device/settings` 는 4-11에 `DEVICE_EVENT` 가 필요하다고 적혀 있어 묻지 않습니다. | 항목이 없으면 `403 SHARE_NOT_GRANTED` 와 `403 INSUFFICIENT_PERMISSION` 을 앱이 나눠야 합니다. 표에 없는 API의 기준이 필요합니다. |

## 4) API 명세서 보완이 필요한 사항

| 번호 | 관련 API | 현재 명세의 부족한 부분 | 추가로 정의해야 할 내용 | 필요한 이유 |
|---|---|---|---|---|
| 1 | `POST /patients/{patient_id}/device` | 시리얼과 비밀값은 기기 라벨 QR에 있다고만 적혀 있습니다. QR 문자열 형식은 없습니다. | QR 원문의 형식. 예: JSON 키 이름, 구분자, 인코딩. `serial_no` 와 `secret` 을 어떻게 나누는지. `secret` 의 32~128자 규칙은 이미 있으므로 반복하지 않아도 됩니다. | 앱이 QR을 읽어 등록 본문을 만들려면 이 형식이 필요합니다. 서버에 QR 생성 API는 없다고 명세에 있습니다. |
| 2 | `GET /patients/{patient_id}/interactions/summary`, `GET .../interaction-graph`, `GET /interaction-edges/{dur_seq}` | `DUR_INFO` 와 `403 SHARE_NOT_GRANTED` 안내는 목록 API(2-4-1)와 4-1에만 있습니다. 요약·그래프·상세의 오류 표에는 없습니다. | 4-1과 같이 네 API 모두 보호자 호출에 `DUR_INFO` 가 필요하고, 없으면 `403 SHARE_NOT_GRANTED` 라고 2-4-2, 2-4-3, 2-4-4 오류 표에 적어 주세요. 아직 서버에 없으면 예정이라고 표시해 주세요. | 2부만 보면 요약과 그래프는 공유 항목 검사가 없는 것처럼 보입니다. |
| 3 | `GET /patients/{patient_id}/pillbox` | 본문은 칸 배열이 칸 수만큼 1번부터 있다고 합니다. 예시는 1번 칸만 있습니다. | 예시에 빈 칸과 충전 전 칸을 포함해 `max_compartments` 와 같은 길이로 적어 주세요. null 수량과 `EMPTY` 도 예시와 같게 적어 주세요. | 예시만 보면 사용 중인 칸만 내려오는 API로 오해합니다. |
| 4 | `POST /patients/{patient_id}/device` | 4-12는 `DEVICE` 동의가 없으면 `403 CONSENT_REQUIRED` 라고 합니다. 2-6-5 오류 표에는 이 코드가 없습니다. | 2-6-5 오류 표에 `CONSENT_REQUIRED` 를 넣고, 현재 서버에 없으면 예정이라고 표시해 주세요. | 구현된 API 표와 예정 절의 오류 코드가 다릅니다. |

## 5) 실제 서버 연동 후 확인할 사항

서버가 꺼져 있어 아래는 하지 못했다. 성공 JSON을 만들어 넣지 않는다.

| 번호 | 관련 API | 확인 항목 | 확인 방법 | 성공 판정 기준 |
|---|---|---|---|---|
| 1 | 병용금기 4개 | 실제 JSON, `dur_seq` 왕복, `disclaimer`, 노드의 `checked` | 테스트 계정으로 요약 → 그래프 → 목록 → 상세를 순서대로 호출하고, 앱 그래프 탭에 같은 값을 표시 | 요약 세 필드가 화면에 있다. `unchecked > 0` 이면 미확인 안내가 있다. 상세에 출처 기준일, 주의사항, 면책 문구가 있다. `dur_seq` 가 다른 선의 상세를 열지 않는다. |
| 2 | `GET /patients/{patient_id}/pillbox` | 칸 수, 상태, null | 등록된 약통으로 조회 body 와 약통 탭을 비교 | 칸이 `compartment_no` 순이고 길이가 `max_compartments` 와 같다. 충전 전 수량은 0으로 보이지 않는다. `OPENED` 만 열림 안내가 있다. |
| 3 | `GET/PATCH /device`, `POST .../sync`, `DELETE .../device` | 부분 수정, `202`, `204` | 설정에서 부저만 바꾸고, 동기화 후 해제 | PATCH body 에 없는 필드는 유지된다. 동기화 응답의 `schedule_version` 이 화면에 요청 완료로만 보인다. 해제 후 약통 조회는 `404 DEVICE_NOT_FOUND` 이고 처방 기록은 조회된다. |
| 4 | 등록, 배정, 충전, 추가 충전, 배정 해제 | 상태 전이와 409 | QR 값으로 등록한 뒤 배정 → 충전 → 추가 충전 → 해제를 호출하고 `pillbox` 를 다시 읽음 | 배정 직후 `PENDING_LOAD`, 충전 후 `ACTIVE`. 남은 약봉지 수가 서버 응답과 같다. `COMPARTMENT_OCCUPIED` 와 `PRESCRIPTION_ALREADY_ASSIGNED` 가 구분된다. 해제 후 그 칸에 다시 배정된다. |
| 5 | 공통 오류 | `401`, `403` 두 종류, `404`, `409`, JSON이 아닌 5xx | 만료 토큰, 공유 없는 보호자, `READ_ONLY` 쓰기, 없는 약통, 잘못된 수량을 각각 호출 | 앱이 종료되지 않는다. `SHARE_NOT_GRANTED` 는 "공유되지 않음", `INSUFFICIENT_PERMISSION` 은 권한 부족으로 구분된다. `401` 은 갱신 후 한 번 재시도한다. |
| 6 | `GET /users/me/managed-patients` | 보호자 복약자가 둘 이상일 때의 실제 배열 | 보호자 계정으로 목록을 호출 | 앱이 임의 환자를 고르지 않은 상태가 맞는지, 목록의 `permission_scope` 와 `shared_items` 유무를 응답에서 확인한다. |

## 6) 프론트엔드에서 추가 구현해야 할 사항

백엔드가 막아서가 아니다. 화면 입력이 없거나, 복약자가 여러 명일 때 고르는 흐름이 없다.

| 번호 | 기능 | 현재 상태 | 추가 구현 사항 | 백엔드 의존 여부 |
|---|---|---|---|---|
| 1 | 약통 등록 | `POST /device` 메서드만 있다. 로그인한 화면은 예시 기기 `TRX-0328` 로 등록하지 않는다. | QR을 읽어 `serial_no`, `secret` 을 보내고, `201` 응답 뒤 `pillbox` 를 다시 읽는다. | 의존. 4)의 QR 형식이 정해진 뒤에 구현한다. |
| 2 | 칸 배정 | 메서드만 있다. | 처방 목록에서 약봉지를 고르고 1~3칸 중 빈 칸을 고른 뒤 호출한다. 성공 직후를 `PENDING_LOAD` 로 본다. | 처방 `prescription_id` 는 약 등록 API 응답에 있다. 그 화면 연동은 이번 탭 밖에 있다. |
| 3 | 최초 충전 | 메서드만 있다. | `PENDING_LOAD` 칸에 약봉지 수 1~100을 입력한다. | 없음. 배정 응답의 `mapping_id` 를 쓰면 된다. |
| 4 | 추가 충전 | 메서드만 있다. | `ACTIVE` 칸에만 `added_count` 1~100을 입력하고, 남은 수는 응답값으로 바꾼다. | 없음. 4) 수량 규칙이 정해지면 안내 문구를 맞춘다. |
| 5 | 배정 해제 | 메서드만 있다. | `204` 뒤에 `pillbox` 를 다시 읽어 그 칸을 비운다. | 없음. |
| 6 | 보호자의 복약자가 여러 명 | 목록 API는 있다. 앱은 한 명을 고르지 않고 안내만 보여 준다. | 복약자 선택 후 고른 `patient_id` 로 그래프와 약통을 호출한다. | 없음. 서버는 이미 목록을 준다. |

로그인하지 않은 그래프·약통은 로컬 데모이다. 그 화면을 서버 연동으로 바꾸라는 요청은 아니다.

## 7) 백엔드 담당자에게 전달할 최종 메시지

안녕하세요. 그래프 탭(병용금기)과 약통 탭을 명세 v0.9와 맞춰 보는 중입니다.

앱에는 조회 호출과 응답 파서를 넣어 두었습니다. 다만 오늘 `http://127.0.0.1:8080` 에 연결하지 못해, 실제 응답이 명세와 같은지는 확인하지 못했습니다. 서버 구현이 잘못되었다는 뜻은 아닙니다.

코드 수정 요청은 아직 없습니다. 아래는 명세만으로 정하기 어려운 질문입니다.

1. `dur_seq` 는 목록·그래프에서 string 이고, `GET /interaction-edges/{dur_seq}` 경로 타입은 number 입니다. 서버는 문자열 경로로 받나요, 정수로 받나요? 숫자 외 문자나 64비트를 넘는 값이 있나요? 받는 쪽에 맞춰 명세 경로 타입을 하나로 정해 주세요.

2. `GET /patients/{patient_id}/interaction-graph` 의 `groups` 타입은 약 번호 배열의 배열인데, 예시는 `[null]` 입니다. 실제 응답에 null 이 들어가나요? 예시가 잘못이면 예시만 고쳐 주세요.

3. `summary.total` 과 그래프 `nodes` 는 같은 약 집합인가요? `unchecked` 는 `checked = false` 노드 수와 같나요? `flagged` 는 `edges` 수, `interactions` 수와 같나요? 같은 약 쌍에 사유가 두 개면 쌍 수와 목록 건수가 달라지나요?

4. 칸 배정 직후 상태는 `PENDING_LOAD` 이고 수량은 null 인가요? 충전 후 `remaining_count` 는 보낸 `loaded_count` 와 같나요? 추가 충전의 `added_count` 는 남은 수에만 더해지나요, 채운 수에도 더해지나요? 배정·충전·추가 충전 예시가 같은 JSON이라, 각 API에 맞는 예시로 바꿔 주세요. 지금 예시는 `valid_to` 가 `valid_from` 보다 앞섭니다.

5. `POST /patients/{patient_id}/device/sync` 의 `202` 와 `schedule_version` 은 서버가 버전을 올렸다는 뜻으로 이해했습니다. `schedule_synced_at` 은 이 요청 시각인가요, 기기가 일정을 받아 간 시각인가요? 앱이 하드웨어 반영 완료를 알 필드가 있나요?

6. 지금 앱이 붙을 서버에 다음이 들어가 있나요? `managed-patients` 의 `shared_items`, `403 SHARE_NOT_GRANTED`, `PATCH /patients/{patient_id}/device/settings`, `POST .../device/sync`, `pillbox` 의 `attention`, 약통 등록의 `403 CONSENT_REQUIRED`. `shared_items` 가 응답에 없을 때와 빈 배열일 때 동작이 다른가요? 필드가 아직 없으면 조회를 호출하고 `403` 으로 판단해도 되나요?

7. 보호자 `DEVICE_EVENT` 는 4-1에 "약통 조회·등록·칸 배정"이라고 되어 있습니다. `load`, `refill`, 칸 배정 해제, 약통 등록 해제, `device/sync` 도 같은 항목이 필요한가요? 설정 변경은 4-11에 필요하다고 되어 있어 제외했습니다.

명세에 추가가 필요한 것도 있습니다.

1. 약통 QR 원문에서 `serial_no` 와 `secret` 을 어떻게 나누는지 형식.
2. 병용금기 요약·그래프·상세의 오류 표에도 `DUR_INFO` 가 없으면 `403 SHARE_NOT_GRANTED` 라고 적어 주세요. 아직이면 예정 표시.
3. `pillbox` 예시를 칸 수만큼, 빈 칸과 충전 전 칸이 보이게.
4. `POST /patients/{patient_id}/device` 오류 표에 `CONSENT_REQUIRED` 를 넣고, 아직이면 예정 표시.

서버가 켜지면 위 응답을 앱 화면과 다시 비교하겠습니다.
