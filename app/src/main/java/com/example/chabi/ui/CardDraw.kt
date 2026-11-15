@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.chabi.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.chabi.R
import com.example.chabi.domain.CardDrawResult
import com.example.chabi.format.won
import kotlin.random.Random

private const val CARD_FLIP_MS = 240

@Composable
internal fun CardDraw(
    participantCount: Int,
    totalWon: Long,
    onDismiss: () -> Unit,
    onConfirm: (CardDrawResult) -> Unit,
) {
    val payerCard = rememberSaveable(participantCount, totalWon) { Random.nextInt(participantCount) }
    var picks by rememberSaveable(participantCount, totalWon) { mutableStateOf(IntArray(0)) }
    var flippingCard by remember { mutableIntStateOf(-1) }
    var celebrate by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val payerTurn = picks.indexOf(payerCard) + 1
    val completedDraw = if (payerTurn > 0 && flippingCard < 0) {
        CardDrawResult(payerTurn, participantCount, totalWon)
    } else null
    val confetti by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.card_draw_confetti))
    val confettiProgress by animateLottieCompositionAsState(confetti, isPlaying = celebrate)

    PleosForm(
        title = "차비 카드 뽑기",
        onDismiss = onDismiss,
        confirmEnabled = completedDraw != null,
        onConfirm = { completedDraw?.let(onConfirm) },
        confirmLabel = "이 결과로 정산하기",
        confirmTag = "confirm-card-draw",
        maxFormWidth = 1120.dp,
        titleStyle = MaterialTheme.typography.headlineSmall,
    ) {
        Text("카드 한 장에 예상 총액 ${won(totalWon)}이 들어 있어요.", style = MaterialTheme.typography.bodyMedium)
        val instruction = when {
            completedDraw != null -> "${completedDraw.payerTurn}번째로 뽑은 사람이 전액을 내요."
            flippingCard >= 0 -> "카드를 뒤집고 있어요."
            else -> "${participantCount}명 중 ${picks.size + 1}번째 사람, 카드를 골라 주세요."
        }
        Text(instruction, Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("card-draw-turn"),
            style = MaterialTheme.typography.titleMedium, color = Teal)
        Surface(shape = RoundedCornerShape(24.dp), color = Paper) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(24.dp)) {
                val gap = 18.dp
                val columns = ((maxWidth + gap) / (150.dp + gap)).toInt().coerceIn(1, minOf(4, participantCount))
                val cardWidth = minOf(200.dp, (maxWidth - gap * (columns - 1)) / columns)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(gap),
                    maxItemsInEachRow = columns,
                ) {
                    repeat(participantCount) { card ->
                        val turn = picks.indexOf(card) + 1
                        DrawCard(
                            number = card + 1,
                            turn = turn,
                            width = cardWidth,
                            canPick = flippingCard < 0 && completedDraw == null && turn == 0,
                            revealed = turn > 0 || completedDraw != null,
                            pays = card == payerCard,
                            onFlipFinished = {
                                if (flippingCard == card) {
                                    flippingCard = -1
                                    if (card == payerCard) {
                                        celebrate = true
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    }
                                }
                            },
                            onPick = {
                                if (flippingCard < 0 && payerCard !in picks && card !in picks) {
                                    flippingCard = card
                                    picks += card
                                }
                            },
                        )
                    }
                }
            }
        }
        Hint(if (completedDraw == null) "한 장을 뒤집은 뒤 다음 사람에게 차례를 넘겨 주세요."
            else "나머지 ${participantCount - 1}명은 0원이에요.")
    }
    if (celebrate) {
        LottieAnimation(
            composition = confetti,
            progress = { confettiProgress },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun DrawCard(number: Int, turn: Int, width: Dp, canPick: Boolean, revealed: Boolean, pays: Boolean,
    onFlipFinished: () -> Unit, onPick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(CARD_FLIP_MS),
        finishedListener = { if (it == 180f) onFlipFinished() },
        label = "card-flip",
    )
    val faceUp = rotation >= 90f
    val haptic = LocalHapticFeedback.current
    val label = when {
        !faceUp -> "카드 $number"
        pays -> "${turn}번째로 뽑은 카드, 당첨"
        turn == 0 -> "카드 $number, 통과"
        else -> "${turn}번째로 뽑은 카드, 통과"
    }
    Surface(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onPick() },
        enabled = canPick,
        modifier = Modifier.width(width).aspectRatio(.69f).testTag("draw-card-$number")
            .graphicsLayer {
                rotationY = if (faceUp) rotation - 180f else rotation
                cameraDistance = size.width.coerceAtLeast(1f) * 4f
            }
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp,
    ) {
        Box(Modifier.clearAndSetSemantics {}) {
            if (faceUp) CardDrawFace(number, pays, sealSize = width * .42f)
            else CardDrawBack(number, logoSize = width * .52f)
        }
    }
}
