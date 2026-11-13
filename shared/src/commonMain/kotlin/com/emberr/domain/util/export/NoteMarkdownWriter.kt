package com.emberr.domain.util.export

import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CanvasBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.CredentialFieldKind
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.QuoteBlock
import com.emberr.domain.model.SolidDividerBlock
import com.emberr.domain.model.TableBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ThreeDotDividerBlock
import com.emberr.domain.model.ToggleBlock
import com.emberr.domain.model.UnknownBlock
import com.emberr.domain.model.VoiceBlock
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.highlightColorNameOrNull
import com.emberr.domain.model.inlineSpansOrEmpty
import com.emberr.domain.model.lastDate
import com.emberr.domain.model.plainLine
import com.emberr.domain.model.summaryLine
import com.emberr.domain.model.valueAsText
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val TOGGLE_MARKER = "- ▸ "
private const val CHECKED_MARKER = "- [x] "
private const val UNCHECKED_MARKER = "- [ ] "
private const val BULLET_MARKER = "- "
private const val SOLID_DIVIDER_LINE = "---"
private const val DOT_DIVIDER_LINE = "* * *"
private const val MOVIE_MARKER = "🎬 "
private const val DEFAULT_CODE_LANGUAGE = "plaintext"
private const val MAX_HEADING_LEVEL = 6
private const val MAX_INDENT_LEVELS = 10
private const val SECONDS_PER_MINUTE = 60
private const val WIKI_LINK_ALIAS_SEPARATOR = "|"
private const val TABLE_WIKI_LINK_ALIAS_SEPARATOR = "\\|"

private val emberrNoteLinkPattern = Regex("""\[([^\]]+)\]\(emberr://note/([^)]+)\)""")

private data class RenderOptions(
    val noteTitlesById: Map<String, String>,
    val categoryNamesById: Map<String, String>,
    val attachmentPathsByStoredName: Map<String, String>,
    val personNamesById: Map<String, String>,
    val canvasPathsByNoteId: Map<String, String>,
    val vaultLinkTargetsById: Map<String, String>?,
    val timeZone: TimeZone
)

object NoteMarkdownWriter {

    fun writeMarkdown(
        blocks: List<NoteBlock>,
        title: String? = null,
        noteTitlesById: Map<String, String> = emptyMap(),
        categoryNamesById: Map<String, String> = emptyMap(),
        attachmentPathsByStoredName: Map<String, String> = emptyMap(),
        personNamesById: Map<String, String> = emptyMap(),
        canvasPathsByNoteId: Map<String, String> = emptyMap(),
        vaultLinkTargetsById: Map<String, String>? = null
    ): String {
        val visibleBlocks = blocks.filter { !it.isDeleted }
        val options = RenderOptions(
            noteTitlesById = noteTitlesById,
            categoryNamesById = categoryNamesById,
            attachmentPathsByStoredName = attachmentPathsByStoredName,
            personNamesById = personNamesById,
            canvasPathsByNoteId = canvasPathsByNoteId,
            vaultLinkTargetsById = vaultLinkTargetsById,
            timeZone = TimeZone.currentSystemDefault()
        )

        val body = renderBlocks(visibleBlocks, options, indentationToIgnore = 0)

        return buildString {
            if (!title.isNullOrBlank()) {
                append("# ")
                append(flattenLineBreaks(title).trim())
                append("\n\n")
            }
            append(body)
        }.trim()
    }

