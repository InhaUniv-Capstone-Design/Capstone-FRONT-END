# 백엔드에 남긴 요청

문서 버전: 2026-11-01.  
이미 코드로 확정된 답변은 다시 묻지 않는다. 아래는 아직 서버에 없거나, 배포 전에 데이터만 필요한 항목이다. 배포일은 적지 않는다.

## 미구현 API

예상 순서는 백엔드가 안내한 그대로다.

1. 약관 동의 관리
   - `GET /users/me/agreements` → 200 배열
   - `POST /users/me/agreements` → 200, 필드 `terms_type`, `terms_version_id`, `version`, `agreed`, `changed_at`. 재동의도 200
   - `POST /users/me/agreements/{termsType}/withdraw` → 204, 본문 없음
2. 약통 설정
   - `PATCH /patients/{id}/device/settings` → 200, GET device와 같은 구조
   - `POST /patients/{id}/device/sync` → 202. 접수이지 기기 반영 완료가 아님
   - pillbox `attention`: null 또는 `OPENED`
   - GET device `schedule_in_sync`: 서버 일정 버전과 기기가 보고한 버전이 같으면 true
3. 그 다음 웹 동의와 열람 범위
   - `GET /patients/{id}/share-summary` → 200 JSON
   - `POST /guardian-links/grant`
   - `POST`가 아니라 명세의 `PATCH /guardian-links/{linkId}/shared-items`
   - `managed-patients`의 보호자 행에 `shared_items` 배열. SELF 행에는 없음
4. 취침 슬롯 `BEDTIME`은 약통 설정과 복약 이력 공유 이후

## 개발 DB 시드

- `GET /terms`는 있으나 `MEDICATION` 약관 행이 없다. 약 등록 동의 화면이 이 행을 찾는다.
- 동의 관리가 생기면 서버가 약 등록 때 동의를 보고 `403 CONSENT_REQUIRED`를 줄 수 있다. 그 전까지 동의 없는 201은 서버 현재 동작이다.

## 배포 전 서버 확인

- 갱신 200에 `access_token`, `refresh_token`, `token_type`, `expires_in`, `user_id`, `account_type`이 항상 있는지
- 알림 설정 GET/PATCH가 항상 8개인지
- `dur_seq`가 숫자 JSON이 아닌지
- `groups`가 null이 아닌지
- 홈 슬롯이 `NONE`/`PENDING`/`DONE`/`MISSED`인지
- 약통이 없을 때 device, pillbox, 칸 조회가 `404 DEVICE_NOT_FOUND`인지
- 보호자 약통 API가 `DEVICE_EVENT` 없이 `403 INSUFFICIENT_PERMISSION` 또는 예정된 `SHARE_NOT_GRANTED`인지. 앱은 403을 성공으로 치지 않는다
- 병용금기 요약·그래프·목록의 보호자 403은 아직 없으므로, 배포 직후 404와 403을 구분할 수 있어야 한다

## 설계를 더 정해야 하는 것

- 약통 시리얼과 비밀값을 기기에서 사용자에게 어떻게 보여줄지. QR과 블루투스는 정하지 않았다. 앱은 직접 입력만 한다.
- `schedule_in_sync`가 배포된 뒤 5초 간격 재조회를 앱에 연결한다. 지금은 연결하지 않았다.
- FCM 토큰 등록은 앱 메서드만 있다. 발급·채널은 서버 푸시 일정과 맞춰야 한다.

## 다시 묻지 않는 확정

성공 코드, 알림 8개, 세션 삭제 조건, 탈퇴 순서, 휴대폰은 앱이 `010` 11자리만 보냄, 약관 철회 버튼은 `MEDICATION`, 공유는 전체만, 보호자는 첫 복약자, 약통 권한, `dur_seq` string, groups 배열, nodes와 summary 차이, 칸 배정·충전·추가 충전 의미, 칸 해제 204, OCR Mock, 약 수정 화면 없음, 처방일 선택, BEDTIME 미지원.
