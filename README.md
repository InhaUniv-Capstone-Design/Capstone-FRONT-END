# 약연 테스트 앱

팀원이 휴대폰에 바로 설치해 볼 수 있는 debug APK입니다. Android Studio를 열거나 환경 변수를 넣지 않아도 됩니다. 서버 주소는 APK 안에 이미 들어 있습니다.

## 받을 파일

[dist/Yakyeon_Test_v1.0.apk](dist/Yakyeon_Test_v1.0.apk)

| 항목 | 값 |
| --- | --- |
| 버전 | 1.0 (`versionCode` 1), debug |
| 패키지 이름 | `com.gamcho.yakyeon` |
| 런처에 보이는 이름 | yakyeon2 |
| 필요한 Android | 8.0 이상 (API 26) |
| 서버 | `https://kue200p7z3.execute-api.ap-northeast-2.amazonaws.com` |
| 설치할 때 넣을 환경값 | 없음 |

로그인, 회원가입, 약물, 약통 화면은 위 배포 서버와 HTTPS로 통신합니다. 이 APK는 `localhost`를 보지 않습니다.

## 휴대폰에 설치

1. 이 저장소에서 `dist/Yakyeon_Test_v1.0.apk`를 휴대폰으로 받습니다. GitHub 파일 페이지의 Download 버튼, 또는 PC로 받은 뒤 카카오톡·드라이브·USB로 옮겨도 됩니다.
2. 휴대폰의 내 파일 앱에서 APK를 누릅니다.
3. 설치가 막히면 **이 출처 허용** 또는 **알 수 없는 앱 설치**를 켠 뒤 다시 설치합니다. 설정 위치는 기기마다 `설정 > 보안` 또는 `설정 > 앱`입니다.
4. 설치 후 앱 서랍에서 **yakyeon2**를 엽니다.
5. 처음 실행하면 알림 권한을 묻습니다. 복약 알림을 보려면 허용합니다. 거부해도 로그인 화면은 열립니다.

이미 같은 패키지가 설치되어 있으면 그 위에 덮어씁니다. 서명이 다른 앱이 같은 패키지로 깔려 있으면 먼저 제거한 뒤 설치합니다.

컴퓨터에 폰을 연결한 경우에는 아래 명령으로 설치할 수 있습니다.

```bash
adb install -r dist/Yakyeon_Test_v1.0.apk
```

## 열어서 확인할 것

1. 로그인 화면이 나오면 팀 계정으로 로그인합니다. 계정이 없으면 **회원가입**으로 만들 수 있습니다. 가입에는 휴대폰 인증이 필요합니다.
2. 로그인 후 홈, 약물 등록, 약통, 복용 기록, 설정으로 이동해 봅니다.
3. 서버에 닿지 않으면 "서버에 연결할 수 없습니다"가 뜨고 앱은 꺼지지 않습니다. Wi-Fi 또는 모바일 데이터를 확인한 뒤 다시 시도합니다.
4. 약통의 실제 버튼 입력은 ESP32 기기가 계정에 연결되어 있을 때만 동작합니다. 기기가 없으면 그 단계는 넘어갑니다.

로그인 화면에는 로그인, 회원가입, 아이디 찾기, 비밀번호 재설정만 있습니다.

## 직접 빌드할 때만 필요한 값

APK만 설치할 때는 이 절을 보지 않아도 됩니다.

| 값 | 설명 |
| --- | --- |
| `yakyeon.apiBaseUrl` | 서버 주소. 비우면 위의 API Gateway 주소를 씁니다. `gradle.properties`에 넣거나 빌드 명령의 `-P`로 넘깁니다. |
| `app/google-services.json` | Firebase 설정. 저장소에 없습니다. 알림(FCM)을 포함한 앱을 직접 빌드할 때만 이 경로에 둡니다. 공유 APK에는 이미 포함되어 있습니다. |
| `local.properties` | Android Studio가 만드는 SDK 경로. 저장소에 올리지 않습니다. |

다른 서버로 다시 빌드하는 예:

```bash
./gradlew :app:assembleDebug -Pyakyeon.apiBaseUrl=https://kue200p7z3.execute-api.ap-northeast-2.amazonaws.com
```

`http://localhost:8080`으로 덮어쓰면 휴대폰에서는 그 휴대폰 자신을 가리킵니다. 팀 테스트에는 기본 배포 주소를 그대로 둡니다. `docs/` 안의 오래된 문서에 localhost가 남아 있어도, 지금 소스의 기본값과 이 APK는 배포 서버입니다.
