# 차비

**같이 탄 차비, 운행 기록으로 나눠요.**

차비는 친구나 가족과 함께 다녀온 운행을 골라 연료비·충전비와 통행료·주차비를 나누는 **Pleos Connect용 차량 앱**입니다. 운전자는 여행 구간을 지도에서 확인하고, 거리와 연비·전비를 바탕으로 계산한 예상 비용을 동행과 함께 확인할 수 있습니다.

## 이용 흐름

1. **운행 선택** · 날짜별 또는 개별 운행을 고르면 지도에 경로가 표시되고, 총 거리와 예상 비용이 갱신됩니다.
2. **비용과 인원 확인** · 차량 비용을 확인하고 필요하면 적용 단가나 연비·전비를 바꿉니다. 통행료·주차비는 해당 비용을 부담할 사람에게만 나눌 수 있습니다. 기본 정산은 이름 입력 없이 인원수만 고르면 됩니다.
3. **정산 결과** · 예상 총액과 사람별 부담금을 확인합니다. 비용별로 균등하게 나누고 남은 원은 등록 순서대로 1원씩 배분합니다.
4. **카드 뽑기와 카카오페이** · 한 사람이 전액을 내기로 했다면 돌아가며 카드를 고릅니다. 당첨 카드가 나오면 나머지 카드가 함께 뒤집히고 결과를 확정할 수 있습니다. 카카오페이 1/N 정산 버튼을 누르면 휴대폰으로 스캔할 링크 QR이 표시되며, 정산 금액은 카카오페이에서 직접 입력합니다.

차량이 정차한 상태에서 조작하도록 설계했습니다. 주행 제한이 걸리면 정산 화면 조작을 차단합니다. 초안은 기기에 저장하며 앱을 다시 열면 마지막 단계부터 이어갈 수 있습니다.

## 구현 범위

- **차량 화면** · Pleos Connect의 화면 배치와 자동·라이트·다크 모드를 지원합니다. 차량의 주행 제한 상태는 Android Automotive의 `CarUxRestrictions`로 확인합니다.
- **운행 데이터** · 현재 운행 기록, 경로 좌표, 차량 설정과 단가는 예시 데이터입니다. 지도 배경에는 MapLibre Native와 OpenFreeMap의 실제 도로·지명을 표시합니다.
- **데이터 연동** · 운행·차량 설정을 제공하는 Repository와 정산 계산을 분리했습니다. 실차 운행 기록, 차량 정보 자동 조회, 실시간 단가는 API와 접근 권한 확인 후 연결할 범위입니다.
- **카카오페이 연동** · QR에는 발급된 정산 링크만 들어갑니다.

## 실행과 검증

| 항목 | 환경 |
| --- | --- |
| 검증 기기 | Pleos Connect 2.1.0 ARM64 에뮬레이터, Android API 34 |
| 개발 환경 | JDK 17, Gradle 8.13, Android SDK Platform 36 / Build Tools 35.0.0 |
| 화면 및 지도 | Kotlin, Jetpack Compose, MapLibre Native |
| 앱 패키지 | `com.example.chabi` |

Android Studio에서 프로젝트를 열고 Gradle Sync 후 Pleos Connect AVD를 실행한 상태에서 **Run**을 누릅니다. 터미널에서 실행 중인 에뮬레이터에 설치할 수도 있습니다. AVD 준비 과정은 [개발 환경 안내](docs/DEVELOPMENT.md#실행-환경)에 있습니다.

```bash
./scripts/setup-local.sh
./scripts/check.sh
./scripts/install-debug.sh
```

`./scripts/check.sh`로 구조 검사, 단위 테스트 52개, debug APK 빌드 및 Android Lint를 확인했습니다. Pleos 에뮬레이터에서 기본 정산, 카드 자동 공개·폭죽, 카카오페이 QR 판독을 검증했습니다.

- **개발 APK** · `app/build/outputs/apk/debug/app-debug.apk`
- **제출 APK** · 서명된 release APK의 배치 위치는 [`apk/`](apk/)입니다. release 빌드와 서명이 남아 있습니다.

[UX 흐름](docs/UX_FLOW.md) · [아키텍처](docs/ARCHITECTURE.md) · [지도와 출처](docs/EMBEDDED_MAP.md) · [검증 결과](docs/VERIFICATION.md)
