package com.example.chabi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.chabi.handoff.QrCode
import com.example.chabi.handoff.QrEncoder
import kotlinx.coroutines.CancellationException
import kotlin.math.floor

@Composable
fun QrHandoff(text: String, encoder: QrEncoder, onDismiss: () -> Unit) {
    var attempt by remember(text, encoder) { mutableIntStateOf(0) }
    var matrix by remember(text, encoder, attempt) { mutableStateOf<QrCode?>(null) }
    var failed by remember(text, encoder, attempt) { mutableStateOf(false) }
    LaunchedEffect(text, encoder, attempt) {
        try { matrix = encoder.encode(text) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    PleosFormDialog("휴대폰으로 요약 받기", onDismiss, true, onConfirm = onDismiss, confirmLabel = "닫기", confirmTag = "close-qr", showCancel = false) {
        val code = matrix
        when {
            code != null -> {
                Text("휴대폰 카메라로 정산 QR을 읽어 주세요.", style = MaterialTheme.typography.bodySmall)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.widthIn(max = 600.dp).fillMaxWidth().aspectRatio(1f).testTag("settlement-qr")
                        .semantics { contentDescription = "정산 요약 텍스트 QR 코드" }) {
                        drawRect(Color.White)
                        val unit = floor(minOf(size.width, size.height) / code.size).coerceAtLeast(1f)
                        val start = Offset((size.width - code.size * unit) / 2, (size.height - code.size * unit) / 2)
                        for (y in 0 until code.size) for (x in 0 until code.size) if (code[x, y]) {
                            drawRect(Color.Black, start + Offset(x * unit, y * unit), Size(unit, unit))
                        }
                    }
                }
                Text("카메라가 인식하지 않으면 텍스트 QR을 지원하는 스캐너를 사용해 주세요.",
                    style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            failed -> {
                Hint("정산 QR을 만들지 못했어요.", true)
                PleosOutlinedButton(onClick = { attempt++ }, modifier = Modifier.fillMaxWidth().testTag("retry-qr")) {
                    Text("다시 만들기")
                }
            }
            else -> CircularProgressIndicator()
        }
    }
}
