package com.emberr.domain.importer.markdown

import com.emberr.domain.importer.ImportLinkResolver
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

private val htmlTagPattern = Regex("""^<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>$""")
private val classAttributePattern = Regex("""class\s*=\s*["']([^"']*)["']""")
private val htmlAttributePattern = Regex("""([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+))""")

internal sealed interface InlinePart {
    data class Words(val styledText: StyledText) : InlinePart
    data class Attachment(val relativePath: String) : InlinePart
    data class WebImage(val url: String, val altText: String) : InlinePart
    data class Embed(val url: String) : InlinePart
}

internal class HtmlMedia(val part: InlinePart?, val fallbackText: String)

internal fun htmlMediaOf(tagName: String, tagAttributes: String, linkResolver: ImportLinkResolver): HtmlMedia? {
    val attributes = htmlAttributesOf(tagAttributes)
    val source = attributes["src"]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when (tagName.lowercase()) {
        "img" -> {
            val altText = attributes["alt"].orEmpty().trim()
            val fileName = if (isExternalDestination(source)) "" else internalPathOf(source).substringAfterLast('/')
            HtmlMedia(imagePartOf(source, altText, linkResolver), fallbackText = altText.ifEmpty { fileName })
        }
        "iframe" -> HtmlMedia(webAddressOf(source)?.let(InlinePart::Embed), fallbackText = source)
        else -> null
    }
}

private fun htmlAttributesOf(tagAttributes: String): Map<String, String> =
    htmlAttributePattern.findAll(tagAttributes).associate { match ->
        val (name, doubleQuotedValue, singleQuotedValue, unquotedValue) = match.destructured
        name.lowercase() to decodeMarkdownEscapes(doubleQuotedValue + singleQuotedValue + unquotedValue)
    }

private fun imagePartOf(destination: String, altText: String, linkResolver: ImportLinkResolver): InlinePart? {
    val webAddress = webAddressOf(destination)
    return when {
        webAddress != null -> InlinePart.WebImage(webAddress, altText)
        isExternalDestination(destination) -> null
        else -> attachmentPathOf(destination, linkResolver)?.let(InlinePart::Attachment)
    }
}

private fun attachmentPathOf(destination: String, linkResolver: ImportLinkResolver): String? =
    internalPathOf(destination).takeIf { it.isNotEmpty() }?.let(linkResolver::attachmentPathFor)

