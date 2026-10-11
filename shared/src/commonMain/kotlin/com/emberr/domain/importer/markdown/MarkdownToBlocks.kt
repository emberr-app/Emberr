package com.emberr.domain.importer.markdown

import com.emberr.domain.importer.ImportLinkResolver
import com.emberr.domain.importer.ImportPaths
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CalloutType
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.QuoteBlock
import com.emberr.domain.model.SolidDividerBlock
import com.emberr.domain.model.TableBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ThreeDotDividerBlock
import com.emberr.domain.model.ToggleBlock
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

private const val DEFAULT_CODE_LANGUAGE = "plaintext"
private const val MATH_CODE_LANGUAGE = "latex"
private const val DOT_DIVIDER_TEXT = "* * *"
private const val TOGGLE_SYMBOL = '▸'
private const val CODE_BLOCK_INDENT = 4
private const val BLOCK_COMMENT_DELIMITER = "%%"
private const val BLOCK_COMMENT_FENCE_LANGUAGE = "emberr-block-comment"
private const val BLOCK_COMMENT_TITLE = "Comment"
private const val MINIMUM_FENCE_LENGTH = 3

private val codeFenceOpening = Regex("^ {0,3}(`{3,}|~{3,})")
private val backtickRun = Regex("`+")
private val calloutMarker = Regex("""^\[!([A-Za-z][A-Za-z0-9_-]*)]([+-]?)""")
private val calloutMarkerWithSpaces = Regex("""^\[![^\]]*][+-]?\s*""")
private val calloutTypesByWrittenName: Map<String, CalloutType> = mapOf(
    "note" to CalloutType.NOTE,
    "abstract" to CalloutType.ABSTRACT,
    "summary" to CalloutType.ABSTRACT,
    "tldr" to CalloutType.ABSTRACT,
    "info" to CalloutType.INFO,
    "todo" to CalloutType.TODO,
    "tip" to CalloutType.TIP,
    "hint" to CalloutType.TIP,
    "important" to CalloutType.TIP,
    "success" to CalloutType.SUCCESS,
    "check" to CalloutType.SUCCESS,
    "done" to CalloutType.SUCCESS,
    "question" to CalloutType.QUESTION,
    "help" to CalloutType.QUESTION,
    "faq" to CalloutType.QUESTION,
    "warning" to CalloutType.WARNING,
    "caution" to CalloutType.WARNING,
    "attention" to CalloutType.WARNING,
    "failure" to CalloutType.FAILURE,
    "fail" to CalloutType.FAILURE,
    "missing" to CalloutType.FAILURE,
    "danger" to CalloutType.DANGER,
    "error" to CalloutType.DANGER,
    "bug" to CalloutType.BUG,
    "example" to CalloutType.EXAMPLE,
    "quote" to CalloutType.QUOTE,
    "cite" to CalloutType.QUOTE
)
private val htmlCommentPattern = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
private val lineBreakingHtmlTag = Regex(
    "</?(p|div|br|details|summary|li|ul|ol|tr|td|th|table|thead|tbody|h[1-6]|aside|blockquote|figure|figcaption|hr|section|header|footer)\\b[^>]*>",
    RegexOption.IGNORE_CASE
)
private val anyHtmlTag = Regex("<[^>]+>")
private val htmlMediaTag = Regex("<(img|iframe)\\b([^>]*)>", RegexOption.IGNORE_CASE)

private val headingTypes = setOf(
    MarkdownElementTypes.ATX_1,
    MarkdownElementTypes.ATX_2,
    MarkdownElementTypes.ATX_3,
    MarkdownElementTypes.ATX_4,
    MarkdownElementTypes.ATX_5,
    MarkdownElementTypes.ATX_6,
    MarkdownElementTypes.SETEXT_1,
    MarkdownElementTypes.SETEXT_2
)
private val firstLevelHeadingTypes = setOf(MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1)

internal class ConvertedMarkdown(
    val blocks: List<NoteBlock>,
    val pendingAttachments: List<PendingAttachment>,
    val pendingWebImages: List<PendingWebImage>,
    val inlineTags: List<String>
)

