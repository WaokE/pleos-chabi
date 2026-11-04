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
    var matrix by remember(text) { mutableStateOf<QrCode?>(null) }
    var failed by remember(text) { mutableStateOf(false) }
    LaunchedEffect(text, encoder) {
        try { matrix = encoder.encode(text) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    PleosFormDialog("휴대폰으로 요약 받기", onDismiss, true, onConfirm = onDismiss, confirmLabel = "닫기", confirmTag = "close-qr", showCancel = false) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconTile(DriveSymbol.SHARE)
            Text("휴대폰 카메라로 QR을 찍어 주세요.", style = MaterialTheme.typography.bodySmall)
        }
        Text("정산 요약이 글자로 보여요. 복사해서 메신저에 붙여 넣을 수 있어요.", style = MaterialTheme.typography.labelSmall, color = Muted)
        val code = matrix
        if (code != null) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.widthIn(max = 600.dp).fillMaxWidth().aspectRatio(1f).testTag("settlement-qr")
                .semantics { contentDescription = "정산 요약 텍스트 QR 코드" }) {
                drawRect(Color.White)
                val unit = floor(minOf(size.width, size.height) / code.size).coerceAtLeast(1f)
                val start = Offset((size.width - code.size * unit) / 2, (size.height - code.size * unit) / 2)
                for (y in 0 until code.size) for (x in 0 until code.size) if (code[x, y]) {
                    drawRect(Color.Black, start + Offset(x * unit, y * unit), Size(unit, unit))
                }
            }
        } else if (failed) Hint("요약 QR을 만들지 못했어요. 내역 복사나 공유를 이용해 주세요.", true)
        else CircularProgressIndicator()
        Text("인터넷 없이 전달돼요. 송금이나 메시지 전송은 하지 않아요.",
            style = MaterialTheme.typography.labelMedium, color = Muted)
    }
}
