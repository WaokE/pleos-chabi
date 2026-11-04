# 차비

**같이 탄 차비, 운행 기록으로 나눠요.**

> 앱 이름 **차비**와 패키지명 `com.example.chabi`는 가칭입니다. 확정되면 `strings.xml`의 `app_name`, `settings.gradle.kts`의 `rootProject.name`, 각 모듈의 `namespace`와 `applicationId`, Kotlin 패키지를 함께 바꿉니다.

Pleos Connect 차량 운행 기록을 정산 대상으로 골라 여행 경비를 나누는 Kotlin + Jetpack Compose MVP입니다. 현재 운행 기록은 **모두 Mock 예시**이며 화면과 복사/공유 결과에도 표시됩니다. 실제 차량 운행 이력 API는 현대자동차 회신 및 지원 범위 확인 후 연결합니다.

## 구현된 흐름

1. **홈**: 새 정산 또는 저장한 입력 이어서 하기.
2. **운행 선택**: 날짜별·개별 다중 선택, 경로 미리보기, 거리·예상 비용 합산 후 선택 확정.
3. **비용·인원 확인**: 기본값으로 계산한 차량 비용과 운전자 포함 인원 선택. 이름 입력 없이 진행.
4. **결과**: 1인 부담금과 총합, 오프라인 텍스트 QR·복사·Android 공유.

연료 종류·연비·전비·단가 변경, 통행료·주차비 추가, 이름 변경과 계산 명세는 선택 기능으로 접어두었습니다. 기타 지출을 새로 추가하는 메뉴는 없으며, 초안에 저장된 기타 지출·부분 참여 설정은 유지합니다.

운행·차량 기본값·단가·경로 좌표는 **Mock 예시**입니다. 우측 지도는 MapLibre Native + OpenFreeMap의 실제 도로·지명을 표시하며 선택한 예시 경로를 겹쳐 그립니다. 실제 차량 자동 판별과 현재 평균가 조회는 아직 연결하지 않았습니다. 선택과 계산 기준은 로컬 초안으로 저장됩니다. 송금·계정·클라우드 기능은 포함하지 않습니다.

[UX 결정 및 데이터 연동 경계](docs/UX_FLOW.md) · [디자인 방향 · 0.6](docs/DESIGN_DIRECTION.md)

## 실행

Android Studio에서 이 폴더를 열고 Gradle Sync 후 Device Manager의 **Pleos_Connect_2_1_API_34**를 실행하고 ▶ Run을 누릅니다. 기본 차량 Driving view는 시스템 고정 영역이며 앱은 오른쪽 앱 영역을 사용합니다.

터미널을 선호하면:

```bash
./scripts/setup-local.sh
./scripts/doctor.sh
./scripts/check.sh
./scripts/run-emulator.sh
# 에뮬레이터 부팅 후 다른 터미널
./scripts/install-debug.sh
```

실행 중인 에뮬레이터가 있다면 다시 켤 필요 없이 설치만 합니다. 여러 기기에서는 `ANDROID_SERIAL=emulator-5554`를 지정합니다.

- 개발 APK: `app/build/outputs/apk/debug/app-debug.apk`
- 도메인 테스트 보고서: `core/build/reports/tests/test/index.html`
- 데이터 테스트: `data-local/build/reports/tests/testDebugUnitTest/index.html`
- QR 테스트: `handoff-zxing/build/reports/tests/test/index.html`
- 앱 단위 테스트 보고서: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint 보고서: `app/build/reports/lint-results-debug.html`
- 제출용 release APK: 서명 설정 후 `apk/팀명_차비_release.apk`로 별도 준비. 현재 APK는 개발용입니다.

## 시연

홈에서 새 정산을 시작하고 11월 1~2일의 4개 운행을 선택하면 **415km**입니다. 기본 연비 12.5km/L, 예시 단가 1,700원/L에서 **56,440원**, 운전자 포함 4명이 **각 14,110원**을 부담합니다. 키보드 입력 없이 결과와 QR까지 진행할 수 있습니다.

3명이면 **18,814 / 18,813 / 18,813원**, 5명이면 **각 11,288원**이며 합계는 같습니다. 통행료·주차비는 필요한 경우에만 추가합니다.

[Mock 시연 가이드](docs/MOCK_DEMO.md) · [검증 결과](docs/VERIFICATION.md) · [Pleos UI 적용 기준](docs/PLEOS_UI_GUIDE.md)

## 차량용 UI

토스의 명확한 금액 계층, 마이루틴의 선택 피드백, 하루콩의 친근한 표정을 참고해 초록색 중심의 UI와 자체 자동차 캐릭터를 적용했습니다. 공식 Foundations의 회색 팔레트와 Asta Sans, 주야간 테마를 유지합니다. Material 3의 버튼·입력·체크·팝업은 Pleos 크기와 상태에 맞춰 조정했습니다. 작은 창은 한 열로 전환하고 선택 거리와 예상 정산액을 하단에 유지합니다. 주행 제한 시 모든 정산 조작을 차단하며 정차 복귀 시 입력 중인 폼을 이어갑니다.

## 정산 규칙

