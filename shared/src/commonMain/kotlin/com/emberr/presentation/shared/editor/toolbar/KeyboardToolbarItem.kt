package com.emberr.presentation.shared.editor.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.data.local.prefs.SyncConstants
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_right2
import emberr.shared.generated.resources.at
import emberr.shared.generated.resources.bookmark
import emberr.shared.generated.resources.calendar_date
import emberr.shared.generated.resources.card_edit2
import emberr.shared.generated.resources.check_square
import emberr.shared.generated.resources.clock_circle
import emberr.shared.generated.resources.code
import emberr.shared.generated.resources.database
import emberr.shared.generated.resources.document_text_12
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.format_bold
import emberr.shared.generated.resources.group
import emberr.shared.generated.resources.image
import emberr.shared.generated.resources.indent_left
import emberr.shared.generated.resources.indent_right
import emberr.shared.generated.resources.italic
import emberr.shared.generated.resources.link
import emberr.shared.generated.resources.microphone
import emberr.shared.generated.resources.minus
import emberr.shared.generated.resources.mouse_square2
import emberr.shared.generated.resources.ordered_list
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.quote_down2
import emberr.shared.generated.resources.redo_circle
import emberr.shared.generated.resources.table
import emberr.shared.generated.resources.text_highlight
import emberr.shared.generated.resources.text_x
import emberr.shared.generated.resources.textalign_center2
import emberr.shared.generated.resources.textalign_justifycenter2
import emberr.shared.generated.resources.textalign_left2
import emberr.shared.generated.resources.textalign_right2
import emberr.shared.generated.resources.underline
import emberr.shared.generated.resources.undo_circle
import emberr.shared.generated.resources.unordered_list
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.editor_align_center
import emberr.shared.generated.resources.editor_align_justify
import emberr.shared.generated.resources.editor_align_left
import emberr.shared.generated.resources.editor_align_right
import emberr.shared.generated.resources.editor_block_bulleted_list
import emberr.shared.generated.resources.editor_block_callout
import emberr.shared.generated.resources.editor_block_canvas
import emberr.shared.generated.resources.editor_block_checkbox
import emberr.shared.generated.resources.editor_block_code
import emberr.shared.generated.resources.editor_block_database
import emberr.shared.generated.resources.editor_block_document_file
import emberr.shared.generated.resources.editor_block_heading_1
import emberr.shared.generated.resources.editor_block_heading_2
import emberr.shared.generated.resources.editor_block_image
import emberr.shared.generated.resources.editor_block_link_to_canvas
import emberr.shared.generated.resources.editor_block_link_to_database
import emberr.shared.generated.resources.editor_block_link_to_note
import emberr.shared.generated.resources.editor_block_numbered_list
import emberr.shared.generated.resources.editor_block_quote
import emberr.shared.generated.resources.editor_block_simple_table
import emberr.shared.generated.resources.editor_block_text
import emberr.shared.generated.resources.editor_block_toggle_list
import emberr.shared.generated.resources.editor_block_voice_note
import emberr.shared.generated.resources.editor_block_web_bookmark
import emberr.shared.generated.resources.editor_current_date
import emberr.shared.generated.resources.editor_current_time
import emberr.shared.generated.resources.editor_format_bold
import emberr.shared.generated.resources.editor_format_highlight
import emberr.shared.generated.resources.editor_format_italic
import emberr.shared.generated.resources.editor_format_strikethrough
import emberr.shared.generated.resources.editor_format_underline
import emberr.shared.generated.resources.editor_indent_decrease
import emberr.shared.generated.resources.editor_indent_increase
import emberr.shared.generated.resources.editor_toolbar_mention_a_note
import emberr.shared.generated.resources.editor_toolbar_more_blocks
import emberr.shared.generated.resources.editor_toolbar_redo
import emberr.shared.generated.resources.editor_toolbar_select_block
import emberr.shared.generated.resources.editor_toolbar_separator
import emberr.shared.generated.resources.editor_toolbar_solid_line_divider
import emberr.shared.generated.resources.editor_toolbar_three_dots_divider
import emberr.shared.generated.resources.editor_toolbar_undo
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource

