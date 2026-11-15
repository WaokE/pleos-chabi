package com.example.chabi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chabi.map.RouteMapProvider

@Composable
fun RoutePreview(state: DriveUiState, mapProvider: RouteMapProvider, modifier: Modifier = Modifier, onFocusTrip: (String) -> Unit = {}) {
    val focus = state.previewTrip
    val scene = remember(state.trips, state.draft.selectedTripIds, focus?.id) { tripMapScene(state.trips, state.draft.selectedTripIds, focus?.id) }
    Panel(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(focus?.let { "${it.origin} → ${it.destination}" } ?: "운행 경로",
                modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Surface(shape = RoundedCornerShape(18.dp), color = Mint) {
                Text("${state.selectedTrips.size}개 선택", Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelSmall, color = Teal)
            }
        }
        EmbeddedRouteMap(mapProvider, scene, Modifier.fillMaxWidth().weight(1f).heightIn(min = 180.dp), onFocusTrip)
    }
}
