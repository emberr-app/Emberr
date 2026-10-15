package com.emberr.presentation.shared.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextRange

interface CrossBlockSelectionCommands {
    fun copy()
    fun cut()
    fun paste()
    fun replaceWith(text: String)
    fun toggleFormat(format: String)
    fun collapseTo(position: BlockTextPosition)
}

fun Modifier.selectTextAcrossBlocksByDragging(
    selection: CrossBlockSelectionState,
    isBlockSelectionActive: () -> Boolean,
    focusedBlockId: () -> String?,
    onPress: () -> Unit,
    onRightClickInsideSelection: (pressInWindow: Offset) -> Unit,
    onSelectingStarted: () -> Unit,
    onDragEndedInsideOneBlock: (blockId: String, selectedRange: TextRange) -> Unit
): Modifier = this
    .onGloballyPositioned { selection.rememberListCoordinates(it) }
    .pointerInput(selection) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val isMousePress = down.type == PointerType.Mouse
            val pressInWindow = selection.windowPointOf(down.position)
            val pressedPosition = pressInWindow?.let { selection.positionAt(it, mustBeInsideBlock = true) }

            val isRightClickInsideSelection = isMousePress &&
                currentEvent.buttons.isSecondaryPressed &&
                pressedPosition != null &&
                selection.contains(pressedPosition)
            if (isRightClickInsideSelection && pressInWindow != null) {
                down.consume()
                onRightClickInsideSelection(pressInWindow)
                return@awaitEachGesture
            }

            selection.clear()
            onPress()
            if (!isMousePress || !currentEvent.buttons.isPrimaryPressed || isBlockSelectionActive()) return@awaitEachGesture
            val anchor = pressedPosition?.takeIf { selection.hasTextField(it.blockId) } ?: return@awaitEachGesture

            var isSelectingAcrossBlocks = false
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes
                    .firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (!change.pressed) {
                    if (isSelectingAcrossBlocks) {
                        change.consume()
                        val range = selection.range
                        if (range != null && range.start.blockId == range.end.blockId) {
                            selection.clear()
                            onDragEndedInsideOneBlock(range.start.blockId, TextRange(range.start.offset, range.end.offset))
                        }
                    }
                    return@awaitEachGesture
                }
                val pointerPosition = selection.windowPointOf(change.position)
                    ?.let { selection.positionAt(it, mustBeInsideBlock = false) }
                    ?: continue
                if (!isSelectingAcrossBlocks) {
                    val hasMovedPastSlop = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                    val textFieldIsHandlingDrag = anchor.blockId == focusedBlockId() && pointerPosition.blockId == anchor.blockId
                    if (!hasMovedPastSlop || textFieldIsHandlingDrag) continue
                    isSelectingAcrossBlocks = true
                    onSelectingStarted()
                    selection.startAt(anchor)
                }
                change.consume()
                selection.extendTo(pointerPosition)
            }
        }
    }

fun handleCrossBlockSelectionKey(
    keyEvent: KeyEvent,
    selection: CrossBlockSelectionState,
    commands: CrossBlockSelectionCommands
): Boolean {
    val range = selection.range ?: return false
    if (keyEvent.type == KeyEventType.KeyUp) return false
    val isKeyDown = keyEvent.type == KeyEventType.KeyDown

    if (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) {
        if (!isKeyDown) return false
        when (keyEvent.key) {
            Key.CtrlLeft, Key.CtrlRight, Key.MetaLeft, Key.MetaRight,
            Key.ShiftLeft, Key.ShiftRight, Key.AltLeft, Key.AltRight -> return false
            Key.C -> commands.copy()
            Key.X -> commands.cut()
            Key.V -> commands.paste()
            else -> {
                selection.clear()
                return false
            }
        }
        return true
    }

    if (isKeyDown) {
        val isEditingKey = when (keyEvent.key) {
            Key.Escape -> {
                selection.clear()
                true
            }
            Key.Backspace, Key.Delete -> {
                commands.replaceWith("")
                true
            }
            Key.DirectionLeft, Key.DirectionUp -> {
                commands.collapseTo(range.start)
                true
            }
            Key.DirectionRight, Key.DirectionDown -> {
                commands.collapseTo(range.end)
                true
            }
            else -> false
        }
        if (isEditingKey) return true
    }

    val typedCharacter = keyEvent.utf16CodePoint
        .takeIf { it in 1 until 0xFFFF }
        ?.toChar()
        ?.takeIf { !it.isISOControl() && !it.isSurrogate() }
        ?: return false
    commands.replaceWith(typedCharacter.toString())
    return true
}
