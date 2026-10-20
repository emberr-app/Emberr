package com.emberr.presentation.shared.editor.selection

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
    fun storedOffsetOnNextLine(storedOffset: Int, downward: Boolean, windowX: Float?): Int?
    fun storedOffsetOnOuterLine(onFirstLine: Boolean, windowX: Float?): Int?
    fun cursorRectInWindow(storedOffset: Int): Rect?
    fun selectionIfFocused(): TextRange?
    fun collapseSelectionIfFocused()
}

interface CrossBlockSelectionHost {
    fun onSelectingStarted()
    fun textLengthOf(blockId: String): Int?
    fun revealFocus(step: SelectionStep)
    fun replaceSelectionWithTypedText(typedText: String)
    fun placeCaret(position: BlockTextPosition)
    fun selectBlock(blockId: String)
    fun clearBlockSelection()
    fun scrollByPage(downward: Boolean, afterScroll: (didScroll: Boolean) -> Unit)
}

private data class CaretGoal(val position: BlockTextPosition, val windowX: Float)

enum class SelectionStep { CharacterBack, CharacterForward, LineUp, LineDown }

val LocalCrossBlockSelection = staticCompositionLocalOf<CrossBlockSelectionState?> { null }

@Stable
class CrossBlockSelectionState {
    private var anchor by mutableStateOf<BlockTextPosition?>(null)
    private var focus by mutableStateOf<BlockTextPosition?>(null)
    var blockIdsInOrder by mutableStateOf<List<String>>(emptyList())
    var host: CrossBlockSelectionHost? = null
    var focusFieldAboveBlocks: (() -> Unit)? = null
    private var caretGoal: CaretGoal? = null
    var longPressedWord: CrossBlockTextRange? = null
        private set
    var isBeingTouched by mutableStateOf(false)
    var magnifiedPointInWindow by mutableStateOf<Offset?>(null)
        private set

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

    val focusPosition: BlockTextPosition? get() = focus

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

    fun selectWord(blockId: String, word: TextRange) {
        val wordStart = BlockTextPosition(blockId, word.start)
        val wordEnd = BlockTextPosition(blockId, word.end)
        startAt(wordStart)
        focus = wordEnd
        longPressedWord = CrossBlockTextRange(wordStart, wordEnd)
    }

    fun extendLongPressedWordTo(position: BlockTextPosition) {
        val word = longPressedWord ?: return
        when {
            position.comesBefore(word.start) -> {
                anchor = word.end
                focus = position
            }
            word.end.comesBefore(position) -> {
                anchor = word.start
                focus = position
            }
            else -> {
                anchor = word.start
                focus = word.end
            }
        }
    }

    fun forgetLongPressedWord() {
        longPressedWord = null
    }

    fun startFromFocusedTextSelection(blockId: String, towards: BlockTextPosition): Boolean {
        val fieldSelection = selectableTextFields[blockId]?.selectionIfFocused()?.takeIf { !it.collapsed } ?: return false
        val movesForward = blockIdsInOrder.indexOf(towards.blockId) > blockIdsInOrder.indexOf(blockId)
        startAt(BlockTextPosition(blockId, if (movesForward) fieldSelection.min else fieldSelection.max))
        return true
    }

    fun takeOverFocusedTextSelection() {
        val (blockId, fieldSelection) = selectableTextFields.entries.firstNotNullOfOrNull { (blockId, field) ->
            field.selectionIfFocused()?.takeIf { !it.collapsed }?.let { blockId to it }
        } ?: return
        startAt(BlockTextPosition(blockId, fieldSelection.start))
        focus = BlockTextPosition(blockId, fieldSelection.end)
    }

    fun collapseFocusedTextSelection() {
        selectableTextFields.values.forEach { it.collapseSelectionIfFocused() }
    }

    fun magnifyFocusLineAt(pointInWindow: Offset) {
        val focus = focus ?: return
        val focusLine = selectableTextFields[focus.blockId]?.cursorRectInWindow(focus.offset)
        magnifiedPointInWindow = focusLine?.let { Offset(pointInWindow.x, it.center.y) }
    }

    fun hideMagnifier() {
        magnifiedPointInWindow = null
    }

    fun startMovingEdge(movesStart: Boolean) {
        val range = range ?: return
        longPressedWord = null
        anchor = if (movesStart) range.end else range.start
        focus = if (movesStart) range.start else range.end
    }

    fun selectWholeNote(): Boolean {
        val firstBlockId = blockIdsInOrder.firstOrNull() ?: return false
        val lastBlockId = blockIdsInOrder.last()
        startAt(BlockTextPosition(firstBlockId, 0))
        focus = BlockTextPosition(lastBlockId, host?.textLengthOf(lastBlockId) ?: 0)
        return true
    }

    fun extendByKeyboard(step: SelectionStep) {
        focus = focusMovedBy(step) ?: return
        host?.revealFocus(step)
    }

