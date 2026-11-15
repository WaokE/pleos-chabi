# 차비 개발 및 차량 데이터 연동

현재 앱은 Mock 데이터로 완성하는 MVP입니다. 실제 Trip/시동/GPS 이력 API가 제공된다고 가정하지 않습니다.

## 환경

JDK 17 / Gradle 8.13 / AGP 8.13.2 / Kotlin 및 Compose Compiler 2.2.20 / Compose BOM 2025.10.01을 사용합니다. 기존 Android Studio와 SDK를 사용하며 별도의 시스템 설치는 필요 없습니다.

```bash
./scripts/setup-local.sh
./scripts/check.sh
```

Gradle 데몬은 `gradle/gradle-daemon-jvm.properties`로 JDK 17을 고정합니다. SDK Platform 36과 Build Tools 35.0.0이 필요합니다. target SDK 34는 현재 Pleos API 34 검증 환경을 기준으로 유지했으며 출시 정책은 별도로 확인합니다.

## 실행 환경

Device Manager의 `Pleos_Connect_2_1_API_34`를 사용합니다. Cold Boot / RAM 4096MB / GPU host로 구성되어 있습니다. 시스템 Driving view를 닫거나 가리는 코드는 없습니다.

터미널 실행 보조 스크립트는 선택 사항입니다.

```bash
./scripts/run-emulator.sh
# 부팅 후
./scripts/install-debug.sh
```

여러 에뮬레이터가 실행 중이면 `ANDROID_SERIAL=emulator-5554 ./scripts/install-debug.sh`처럼 대상 기기를 지정합니다. 스크립트는 debug APK를 빌드하고 선택한 기기에 설치한 뒤 앱을 실행합니다. 제출 APK의 위치와 상태는 [APK 안내](../apk/README.md)에 있습니다.

다른 Apple Silicon Mac에 이미지를 설치하려면 Android Studio SDK Manager → SDK Update Sites에 아래 공식 URL을 추가한 뒤 API 34의 Pleos Connect 이미지를 설치하고, Automotive → Pleos Connect v2.1.0 기기로 AVD를 생성합니다.

`https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`

이 Mac의 이미지는 Android SDK가 인식하는 `~/Library/Android/sdk/system-images/android-34/android-automotive/arm64-v8a/`에 있습니다. Windows/Linux는 공식 문서의 해당 아키텍처 이미지를 사용합니다.

## 데이터 경계

`core` 모듈의 `TripRepository` 계약:

- `source`: 예시 운행과 실제 차량 데이터를 구분합니다. 현재 예시 운행 화면에서는 출처 칩을 표시하지 않습니다.
- `getTrips()`: 접근 가능한 운행 기록 목록.
- `getTripDetail(id)`: 개별 운행의 상세 기록 또는 null.

현재 구현체는 `data-local` 모듈의 `MockTripRepository`뿐입니다. 화면, 도메인, 정산 계산에는 차량 SDK 타입이 등장하지 않습니다. SDK/API 지원 여부가 확정되면 새 데이터 어댑터 모듈을 작성하고 `ChabiApplication.tripRepository` 한 곳의 의존성 바인딩을 교체합니다.

실제 어댑터 구현 시 확인할 사항:

1. 과거 운행 이력 조회가 실제 지원되는지와 접근/승인 조건.
2. Trip 구분 기준과 안정적인 고유 ID. 시동 ON/OFF라고 임의로 가정하지 않기.
3. 시작·종료 시간의 타임존을 일관되게 변환. 현재 화면은 Repository에서 정규화한 현지 시각을 표시.
4. 거리는 정수 m, 소비량은 L 또는 kWh, 연비는 km/L 또는 km/kWh로 변환.
5. 소비량이 없으면 null. 0과 미제공을 구별.
6. 취득 가능한 값만 매핑하고 미지원 기능은 실패/미지원으로 반환.
7. 새 데이터 소스에서 저장된 Trip ID가 사라지면 조용히 계산에서 빼지 않고 사용자가 선택을 다시 확인하도록 처리.