enum class KeyboardToolbarItem(val titleResource: StringResource) {
    UNDO(Res.string.editor_toolbar_undo),
    REDO(Res.string.editor_toolbar_redo),
    LINK_TO_NOTE(Res.string.editor_toolbar_mention_a_note),
    SELECT_BLOCK(Res.string.editor_toolbar_select_block),
    CURRENT_TIME(Res.string.editor_current_time),
    CURRENT_DATE(Res.string.editor_current_date),
    TEXT(Res.string.editor_block_text),
    HEADING_1(Res.string.editor_block_heading_1),
    HEADING_2(Res.string.editor_block_heading_2),
    CHECKBOX(Res.string.editor_block_checkbox),
    BULLETED_LIST(Res.string.editor_block_bulleted_list),
    NUMBERED_LIST(Res.string.editor_block_numbered_list),
    TOGGLE_LIST(Res.string.editor_block_toggle_list),
    QUOTE(Res.string.editor_block_quote),
    CALLOUT(Res.string.editor_block_callout),
    CODE(Res.string.editor_block_code),
    VOICE_NOTE(Res.string.editor_block_voice_note),
    IMAGE(Res.string.editor_block_image),
    DOCUMENT(Res.string.editor_block_document_file),
    WEB_BOOKMARK(Res.string.editor_block_web_bookmark),
    SIMPLE_TABLE(Res.string.editor_block_simple_table),
    DATABASE(Res.string.editor_block_database),
    CANVAS(Res.string.editor_block_canvas),
    LINKED_NOTE(Res.string.editor_block_link_to_note),
    LINKED_CANVAS(Res.string.editor_block_link_to_canvas),
    LINKED_DATABASE(Res.string.editor_block_link_to_database),
    SOLID_LINE_DIVIDER(Res.string.editor_toolbar_solid_line_divider),
    THREE_DOTS_DIVIDER(Res.string.editor_toolbar_three_dots_divider),
    BOLD(Res.string.editor_format_bold),
    ITALIC(Res.string.editor_format_italic),
    STRIKETHROUGH(Res.string.editor_format_strikethrough),
    UNDERLINE(Res.string.editor_format_underline),
    HIGHLIGHT(Res.string.editor_format_highlight),
    ALIGN_LEFT(Res.string.editor_align_left),
    ALIGN_RIGHT(Res.string.editor_align_right),
    ALIGN_CENTER(Res.string.editor_align_center),
    JUSTIFY(Res.string.editor_align_justify),
    DECREASE_INDENT(Res.string.editor_indent_decrease),
    INCREASE_INDENT(Res.string.editor_indent_increase),
    MORE_BLOCKS(Res.string.editor_toolbar_more_blocks),
    DIVIDER(Res.string.editor_toolbar_separator)
}

@Composable
fun KeyboardToolbarItem.label(): String = stringResource(titleResource)

val DefaultKeyboardToolbarItems: List<KeyboardToolbarItem> = listOf(
    KeyboardToolbarItem.UNDO,
    KeyboardToolbarItem.REDO,
    KeyboardToolbarItem.DIVIDER,
    KeyboardToolbarItem.LINK_TO_NOTE,
    KeyboardToolbarItem.SELECT_BLOCK,
    KeyboardToolbarItem.CURRENT_TIME,
    KeyboardToolbarItem.CURRENT_DATE,
    KeyboardToolbarItem.DIVIDER,
    KeyboardToolbarItem.TEXT,
    KeyboardToolbarItem.HEADING_1,
    KeyboardToolbarItem.HEADING_2,
    KeyboardToolbarItem.CHECKBOX,
    KeyboardToolbarItem.BULLETED_LIST,
    KeyboardToolbarItem.NUMBERED_LIST,
    KeyboardToolbarItem.TOGGLE_LIST,
    KeyboardToolbarItem.QUOTE,
    KeyboardToolbarItem.CALLOUT,
    KeyboardToolbarItem.CODE,
    KeyboardToolbarItem.DIVIDER,
    KeyboardToolbarItem.BOLD,
    KeyboardToolbarItem.ITALIC,
    KeyboardToolbarItem.STRIKETHROUGH,
    KeyboardToolbarItem.UNDERLINE,
    KeyboardToolbarItem.HIGHLIGHT,
    KeyboardToolbarItem.DIVIDER,
    KeyboardToolbarItem.ALIGN_LEFT,
    KeyboardToolbarItem.ALIGN_RIGHT,
    KeyboardToolbarItem.ALIGN_CENTER,
    KeyboardToolbarItem.JUSTIFY,
    KeyboardToolbarItem.DIVIDER,
    KeyboardToolbarItem.INCREASE_INDENT,
    KeyboardToolbarItem.DECREASE_INDENT,
    KeyboardToolbarItem.DIVIDER,
    KeyboardToolbarItem.MORE_BLOCKS
)

