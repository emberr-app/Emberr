package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.WatchlistView
import com.emberr.domain.util.system.isDesktopPlatform
import kotlinx.coroutines.CancellationException

private val ViewDropLineWidth = 2.dp

internal data class ViewPillSpan(val left: Float, val right: Float) {
    val center: Float get() = (left + right) / 2f
}

@Stable
internal class WatchlistViewDragState {
    var draggedView by mutableStateOf<WatchlistView?>(null)
        private set
    var pointerX by mutableFloatStateOf(0f)
        private set
    private var grabX by mutableFloatStateOf(0f)
    val pillSpans = mutableStateMapOf<WatchlistView, ViewPillSpan>()

    fun start(view: WatchlistView, grabXInPill: Float) {
        val pillLeft = pillSpans[view]?.left ?: return
        draggedView = view
        grabX = grabXInPill
        pointerX = pillLeft + grabXInPill
    }

    fun moveTo(view: WatchlistView, pointerXInPill: Float) {
        if (draggedView != view) return
        val pillLeft = pillSpans[view]?.left ?: return
        pointerX = pillLeft + pointerXInPill
    }

    fun offsetOf(view: WatchlistView): Float {
        if (draggedView != view) return 0f
        val pillLeft = pillSpans[view]?.left ?: return 0f
        return pointerX - pillLeft - grabX
    }

    fun reset() {
        draggedView = null
    }
}

internal fun viewToDropBefore(views: List<WatchlistView>, pillSpans: Map<WatchlistView, ViewPillSpan>, pointerX: Float): WatchlistView? =
    views.firstOrNull { view -> pillSpans[view]?.let { pointerX < it.center } == true }

internal fun dropKeepsViewInPlace(views: List<WatchlistView>, draggedView: WatchlistView, beforeView: WatchlistView?): Boolean =
    beforeView == draggedView || beforeView == views.getOrNull(views.indexOf(draggedView) + 1)

internal fun viewsWithDroppedView(views: List<WatchlistView>, draggedView: WatchlistView, beforeView: WatchlistView?): List<WatchlistView> {
    val withoutDragged = views - draggedView
    val insertAt = beforeView?.let { withoutDragged.indexOf(it) }?.takeIf { it >= 0 } ?: withoutDragged.size
    return withoutDragged.toMutableList().apply { add(insertAt, draggedView) }
}

internal fun Modifier.watchlistViewDropLine(dragState: WatchlistViewDragState, views: List<WatchlistView>, lineColor: Color): Modifier =
    drawWithContent {
        drawContent()
        val draggedView = dragState.draggedView ?: return@drawWithContent
        val beforeView = viewToDropBefore(views, dragState.pillSpans, dragState.pointerX)
        if (dropKeepsViewInPlace(views, draggedView, beforeView)) return@drawWithContent
        val lineX = (
            if (beforeView == null) {
                views.lastOrNull()?.let { dragState.pillSpans[it]?.right }
            } else {
                dragState.pillSpans[beforeView]?.left
            }
        ) ?: return@drawWithContent
        drawLine(lineColor, Offset(lineX, 0f), Offset(lineX, size.height), ViewDropLineWidth.toPx())
    }

internal fun Modifier.watchlistViewPillDrag(
    dragState: WatchlistViewDragState,
    view: WatchlistView,
    isEnabled: Boolean,
    onDrop: () -> Unit,
    onLongPressWithoutDrag: () -> Unit
): Modifier = pointerInput(view, isEnabled) {
    if (!isEnabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (isDesktopPlatform) {
            if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
            dragState.start(view, grabXInPill = down.position.x)
            dragState.moveTo(view, dragStart.position.x)
            followDragUntilDrop(dragState, view, dragStart.id, onDrop)
            return@awaitEachGesture
        }

        val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        longPress.consume()
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == longPress.id }
                ?: return@awaitEachGesture
            if (change.changedToUpIgnoreConsumed()) {
                change.consume()
                onLongPressWithoutDrag()
                return@awaitEachGesture
            }
            if ((change.position - longPress.position).getDistance() > viewConfiguration.touchSlop) {
                dragState.start(view, grabXInPill = longPress.position.x)
                dragState.moveTo(view, change.position.x)
                change.consume()
                followDragUntilDrop(dragState, view, longPress.id, onDrop)
                return@awaitEachGesture
            }
            change.consume()
        }
    }
}

private suspend fun AwaitPointerEventScope.followDragUntilDrop(
    dragState: WatchlistViewDragState,
    view: WatchlistView,
    pointerId: PointerId,
    onDrop: () -> Unit
) {
    try {
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == pointerId }
            if (change == null) {
                dragState.reset()
                return
            }
            if (change.changedToUpIgnoreConsumed()) {
                change.consume()
                onDrop()
                return
            }
            dragState.moveTo(view, change.position.x)
            change.consume()
        }
    } catch (cancellation: CancellationException) {
        dragState.reset()
        throw cancellation
    }
}