차량 이력·인증·차량 권한은 실제 구현체를 추가할 때 필요한 범위만 도입합니다. 지도 표시에는 MapLibre Native OpenGL 13.7.1, OkHttp 4.12.0과 지도 모듈의 INTERNET/ACCESS_NETWORK_STATE 권한을 사용합니다.

## 차량 기본값과 단가

`DriveDefaultsRepository`는 차량 기본값과 에너지별 기준 단가를 제공합니다. 현재 Application의 바인딩은 `MockDriveDefaultsRepository`입니다. 실제 차량/가격 소스를 도입할 때 이 경계를 교체하며, UI와 계산 계층에 SDK 타입을 전파하지 않습니다. 구현은 `data-local`, 계약은 `core`에 있습니다. 단가 출처·기준일은 초안에 저장합니다. 재실행 시 새 기본값으로 덮어쓰지 않습니다.

가격 API와 실제 차량 연료 종류 자동 조회는 아직 구현하지 않았습니다. 상세 결정은 [UX_FLOW.md](UX_FLOW.md)에 기록했습니다.

## 정산과 입력 검증

`core` 모듈의 `domain/` 계산은 Android와 SDK에 독립적입니다. 소비량과 소수 단가를 Decimal로 곱한 후 원화 반올림을 한 번, 비용 분할은 Long 정수 몫/나머지로 처리합니다. 미참여자는 해당 비용을 부담하지 않습니다.

유효하지 않은 연비·단가·금액, 중복 비용 ID, 빈 참여 집합, 사라진 운행, 빈/중복 이름과 한도 초과 금액은 최종 정산을 만들지 않습니다. 대소문자나 앞뒤 공백만 다른 이름도 중복으로 봅니다. 최대 총액은 999,999,999,999원이며 UI에서 추가 지출 한 건은 999,999,999원까지 입력합니다.

이 한도와 판정은 `core/domain/SettlementRules.kt`의 상수·함수로 정의하며 계산기와 화면이 같은 값을 참조합니다. 값을 바꿀 때는 이 파일만 수정합니다.

비용별 기본 분담은 현재 전체 참가자입니다. 일부 참여를 명시하면 그 ID 집합을 보존합니다. 인원을 줄여 집합이 비어도 전체 참여로 바꾸지 않습니다.

## 초안 저장

`DraftStore`는 suspend 계약이며 `data-local/LocalDraftStore`가 앱 전용 SharedPreferences에 버전 1의 JSON 초안 하나를 저장합니다. 읽기·JSON 변환·디스크 쓰기는 주입한 IO dispatcher에서 수행합니다. ViewModel의 단일 저장 큐로 쓰기 순서를 보장하고 디스크 쓰기 실패는 사용자에게 알립니다. 선택 Trip ID, 연료 프리셋 ID, 연비·단가·출처·기준일, 추가 경비, 참가자와 분담 설정, 마지막 단계와 확정한 카드 결과를 보존합니다. 기존 초안에 마지막 단계나 카드 결과가 없으면 해당 값은 비워 둡니다. 정산 결과는 저장한 초안과 Repository 데이터에서 다시 계산합니다.

## 앱 내부 지도

`ui/EmbeddedRouteMap`은 `RouteMapProvider/RouteMapSession` 계약에만 의존해 Pleos 조작 UI와 수명주기를 연결합니다. SDK View/레이어/카메라와 HTTP 설정은 별도 `route-map-maplibre` 모듈 내부에 격리했습니다. `TripMapScene`은 선택/확인 상태와 유효한 좌표만으로 공급자 중립 표시 모델을 만듭니다. 구현 모듈의 `MapRuntime`은 앱 식별 User-Agent, 요청 동시성 4, 50MB HTTP 캐시를 설정합니다. 주야간에는 OpenFreeMap Liberty/Dark 스타일을 사용합니다.

Mock 경로는 assets의 `mock_routes.geojson`에서 읽고 Repository를 통해 제공합니다. UI는 SDK 경로 타입에 의존하지 않습니다. 지도 로딩과 정산 계산은 분리되어 있어 지도 네트워크 실패가 비용 계산을 변경하지 않습니다. [상세·출처](EMBEDDED_MAP.md)

