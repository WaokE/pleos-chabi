package com.example.chabi.ui

import android.annotation.SuppressLint
import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

private const val OEM_TOKENS = "com.android.oem.tokens"

// AAOS supplies the optional token library at runtime. Its public IDs are assigned by the device.
@SuppressLint("DiscouragedApi")
internal fun readOemPalette(resources: Resources, fallback: PleosColors): PleosColors? {
    val enabled = resources.getIdentifier("enable_oem_tokens", "bool", OEM_TOKENS)
    val style = resources.getIdentifier("OemStyle", "style", OEM_TOKENS)
    if (enabled == 0 || style == 0 || !resources.getBoolean(enabled)) return null

    val theme = resources.newTheme().apply { applyStyle(style, true) }
    val value = TypedValue()
    fun color(name: String): Color? {
        val attr = resources.getIdentifier(name, "attr", OEM_TOKENS)
        if (attr == 0 || !theme.resolveAttribute(attr, value, true)) return null
        if (value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) return Color(value.data)
        return if (value.resourceId != 0) {
            runCatching { Color(resources.getColor(value.resourceId, theme)) }.getOrNull()
        } else null
    }

    val accent = color("colorPrimary") ?: return null
    val surface = color(if (fallback.dark) "colorSurfaceA" else "colorSecondaryContainer") ?: return null
    val candidate = fallback.copy(
        background = color("colorSurfaceContainer") ?: return null,
        surface = surface,
        popup = color("colorSecondaryContainer") ?: return null,
        high = color("colorSecondaryContainerVariant2") ?: return null,
        text = color("colorOnSurface") ?: return null,
        secondary = color("colorOnSurfaceVariant1") ?: return null,
        outline = color("colorOutlineVariant1") ?: return null,
        accent = accent,
        onAccent = color("colorOnPrimary") ?: return null,
        accentContainer = lerp(surface, accent, .12f),
    )
    return candidate.takeIf { it.hasLegibleText() }
}

private fun PleosColors.hasLegibleText(): Boolean {
    fun legible(foreground: Color, background: Color): Boolean {
        if (foreground.alpha != 1f || background.alpha != 1f) return false
        val a = foreground.luminance()
        val b = background.luminance()
        return (max(a, b) + .05f) / (min(a, b) + .05f) >= 4.5f
    }
    val surfaces = listOf(background, surface, popup, high, accentContainer)
    return surfaces.all { legible(text, it) && legible(secondary, it) && legible(accent, it) } &&
        legible(onAccent, accent) && legible(onAccent, lerp(accent, text, .12f)) &&
        legible(error, surface) && legible(error, popup)
}