    private fun renderBlocks(blocks: List<NoteBlock>, options: RenderOptions, indentationToIgnore: Int): String {
        val outerBlocks = mutableListOf<NoteBlock>()
        val calloutBodies = mutableMapOf<String, List<NoteBlock>>()
        var index = 0
        while (index < blocks.size) {
            val block = blocks[index]
            outerBlocks.add(block)
            index++
            if (block is CalloutBlock) {
                val body = blocks.drop(index).takeWhile { it.indentationLevel > block.indentationLevel }
                calloutBodies[block.id] = body
                index += body.size
            }
        }

        val indentsByLevel = mutableListOf("")
        return joinRenderedBlocks(outerBlocks) { block ->
            val level = (block.indentationLevel - indentationToIgnore).coerceIn(0, minOf(indentsByLevel.lastIndex, MAX_INDENT_LEVELS))
            val indent = indentsByLevel[level]
            val rendered = withVaultNoteLinks(renderBlock(block, options, indent), block, options)
            if (rendered.isNotEmpty()) {
                indentsByLevel.subList(level + 1, indentsByLevel.size).clear()
                if (isListStyleBlock(block)) indentsByLevel.add(indent + " ".repeat(nestedContentWidthOf(block)))
            }
            when {
                block is CalloutBlock -> quoteCallout(block, rendered, calloutBodies.getValue(block.id), options, indent)
                isListStyleBlock(block) -> rendered
                else -> indentEveryLine(rendered, indent)
            }
        }
    }

    private fun nestedContentWidthOf(block: NoteBlock): Int =
        if (block is NumberedListBlock) "${block.number}. ".length else BULLET_MARKER.length

    private fun indentEveryLine(text: String, indent: String): String =
        if (indent.isEmpty()) text
        else text.split('\n').joinToString("\n") { line -> if (line.isEmpty()) line else "$indent$line" }

    private fun quoteCallout(
        callout: CalloutBlock,
        renderedHeader: String,
        body: List<NoteBlock>,
        options: RenderOptions,
        indent: String
    ): String {
        val renderedBody = renderBlocks(body, options, indentationToIgnore = callout.indentationLevel + 1)
        val calloutText = if (renderedBody.isEmpty()) renderedHeader else "$renderedHeader\n$renderedBody"
        return calloutText
            .split('\n')
            .joinToString("\n") { line -> "$indent> $line".trimEnd() }
    }

    private fun joinRenderedBlocks(
        blocks: List<NoteBlock>,
        render: (NoteBlock) -> String
    ): String {
        val output = StringBuilder()
        var previousBlock: NoteBlock? = null

        for (block in blocks) {
            val rendered = render(block)
            if (rendered.isEmpty()) continue

            if (output.isNotEmpty()) {
                val staysTight = previousBlock != null &&
                    isListStyleBlock(previousBlock) &&
                    isListStyleBlock(block)
                output.append(if (staysTight) "\n" else "\n\n")
            }
            output.append(rendered)
            previousBlock = block
        }
        return output.toString()
    }

    private fun isListStyleBlock(block: NoteBlock): Boolean = when (block) {
        is CheckboxBlock, is BulletedListBlock, is NumberedListBlock, is ToggleBlock -> true
        else -> false
    }

    private fun renderBlock(block: NoteBlock, options: RenderOptions, indent: String): String {
        return when (block) {
            is TextBlock -> formatText(block, block.text)
            is HeadingBlock -> renderHeading(block)
            is QuoteBlock -> renderQuote(block)
            is CheckboxBlock -> renderListItem(
                block = block,
                text = block.text,
                marker = if (block.isChecked) CHECKED_MARKER else UNCHECKED_MARKER,
                indent = indent,
                trailingGroup = renderTaskAttributes(block, options)
            )
            is BulletedListBlock -> renderListItem(block, block.text, BULLET_MARKER, indent)
            is NumberedListBlock -> renderListItem(block, block.text, "${block.number}. ", indent)
            is ToggleBlock -> renderListItem(block, block.text, TOGGLE_MARKER, indent)
            is CalloutBlock -> renderCalloutHeader(block)
            is CodeBlock -> renderCode(block)
            is BookmarkBlock -> renderBookmark(block)
            is LinkedNoteBlock -> renderLinkedNote(block, options)
            is ImageBlock -> renderImage(block, options)
            is DocumentBlock -> renderDocument(block, options)
            is VoiceBlock -> renderVoice(block, options)
            is CanvasBlock -> options.canvasPathsByNoteId[block.canvasNoteId]?.let { path -> "![[$path]]" } ?: "[Canvas]"
            is PropertyBlock -> renderProperty(block, options)
            is DatabaseBlock -> ""
            is MovieBlock -> renderMovie(block, options)
            is WatchlistBlock -> ""
            is CredentialsBlock -> renderCredentials(block)
            is PersonDetailBlock -> flattenLineBreaks(block.plainLine())
            is TableBlock -> renderTable(block)
            is SolidDividerBlock -> SOLID_DIVIDER_LINE
            is ThreeDotDividerBlock -> DOT_DIVIDER_LINE
            is UnknownBlock -> ""
        }
    }

