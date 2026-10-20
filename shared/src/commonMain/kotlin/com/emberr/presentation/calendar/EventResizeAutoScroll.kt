package com.emberr.presentation.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val AutoScrollEdgeZone = 48.dp
private val AutoScrollFastestStepPerFrame = 16.dp

class EventResizeAutoScroll(
    private val scrollState: ScrollState,
    private val edgeZonePx: Float,
    private val fastestStepPx: Float
) {
    var viewportCoordinates: LayoutCoordinates? = null
    var visibleTopInsetPx = 0f
    var visibleBottomInsetPx = 0f

    fun scrollTowards(pointerInWindow: Offset): Boolean {
        val viewport = viewportCoordinates?.takeIf { it.isAttached }?.boundsInWindow() ?: return false
        val upperZoneEnd = viewport.top + visibleTopInsetPx + edgeZonePx
        val lowerZoneStart = viewport.bottom - visibleBottomInsetPx - edgeZonePx
        val step = when {
            pointerInWindow.y < upperZoneEnd -> -fastestStepPx * closeness(upperZoneEnd - pointerInWindow.y)
            pointerInWindow.y > lowerZoneStart -> fastestStepPx * closeness(pointerInWindow.y - lowerZoneStart)
            else -> 0f
        }
        return step != 0f && scrollState.dispatchRawDelta(step) != 0f
    }

    private fun closeness(distanceIntoZone: Float): Float = (distanceIntoZone / edgeZonePx).coerceIn(0f, 1f)
}

@Composable
fun rememberEventResizeAutoScroll(scrollState: ScrollState, visibleTopInset: Dp, visibleBottomInset: Dp): EventResizeAutoScroll {
    val density = LocalDensity.current
    val autoScroll = remember(scrollState, density) {
        with(density) {
            EventResizeAutoScroll(
                scrollState = scrollState,
                edgeZonePx = AutoScrollEdgeZone.toPx(),
                fastestStepPx = AutoScrollFastestStepPerFrame.toPx()
            )
        }
    }
    SideEffect {
        with(density) {
            autoScroll.visibleTopInsetPx = visibleTopInset.toPx()
            autoScroll.visibleBottomInsetPx = visibleBottomInset.toPx()
        }
    }
    return autoScroll
}
