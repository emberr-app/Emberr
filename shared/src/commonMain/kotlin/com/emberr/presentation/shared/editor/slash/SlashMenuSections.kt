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
import emberr.shared.generated.resources.editor_align_center
import emberr.shared.generated.resources.editor_align_justify
import emberr.shared.generated.resources.editor_align_left
import emberr.shared.generated.resources.editor_align_right
import emberr.shared.generated.resources.editor_block_bulleted_list
import emberr.shared.generated.resources.editor_block_callout
import emberr.shared.generated.resources.editor_block_canvas
import emberr.shared.generated.resources.editor_block_code_block
import emberr.shared.generated.resources.editor_block_credentials
import emberr.shared.generated.resources.editor_block_database
import emberr.shared.generated.resources.editor_block_document_file
import emberr.shared.generated.resources.editor_block_heading_1
import emberr.shared.generated.resources.editor_block_heading_2
import emberr.shared.generated.resources.editor_block_image
import emberr.shared.generated.resources.editor_block_link_to_canvas
import emberr.shared.generated.resources.editor_block_link_to_database
import emberr.shared.generated.resources.editor_block_link_to_note
import emberr.shared.generated.resources.editor_block_movie_or_show
import emberr.shared.generated.resources.editor_block_numbered_list
import emberr.shared.generated.resources.editor_block_quote
import emberr.shared.generated.resources.editor_block_simple_table
import emberr.shared.generated.resources.editor_block_text
import emberr.shared.generated.resources.editor_block_todo_list
import emberr.shared.generated.resources.editor_block_toggle_list
import emberr.shared.generated.resources.editor_block_voice_note
import emberr.shared.generated.resources.editor_block_watchlist
import emberr.shared.generated.resources.editor_block_web_bookmark
import emberr.shared.generated.resources.editor_current_date
import emberr.shared.generated.resources.editor_current_time
import emberr.shared.generated.resources.editor_divider_solid_line
import emberr.shared.generated.resources.editor_divider_three_dots
import emberr.shared.generated.resources.editor_format_bold_text
import emberr.shared.generated.resources.editor_format_highlight_text
import emberr.shared.generated.resources.editor_format_italic_text
import emberr.shared.generated.resources.editor_format_strikethrough_text
import emberr.shared.generated.resources.editor_format_underline_text
import emberr.shared.generated.resources.editor_indent_decrease
import emberr.shared.generated.resources.editor_indent_increase
import emberr.shared.generated.resources.editor_slash_section_advanced_blocks
import emberr.shared.generated.resources.editor_slash_section_alignment
import emberr.shared.generated.resources.editor_slash_section_basic_blocks
import emberr.shared.generated.resources.editor_slash_section_date_time
import emberr.shared.generated.resources.editor_slash_section_dividers
import emberr.shared.generated.resources.editor_slash_section_indentation
import emberr.shared.generated.resources.editor_slash_section_inline_formatting
import emberr.shared.generated.resources.editor_slash_section_media_links
import emberr.shared.generated.resources.editor_slash_section_properties
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource

fun propertyInsertKey(propertyType: PropertyType): String = "property_${propertyType.name.lowercase()}"

fun customPropertyInsertKey(propertyId: String): String = "custom_property_$propertyId"

class SlashMenuLabels(private val textByResource: Map<StringResource, String>) {
    operator fun get(resource: StringResource): String = textByResource.getValue(resource)
}

private val slashMenuLabelResources = listOf(
    Res.string.editor_block_text,
    Res.string.editor_block_heading_1,
    Res.string.editor_block_heading_2,
    Res.string.editor_block_todo_list,
    Res.string.editor_block_bulleted_list,
    Res.string.editor_block_numbered_list,
    Res.string.editor_block_toggle_list,
    Res.string.editor_block_quote,
    Res.string.editor_block_callout,
    Res.string.editor_block_code_block,
    Res.string.editor_block_voice_note,
    Res.string.editor_block_image,
    Res.string.editor_block_document_file,
    Res.string.editor_block_web_bookmark,
    Res.string.editor_block_movie_or_show,
    Res.string.editor_block_watchlist,
    Res.string.editor_block_link_to_note,
    Res.string.editor_block_link_to_canvas,
    Res.string.editor_block_link_to_database,
    Res.string.editor_block_simple_table,
    Res.string.editor_block_database,
    Res.string.editor_block_canvas,
    Res.string.editor_block_credentials,
    Res.string.editor_format_bold_text,
    Res.string.editor_format_italic_text,
    Res.string.editor_format_underline_text,
    Res.string.editor_format_strikethrough_text,
    Res.string.editor_format_highlight_text,
    Res.string.editor_align_left,
    Res.string.editor_align_right,
    Res.string.editor_align_center,
    Res.string.editor_align_justify,
    Res.string.editor_indent_decrease,
    Res.string.editor_indent_increase,
    Res.string.editor_divider_solid_line,
    Res.string.editor_divider_three_dots,
    Res.string.editor_current_time,
    Res.string.editor_current_date,
    Res.string.editor_slash_section_basic_blocks,
    Res.string.editor_slash_section_media_links,
    Res.string.editor_slash_section_advanced_blocks,
    Res.string.editor_slash_section_properties,
    Res.string.editor_slash_section_inline_formatting,
    Res.string.editor_slash_section_alignment,
    Res.string.editor_slash_section_indentation,
    Res.string.editor_slash_section_dividers,
    Res.string.editor_slash_section_date_time
)

