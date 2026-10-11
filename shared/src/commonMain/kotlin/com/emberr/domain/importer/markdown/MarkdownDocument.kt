package com.emberr.domain.importer.markdown

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.LinkMap

private val urlSchemeAtStart = Regex("^[a-zA-Z][a-zA-Z0-9+.-]+:")
private val standaloneWikiLinkPattern = Regex("""^(!?)\[\[([^\[\]\n]+)]]$""")
private val markupTokenTypes = setOf(
    MarkdownTokenTypes.EMPH,
    MarkdownTokenTypes.BACKTICK,
    MarkdownTokenTypes.HTML_TAG,
    GFMTokenTypes.TILDE
)

internal val linkNodeTypes = setOf(
    MarkdownElementTypes.INLINE_LINK,
    MarkdownElementTypes.FULL_REFERENCE_LINK,
    MarkdownElementTypes.SHORT_REFERENCE_LINK,
    MarkdownElementTypes.AUTOLINK,
    GFMTokenTypes.GFM_AUTOLINK
)

internal class MarkdownDocument(private val text: CharSequence, private val linkMap: LinkMap) {

    fun textOf(node: ASTNode): String = node.getTextInNode(text).toString()

    fun destinationOf(linkNode: ASTNode): String? = when (linkNode.type) {
        MarkdownElementTypes.INLINE_LINK ->
            linkNode.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                ?.let { destination -> cleanInlineDestination(textOf(destination)) }
                ?: ""
        MarkdownElementTypes.FULL_REFERENCE_LINK, MarkdownElementTypes.SHORT_REFERENCE_LINK ->
            linkNode.findChildOfType(MarkdownElementTypes.LINK_LABEL)
                ?.let { label -> textOf(label) }
                ?.takeUnless(::isFootnoteLabel)
                ?.let { label -> linkMap.getLinkInfo(label) }
                ?.destination
                ?.toString()
        MarkdownElementTypes.AUTOLINK -> textOf(linkNode).removeSurrounding("<", ">")
        GFMTokenTypes.GFM_AUTOLINK -> textOf(linkNode)
        else -> null
    }

    fun labelNodesOf(linkNode: ASTNode): List<ASTNode> {
        val labelHolder = linkNode.findChildOfType(MarkdownElementTypes.LINK_TEXT)
            ?: linkNode.findChildOfType(MarkdownElementTypes.LINK_LABEL)
            ?: return emptyList()
        return labelHolder.children.withoutSurroundingBrackets()
    }

    fun plainTextOf(nodes: List<ASTNode>): String {
        val builder = StyledTextBuilder()
        fun appendLeaves(node: ASTNode) {
            if (node.children.isEmpty()) {
                if (node.type !in markupTokenTypes) builder.appendMarkdown(textOf(node), CharacterStyle())
            } else {
                node.children.forEach { child -> appendLeaves(child) }
            }
        }
        nodes.forEach { node -> appendLeaves(node) }
        return builder.build().text
    }

    private fun cleanInlineDestination(rawDestination: String): String {
        val trimmed = rawDestination.trim()
        val withoutAngleBrackets = if (trimmed.startsWith('<') && trimmed.endsWith('>')) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed
        }
        return decodeMarkdownEscapes(withoutAngleBrackets)
    }

    private fun List<ASTNode>.withoutSurroundingBrackets(): List<ASTNode> {
        val withoutOpening = if (firstOrNull()?.type == MarkdownTokenTypes.LBRACKET) drop(1) else this
        return if (withoutOpening.lastOrNull()?.type == MarkdownTokenTypes.RBRACKET) withoutOpening.dropLast(1) else withoutOpening
    }
}

internal class WikiLink(val target: String, private val alias: String?, private val isEmbed: Boolean, private val innerText: String) {

    val shownText: String
        get() = when {
            !isEmbed && alias != null -> alias
            target.isNotEmpty() -> target
            else -> innerText.trim().removePrefix("#")
        }

    companion object {
        fun fromInnerText(innerText: String, isEmbed: Boolean) = WikiLink(
            target = innerText.substringBefore('|').substringBefore('#').trim(),
            alias = innerText.substringAfter('|', missingDelimiterValue = "").trim().takeIf { it.isNotEmpty() },
            isEmbed = isEmbed,
            innerText = innerText
        )

        fun standaloneIn(paragraphText: String): WikiLink? =
            standaloneWikiLinkPattern.matchEntire(paragraphText.trim())?.let { match ->
                fromInnerText(match.groupValues[2], isEmbed = match.groupValues[1] == "!")
            }
    }
}

internal fun isFootnoteLabel(label: String): Boolean = label.startsWith("[^")

internal fun isExternalDestination(destination: String): Boolean =
    destination.startsWith("www.", ignoreCase = true) || urlSchemeAtStart.containsMatchIn(destination)

internal fun webAddressOf(destination: String): String? = when {
    destination.startsWith("http://", ignoreCase = true) || destination.startsWith("https://", ignoreCase = true) -> destination
    destination.startsWith("www.", ignoreCase = true) -> "https://$destination"
    else -> null
}

internal fun isSameAddress(label: String, address: String): Boolean =
    label == address || listOf("https://", "http://", "mailto:").any { prefix -> prefix + label == address }

internal fun internalPathOf(destination: String): String = percentDecode(destination.substringBefore('#')).trim()

internal fun percentDecode(text: String): String {
    if ('%' !in text) return text

    val output = StringBuilder()
    val pendingBytes = mutableListOf<Byte>()
    fun flushPendingBytes() {
        if (pendingBytes.isEmpty()) return
        output.append(pendingBytes.toByteArray().decodeToString())
        pendingBytes.clear()
    }

    var index = 0
    while (index < text.length) {
        val highDigit = if (text[index] == '%') text.getOrNull(index + 1)?.digitToIntOrNull(16) else null
        val lowDigit = if (highDigit != null) text.getOrNull(index + 2)?.digitToIntOrNull(16) else null
        if (highDigit != null && lowDigit != null) {
            pendingBytes += (highDigit * 16 + lowDigit).toByte()
            index += 3
        } else {
            flushPendingBytes()
            output.append(text[index])
            index++
        }
    }
    flushPendingBytes()
    return output.toString()
}
