package com.example.chabi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chabi.R

@Composable
internal fun CardDrawBack(number: Int, logoSize: Dp) {
    val foil = Color(0xFFB6DBBB)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF205D4C), Color(0xFF102F2A))))) {
        Canvas(Modifier.matchParentSize().padding(7.dp)) {
            val corner = CornerRadius(12.dp.toPx())
            val frame = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), corner)) }
            clipPath(frame) {
                val gap = 15.dp.toPx()
                val lattice = foil.copy(alpha = .14f)
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(lattice, Offset(x, 0f), Offset(x + size.height, size.height), 1.dp.toPx())
                    drawLine(lattice, Offset(x, 0f), Offset(x - size.height, size.height), 1.dp.toPx())
                    x += gap
                }
            }
            drawRoundRect(foil.copy(alpha = .72f), cornerRadius = corner, style = Stroke(1.dp.toPx()))
        }
        CardCornerMarks(number, foil)
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(logoSize).clip(CircleShape).border(2.dp, foil, CircleShape),
            )
            Text(stringResource(R.string.app_name), color = Color(0xFFE6F2E2),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
internal fun CardDrawFace(number: Int, pays: Boolean, sealSize: Dp) {
    val paper = when {
        pays -> listOf(Color(0xFFFFEDB7), Color(0xFFE7C981))
        LocalPleosColors.current.dark -> listOf(Color(0xFFE0ECD9), Color(0xFFC1D0BD))
        else -> listOf(Color(0xFFFFFBEF), Color(0xFFE5ECDC))
    }
    val ink = if (pays) Color(0xFF654119) else Color(0xFF1B4B3A)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(paper))) {
        if (pays) Box(Modifier.matchParentSize().background(Brush.radialGradient(listOf(Color(0xFFFFF7DC), Color.Transparent))))
        Canvas(Modifier.matchParentSize().padding(7.dp)) {
            val frame = if (pays) Color(0xFFA66B27) else ink.copy(alpha = .36f)
            drawRoundRect(frame, cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(if (pays) 3.dp.toPx() else 1.dp.toPx()))
        }
        CardCornerMarks(number, ink)
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(sealSize).border(1.dp, ink, CircleShape), contentAlignment = Alignment.Center) {
                if (pays) {
                    Text("₩", color = ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    DriveIcon(DriveSymbol.CHECK, Modifier.size(sealSize * .52f), tint = ink)
                }
            }
            Text(if (pays) "당첨" else "통과", color = ink,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun BoxScope.CardCornerMarks(number: Int, ink: Color) {
    val mark = number.toString().padStart(2, '0')
    Text(mark, Modifier.align(Alignment.TopStart).padding(14.dp), color = ink,
        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    Text(mark, Modifier.align(Alignment.BottomEnd).padding(14.dp).rotate(180f), color = ink,
        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
}