- 금액은 `Long` 정수 원, 소비량·연비·단가는 `BigDecimal`을 사용합니다. 금액 계산에 `Double`을 사용하지 않습니다.
- 운행별 소비량이 있으면 우선 사용합니다. 없으면 거리 ÷ 연비/전비로 추정합니다. 서로 다른 에너지 단위는 혼용하지 않습니다.
- 전체 소비 비용을 합한 뒤 **HALF_UP으로 원 단위 반올림을 한 번** 합니다.
- 비용마다 해당 참여자들만 균등하게 나누고, 남은 원은 **참가자 등록 순서대로 1원씩** 배분합니다.
- 전체 참가자는 `null`, 명시적으로 선택한 일부는 ID 집합입니다. 빈 집합을 전체로 처리하지 않습니다.
- 참가자 삭제 후 해당 비용의 참여자가 없어지면 정산을 막고 재선택을 요청합니다.
- 수정은 동일 비용 ID를 교체합니다. 시연 비용은 반복 추가되지 않습니다. 서로 다른 영수증은 같은 금액이어도 별개의 비용으로 등록할 수 있습니다.
- 에너지 소비 비용과 주유·충전 결제액은 다를 수 있습니다. 추가 지출에 같은 비용을 다시 넣지 않도록 안내합니다.

## 구조와 SDK 교체

```text
core/                 domain: 정산·초안 규칙·계약 / format: 출력 / handoff: QR 계약
data-local/           Mock 운행·차량·단가 / 로컬 초안·JSON / IO 처리
handoff-zxing/        QR 생성 구현 (ZXing 타입 격리)
route-map-api/         SDK 중립 지도 계약·표시 모델·상태·출처
route-map-maplibre/    MapLibre/OpenFreeMap 구현·네트워크·SDK 수명주기
app/.../ui/           화면·ViewModel·Pleos 지도 조작 UI
app/ChabiApplication  구현체 주입 지점
```

정산 계산과 Mock 운행 Repository는 차량 SDK와 독립적입니다. `Trip.route` 좌표를 앱 내부 MapLibre 지도에 표시하며, 큰 창에서는 목록 왼쪽·지도 오른쪽 배치를 유지합니다. 선택 경로 전체 맞춤, 확대·축소, 경로를 눌러 확인하기와 주야간 지도를 제공합니다. 지도 타일만 네트워크로 가져오며 초안과 경로 좌표를 서버에 업로드하지 않습니다. [지도 구현과 데이터 경계](docs/EMBEDDED_MAP.md) · [아키텍처와 SDK 교체 절차](docs/ARCHITECTURE.md)

NaviHelper로 외부 순정 앱을 띄우는 방식은 목록 옆 지도 UX와 맞지 않아 사용하지 않습니다. 조사 내용은 [NaviHelper 한계](docs/NAVI_SDK_FINDINGS.md)에 있습니다. 순정 내비의 지도 화면을 내장한 것으로 표시하지 않습니다. 조작 가능 상태는 AAOS 기본 `android.car`로 확인합니다.

지원 API가 확정되면 `TripRepository`를 구현하고 `ChabiApplication.tripRepository` 바인딩을 교체합니다. Repository가 반환하는 시간·거리(m)·소비량(L/kWh)을 표준 모델로 변환하고 `source`를 지정하면 계산·정산 코드를 바꿀 필요가 없습니다. [개발/연동 문서](docs/DEVELOPMENT.md)에 정리했습니다.

## 검증

`./scripts/check.sh` 한 번으로 아키텍처 경계 검사, 전체 단위 테스트, debug APK 빌드와 Android 모듈 Lint를 실행합니다. GitHub Actions도 같은 스크립트를 실행하도록 구성했습니다. 공용 Maven 의존성만 사용하며 차량 SDK 키나 인증 정보는 필요 없습니다. 원격 CI 실행 결과는 저장소 업로드 후 확인해야 합니다.

## 환경 및 한계

| 항목 | 설정 |
| --- | --- |
| Kotlin / Compose Compiler | 2.2.20 |
| Compose BOM | 2025.10.01 |
| Activity / Lifecycle | 1.11.0 / 2.9.4 |
| JDK / Gradle / AGP | 17 / 8.13 / 8.13.2 |
| Android | min 28 / compile 36 / target 34 |
| 검증 대상 | Pleos Connect 2.1.0 ARM64, API 34 |
| Application ID | `com.example.chabi` — 가칭. 확정 전까지 사용하는 임시 패키지명 |

주차 후 조작하는 MVP이며 운전 중 사용 최적화(DO)는 선언하지 않았습니다. Android Automotive의 `CarUxRestrictions`를 별도 어댑터로 구독해 주행 제한 중에는 입력 화면을 차단합니다. 차량 상태를 확인할 수 없을 때도 입력을 허용하지 않습니다. 실제 차량 적용 전에는 규제·창 크기 전환·지원 API와 권한을 재검증해야 합니다. 비율/지정 금액 분담, 실제 Trip 수집, 이력 관리 및 release 서명은 후속 범위입니다.

공모전 일정과 제출 준비는 [공모전 메모](docs/COMPETITION.md)를 참고합니다. `local.properties`, `.local/`, 빌드 출력, 서명 키는 Git에서 제외됩니다.
