@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.chabi.ui

import com.example.chabi.format.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.chabi.domain.*

@Composable
fun ReviewScreen(state: DriveUiState, vm: ChabiViewModel) {
    var editBasis by rememberSaveable { mutableStateOf(false) }
    var showNames by rememberSaveable { mutableStateOf(false) }
    val costs: @Composable ColumnScope.() -> Unit = {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconTile(DriveSymbol.FUEL)
                Text("예상 ${state.draft.energyType.title}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                PleosTextButton(onClick = { editBasis = true }, modifier = Modifier.testTag("edit-cost-basis")) { Text("기준 변경") }
            }
            Text(state.energy?.let { won(it.amountWon) } ?: "계산 기준을 확인해 주세요", color = Teal,
                style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("energy-total"))
            Text("운행 ${state.selectedTrips.size}개 · ${distance(state.distanceMeters)} · ${duration(state.durationMinutes)}",
                style = MaterialTheme.typography.bodySmall, color = Muted)
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), color = LocalPleosColors.current.high) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val profile = state.defaults?.presets?.find { it.id == state.draft.energyProfileId }
                    Text("${profile?.label ?: state.draft.energyType.title} · ${state.draft.unitPriceInput}원/${state.draft.energyType.quantityUnit}",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${state.draft.energyType.efficiencyName} ${state.draft.efficiencyInput} ${state.draft.energyType.efficiencyUnit}" +
                        if (state.draft.useTripEfficiency) " · 운행마다 기록된 ${state.draft.energyType.efficiencyName} 우선" else "",
                        style = MaterialTheme.typography.labelMedium, color = Muted)
                    PriceBasisNote(state.draft)
                }
            }
            if (state.energy?.consumptionProvidedCount != 0 && state.energy != null) Hint("운행 ${state.energy.consumptionProvidedCount}개는 기록된 소비량으로 계산했어요.")
            state.energyError?.let { Hint(it, true) }
            PleosTextButton(onClick = { vm.setStep(Step.TRIPS) }) { Text("운행 다시 고르기") }
        }
        VehicleExpenses(state, vm)
    }
    val people: @Composable ColumnScope.() -> Unit = {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconTile(DriveSymbol.PEOPLE, warm = true)
                Text("운전자 포함 인원", style = MaterialTheme.typography.titleSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(minOf(state.draft.participants.size, 5)) { index -> CompanionFace(index, Modifier.size(52.dp)) }
                if (state.draft.participants.size > 5) Text("+${state.draft.participants.size - 5}", style = MaterialTheme.typography.labelMedium, color = Muted)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                PleosOutlinedButton(onClick = { vm.setParticipantCount(state.draft.participants.size - 1) },
                    modifier = Modifier.size(80.dp).testTag("people-minus"), enabled = state.draft.participants.size > MIN_PARTICIPANTS,
                    contentPadding = PaddingValues(0.dp)) { Text("−") }
                Text("${state.draft.participants.size}명", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("people-count"))
                PleosOutlinedButton(onClick = { vm.setParticipantCount(state.draft.participants.size + 1) },
                    modifier = Modifier.size(80.dp).testTag("people-plus"), enabled = state.draft.participants.size < MAX_PARTICIPANTS,
                    contentPadding = PaddingValues(0.dp)) { Text("+") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(2, 3, 4, 5).forEach { count -> NumberPill("${count}명", state.draft.participants.size == count) { vm.setParticipantCount(count) } }
            }
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), color = Mint) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("한 사람당")
                    val shares = state.settlement?.shares.orEmpty().map { it.amountWon }
                    if (shares.isNotEmpty()) {
                        Text(perPersonAmountText(shares),
                            style = MaterialTheme.typography.headlineSmall, color = Teal, modifier = Modifier.testTag("share-preview"))
                        if (shares.distinct().size != 1) Text("나누고 남은 1원이나 일부만 나눠 낸 비용 때문에 사람마다 달라요.", style = MaterialTheme.typography.labelSmall, color = Muted)
                    } else Hint(state.settlementError ?: "정산 정보를 확인해 주세요.", true)
                }
            }
            PleosTextButton(onClick = { showNames = !showNames }, modifier = Modifier.testTag("toggle-names")) {
                Text(if (showNames) "이름 입력 접기" else "이름 입력하기 (선택)")
            }
            if (showNames) {
                state.draft.participants.forEachIndexed { index, person ->
                    PleosTextField(person.name, { vm.renamePerson(person.id, it) }, Modifier.fillMaxWidth(),
                        label = { Text(if (index == 0) "운전자" else "동행 $index") }, singleLine = true,
                        isError = isInvalidParticipantName(person.name, state.draft.participants))
                }
            }
        }
        Text("${state.draft.energyType.title}는 고른 운행을 모두 같이 탄 것으로 보고 똑같이 나눠요.", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= PleosBreakpoints.TwoColumnWidth) Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(20.dp), content = costs)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp), content = people)
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("예상 ${state.draft.energyType.title}", style = MaterialTheme.typography.labelMedium, color = Muted)
                        Text(state.energy?.let { won(it.amountWon) } ?: "계산 기준을 확인해 주세요", style = MaterialTheme.typography.titleLarge,
                            color = Teal, modifier = Modifier.testTag("energy-total"))
                    }
                    PleosTextButton(onClick = { editBasis = true }, modifier = Modifier.testTag("edit-cost-basis")) { Text("기준 변경") }
                }
                val profile = state.defaults?.presets?.find { it.id == state.draft.energyProfileId }
                Text("${profile?.label ?: state.draft.energyType.title} · ${state.draft.unitPriceInput}원/${state.draft.energyType.quantityUnit} · ${if (state.draft.priceIsMock) "예시 단가" else state.draft.priceSource}",
                    style = MaterialTheme.typography.labelSmall, color = Muted)
                state.energyError?.let { Hint(it, true) }
            }
            people(); VehicleExpenses(state, vm)
        }
    }
    if (editBasis) CostBasisEditor(state, vm, onDismiss = { editBasis = false })
}