internal object MarkdownToBlocks {

    fun convert(
        markdown: String,
        linkResolver: ImportLinkResolver,
        now: Long,
        newBlockId: () -> String,
        baseIndentation: Int = 0
    ): ConvertedMarkdown {
        val markdownText: CharSequence = withBlockCommentsFenced(markdown)
        val parsed = try {
            val root = MarkdownParser(
                flavour = GFMFlavourDescriptor(),
                assertionsEnabled = false,
                cancellationToken = CancellationToken.NonCancellable
            ).buildMarkdownTreeFromString(markdownText)
            root to LinkMap.buildLinkMap(root, markdownText)
        } catch (_: Exception) {
            return ConvertedMarkdown(PlainTextToBlocks.convert(markdownText.toString(), now, newBlockId), emptyList(), emptyList(), emptyList())
        }

        val (root, linkMap) = parsed
        return MarkdownBlockBuilder(MarkdownDocument(markdownText, linkMap), linkResolver, now, newBlockId).build(root, baseIndentation)
    }

    private fun withBlockCommentsFenced(markdown: String): String {
        val lines = markdown.lines()
        val output = mutableListOf<String>()
        var openFence: String? = null
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val fenceOpening = codeFenceOpening.find(line)?.groupValues?.get(1)
            val closingCommentIndex = if (openFence == null && line.trim() == BLOCK_COMMENT_DELIMITER) {
                (index + 1 until lines.size).firstOrNull { lines[it].trim() == BLOCK_COMMENT_DELIMITER }
            } else {
                null
            }
            when {
                openFence != null -> {
                    output += line
                    if (closesFence(line, openFence)) openFence = null
                }
                fenceOpening != null -> {
                    output += line
                    openFence = fenceOpening
                }
                closingCommentIndex != null -> {
                    val commentLines = lines.subList(index + 1, closingCommentIndex)
                    val commentFence = "`".repeat(maxOf(MINIMUM_FENCE_LENGTH, longestBacktickRun(commentLines) + 1))
                    output += commentFence + BLOCK_COMMENT_FENCE_LANGUAGE
                    output += commentLines
                    output += commentFence
                    index = closingCommentIndex
                }
                else -> output += line
            }
            index++
        }
        return output.joinToString("\n")
    }

    private fun closesFence(line: String, openFence: String): Boolean {
        val trimmed = line.trim()
        return trimmed.length >= openFence.length && trimmed.all { it == openFence.first() }
    }

    private fun longestBacktickRun(lines: List<String>): Int =
        lines.maxOfOrNull { line -> backtickRun.findAll(line).maxOfOrNull { it.value.length } ?: 0 } ?: 0
}