internal class MarkdownInlineConverter(
    private val document: MarkdownDocument,
    private val linkResolver: ImportLinkResolver,
    private val inlineTags: MutableCollection<String>,
    private val keepsMediaAsText: Boolean = false
) {

    private val parts = mutableListOf<InlinePart>()
    private var words = StyledTextBuilder()
    private var htmlStyle = CharacterStyle()

    fun convert(nodes: List<ASTNode>): List<InlinePart> {
        nodes.forEach { node -> visit(node, CharacterStyle()) }
        finishWords()
        return parts.toList()
    }

    private fun visit(node: ASTNode, style: CharacterStyle) {
        when (node.type) {
            MarkdownElementTypes.STRONG -> visitAll(node.children.drop(2).dropLast(2), style.copy(bold = true))
            MarkdownElementTypes.EMPH -> visitAll(node.children.drop(1).dropLast(1), style.copy(italic = true))
            GFMElementTypes.STRIKETHROUGH -> visitAll(
                node.children.dropWhile { it.type == GFMTokenTypes.TILDE }.dropLastWhile { it.type == GFMTokenTypes.TILDE },
                style.copy(strikeThrough = true)
            )
            MarkdownElementTypes.CODE_SPAN -> appendCodeSpan(node, style)
            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> visitLink(node, style)
            MarkdownElementTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK -> appendLiteral(document.destinationOf(node).orEmpty(), style)
            MarkdownElementTypes.IMAGE -> visitImage(node, style)
            GFMElementTypes.INLINE_MATH, GFMElementTypes.BLOCK_MATH -> appendLiteral(document.textOf(node), style)
            MarkdownTokenTypes.HTML_TAG -> applyHtmlTag(document.textOf(node), style)
            MarkdownTokenTypes.EOL -> appendLiteral("\n", style)
            MarkdownTokenTypes.HARD_LINE_BREAK, MarkdownTokenTypes.BLOCK_QUOTE -> Unit
            else -> if (node.children.isEmpty()) appendSourceText(node, style) else visitAll(node.children, style)
        }
    }

    private fun visitAll(nodes: List<ASTNode>, style: CharacterStyle) {
        nodes.forEach { node -> visit(node, style) }
    }

    private fun visitLink(linkNode: ASTNode, style: CharacterStyle) {
        val destination = document.destinationOf(linkNode)
        if (destination == null) {
            appendSourceText(linkNode, style)
            return
        }

        val labelNodes = document.labelNodesOf(linkNode)
        if (isExternalDestination(destination)) {
            val labelText = document.plainTextOf(labelNodes)
            if (labelText.isBlank()) {
                appendLiteral(destination, style)
            } else {
                visitAll(labelNodes, style)
                if (!isSameAddress(labelText, destination)) appendLiteral(" ($destination)", style)
            }
            return
        }

        val attachmentPath = attachmentPathOf(destination, linkResolver)
        if (attachmentPath != null) {
            addMedia(InlinePart.Attachment(attachmentPath), document.plainTextOf(labelNodes), style)
        } else {
            visitAll(labelNodes, style)
        }
    }

    private fun visitImage(imageNode: ASTNode, style: CharacterStyle) {
        val linkNode = imageNode.children.firstOrNull { it.type in linkNodeTypes }
        val destination = linkNode?.let(document::destinationOf)
        if (linkNode == null || destination == null) {
            appendSourceText(imageNode, style)
            return
        }

        val altText = document.plainTextOf(document.labelNodesOf(linkNode))
        val imagePart = imagePartOf(destination, altText, linkResolver)
        when {
            imagePart != null -> addMedia(imagePart, altText, style)
            isExternalDestination(destination) -> appendLiteral(altText, style)
            else -> appendSourceText(imageNode, style)
        }
    }

    private fun appendCodeSpan(codeSpan: ASTNode, style: CharacterStyle) {
        val code = codeSpan.children.drop(1).dropLast(1)
            .joinToString("") { child -> document.textOf(child) }
            .replace('\n', ' ')
        val withoutPadding = if (code.length >= 2 && code.startsWith(' ') && code.endsWith(' ') && code.isNotBlank()) {
            code.substring(1, code.length - 1)
        } else {
            code
        }
        appendLiteral(withoutPadding, style)
    }

    private fun applyHtmlTag(tag: String, style: CharacterStyle) {
        val match = htmlTagPattern.matchEntire(tag.trim()) ?: return
        val isOpening = match.groupValues[1].isEmpty()
        when (match.groupValues[2].lowercase()) {
            "br" -> appendLiteral("\n", style)
            "u", "ins" -> htmlStyle = htmlStyle.copy(underline = isOpening)
            "b", "strong" -> htmlStyle = htmlStyle.copy(bold = isOpening)
            "i", "em" -> htmlStyle = htmlStyle.copy(italic = isOpening)
            "s", "del", "strike" -> htmlStyle = htmlStyle.copy(strikeThrough = isOpening)
            "mark" -> htmlStyle = htmlStyle.copy(
                highlight = isOpening,
                highlightColorName = if (isOpening) highlightColorNameIn(match.groupValues[3]) else null
            )
            "img", "iframe" -> addHtmlMedia(match.groupValues[2], match.groupValues[3], style)
        }
    }

    private fun addHtmlMedia(tagName: String, tagAttributes: String, style: CharacterStyle) {
        val media = htmlMediaOf(tagName, tagAttributes, linkResolver) ?: return
        val mediaPart = media.part
        if (mediaPart != null) addMedia(mediaPart, media.fallbackText, style) else appendLiteral(media.fallbackText, style)
    }

    private fun highlightColorNameIn(tagAttributes: String): String? =
        classAttributePattern.find(tagAttributes)?.groupValues?.get(1)?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

    private fun addMedia(part: InlinePart, labelText: String, style: CharacterStyle) {
        if (keepsMediaAsText) {
            appendLiteral(labelText, style)
            return
        }
        finishWords()
        parts += part
    }

    private fun appendSourceText(node: ASTNode, style: CharacterStyle) {
        words.appendMarkdown(document.textOf(node), style.combinedWith(htmlStyle))
    }

    private fun appendLiteral(text: String, style: CharacterStyle) {
        words.appendLiteral(text, style.combinedWith(htmlStyle))
    }

    private fun finishWords() {
        val finishedParts = if (keepsMediaAsText) {
            listOf(InlinePart.Words(words.build())).filter { it.styledText.text.isNotEmpty() }
        } else {
            words.buildAroundAttachmentEmbeds(linkResolver::attachmentPathFor)
        }
        finishedParts.forEach { part -> if (part is InlinePart.Words) inlineTags += part.styledText.tags }
        parts += finishedParts
        words = StyledTextBuilder()
    }
}
