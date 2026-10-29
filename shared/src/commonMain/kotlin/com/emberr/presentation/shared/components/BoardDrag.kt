package com.emberr.presentation.shared.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import com.emberr.domain.util.system.isDesktopPlatform

private const val BoardEdgeScrollStepPixels = 14f

internal class GestureCoordinates {
    var coordinates: LayoutCoordinates? = null
}

internal fun Modifier.boardDragGesture(
    key: Any,
    isEnabled: Boolean,
    gestureCoordinates: GestureCoordinates,
    onStart: (pointerInRoot: Offset, pointerInItem: Offset, itemSize: IntSize) -> Unit,
    onMove: (pointerInRoot: Offset) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit
): Modifier = pointerInput(key, isEnabled) {
    if (!isEnabled) return@pointerInput
    val startDrag = { pointerInItem: Offset ->
        gestureCoordinates.coordinates?.let { coordinates ->
            onStart(coordinates.localToRoot(pointerInItem), pointerInItem, coordinates.size)
        }
        Unit
    }
    val followPointer = { change: PointerInputChange, _: Offset ->
        change.consume()
        gestureCoordinates.coordinates?.let { onMove(it.localToRoot(change.position)) }
        Unit
    }
    if (isDesktopPlatform) {
        detectDragGestures(onDragStart = startDrag, onDragEnd = onDrop, onDragCancel = onCancel, onDrag = followPointer)
    } else {
        detectDragGesturesAfterLongPress(onDragStart = startDrag, onDragEnd = onDrop, onDragCancel = onCancel, onDrag = followPointer)
    }
}

internal suspend fun ScrollState.scrollWhenPointerNearEdges(pointerX: Float, viewportWidthPx: Int, edgeZonePx: Float) {
    when {
        pointerX < edgeZonePx -> scrollBy(-BoardEdgeScrollStepPixels)
        pointerX > viewportWidthPx - edgeZonePx -> scrollBy(BoardEdgeScrollStepPixels)
    }
}
