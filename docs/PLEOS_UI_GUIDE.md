# Pleos Connect UI 적용 기준

확인 및 구현: 2026-11-04. 공개된 공식 UI Design Guide의 Foundations, Components, Display Layout을 기준으로 차비의 Compose UI를 조정했습니다. 이 문서는 구현 기준과 검증 범위를 기록하며 공식 인증을 의미하지 않습니다.

## Foundations

- [Introduction](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro): 충분한 명도 대비, 짧은 안내, 주야간 대응 및 터치 피드백을 적용합니다. 일반 안내는 최대 3줄로 구성하고 금액·오류 등 의사결정에 필요한 내용은 별도로 표시합니다.
- [Colors](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors): 공식 회색 팔레트를 기본으로 사용합니다. 앱 고유의 강조색은 주간 `#126B58`, 야간 `#A4E2C1`이며 배경과 함께 전환합니다. 시스템 차량 UI의 상태 색상을 재정의하지 않습니다.
- [Typography](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography): Asta Sans를 번들에 포함했습니다. OFL 원문도 APK assets에 포함합니다. Headline 60/56/48, Title 40/36/32, Body 30/28/26, Label 24/20을 사용합니다. 화면 공간이 작으면 작은 계층의 제목을 선택하고 본문을 과도하게 줄이지 않습니다.

공식 이미지의 숫자 크기를 글자는 `sp`, 레이아웃은 `dp`로 적용합니다. API 34 에뮬레이터의 160dpi 환경에서는 레이아웃 값이 픽셀 값과 일치합니다. 실제 디스플레이에서의 물리적인 글자·터치 크기는 기기 밀도를 포함해 추가 확인해야 합니다.

색상과 크기 토큰은 `ui/Design.kt`에 모았습니다. 밝고 어두운 배경, 보조 본문, 강조·경고·오류, 선택 상태 및 버튼 눌림 상태의 글자 대비를 4.5:1 이상으로 검사하는 단위 테스트를 포함합니다. 이 검사는 선언한 토큰 조합의 수치 검사이며 모든 화면 픽셀을 자동 평가하는 검사는 아닙니다.

## Components

Material 3의 기본 크기를 그대로 사용하지 않고 `ui/PleosComponents.kt`에서 입력·접근성 동작을 유지하며 외형과 상태를 조정합니다.

| 공식 문서 | 적용 |
| --- | --- |
| [Basic Button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button) | Medium 높이 80 / 모서리 24, 주요 진행·팝업 버튼 Large 높이 100 / 모서리 32. 눌림·비활성 색상 구분 |
| [Text Button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/text-button) | 최소 64 높이와 80 너비의 조작 영역 |
| [Checkbox](https://document.pleos.ai/docs/connect/guide/docs-design/components/selections/checkbox) | 48 크기의 체크 표시, 모서리 12. Trip 카드 전체가 체크 조작 영역이며 선택 상태를 접근성에 전달 |
| [Text Field](https://document.pleos.ai/docs/connect/guide/docs-design/components/fields/text-field) | 최소 80 높이 / 모서리 24, 본문 28, 라벨 24, 포커스·오류 구분 |
| [Segmented Menu](https://document.pleos.ai/docs/connect/guide/docs-design/components/segmented-menu) | 연료 종류·빠른 인원 선택의 선택 상태 구분. 단계 표시는 조작 없는 진행 안내로 단순화 |
| [System Popup](https://document.pleos.ai/docs/connect/guide/docs-design/components/popups/system-popup) | Medium 표기 너비 760을 최대값으로 적용. 모서리 48, 안쪽 여백 48, 스크롤 본문과 별도 하단 동작 영역 |

빠른 선택 칩은 64 이상, 토글 조작 영역은 96×80입니다. 버튼과 빠른 선택 등에 터치 피드백을 제공합니다. 팝업은 작은 창과 키보드 표시 시 가용 높이에 맞추고 저장·취소 버튼을 본문 스크롤 밖에 둡니다.

## Display Layout

- [Layout](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout), [Window](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window)를 참고했습니다. Driving view와 시스템 영역을 침범하지 않고 앱에 할당된 창 안에서 배치합니다. 화면 방향을 고정하지 않으며 Activity 크기 변경을 허용합니다.
- 너비 1200dp 이상은 운행 목록과 경로 미리보기를 약 1:0.85 비율로 표시하고 비용·인원 및 결과는 두 열로 배치합니다. 작은 창은 한 열로 전환하며 경로는 선택적으로 펼칩니다. 운행 선택·비용 확인의 하단 진행 영역에는 선택 수·거리와 예상 금액을 유지합니다. 분기점과 미리보기 폭은 앱의 콘텐츠에 맞춘 값입니다. 분기점은 `ui/Design.kt`의 `PleosBreakpoints`에서 정의합니다(두 열 1200dp, 하단 진행 영역 세로 배치 1000dp 미만, 낮은 창 850dp 미만, 금액 행 세로 배치 600dp 미만).
- 높이 850dp 미만에서는 부가 설명을 줄입니다. 목록·폼 본문은 스크롤하며 금액 입력과 참가자 선택은 공간에 따라 줄바꿈합니다.
- `adjustResize`와 팝업 IME inset을 적용하여 시스템 키보드 위에서 동작 영역에 접근할 수 있게 합니다.

## 정차 상태와 주행 제한

[운전자 주의 분산 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline)와 [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)에 따라 AAOS `CarUxRestrictionsManager`를 사용합니다.

정산 앱은 DO 최적화 앱으로 선언하지 않습니다. 시스템이 DO를 요구하면 운행 선택·비용 입력·정산 화면 전체를 안내 화면으로 교체하고 키보드와 포커스를 닫습니다. 연결 중이거나 상태 조회가 실패해도 입력을 차단합니다. 다시 조작 가능한 상태가 되면 저장된 초안과 열려 있던 입력 폼을 복원합니다. 고정 속도 임계값을 앱에서 임의로 정하지 않고 시스템의 지역·차량별 UX 제한을 따릅니다.

`platform/CarUxMonitor.kt`는 안전 상태만 담당합니다. Trip 데이터는 기존 Mock Repository를 유지하므로 Pleos 차량 데이터 SDK에 새 의존성을 추가하지 않습니다.

## 검증 범위

Pleos Connect 2.1.0 / Android API 34 ARM64 에뮬레이터에서 다음을 확인했습니다.

- 앱 창 1682×1210dp 및 841×1210dp의 가로·세로 배치
- 주간·야간, 시스템 글자 크기 130%, 키보드 표시 상태
- 운행 4개 선택, 415km / 차량 비용 56,440원, 좁은 창의 선택 요약. 추가 비용이 저장된 98,441원 초안도 확인
- 주행 제한에 따른 화면 차단과 정차 복귀 시 입력 중 폼 보존

실차별 밀도·창 크기·하드웨어 터치 피드백 및 지역별 주행 정책 검증은 실제 대상 기기에서 진행해야 합니다. 빌드 및 상세 검증 결과는 [VERIFICATION.md](VERIFICATION.md)에 기록합니다.

현재 MVP의 구체적인 화면 흐름과 Mock 연동 경계는 [UX_FLOW.md](UX_FLOW.md)를 참고합니다.

0.6에서 적용한 친근한 색·캐릭터·화면별 정보 계층은 [디자인 방향](DESIGN_DIRECTION.md)에 정리했습니다. 주요 버튼에도 앱 강조색을 사용하며 주야간·눌림 상태의 글자 대비 검사를 유지합니다.
