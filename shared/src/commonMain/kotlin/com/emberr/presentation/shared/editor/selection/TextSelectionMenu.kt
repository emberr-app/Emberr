package com.emberr.presentation.shared.editor.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
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
import com.emberr.presentation.shared.editor.slash.SlashMenuItemData
import com.emberr.presentation.shared.editor.slash.SlashMenuList
import com.emberr.presentation.shared.editor.slash.SlashMenuSectionData
import com.emberr.presentation.shared.editor.formatting.HighlightColorCircles
import com.emberr.presentation.shared.editor.formatting.HighlightColorMenuWidth
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.format_bold
import emberr.shared.generated.resources.italic
import emberr.shared.generated.resources.scissor2
import emberr.shared.generated.resources.select_all
import emberr.shared.generated.resources.text_highlight
import emberr.shared.generated.resources.text_x
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.underline
import kotlin.math.roundToInt
import emberr.shared.generated.resources.common_delete
import emberr.shared.generated.resources.editor_block_text
import emberr.shared.generated.resources.editor_copy
import emberr.shared.generated.resources.editor_cut
import emberr.shared.generated.resources.editor_format_bold
import emberr.shared.generated.resources.editor_format_highlight
import emberr.shared.generated.resources.editor_format_italic
import emberr.shared.generated.resources.editor_format_strikethrough
import emberr.shared.generated.resources.editor_format_underline
import emberr.shared.generated.resources.editor_formatting
import emberr.shared.generated.resources.editor_paste
import emberr.shared.generated.resources.editor_select_all_title_case
import org.jetbrains.compose.resources.stringResource

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
                    actions.copy?.let { SlashMenuItemData(stringResource(Res.string.editor_copy), Res.drawable.copy, action = closeThen(it)) },
                    actions.cut?.let { SlashMenuItemData(stringResource(Res.string.editor_cut), Res.drawable.scissor2, action = closeThen(it)) },
                    actions.paste?.let { SlashMenuItemData(stringResource(Res.string.editor_paste), Icons.Default.ContentPaste, action = closeThen(it)) },
                    actions.delete?.let { SlashMenuItemData(stringResource(Res.string.common_delete), Res.drawable.trash, action = closeThen(it)) },
                    actions.selectAll?.let { SlashMenuItemData(stringResource(Res.string.editor_select_all_title_case), Res.drawable.select_all, action = closeThen(it)) }
                )
                val formattingItems = if (toggleFormat == null) {
                    emptyList()
                } else {
                    listOf(
                        SlashMenuItemData(stringResource(Res.string.editor_format_bold), Res.drawable.format_bold, 13.dp, closeThen { toggleFormat("bold") }),
                        SlashMenuItemData(stringResource(Res.string.editor_format_italic), Res.drawable.italic, 13.dp, closeThen { toggleFormat("italic") }),
                        SlashMenuItemData(stringResource(Res.string.editor_format_underline), Res.drawable.underline, 15.dp, closeThen { toggleFormat("underline") }),
                        SlashMenuItemData(stringResource(Res.string.editor_format_strikethrough), Res.drawable.text_x, 15.dp, closeThen { toggleFormat("strike") }),
                        SlashMenuItemData(stringResource(Res.string.editor_format_highlight), Res.drawable.text_highlight, 19.dp) { isChoosingHighlightColor = true }
                    )
                }
                SlashMenuList(
                    sections = listOfNotNull(
                        SlashMenuSectionData(title = stringResource(Res.string.editor_block_text), items = textItems).takeIf { textItems.isNotEmpty() },
                        SlashMenuSectionData(title = stringResource(Res.string.editor_formatting), items = formattingItems).takeIf { formattingItems.isNotEmpty() }
                    )
                )
            }
        }
    }
}