@Composable
fun rememberSlashMenuLabels(): SlashMenuLabels {
    val texts = slashMenuLabelResources.map { stringResource(it) }
    return remember(texts) { SlashMenuLabels(slashMenuLabelResources.zip(texts).toMap()) }
}

@Composable
fun rememberCustomProperties(): List<CustomPropertyEntity> {
    val noteRepository = koinInject<NoteRepository>()
    val customProperties by remember(noteRepository) { noteRepository.getCustomProperties() }
        .collectAsState(initial = emptyList())
    return customProperties
}

fun buildSlashMenuSections(
    labels: SlashMenuLabels,
    onChangeBlockType: (String) -> Unit,
    onToggleFormat: (String) -> Unit,
    onAdjustIndentation: (Boolean) -> Unit,
    onSetAlignment: (TextAlignment) -> Unit,
    onInsertMediaBlock: (String) -> Unit,
    onInsertText: (String) -> Unit,
    usesTwentyFourHourClock: Boolean,
    customProperties: List<CustomPropertyEntity> = emptyList()
): List<SlashMenuSectionData> = listOf(
    SlashMenuSectionData(labels[Res.string.editor_slash_section_basic_blocks], listOf(
        SlashMenuItemData(labels[Res.string.editor_block_text], Icons.AutoMirrored.Filled.Subject) { onChangeBlockType("text") },
        SlashMenuItemData(labels[Res.string.editor_block_heading_1], SlashMenuIcon.Label("H1")) { onChangeBlockType("h1") },
        SlashMenuItemData(labels[Res.string.editor_block_heading_2], SlashMenuIcon.Label("H2")) { onChangeBlockType("h2") },
        SlashMenuItemData(labels[Res.string.editor_block_todo_list], Res.drawable.check_square) { onChangeBlockType("checkbox") },
        SlashMenuItemData(labels[Res.string.editor_block_bulleted_list], Res.drawable.unordered_list, 15.dp) { onChangeBlockType("bullet") },
        SlashMenuItemData(labels[Res.string.editor_block_numbered_list], Res.drawable.ordered_list, 14.dp) { onChangeBlockType("number") },
        SlashMenuItemData(labels[Res.string.editor_block_toggle_list], Res.drawable.arrow_right2) { onChangeBlockType("toggle") },
        SlashMenuItemData(labels[Res.string.editor_block_quote], Res.drawable.quote_down2, 14.dp) { onChangeBlockType("quote") },
        SlashMenuItemData(labels[Res.string.editor_block_callout], Res.drawable.card_edit2) { onChangeBlockType("callout") },
        SlashMenuItemData(labels[Res.string.editor_block_code_block], Res.drawable.code) { onChangeBlockType("code") }
    )),
    SlashMenuSectionData(labels[Res.string.editor_slash_section_media_links], listOf(
        SlashMenuItemData(labels[Res.string.editor_block_voice_note], Res.drawable.microphone) { onChangeBlockType("voice") },
        SlashMenuItemData(labels[Res.string.editor_block_image], Res.drawable.image) { onInsertMediaBlock("image") },
        SlashMenuItemData(labels[Res.string.editor_block_document_file], Res.drawable.document_text_12) { onInsertMediaBlock("document") },
        SlashMenuItemData(labels[Res.string.editor_block_web_bookmark], Res.drawable.bookmark) { onInsertMediaBlock("bookmark") },
        SlashMenuItemData(labels[Res.string.editor_block_movie_or_show], Res.drawable.film) { onInsertMediaBlock("movie") },
        SlashMenuItemData(labels[Res.string.editor_block_watchlist], Res.drawable.tv) { onInsertMediaBlock("watchlist") },
        SlashMenuItemData(labels[Res.string.editor_block_link_to_note], Res.drawable.link) { onInsertMediaBlock("linked_note") },
        SlashMenuItemData(labels[Res.string.editor_block_link_to_canvas], Res.drawable.group) { onInsertMediaBlock("linked_canvas") },
        SlashMenuItemData(labels[Res.string.editor_block_link_to_database], Res.drawable.database) { onInsertMediaBlock("linked_database") }
    )),
    SlashMenuSectionData(labels[Res.string.editor_slash_section_advanced_blocks], listOf(
        SlashMenuItemData(labels[Res.string.editor_block_simple_table], Res.drawable.table) { onInsertMediaBlock("table") },
        SlashMenuItemData(labels[Res.string.editor_block_database], Res.drawable.database) { onInsertMediaBlock("database") },
        SlashMenuItemData(labels[Res.string.editor_block_canvas], Res.drawable.group) { onInsertMediaBlock("canvas") },
        SlashMenuItemData(labels[Res.string.editor_block_credentials], Res.drawable.lock_password) { onInsertMediaBlock("credentials") }
            .copy(searchWords = listOf("login", "password", "credentials", "account"))
    )),
    dateAndTimeSlashMenuSection(labels, onInsertText, usesTwentyFourHourClock),
    SlashMenuSectionData(
        labels[Res.string.editor_slash_section_properties],
        PropertyType.entries.filterNot { it.isOnlyForDatabases }.map { propertyType ->
            SlashMenuItemData(propertyType.label, propertyType.iconResource()) { onInsertMediaBlock(propertyInsertKey(propertyType)) }
        } + customProperties.map { property ->
            SlashMenuItemData(property.name, property.valueType.iconResource()) {
                onInsertMediaBlock(customPropertyInsertKey(property.propertyId))
            }
        }
    ),
    SlashMenuSectionData(labels[Res.string.editor_slash_section_inline_formatting], listOf(
        SlashMenuItemData(labels[Res.string.editor_format_bold_text], Res.drawable.format_bold, 13.dp) { onToggleFormat("bold") },
        SlashMenuItemData(labels[Res.string.editor_format_italic_text], Res.drawable.italic, 13.dp) { onToggleFormat("italic") },
        SlashMenuItemData(labels[Res.string.editor_format_underline_text], Res.drawable.underline, 15.dp) { onToggleFormat("underline") },
        SlashMenuItemData(labels[Res.string.editor_format_strikethrough_text], Res.drawable.text_x, 15.dp) { onToggleFormat("strike") },
        SlashMenuItemData(labels[Res.string.editor_format_highlight_text], Res.drawable.text_highlight, 19.dp) { onToggleFormat("highlight") }
    )),
    SlashMenuSectionData(labels[Res.string.editor_slash_section_alignment], listOf(
        SlashMenuItemData(labels[Res.string.editor_align_left], Res.drawable.textalign_left2) { onSetAlignment(TextAlignment.LEFT) },
        SlashMenuItemData(labels[Res.string.editor_align_right], Res.drawable.textalign_right2) { onSetAlignment(TextAlignment.RIGHT) },
        SlashMenuItemData(labels[Res.string.editor_align_center], Res.drawable.textalign_center2) { onSetAlignment(TextAlignment.CENTER) },
        SlashMenuItemData(labels[Res.string.editor_align_justify], Res.drawable.textalign_justifycenter2) { onSetAlignment(TextAlignment.JUSTIFY) }
    )),
    SlashMenuSectionData(labels[Res.string.editor_slash_section_indentation], listOf(
        SlashMenuItemData(labels[Res.string.editor_indent_decrease], Res.drawable.indent_right) { onAdjustIndentation(false) },
        SlashMenuItemData(labels[Res.string.editor_indent_increase], Res.drawable.indent_left) { onAdjustIndentation(true) }
    )),
    SlashMenuSectionData(labels[Res.string.editor_slash_section_dividers], listOf(
        SlashMenuItemData(labels[Res.string.editor_divider_solid_line], Res.drawable.minus) { onChangeBlockType("divider_solid") },
        SlashMenuItemData(labels[Res.string.editor_divider_three_dots], Res.drawable.ellipsis) { onChangeBlockType("divider_dots") }
    )),
)

