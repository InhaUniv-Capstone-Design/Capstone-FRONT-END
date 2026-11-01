# 서버 배포 전 프론트엔드 작업

문서 버전: 2026-11-01.  
브랜치: `main` (`b1f8743`). 이번 수정은 커밋하지 않았다. 기존 미커밋 프론트 파일을 덮어쓰지 않고 필요한 부분만 고쳤다.

## 실제 수정 파일

### Android

| 파일 | 내용 |
| --- | --- |
| `app/src/main/java/com/example/yakyeon2/api/AuthContracts.java` | 휴대폰은 `010` 11자리 |
| `app/src/main/java/com/example/yakyeon2/api/RefreshGate.java` | 갱신 요청을 하나로 합침 |
| `app/src/main/java/com/example/yakyeon2/api/ApiClient.java` | 대기 중인 요청은 새 토큰을 씀. 파싱 오류·5xx·네트워크는 세션을 지우지 않음 |
| `app/src/main/java/com/example/yakyeon2/api/AuthApi.java` | 탈퇴 전에 저장된 푸시 토큰 삭제. 404만 건너뜀 |
| `app/src/main/java/com/example/yakyeon2/data/AuthTokenStore.java` | FCM 토큰 저장 칸 |
| `app/src/main/java/com/example/yakyeon2/data/LocalAccountData.java` | 계정 캐시 파일 삭제 |
| `app/src/main/java/com/example/yakyeon2/data/MedicationStore.java` | 약·기록·약통 연결 표시 삭제 |
| `app/src/main/java/com/example/yakyeon2/data/NotificationPreferences.java` | 알림 기본값 복구 |
| `app/src/main/java/com/example/yakyeon2/MainActivity.java` | 탈퇴·로그아웃 성공 뒤에만 로컬 삭제. DEVICE_NOT_FOUND만 연결 표시 해제. 그래프 건수 검사. 약통 직접 등록 |
| `app/src/main/java/com/example/yakyeon2/api/SettingsContracts.java` | 보호자는 목록의 첫 복약자. `schedule_in_sync`는 없을 때 null |
| `app/src/main/java/com/example/yakyeon2/api/CareContracts.java` | `dur_seq` 문자열, groups 크기, 건수 일치 |
| `app/src/main/java/com/example/yakyeon2/api/HomeRegistration.java` | 처방일 추정값 주석 |
| `app/src/main/java/com/example/yakyeon2/main/SettingsScreens.java` | 로그인 약통 검색에 시리얼·비밀값 입력. 동기화 문구 |
| `app/src/main/res/values/strings.xml` | 미배포 404, 등록, 동기화 문구 |
| 테스트 | `RefreshGateTest`, `AccountWipeTest`, `ApiContractTest`, `CareContractTest` |

### 웹 (`yakyeon-consent-web`, 이 저장소 밖)

| 파일 | 내용 |
| --- | --- |
| `js/session.js` | 로그인·갱신 6필드가 아니면 저장하지 않음 |
| `js/http.js` | 동시 갱신 1회. 5xx·네트워크·필드 누락은 세션 유지. 401 재사용·무효 토큰만 삭제 |
| `js/api.js` | 로그인 응답도 같은 6필드 |
| `tests/http.test.js` | 위 규칙 |

## 완료한 작업

- 휴대폰 입력을 서버보다 좁은 `010` 11자리로 고정했다.
- Refresh Token 동시 갱신을 앱과 웹에서 한 요청으로 모았다.
- 탈퇴 성공 전에는 로컬 데이터를 지우지 않는다. 성공 뒤에 토큰, 약, 알림, 공유 설정, 약통 연결 표시, 사용자 이름을 지운다.
- 로컬 저장은 계정별로 나뉘어 있지 않다. 이 기기의 약연 캐시 하나가 현재 계정이다. 그래서 성공한 로그아웃과 탈퇴가 그 캐시 전체를 지운다.
- 푸시 토큰이 기기에 없으면 `DELETE /users/me/push-token`을 보내지 않는다. 앱은 아직 FCM 토큰을 받지 않는다.
- `DEVICE_NOT_FOUND`에서만 로컬 약통 연결 표시를 끈다.
- 보호자 여러 명은 선택 화면 없이 서버 정렬의 첫 복약자를 쓴다.
- 약통 등록은 로그인 화면에서 시리얼 번호와 비밀값을 받아 `POST /patients/{id}/device`로 보낸다. 비밀값 입력칸은 비밀번호 타입이고, 전송 직후 칸을 비운다. 로그에 넣지 않는다.
- `schedule_in_sync`가 없으면 동기화 완료로 보이지 않는다. 5초 재조회는 넣지 않았다.
- `attention`이 없어도 약통 JSON 파싱은 실패하지 않는다.
- 코드 없는 404는 "이 기능은 서버에 아직 없습니다"로 보여 프론트 오류와 구분한다.

## 테스트 결과

2026-11-01, 서버 없이 실행했다.

| 명령 | 결과 |
| --- | --- |
| `./gradlew test --offline` | BUILD SUCCESSFUL. JVM 161개, 실패 0 |
| `./gradlew assembleDebug --offline` | BUILD SUCCESSFUL. `app-debug` 패키지 |
| `yakyeon-consent-web` `npm test` | 23개 통과, 0개 실패 |

에뮬레이터와 브라우저에서 화면을 눌러 보지는 않았다. 약통 입력칸의 실제 배치는 기기에서 한 번 더 봐야 한다.

## 남은 작업

- 서버 Base URL은 `yakyeon.apiBaseUrl`과 웹 `js/config.js`에 배포 주소를 넣어야 한다. 기본값은 `http://localhost:8080`과 `http://127.0.0.1:8080`이다.
- 약관·약통 설정·공유 범위 API는 화면이 아직 호출한다. 배포 전에는 404가 날 수 있고, 그 404를 앱 결함으로 보지 않는다.
- FCM 토큰 발급은 없다. 탈퇴 시 푸시 삭제는 토큰이 저장된 뒤에만 동작한다.
- 5초 간격 `GET device` 재조회는 `schedule_in_sync` 배포 뒤에 연결한다.
- 시제품 웹 `Capstone-FRONT-END-WEB`은 localStorage 데모다. API 연동 대상이 아니다.

## 배포 전 확인

- 디버그 빌드가 설치되는지
- 로그인 화면 휴대폰이 `011`과 10자리를 거절하는지
- 약통 검색 화면에 시리얼·비밀값 칸이 보이는지
- 비밀값이 로그캣에 없는지