fun keyboardToolbarItemsFrom(savedValue: String): List<KeyboardToolbarItem> {
    if (savedValue == SyncConstants.DEFAULT_KEYBOARD_TOOLBAR_ITEMS) return DefaultKeyboardToolbarItems
    val knownItems = savedValue.split(",").mapNotNull { name ->
        KeyboardToolbarItem.entries.firstOrNull { it.name == name }
    }
    return knownItems.filterIndexed { index, item ->
        item == KeyboardToolbarItem.DIVIDER || knownItems.indexOf(item) == index
    }
}

fun keyboardToolbarSavedValueOf(items: List<KeyboardToolbarItem>): String =
    items.joinToString(",") { it.name }

fun keyboardToolbarItemsToShow(items: List<KeyboardToolbarItem>, showHistory: Boolean): List<KeyboardToolbarItem> {
    val usableItems = if (showHistory) {
        items
    } else {
        items.filterNot { it == KeyboardToolbarItem.UNDO || it == KeyboardToolbarItem.REDO }
    }
    val itemsWithoutExtraDividers = buildList {
        usableItems.forEach { item ->
            val isLeadingOrRepeatedDivider = item == KeyboardToolbarItem.DIVIDER &&
                (isEmpty() || last() == KeyboardToolbarItem.DIVIDER)
            if (!isLeadingOrRepeatedDivider) add(item)
        }
    }
    return itemsWithoutExtraDividers.dropLastWhile { it == KeyboardToolbarItem.DIVIDER }
}

