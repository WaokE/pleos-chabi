@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.example.chabi.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chabi.R

/** Raw grayscale and type sizes transcribed from the official Pleos Foundations assets. */
object PleosTokens {
    val white = Color(0xFFFFFFFF)
    val gray50 = Color(0xFFF7F8FA)
    val gray100 = Color(0xFFEDEEF2)
    val gray200 = Color(0xFFE0E1E6)
    val gray300 = Color(0xFFC7C9CE)
    val gray400 = Color(0xFFA4A8AE)
    val gray500 = Color(0xFF686A72)
    val gray600 = Color(0xFF44464E)
    val gray700 = Color(0xFF313236)
    val gray800 = Color(0xFF1D1E21)
    val gray900 = Color(0xFF131417)
    const val minTouch = 64
    const val buttonMedium = 80
    const val buttonLarge = 100
    const val fieldHeight = 80
    const val labelSmall = 20
    const val labelMedium = 24
    const val bodySmall = 26
    const val bodyMedium = 28
    const val bodyLarge = 30
    const val titleSmall = 32
    const val titleMedium = 36
    const val titleLarge = 40
    const val headlineSmall = 48
    const val headlineMedium = 56
    const val headlineLarge = 60
}

@Immutable
data class PleosColors(
    val dark: Boolean,
    val background: Color, val surface: Color, val popup: Color, val high: Color,
    val text: Color, val secondary: Color, val outline: Color,
    val accent: Color, val onAccent: Color, val accentContainer: Color,
    val warning: Color, val warningContainer: Color, val error: Color,
)

internal fun pleosColors(dark: Boolean) = if (dark) PleosColors(
    true, PleosTokens.gray900, PleosTokens.gray800, PleosTokens.gray900, PleosTokens.gray700,
    PleosTokens.white, Color(0xFFD9DADD), PleosTokens.gray600,
    Color(0xFFA4E2C1), PleosTokens.gray900, Color(0xFF193D32), Color(0xFFFFCB93), Color(0xFF422B18), Color(0xFFFFB4AB),
) else PleosColors(
    false, PleosTokens.gray50, PleosTokens.white, PleosTokens.white, PleosTokens.gray100,
    PleosTokens.gray900, Color(0xFF393A3D), PleosTokens.gray200,
    Color(0xFF126B58), Color.White, Color(0xFFE4F1E6), Color(0xFF884010), Color(0xFFFFEDD9), Color(0xFFB3261E),
)

val LocalPleosColors = staticCompositionLocalOf { pleosColors(false) }
val Ink: Color @Composable get() = LocalPleosColors.current.text
val Muted: Color @Composable get() = LocalPleosColors.current.secondary
val Paper: Color @Composable get() = LocalPleosColors.current.background
val Line: Color @Composable get() = LocalPleosColors.current.outline
val Teal: Color @Composable get() = LocalPleosColors.current.accent
val Mint: Color @Composable get() = LocalPleosColors.current.accentContainer
val Orange: Color @Composable get() = LocalPleosColors.current.warning

private val AstaSans by lazy { FontFamily(
    Font(R.font.asta_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.asta_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.asta_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.asta_sans, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
) }

private fun type(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = AstaSans, fontSize = size.sp, lineHeight = (size * 1.35f).sp, fontWeight = weight,
)

@Composable
fun DriveTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    val colors = remember(resources, configuration, dark) {
        val fallback = pleosColors(dark)
        readOemPalette(resources, fallback) ?: fallback
    }
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPleosColors provides colors) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = colors.accent, onPrimary = colors.onAccent,
                primaryContainer = colors.accentContainer, onPrimaryContainer = colors.accent,
                secondary = colors.accent, onSecondary = colors.onAccent,
                secondaryContainer = colors.accentContainer, onSecondaryContainer = colors.accent,
                background = colors.background, onBackground = colors.text, surface = colors.surface,
                onSurface = colors.text, surfaceVariant = colors.high, onSurfaceVariant = colors.secondary,
                outline = colors.outline, outlineVariant = colors.outline, error = colors.error,
                surfaceContainer = colors.surface, surfaceContainerHigh = colors.high,
                surfaceContainerHighest = colors.high, surfaceTint = Color.Transparent,
            ),
            typography = Typography(
                headlineLarge = type(60, FontWeight.Bold), headlineMedium = type(56, FontWeight.Bold),
                headlineSmall = type(48, FontWeight.Bold), titleLarge = type(40, FontWeight.Bold),
                titleMedium = type(36, FontWeight.Bold), titleSmall = type(32, FontWeight.Bold),
                bodyLarge = type(30), bodyMedium = type(28), bodySmall = type(26),
                labelLarge = type(28, FontWeight.SemiBold), labelMedium = type(24), labelSmall = type(20),
            ),
        ) { CompositionLocalProvider(LocalContentColor provides colors.text) { content() } }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, dark: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(32.dp),
        color = if (dark) PleosTokens.gray900 else MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun Eyebrow(text: String, color: Color = Teal) {
    Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
fun Hint(text: String, error: Boolean = false) {
    Text(text, color = if (error) MaterialTheme.colorScheme.error else Muted,
        style = MaterialTheme.typography.bodySmall, maxLines = if (error) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis)
}

@Composable
fun MoneyRow(label: String, value: String, strong: Boolean = false, inverse: Boolean = false) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (!inverse && maxWidth < PleosBreakpoints.NarrowRowWidth) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
            Text(value, color = Ink, style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold)
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = if (inverse) Color(0xFFD9DADD) else Muted,
                style = if (inverse) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall)
            Text(value, color = if (inverse) Color.White else Ink, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (strong) 32.sp else 26.sp)
        }
    }
}

@Composable
fun SectionTitle(title: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (detail != null) Hint(detail)
    }
}

/** Window-size thresholds shared by the screens' responsive layouts. */
object PleosBreakpoints {
    /** At or above this width, screens use two columns (trip list + map, costs + people, result + shares). */
    val TwoColumnWidth = 1200.dp
    /** Below this width, the step footer stacks its summary above the action button. */
    val FooterStackWidth = 1000.dp
    /** Below this height, step headings use smaller type and spacing. */
    val CompactHeight = 850.dp
    /** Below this width, money rows stack the label above the value. */
    val NarrowRowWidth = 600.dp
}
