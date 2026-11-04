package com.example.chabi.ui

import com.example.chabi.format.*

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chabi.domain.TripDataSource
import com.example.chabi.map.RouteMapProvider

@Composable
fun RoutePreview(state: DriveUiState, mapProvider: RouteMapProvider, modifier: Modifier = Modifier, onFocusTrip: (String) -> Unit = {}) {
    val focus = state.previewTrip
    var credits by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val scene = remember(state.trips, state.draft.selectedTripIds, focus?.id) { tripMapScene(state.trips, state.draft.selectedTripIds, focus?.id) }
    Box(modifier) {
        Panel(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(focus?.let { "${it.origin} → ${it.destination}" } ?: "운행 경로", style = MaterialTheme.typography.titleSmall, maxLines = 2)
                }
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), color = Mint) {
                    Text("${state.selectedTrips.size}개 선택", Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall, color = Teal)
                }
            }
            EmbeddedRouteMap(mapProvider, scene, Modifier.fillMaxWidth().weight(1f).heightIn(min = 180.dp), onFocusTrip)
            PleosTextButton(onClick = { credits = true }, modifier = Modifier.fillMaxWidth().testTag("map-credits"), contentPadding = PaddingValues(0.dp)) {
                Text("${mapProvider.info.attribution} ⓘ", style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 2)
            }
        }
    }
    if (credits) PleosFormDialog("지도 정보", onDismiss = { credits = false }, confirmEnabled = true,
        onConfirm = { credits = false }, confirmLabel = "닫기", showCancel = false) {
        Text(mapProvider.info.name, style = MaterialTheme.typography.bodySmall)
        if (state.source == TripDataSource.MOCK) Text("예시 경로는 도로를 따라 만든 시연용 데이터예요. 실제 주행 기록이 아니에요.", style = MaterialTheme.typography.bodySmall)
        for ((label, url) in mapProvider.info.links) {
            PleosOutlinedButton(onClick = { runCatching { uri.openUri(url) } }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
    }
}
