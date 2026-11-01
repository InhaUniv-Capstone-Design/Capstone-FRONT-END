# 테스트 현황

조사일: 2026-10-26. 테스트를 실행하지 않았고 삭제하지 않았다. 통과 여부는 미확인이다. 개수는 소스의 `@Test`다.

이전 작업 세션에서 단위 테스트 78개 통과와 계측 `GraphWebRenderTest#homeGraphChecksReachTheGraphTab` 통과가 보고된 적이 있다. 그 실행은 이 진단 차례가 아니다. 약통·하단 탭 변경 이후 계측을 다시 돌렸다는 기록은 이 조사에서 확인하지 못했다.

## 단위 테스트

경로 `app/src/test/java/com/example/yakyeon2`. JVM. 로컬 prefs와 WebView는 쓰지 않는다.

| 파일 | 메서드 | 검증하는 동작 |
| --- | --- | --- |
| `ExampleUnitTest` | `addition_isCorrect` | 템플릿 `2+2`. 제품 동작 없음. 목적 확인 전 유지 |
| `AuthValidationTest` | `loginRequiresIdAndPassword`, `signupTypeRequiresSelection`, `signupFormRequiresCompleteInput`, `signupIdAndPasswordFollowFormat`, `signupRequiresConsent`, `remoteChecksStayDisconnected` | 빈 값, 유형, 필수값, 아이디·비밀번호 형식, 동의, 원격 검사는 `NOT_CONNECTED` |
| `AuthFlowTest` | `caregiverDoesNotNeedAnAcceptedLinkBeforeTheForm`, `signupTypeWaitsForNext`, `allSignupConsentItemsAreRequired` | 대리인 선행 조건, `다음` 전 이동 안 함, 동의 세 항목 |
| `LatinCredentialFilterTest` | `filterKeepsLatinLettersAndDigitsOnly` | 입력 필터 |
| `DevHomeGateTest` | `debugBuildCanOpenHomeWithoutLogin`, `releaseBuildCannotOpenHomeWithoutLogin` | 디버그 홈 게이트 |
| `AppNavigatorTest` | `backFromSignupTypeReturnsToLogin`, `backFromSignupFormReturnsToType`, `rootTabsHaveNoBackStack`, `medicationFlowPopsOneScreen`, `historyReturnsToMedicationList` | 뒤로 가기 스택 |
| `MedicineNameCatalogTest` | `searchReturnsMatchesInCatalogOrder`, `blankQueryReturnsNothing` | 로컬 약 이름 |
| `ClockTimeTest` | `parsesHourAndMinute`, `rejectsOutOfRangeText` | 시각 파싱 |
| `RegistrationChromeTest` | `commentActionMatchesMedicationFlow`, `registrationHeaderMatchesFigmaCopy`, `historyHeaderActionOpensHistory`, `historyScreenHasNoHeaderAction` | 헤더 comment·내역 액션, 등록 문구 |
| `DevFixturesTest` | `samplesStayOutOfTheSavedState`, `resetClearsSamplesAndKeepsCommittedMedicine`, `secondFixtureReplacesTheFirst`, `scheduleFixtureCoversEveryDisplayedStatus`, `fullPillboxLeavesNoEmptySlot` | fixture 비저장, 초기화, 교체, 네 상태, 만석 |
| `DevGraphChecksTest` | `graphNodesStayEmptyUntilARealMedicineIsSaved`, `graphCheckMedicinesStaySavedAndCanBeCleared`, `oneRealMedicineHasNoLine`, `devRelationUsesRealMedicinesOnly`, `resetDoesNotDeleteGraphCheckMedicines` | 샘플 제외, 검증 약 저장·삭제, 선 조건, 초기화와 검증 약 분리 |
| `DrugRelationsTest` | `nodesFollowActiveNonSampleMedicines`, `confirmedEdgesStayEmpty`, `devScenarioLinksOnlyTheFirstTwoRealMedicines`, `graphModelCarriesLabels`, `placementIsDeterministic` | 노드, 빈 DUR, 개발용 쌍, 모델, `GraphPlacement` |
| `InMemoryYakyeonRepositoryTest` | `accountMethodsStayDisconnected` | 계정 저장소가 연결 전 |
| `MedicationRulesTest` | 아래 22개 | 복용 규칙과 등록 |

`MedicationRulesTest` 메서드:

- 용량: `tabletAmountsMatchTheFourChoices`, `capsuleAmountsMatchTheTwoChoices`, `liquidAndPowderAmountsMatchTheirRanges`
- 알람: `bedtimeSkipsMealTimingAndAlarmTime`, `fourHourGapRejectsMealAlarmsWithinFourHours`, `fourHourGapAllowsBedtimeBesideAnyMeal`, `fourHourGapChecksOnlyTheRoundsThisEnvelopeRegisters`
- 기록: `missedBecomesDelayedOnlyInsideThreeHours`, `historyUsesCompletionDelayAndMissLabels`
- 범위: `defaultHistoryIsTwoWeeksAndCannotExceedOneYear`
- 등록: `commitRequiresConfirmationAndAResolvedSlotChoice`, `commitAllowsContinuingWithoutAPillbox`, `commitRejectsAFourthSlot`, `commitStoresSeparateAlarmTimesPerEnvelope`
- 수정: `savedAlarmEditKeepsOtherEnvelopes`, `savedAlarmEditRebuildsUpcomingRecordsOnThatEnvelope`, `savedAlarmEditRejectsAGapUnderFourHours`
- 약통 문구 분기: `registrationExplainsAMissingPillbox`, `registrationExplainsAFullPillbox`, `linkedPillboxOffersOnlyEmptySlots`
- 초안: `draftSurvivesAPausedRegistration`, `committedDraftCannotBeConfirmedTwice`

`GraphPlacement`는 UI가 쓰지 않고 단위 테스트만 있다. 삭제의 근거가 되지 않는다. 사용처가 UI에 없는 것은 사실이다.

## 계측 테스트

경로 `app/src/androidTest/java/com/example/yakyeon2`.

| 파일 | 메서드 | 검증 |
| --- | --- | --- |
| `ExampleInstrumentedTest` | `useAppContext` | 패키지명 `com.example.yakyeon2`. 템플릿. 유지 |
| `GraphWebRenderTest` | `homeGraphChecksReachTheGraphTab` | 디버그 홈에서 그래프 검증 약이 그래프 탭 노드로 보이는지 |
| `GraphWebRenderTest` | `graphWebViewLoadsTheLocalDocument` | WebView가 로컬 그래프 문서를 읽는지 |

계측은 에뮬레이터와 디버그 앱이 필요하다. 이번 조사에서 실행하지 않았다. Gradle은 `connectedDebugAndroidTest`에 `--tests`가 아니라 `-Pandroid.testInstrumentationRunnerArguments.class`를 쓴다.

## 이름과 중복

같은 규칙을 단위 테스트와 계측이 동시에 깊게 보는 경우는 거의 없다. 그래프만 규칙(`DrugRelationsTest`)과 화면(`GraphWebRenderTest`)이 나뉜다. 이것은 중복 삭감 대상이 아니다.

`ExampleUnitTest`와 `ExampleInstrumentedTest`는 제품과 무관하다. 목적을 확인하기 전에는 삭제하지 않는다.

`DevFixturesTest.fullPillboxLeavesNoEmptySlot`과 `MedicationRulesTest.registrationExplainsAFullPillbox`는 만석을 보지만, 하나는 fixture 메모리이고 하나는 등록 문구다.

`savedAlarmEditRebuildsUpcomingRecordsOnThatEnvelope`는 예정 회차를 다시 만드는 현재 동작을 고정한다. 그 동작을 바꾸면 이 테스트가 먼저 실패한다. 회의로 유지 규칙이 바뀌기 전에는 실패를 고치려고 테스트를 지우지 않는다.

## 없는 테스트

| 영역 | 단위 | UI 계측 |
| --- | --- | --- |
| 약통 탭 카드, 연결 플래그, 슬롯 탭 문구, 설정 안내 | 없음 | 없음 |
| 하단 탭 아이콘·라벨·선택색 | 없음 | 없음 |
| 설정 목록, pending 화면 | 없음 | 없음 |
| 로그인·가입 레이아웃 | 검증 로직만 | 없음 |
| 일정·내역 화면 | 규칙만 | 없음 |
| 수기·약봉투·칸 선택 레이아웃 | 등록 규칙만 | 없음 |
| 촬영·OCR | 없음 | 없음 |
| Wi-Fi·약통 하드웨어 | 없음 | 없음 |
| 로그인 성공, 문자, 중복, DUR | `NOT_CONNECTED`만 | 없음 |
| 프로세스 종료 후 `yakyeon_medications` 복원 | 없음. `LocalMedicationState` 계측 없음 | 없음 |
| 하단 탭 전환 후 약통 상태 유지 | 없음 | 없음 |

## 격리

