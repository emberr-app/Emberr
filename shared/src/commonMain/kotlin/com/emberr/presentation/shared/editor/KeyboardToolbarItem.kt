package com.emberr.presentation.shared.editor

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
import emberr.shared.generated.resources.card_edit2
import emberr.shared.generated.resources.check_square
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

enum class KeyboardToolbarItem(val title: String) {
    UNDO("Undo"),
    REDO("Redo"),
    LINK_TO_NOTE("Mention a Note"),
    SELECT_BLOCK("Select Block"),
    TEXT("Text"),
    HEADING_1("Heading 1"),
    HEADING_2("Heading 2"),
    CHECKBOX("Checkbox"),
    BULLETED_LIST("Bulleted List"),
    NUMBERED_LIST("Numbered List"),
    TOGGLE_LIST("Toggle List"),
    QUOTE("Quote"),
    CALLOUT("Callout"),
    CODE("Code"),
    VOICE_NOTE("Voice Note"),
    IMAGE("Image"),
    DOCUMENT("Document / File"),
    WEB_BOOKMARK("Web Bookmark"),
    SIMPLE_TABLE("Simple Table"),
    DATABASE("Database"),
    CANVAS("Canvas"),
    LINKED_NOTE("Link to Note"),
    LINKED_CANVAS("Link to Canvas"),
    LINKED_DATABASE("Link to Database"),
    SOLID_LINE_DIVIDER("Solid Line Divider"),
    THREE_DOTS_DIVIDER("Three Dots Divider"),
    BOLD("Bold"),
    ITALIC("Italic"),
    STRIKETHROUGH("Strikethrough"),
    UNDERLINE("Underline"),
    HIGHLIGHT("Highlight"),
    ALIGN_LEFT("Align Left"),
    ALIGN_RIGHT("Align Right"),
    ALIGN_CENTER("Align Center"),
    JUSTIFY("Justify"),
    DECREASE_INDENT("Decrease Indent"),
    INCREASE_INDENT("Increase Indent"),
    MORE_BLOCKS("More Blocks"),
    DIVIDER("Separator")
}

val DefaultKeyboardToolbarItems: List<KeyboardToolbarItem> = listOf(
    KeyboardToolbarItem.UNDO,
    KeyboardToolbarItem.REDO,
    KeyboardToolbarItem.DIVIDER,
    KeyboardToolbarItem.LINK_TO_NOTE,
    KeyboardToolbarItem.SELECT_BLOCK,
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
    KeyboardToolbarItem.DECREASE_INDENT,
    KeyboardToolbarItem.INCREASE_INDENT,
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
        KeyboardToolbarItem.TEXT -> Icon(Icons.AutoMirrored.Filled.Subject, item.title, tint = tint, modifier = Modifier.size(19.dp))
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
    Icon(painterResource(drawable), item.title, tint = tint, modifier = Modifier.size(size))
}
