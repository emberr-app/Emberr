package com.emberr.presentation.shared.editor.text

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.relocation.bringIntoView

internal fun Modifier.showCursorLineInsteadOfOtherLines(cursorRectWhileFocused: () -> Rect?): Modifier =
    this then CursorLineBringIntoViewElement(cursorRectWhileFocused)

private data class CursorLineBringIntoViewElement(
    val cursorRectWhileFocused: () -> Rect?
) : ModifierNodeElement<CursorLineBringIntoViewNode>() {
    override fun create() = CursorLineBringIntoViewNode(cursorRectWhileFocused)

    override fun update(node: CursorLineBringIntoViewNode) {
        node.cursorRectWhileFocused = cursorRectWhileFocused
    }
}

private class CursorLineBringIntoViewNode(
    var cursorRectWhileFocused: () -> Rect?
) : Modifier.Node(), BringIntoViewModifierNode {

    override suspend fun bringIntoView(childCoordinates: LayoutCoordinates, boundsProvider: () -> Rect?) {
        val ownCoordinates = requireLayoutCoordinates()
        bringIntoView(
            bounds = redirected@{
                if (!childCoordinates.isAttached || !ownCoordinates.isAttached) return@redirected null
                val requested = boundsProvider()?.let { rect ->
                    Rect(ownCoordinates.localPositionOf(childCoordinates, rect.topLeft), rect.size)
                } ?: return@redirected null
                val cursor = cursorRectWhileFocused() ?: return@redirected requested
                val isLineSized = requested.height <= cursor.height * 2
                val isOnCursorLine = requested.top < cursor.bottom && requested.bottom > cursor.top
                if (isLineSized && !isOnCursorLine) cursor else requested
            }
        )
    }
}