단위 테스트는 `MedicationStore`를 새로 만든다. 기기 prefs를 공유하지 않는다. fixture 테스트는 export 문자열에 `fixture-`가 없는지를 본다.

계측 `GraphWebRenderTest`는 앱 프로세스의 저장소를 건드릴 수 있다. 그래프 검증 약은 `commit`이라 기기에 남는다. 테스트가 비우기를 호출하는지는 소스의 정리 코드를 이번 표에서 실행으로 확인하지 않았다. 재실행 전 기기에 `그래프 검증` 약이 남아 있을 수 있다.

자동 테스트는 규칙과 그래프 렌더를 본다. 에뮬레이터에서 보던 약통 색, 탭 라벨, 다이얼로그 문구는 자동 테스트에 없다. 그 확인은 수동이었고 이번 조사에서 다시 하지 않았다.

## 서버 연동 후에 다시 볼 것

- `remoteChecksStayDisconnected`, `accountMethodsStayDisconnected`는 연결 전이 정상인 테스트다. 서버가 생기면 실패해야 맞는 테스트가 된다. 그 전에 지우지 않는다.
- 로그인 성공, 가입 저장, 문자, 중복, DUR 쌍, 기기 검색은 새 테스트가 필요하다. 현재 테스트가 그 성공을 증명하지 않는다.
- 그래프 개발용 선은 서버 쌍이 생긴 뒤에도 샘플·검증 약과 섞이지 않는지 따로 남아야 한다.

## 2026-10-27 처방 시나리오 테스트

파일 `ScenarioPrescriptionsTest`. 단위 테스트만이다. UI 계측은 추가하지 않았고 실행하지 않았다. 기존 테스트 파일은 수정하지 않았다. 전체 단위 테스트도 다시 실행하지 않았다.

명령:

`./gradlew :app:testDebugUnitTest --tests com.example.yakyeon2.data.ScenarioPrescriptionsTest --offline`

첫 실행은 10개 중 1개 실패했다. `manualEntryDoesNotCreateCompletedAndDoesNotChangeOtherRounds`가 약2의 같은 시각 아침을 예정으로 기대했다. 09:01은 그 회차보다 뒤라 기존 규칙상 누락이다. 약1에 넣은 지연이 약2로 옮겨 가지는 않는다. 기대값을 누락으로 고친 뒤 다시 실행했다.

둘째 실행: `BUILD SUCCESSFUL`. XML `tests="10" skipped="0" failures="0" errors="0"`. 시각 `2026-10-26T13:53:51.298Z`.

| 메서드 | 확인한 동작 |
| --- | --- |
| `knownRegistrationHasSixMedicinesAndNoDermatology` | 약 6개, 병원 2개, 피부과 없음, 시각·기간·칸 0과 1, Wi-Fi 플래그 꺼짐 |
| `dermatologyRegistrationAddsThreeMedicinesWithoutChangingTheFirstSix` | 약 9개, 기존 봉투 id·기록 수·칸 유지, 칸 2 점유 |
| `repeatingScenarioRegistrationDoesNotDuplicate` | 같은 병원 재설치는 commit하지 않음 |
| `referenceDayRoundsMatchEachMedicinesOwnSchedule` | 약1~약9의 2026-10-27 회차 |
| `statusUsesTheScheduledInstantAndTheThreeHourManualWindow` | 예정 시각 1ms 전은 예정. 정각부터 표시는 누락. 3시간 정각까지 수기 가능. 그 다음 1ms는 불가. 수기 결과는 지연이고 다시 호출해도 시각이 바뀌지 않음 |
| `manualEntryDoesNotCreateCompletedAndDoesNotChangeOtherRounds` | 완료가 생기지 않음. 같은 약의 점심은 예정. 다른 약의 같은 시각은 누락 |
| `recordsStayOnTheirCalendarDate` | 10월 27일과 28일 기록이 다른 id |
| `roundsExistOnStartAndEndDaysAndNotOutsideThePeriod` | 시작일·종료일 포함, 그 전날과 다음 날 없음 |
| `registrationLaterOnTheStartDayOmitsEarlierRounds` | 시작일 08:00 등록이면 그날 07:30만 빠짐 |
| `applySavedEditKeepsManualRecordsAndReplacesUnmarkedOnes` | 아래 결정 기록. 제품 확정이 아님 |

