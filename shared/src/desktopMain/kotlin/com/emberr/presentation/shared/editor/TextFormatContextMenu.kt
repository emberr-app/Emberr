package com.emberr.presentation.shared.editor

import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun TextFormatContextMenu(
    onToggleFormat: (String) -> Unit,
    markdownClipboardActions: MarkdownClipboardActions?,
    content: @Composable () -> Unit
) {
    val latestOnToggleFormat by rememberUpdatedState(onToggleFormat)
    val textContextMenu = remember(markdownClipboardActions) {
        EmberrTextContextMenu(markdownClipboardActions) { format -> latestOnToggleFormat(format) }
    }
    CompositionLocalProvider(LocalTextContextMenu provides textContextMenu, content = content)
}

@OptIn(ExperimentalFoundationApi::class)
private class EmberrTextContextMenu(
    private val markdownClipboardActions: MarkdownClipboardActions?,
    private val onToggleFormat: (String) -> Unit
) : TextContextMenu {

    @Composable
    override fun Area(textManager: TextContextMenu.TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
        var selectionWhenOpened by remember { mutableStateOf(TextRange.Zero) }
        var tableCellKeyWhenOpened by remember { mutableStateOf<String?>(null) }

        Box(
            modifier = Modifier.pointerInput(textManager, state) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) continue
                        val press = event.changes.firstOrNull() ?: continue
                        press.consume()
                        selectionWhenOpened = GlobalEditorState.currentSelection
                        tableCellKeyWhenOpened = GlobalEditorState.currentlyFocusedTableCellKey
                        state.status = ContextMenuState.Status.Open(Rect(press.position, 0f))
                    }
                }
            }
        ) {
            content()
            val status = state.status
            if (status is ContextMenuState.Status.Open) {
                TextSelectionMenu(
                    positionInParent = status.rect.topLeft,
                    actions = menuActionsFor(textManager, selectionWhenOpened, tableCellKeyWhenOpened),
                    onClose = { state.status = ContextMenuState.Status.Closed },
                    takesKeyboardFocus = false
                )
            }
        }
    }

    private fun menuActionsFor(
        textManager: TextContextMenu.TextManager,
        selectionWhenOpened: TextRange,
        tableCellKeyWhenOpened: String?
    ): TextSelectionMenuActions {
        val clipboardActions = markdownClipboardActions
        val hasSelectedText = textManager.selectedText.isNotEmpty()
        return TextSelectionMenuActions(
            copy = textManager.copy?.takeIf { it.enabled }?.let { defaultCopy ->
                { if (clipboardActions?.copySelection?.invoke() != true) defaultCopy.execute() }
            },
            cut = textManager.cut?.takeIf { it.enabled }?.let { defaultCut ->
                { if (clipboardActions?.cutSelection?.invoke() != true) defaultCut.execute() }
            },
            paste = textManager.paste?.takeIf { it.enabled }?.execute,
            delete = clipboardActions?.deleteSelection?.takeIf { hasSelectedText }?.let { deleteSelection ->
                { deleteSelection() }
            },
            selectAll = textManager.selectAll?.takeIf { it.enabled }?.execute,
            toggleFormat = { format ->
                GlobalEditorState.currentSelection = selectionWhenOpened
                GlobalEditorState.currentlyFocusedTableCellKey = tableCellKeyWhenOpened
                onToggleFormat(format)
            }
        )
    }
}