private class MarkdownBlockBuilder(
    private val document: MarkdownDocument,
    private val linkResolver: ImportLinkResolver,
    private val now: Long,
    private val newBlockId: () -> String
) {

    private val blocks = mutableListOf<NoteBlock>()
    private val pendingAttachments = mutableListOf<PendingAttachment>()
    private val pendingWebImages = mutableListOf<PendingWebImage>()
    private val inlineTags = mutableListOf<String>()

    fun build(root: ASTNode, baseIndentation: Int): ConvertedMarkdown {
        root.children.forEach { node -> convertBlock(node, baseIndentation) }
        return ConvertedMarkdown(blocks.toList(), pendingAttachments.toList(), pendingWebImages.toList(), inlineTags.toList())
    }

    private fun convertBlock(node: ASTNode, indentation: Int) {
        when (node.type) {
            in headingTypes -> addHeading(node, indentation)
            MarkdownElementTypes.PARAGRAPH -> addParagraph(node, indentation)
            MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> addList(node, indentation)
            MarkdownElementTypes.BLOCK_QUOTE, GFMElementTypes.ALERT -> addQuote(node, indentation)
            MarkdownElementTypes.CODE_FENCE -> addCodeFence(node, indentation)
            MarkdownElementTypes.CODE_BLOCK -> addIndentedCode(node, indentation)
            GFMElementTypes.BLOCK_MATH -> addCode(mathSourceOf(node), MATH_CODE_LANGUAGE, indentation)
            MarkdownTokenTypes.HORIZONTAL_RULE -> addDivider(node, indentation)
            GFMElementTypes.TABLE -> addTable(node, indentation)
            MarkdownElementTypes.HTML_BLOCK -> addHtmlBlock(node, indentation)
            MarkdownElementTypes.LINK_DEFINITION -> addFootnoteDefinitionAsText(node, indentation)
            else -> node.children.forEach { child -> convertBlock(child, indentation) }
        }
    }

    private fun addHeading(heading: ASTNode, indentation: Int) {
        val content = heading.children.firstOrNull {
            it.type == MarkdownTokenTypes.ATX_CONTENT || it.type == MarkdownTokenTypes.SETEXT_CONTENT
        } ?: return
        val level = if (heading.type in firstLevelHeadingTypes) 1 else 2
        addInlineParts(inlinePartsOf(content.children), indentation) { styledText, blockIndentation ->
            HeadingBlock(
                id = newBlockId(),
                text = styledText.text,
                level = level,
                indentationLevel = blockIndentation,
                inlineSpans = styledText.spans,
                updatedAt = now
            )
        }
    }

    private fun addParagraph(paragraph: ASTNode, indentation: Int) {
        val onlyChild = paragraph.children
            .filterNot { it.type == MarkdownTokenTypes.WHITE_SPACE || it.type == MarkdownTokenTypes.EOL }
            .singleOrNull()
        if (onlyChild != null && onlyChild.type == GFMElementTypes.BLOCK_MATH) {
            addCode(mathSourceOf(onlyChild), MATH_CODE_LANGUAGE, indentation)
            return
        }
        if (addStandaloneLink(paragraph, onlyChild, indentation)) return
        addInlineParts(inlinePartsOf(paragraph.children), indentation, ::textBlock)
    }

    private fun mathSourceOf(math: ASTNode): String = document.textOf(math).trim().removeSurrounding("\$\$").trim()

    private fun addStandaloneLink(paragraph: ASTNode, onlyChild: ASTNode?, indentation: Int): Boolean {
        val wikiLink = WikiLink.standaloneIn(document.textOf(paragraph))
        if (wikiLink != null) return addStandaloneWikiLink(wikiLink, indentation)

        if (onlyChild == null || onlyChild.type !in linkNodeTypes) return false
        val destination = document.destinationOf(onlyChild) ?: return false

        val webAddress = webAddressOf(destination)
        if (webAddress != null) {
            blocks += bookmarkBlock(webAddress, document.plainTextOf(document.labelNodesOf(onlyChild)), indentation)
            return true
        }
        if (isExternalDestination(destination)) return false

        val noteId = internalPathOf(destination).takeIf { it.isNotEmpty() }?.let(linkResolver::noteIdFor) ?: return false
        blocks += linkedNoteBlock(noteId, indentation)
        return true
    }

    private fun addStandaloneWikiLink(wikiLink: WikiLink, indentation: Int): Boolean {
        if (wikiLink.target.isEmpty()) return false

        val attachmentPath = linkResolver.attachmentPathFor(wikiLink.target)
        if (attachmentPath != null) {
            addAttachment(attachmentPath, indentation)
            return true
        }
        val noteId = linkResolver.noteIdFor(wikiLink.target) ?: return false
        blocks += linkedNoteBlock(noteId, indentation)
        return true
    }

    private fun addList(list: ASTNode, indentation: Int) {
        val isOrdered = list.type == MarkdownElementTypes.ORDERED_LIST
        list.children
            .filter { it.type == MarkdownElementTypes.LIST_ITEM }
            .forEachIndexed { index, item -> addListItem(item, indentation, number = if (isOrdered) index + 1 else null) }
    }

    private fun addListItem(item: ASTNode, indentation: Int, number: Int?) {
        val checkBox = item.findChildOfType(GFMTokenTypes.CHECK_BOX)
        val contentNodes = item.children.filter { it.children.isNotEmpty() }
        val firstParagraph = contentNodes.firstOrNull()?.takeIf { it.type == MarkdownElementTypes.PARAGRAPH }
        val linkedNoteId = if (checkBox == null) firstParagraph?.let(::noteIdOfOnlyLinkIn) else null

        if (linkedNoteId != null) {
            blocks += linkedNoteBlock(linkedNoteId, indentation)
        } else {
            val parts = firstParagraph?.let { inlinePartsOf(it.children) }.orEmpty()
            val itemWords = parts.firstOrNull() as? InlinePart.Words
            if (itemWords == null && checkBox == null && parts.isNotEmpty()) {
                addInlineParts(parts, indentation, ::textBlock)
            } else {
                blocks += listItemBlock(itemWords?.styledText ?: StyledText.EMPTY, checkBox, number, indentation)
                addInlineParts(if (itemWords != null) parts.drop(1) else parts, indentation + 1, ::textBlock)
            }
        }
        val remainingNodes = if (firstParagraph != null) contentNodes.drop(1) else contentNodes
        remainingNodes.forEach { child -> convertBlock(child, indentation + 1) }
    }

    private fun noteIdOfOnlyLinkIn(paragraph: ASTNode): String? {
        val wikiLink = WikiLink.standaloneIn(document.textOf(paragraph))
        if (wikiLink != null) return wikiLink.target.takeIf { it.isNotEmpty() }?.let(linkResolver::noteIdFor)

        val onlyChild = paragraph.children
            .filterNot { it.type == MarkdownTokenTypes.WHITE_SPACE || it.type == MarkdownTokenTypes.EOL }
            .singleOrNull()
            ?.takeIf { it.type in linkNodeTypes }
            ?: return null
        val destination = document.destinationOf(onlyChild)?.takeUnless(::isExternalDestination) ?: return null
        return internalPathOf(destination).takeIf { it.isNotEmpty() }?.let(linkResolver::noteIdFor)
    }

    private fun listItemBlock(styledText: StyledText, checkBox: ASTNode?, number: Int?, indentation: Int): NoteBlock {
        val id = newBlockId()
        return when {
            checkBox != null -> {
                val task = TasksPluginFields.read(styledText)
                CheckboxBlock(
                    id = id,
                    text = task.styledText.text,
                    isChecked = isChecked(checkBox),
                    indentationLevel = indentation,
                    inlineSpans = task.styledText.spans,
                    reminderTimestamp = task.reminderTimestamp,
                    completedAt = task.completedAt,
                    recurrenceRule = task.recurrenceRule,
                    updatedAt = now
                )
            }
            number != null -> NumberedListBlock(
                id = id,
                text = styledText.text,
                number = number,
                indentationLevel = indentation,
                inlineSpans = styledText.spans,
                updatedAt = now
            )
            styledText.text.firstOrNull() == TOGGLE_SYMBOL -> {
                val markerLength = if (styledText.text.getOrNull(1) == ' ') 2 else 1
                val toggleText = styledText.withoutFirstCharacters(markerLength)
                ToggleBlock(
                    id = id,
                    text = toggleText.text,
                    indentationLevel = indentation,
                    inlineSpans = toggleText.spans,
                    updatedAt = now
                )
            }
            else -> BulletedListBlock(
                id = id,
                text = styledText.text,
                indentationLevel = indentation,
                inlineSpans = styledText.spans,
                updatedAt = now
            )
        }
    }

    private fun isChecked(checkBox: ASTNode): Boolean {
        val checkBoxText = document.textOf(checkBox)
        return checkBoxText.length > 1 && checkBoxText[1] != ' '
    }

    private fun addQuote(quote: ASTNode, indentation: Int) {
        val contentNodes = quote.children.filter { it.children.isNotEmpty() }

        val alertMarker = quote.findChildOfType(GFMTokenTypes.ALERT_TITLE)?.let { calloutMarker.find(document.textOf(it).trim()) }
        if (alertMarker != null) {
            addCallout(alertMarker.groupValues[1], foldMarker = "", titleNodes = emptyList(), body = contentNodes.map(::calloutPartOf), indentation)
            return
        }

        val firstParagraph = contentNodes.firstOrNull()?.takeIf { it.type == MarkdownElementTypes.PARAGRAPH }
        val marker = firstParagraph?.let { calloutMarker.find(document.textOf(it)) }
        if (firstParagraph == null || marker == null) {
            contentNodes.forEach { child -> addCalloutPart(calloutPartOf(child), indentation, ::quoteBlock) }
            return
        }

        val firstLineEnd = firstParagraph.children.indexOfFirst { it.type == MarkdownTokenTypes.EOL }
        val titleNodes = if (firstLineEnd == -1) firstParagraph.children else firstParagraph.children.subList(0, firstLineEnd)
        val restOfFirstParagraph = if (firstLineEnd == -1) emptyList() else firstParagraph.children.drop(firstLineEnd + 1)
        val body = listOfNotNull<CalloutPart>(restOfFirstParagraph.takeIf { it.isNotEmpty() }?.let { CalloutPart.Words(it) }) +
            contentNodes.drop(1).map(::calloutPartOf)
        addCallout(marker.groupValues[1], marker.groupValues[2], titleNodes, body, indentation)
    }

    private fun addCallout(writtenType: String, foldMarker: String, titleNodes: List<ASTNode>, body: List<CalloutPart>, indentation: Int) {
        val calloutType = calloutTypesByWrittenName[writtenType.lowercase()] ?: CalloutType.NOTE
        val title = calloutTitle(writtenType, calloutType, titleNodes)
        blocks += CalloutBlock(
            id = newBlockId(),
            text = title.text,
            calloutTypeName = calloutType.name,
            isFoldable = foldMarker.isNotEmpty(),
            isExpanded = foldMarker != "-",
            indentationLevel = indentation,
            inlineSpans = title.spans,
            updatedAt = now
        )
        body.forEach { part -> addCalloutPart(part, indentation + 1, ::textBlock) }
    }

    private fun calloutTitle(writtenType: String, calloutType: CalloutType, titleNodes: List<ASTNode>): StyledText {
        val writtenTitle = (inlinePartsOf(titleNodes).firstOrNull() as? InlinePart.Words)?.styledText
        val markerLength = writtenTitle?.let { calloutMarkerWithSpaces.find(it.text)?.value?.length } ?: 0
        val titleWithoutMarker = writtenTitle?.withoutFirstCharacters(markerLength)
        return when {
            titleWithoutMarker != null && titleWithoutMarker.text.isNotBlank() -> titleWithoutMarker
            writtenType.equals(calloutType.name, ignoreCase = true) -> StyledText.EMPTY
            else -> StyledText(text = writtenType.lowercase().replaceFirstChar { it.titlecase() }, spans = emptyList())
        }
    }

    private fun calloutPartOf(node: ASTNode): CalloutPart =
        if (node.type == MarkdownElementTypes.PARAGRAPH) CalloutPart.Words(node.children) else CalloutPart.Block(node)

    private fun addCalloutPart(part: CalloutPart, indentation: Int, wordsBlock: (StyledText, Int) -> NoteBlock) {
        when (part) {
            is CalloutPart.Words -> addInlineParts(inlinePartsOf(part.nodes), indentation, wordsBlock)
            is CalloutPart.Block -> convertBlock(part.node, indentation)
        }
    }

    private fun addCodeFence(fence: ASTNode, indentation: Int) {
        val language = fence.findChildOfType(MarkdownTokenTypes.FENCE_LANG)
            ?.let { document.textOf(it).trim().substringBefore(' ').lowercase() }
            .orEmpty()
        val children = fence.children.let { if (it.lastOrNull()?.type == MarkdownTokenTypes.CODE_FENCE_END) it.dropLast(1) else it }
        val firstLineEnd = children.indexOfFirst { it.type == MarkdownTokenTypes.EOL }
        val codeNodes = if (firstLineEnd == -1) {
            emptyList()
        } else {
            children.drop(firstLineEnd + 1).filter {
                it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT || it.type == MarkdownTokenTypes.EOL
            }
        }
        val fenceIndent = document.textOf(fence).takeWhile { it == ' ' }.length
        val code = codeNodes.joinToString("") { document.textOf(it) }
            .lines()
            .joinToString("\n") { line -> line.withoutLeadingSpaces(fenceIndent) }
            .trimEnd('\n')
        if (language == BLOCK_COMMENT_FENCE_LANGUAGE) {
            addBlockComment(code, indentation)
        } else {
            addCode(code, language.ifEmpty { DEFAULT_CODE_LANGUAGE }, indentation)
        }
    }

    private fun addBlockComment(commentMarkdown: String, indentation: Int) {
        blocks += ToggleBlock(
            id = newBlockId(),
            text = BLOCK_COMMENT_TITLE,
            isExpanded = false,
            indentationLevel = indentation,
            updatedAt = now
        )
        val comment = MarkdownToBlocks.convert(commentMarkdown, linkResolver, now, newBlockId, baseIndentation = indentation + 1)
        blocks += comment.blocks
        pendingAttachments += comment.pendingAttachments
        pendingWebImages += comment.pendingWebImages
        inlineTags += comment.inlineTags
    }

    private fun addIndentedCode(codeBlock: ASTNode, indentation: Int) {
        val code = codeBlock.children
            .filter { it.type == MarkdownTokenTypes.CODE_LINE || it.type == MarkdownTokenTypes.EOL }
            .joinToString("") { document.textOf(it) }
            .lines()
            .joinToString("\n") { line -> if (line.startsWith('\t')) line.drop(1) else line.withoutLeadingSpaces(CODE_BLOCK_INDENT) }
            .trimEnd('\n')
        addCode(code, DEFAULT_CODE_LANGUAGE, indentation)
    }

    private fun addCode(code: String, language: String, indentation: Int) {
        blocks += CodeBlock(id = newBlockId(), code = code, language = language, indentationLevel = indentation, updatedAt = now)
    }

    private fun addDivider(rule: ASTNode, indentation: Int) {
        blocks += if (document.textOf(rule).trim() == DOT_DIVIDER_TEXT) {
            ThreeDotDividerBlock(id = newBlockId(), indentationLevel = indentation, updatedAt = now)
        } else {
            SolidDividerBlock(id = newBlockId(), indentationLevel = indentation, updatedAt = now)
        }
    }

    private fun addTable(table: ASTNode, indentation: Int) {
        val rows = table.children
            .filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }
            .map { row -> row.children.filter { it.type == GFMTokenTypes.CELL }.map(::cellTextOf) }
        if (rows.isEmpty()) return

        val columnCount = rows.maxOf { it.size }.coerceAtLeast(1)
        val cellSpans = buildMap {
            rows.forEachIndexed { rowIndex, cells ->
                cells.forEachIndexed { columnIndex, cell ->
                    if (cell.spans.isNotEmpty()) put("$rowIndex:$columnIndex", cell.spans)
                }
            }
        }
        blocks += TableBlock(
            id = newBlockId(),
            rows = rows.map { cells -> List(columnCount) { columnIndex -> cells.getOrNull(columnIndex)?.text.orEmpty() } },
            cellSpans = cellSpans,
            indentationLevel = indentation,
            updatedAt = now
        )
    }

    private fun cellTextOf(cell: ASTNode): StyledText {
        val parts = MarkdownInlineConverter(document, linkResolver, inlineTags, keepsMediaAsText = true).convert(cell.children.ifEmpty { listOf(cell) })
        return (parts.singleOrNull() as? InlinePart.Words)?.styledText ?: StyledText.EMPTY
    }

    private fun addFootnoteDefinitionAsText(definition: ASTNode, indentation: Int) {
        val label = definition.findChildOfType(MarkdownElementTypes.LINK_LABEL) ?: return
        if (!isFootnoteLabel(document.textOf(label))) return
        blocks += textBlock(StyledText(text = document.textOf(definition).trim(), spans = emptyList()), indentation)
    }

    private fun addHtmlBlock(htmlBlock: ASTNode, indentation: Int) {
        val html = document.textOf(htmlBlock).replace(htmlCommentPattern, "")
        var textStart = 0
        htmlMediaTag.findAll(html).forEach { tag ->
            addHtmlText(html.substring(textStart, tag.range.first), indentation)
            val media = htmlMediaOf(tag.groupValues[1], tag.groupValues[2], linkResolver)
            val mediaPart = media?.part
            when {
                mediaPart != null -> addInlineParts(listOf(mediaPart), indentation, ::textBlock)
                media != null && media.fallbackText.isNotBlank() ->
                    blocks += textBlock(StyledText(text = media.fallbackText.trim(), spans = emptyList()), indentation)
            }
            textStart = tag.range.last + 1
        }
        addHtmlText(html.substring(textStart), indentation)
    }

    private fun addHtmlText(html: String, indentation: Int) {
        val visibleText = html
            .replace(lineBreakingHtmlTag, "\n")
            .replace(anyHtmlTag, "")
        decodeMarkdownEscapes(visibleText)
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line -> blocks += textBlock(StyledText(text = line, spans = emptyList()), indentation) }
    }

    private fun addInlineParts(parts: List<InlinePart>, indentation: Int, wordsBlock: (StyledText, Int) -> NoteBlock) {
        parts.forEach { part ->
            when (part) {
                is InlinePart.Words -> blocks += wordsBlock(part.styledText, indentation)
                is InlinePart.Attachment -> addAttachment(part.relativePath, indentation)
                is InlinePart.WebImage -> addWebImage(part.url, part.altText, indentation)
                is InlinePart.Embed -> blocks += bookmarkBlock(part.url, labelText = "", indentation)
            }
        }
    }

    private fun addAttachment(relativePath: String, indentation: Int) {
        val id = newBlockId()
        blocks += if (ImportPaths.isImageFile(relativePath)) {
            ImageBlock(id = id, indentationLevel = indentation, updatedAt = now)
        } else {
            DocumentBlock(id = id, fileName = relativePath.substringAfterLast('/'), indentationLevel = indentation, updatedAt = now)
        }
        pendingAttachments += PendingAttachment(blockId = id, relativePath = relativePath)
    }

    private fun addWebImage(url: String, altText: String, indentation: Int) {
        val id = newBlockId()
        blocks += ImageBlock(id = id, indentationLevel = indentation, updatedAt = now)
        pendingWebImages += PendingWebImage(blockId = id, url = url, altText = altText)
    }

    private fun inlinePartsOf(nodes: List<ASTNode>): List<InlinePart> =
        MarkdownInlineConverter(document, linkResolver, inlineTags).convert(nodes)

    private fun textBlock(styledText: StyledText, indentation: Int): NoteBlock = TextBlock(
        id = newBlockId(),
        text = styledText.text,
        indentationLevel = indentation,
        inlineSpans = styledText.spans,
        updatedAt = now
    )

    private fun quoteBlock(styledText: StyledText, indentation: Int): NoteBlock = QuoteBlock(
        id = newBlockId(),
        text = styledText.text,
        indentationLevel = indentation,
        inlineSpans = styledText.spans,
        updatedAt = now
    )

    private fun bookmarkBlock(url: String, labelText: String, indentation: Int): NoteBlock = BookmarkBlock(
        id = newBlockId(),
        url = url,
        title = labelText.takeIf { it.isNotBlank() && !isSameAddress(it, url) },
        indentationLevel = indentation,
        updatedAt = now
    )

    private fun linkedNoteBlock(noteId: String, indentation: Int): NoteBlock =
        LinkedNoteBlock(id = newBlockId(), linkedNoteId = noteId, indentationLevel = indentation, updatedAt = now)

    private fun String.withoutLeadingSpaces(maximumCount: Int): String {
        var removedCount = 0
        while (removedCount < maximumCount && removedCount < length && this[removedCount] == ' ') removedCount++
        return substring(removedCount)
    }
}

private sealed interface CalloutPart {
    class Words(val nodes: List<ASTNode>) : CalloutPart
    class Block(val node: ASTNode) : CalloutPart
}