완료 상태를 만드는 공개 저장 경로는 없다. `MedicationStore.recordManual`은 `DoseRecord.markDelayed`만 호출한다. `COMPLETED`는 `restoreManual`로만 복원되며, 그 경로는 `DevFixtures`와 상태 코덱이 쓴다. 이 시나리오는 완료를 만들지 않는다.

## 2026-10-26 봉투 정책 테스트

파일 `EnvelopePolicyTest`. 시나리오 10개는 유지했다. 약5 기대값은 시점 하나에서 식후 30분과 취침 둘로 바뀌었다. 회차 네 개는 그대로다. 약6 기록 수는 내과 기간으로 센다. 피부과 기간으로 세던 비교는 기간이 달라 틀린 기대값이었다.

전체 단위 테스트 `./gradlew :app:testDebugUnitTest --offline`는 2026-10-26에 103개, 실패 0, 오류 0, 건너뜀 0으로 통과했다. 화면 정리 뒤 2026-10-27 00:42 UTC 근처에도 같은 103개가 실패 없이 통과했다. `./gradlew :app:assembleDebug --offline`도 그 차례에 성공했다.

에뮬레이터 `emulator-5554`에서 디버그 APK를 지운 데이터로 다시 설치한 뒤 화면을 눌렀다. 로그인에 `개발용 홈 보기`가 있고, 홈에는 개발용 버튼이 없다. 인사말을 길게 누르면 `개발용 도구`가 열린다. 시나리오 한의원·내과 뒤 홈은 6개, 피부과 추가 뒤 9개였다. 그래프는 약1부터 약9까지 노드만 있고 선은 0개였다. 약통은 `연결 안 됨`이고 칸 1 한의원, 칸 2 내과, 칸 3 피부과였다. 약5는 1일 4회, 약6은 1일 2회, 약9는 1일 1회였다. 한의원 아침을 오전 8:00으로 저장한 뒤 다시 열면 그 시각이 남았다. 어제 내역의 한의원 아침은 09:00 누락 그대로였다. 내과 칸을 다른 칸으로 고른 뒤 취소하면 칸 2가 유지됐다. 한의원에서 사용 중인 2칸은 `사용 중인 칸은 선택할 수 없습니다.`가 뜨고 선택되지 않았다. 빈 1칸 확정 뒤 목록은 `앱에 배정한 칸 1번`이었다. 재연결은 연결 해제 전에 요청이 거절되고, 거절 뒤에도 연결 해제이며, 동의 뒤에는 이 기기 표시이고 서버 전달이 아니라고 적힌다. 설정에서 알림 설정은 서버 데이터 없음으로 열린다. 와이파이 검색, 푸시, 서버 로그인은 하지 않았다.

## 2026-10-27 통합 확인

`./gradlew :app:testDebugUnitTest --offline --rerun-tasks`와 이어서 돌린 `assembleDebug`는 2026-10-27 10:54 KST에 성공했다. XML 시각은 `2026-10-27T01:54`다. 결과는 클래스 16개, 테스트 103개, 실패 0, 오류 0, 건너뜀 0이다. 회차 선택 문구를 나눈 뒤 `02:22 UTC` 결과도 103개, 실패 0이다. 테스트 메서드를 늘리지는 않았고, `missedRecordCanBeDelayedOnlyInsideThreeHours`에 정각·예정 전·3시간 후·수기 뒤 구분을 추가했다.

같은 날 `emulator-5554`에서 앱 데이터를 지우고 디버그 APK로 다시 눌렀다. 시각은 10:54~11:22 KST였다.

