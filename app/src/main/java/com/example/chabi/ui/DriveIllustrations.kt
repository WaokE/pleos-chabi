package com.example.chabi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/** Original code-drawn artwork; never a route or a vehicle-data visualization. */
@Composable
fun DriveBuddy(modifier: Modifier = Modifier, celebrating: Boolean = false) {
    val c = LocalPleosColors.current
    Canvas(modifier) {
        withTransform({ scale(size.width / 240f, size.height / 200f, pivot = Offset.Zero) }) {
            val body = if (c.dark) Color(0xFFA9DCB2) else Color(0xFFC2E5A8)
            val deep = Color(0xFF203C33)
            drawOval(deep.copy(alpha = .09f), Offset(35f, 164f), Size(170f, 21f))
            drawRoundRect(deep, Offset(41f, 132f), Size(27f, 42f), CornerRadius(12f))
            drawRoundRect(deep, Offset(172f, 132f), Size(27f, 42f), CornerRadius(12f))
            val roof = Path().apply { moveTo(48f, 99f); lineTo(64f, 49f); quadraticTo(68f, 37f, 83f, 37f); lineTo(157f, 37f); quadraticTo(172f, 37f, 177f, 51f); lineTo(194f, 99f); close() }
            drawPath(roof, body)
            val glass = Path().apply { moveTo(68f, 89f); lineTo(80f, 53f); lineTo(158f, 53f); lineTo(171f, 89f); close() }
            drawPath(glass, deep)
            drawLine(Color.White.copy(alpha = .24f), Offset(96f, 57f), Offset(86f, 82f), 9f, StrokeCap.Round)
            drawRoundRect(body, Offset(31f, 88f), Size(179f, 71f), CornerRadius(27f))
            drawRoundRect(Color(0xFFD8EFC7), Offset(38f, 96f), Size(165f, 21f), CornerRadius(15f))
            drawRoundRect(deep, Offset(17f, 86f), Size(23f, 17f), CornerRadius(8f))
            drawRoundRect(deep, Offset(200f, 86f), Size(23f, 17f), CornerRadius(8f))
            drawCircle(Color(0xFFFFF9D7), 16f, Offset(62f, 121f)); drawCircle(Color(0xFFFFF9D7), 16f, Offset(178f, 121f))
            drawCircle(deep, 4.8f, Offset(96f, 119f)); drawCircle(deep, 4.8f, Offset(144f, 119f))
            val smile = Path().apply { moveTo(109f, 129f); quadraticTo(120f, 140f, 131f, 129f) }
            drawPath(smile, deep, style = Stroke(4f, cap = StrokeCap.Round))
            drawOval(Color(0xFFED9A78).copy(alpha = .55f), Offset(81f, 128f), Size(13f, 7f))
            drawOval(Color(0xFFED9A78).copy(alpha = .55f), Offset(146f, 128f), Size(13f, 7f))
            drawRoundRect(Color.White, Offset(99f, 148f), Size(42f, 13f), CornerRadius(5f))
            if (celebrating) {
                listOf(Offset(25f, 37f), Offset(205f, 31f), Offset(222f, 65f)).forEachIndexed { i, p ->
                    val color = if (i == 1) Color(0xFFF2C55C) else Color(0xFFB9A5E3)
                    drawLine(color, p - Offset(7f, 0f), p + Offset(7f, 0f), 4f, StrokeCap.Round)
                    drawLine(color, p - Offset(0f, 7f), p + Offset(0f, 7f), 4f, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
fun DriveJourneyArt(modifier: Modifier = Modifier) {
    val c = LocalPleosColors.current
    Box(modifier.clip(RoundedCornerShape(40.dp)).background(c.accentContainer), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val road = Path().apply {
                moveTo(-size.width * .1f, size.height * .7f)
                cubicTo(size.width * .2f, size.height * .1f, size.width * .5f, size.height * 1.2f, size.width * 1.12f, size.height * .3f)
            }
            drawPath(road, c.surface.copy(alpha = .75f), style = Stroke(76.dp.toPx(), cap = StrokeCap.Round))
            drawPath(road, c.accent.copy(alpha = .15f), style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 12.dp.toPx()))))
            drawCircle(Color(0xFFF3D482), 26.dp.toPx(), Offset(size.width * .82f, size.height * .2f))
            drawCircle(c.accent.copy(alpha = .13f), 14.dp.toPx(), Offset(size.width * .18f, size.height * .22f))
            drawCircle(c.accent.copy(alpha = .13f), 7.dp.toPx(), Offset(size.width * .12f, size.height * .3f))
        }
        DriveBuddy(Modifier.width(260.dp).height(220.dp), celebrating = true)
    }
}

enum class DriveSymbol { ROUTE, WALLET, PEOPLE, CHECK, FUEL, CLOCK }

@Composable
fun DriveIcon(symbol: DriveSymbol, modifier: Modifier = Modifier, tint: Color = Teal) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) }) {
            val stroke = Stroke(1.9f, cap = StrokeCap.Round)
            fun line(a: Offset, b: Offset) = drawLine(tint, a, b, 1.9f, StrokeCap.Round)
            when (symbol) {
                DriveSymbol.ROUTE -> {
                    drawCircle(tint, 2.4f, Offset(5f, 18f)); drawCircle(tint, 2.4f, Offset(19f, 6f))
                    val path = Path().apply { moveTo(8f, 18f); lineTo(15f, 18f); cubicTo(23f, 18f, 23f, 12f, 15f, 12f); lineTo(10f, 12f); cubicTo(1f, 12f, 2f, 6f, 10f, 6f); lineTo(15f, 6f) }
                    drawPath(path, tint, style = stroke)
                }
                DriveSymbol.WALLET -> {
                    drawRoundRect(tint, Offset(3f, 5f), Size(18f, 15f), CornerRadius(3f), style = stroke)
                    drawRoundRect(tint, Offset(14f, 10f), Size(8f, 6f), CornerRadius(2f), style = stroke)
                    drawCircle(tint, 1f, Offset(17f, 13f))
                }
                DriveSymbol.PEOPLE -> {
                    drawCircle(tint, 3f, Offset(9f, 7f), style = stroke); drawCircle(tint, 2.5f, Offset(18f, 9f), style = stroke)
                    drawArc(tint, 180f, 180f, false, Offset(2f, 13f), Size(14f, 13f), style = stroke)
                    drawArc(tint, 245f, 115f, false, Offset(12f, 14f), Size(11f, 10f), style = stroke)
                }
                DriveSymbol.CHECK -> { line(Offset(5f, 12f), Offset(10f, 17f)); line(Offset(10f, 17f), Offset(20f, 6f)) }
                DriveSymbol.FUEL -> {
                    drawRoundRect(tint, Offset(4f, 3f), Size(11f, 18f), CornerRadius(2f), style = stroke)
                    drawRoundRect(tint, Offset(7f, 6f), Size(5f, 5f), CornerRadius(1f), style = stroke)
                    val p = Path().apply { moveTo(15f, 12f); lineTo(18f, 12f); lineTo(18f, 18f); quadraticTo(22f, 22f, 22f, 17f); lineTo(22f, 8f); lineTo(19f, 5f) }; drawPath(p, tint, style = stroke)
                }
                DriveSymbol.CLOCK -> { drawCircle(tint, 9f, Offset(12f, 12f), style = stroke); line(Offset(12f, 6f), Offset(12f, 12f)); line(Offset(12f, 12f), Offset(16f, 14f)) }
            }
        }
    }
}

