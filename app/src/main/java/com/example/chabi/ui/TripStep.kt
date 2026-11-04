@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.chabi.ui

import com.example.chabi.format.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chabi.map.RouteMapProvider
import com.example.chabi.domain.Trip
import java.time.format.DateTimeFormatter

@Composable
fun TripStep(state: DriveUiState, vm: ChabiViewModel, mapProvider: RouteMapProvider) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val split = maxWidth >= PleosBreakpoints.TwoColumnWidth
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TripList(state, vm, mapProvider, Modifier.weight(1f), showMap = !split)
            if (split) RoutePreview(state, mapProvider, Modifier.weight(0.85f).fillMaxHeight(), vm::focusTrip)
        }
    }
}

@Composable
private fun TripList(state: DriveUiState, vm: ChabiViewModel, mapProvider: RouteMapProvider, modifier: Modifier, showMap: Boolean) {
    var mapExpanded by rememberSaveable { mutableStateOf(false) }
    val visible = when (state.filter) {
        TripFilter.ALL -> state.trips
        TripFilter.SELECTED -> state.selectedTrips
    }
    val grouped = visible.groupBy { it.startedAt.toLocalDate() }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp),
        modifier = modifier.fillMaxSize().testTag("trip-list")) {
        if (showMap) item {
            PleosOutlinedButton(onClick = { mapExpanded = !mapExpanded }, modifier = Modifier.fillMaxWidth().testTag("toggle-route-preview")) {
                Text(if (mapExpanded) "지도 접기" else "지도로 보기")
            }
            if (mapExpanded) { Spacer(Modifier.height(12.dp)); RoutePreview(state, mapProvider, Modifier.fillMaxWidth().height(680.dp), vm::focusTrip) }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TripFilter.entries.forEach { filter -> NumberPill(
                    if (filter == TripFilter.SELECTED) "${filter.title} ${state.selectedTrips.size}" else filter.title,
                    selected = state.filter == filter, onClick = { vm.setFilter(filter) }) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("운행 ${visible.size}개", Modifier.weight(1f), fontSize = 26.sp, color = Muted)
                // With only "all" and "selected" filters, toggling the visible list also covers clearing the selection.
                PleosTextButton(onClick = { vm.toggleGroup(visible.map { it.id }.toSet()) }, enabled = visible.isNotEmpty()) {
                    Text(if (visible.isNotEmpty() && visible.all { it.id in state.draft.selectedTripIds }) "모두 해제" else "모두 선택")
                }
            }
        }
        if (visible.isEmpty()) item {
            Panel { DriveBuddy(Modifier.size(100.dp))
                if (state.filter == TripFilter.SELECTED) SectionTitle("아직 고른 운행이 없어요", "전체 운행에서 정산할 운행을 골라 주세요.")
                else SectionTitle("불러온 운행 기록이 없어요") }
        }
        grouped.forEach { (day, trips) ->
            item(key = "date:$day") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(day.format(DateTimeFormatter.ofPattern("M월 d일 · E요일", java.util.Locale.KOREA)), fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        Text("운행 ${trips.size}개 · ${distance(trips.sumOf { it.distanceMeters })}", color = Muted, fontSize = 24.sp)
                    }
                    PleosOutlinedButton(onClick = { vm.toggleGroup(trips.map { it.id }.toSet()) },
                        modifier = Modifier.heightIn(min = 80.dp).testTag("select-date:$day"), shape = RoundedCornerShape(24.dp)) {
                        Text(if (trips.all { it.id in state.draft.selectedTripIds }) "이날 모두 해제" else "이날 모두 선택")
                    }
                }
            }
            items(trips, key = { "trip:${it.id}" }) { trip ->
                TripCard(trip, trip.id in state.draft.selectedTripIds, onToggle = { vm.toggleTrip(trip.id) })
            }
        }
    }
}

@Composable
private fun TripCard(trip: Trip, selected: Boolean, onToggle: () -> Unit) {
    val background by animateColorAsState(if (selected) Mint else MaterialTheme.colorScheme.surface, label = "trip-selection")
    Surface(shape = RoundedCornerShape(28.dp), color = background,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Teal else Line),
        modifier = Modifier.fillMaxWidth().testTag("trip:${trip.id}")
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })) {
        Row(Modifier.padding(horizontal = 22.dp, vertical = 20.dp).heightIn(min = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            PleosCheckbox(selected)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${trip.origin} → ${trip.destination}", style = MaterialTheme.typography.titleSmall, maxLines = 2,
                    color = Ink, fontWeight = FontWeight.Bold)
                Text("${trip.startedAt.toLocalTime()}–${trip.endedAt.toLocalTime()} · ${duration(trip.durationMinutes)}",
                    color = Muted, style = MaterialTheme.typography.labelMedium, maxLines = 2)
            }
            // The checkbox already shows the selection; the card only adds the distance.
            Text(distance(trip.distanceMeters), color = if (selected) Teal else Ink, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }
    }
}
