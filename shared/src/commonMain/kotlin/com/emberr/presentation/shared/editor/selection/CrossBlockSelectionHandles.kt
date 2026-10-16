package com.emberr.presentation.shared.editor.selection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

enum class SelectionEdge { Start, End }

val SelectionHandleRadius = 11.dp
val SelectionHandleTouchRadius = 24.dp

private class OverlayCoordinates {
    var coordinates: LayoutCoordinates? = null
}

@Composable
fun CrossBlockSelectionHandles(
    selection: CrossBlockSelectionState,
    listState: LazyListState,
    topInsetPx: Float,
    modifier: Modifier = Modifier
) {
    val handleColor = MaterialTheme.colorScheme.primary
    val overlay = remember { OverlayCoordinates() }
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlay.coordinates = it }
            .textSelectionMagnifier {
                val point = selection.magnifiedPointInWindow
                val coordinates = overlay.coordinates?.takeIf { it.isAttached }
                if (point == null || coordinates == null) Offset.Unspecified else coordinates.windowToLocal(point)
            }
            .drawBehind {
                if (listState.layoutInfo.totalItemsCount == 0) return@drawBehind
                val coordinates = overlay.coordinates?.takeIf { it.isAttached } ?: return@drawBehind
                val radius = SelectionHandleRadius.toPx()
                clipRect(top = topInsetPx) {
                    SelectionEdge.entries.forEach { edge ->
                        val tip = selection.handleTipInWindow(edge) ?: return@forEach
                        drawSelectionHandle(coordinates.windowToLocal(tip), edge, radius, handleColor)
                    }
                }
            }
    )
}

fun CrossBlockSelectionState.cursorRectOf(edge: SelectionEdge): Rect? {
    val range = range ?: return null
    return cursorRectInWindow(if (edge == SelectionEdge.Start) range.start else range.end)
}

fun CrossBlockSelectionState.edgeHandleAt(pointInWindow: Offset, handleRadiusPx: Float, touchRadiusPx: Float): SelectionEdge? =
    SelectionEdge.entries
        .mapNotNull { edge ->
            val tip = handleTipInWindow(edge) ?: return@mapNotNull null
            edge to (handleCenter(tip, edge, handleRadiusPx) - pointInWindow).getDistance()
        }
        .filter { (_, distance) -> distance <= touchRadiusPx }
        .minByOrNull { (_, distance) -> distance }
        ?.first

private fun CrossBlockSelectionState.handleTipInWindow(edge: SelectionEdge): Offset? {
    val cursorRect = cursorRectOf(edge) ?: return null
    return if (edge == SelectionEdge.Start) cursorRect.bottomLeft else cursorRect.bottomRight
}

private fun handleCenter(tip: Offset, edge: SelectionEdge, radius: Float): Offset =
    Offset(if (edge == SelectionEdge.Start) tip.x - radius else tip.x + radius, tip.y + radius)

private fun DrawScope.drawSelectionHandle(tip: Offset, edge: SelectionEdge, radius: Float, color: Color) {
    drawCircle(color, radius, handleCenter(tip, edge, radius))
    val pointedCornerLeft = if (edge == SelectionEdge.Start) tip.x - radius else tip.x
    drawRect(color, topLeft = Offset(pointedCornerLeft, tip.y), size = Size(radius, radius))
}
