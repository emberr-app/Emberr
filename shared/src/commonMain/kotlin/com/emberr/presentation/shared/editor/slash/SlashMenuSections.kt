package com.emberr.presentation.shared.editor.slash

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.domain.model.PropertyType
import com.emberr.domain.model.TextAlignment
import com.emberr.domain.repository.NoteRepository
import com.emberr.presentation.shared.editor.blockViews.property.iconResource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_right2
import emberr.shared.generated.resources.bookmark
import emberr.shared.generated.resources.calendar_date
import emberr.shared.generated.resources.card_edit2
import emberr.shared.generated.resources.check_square
import emberr.shared.generated.resources.clock_circle
import emberr.shared.generated.resources.code
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.database
import emberr.shared.generated.resources.document_text_12
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.film
import emberr.shared.generated.resources.tv
import emberr.shared.generated.resources.format_bold
import emberr.shared.generated.resources.group
import emberr.shared.generated.resources.image
import emberr.shared.generated.resources.indent_left
import emberr.shared.generated.resources.indent_right
import emberr.shared.generated.resources.italic
import emberr.shared.generated.resources.link
import emberr.shared.generated.resources.lock_password
import emberr.shared.generated.resources.microphone
import emberr.shared.generated.resources.minus
import emberr.shared.generated.resources.ordered_list
import emberr.shared.generated.resources.quote_down2
import emberr.shared.generated.resources.table
import emberr.shared.generated.resources.text_highlight
import emberr.shared.generated.resources.text_x
import emberr.shared.generated.resources.textalign_center2
import emberr.shared.generated.resources.textalign_justifycenter2
import emberr.shared.generated.resources.textalign_left2
import emberr.shared.generated.resources.textalign_right2
import emberr.shared.generated.resources.underline
import emberr.shared.generated.resources.unordered_list
import org.koin.compose.koinInject
import com.emberr.presentation.shared.editor.text.currentDateText
import com.emberr.presentation.shared.editor.text.currentTimeText

fun propertyInsertKey(propertyType: PropertyType): String = "property_${propertyType.name.lowercase()}"

fun customPropertyInsertKey(propertyId: String): String = "custom_property_$propertyId"

@Composable
fun rememberCustomProperties(): List<CustomPropertyEntity> {
    val noteRepository = koinInject<NoteRepository>()
    val customProperties by remember(noteRepository) { noteRepository.getCustomProperties() }
        .collectAsState(initial = emptyList())
    return customProperties
}

