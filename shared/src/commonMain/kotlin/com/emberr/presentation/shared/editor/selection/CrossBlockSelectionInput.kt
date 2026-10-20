package com.emberr.presentation.shared.editor.selection

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.triggerHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface CrossBlockSelectionCommands {
    fun copy()
    fun cut()
    fun paste()
    fun pasteText(text: String)
    fun replaceWith(text: String)
    fun splitAtSelection()
    fun adjustIndentation(increase: Boolean)
    fun toggleFormat(format: String)
    fun collapseTo(position: BlockTextPosition)
}

private val AutoScrollEdgeZone = 48.dp
private val AutoScrollFastestStepPerFrame = 20.dp

fun Modifier.selectTextAcrossBlocksByDragging(
    selection: CrossBlockSelectionState,
    scrollableState: ScrollableState,
    topInsetPx: Float,
    isBlockSelectionActive: () -> Boolean,
    focusedBlockId: () -> String?,
    onPress: () -> Unit,
    onRightClickInsideSelection: (pressInWindow: Offset) -> Unit,
    onDragEndedInsideOneBlock: (blockId: String, selectedRange: TextRange) -> Unit
): Modifier = this
    .onGloballyPositioned { selection.rememberListCoordinates(it) }
    .pointerInput(selection, scrollableState) {
        coroutineScope {
            val autoScroll = AutoScrollWhileSelecting(this, scrollableState, selection)
            awaitEachGesture {
                val down = awaitPressOfAnyMouseButton()
                val isMousePress = down.type == PointerType.Mouse
                val buttons = currentEvent.buttons
                val pressInWindow = selection.windowPointOf(down.position)
                val pressedPosition = pressInWindow?.let { selection.positionAt(it, mustBeInsideBlock = true) }

                val isRightClickInsideSelection = isMousePress &&
                    buttons.isSecondaryPressed &&
                    pressedPosition != null &&
                    selection.contains(pressedPosition)
                if (isRightClickInsideSelection && pressInWindow != null) {
                    down.consume()
                    onRightClickInsideSelection(pressInWindow)
                    return@awaitEachGesture
                }

                val isPrimaryMousePress = isMousePress && buttons.isPrimaryPressed && !isBlockSelectionActive()
                val shiftClickAnchor = if (isPrimaryMousePress && currentEvent.keyboardModifiers.isShiftPressed && pressedPosition != null) {
                    selection.anchorPosition
                        ?: selection.focusedTextSelectionAnchor()?.takeIf { it.blockId != pressedPosition.blockId }
                } else {
                    null
                }
                if (shiftClickAnchor == null) selection.clear()
                onPress()
                if (!isPrimaryMousePress) return@awaitEachGesture

                val anchor: BlockTextPosition
                var isSelectingAcrossBlocks: Boolean
                if (shiftClickAnchor != null && pressedPosition != null) {
                    down.consume()
                    if (!selection.isActive) selection.startAt(shiftClickAnchor)
                    selection.extendTo(pressedPosition)
                    anchor = shiftClickAnchor
                    isSelectingAcrossBlocks = true
                } else {
                    anchor = pressedPosition?.takeIf { selection.hasTextField(it.blockId) } ?: return@awaitEachGesture
                    isSelectingAcrossBlocks = false
                }

                try {
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
                            selection.startAt(anchor)
                        }
                        change.consume()
                        selection.extendTo(pointerPosition)
                        autoScroll.follow(change.position, autoScrollStepFor(change.position.y, topInsetPx))
                    }
                } finally {
                    autoScroll.stop()
                }
            }
        }
    }

