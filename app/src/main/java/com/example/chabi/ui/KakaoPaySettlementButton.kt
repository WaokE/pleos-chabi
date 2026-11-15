package com.example.chabi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.chabi.R
import com.example.chabi.handoff.QrCode
import com.example.chabi.handoff.QrEncoder
import kotlinx.coroutines.CancellationException
import kotlin.math.floor

private const val KAKAOPAY_SETTLEMENT_URL = "https://link.kakaopay.com/__/-cYSBaN"

@Composable
internal fun KakaoPaySettlementButton(qrEncoder: QrEncoder) {
    var showQr by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().heightIn(min = 80.dp)
            .testTag("open-kakaopay-settlement")
            .clickable(role = Role.Button) { showQr = true },
        contentAlignment = Alignment.Center,
    ) {
        // Official Regular artwork. Keep the image and its proportions intact.
        Image(
            painterResource(R.drawable.kakaopay_split_bill_button),
            contentDescription = "카카오페이 1/N 정산",
            modifier = Modifier.fillMaxWidth().aspectRatio(220f / 52f),
        )
    }

    val dismiss = { showQr = false }
    PleosModal(showQr, onDismiss = dismiss) {
        KakaoPaySettlementQr(qrEncoder, onDismiss = dismiss)
    }
}

@Composable
private fun KakaoPaySettlementQr(encoder: QrEncoder, onDismiss: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var matrix by remember(encoder, attempt) { mutableStateOf<QrCode?>(null) }
    var failed by remember(encoder, attempt) { mutableStateOf(false) }
    LaunchedEffect(encoder, attempt) {
        try {
            matrix = encoder.encode(KAKAOPAY_SETTLEMENT_URL)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        }
    }
    val qrWidth = (LocalConfiguration.current.screenHeightDp - 355).coerceIn(136, 360).dp
    PleosForm(
        title = "카카오페이 1/N 정산",
        onDismiss = onDismiss,
        confirmEnabled = true,
        onConfirm = onDismiss,
        confirmLabel = "닫기",
        confirmTag = "close-kakaopay-settlement",
        showCancel = false,
    ) {
        val code = matrix
        when {
            code != null -> {
                Text("휴대폰 카메라로 스캔한 뒤 카카오페이에서 금액을 입력해 주세요.", style = MaterialTheme.typography.bodyMedium)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Canvas(
                        Modifier.widthIn(max = qrWidth).fillMaxWidth().aspectRatio(1f)
                            .testTag("kakaopay-settlement-qr")
                            .semantics { contentDescription = "카카오페이 1/N 정산 링크 QR 코드" },
                    ) {
                        drawRect(Color.White)
                        val unit = floor(size.minDimension / code.size).coerceAtLeast(1f)
                        val origin = Offset((size.width - code.size * unit) / 2, (size.height - code.size * unit) / 2)
                        for (y in 0 until code.size) for (x in 0 until code.size) if (code[x, y]) {
                            drawRect(Color.Black, origin + Offset(x * unit, y * unit), Size(unit, unit))
                        }
                    }
                }
            }
            failed -> {
                Hint("정산 QR을 만들지 못했어요.", error = true)
                PleosOutlinedButton(onClick = { attempt++ }, modifier = Modifier.fillMaxWidth().testTag("retry-kakaopay-qr")) {
                    Text("다시 만들기")
                }
            }
            else -> CircularProgressIndicator()
        }
    }
}
