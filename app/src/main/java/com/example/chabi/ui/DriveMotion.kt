package com.example.chabi.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

private const val PAGE_DURATION_MS = 260
private const val PAGE_FADE_IN_DELAY_MS = 75
private const val PAGE_FADE_OUT_MS = 125
private const val MODAL_ENTER_MS = 200
private const val MODAL_EXIT_MS = 160
private const val MODAL_CONTENT = "modal-content"

@Composable
internal fun AnimatedStep(
    state: DriveUiState,
    modifier: Modifier = Modifier,
    content: @Composable (DriveUiState) -> Unit,
) {
    val maxOffset = with(LocalDensity.current) { 96.dp.roundToPx() }
    val latestState by rememberUpdatedState(state)
    val latestContent by rememberUpdatedState(content)
    AnimatedContent(
        targetState = state.step,
        modifier = modifier.clipToBounds(),
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            val slide = tween<IntOffset>(PAGE_DURATION_MS, easing = FastOutSlowInEasing)
            // A short slide keeps the main controls close to their original position.
            val incoming = slideInHorizontally(slide) { direction * minOf(it / 8, maxOffset) } +
                fadeIn(tween(durationMillis = PAGE_DURATION_MS - PAGE_FADE_IN_DELAY_MS, delayMillis = PAGE_FADE_IN_DELAY_MS))
            val outgoing = slideOutHorizontally(slide) { -direction * minOf(it / 8, maxOffset) } +
                fadeOut(tween(PAGE_FADE_OUT_MS))
            (incoming togetherWith outgoing).using(null)
        },
        label = "settlement-step",
    ) { step ->
        val current = latestState
        val active = step == current.step
        val lastState = remember { mutableStateOf(current) }
        if (active) SideEffect { lastState.value = current }
        val interactive = active && transition.currentState == EnterExitState.Visible
        Box(Modifier.fillMaxSize().then(if (interactive) Modifier else Modifier.blockInput())) {
            latestContent(if (active) current else lastState.value)
        }
    }
}

/** Keeps the window and its contents alive through the exit fade. */
@Composable
internal fun PleosModal(visible: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val visibility = remember { MutableTransitionState(false) }
    // Capture the dialog's form state in the surrounding screen, which is saved during parking restrictions.
    val savedContent = rememberSaveableStateHolder()
    visibility.targetState = visible
    if (!visible && visibility.isIdle && !visibility.currentState) {
        SideEffect { savedContent.removeState(MODAL_CONTENT) }
        return
    }

    Dialog(onDismissRequest = { if (visible) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        SideEffect {
            window?.setDimAmount(0f)
            window?.setWindowAnimations(0)
        }
        LaunchedEffect(visible) {
            if (!visible) {
                keyboard?.hide()
                focus.clearFocus(force = true)
            }
        }
        AnimatedVisibility(
            visibleState = visibility,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(MODAL_ENTER_MS)),
            exit = fadeOut(tween(MODAL_EXIT_MS)),
            label = "pleos-modal",
        ) {
            val interactive = visible && transition.currentState == EnterExitState.Visible
            savedContent.SaveableStateProvider(MODAL_CONTENT) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                        .then(if (interactive) Modifier else Modifier.blockInput()),
                ) { content() }
            }
        }
    }
}

private fun Modifier.blockInput() = this
    .clearAndSetSemantics { hideFromAccessibility() }
    .focusProperties { canFocus = false }
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