@Composable
internal fun KeyboardToolbarItemIcon(item: KeyboardToolbarItem, tint: Color) {
    when (item) {
        KeyboardToolbarItem.UNDO -> ToolbarItemDrawable(Res.drawable.undo_circle, item, tint, 18.dp)
        KeyboardToolbarItem.REDO -> ToolbarItemDrawable(Res.drawable.redo_circle, item, tint, 18.dp)
        KeyboardToolbarItem.LINK_TO_NOTE -> ToolbarItemDrawable(Res.drawable.at, item, tint, 18.dp)
        KeyboardToolbarItem.SELECT_BLOCK -> ToolbarItemDrawable(Res.drawable.mouse_square2, item, tint, 16.dp)
        KeyboardToolbarItem.CURRENT_TIME -> ToolbarItemDrawable(Res.drawable.clock_circle, item, tint, 17.dp)
        KeyboardToolbarItem.CURRENT_DATE -> ToolbarItemDrawable(Res.drawable.calendar_date, item, tint, 17.dp)
        KeyboardToolbarItem.TEXT -> Icon(Icons.AutoMirrored.Filled.Subject, item.label(), tint = tint, modifier = Modifier.size(19.dp))
        KeyboardToolbarItem.HEADING_1 -> Text("H1", style = MaterialTheme.typography.labelSmall, color = tint)
        KeyboardToolbarItem.HEADING_2 -> Text("H2", style = MaterialTheme.typography.labelSmall, color = tint)
        KeyboardToolbarItem.CHECKBOX -> ToolbarItemDrawable(Res.drawable.check_square, item, tint, 16.dp)
        KeyboardToolbarItem.BULLETED_LIST -> ToolbarItemDrawable(Res.drawable.unordered_list, item, tint, 16.dp)
        KeyboardToolbarItem.NUMBERED_LIST -> ToolbarItemDrawable(Res.drawable.ordered_list, item, tint, 15.dp)
        KeyboardToolbarItem.TOGGLE_LIST -> ToolbarItemDrawable(Res.drawable.arrow_right2, item, tint, 18.dp)
        KeyboardToolbarItem.QUOTE -> ToolbarItemDrawable(Res.drawable.quote_down2, item, tint, 15.dp)
        KeyboardToolbarItem.CALLOUT -> ToolbarItemDrawable(Res.drawable.card_edit2, item, tint, 16.dp)
        KeyboardToolbarItem.CODE -> ToolbarItemDrawable(Res.drawable.code, item, tint, 18.dp)
        KeyboardToolbarItem.VOICE_NOTE -> ToolbarItemDrawable(Res.drawable.microphone, item, tint, 17.dp)
        KeyboardToolbarItem.IMAGE -> ToolbarItemDrawable(Res.drawable.image, item, tint, 17.dp)
        KeyboardToolbarItem.DOCUMENT -> ToolbarItemDrawable(Res.drawable.document_text_12, item, tint, 17.dp)
        KeyboardToolbarItem.WEB_BOOKMARK -> ToolbarItemDrawable(Res.drawable.bookmark, item, tint, 16.dp)
        KeyboardToolbarItem.SIMPLE_TABLE -> ToolbarItemDrawable(Res.drawable.table, item, tint, 17.dp)
        KeyboardToolbarItem.DATABASE -> ToolbarItemDrawable(Res.drawable.database, item, tint, 17.dp)
        KeyboardToolbarItem.CANVAS -> ToolbarItemDrawable(Res.drawable.group, item, tint, 17.dp)
        KeyboardToolbarItem.LINKED_NOTE -> ToolbarItemDrawable(Res.drawable.link, item, tint, 17.dp)
        KeyboardToolbarItem.LINKED_CANVAS -> ToolbarItemDrawable(Res.drawable.group, item, tint, 17.dp)
        KeyboardToolbarItem.LINKED_DATABASE -> ToolbarItemDrawable(Res.drawable.database, item, tint, 17.dp)
        KeyboardToolbarItem.SOLID_LINE_DIVIDER -> ToolbarItemDrawable(Res.drawable.minus, item, tint, 18.dp)
        KeyboardToolbarItem.THREE_DOTS_DIVIDER -> ToolbarItemDrawable(Res.drawable.ellipsis, item, tint, 18.dp)
        KeyboardToolbarItem.BOLD -> ToolbarItemDrawable(Res.drawable.format_bold, item, tint, 14.dp)
        KeyboardToolbarItem.ITALIC -> ToolbarItemDrawable(Res.drawable.italic, item, tint, 14.dp)
        KeyboardToolbarItem.STRIKETHROUGH -> ToolbarItemDrawable(Res.drawable.text_x, item, tint, 16.dp)
        KeyboardToolbarItem.UNDERLINE -> ToolbarItemDrawable(Res.drawable.underline, item, tint, 16.dp)
        KeyboardToolbarItem.HIGHLIGHT -> ToolbarItemDrawable(Res.drawable.text_highlight, item, tint, 19.dp)
        KeyboardToolbarItem.ALIGN_LEFT -> ToolbarItemDrawable(Res.drawable.textalign_left2, item, tint, 18.dp)
        KeyboardToolbarItem.ALIGN_RIGHT -> ToolbarItemDrawable(Res.drawable.textalign_right2, item, tint, 18.dp)
        KeyboardToolbarItem.ALIGN_CENTER -> ToolbarItemDrawable(Res.drawable.textalign_center2, item, tint, 18.dp)
        KeyboardToolbarItem.JUSTIFY -> ToolbarItemDrawable(Res.drawable.textalign_justifycenter2, item, tint, 18.dp)
        KeyboardToolbarItem.DECREASE_INDENT -> ToolbarItemDrawable(Res.drawable.indent_right, item, tint, 18.dp)
        KeyboardToolbarItem.INCREASE_INDENT -> ToolbarItemDrawable(Res.drawable.indent_left, item, tint, 18.dp)
        KeyboardToolbarItem.MORE_BLOCKS -> ToolbarItemDrawable(Res.drawable.plus, item, tint, 17.dp)
        KeyboardToolbarItem.DIVIDER -> Box(
            modifier = Modifier
                .width(1.5.dp)
                .height(18.dp)
                .background(tint.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun ToolbarItemDrawable(drawable: DrawableResource, item: KeyboardToolbarItem, tint: Color, size: Dp) {
    Icon(painterResource(drawable), item.label(), tint = tint, modifier = Modifier.size(size))
}
