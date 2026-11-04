# NaviHelper SDK 실물 및 실연동 검증

> 이전 0.3.1 실험 기록입니다. 외부 앱 전환은 요청한 UX와 달라 0.4에서 시험 UI·SDK·권한을 제거했습니다. 현재는 [앱 내부 지도](EMBEDDED_MAP.md)를 사용합니다. 아래 구현/권한 설명은 실험 당시 기준입니다.

확인일: 2026-11-04. 앱 0.3.1-navi-test / Pleos Connect 2.1.0, Android API 34 ARM64 에뮬레이터.

## 결론

**NaviHelper 3.0.5로 순정 Navigation 앱 전면 표시, 현재 안내 경로 전체 보기, 예상 Trip 정보 조회, 새 길 안내 및 안내 종료가 실제로 동작했습니다.**

화면은 정산 앱 내부 지도 영역에 삽입되는 방식이 아니라 `ai.umos.maps.android.navigation.app/.MainActivity`로 전환됩니다. 과거 운행 GPS 궤적을 정산 목록 옆 지도에 겹쳐 그리는 기능을 검증한 것은 아닙니다.

## 구현과 실행

개발 debug APK에만 `ai.pleos.playground:NaviHelper:3.0.5`와 아래 권한을 포함합니다.

- `pleos.car.permission.NAVI_ROUTE`
- `pleos.car.permission.NAVI_ROUTE_SEARCH`
- `pleos.car.permission.NAVI_CUSTOM_MAP`

운행 선택 → **순정 내비 연동 확인**에서 서비스에 연결합니다. 일반 버튼은 순정 앱 열기와 현재 안내 경로 전체 보기입니다. **연동 시험 (개발용)**을 펼치면 선택한 예시 도착지까지의 예상 Trip 조회, 새 길 안내 시험 및 이 앱이 만든 안내 종료를 사용할 수 있습니다.

새 길 안내 시험은 현재 경로 상태가 확인됐고 활성 안내가 없을 때만 허용합니다. 별도 확인 창에서 기존 이력의 재생이 아니라 현재 위치에서 새 경로를 요청한다는 점을 안내합니다. 종료 버튼은 이 앱이 요청한 목표 좌표와 실제 목적지 콜백을 대조해 시험 경로임을 확인한 경우에만 활성화합니다. Activity가 파괴되면 SDK 리스너와 연결을 해제합니다.

`NavigationGateway` 경계에는 SDK 타입이 없습니다. `src/debug`만 SDK 구현을 사용하며 `src/release`는 시험 UI를 숨기는 구현입니다. 최종 debug APK의 DEX에는 NaviHelper가 있고 release APK에는 없는 것을 확인했습니다. `NAVI_CUSTOM_MAP` 등의 권한도 release merged manifest에는 없습니다. 정산 도메인과 Trip/단가 Repository는 변경하지 않았습니다.

## 실제 관찰 결과

| 항목 | 결과 |
| --- | --- |
| 초기 연결 | `initialize(onConnected, onFailed)`의 연결 성공 콜백 수신 |
| 현재 경로 상태 | 초기 `SAFE_GUIDING`, 탐색 false, 활성 길 안내 없음 |
| 순정 앱 표시 | `Foreground` 호출 후 실제 전면 패키지가 순정 Navigation 앱으로 전환 |
| 예상 Trip | 현재 위치에서 예시 춘천 도착지까지 거리 **101,547m**, 원시 ETA **5556** 콜백 수신 |
| 새 길 안내 | 요청 약 8초 후 경로 시작 콜백과 `ROUTE_DRIVE` / `ROUTE_GUIDING` 상태 수신. 순정 화면에서 **101km / 약 1시간 33분 / 춘천** 표시 |
| 전체 보기 | `OverviewRoute(RouteOverviewShowInfo(durationMs = 15000))` 호출 후 실제 경로 전체와 회전 안내 목록 표시 |
| 안내 종료 | 종료 요청 후 `onRouteCancelled`, `FREE_DRIVE`, `SAFE_GUIDING` 복귀 확인 |
| 앱 복귀 | 하드웨어 Back만으로 자동 복귀하지 않음. 시스템 도크의 Drive Split 아이콘으로 복귀 가능 |
| 화면 유지 | 최초 표준 실행 모드에서는 도크 복귀가 새 Activity를 만들었음. `singleTask` 적용 후 동일 연결 팝업·운행 선택 화면 복귀를 반복 확인 |
| 정산 데이터 | 기존 Mock 98km / 3인 / 예상 연료비 13,328원 유지. 시작 전 초안 JSON과 종료 후 초안 JSON이 동일함 |