    fun selectFromTextFieldToEdgeOfNote(blockId: String, fieldSelection: TextRange, toEnd: Boolean): Boolean {
        val edge = edgeOfNote(toEnd)?.takeIf { it.blockId != blockId } ?: return false
        anchor = BlockTextPosition(blockId, fieldSelection.start)
        focus = edge
        host?.onSelectingStarted()
        host?.revealFocus(if (toEnd) SelectionStep.LineDown else SelectionStep.LineUp)
        return true
    }

    fun extendToEdgeOfNote(toEnd: Boolean) {
        focus = edgeOfNote(toEnd) ?: return
        host?.revealFocus(if (toEnd) SelectionStep.LineDown else SelectionStep.LineUp)
    }

    private fun edgeOfNote(toEnd: Boolean): BlockTextPosition? {
        val edgeBlockId = (if (toEnd) blockIdsInOrder.lastOrNull() else blockIdsInOrder.firstOrNull()) ?: return null
        return BlockTextPosition(edgeBlockId, if (toEnd) host?.textLengthOf(edgeBlockId) ?: 0 else 0)
    }

    fun moveCaret(
        from: BlockTextPosition,
        step: SelectionStep,
        selectsBlocksWithoutText: Boolean,
        moveInsideBlock: (Int) -> Unit
    ): Boolean {
        val host = host ?: return false
        val goalX = goalXFor(from)
        val field = selectableTextFields[from.blockId]
        val offsetInsideBlock = when (step) {
            SelectionStep.LineUp -> field?.storedOffsetOnNextLine(from.offset, downward = false, windowX = goalX)
            SelectionStep.LineDown -> field?.storedOffsetOnNextLine(from.offset, downward = true, windowX = goalX)
            SelectionStep.CharacterBack, SelectionStep.CharacterForward -> null
        }
        if (offsetInsideBlock != null) {
            rememberGoal(BlockTextPosition(from.blockId, offsetInsideBlock), step, goalX)
            moveInsideBlock(offsetInsideBlock)
            return true
        }
        return moveCaretOutOfBlock(from, step, goalX, host, selectsBlocksWithoutText)
    }

    fun moveCaretFromSelectedBlock(blockId: String, step: SelectionStep): Boolean {
        val host = host ?: return false
        val from = BlockTextPosition(blockId, 0)
        return moveCaretOutOfBlock(from, step, goalXFor(from), host, selectsBlocksWithoutText = true)
    }

    fun moveCaretAboveBlocks(): Boolean {
        val fieldAbove = focusFieldAboveBlocks ?: return false
        caretGoal = null
        host?.clearBlockSelection()
        fieldAbove()
        return true
    }

    fun moveCaretByPage(from: BlockTextPosition, downward: Boolean): Boolean {
        val host = host ?: return false
        val caretPoint = selectableTextFields[from.blockId]?.cursorRectInWindow(from.offset)?.center ?: return false
        host.scrollByPage(downward) { didScroll ->
            val target = if (didScroll) positionAt(caretPoint, mustBeInsideBlock = false) else null
            if (target == null) {
                moveCaretToEdgeOfNote(toEnd = downward)
            } else {
                caretGoal = null
                goTo(target, host)
            }
        }
        return true
    }

    fun moveCaretIntoFirstBlock(windowX: Float?, selectsBlocksWithoutText: Boolean): Boolean {
        val host = host ?: return false
        val firstBlockId = blockIdsInOrder.firstOrNull { selectsBlocksWithoutText || host.textLengthOf(it) != null } ?: return false
        val offset = selectableTextFields[firstBlockId]?.storedOffsetOnOuterLine(onFirstLine = true, windowX = windowX) ?: 0
        val target = BlockTextPosition(firstBlockId, offset)
        caretGoal = windowX?.let { CaretGoal(target, it) }
        goTo(target, host)
        return true
    }

    fun moveCaretToEdgeOfNote(toEnd: Boolean): Boolean {
        val host = host ?: return false
        val blockIdsWithText = blockIdsInOrder.filter { host.textLengthOf(it) != null }
        val blockId = (if (toEnd) blockIdsWithText.lastOrNull() else blockIdsWithText.firstOrNull()) ?: return false
        caretGoal = null
        host.placeCaret(BlockTextPosition(blockId, if (toEnd) host.textLengthOf(blockId) ?: 0 else 0))
        return true
    }

    private fun moveCaretOutOfBlock(
        from: BlockTextPosition,
        step: SelectionStep,
        goalX: Float?,
        host: CrossBlockSelectionHost,
        selectsBlocksWithoutText: Boolean
    ): Boolean {
        val target = positionInNeighbourBlock(from, step, goalX, skipsBlocksWithoutText = !selectsBlocksWithoutText)
        if (target == null) {
            val movesBack = step == SelectionStep.LineUp || step == SelectionStep.CharacterBack
            return movesBack && moveCaretAboveBlocks()
        }
        rememberGoal(target, step, goalX)
        goTo(target, host)
        return true
    }

