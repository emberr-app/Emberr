package com.emberr.presentation.shared.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.canvas.horizontalScrollArrivesAsBackAndForwardButtons
import com.emberr.ui.theme.LocalPaperGrainMover
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs

private const val SETTLE_FRACTION = 0.16f
private const val SETTLE_THRESHOLD_PX = 0.5f
private const val MIN_STEP_PX = 1f

val DefaultMouseWheelNotch: Dp = 90.dp

@Composable
fun Modifier.horizontalScrollFromBackAndForwardButtons(state: ScrollableState): Modifier {
    if (!horizontalScrollArrivesAsBackAndForwardButtons) return this

    val stepPx = with(LocalDensity.current) { DefaultMouseWheelNotch.toPx() }
    val glide = rememberWheelGlide(state, horizontal = true)

    return this.pointerInput(state, stepPx) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Press) continue
                val direction = when {
                    event.buttons.isBackPressed -> -1f
                    event.buttons.isForwardPressed -> 1f
                    else -> continue
                }
                event.changes.forEach { it.consume() }
                glide.pendingPx += direction * stepPx
                glide.wakeups.trySend(Unit)
            }
        }
    }
}

@Composable
private fun rememberWheelGlide(state: ScrollableState, horizontal: Boolean): WheelGlide {
    val glide = remember(state) { WheelGlide() }
    val paperGrainMover by rememberUpdatedState(LocalPaperGrainMover.current)

    LaunchedEffect(state) {
        while (true) {
            glide.wakeups.receive()
            state.scroll {
                while (true) {
                    withFrameNanos {}
                    val remaining = glide.pendingPx
                    if (abs(remaining) < SETTLE_THRESHOLD_PX) {
                        glide.pendingPx = 0f
                        break
                    }
                    val requested = remaining * SETTLE_FRACTION
                    val consumed = scrollBy(if (abs(requested) < MIN_STEP_PX) remaining else requested)
                    paperGrainMover.moveGrainBy(if (horizontal) Offset(-consumed, 0f) else Offset(0f, -consumed))
                    glide.pendingPx -= consumed
                    if (abs(consumed) < 0.01f) {
                        glide.pendingPx = 0f
                        break
                    }
                }
            }
        }
    }

    return glide
}

private class WheelGlide {
    var pendingPx = 0f
    val wakeups = Channel<Unit>(Channel.CONFLATED)
}