    private fun renderHeading(block: HeadingBlock): String {
        val level = block.level.coerceIn(1, MAX_HEADING_LEVEL)
        val formattedText = formatText(block, flattenLineBreaks(block.text))
        return ("#".repeat(level) + " " + formattedText).trimEnd()
    }

    private fun renderQuote(block: QuoteBlock): String =
        formatText(block, block.text)
            .split('\n')
            .joinToString("\n") { line -> "> $line".trimEnd() }

    private fun renderCalloutHeader(block: CalloutBlock): String {
        val foldMarker = when {
            !block.isFoldable -> ""
            block.isExpanded -> "+"
            else -> "-"
        }
        val formattedTitle = formatText(block, flattenLineBreaks(block.text))
        return "[!${block.calloutTypeName.lowercase()}]$foldMarker $formattedTitle".trimEnd()
    }

    private fun renderListItem(
        block: NoteBlock,
        text: String,
        marker: String,
        indent: String,
        trailingGroup: String? = null
    ): String {
        val formattedText = formatText(block, text)
        val continuationIndent = indent + " ".repeat(marker.length)

        val renderedLines = formattedText
            .split('\n')
            .mapIndexed { lineIndex, line ->
                if (lineIndex == 0) "$indent$marker$line".trimEnd()
                else "$continuationIndent$line".trimEnd()
            }
            .joinToString("\n")

        return if (trailingGroup == null) renderedLines else "$renderedLines $trailingGroup"
    }

