package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.editor.GlobalEditorState
import kotlinx.coroutines.CancellationException

private val ViewDropLineWidth = 2.dp

internal fun viewToDropBefore(viewIds: List<String>, pillSpans: Map<String, HeaderSpan>, pointerX: Float): String? =
    viewIds.firstOrNull { viewId -> pillSpans[viewId]?.let { pointerX < it.center } == true }

internal fun dropKeepsViewInPlace(viewIds: List<String>, draggedViewId: String, beforeViewId: String?): Boolean =
    beforeViewId == draggedViewId || beforeViewId == viewIds.getOrNull(viewIds.indexOf(draggedViewId) + 1)

@Stable
internal class DatabaseViewDragState {
    var draggedViewId by mutableStateOf<String?>(null)
        private set
    var pointerX by mutableStateOf(0f)
        private set
    private var grabX by mutableStateOf(0f)
    val pillSpans = mutableStateMapOf<String, HeaderSpan>()

    val isDragging: Boolean get() = draggedViewId != null

    fun start(viewId: String, grabXInPill: Float) {
        val pillLeft = pillSpans[viewId]?.left ?: return
        draggedViewId = viewId
        grabX = grabXInPill
        pointerX = pillLeft + grabXInPill
    }

    fun moveTo(viewId: String, pointerXInPill: Float) {
        if (draggedViewId != viewId) return
        val pillLeft = pillSpans[viewId]?.left ?: return
        pointerX = pillLeft + pointerXInPill
    }

    fun shiftPointerBy(distance: Float) {
        pointerX += distance
    }

    fun offsetOf(viewId: String): Float {
        if (draggedViewId != viewId) return 0f
        val pillLeft = pillSpans[viewId]?.left ?: return 0f
        return pointerX - pillLeft - grabX
    }

    fun reset() {
        draggedViewId = null
    }
}

internal fun Modifier.viewDropLine(dragState: DatabaseViewDragState, viewIds: List<String>, lineColor: Color): Modifier =
    drawWithContent {
        drawContent()
        val draggedViewId = dragState.draggedViewId ?: return@drawWithContent
        val beforeViewId = viewToDropBefore(viewIds, dragState.pillSpans, dragState.pointerX)
        if (dropKeepsViewInPlace(viewIds, draggedViewId, beforeViewId)) return@drawWithContent
        val lineX = (
            if (beforeViewId == null) {
                viewIds.lastOrNull()?.let { dragState.pillSpans[it]?.right }
            } else {
                dragState.pillSpans[beforeViewId]?.left
            }
        ) ?: return@drawWithContent
        drawLine(lineColor, Offset(lineX, 0f), Offset(lineX, size.height), ViewDropLineWidth.toPx())
    }

internal fun Modifier.viewPillDragGesture(
    dragState: DatabaseViewDragState,
    viewId: String,
    isEnabled: Boolean,
    onDrop: () -> Unit,
    onLongPressWithoutDrag: () -> Unit
): Modifier = pointerInput(viewId, isEnabled) {
    if (!isEnabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (isDesktopPlatform) {
            if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
            dragState.start(viewId, grabXInPill = down.position.x)
            dragState.moveTo(viewId, dragStart.position.x)
            followDragUntilDrop(dragState, viewId, dragStart.id, onDrop)
            return@awaitEachGesture
        }

        GlobalEditorState.pressStartedOnDatabaseViewPill = true
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
                dragState.start(viewId, grabXInPill = longPress.position.x)
                dragState.moveTo(viewId, change.position.x)
                change.consume()
                followDragUntilDrop(dragState, viewId, longPress.id, onDrop)
                return@awaitEachGesture
            }
            change.consume()
        }
    }
}

private suspend fun AwaitPointerEventScope.followDragUntilDrop(
    dragState: DatabaseViewDragState,
    viewId: String,
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
            dragState.moveTo(viewId, change.position.x)
            change.consume()
        }
    } catch (cancellation: CancellationException) {
        dragState.reset()
        throw cancellation
    }
}