- 한의원·내과 설치 후 홈 6개, 재실행 후에도 유지. 피부과 추가 후 9개, 강제 종료 후 다시 9개.
- 내과 시각을 회차마다 따로 저장했다. 저장소는 아침 8:31, 점심 14:01, 저녁 19:01, 취침 23:01이었다. 그 뒤 간격 확인 중 아침 휠을 잘못 눌러 아침만 6:31로 한 번 더 저장됐다. 점심·저녁·취침 분은 그대로였다.
- 10월 26일과 10월 27일 내과 아침 기록은 08:30 그대로였다. 10월 28일 아침은 06:31, 점심 14:01, 저녁 19:01로 다시 만들어졌다.
- 한의원 수정 화면을 열고 휠을 돌리지 않고 나온 뒤 저장 분은 `540,810,1200`이고 취침 칸은 비어 있다. 한의원 `BEDTIME` 기록은 0개다.
- 약4 기록은 아침·점심·저녁, 약5는 네 회차, 약6은 아침·저녁, 약9는 저녁만.
- 11:08에 일정에서 피부과 07:30은 누락이고 `복용 시각 수정`이 없다. 내과 08:30과 한의원 09:00은 누락이지만 수정 링크가 있다. 점심 이후는 예정이다. 정각 순간은 에뮬레이터에서 맞추지 못했다.
- 한의원 칸을 비운 뒤 내과가 쓰는 2칸은 선택으로 바뀌지 않았다. 빈 1칸을 고르고 취소하면 저장 칸은 빈 값이다. 다시 1칸을 확정하면 저장 칸은 0이고 목록은 `앱에 배정한 칸 1번`이다. 재실행 후에도 세 칸이 남고 약통 화면은 `연결 안 됨`이다.
- 기록 내역을 끝까지 내리면 마지막 안내 문장이 하단 탭 위에 보인다. 스크롤 도중 행이 화면 끝에 걸치는 것은 다음 내용이 아래에 있는 상태다.
- 재연결은 연결 기록 없음에서 요청이 진행되지 않았다. 해제 뒤에만 동의·거절 창이 열렸다. 거절 후 상태는 연결 해제, 동의 후 문구는 이 기기 표시이고 서버 연동이 아니다.
- `그래프 검증 약 1개` 뒤에 병원은 한의원·내과·피부과·그래프 검증이었다. `예시 초기화`는 그 넷을 지우지 않았다. `시나리오 약 지우기` 뒤에는 그래프 검증만 남았다.
- 홈·그래프·약통·내역에서 개발용 버튼은 보이지 않았다.

4시간 미만 저장 거절의 안내 문장은 이번 에뮬레이터 조작에서 다시 캡처하지 못했다. 휠을 맞추다 다른 회차가 저장될 수 있어 그 시도는 저장 전에 중단했다. 거절 자체는 단위 테스트 `mealTimesMustBeAtLeastFourHoursAndBedtimeIsExempt`가 본다.

`GraphWebRenderTest`는 `connectedDebugAndroidTest`로 실행했다. 첫 실행은 테스트 2개 중 `committedMedicinesReachCytoscapeNodesAndEdges`가 실패했다. `homeGraphChecksReachTheGraphTab`이 먼저 그래프 검증 약 3개를 저장한 뒤, 다음 테스트가 빈 그래프를 기대해서다. `taking=3`. 그 테스트 시작에서 `clearGraphChecks`를 호출하게 고친 뒤 다시 실행하면 두 테스트 모두 통과했다. 시각은 `2026-10-27T02:27:46`. 실제 휴대폰, 서버 로그인, OCR, DUR, 푸시, Wi-Fi 검색은 실행하지 않았다.

## 2026-10-28 설정 화면

`./gradlew :app:testDebugUnitTest --offline`는 클래스 17, 테스트 108, 실패 0. `./gradlew :app:assembleDebug --offline`는 성공. 추가 파일은 `SettingsStateTest`다.

| 메서드 | 확인한 동작 |
| --- | --- |
| `pillboxSwitchOffClearsChildrenAndOnRestoresThem` | 약통 상태 알림을 끄면 하위 넷이 꺼지고, 켜면 끄기 전 값이 돌아온다. 꺼진 동안의 변경은 무시 |
| `notificationStateSurvivesExportWithoutTouchingMedications` | 알림 문자 복원. 약 저장 문자열은 그대로. 약통 연결 플래그는 꺼짐 |
| `shareKeepsCurrentMedicinesAndOnlyKnownPeriods` | 현재 복용 약은 항상 포함. 7·30·90일만 저장. 14일은 거절 |
| `legalMarksRoundTripAndDoNotConnectAPillbox` | 복약 정보 수집과 서비스 이용 고지 표시가 복원된다. 약통 연결은 켜지지 않음 |
| `disconnectingThePillboxKeepsSlotsAndRecords` | 연결 플래그만 꺼진다. 한의원 칸, 기록 수, 약 저장 문자열은 그대로 |

에뮬레이터 `emulator-5554`에 디버그 APK를 덮어 설치했다. 데이터는 지우지 않았다. 시작 홈은 복용 0개였다. `개발용 홈 보기` 뒤 인사말을 길게 눌러 `시나리오 한의원·내과`를 넣었고 홈은 `현재 6개`가 됐다. 피부과 추가는 이 조작에서 목록에 반영되지 않았다.

확인한 흐름:

- 설정 네 행을 열고 뒤로 가면 설정 목록이다. 대리인 행은 없다.
- 프로필에 `김약연`이 없고, 로그아웃 확인 뒤에도 프로필에 남는다.
- 알림은 처음에 모두 꺼져 있다. 기기 알림 권한이 꺼져 있다는 문장이 보인다. 복용 시간을 켜고, 약통 상태를 켠 뒤 약통 열림만 켜면, 약통 상태를 끄면 하위 스위치가 잠기고 꺼진다. 다시 켜면 약통 열림만 켜진다.
- 강제 종료 후 `개발용 홈 보기`로 들어오면 홈은 여전히 6개다. 알림 스위치는 복용 시간과 약통 상태가 켜져 있다. prefs `notifications`는 `100110001000`, `share`는 `7,1110`, `legal`는 `100000`이다.
- 공유 기간을 최근 7일로 바꾸면 재실행 후에도 최근 7일이다. 미리보기는 약1–약6, 이 기간 기록 126건, 병용 0건, 이미지 파일을 저장하지 않았다고 적는다.
- 이용약관 철회를 취소하면 문구가 그대로다. 철회하면 `이 기기에서 철회 표시됨`이 되고 약 6개는 남는다. 약통 안내 본문은 와이파이다.
- 약통은 `연결 안 됨`, 한의원 1칸, 내과 2칸, 3칸은 비어 있다. 약5는 1일 4회, 약6은 1일 2회. 약통 설정에 식별 번호와 `연결됨`이 없다. 기기 찾기는 0개이고 다시 검색은 `기기를 찾지 못했습니다.`
- 저장 칸은 한의원 0, 내과 1. 아침 시각은 한의원 540, 내과 510이다.
- 그래프 탭은 서버 미연결과 관계 0건을 보여 줬다. WebView 안 노드 픽셀은 접근성 덤프로 읽지 못했다. 내역에는 한의원 09:00, 내과 08:30 누락이 남아 있다.
- 연결 플래그가 켜진 약통 설정의 부저, LED, 연결 해제, 연결 전 안내는 이번 에뮬레이터에서 열지 않았다. 연결 해제 후 칸이 남는 것은 단위 테스트만 본다.
- 비밀번호 변경, 회원 탈퇴, 공유 토글 개별 변경, 기간 달력, QR, 약통 연결 동의는 실행하지 않았다.

| 메서드 | 확인한 동작 |
| --- | --- |
| `medicineFiveKeepsTwoTimingsWithoutAddingAFifthRound` | 약5 시점 둘, 회차 넷, 기간 일수만큼만 기록 |
| `dermatologyMedicineNineCreatesOnlyTheEveningRound` | 약9는 저녁만. 약6은 내과 기간의 아침·저녁 |
| `timingsSurviveExportAndASingleStoredTimingStillRestores` | 약5 복수 시점과 약1·약4 단일 시점 복원 |
| `envelopeTimeEditMovesFutureRoundsAndLeavesTheOtherEnvelope` | 예정 시각만 새 시각. 당일 누락은 예전 시각. 내과 기록 유지. 저장 후 복원 |
| `manualDelayedRecordStaysWhenItsEnvelopeTimeChanges` | 지연 객체와 시각 유지 |
| `openingTheEditWheelsDoesNotStoreAnUnusedBedtime` | 사용하지 않는 취침은 표시만 하고 저장하지 않음. 저장 후에도 취침 키와 기록이 없음 |
| `pendingSlotStaysChosenAcrossRefreshUntilConfirm` | 확정 전 대기 칸 유지. 다른 봉투 칸은 배정 실패. Wi-Fi 플래그 꺼짐 |
| `storedUnusedBedtimeMinuteDoesNotCreateClinicRounds` | 넣어 둔 취침 시각은 한의원 기록을 늘리지 않음 |
| `slotAssignmentRejectsASlotAnotherEnvelopeUses` | 사용 중 칸 거절 |
| `cancellingSlotAssignmentKeepsThePreviousSlot` | 취소 후 칸·약·기록 유지 |
| `confirmingSlotAssignmentStoresTheSlotWithoutConnectingWifi` | 확정은 칸만 저장 |
| `clearingPillboxUseKeepsMedicinesAndRecords` | 약통 해제도 약과 기록 유지 |
| `reconnectStaysUnlinkedUntilAcceptedAndRejectKeepsDisconnected` | 동의 전 미연결, 거절 시 해제 유지 |
| `scenarioInstallLeavesAnInProgressDraftUntouched` | 작성 중 초안을 덮지 않음 |
| `removeScenarioLeavesGraphCheckEnvelopes` | 시나리오만 지우고 그래프 검증 약은 남음 |
