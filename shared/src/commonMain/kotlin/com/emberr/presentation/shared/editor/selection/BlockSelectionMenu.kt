package com.emberr.presentation.shared.editor.selection

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.TextAlignment
import com.emberr.domain.model.textAlignmentOrNull
import com.emberr.presentation.shared.editor.slash.SlashMenuIcon
import com.emberr.presentation.shared.editor.slash.SlashMenuItemData
import com.emberr.presentation.shared.editor.slash.SlashMenuList
import com.emberr.presentation.shared.editor.slash.SlashMenuSectionData
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_down
import emberr.shared.generated.resources.arrow_right2
import emberr.shared.generated.resources.arrow_up
import emberr.shared.generated.resources.card_edit2
import emberr.shared.generated.resources.check_square
import emberr.shared.generated.resources.code
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.eye_closed
import emberr.shared.generated.resources.eye_open
import emberr.shared.generated.resources.format_bold
import emberr.shared.generated.resources.indent_left
import emberr.shared.generated.resources.indent_right
import emberr.shared.generated.resources.italic
import emberr.shared.generated.resources.mouse_square2
import emberr.shared.generated.resources.ordered_list
import emberr.shared.generated.resources.quote_down2
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.scissor2
import emberr.shared.generated.resources.select_all
import emberr.shared.generated.resources.text_highlight
import emberr.shared.generated.resources.text_x
import emberr.shared.generated.resources.textalign_center2
import emberr.shared.generated.resources.textalign_justifycenter2
import emberr.shared.generated.resources.textalign_left2
import emberr.shared.generated.resources.textalign_right2
import emberr.shared.generated.resources.thumbtack
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.underline
import emberr.shared.generated.resources.unordered_list
import emberr.shared.generated.resources.x
import emberr.shared.generated.resources.editor_blocks_selected
import emberr.shared.generated.resources.editor_hide_cover_image
import emberr.shared.generated.resources.editor_hide_note_icon
import emberr.shared.generated.resources.editor_pin
import emberr.shared.generated.resources.editor_show_cover_image
import emberr.shared.generated.resources.editor_show_note_icon
import emberr.shared.generated.resources.editor_unpin
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import emberr.shared.generated.resources.common_delete
import emberr.shared.generated.resources.editor_add_block_above
import emberr.shared.generated.resources.editor_add_block_below
import emberr.shared.generated.resources.editor_align_center
import emberr.shared.generated.resources.editor_align_justify
import emberr.shared.generated.resources.editor_align_left
import emberr.shared.generated.resources.editor_align_right
import emberr.shared.generated.resources.editor_block_bulleted_list
import emberr.shared.generated.resources.editor_block_callout
import emberr.shared.generated.resources.editor_block_code_block
import emberr.shared.generated.resources.editor_block_heading_1
import emberr.shared.generated.resources.editor_block_heading_2
import emberr.shared.generated.resources.editor_block_numbered_list
import emberr.shared.generated.resources.editor_block_quote
import emberr.shared.generated.resources.editor_block_text
import emberr.shared.generated.resources.editor_block_todo_list
import emberr.shared.generated.resources.editor_block_toggle_list
import emberr.shared.generated.resources.editor_clear_selection
import emberr.shared.generated.resources.editor_copy
import emberr.shared.generated.resources.editor_cut
import emberr.shared.generated.resources.editor_format_bold
import emberr.shared.generated.resources.editor_format_highlight
import emberr.shared.generated.resources.editor_format_italic
import emberr.shared.generated.resources.editor_format_strikethrough
import emberr.shared.generated.resources.editor_format_underline
import emberr.shared.generated.resources.editor_formatting
import emberr.shared.generated.resources.editor_indent_decrease
import emberr.shared.generated.resources.editor_indent_increase
import emberr.shared.generated.resources.editor_preview
import emberr.shared.generated.resources.editor_select_all_blocks
import emberr.shared.generated.resources.editor_select_more_blocks
import emberr.shared.generated.resources.editor_slash_section_alignment
import emberr.shared.generated.resources.editor_slash_section_indentation
import emberr.shared.generated.resources.editor_turn_into

