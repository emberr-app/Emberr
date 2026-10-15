package com.emberr.presentation.shared.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextRange

fun interface TextOffsetFinder {
    fun storedOffsetAt(pointInWindow: Offset): Int?
}

val LocalCrossBlockSelection = staticCompositionLocalOf<CrossBlockSelectionState?> { null }

@Stable
class CrossBlockSelectionState {
    private var anchor by mutableStateOf<BlockTextPosition?>(null)
    private var focus by mutableStateOf<BlockTextPosition?>(null)
    var blockIdsInOrder by mutableStateOf<List<String>>(emptyList())

    private var listCoordinates: LayoutCoordinates? = null
    private val blockCoordinates = mutableMapOf<String, LayoutCoordinates>()
    private val textOffsetFinders = mutableMapOf<String, TextOffsetFinder>()

    val range: CrossBlockTextRange?
        get() {
            val anchor = anchor ?: return null
            val focus = focus ?: return null
            return orderedTextRange(anchor, focus, blockIdsInOrder)
        }

    val isActive: Boolean get() = range != null

    fun blockIdsInSelection(): List<String> = range?.let { blockIdsInRange(it, blockIdsInOrder) }.orEmpty()

    fun startAt(position: BlockTextPosition) {
        anchor = position
        focus = position
    }

    fun extendTo(position: BlockTextPosition) {
        focus = position
    }

    fun clear() {
        anchor = null
        focus = null
    }

    fun selectedTextRangeIn(blockId: String, textLength: Int): TextRange? {
        val range = range ?: return null
        if (blockId !in blockIdsInSelection()) return null
        val from = if (blockId == range.start.blockId) range.start.offset.coerceIn(0, textLength) else 0
        val to = if (blockId == range.end.blockId) range.end.offset.coerceIn(from, textLength) else textLength
        return TextRange(from, to)
    }

    fun contains(position: BlockTextPosition): Boolean {
        val range = range ?: return false
        if (position.blockId !in blockIdsInSelection()) return false
        if (position.blockId == range.start.blockId && position.offset < range.start.offset) return false
        if (position.blockId == range.end.blockId && position.offset > range.end.offset) return false
        return true
    }

    fun isBlockInRange(blockId: String): Boolean = blockId in blockIdsInSelection()

    fun hasTextField(blockId: String): Boolean = blockId in textOffsetFinders

    fun rememberListCoordinates(coordinates: LayoutCoordinates) {
        listCoordinates = coordinates
    }

    fun windowPointOf(pointInList: Offset): Offset? = listCoordinates?.takeIf { it.isAttached }?.localToWindow(pointInList)

    fun rememberBlockCoordinates(blockId: String, coordinates: LayoutCoordinates) {
        blockCoordinates[blockId] = coordinates
    }

    fun forgetBlockCoordinates(blockId: String) {
        if (blockCoordinates[blockId]?.isAttached != true) blockCoordinates.remove(blockId)
    }

    fun registerTextField(blockId: String, finder: TextOffsetFinder) {
        textOffsetFinders[blockId] = finder
    }

    fun forgetTextField(blockId: String, finder: TextOffsetFinder) {
        if (textOffsetFinders[blockId] === finder) textOffsetFinders.remove(blockId)
    }

    fun positionAt(pointInWindow: Offset, mustBeInsideBlock: Boolean): BlockTextPosition? {
        val closestBlock = blockCoordinates.entries
            .filter { it.value.isAttached }
            .map { (blockId, coordinates) -> blockId to verticalDistance(coordinates, pointInWindow.y) }
            .minByOrNull { it.second }
            ?: return null
        val (blockId, distance) = closestBlock
        if (mustBeInsideBlock && distance > 0f) return null
        val offset = textOffsetFinders[blockId]?.storedOffsetAt(pointInWindow) ?: 0
        return BlockTextPosition(blockId, offset)
    }

    private fun verticalDistance(coordinates: LayoutCoordinates, y: Float): Float {
        val top = coordinates.positionInWindow().y
        val bottom = top + coordinates.size.height
        return when {
            y < top -> top - y
            y > bottom -> y - bottom
            else -> 0f
        }
    }
}
