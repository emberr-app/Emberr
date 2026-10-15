package com.emberr.presentation.shared.editor

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
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.format_bold
import emberr.shared.generated.resources.highlight
import emberr.shared.generated.resources.italic
import emberr.shared.generated.resources.scissor2
import emberr.shared.generated.resources.text_x
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.underline
import kotlin.math.roundToInt

private val CrossBlockSelectionMenuWidth = 260.dp

@Composable
fun CrossBlockSelectionMenu(
    positionInEditor: Offset,
    commands: CrossBlockSelectionCommands,
    onClose: () -> Unit
) {
    var isChoosingHighlightColor by remember(positionInEditor) { mutableStateOf(false) }

    fun closeThen(action: () -> Unit): () -> Unit = {
        onClose()
        action()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(positionInEditor.x.roundToInt(), positionInEditor.y.roundToInt()) }
            .size(1.dp)
    ) {
        EmberrDesktopMenu(
            expanded = true,
            onDismissRequest = onClose,
            modifier = Modifier.width(if (isChoosingHighlightColor) HighlightColorMenuWidth else CrossBlockSelectionMenuWidth)
        ) {
            if (isChoosingHighlightColor) {
                HighlightColorCircles(
                    onColorChosen = { formatWithColor ->
                        onClose()
                        commands.toggleFormat(formatWithColor)
                    },
                    onClose = onClose
                )
            } else {
                SlashMenuList(
                    sections = listOf(
                        SlashMenuSectionData(
                            title = "Selected Text",
                            items = listOf(
                                SlashMenuItemData("Copy", Res.drawable.copy, action = closeThen { commands.copy() }),
                                SlashMenuItemData("Cut", Res.drawable.scissor2, action = closeThen { commands.cut() }),
                                SlashMenuItemData("Paste", Icons.Default.ContentPaste, action = closeThen { commands.paste() }),
                                SlashMenuItemData("Delete", Res.drawable.trash, action = closeThen { commands.replaceWith("") })
                            )
                        ),
                        SlashMenuSectionData(
                            title = "Formatting",
                            items = listOf(
                                SlashMenuItemData("Bold", Res.drawable.format_bold, 13.dp, closeThen { commands.toggleFormat("bold") }),
                                SlashMenuItemData("Italic", Res.drawable.italic, 13.dp, closeThen { commands.toggleFormat("italic") }),
                                SlashMenuItemData("Underline", Res.drawable.underline, 15.dp, closeThen { commands.toggleFormat("underline") }),
                                SlashMenuItemData("Strikethrough", Res.drawable.text_x, 15.dp, closeThen { commands.toggleFormat("strike") }),
                                SlashMenuItemData("Highlight", Res.drawable.highlight, 15.dp) { isChoosingHighlightColor = true }
                            )
                        )
                    )
                )
            }
        }
    }
}