    private fun renderTaskAttributes(block: CheckboxBlock, options: RenderOptions): String? {
        val allDay = block.allDay
        val allDayStartDate = block.reminderTimestamp?.takeIf { allDay != null }
            ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(options.timeZone).date }
        val allDayLastDate = allDayStartDate?.let { allDay?.lastDate(it) }?.takeIf { it != allDayStartDate }
        return TaskAttributesFormat.render(
            TaskAttributes(
                dueText = allDayStartDate?.toString()
                    ?: block.reminderTimestamp?.let { TaskDueTimeFormat.render(it, options.timeZone) },
                endDateText = allDayLastDate?.toString(),
                reminderTimeText = block.reminderTimestamp?.takeIf { allDay?.isReminderOn == true }
                    ?.let { TaskDueTimeFormat.renderTime(it, options.timeZone) },
                durationMinutes = block.durationMinutes.takeIf { allDay == null && it != DEFAULT_TASK_DURATION_MINUTES },
                categoryName = block.categoryId?.let { options.categoryNamesById[it] },
                repeatText = block.recurrenceRule?.let { TaskRepeatFormat.render(it) },
                link = block.url?.takeIf { it.isNotBlank() },
                details = block.description?.takeIf { it.isNotBlank() }
            )
        )
    }

    private fun renderCode(block: CodeBlock): String {
        val fence = "`".repeat(longestBacktickRun(block.code).coerceAtLeast(2) + 1)
        val language = if (block.language.isBlank() || block.language == DEFAULT_CODE_LANGUAGE) {
            ""
        } else {
            block.language
        }

        return buildString {
            append(fence)
            append(language)
            append('\n')
            if (block.code.isNotEmpty()) {
                append(block.code)
                append('\n')
            }
            append(fence)
        }
    }

    private fun renderBookmark(block: BookmarkBlock): String {
        val label = block.title?.takeIf { it.isNotBlank() } ?: block.url.ifBlank { "Bookmark" }
        return "[${escapeLinkLabel(label)}](${escapeLinkTarget(block.url)})"
    }

    private fun withVaultNoteLinks(rendered: String, block: NoteBlock, options: RenderOptions): String {
        val linkTargetsById = options.vaultLinkTargetsById ?: return rendered
        val aliasSeparator = if (block is TableBlock) TABLE_WIKI_LINK_ALIAS_SEPARATOR else WIKI_LINK_ALIAS_SEPARATOR
        return emberrNoteLinkPattern.replace(rendered) { link ->
            val title = link.groupValues[1]
            val target = linkTargetsById[link.groupValues[2]]
            if (target == null) title else wikiLinkTo(target, title, aliasSeparator)
        }
    }

    private fun wikiLinkTo(target: String, title: String, aliasSeparator: String = WIKI_LINK_ALIAS_SEPARATOR): String {
        val alias = flattenLineBreaks(title).replace("[", "").replace("]", "").replace("|", "").trim()
        return if (alias.isEmpty() || alias == target) "[[$target]]" else "[[$target$aliasSeparator$alias]]"
    }

    private fun renderLinkedNote(block: LinkedNoteBlock, options: RenderOptions): String {
        val knownTitle = options.noteTitlesById[block.linkedNoteId]?.takeIf { it.isNotBlank() }
        options.vaultLinkTargetsById?.let { linkTargetsById ->
            val target = linkTargetsById[block.linkedNoteId]
            return when {
                target != null -> wikiLinkTo(target, knownTitle.orEmpty())
                knownTitle != null -> InlineMarkdownFormatter.escapeMarkdown(flattenLineBreaks(knownTitle))
                else -> ""
            }
        }
        val title = knownTitle ?: return ""

        val safeTitle = flattenLineBreaks(title)
            .replace("[", "")
            .replace("]", "")
            .trim()
        return "[[${safeTitle.ifEmpty { "Untitled" }}]]"
    }

    private fun renderImage(block: ImageBlock, options: RenderOptions): String {
        if (MarkdownAttachments.storedFileNameOf(block.localFilePath) == null) return ""
        val target = exportedMediaLinkTargetOf(block.localFilePath, options) ?: return "[Image]"
        return "![]($target)"
    }

    private fun renderDocument(block: DocumentBlock, options: RenderOptions): String {
        if (MarkdownAttachments.storedFileNameOf(block.localFilePath) == null) return ""
        val label = escapeLinkLabel(block.fileName.ifBlank { "Document" })
        val target = exportedMediaLinkTargetOf(block.localFilePath, options) ?: return "[File: $label]"
        return "[$label]($target)"
    }

    private fun renderVoice(block: VoiceBlock, options: RenderOptions): String {
        if (MarkdownAttachments.storedFileNameOf(block.localFilePath) == null) return ""
        val label = "Voice note (${durationTextOf(block.durationSeconds)})"
        val target = exportedMediaLinkTargetOf(block.localFilePath, options) ?: return "[$label]"
        return "[$label]($target)"
    }

    private fun durationTextOf(totalSeconds: Int): String {
        val minutes = totalSeconds / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    private fun renderMovie(block: MovieBlock, options: RenderOptions): String {
        val summary = block.summaryLine() ?: return ""
        val summaryLine = "$MOVIE_MARKER${flattenLineBreaks(summary)}"
        val posterTarget = exportedMediaLinkTargetOf(block.details?.posterFilePath, options)
        return if (posterTarget == null) summaryLine else "![]($posterTarget)\n\n$summaryLine"
    }

    private fun renderCredentials(block: CredentialsBlock): String {
        val usernameLines = block.fields
            .filter { it.kind == CredentialFieldKind.USERNAME && it.plainValue.isNotBlank() }
            .map { field -> "- ${flattenLineBreaks(field.label).ifBlank { "Username" }}: ${flattenLineBreaks(field.plainValue)}" }
        if (block.serviceName.isBlank() && block.loginUrl.isBlank() && usernameLines.isEmpty()) return ""
        val title = "**${flattenLineBreaks(block.serviceName).ifBlank { "Credentials" }}**"
        val header = if (block.loginUrl.isBlank()) title else "$title [${escapeLinkLabel(block.loginUrl)}](${escapeLinkTarget(block.loginUrl)})"
        return (listOf(header) + usernameLines).joinToString("\n")
    }

    private fun renderProperty(block: PropertyBlock, options: RenderOptions): String {
        val value = flattenLineBreaks(block.valueAsText { options.personNamesById[it] })
        return if (value.isBlank()) "" else "**${flattenLineBreaks(block.label)}:** $value"
    }

    private fun renderTable(block: TableBlock): String {
        if (block.rows.isEmpty()) return ""

        val columnCount = block.rows.maxOf { it.size }.coerceAtLeast(1)
        val lines = mutableListOf<String>()

        block.rows.forEachIndexed { rowIndex, cells ->
            lines.add(renderTableRow(cells, columnCount))
            if (rowIndex == 0) lines.add(renderTableSeparator(columnCount))
        }
        return lines.joinToString("\n")
    }

    private fun renderTableRow(cells: List<String>, columnCount: Int): String =
        (0 until columnCount).joinToString(" | ", prefix = "| ", postfix = " |") { columnIndex ->
            escapeTableCell(cells.getOrNull(columnIndex).orEmpty())
        }

    private fun renderTableSeparator(columnCount: Int): String =
        (0 until columnCount).joinToString(" | ", prefix = "| ", postfix = " |") { "---" }

    private fun escapeTableCell(value: String): String = value
        .replace("\\", "\\\\")
        .replace("|", "\\|")
        .replace("\r", "")
        .replace("\n", "<br>")

    private fun formatText(block: NoteBlock, text: String): String =
        InlineMarkdownFormatter.toMarkdown(
            text = text,
            spans = block.inlineSpansOrEmpty(),
            isWholeBlockBold = block.isBold,
            isWholeBlockItalic = block.isItalic,
            isWholeBlockStrikeThrough = block.isStrikeThrough,
            isWholeBlockUnderlined = block.isUnderlined,
            isWholeBlockHighlighted = block.isHighlighted,
            wholeBlockHighlightColorName = block.highlightColorNameOrNull()
        )

    private fun flattenLineBreaks(text: String): String = buildString(text.length) {
        for (character in text) {
            if (character == '\n' || character == '\r') append(' ') else append(character)
        }
    }

    private fun longestBacktickRun(text: String): Int {
        var longestRun = 0
        var currentRun = 0
        for (character in text) {
            if (character == '`') {
                currentRun++
                if (currentRun > longestRun) longestRun = currentRun
            } else {
                currentRun = 0
            }
        }
        return longestRun
    }

    private fun exportedMediaLinkTargetOf(localFilePath: String?, options: RenderOptions): String? {
        val storedFileName = MarkdownAttachments.storedFileNameOf(localFilePath) ?: return null
        val pathInExport = options.attachmentPathsByStoredName[storedFileName] ?: return null
        return escapeLinkTarget(pathInExport)
    }

    private fun escapeLinkLabel(label: String): String = flattenLineBreaks(label)
        .replace("\\", "\\\\")
        .replace("[", "\\[")
        .replace("]", "\\]")

    private fun escapeLinkTarget(target: String): String {
        val singleLine = flattenLineBreaks(target).trim()
        if (singleLine.isEmpty()) return ""
        val needsAngleBrackets = singleLine.any { it == ' ' || it == '(' || it == ')' }
        return if (needsAngleBrackets) "<${singleLine.replace(">", "%3E")}>" else singleLine
    }
}
