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
import com.example.chabi.handoff.QrEncoder
import com.example.chabi.domain.TripDataSource
import com.example.chabi.domain.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ResultStep(state: DriveUiState, vm: ChabiViewModel, qrEncoder: QrEncoder) {
    val result = state.settlement
    var showQr by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    if (result == null) {
        Panel { SectionTitle("정산 정보를 다시 확인해 주세요", state.settlementError)
            PleosButton(onClick = { vm.setStep(Step.REVIEW) }) { Text("비용·인원 다시 보기") } }
        return
    }
    val isMock = state.source == TripDataSource.MOCK
    val hero: @Composable ColumnScope.() -> Unit = {
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Surface(shape = RoundedCornerShape(28.dp), color = Mint) {
                    DriveBuddy(Modifier.width(120.dp).height(100.dp).padding(4.dp), celebrating = true)
                }
                Text("운전자 포함 ${result.shares.size}명", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
            val values = result.shares.map { it.amountWon }
            val uniform = values.distinct().size == 1
            Text(if (uniform) "한 사람당" else "한 사람당 (사람마다 달라요)", style = MaterialTheme.typography.bodyLarge, color = Muted)
            Text(perPersonAmountText(values), color = Teal, modifier = Modifier.testTag("per-person-total"),
                style = if (uniform) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall)
            Text("운행 ${result.tripCount}개 · ${distance(result.distanceMeters)}", style = MaterialTheme.typography.bodySmall, color = Muted)
            HorizontalDivider()
            Box(Modifier.testTag("settlement-total")) { MoneyRow("예상 총액", won(result.totalWon), strong = true) }
            PleosButton(onClick = { showQr = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).testTag("show-qr"),
                shape = RoundedCornerShape(32.dp)) {
                DriveIcon(DriveSymbol.QR, Modifier.size(28.dp), LocalContentColor.current)
                Spacer(Modifier.width(12.dp)); Text("휴대폰으로 요약 받기")
            }
        }
        Text("연비와 단가로 계산한 금액이라 실제 주유·충전 금액과 다를 수 있어요.", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
    val shares: @Composable ColumnScope.() -> Unit = {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconTile(DriveSymbol.PEOPLE, warm = true)
                Text("이렇게 나누면 돼요", style = MaterialTheme.typography.titleSmall)
            }
            result.shares.forEachIndexed { index, share ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CompanionFace(index, Modifier.size(56.dp))
                    Column(Modifier.weight(1f)) {
                        Text(share.participant.name, style = MaterialTheme.typography.bodyMedium)
                        if (index == 0) Text("운전자", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                    Text(won(share.amountWon), style = MaterialTheme.typography.titleSmall)
                }
            }
            if (result.hasRemainderAllocation) {
                Text("비용을 나누고 남은 원은 등록 순서대로 1원씩 더했어요.", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
        PleosOutlinedButton(onClick = { details = !details }, modifier = Modifier.fillMaxWidth().testTag("toggle-result-details")) {
            Text(if (details) "계산 내역 접기" else "계산 내역 보기")
        }
        if (details) Panel {
            result.costs.forEach { cost ->
                MoneyRow(cost.title, won(cost.amountWon))
                if (cost.participantIds.size != result.shares.size) Hint("참여: " + result.shares.filter { it.participant.id in cost.participantIds }.joinToString(" · ") { it.participant.name })
            }
            HorizontalDivider()
            Text("적용 단가 ${state.draft.unitPriceInput}원/${result.energyType.quantityUnit}", style = MaterialTheme.typography.bodySmall)
            PriceBasisNote(state.draft)
        }
        if (isMock) Text("예시 운행으로 계산한 결과예요.", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= PleosBreakpoints.TwoColumnWidth) Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(20.dp), content = hero)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp), content = shares)
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) { hero(); shares() }
    }
    if (showQr) QrHandoff(ResultSummary.create(result, state.draft, isMock), qrEncoder, onDismiss = { showQr = false })
}

/**
 * Per-person amount shown on the review and result screens.
 * One value when everyone pays the same; otherwise the min~max range on two lines,
 * since leftover won and partial expenses can make shares differ.
 */
internal fun perPersonAmountText(amounts: List<Long>): String {
    require(amounts.isNotEmpty())
    return if (amounts.distinct().size == 1) won(amounts.first()) else "${won(amounts.min())}~\n${won(amounts.max())}"
}