fun Modifier.selectTextAcrossBlocksByTouch(
    selection: CrossBlockSelectionState,
    scrollableState: ScrollableState,
    topInsetPx: Float,
    isBlockSelectionActive: () -> Boolean
): Modifier = this
    .onGloballyPositioned { selection.rememberListCoordinates(it) }
    .pointerInput(selection, scrollableState) {
        coroutineScope {
            val gestureScope = this
            val autoScroll = AutoScrollWhileSelecting(gestureScope, scrollableState, selection)
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (isBlockSelectionActive()) return@awaitEachGesture
                val pressInWindow = selection.windowPointOf(down.position) ?: return@awaitEachGesture
                selection.isBeingTouched = true

                val grabbedEdge = selection.edgeHandleAt(pressInWindow, SelectionHandleRadius.toPx(), SelectionHandleTouchRadius.toPx())
                val grabbedEdgeRect = grabbedEdge?.let { selection.cursorRectOf(it) }
                if (grabbedEdge != null && grabbedEdgeRect != null) {
                    down.consume()
                    selection.startMovingEdge(movesStart = grabbedEdge == SelectionEdge.Start)
                    val fingerOffsetFromEdge = pressInWindow - grabbedEdgeRect.center
                    try {
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                .firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                            change.consume()
                            if (!change.pressed) return@awaitEachGesture
                            val edgeInList = change.position - fingerOffsetFromEdge
                            val edgeInWindow = selection.windowPointOf(edgeInList)
                            edgeInWindow
                                ?.let { selection.positionAt(it, mustBeInsideBlock = false) }
                                ?.let { selection.extendTo(it) }
                            edgeInWindow?.let { selection.magnifyFocusLineAt(it) }
                            autoScroll.follow(edgeInList, autoScrollStepFor(change.position.y, topInsetPx))
                        }
                    } finally {
                        autoScroll.stop()
                        selection.hideMagnifier()
                        selection.isBeingTouched = false
                    }
                }

                selection.forgetLongPressedWord()
                val wasSelecting = selection.isActive
                val pressedBlockId = selection.positionAt(pressInWindow, mustBeInsideBlock = true)?.blockId
                val longPressTime = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis
                var isLongPress = false
                var isSelectingAcrossBlocks = false
                val vibrationOnLongPress = gestureScope.launch {
                    delay(viewConfiguration.longPressTimeoutMillis)
                    if (pressedBlockId != null) triggerHapticFeedback()
                }
                try {
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes
                            .firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                        if (change.uptimeMillis >= longPressTime) isLongPress = true
                        if (!change.pressed) {
                            if (isSelectingAcrossBlocks) {
                                change.consume()
                                return@awaitEachGesture
                            }
                            if (!isLongPress && wasSelecting) selection.clear()
                            gestureScope.launch {
                                withFrameNanos { }
                                selection.takeOverFocusedTextSelection()
                            }
                            return@awaitEachGesture
                        }
                        if (!isLongPress) {
                            val isScrolling = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                            if (isScrolling) return@awaitEachGesture
                            continue
                        }
                        if (isBlockSelectionActive()) return@awaitEachGesture

                        val pointerInWindow = selection.windowPointOf(change.position) ?: continue
                        val pointerPosition = selection.positionAt(pointerInWindow, mustBeInsideBlock = false) ?: continue
                        if (selection.longPressedWord != null) {
                            change.consume()
                            selection.extendLongPressedWordTo(pointerPosition)
                            selection.magnifyFocusLineAt(pointerInWindow)
                            autoScroll.follow(change.position, autoScrollStepFor(change.position.y, topInsetPx))
                            continue
                        }
                        if (!isSelectingAcrossBlocks) {
                            if (pressedBlockId == null || pointerPosition.blockId == pressedBlockId) continue
                            isSelectingAcrossBlocks = selection.startFromFocusedTextSelection(pressedBlockId, towards = pointerPosition)
                            if (!isSelectingAcrossBlocks) continue
                        }
                        change.consume()
                        selection.extendTo(pointerPosition)
                        selection.magnifyFocusLineAt(pointerInWindow)
                        autoScroll.follow(change.position, autoScrollStepFor(change.position.y, topInsetPx))
                    }
                } finally {
                    vibrationOnLongPress.cancel()
                    autoScroll.stop()
                    selection.hideMagnifier()
                    selection.isBeingTouched = false
                }
            }
        }
    }

private suspend fun AwaitPointerEventScope.awaitPressOfAnyMouseButton(): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val press = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
        if (press != null) return press
    }
}

private fun PointerInputScope.autoScrollStepFor(pointerY: Float, topInsetPx: Float): Float {
    val edgeZone = AutoScrollEdgeZone.toPx()
    val fastestStep = AutoScrollFastestStepPerFrame.toPx()
    val topZoneEnd = topInsetPx + edgeZone
    val bottomZoneStart = size.height - edgeZone
    return when {
        pointerY < topZoneEnd -> -fastestStep * ((topZoneEnd - pointerY) / edgeZone).coerceAtMost(1f)
        pointerY > bottomZoneStart -> fastestStep * ((pointerY - bottomZoneStart) / edgeZone).coerceAtMost(1f)
        else -> 0f
    }
}

private class AutoScrollWhileSelecting(
    private val scope: CoroutineScope,
    private val scrollableState: ScrollableState,
    private val selection: CrossBlockSelectionState
) {
    private var stepPerFrame = 0f
    private var pointerInList = Offset.Zero
    private var scrollJob: Job? = null

    fun follow(pointer: Offset, step: Float) {
        pointerInList = pointer
        stepPerFrame = step
        if (step == 0f || scrollJob?.isActive == true) return
        scrollJob = scope.launch {
            while (stepPerFrame != 0f) {
                withFrameNanos { }
                scrollableState.scrollBy(stepPerFrame)
                selection.windowPointOf(pointerInList)
                    ?.let { selection.positionAt(it, mustBeInsideBlock = false) }
                    ?.let { selection.extendTo(it) }
            }
        }
    }

    fun stop() {
        stepPerFrame = 0f
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
            Key.A -> selection.selectWholeNote()
            Key.MoveHome, Key.MoveEnd -> {
                if (!keyEvent.isShiftPressed) {
                    selection.clear()
                    return false
                }
                selection.extendToEdgeOfNote(toEnd = keyEvent.key == Key.MoveEnd)
            }
            else -> {
                selection.clear()
                return false
            }
        }
        return true
    }

    if (isKeyDown && keyEvent.isShiftPressed) {
        val step = selectionStepFor(keyEvent.key)
        if (step != null) {
            selection.extendByKeyboard(step)
            return true
        }
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
            Key.Enter, Key.NumPadEnter -> {
                commands.splitAtSelection()
                true
            }
            Key.Tab -> {
                commands.adjustIndentation(increase = !keyEvent.isShiftPressed)
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

fun selectionStepFor(key: Key): SelectionStep? = when (key) {
    Key.DirectionLeft -> SelectionStep.CharacterBack
    Key.DirectionRight -> SelectionStep.CharacterForward
    Key.DirectionUp -> SelectionStep.LineUp
    Key.DirectionDown -> SelectionStep.LineDown
    else -> null
}
