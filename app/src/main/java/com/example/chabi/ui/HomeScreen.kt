package com.example.chabi.ui

import com.example.chabi.format.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chabi.domain.Trip
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(state: DriveUiState, vm: ChabiViewModel) {
    var confirmNew by rememberSaveable { mutableStateOf(false) }
    val welcome: @Composable ColumnScope.() -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("같이 탄 차비,", style = MaterialTheme.typography.headlineMedium)
            Text("운행 기록으로 나눠요", style = MaterialTheme.typography.headlineMedium, color = Teal)
        }
        Text("운행을 고르면 연료비를 계산해서\n인원수대로 나눠 드려요.",
            style = MaterialTheme.typography.bodyLarge, color = Muted)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.hasDraft) {
                val tripCount = state.selectedTrips.size
                val tripSummary = if (tripCount > 0) " · 운행 ${tripCount}개 · ${distance(state.distanceMeters)}" else ""
                Text("${state.resumeStep.shortTitle}$tripSummary", style = MaterialTheme.typography.labelMedium, color = Muted)
            }
            PleosButton(
                onClick = if (state.hasDraft) vm::resumeDraft else vm::startNew,
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                    .testTag(if (state.hasDraft) "resume-settlement" else "new-settlement"),
                shape = RoundedCornerShape(32.dp),
            ) {
                DriveIcon(DriveSymbol.ROUTE, Modifier.size(28.dp), LocalContentColor.current)
                Spacer(Modifier.width(14.dp))
                Text(if (state.hasDraft) "이어서 하기" else "운행 고르기")
            }
            if (state.hasDraft) {
                PleosOutlinedButton(onClick = { confirmNew = true }, modifier = Modifier.fillMaxWidth().testTag("new-settlement")) {
                    Text("새 정산")
                }
            }
        }
    }
    val overview: @Composable ColumnScope.() -> Unit = {
        DriveJourneyArt(Modifier.fillMaxWidth().height(330.dp))
        TripRecordSummary(state)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= PleosBreakpoints.TwoColumnWidth) Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 38.dp),
            horizontalArrangement = Arrangement.spacedBy(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp), content = welcome)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp), content = overview)
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)) { welcome(); overview() }
    }
    val dismissConfirmation = { confirmNew = false }
    PleosModal(confirmNew, onDismiss = dismissConfirmation) {
        PleosForm("새 정산을 시작할까요?", onDismiss = dismissConfirmation, confirmEnabled = true,
            onConfirm = { dismissConfirmation(); vm.startNew() }, confirmLabel = "새로 시작", confirmTag = "confirm-new") {
            Hint("이전에 고른 운행과 입력한 비용·인원이 지워져요.")
        }
    }
}

/**
 * What the user can settle right now: the loaded trip records, their date span and total distance.
 * Replaces a decorative step list with information the repository actually provides.
 */
@Composable
private fun TripRecordSummary(state: DriveUiState) {
    Surface(Modifier.fillMaxWidth().testTag("trip-record-summary"), shape = RoundedCornerShape(28.dp),
        color = LocalPleosColors.current.surface) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            IconTile(DriveSymbol.ROUTE)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("불러온 운행 기록", style = MaterialTheme.typography.labelMedium, color = Muted)
                if (state.trips.isEmpty()) Text("아직 불러온 운행이 없어요", style = MaterialTheme.typography.bodyMedium)
                else {
                    Text("운행 ${state.trips.size}개 · ${tripDateSpan(state.trips)}", style = MaterialTheme.typography.titleSmall)
                    Text("모두 ${distance(state.trips.sumOf { it.distanceMeters })}", style = MaterialTheme.typography.labelMedium, color = Muted)
                }
            }
        }
    }
}

/** "11월 1일" for a single day, "11월 1일 ~ 11월 3일" for a range. Trips must not be empty. */
internal fun tripDateSpan(trips: List<Trip>): String {
    val format = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA)
    val first = trips.minOf { it.startedAt }.toLocalDate()
    val last = trips.maxOf { it.startedAt }.toLocalDate()
    return if (first == last) first.format(format) else "${first.format(format)} ~ ${last.format(format)}"
}
