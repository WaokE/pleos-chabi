package com.example.chabi.ui

import com.example.chabi.format.*

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chabi.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chabi.domain.TripDataSource
import com.example.chabi.domain.*
import com.example.chabi.platform.DrivingAccess
import com.example.chabi.map.RouteMapProvider
import com.example.chabi.handoff.QrEncoder

@Composable
fun ChabiApp(viewModel: ChabiViewModel, access: DrivingAccess, retryConnection: () -> Unit, mapProvider: RouteMapProvider, qrEncoder: QrEncoder) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    LaunchedEffect(access) {
        if (access != DrivingAccess.PARKED) { keyboard?.hide(); focus.clearFocus(force = true) }
    }
    LaunchedEffect(state.step) { keyboard?.hide(); focus.clearFocus(force = true) }
    BackHandler {
        if (access == DrivingAccess.PARKED && state.step != Step.HOME) viewModel.back()
        else (context as? Activity)?.moveTaskToBack(true)
    }
    val savedContent = rememberSaveableStateHolder()
    DriveTheme {
        if (access != DrivingAccess.PARKED) { ParkingGate(access, retryConnection); return@DriveTheme }
        savedContent.SaveableStateProvider("parked-content") {
            BoxWithConstraints(Modifier.fillMaxSize().background(Paper).semantics { testTagsAsResourceId = true }) {
                val compact = maxHeight < PleosBreakpoints.CompactHeight
                Column(Modifier.fillMaxSize().padding(horizontal = if (maxWidth >= PleosBreakpoints.TwoColumnWidth) 32.dp else 20.dp)) {
                    AppHeader(state, viewModel)
                    state.notice?.let { notice ->
                        Surface(color = LocalPleosColors.current.warningContainer, shape = RoundedCornerShape(24.dp)) {
                            Row(Modifier.padding(start = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(notice, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 3)
                                PleosTextButton(onClick = viewModel::dismissNotice) { Text("닫기") }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        when {
                            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                            state.loadError -> Panel {
                                SectionTitle("운행 기록을 불러오지 못했어요", "작성하던 내용은 그대로 남아 있어요.")
                                PleosButton(onClick = viewModel::loadTrips) { Text("다시 불러오기") }
                            }
                            else -> AnimatedStep(state, Modifier.fillMaxSize()) { pageState ->
                                StepPage(pageState, viewModel, compact, mapProvider, qrEncoder)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepPage(state: DriveUiState, vm: ChabiViewModel, compact: Boolean, mapProvider: RouteMapProvider, qrEncoder: QrEncoder) {
    Column(Modifier.fillMaxSize()) {
        if (state.step != Step.HOME) {
            Spacer(Modifier.height(if (compact) 8.dp else 18.dp))
            Text(when (state.step) {
                Step.TRIPS -> "정산할 운행을 골라요"
                Step.REVIEW -> "비용과 인원을 확인해요"
                else -> "나눌 금액을 확인해요"
            }, style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge, maxLines = 2)
            Spacer(Modifier.height(if (compact) 16.dp else 24.dp))
        }
        Box(Modifier.weight(1f)) {
            when (state.step) {
                Step.HOME -> HomeScreen(state, vm)
                Step.TRIPS -> TripStep(state, vm, mapProvider)
                Step.REVIEW -> ReviewScreen(state, vm)
                Step.RESULT -> ResultStep(state, vm, qrEncoder)
            }
        }
        if (state.step == Step.TRIPS || state.step == Step.REVIEW) StepFooter(state, vm)
    }
}

@Composable
private fun AppHeader(state: DriveUiState, vm: ChabiViewModel) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= PleosBreakpoints.TwoColumnWidth
        Column {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.step != Step.HOME) PleosOutlinedButton(onClick = vm::back, modifier = Modifier.testTag("back-step"),
                    contentPadding = PaddingValues(horizontal = 18.dp)) { Text("← 이전") }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DriveBuddy(Modifier.size(54.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall)
                }
                if (wide && state.step != Step.HOME) FlowSteps(state.step)
                if (state.source != TripDataSource.MOCK) {
                    Surface(color = Mint, shape = RoundedCornerShape(20.dp)) {
                        Text(state.source.label, Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.labelSmall, color = Teal, maxLines = 2)
                    }
                }
                if (state.step == Step.RESULT) PleosTextButton(onClick = { vm.setStep(Step.HOME) }, modifier = Modifier.testTag("go-home")) { Text("홈") }
            }
            if (!wide && state.step != Step.HOME) FlowSteps(state.step)
        }
    }
}

@Composable
private fun FlowSteps(current: Step) {
    val steps = listOf(Step.TRIPS, Step.REVIEW, Step.RESULT)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { index, step ->
            val active = current == step
            val done = steps.indexOf(current) > index
            Surface(shape = RoundedCornerShape(20.dp), color = if (active) Mint else androidx.compose.ui.graphics.Color.Transparent) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (done) "✓" else "${index + 1}", color = if (active || done) Teal else Muted,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(step.shortTitle, color = if (active) Teal else Muted, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun StepFooter(state: DriveUiState, vm: ChabiViewModel) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        val narrow = maxWidth < PleosBreakpoints.FooterStackWidth
        val summary: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("운행 ${state.selectedTrips.size}개 · ${distance(state.distanceMeters)}",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(if (state.step == Step.TRIPS) state.energy?.let { "예상 ${won(it.amountWon + state.additionalWon)}" } ?: "운행을 고르면 예상 금액이 보여요"
                    else "운전자 포함 ${state.draft.participants.size}명 · ${state.settlement?.let { "예상 ${won(it.totalWon)}" } ?: "금액을 계산할 수 없어요"}",
                    style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
        val action: @Composable (Modifier) -> Unit = { modifier ->
            PleosButton(onClick = vm::next, enabled = state.canContinue,
                modifier = modifier.heightIn(min = 100.dp).testTag("next-step"), shape = RoundedCornerShape(32.dp)) {
                Text(if (state.step == Step.TRIPS) "운행 ${state.selectedTrips.size}개로 정산하기" else "결과 보기")
            }
        }
        Surface(shape = RoundedCornerShape(32.dp), color = LocalPleosColors.current.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (narrow) { summary(); action(Modifier.fillMaxWidth()) }
                else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Surface(shape = RoundedCornerShape(22.dp), color = Mint) {
                        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { DriveIcon(DriveSymbol.CHECK, Modifier.size(30.dp)) }
                    }
                    Box(Modifier.weight(1f)) { summary() }; action(Modifier.widthIn(min = 440.dp))
                }
            }
        }
    }
}

@Composable
private fun ParkingGate(access: DrivingAccess, retry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())
        .semantics { testTagsAsResourceId = true }, contentAlignment = Alignment.Center) {
        Panel(Modifier.widthIn(max = 760.dp).padding(32.dp)) {
            Eyebrow(stringResource(R.string.app_name))
            Text(when (access) {
                DrivingAccess.CHECKING -> "차량 상태를 확인하고 있어요"
                DrivingAccess.RESTRICTED -> "주행 중에는 쓸 수 없어요"
                else -> "차량 상태를 확인할 수 없어요"
            }, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("parking-gate"))
            Hint(if (access == DrivingAccess.RESTRICTED) "차를 세우면 하던 화면으로 돌아와요."
                else "주행 중인지 확인한 뒤 시작해요.")
            if (access == DrivingAccess.CHECKING) CircularProgressIndicator()
            if (access == DrivingAccess.UNAVAILABLE) PleosButton(onClick = retry) { Text("다시 확인") }
        }
    }
}