    private fun goTo(target: BlockTextPosition, host: CrossBlockSelectionHost) {
        if (host.textLengthOf(target.blockId) == null) host.selectBlock(target.blockId) else host.placeCaret(target)
    }

    private fun goalXFor(from: BlockTextPosition): Float? =
        caretGoal?.takeIf { it.position == from }?.windowX
            ?: selectableTextFields[from.blockId]?.cursorRectInWindow(from.offset)?.left

    private fun rememberGoal(position: BlockTextPosition, step: SelectionStep, goalX: Float?) {
        val movesByLine = step == SelectionStep.LineUp || step == SelectionStep.LineDown
        caretGoal = if (movesByLine && goalX != null) CaretGoal(position, goalX) else null
    }

    fun extendTo(position: BlockTextPosition) {
        focus = position
    }

    fun clear() {
        anchor = null
        focus = null
        longPressedWord = null
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
        val focusRect = cursorRectInWindow(focus) ?: return null
        val visibleTop = list.positionInWindow().y + topInsetPx
        val visibleBottom = list.positionInWindow().y + list.size.height - bottomInsetPx
        return when {
            focusRect.top < visibleTop -> focusRect.top - visibleTop
            focusRect.bottom > visibleBottom -> focusRect.bottom - visibleBottom
            else -> 0f
        }
    }

    fun cursorRectInWindow(position: BlockTextPosition): Rect? =
        selectableTextFields[position.blockId]?.cursorRectInWindow(position.offset)
            ?: blockCoordinates[position.blockId]?.takeIf { it.isAttached }?.let { Rect(it.positionInWindow(), it.size.toSize()) }

    fun rememberListCoordinates(coordinates: LayoutCoordinates) {
        listCoordinates = coordinates
    }

    fun windowPointOf(pointInList: Offset): Offset? = listCoordinates?.takeIf { it.isAttached }?.localToWindow(pointInList)

    fun rememberBlockCoordinates(blockId: String, coordinates: LayoutCoordinates) {
        blockCoordinates.entries.removeAll { (otherBlockId, otherCoordinates) -> otherCoordinates === coordinates && otherBlockId != blockId }
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
        val blockIdsInNote = blockIdsInOrder.toSet()
        val closestBlock = blockCoordinates.entries
            .filter { it.value.isAttached && it.key in blockIdsInNote }
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
        val goalX = goalXFor(focus)
        val field = selectableTextFields[focus.blockId]
        val offsetInsideBlock = when (step) {
            SelectionStep.CharacterBack -> field?.storedOffsetBeside(focus.offset, forward = false)
            SelectionStep.CharacterForward -> field?.storedOffsetBeside(focus.offset, forward = true)
            SelectionStep.LineUp -> field?.storedOffsetOnNextLine(focus.offset, downward = false, windowX = goalX)
            SelectionStep.LineDown -> field?.storedOffsetOnNextLine(focus.offset, downward = true, windowX = goalX)
        }
        val movedFocus = offsetInsideBlock?.let { BlockTextPosition(focus.blockId, it) }
            ?: positionInNeighbourBlock(focus, step, goalX, skipsBlocksWithoutText = false)
            ?: return null
        rememberGoal(movedFocus, step, goalX)
        return movedFocus
    }

    private fun positionInNeighbourBlock(
        from: BlockTextPosition,
        step: SelectionStep,
        cursorX: Float?,
        skipsBlocksWithoutText: Boolean
    ): BlockTextPosition? {
        val movesForward = step == SelectionStep.CharacterForward || step == SelectionStep.LineDown
        val fromIndex = blockIdsInOrder.indexOf(from.blockId)
        if (fromIndex == -1) return null
        val blockIdsInDirection = if (movesForward) blockIdsInOrder.drop(fromIndex + 1) else blockIdsInOrder.take(fromIndex).asReversed()
        val neighbourId = blockIdsInDirection.firstOrNull { !skipsBlocksWithoutText || host?.textLengthOf(it) != null } ?: return null
        val neighbourField = selectableTextFields[neighbourId]
        val neighbourLength = host?.textLengthOf(neighbourId) ?: 0
        val offsetInNeighbour = when (step) {
            SelectionStep.CharacterForward -> 0
            SelectionStep.CharacterBack -> neighbourLength
            SelectionStep.LineDown -> neighbourField?.storedOffsetOnOuterLine(onFirstLine = true, windowX = cursorX) ?: 0
            SelectionStep.LineUp -> neighbourField?.storedOffsetOnOuterLine(onFirstLine = false, windowX = cursorX) ?: neighbourLength
        }
        return BlockTextPosition(neighbourId, offsetInNeighbour)
    }

    private fun BlockTextPosition.comesBefore(other: BlockTextPosition): Boolean {
        val index = blockIdsInOrder.indexOf(blockId)
        val otherIndex = blockIdsInOrder.indexOf(other.blockId)
        return index < otherIndex || (index == otherIndex && offset < other.offset)
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