fun buildSlashMenuSections(
    onChangeBlockType: (String) -> Unit,
    onToggleFormat: (String) -> Unit,
    onAdjustIndentation: (Boolean) -> Unit,
    onSetAlignment: (TextAlignment) -> Unit,
    onInsertMediaBlock: (String) -> Unit,
    onInsertText: (String) -> Unit,
    usesTwentyFourHourClock: Boolean,
    customProperties: List<CustomPropertyEntity> = emptyList()
): List<SlashMenuSectionData> = listOf(
    SlashMenuSectionData("Basic Blocks", listOf(
        SlashMenuItemData("Text", Icons.AutoMirrored.Filled.Subject) { onChangeBlockType("text") },
        SlashMenuItemData("Heading 1", SlashMenuIcon.Label("H1")) { onChangeBlockType("h1") },
        SlashMenuItemData("Heading 2", SlashMenuIcon.Label("H2")) { onChangeBlockType("h2") },
        SlashMenuItemData("To-do List", Res.drawable.check_square) { onChangeBlockType("checkbox") },
        SlashMenuItemData("Bulleted List", Res.drawable.unordered_list, 15.dp) { onChangeBlockType("bullet") },
        SlashMenuItemData("Numbered List", Res.drawable.ordered_list, 14.dp) { onChangeBlockType("number") },
        SlashMenuItemData("Toggle List", Res.drawable.arrow_right2) { onChangeBlockType("toggle") },
        SlashMenuItemData("Quote", Res.drawable.quote_down2, 14.dp) { onChangeBlockType("quote") },
        SlashMenuItemData("Callout", Res.drawable.card_edit2) { onChangeBlockType("callout") },
        SlashMenuItemData("Code Block", Res.drawable.code) { onChangeBlockType("code") }
    )),
    SlashMenuSectionData("Media & Links", listOf(
        SlashMenuItemData("Voice Note", Res.drawable.microphone) { onChangeBlockType("voice") },
        SlashMenuItemData("Image", Res.drawable.image) { onInsertMediaBlock("image") },
        SlashMenuItemData("Document / File", Res.drawable.document_text_12) { onInsertMediaBlock("document") },
        SlashMenuItemData("Web Bookmark", Res.drawable.bookmark) { onInsertMediaBlock("bookmark") },
        SlashMenuItemData("Movie or Show", Res.drawable.film) { onInsertMediaBlock("movie") },
        SlashMenuItemData("Watchlist", Res.drawable.tv) { onInsertMediaBlock("watchlist") },
        SlashMenuItemData("Link to Note", Res.drawable.link) { onInsertMediaBlock("linked_note") },
        SlashMenuItemData("Link to Canvas", Res.drawable.group) { onInsertMediaBlock("linked_canvas") },
        SlashMenuItemData("Link to Database", Res.drawable.database) { onInsertMediaBlock("linked_database") }
    )),
    SlashMenuSectionData("Advanced Blocks", listOf(
        SlashMenuItemData("Simple Table", Res.drawable.table) { onInsertMediaBlock("table") },
        SlashMenuItemData("Database", Res.drawable.database) { onInsertMediaBlock("database") },
        SlashMenuItemData("Canvas", Res.drawable.group) { onInsertMediaBlock("canvas") },
        SlashMenuItemData("Credentials", Res.drawable.lock_password) { onInsertMediaBlock("credentials") }
            .copy(searchWords = listOf("login", "password", "credentials", "account"))
    )),
    dateAndTimeSlashMenuSection(onInsertText, usesTwentyFourHourClock),
    SlashMenuSectionData(
        "Properties",
        PropertyType.entries.filterNot { it.isOnlyForDatabases }.map { propertyType ->
            SlashMenuItemData(propertyType.label, propertyType.iconResource()) { onInsertMediaBlock(propertyInsertKey(propertyType)) }
        } + customProperties.map { property ->
            SlashMenuItemData(property.name, property.valueType.iconResource()) {
                onInsertMediaBlock(customPropertyInsertKey(property.propertyId))
            }
        }
    ),
    SlashMenuSectionData("Inline Text Formatting", listOf(
        SlashMenuItemData("Bold Text", Res.drawable.format_bold, 13.dp) { onToggleFormat("bold") },
        SlashMenuItemData("Italic Text", Res.drawable.italic, 13.dp) { onToggleFormat("italic") },
        SlashMenuItemData("Underline Text", Res.drawable.underline, 15.dp) { onToggleFormat("underline") },
        SlashMenuItemData("Strikethrough Text", Res.drawable.text_x, 15.dp) { onToggleFormat("strike") },
        SlashMenuItemData("Highlight Text", Res.drawable.text_highlight, 19.dp) { onToggleFormat("highlight") }
    )),
    SlashMenuSectionData("Alignment", listOf(
        SlashMenuItemData("Align Left", Res.drawable.textalign_left2) { onSetAlignment(TextAlignment.LEFT) },
        SlashMenuItemData("Align Right", Res.drawable.textalign_right2) { onSetAlignment(TextAlignment.RIGHT) },
        SlashMenuItemData("Align Center", Res.drawable.textalign_center2) { onSetAlignment(TextAlignment.CENTER) },
        SlashMenuItemData("Justify", Res.drawable.textalign_justifycenter2) { onSetAlignment(TextAlignment.JUSTIFY) }
    )),
    SlashMenuSectionData("Indentation", listOf(
        SlashMenuItemData("Decrease Indent", Res.drawable.indent_right) { onAdjustIndentation(false) },
        SlashMenuItemData("Increase Indent", Res.drawable.indent_left) { onAdjustIndentation(true) }
    )),
    SlashMenuSectionData("Dividers", listOf(
        SlashMenuItemData("Solid Line", Res.drawable.minus) { onChangeBlockType("divider_solid") },
        SlashMenuItemData("Three Dots", Res.drawable.ellipsis) { onChangeBlockType("divider_dots") }
    )),
)

fun dateAndTimeSlashMenuSection(onInsertText: (String) -> Unit, usesTwentyFourHourClock: Boolean) =
    SlashMenuSectionData("Date & Time", listOf(
        SlashMenuItemData("Current Time", Res.drawable.clock_circle) { onInsertText(currentTimeText(usesTwentyFourHourClock)) },
        SlashMenuItemData("Current Date", Res.drawable.calendar_date) { onInsertText(currentDateText()) }
    ))

fun SlashMenuItemData.matchesSlashQuery(query: String): Boolean =
    query.isBlank() || label.contains(query, ignoreCase = true) || searchWords.any { it.contains(query, ignoreCase = true) }

fun filteredSlashMenuSections(
    query: String,
    onChangeBlockType: (String) -> Unit,
    onToggleFormat: (String) -> Unit,
    onAdjustIndentation: (Boolean) -> Unit,
    onSetAlignment: (TextAlignment) -> Unit,
    onInsertMediaBlock: (String) -> Unit,
    onInsertText: (String) -> Unit,
    usesTwentyFourHourClock: Boolean,
    customProperties: List<CustomPropertyEntity> = emptyList()
): List<SlashMenuSectionData> = buildSlashMenuSections(
    onChangeBlockType,
    onToggleFormat,
    onAdjustIndentation,
    onSetAlignment,
    onInsertMediaBlock,
    onInsertText,
    usesTwentyFourHourClock,
    customProperties
).map { section ->
    section.copy(items = section.items.filter { it.matchesSlashQuery(query) })
}.filter { it.items.isNotEmpty() }
