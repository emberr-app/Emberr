package com.emberr.presentation.shared.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.format_bold
import emberr.shared.generated.resources.italic
import emberr.shared.generated.resources.scissor2
import emberr.shared.generated.resources.text_highlight
import emberr.shared.generated.resources.text_x
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.underline
import kotlin.math.roundToInt

private val TextSelectionMenuWidth = 260.dp

class TextSelectionMenuActions(
    val copy: (() -> Unit)? = null,
    val cut: (() -> Unit)? = null,
    val paste: (() -> Unit)? = null,
    val delete: (() -> Unit)? = null,
    val selectAll: (() -> Unit)? = null,
    val toggleFormat: ((String) -> Unit)? = null
)

@Composable
fun TextSelectionMenu(
    positionInParent: Offset,
    actions: TextSelectionMenuActions,
    onClose: () -> Unit,
    takesKeyboardFocus: Boolean = true
) {
    var isChoosingHighlightColor by remember(positionInParent) { mutableStateOf(false) }

    fun closeThen(action: () -> Unit): () -> Unit = {
        onClose()
        action()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(positionInParent.x.roundToInt(), positionInParent.y.roundToInt()) }
            .size(1.dp)
    ) {
        EmberrDesktopMenu(
            expanded = true,
            onDismissRequest = onClose,
            properties = PopupProperties(focusable = takesKeyboardFocus),
            modifier = Modifier.width(if (isChoosingHighlightColor) HighlightColorMenuWidth else TextSelectionMenuWidth)
        ) {
            val toggleFormat = actions.toggleFormat
            if (isChoosingHighlightColor && toggleFormat != null) {
                HighlightColorCircles(
                    onColorChosen = { formatWithColor ->
                        onClose()
                        toggleFormat(formatWithColor)
                    },
                    onClose = onClose
                )
            } else {
                val textItems = listOfNotNull(
                    actions.copy?.let { SlashMenuItemData("Copy", Res.drawable.copy, action = closeThen(it)) },
                    actions.cut?.let { SlashMenuItemData("Cut", Res.drawable.scissor2, action = closeThen(it)) },
                    actions.paste?.let { SlashMenuItemData("Paste", Icons.Default.ContentPaste, action = closeThen(it)) },
                    actions.delete?.let { SlashMenuItemData("Delete", Res.drawable.trash, action = closeThen(it)) },
                    actions.selectAll?.let { SlashMenuItemData("Select All", Icons.Default.SelectAll, action = closeThen(it)) }
                )
                val formattingItems = if (toggleFormat == null) {
                    emptyList()
                } else {
                    listOf(
                        SlashMenuItemData("Bold", Res.drawable.format_bold, 13.dp, closeThen { toggleFormat("bold") }),
                        SlashMenuItemData("Italic", Res.drawable.italic, 13.dp, closeThen { toggleFormat("italic") }),
                        SlashMenuItemData("Underline", Res.drawable.underline, 15.dp, closeThen { toggleFormat("underline") }),
                        SlashMenuItemData("Strikethrough", Res.drawable.text_x, 15.dp, closeThen { toggleFormat("strike") }),
                        SlashMenuItemData("Highlight", Res.drawable.text_highlight, 19.dp) { isChoosingHighlightColor = true }
                    )
                }
                SlashMenuList(
                    sections = listOfNotNull(
                        SlashMenuSectionData(title = "Text", items = textItems).takeIf { textItems.isNotEmpty() },
                        SlashMenuSectionData(title = "Formatting", items = formattingItems).takeIf { formattingItems.isNotEmpty() }
                    )
                )
            }
        }
    }
}