외부 순정 앱 전환 방식은 목록 옆 지도 UX와 맞지 않아 사용하지 않습니다. [NaviHelper 조사](NAVI_SDK_FINDINGS.md)가 API 범위의 근거입니다.

## 후속 범위

실제 차량 데이터 API·권한, 실차의 규제 및 창 전환, 앱 패키지명 확정과 release 서명, 비율/지정 금액 분담은 별도로 진행합니다.

현재 앱은 DO를 선언하지 않습니다. `platform/CarUxMonitor`가 AAOS `CarUxRestrictionsManager`를 구독하고, DO가 요구되면 조작 화면을 주차 안내로 교체합니다. 처음 연결 중이거나 상태 조회에 실패한 경우에도 입력을 차단합니다. Activity가 화면에서 사라지면 구독을 해제하고 복귀 시 다시 확인합니다. 이 기능은 TripRepository 및 Pleos SDK와 독립적이며 Android SDK의 기본 `android.car` 공유 라이브러리만 사용합니다. API 34에서 검증하며, AAOS API 29 미만은 상태 확인 불가로 처리합니다.

## UI 구현 기준

[Pleos UI 가이드 적용 문서](PLEOS_UI_GUIDE.md)에 공식 출처, 색·타입·컴포넌트 크기, 반응형 분기와 검증 범위를 정리했습니다. `ui/Design.kt`의 토큰·`PleosBreakpoints`와 `ui/PleosComponents.kt`의 래퍼를 사용하고 새로운 화면에 Material 기본 스타일을 직접 추가하지 않습니다. 자체 캐릭터·아이콘은 `ui/DriveIllustrations.kt`에서 그리며, 화면별 적용 결정은 [디자인 방향](DESIGN_DIRECTION.md)을 따릅니다.

## 공식 참고

- [Pleos 개발 환경](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- [Pleos 화면 모델](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)
- [주행 규제](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom)
- [Compose Compiler](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
- [Android 아키텍처](https://developer.android.com/topic/architecture/recommendations)

## 아키텍처 경계 검사

Repository/초안 저장 계약은 순수 Kotlin `core`에 있고 ViewModel은 이를 생성자에서 명시적으로 주입받습니다. ViewModel은 Mock 기본 구현을 갖지 않습니다. 지도 SDK는 `route-map-maplibre` 안에만 두며 `verifyArchitecture`가 모듈 의존성과 금지된 소스 참조를 검사합니다. 앱 `preBuild` 및 각 모듈 `check`에 연결되어 있습니다. [교체 절차와 모듈 설명](ARCHITECTURE.md)

## 결과 화면과 검증 진입점

결과 화면의 1/N 정산 버튼은 발급된 카카오페이 링크를 `QrEncoder`에 전달합니다. `handoff-zxing`이 링크 QR을 생성하고 UI는 중립 매트릭스를 그립니다. 금액과 참가자 정보는 로컬에 남으며 휴대폰에서 링크를 엽니다. 버튼 자산은 [공식 Regular 버튼](https://developers.kakaopay.com/docs/moneytransfer/splitbills.link/splitbills-link-hands-on#design-guide)입니다.

카드 뽑기에서 당첨 카드가 열리면 남은 카드를 공개하고 `res/raw/card_draw_confetti.json`을 Lottie Compose 6.7.1로 한 번 재생합니다. 사용자가 결과를 확정하면 ViewModel이 초안에 저장합니다.

로컬과 CI는 `scripts/check.sh`를 공통 진입점으로 사용합니다. CI는 JDK 17과 Android Platform 36/Build Tools 35.0.0을 준비하며 에뮬레이터 없이 단위 테스트·구조 검사·Lint·debug APK를 검증합니다. 실제 차량/에뮬레이터 UI 검증을 대체하지 않습니다. [checkout 공식 사용법](https://github.com/actions/checkout), [setup-java 공식 사용법](https://github.com/actions/setup-java).