SDK 요청 메서드가 반환하는 시점에 처리 완료로 간주하지 않습니다. 초기 시험의 빠른 UI 검사는 아직 응답 전이어서 실패했고, 실제 콜백 및 후속 화면을 확인한 뒤 성공 여부를 판단했습니다. 확장된 팝업에서는 연결 문구가 본문 스크롤 밖으로 가려져 UI 검사에서 누락될 수 있어, 복귀 확인에는 고정 팝업 제목과 실제 전면 Activity를 사용했습니다. 경로 취소도 순정 앱을 전면으로 표시하는 동작이 관찰됐습니다.

예상 101,547m는 **현재 위치에서 새로 탐색한 예상 거리**이며, Mock 과거 운행 98km를 대체하지 않습니다. 조회한 예상 거리·ETA·소비량을 정산 초안에 쓰지 않습니다.

## 지도와 데이터의 한계

- 공개 NaviHelper 인터페이스에 앱 안에 붙일 MapView/Surface 반환이나 임의 GPS 폴리라인 추가 API는 확인되지 않았습니다. 내부 `updateOccupiedRegion(Rect)`를 공개 지도 컴포넌트처럼 사용하지 않았습니다.
- `TripRequest`의 입력은 목적지 좌표·POI 정보입니다. 날짜별 과거 운행 목록·Trip ID별 기록 GPS 궤적 조회 API는 확인한 공개 인터페이스에 없습니다.
- 에뮬레이터의 순정 지도에서 경로선과 회전 안내는 표시됐지만 도로·지명 등 배경 레이어는 일부 누락됐습니다. 관련 로그에 지도 HTTP 401, GL 및 스타일 이미지 경고가 있었으나 단일 원인으로 확정하지 않았습니다.
- 예시 좌표와 빈 POI ID로 경로 생성은 성공했습니다. 별도의 POI 상세 조회에는 오류가 관찰됐으므로 실제 데이터 연결 시 유효한 POI ID·공급자 정보 정규화가 필요합니다.
- 이 시험으로 과거 운행을 지도에 표시하는 최종 UX가 완성된 것으로 간주하지 않습니다. 그 구성이 필요하면 별도의 앱 내부 지도 렌더러와 실제 기록 경로 공급원이 필요합니다.

## 산출물과 검증 범위

`assembleDebug`, `testDebugUnitTest` 32개, `lintDebug`, `assembleRelease` 성공. Lint 오류 0개. release는 서명되지 않은 개발 확인용 산출물이며 배포하지 않았습니다.

실제 화면과 콜백 로그는 Git 제외 `.local/navi-qa/`에 보관합니다. 대표 이미지는 `native-route-overview.png`, `final-native-overview.png`, `final-native-cancelled.png`입니다. 이전 운행·참가자·단가를 그대로 보존했고 시험 길 안내는 종료했습니다. 차량은 P / 0km/h 상태를 유지합니다.

## 공식 참고

- [SDK 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
- [배포 및 의존성](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk)
- [초기화와 해제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release)
- [이벤트 리스너](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners)
- [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions)
- [목적지 요청과 콜백 확인](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations)

이전 `/api-reference/connect-sdk-pleos/…` 경로는 HTML meta refresh로 `/docs/pleos-only/connect-sdk-pleos/…`를 안내하는 경우가 있었습니다. 원래 주소의 본문이 비었다고 API가 없다고 판단하지 않아야 합니다. 공식 AAR 및 의존 모델은 `.local/navi-research/`에 별도로 보관해 공개 인터페이스도 확인했습니다.