fun dateAndTimeSlashMenuSection(labels: SlashMenuLabels, onInsertText: (String) -> Unit, usesTwentyFourHourClock: Boolean) =
    SlashMenuSectionData(labels[Res.string.editor_slash_section_date_time], listOf(
        SlashMenuItemData(labels[Res.string.editor_current_time], Res.drawable.clock_circle) { onInsertText(currentTimeText(usesTwentyFourHourClock)) },
        SlashMenuItemData(labels[Res.string.editor_current_date], Res.drawable.calendar_date) { onInsertText(currentDateText()) }
    ))

fun buildSlashMenuSearchTexts(labels: SlashMenuLabels, customProperties: List<CustomPropertyEntity> = emptyList()): List<String> =
    buildSlashMenuSections(
        labels = labels,
        onChangeBlockType = {},
        onToggleFormat = {},
        onAdjustIndentation = {},
        onSetAlignment = {},
        onInsertMediaBlock = {},
        onInsertText = {},
        usesTwentyFourHourClock = false,
        customProperties = customProperties
    ).flatMap { section -> section.items.flatMap { item -> listOf(item.label) + item.searchWords } }

fun slashQueryCanStillMatch(query: String, slashMenuSearchTexts: List<String>): Boolean =
    !query.contains(" ") && (query.length <= 2 || slashMenuSearchTexts.any { it.contains(query, ignoreCase = true) })

fun SlashMenuItemData.matchesSlashQuery(query: String): Boolean =
    query.isBlank() || label.contains(query, ignoreCase = true) || searchWords.any { it.contains(query, ignoreCase = true) }

fun filteredSlashMenuSections(
    labels: SlashMenuLabels,
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
    labels,
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
