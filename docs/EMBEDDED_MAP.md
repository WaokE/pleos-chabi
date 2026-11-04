# 앱 내부 운행 지도 — MVP 0.6

목표는 운행 목록을 유지하면서 우측 지도에 선택한 여러 운행의 경로를 함께 표시하는 것입니다. 순정 내비 앱으로 전환하지 않습니다.

## 구현

- 공통 `RouteMapProvider/RouteMapSession` 계약을 통해 지도를 주입합니다. 현재 `route-map-maplibre` 구현에서 MapLibre Native Android 13.7.1의 MapView를 TextureView로 생성하며 앱은 일반 Android View로만 호스팅합니다. SDK 클래스는 앱 UI나 도메인에 노출하지 않습니다.
- OpenFreeMap Liberty(주간)/Dark(야간) 스타일로 실제 도로·지명 배경을 제공합니다.
- 선택 경로는 초록 실선, 확인 중 선택 경로는 굵은 실선, 확인 중 미선택 경로는 점선입니다. 다른 경로는 얇은 회색으로 표시합니다.
- 모든 선택 구간을 한 화면에 맞춥니다. 선택이 없으면 확인 중 경로, 그것도 없으면 제공된 경로 전체를 맞춥니다. 지도에서 구간을 눌러도 정산 선택은 바뀌지 않습니다.
- 확대·축소·전체 경로 버튼은 80dp 이상, 지도 정보는 64dp 이상 터치 영역입니다. 큰 창에서는 목록/지도 두 열, 작은 창에서는 펼침 지도로 전환합니다.
- 스타일·타일 로딩 중 표시와 실패 시 재시도를 제공합니다. 네트워크 없는 상태에서도 정산 계산은 로컬에서 수행됩니다. 오프라인 지도를 보장하지 않습니다.
- 주행 제한 시 지도 포함 모든 정산 UI를 숨기고 주차 안내를 표시합니다.

## 데이터와 계산 경계

`Trip.route`의 위·경도만 GeoJSON 레이어로 변환합니다. 유효하지 않은 값이 섞이거나 점이 2개 미만인 경로는 표시하지 않습니다. 누락된 부분을 직선으로 만들지 않습니다. 공급자가 좌표 구간의 의미를 보증해야 하며, 미래에 GPS 구간 단절 정보가 제공되면 모델을 확장해야 합니다.

현재 `data-local`의 `mock_routes.geojson`은 공개 지점을 OSRM 도로 라우팅으로 연결해 만든 정적 시연 데이터입니다. 앱 실행 시 라우팅 API를 호출하지 않습니다. 실제 운행 이력·순정 내비 경로·실차 GPS가 아니며 화면에 이를 표시합니다. 원래 정산 시나리오의 거리(4개 여행 합계 415km)를 유지하므로 지도 경로의 라우팅 거리와 정산 예시 거리는 다를 수 있습니다. 정산은 `distanceMeters`와 소비량을 사용하며 지도 선의 길이로 재계산하지 않습니다.

`core`의 `TripRepository`를 실제 구현으로 교체하면 제공된 좌표가 같은 지도에 표시됩니다. 지도 엔진 교체는 별도의 `RouteMapProvider` 구현을 주입합니다. 지도 출처·링크도 공급자 메타데이터로 교체되며 화면에 OpenFreeMap을 고정하지 않습니다. 과거 Trip/GPS 조회 지원 여부는 별도 확인이 필요합니다.

## 네트워크·출처

스타일·타일·폰트·스프라이트를 OpenFreeMap에서 요청합니다. 요청할 지도 영역은 공급자에 노출되지만 초안, 참가자, 정산 금액과 경로 GeoJSON 자체를 업로드하지 않습니다. 위치 권한·API 키·계정을 도입하지 않았습니다. 고유 User-Agent와 호스트당 요청 4개, 50MB HTTP 캐시를 사용하며 대량 오프라인 다운로드 기능은 없습니다.

지도 아래에 © OpenStreetMap 기여자 · OpenMapTiles · OpenFreeMap을 항상 표시하고 지도 정보에서 각 출처 링크를 제공합니다. MapLibre 라이선스는 앱 assets/licenses에 포함합니다. 순정 내비의 화면이나 지도 자산을 복제하지 않습니다.

## 공식 참고

- [MapLibre Android Quickstart](https://maplibre.org/maplibre-native/android/examples/getting-started/)
- [GeoJSON 레이어](https://maplibre.org/maplibre-native/android/examples/geojson-guide/)
- [OpenFreeMap 모바일·스타일·요청 기준](https://openfreemap.org/quick_start/)
- [OpenStreetMap 저작권/ODbL](https://www.openstreetmap.org/copyright)
- [OpenMapTiles](https://openmaptiles.org/)
- [OSRM route API](https://project-osrm.org/docs/v5.24.0/api/)

## SDK 교체

[아키텍처와 교체 절차](ARCHITECTURE.md)를 참고합니다. 지원이 확인되지 않은 Pleos 지도 어댑터나 성공을 가장하는 SDK 구현은 두지 않았습니다.
