package com.emberr.presentation.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.emberr.domain.util.system.isDesktopPlatform
import kotlin.math.abs

private const val TrackpadSwipeThreshold = 3f
private const val TrackpadSwipeGestureGapMillis = 300L

@Composable
fun Modifier.swipeDatesWithTrackpad(onSwipePrevious: () -> Unit, onSwipeNext: () -> Unit): Modifier {
    if (!isDesktopPlatform) return this
    val currentOnSwipePrevious by rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext by rememberUpdatedState(onSwipeNext)

    return pointerInput(Unit) {
        var swipedAmount = 0f
        var lastSwipeEventMillis = 0L
        var hasSwipedThisGesture = false

        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val amount = horizontalSwipeAmount(event) ?: continue
                val eventMillis = event.changes.firstOrNull()?.uptimeMillis ?: continue
                event.changes.forEach { change -> change.consume() }

                if (eventMillis - lastSwipeEventMillis > TrackpadSwipeGestureGapMillis) {
                    swipedAmount = 0f
                    hasSwipedThisGesture = false
                }
                lastSwipeEventMillis = eventMillis
                if (hasSwipedThisGesture) continue

                swipedAmount += amount
                if (swipedAmount <= -TrackpadSwipeThreshold) {
                    hasSwipedThisGesture = true
                    currentOnSwipePrevious()
                } else if (swipedAmount >= TrackpadSwipeThreshold) {
                    hasSwipedThisGesture = true
                    currentOnSwipeNext()
                }
            }
        }
    }
}

private fun horizontalSwipeAmount(event: PointerEvent): Float? {
    if (event.type == PointerEventType.Press) {
        return when {
            event.buttons.isBackPressed -> -1f
            event.buttons.isForwardPressed -> 1f
            else -> null
        }
    }
    if (event.type != PointerEventType.Scroll) return null
    val scrollDelta = event.changes.firstOrNull()?.scrollDelta ?: return null
    return if (abs(scrollDelta.x) > abs(scrollDelta.y)) scrollDelta.x else null
}
