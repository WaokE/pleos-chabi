package com.example.chabi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Material behavior with Pleos medium geometry, palette, state feedback and typography. */
@Composable
fun PleosButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp), contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colors = LocalPleosColors.current
    val haptic = LocalHapticFeedback.current
    androidx.compose.material3.Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        modifier = modifier.heightIn(min = PleosTokens.buttonMedium.dp).defaultMinSize(minWidth = 80.dp), enabled = enabled,
        shape = shape, contentPadding = contentPadding, interactionSource = source,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) lerp(colors.accent, colors.text, .12f) else colors.accent,
            contentColor = if (colors.dark) PleosTokens.gray900 else Color.White, disabledContainerColor = colors.high, disabledContentColor = colors.secondary,
        ), content = content,
    )
}

@Composable
fun PleosOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp), contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalPleosColors.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    androidx.compose.material3.OutlinedButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        modifier = modifier.heightIn(min = 80.dp).defaultMinSize(minWidth = 80.dp), enabled = enabled,
        shape = shape, contentPadding = contentPadding, interactionSource = source,
        border = BorderStroke(2.dp, colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (pressed) colors.high else colors.surface,
            contentColor = colors.text, disabledContentColor = colors.secondary), content = content,
    )
}

@Composable
fun PleosTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp), content: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    androidx.compose.material3.TextButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        modifier = modifier.heightIn(min = 64.dp).widthIn(min = 80.dp), enabled = enabled,
        contentPadding = contentPadding, shape = RoundedCornerShape(16.dp),
    ) { ProvideTextStyle(MaterialTheme.typography.bodySmall) { content() } }
}

@Composable
fun NumberPill(text: String, selected: Boolean = false, onClick: () -> Unit) {
    val colors = LocalPleosColors.current
    val haptic = LocalHapticFeedback.current
    Surface(onClick = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        modifier = Modifier.heightIn(min = 64.dp).widthIn(min = 80.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(24.dp), color = if (selected) colors.accentContainer else colors.surface,
        contentColor = if (selected) colors.accent else colors.secondary,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.accent else colors.outline)) {
        Box(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PleosTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null, suffix: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val c = LocalPleosColors.current
    androidx.compose.material3.OutlinedTextField(value = value, onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 80.dp), shape = RoundedCornerShape(24.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        label = label?.let { { ProvideTextStyle(MaterialTheme.typography.labelMedium) { it() } } },
        suffix = suffix?.let { { ProvideTextStyle(MaterialTheme.typography.bodySmall) { it() } } },
        singleLine = singleLine, maxLines = maxLines, isError = isError, keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.popup, unfocusedContainerColor = c.popup,
            focusedTextColor = c.text, unfocusedTextColor = c.text, cursorColor = c.text,
            focusedBorderColor = c.accent, unfocusedBorderColor = c.outline,
            focusedLabelColor = c.accent, unfocusedLabelColor = c.secondary,
        ))
}

@Composable
fun PleosCheckbox(checked: Boolean) {
    val accent = Teal
    val outline = LocalPleosColors.current.secondary
    val inkOnAccent = if (LocalPleosColors.current.dark) PleosTokens.gray900 else Color.White
    Canvas(Modifier.size(48.dp)) {
        if (checked) {
            drawRoundRect(accent, cornerRadius = CornerRadius(12.dp.toPx()))
            val check = Path().apply {
                moveTo(size.width * .24f, size.height * .51f)
                lineTo(size.width * .43f, size.height * .69f)
                lineTo(size.width * .78f, size.height * .32f)
            }
            drawPath(check, inkOnAccent, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        } else drawRoundRect(outline, topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), cornerRadius = CornerRadius(10.dp.toPx()),
            style = Stroke(4.dp.toPx()))
    }
}

@Composable
fun PleosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = LocalPleosColors.current
    val haptic = LocalHapticFeedback.current
    Box(Modifier.size(width = 96.dp, height = 80.dp)
        .toggleable(checked, role = Role.Switch, onValueChange = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onCheckedChange(it) }),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = 80.dp, height = 48.dp)) {
            val track = if (checked) c.accent else if (c.dark) PleosTokens.gray300 else PleosTokens.gray600
            val thumb = if (c.dark) PleosTokens.gray900 else Color.White
            drawRoundRect(track, cornerRadius = CornerRadius(24.dp.toPx()))
            drawCircle(thumb, radius = 18.dp.toPx(), center = Offset(if (checked) size.width - 24.dp.toPx() else 24.dp.toPx(), size.height / 2))
        }
    }
}

/** Official medium-popup width, scrollable body, and always reachable action area. */
@Composable
fun PleosFormDialog(title: String, onDismiss: () -> Unit, confirmEnabled: Boolean,
    onConfirm: () -> Unit, confirmLabel: String = "저장", confirmTag: String = "confirm-dialog", showCancel: Boolean = true, body: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().imePadding().padding(24.dp)
            .semantics { testTagsAsResourceId = true }, contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 760.dp).fillMaxWidth().heightIn(max = maxHeight),
                shape = RoundedCornerShape(48.dp), color = LocalPleosColors.current.popup,
                border = BorderStroke(2.dp, Line)) {
                Column(Modifier.padding(48.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp), content = body)
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (showCancel) PleosOutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 100.dp),
                            shape = RoundedCornerShape(32.dp)) { Text("취소") }
                        PleosButton(onClick = onConfirm, enabled = confirmEnabled,
                            modifier = Modifier.weight(1f).heightIn(min = 100.dp).testTag(confirmTag),
                            shape = RoundedCornerShape(32.dp)) { Text(confirmLabel) }
                    }
                }
            }
        }
    }
}
