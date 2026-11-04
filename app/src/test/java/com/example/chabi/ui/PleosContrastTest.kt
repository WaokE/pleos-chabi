package com.example.chabi.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class PleosContrastTest {
    private fun luminance(color: Color): Double {
        fun channel(v: Float): Double = if (v <= .04045) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
        return .2126 * channel(color.red) + .7152 * channel(color.green) + .0722 * channel(color.blue)
    }
    private fun check(label: String, foreground: Color, background: Color) {
        val a = luminance(foreground)
        val b = luminance(background)
        val ratio = (max(a, b) + .05) / (min(a, b) + .05)
        assertTrue("$label has only $ratio:1 contrast", ratio >= 4.5)
    }
    @Test fun `light and dark foregrounds meet 4 point 5 contrast across their surfaces`() {
        for (dark in listOf(false, true)) {
            val colors = pleosColors(dark)
            for (background in listOf(colors.background, colors.surface, colors.popup, colors.high)) {
                check("$dark primary", colors.text, background)
                check("$dark secondary", colors.secondary, background)
            }
            check("$dark accent", colors.accent, colors.accentContainer)
            check("$dark accent on background", colors.accent, colors.background)
            check("$dark warning", colors.warning, colors.warningContainer)
            check("$dark error", colors.error, colors.surface)
            check("$dark toggle", colors.surface, colors.text)
        }
    }
    @Test fun `filled and inverse controls retain contrast including pressed state`() {
        for (background in listOf(PleosTokens.gray600, PleosTokens.gray700, PleosTokens.gray500, PleosTokens.gray900)) {
            check("button and inverse text", Color.White, background)
        }
        for (dark in listOf(false, true)) {
            val c = pleosColors(dark)
            val onAccent = if (dark) PleosTokens.gray900 else Color.White
            check("$dark branded primary action", onAccent, c.accent)
            check("$dark selected pill", c.accent, c.accentContainer)
            check("$dark pressed primary action", onAccent, androidx.compose.ui.graphics.lerp(c.accent, c.text, .12f))
        }
        check("summary accent", Color(0xFFA4E2C1), PleosTokens.gray900)
        check("summary label", Color(0xFFD9DADD), PleosTokens.gray900)
    }
}
