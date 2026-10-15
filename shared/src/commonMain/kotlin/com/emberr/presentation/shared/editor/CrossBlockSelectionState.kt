package com.emberr.presentation.shared.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.toSize

interface SelectableTextField {
    fun storedOffsetAt(pointInWindow: Offset): Int?
    fun storedOffsetBeside(storedOffset: Int, forward: Boolean): Int?
    fun storedOffsetOnNextLine(storedOffset: Int, downward: Boolean): Int?
    fun storedOffsetOnOuterLine(onFirstLine: Boolean, windowX: Float?): Int?
    fun cursorRectInWindow(storedOffset: Int): Rect?
    fun selectionIfFocused(): TextRange?
}

interface CrossBlockSelectionHost {
    fun onSelectingStarted()
    fun textLengthOf(blockId: String): Int?
    fun revealFocus(step: SelectionStep)
}

enum class SelectionStep { CharacterBack, CharacterForward, LineUp, LineDown }

val LocalCrossBlockSelection = staticCompositionLocalOf<CrossBlockSelectionState?> { null }

@Stable
class CrossBlockSelectionState {
    private var anchor by mutableStateOf<BlockTextPosition?>(null)
    private var focus by mutableStateOf<BlockTextPosition?>(null)
    var blockIdsInOrder by mutableStateOf<List<String>>(emptyList())
    var host: CrossBlockSelectionHost? = null

    private var listCoordinates: LayoutCoordinates? = null
    private val blockCoordinates = mutableMapOf<String, LayoutCoordinates>()
    private val selectableTextFields = mutableMapOf<String, SelectableTextField>()

    val range: CrossBlockTextRange?
        get() {
            val anchor = anchor ?: return null
            val focus = focus ?: return null
            return orderedTextRange(anchor, focus, blockIdsInOrder)
        }

    val isActive: Boolean get() = range != null

    val anchorPosition: BlockTextPosition? get() = anchor

    fun blockIdsInSelection(): List<String> = range?.let { blockIdsInRange(it, blockIdsInOrder) }.orEmpty()

    fun startAt(position: BlockTextPosition) {
        anchor = position
        focus = position
        host?.onSelectingStarted()
    }

    fun startFromTextField(blockId: String, fieldSelection: TextRange, step: SelectionStep): Boolean {
        anchor = BlockTextPosition(blockId, fieldSelection.start)
        focus = BlockTextPosition(blockId, fieldSelection.end)
        val movedFocus = focusMovedBy(step)
        if (movedFocus == null) {
            clear()
            return false
        }
        host?.onSelectingStarted()
        focus = movedFocus
        host?.revealFocus(step)
        return true
    }

    fun extendByKeyboard(step: SelectionStep) {
        focus = focusMovedBy(step) ?: return
        host?.revealFocus(step)
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

    fun continuesPastBlock(blockId: String): Boolean {
        val range = range ?: return false
        return blockId != range.end.blockId && blockId in blockIdsInSelection()
    }

    fun hasTextField(blockId: String): Boolean = blockId in selectableTextFields

    fun focusedTextSelectionAnchor(): BlockTextPosition? = selectableTextFields.entries.firstNotNullOfOrNull { (blockId, field) ->
        field.selectionIfFocused()?.let { BlockTextPosition(blockId, it.start) }
    }

    fun scrollNeededToShowFocus(topInsetPx: Float, bottomInsetPx: Float): Float? {
        val focus = focus ?: return null
        val list = listCoordinates?.takeIf { it.isAttached } ?: return null
        val focusRect = selectableTextFields[focus.blockId]?.cursorRectInWindow(focus.offset)
            ?: blockCoordinates[focus.blockId]?.takeIf { it.isAttached }?.let { Rect(it.positionInWindow(), it.size.toSize()) }
            ?: return null
        val visibleTop = list.positionInWindow().y + topInsetPx
        val visibleBottom = list.positionInWindow().y + list.size.height - bottomInsetPx
        return when {
            focusRect.top < visibleTop -> focusRect.top - visibleTop
            focusRect.bottom > visibleBottom -> focusRect.bottom - visibleBottom
            else -> 0f
        }
    }

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

    fun registerTextField(blockId: String, field: SelectableTextField) {
        selectableTextFields[blockId] = field
    }

    fun forgetTextField(blockId: String, field: SelectableTextField) {
        if (selectableTextFields[blockId] === field) selectableTextFields.remove(blockId)
    }

    fun positionAt(pointInWindow: Offset, mustBeInsideBlock: Boolean): BlockTextPosition? {
        val closestBlock = blockCoordinates.entries
            .filter { it.value.isAttached }
            .map { (blockId, coordinates) -> blockId to verticalDistance(coordinates, pointInWindow.y) }
            .minByOrNull { it.second }
            ?: return null
        val (blockId, distance) = closestBlock
        if (mustBeInsideBlock && distance > 0f) return null
        val offset = selectableTextFields[blockId]?.storedOffsetAt(pointInWindow) ?: 0
        return BlockTextPosition(blockId, offset)
    }

    private fun focusMovedBy(step: SelectionStep): BlockTextPosition? {
        val focus = focus ?: return null
        val field = selectableTextFields[focus.blockId]
        val offsetInsideBlock = when (step) {
            SelectionStep.CharacterBack -> field?.storedOffsetBeside(focus.offset, forward = false)
            SelectionStep.CharacterForward -> field?.storedOffsetBeside(focus.offset, forward = true)
            SelectionStep.LineUp -> field?.storedOffsetOnNextLine(focus.offset, downward = false)
            SelectionStep.LineDown -> field?.storedOffsetOnNextLine(focus.offset, downward = true)
        }
        if (offsetInsideBlock != null) return BlockTextPosition(focus.blockId, offsetInsideBlock)

        val movesForward = step == SelectionStep.CharacterForward || step == SelectionStep.LineDown
        val focusIndex = blockIdsInOrder.indexOf(focus.blockId)
        val neighbourId = blockIdsInOrder.getOrNull(if (movesForward) focusIndex + 1 else focusIndex - 1) ?: return null
        val neighbourField = selectableTextFields[neighbourId]
        val neighbourLength = host?.textLengthOf(neighbourId) ?: 0
        val cursorX = field?.cursorRectInWindow(focus.offset)?.left
        val offsetInNeighbour = when (step) {
            SelectionStep.CharacterForward -> 0
            SelectionStep.CharacterBack -> neighbourLength
            SelectionStep.LineDown -> neighbourField?.storedOffsetOnOuterLine(onFirstLine = true, windowX = cursorX) ?: 0
            SelectionStep.LineUp -> neighbourField?.storedOffsetOnOuterLine(onFirstLine = false, windowX = cursorX) ?: neighbourLength
        }
        return BlockTextPosition(neighbourId, offsetInNeighbour)
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