@Composable
fun BlockSelectionMenuContent(
    selectedCount: Int,
    onCloseMenu: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    selectedBlocks: List<NoteBlock> = emptyList(),
    isSelectionPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null,
    onAddBlockAbove: (() -> Unit)? = null,
    onAddBlockBelow: (() -> Unit)? = null,
    onChangeBlockType: ((String) -> Unit)? = null,
    onToggleFormat: ((String) -> Unit)? = null,
    onSetAlignment: ((TextAlignment) -> Unit)? = null,
    onAdjustIndentation: ((Boolean) -> Unit)? = null,
    onUpdateLinkedNoteOptions: ((id: String, showIcon: Boolean, showCoverImage: Boolean) -> Unit)? = null
) {
    fun closeThen(action: () -> Unit): () -> Unit = {
        onCloseMenu()
        action()
    }

    val selectedLinkedNote = selectedBlocks.singleOrNull() as? LinkedNoteBlock
    val hasTextBlockSelected = selectedBlocks.any { it.textAlignmentOrNull() != null }

    val addBlockItems = buildList {
        if (onAddBlockAbove != null) {
            add(SlashMenuItemData(stringResource(Res.string.editor_add_block_above), Res.drawable.arrow_up, action = closeThen(onAddBlockAbove)))
        }
        if (onAddBlockBelow != null) {
            add(SlashMenuItemData(stringResource(Res.string.editor_add_block_below), Res.drawable.arrow_down, action = closeThen(onAddBlockBelow)))
        }
    }
    val selectionItems = listOf(
        SlashMenuItemData(stringResource(Res.string.editor_select_more_blocks), Res.drawable.mouse_square2, action = onCloseMenu),
        SlashMenuItemData(stringResource(Res.string.editor_select_all_blocks), Res.drawable.select_all, action = closeThen(onSelectAll)),
        SlashMenuItemData(stringResource(Res.string.editor_clear_selection), Res.drawable.x, 14.dp, action = closeThen(onClearSelection))
    )

    val sections = buildList {
        add(
            SlashMenuSectionData(
                title = pluralStringResource(Res.plurals.editor_blocks_selected, selectedCount, selectedCount),
                items = buildList {
                    add(SlashMenuItemData(stringResource(Res.string.editor_copy), Res.drawable.copy, action = closeThen(onCopy)))
                    add(SlashMenuItemData(stringResource(Res.string.editor_cut), Res.drawable.scissor2, action = closeThen(onCut)))
                    if (onTogglePin != null) {
                        add(
                            SlashMenuItemData(
                                label = if (isSelectionPinned) stringResource(Res.string.editor_unpin) else stringResource(Res.string.editor_pin),
                                icon = Res.drawable.thumbtack,
                                action = closeThen(onTogglePin)
                            )
                        )
                    }
                    add(SlashMenuItemData(stringResource(Res.string.common_delete), Res.drawable.trash, action = closeThen(onDelete)))
                    if (addBlockItems.isEmpty()) {
                        addAll(selectionItems)
                    }
                }
            )
        )

        if (addBlockItems.isNotEmpty()) {
            add(SlashMenuSectionData(title = "", items = addBlockItems))
            add(SlashMenuSectionData(title = "", items = selectionItems))
        }

        if (selectedLinkedNote != null && onUpdateLinkedNoteOptions != null) {
            add(
                SlashMenuSectionData(
                    title = stringResource(Res.string.editor_preview),
                    items = listOf(
                        SlashMenuItemData(
                            label = if (selectedLinkedNote.showIcon) stringResource(Res.string.editor_hide_note_icon) else stringResource(Res.string.editor_show_note_icon),
                            icon = if (selectedLinkedNote.showIcon) Res.drawable.eye_closed else Res.drawable.eye_open,
                            action = closeThen {
                                onUpdateLinkedNoteOptions(
                                    selectedLinkedNote.id,
                                    !selectedLinkedNote.showIcon,
                                    selectedLinkedNote.showCoverImage
                                )
                            }
                        ),
                        SlashMenuItemData(
                            label = if (selectedLinkedNote.showCoverImage) stringResource(Res.string.editor_hide_cover_image) else stringResource(Res.string.editor_show_cover_image),
                            icon = if (selectedLinkedNote.showCoverImage) Res.drawable.eye_closed else Res.drawable.eye_open,
                            action = closeThen {
                                onUpdateLinkedNoteOptions(
                                    selectedLinkedNote.id,
                                    selectedLinkedNote.showIcon,
                                    !selectedLinkedNote.showCoverImage
                                )
                            }
                        )
                    )
                )
            )
        }

        val optionsThatOpenAnotherMenu = buildList {
            if (onChangeBlockType != null && hasTextBlockSelected) {
                add(
                    optionThatOpensMenu(
                        label = stringResource(Res.string.editor_turn_into),
                        icon = SlashMenuIcon.Drawable(Res.drawable.refresh_cw),
                        items = listOf(
                            SlashMenuItemData(stringResource(Res.string.editor_block_text), Icons.AutoMirrored.Filled.Subject, action = closeThen { onChangeBlockType("text") }),
                            SlashMenuItemData(stringResource(Res.string.editor_block_heading_1), SlashMenuIcon.Label("H1"), closeThen { onChangeBlockType("h1") }),
                            SlashMenuItemData(stringResource(Res.string.editor_block_heading_2), SlashMenuIcon.Label("H2"), closeThen { onChangeBlockType("h2") }),
                            SlashMenuItemData(stringResource(Res.string.editor_block_todo_list), Res.drawable.check_square, action = closeThen { onChangeBlockType("checkbox") }),
                            SlashMenuItemData(stringResource(Res.string.editor_block_bulleted_list), Res.drawable.unordered_list, 15.dp, closeThen { onChangeBlockType("bullet") }),
                            SlashMenuItemData(stringResource(Res.string.editor_block_numbered_list), Res.drawable.ordered_list, 14.dp, closeThen { onChangeBlockType("number") }),
                            SlashMenuItemData(stringResource(Res.string.editor_block_toggle_list), Res.drawable.arrow_right2, action = closeThen { onChangeBlockType("toggle") }),
                            SlashMenuItemData(stringResource(Res.string.editor_block_quote), Res.drawable.quote_down2, 14.dp, closeThen { onChangeBlockType("quote") }),
                            SlashMenuItemData(stringResource(Res.string.editor_block_callout), Res.drawable.card_edit2, action = closeThen { onChangeBlockType("callout") }),
                            SlashMenuItemData(stringResource(Res.string.editor_block_code_block), Res.drawable.code, action = closeThen { onChangeBlockType("code") })
                        )
                    )
                )
            }

            if (onToggleFormat != null && hasTextBlockSelected) {
                add(
                    optionThatOpensMenu(
                        label = stringResource(Res.string.editor_formatting),
                        icon = SlashMenuIcon.Drawable(Res.drawable.format_bold, 13.dp),
                        items = listOf(
                            SlashMenuItemData(stringResource(Res.string.editor_format_bold), Res.drawable.format_bold, 13.dp, closeThen { onToggleFormat("bold") }),
                            SlashMenuItemData(stringResource(Res.string.editor_format_italic), Res.drawable.italic, 13.dp, closeThen { onToggleFormat("italic") }),
                            SlashMenuItemData(stringResource(Res.string.editor_format_underline), Res.drawable.underline, 15.dp, closeThen { onToggleFormat("underline") }),
                            SlashMenuItemData(stringResource(Res.string.editor_format_strikethrough), Res.drawable.text_x, 15.dp, closeThen { onToggleFormat("strike") }),
                            SlashMenuItemData(stringResource(Res.string.editor_format_highlight), Res.drawable.text_highlight, 19.dp, closeThen { onToggleFormat("highlight") })
                        )
                    )
                )
            }

            if (onSetAlignment != null && hasTextBlockSelected) {
                add(
                    optionThatOpensMenu(
                        label = stringResource(Res.string.editor_slash_section_alignment),
                        icon = SlashMenuIcon.Drawable(Res.drawable.textalign_left2),
                        items = listOf(
                            SlashMenuItemData(stringResource(Res.string.editor_align_left), Res.drawable.textalign_left2, action = closeThen { onSetAlignment(TextAlignment.LEFT) }),
                            SlashMenuItemData(stringResource(Res.string.editor_align_right), Res.drawable.textalign_right2, action = closeThen { onSetAlignment(TextAlignment.RIGHT) }),
                            SlashMenuItemData(stringResource(Res.string.editor_align_center), Res.drawable.textalign_center2, action = closeThen { onSetAlignment(TextAlignment.CENTER) }),
                            SlashMenuItemData(stringResource(Res.string.editor_align_justify), Res.drawable.textalign_justifycenter2, action = closeThen { onSetAlignment(TextAlignment.JUSTIFY) })
                        )
                    )
                )
            }

            if (onAdjustIndentation != null) {
                add(
                    optionThatOpensMenu(
                        label = stringResource(Res.string.editor_slash_section_indentation),
                        icon = SlashMenuIcon.Drawable(Res.drawable.indent_left),
                        items = listOf(
                            SlashMenuItemData(stringResource(Res.string.editor_indent_decrease), Res.drawable.indent_right, action = closeThen { onAdjustIndentation(false) }),
                            SlashMenuItemData(stringResource(Res.string.editor_indent_increase), Res.drawable.indent_left, action = closeThen { onAdjustIndentation(true) })
                        )
                    )
                )
            }
        }

        if (optionsThatOpenAnotherMenu.isNotEmpty()) {
            add(SlashMenuSectionData(title = "", items = optionsThatOpenAnotherMenu))
        }
    }

    SlashMenuList(sections = sections, maxHeight = BlockSelectionMenuMaxHeight)
}

private val BlockSelectionMenuMaxHeight = 520.dp

private fun optionThatOpensMenu(label: String, icon: SlashMenuIcon, items: List<SlashMenuItemData>) =
    SlashMenuItemData(
        label = label,
        icon = icon,
        action = {},
        submenuSections = listOf(SlashMenuSectionData(title = label, items = items))
    )