@Composable
fun IconTile(symbol: DriveSymbol, modifier: Modifier = Modifier, warm: Boolean = false) {
    val c = LocalPleosColors.current
    Box(modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(if (warm) c.warningContainer else c.accentContainer), contentAlignment = Alignment.Center) {
        DriveIcon(symbol, Modifier.size(32.dp), if (warm) c.warning else c.accent)
    }
}

@Composable
fun CompanionFace(index: Int, modifier: Modifier = Modifier) {
    val c = LocalPleosColors.current
    val tones = if (c.dark) listOf(Color(0xFF284E40), Color(0xFF4A3823), Color(0xFF3B354D), Color(0xFF29454A))
        else listOf(Color(0xFFDFF0D4), Color(0xFFFFE9C8), Color(0xFFEAE2F8), Color(0xFFDDEFF0))
    Canvas(modifier) {
        drawCircle(tones[index % tones.size])
        val eyeY = size.height * .43f
        drawCircle(c.text, size.width * .035f, Offset(size.width * .37f, eyeY))
        drawCircle(c.text, size.width * .035f, Offset(size.width * .63f, eyeY))
        val smile = Path().apply { moveTo(size.width * .4f, size.height * .59f); quadraticTo(size.width * .5f, size.height * .69f, size.width * .6f, size.height * .59f) }
        drawPath(smile, c.text, style = Stroke(size.width * .025f, cap = StrokeCap.Round))
    }
}
