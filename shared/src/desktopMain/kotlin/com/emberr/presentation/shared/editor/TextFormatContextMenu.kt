package com.emberr.presentation.shared.editor

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import com.emberr.presentation.shared.components.EmberrDesktopMenu

private val formatsByMenuLabel = listOf(
    "Bold" to "bold",
    "Italic" to "italic",
    "Strikethrough" to "strike",
    "Underline" to "underline"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun TextFormatContextMenu(
    onToggleFormat: (String) -> Unit,
    markdownClipboardActions: MarkdownClipboardActions?,
    content: @Composable () -> Unit
) {
    var isChoosingHighlightColor by remember { mutableStateOf(false) }
    var selectionBeforeChoosing by remember { mutableStateOf(TextRange.Zero) }
    var cellKeyBeforeChoosing by remember { mutableStateOf<String?>(null) }

    Box {
        val defaultTextContextMenu = LocalTextContextMenu.current
        val textContextMenu = remember(defaultTextContextMenu, markdownClipboardActions) {
            if (markdownClipboardActions == null) defaultTextContextMenu
            else MarkdownCopyTextContextMenu(defaultTextContextMenu, markdownClipboardActions)
        }
        CompositionLocalProvider(LocalTextContextMenu provides textContextMenu) {
            ContextMenuDataProvider(
                items = {
                    val selectionWhenMenuOpened = GlobalEditorState.currentSelection
                    val cellKeyWhenMenuOpened = GlobalEditorState.currentlyFocusedTableCellKey

                    formatsByMenuLabel.map { (label, format) ->
                        ContextMenuItem(label) {
                            GlobalEditorState.currentSelection = selectionWhenMenuOpened
                            GlobalEditorState.currentlyFocusedTableCellKey = cellKeyWhenMenuOpened
                            onToggleFormat(format)
                        }
                    } + ContextMenuItem("Highlight") {
                        selectionBeforeChoosing = selectionWhenMenuOpened
                        cellKeyBeforeChoosing = cellKeyWhenMenuOpened
                        isChoosingHighlightColor = true
                    }
                },
                content = content
            )
        }

        EmberrDesktopMenu(
            expanded = isChoosingHighlightColor,
            onDismissRequest = { isChoosingHighlightColor = false },
            modifier = Modifier.width(HighlightColorMenuWidth)
        ) {
            HighlightColorCircles(
                onColorChosen = { formatWithColor ->
                    GlobalEditorState.currentSelection = selectionBeforeChoosing
                    GlobalEditorState.currentlyFocusedTableCellKey = cellKeyBeforeChoosing
                    onToggleFormat(formatWithColor)
                },
                onClose = { isChoosingHighlightColor = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class MarkdownCopyTextContextMenu(
    private val defaultTextContextMenu: TextContextMenu,
    private val markdownClipboardActions: MarkdownClipboardActions
) : TextContextMenu {

    @Composable
    override fun Area(textManager: TextContextMenu.TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
        val markdownCopyTextManager = remember(textManager) {
            object : TextContextMenu.TextManager by textManager {
                override val copy: TextContextMenu.Action?
                    get() = textManager.copy?.let { defaultCopy ->
                        TextContextMenu.Action(defaultCopy.enabled) {
                            if (markdownClipboardActions.copySelection?.invoke() != true) defaultCopy.execute()
                        }
                    }

                override val cut: TextContextMenu.Action?
                    get() = textManager.cut?.let { defaultCut ->
                        TextContextMenu.Action(defaultCut.enabled) {
                            if (markdownClipboardActions.cutSelection?.invoke() != true) defaultCut.execute()
                        }
                    }
            }
        }
        defaultTextContextMenu.Area(markdownCopyTextManager, state, content)
    }
}