@Composable
fun PriceBasisNote(draft: SettlementDraft) {
    Text("${draft.priceSource}${draft.priceAsOf.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}",
        color = Muted, style = MaterialTheme.typography.labelMedium)
    if (draft.priceIsMock) Text("예시 단가예요. 실제 평균가가 아니에요.", color = Muted, style = MaterialTheme.typography.labelSmall)
}


@Composable
private fun CostBasisEditor(state: DriveUiState, vm: ChabiViewModel, onDismiss: () -> Unit) {
    var profileId by rememberSaveable { mutableStateOf(state.draft.energyProfileId) }
    var efficiency by rememberSaveable { mutableStateOf(state.draft.efficiencyInput) }
    var price by rememberSaveable { mutableStateOf(state.draft.unitPriceInput) }
    var useTrip by rememberSaveable { mutableStateOf(state.draft.useTripEfficiency) }
    var reference by rememberSaveable { mutableStateOf(false) }
    val editor = remember { DraftEditor() }
    val preset = state.defaults?.presets?.find { it.id == profileId }
    // Same rule the ViewModel commits with, so the hint and validation match what "적용" stores.
    val draft = preset?.let { editor.previewCostSettings(state.draft, it, efficiency, price, useTrip, reference) }
        ?: state.draft.copy(efficiencyInput = efficiency, unitPriceInput = price, useTripEfficiency = useTrip)
    val validation = runCatching { EnergyCalculator.settings(draft) }
    PleosFormDialog("계산 기준 변경", onDismiss, validation.isSuccess, onConfirm = {
        vm.applyCostSettings(profileId, efficiency, price, useTrip, reference); onDismiss()
    }, confirmLabel = "적용", confirmTag = "apply-cost-basis") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.defaults?.presets?.forEach { option -> NumberPill(option.label, profileId == option.id) {
                if (profileId != option.id) {
                    profileId = option.id; efficiency = option.efficiency; price = option.price; reference = true
                }
            } }
        }
        PleosTextField(efficiency, { efficiency = it.take(12) }, Modifier.fillMaxWidth().testTag("efficiency-input"),
            label = { Text(draft.energyType.efficiencyName) },
            suffix = { Text(draft.energyType.efficiencyUnit) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = validation.isFailure)
        PleosTextField(price, { price = it.take(9); reference = false }, Modifier.fillMaxWidth().testTag("price-input"),
            label = { Text("단가") }, suffix = { Text("원/${draft.energyType.quantityUnit}") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = validation.isFailure)
        PleosOutlinedButton(onClick = { if (preset != null) { price = preset.price; reference = true } }, modifier = Modifier.fillMaxWidth()) { Text("예시 단가로 되돌리기") }
        Hint(if (draft.priceSource == DIRECT_PRICE_SOURCE) "직접 입력한 단가로 계산해요."
            else "${draft.priceSource}${draft.priceAsOf.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}")
        if (draft.energyType == EnergyType.ELECTRIC) Hint("충전 단가는 충전 사업자, 충전 속도, 회원 여부에 따라 달라요.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("운행마다 기록된 ${draft.energyType.efficiencyName} 쓰기", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            PleosSwitch(useTrip, { useTrip = it })
        }
        validation.exceptionOrNull()?.message?.let { Hint(it, true) }
        Hint("소비량이 기록된 운행은 그 값을 쓰고, 나머지는 거리와 ${draft.energyType.efficiencyName}로 계산해요.")
    }
}
