package com.emberr.domain.importer.markdown

import com.emberr.domain.model.InlineSpan
import org.intellij.markdown.html.entities.Entities

private const val ESCAPABLE_CHARACTERS = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
private val entityPattern = Regex("&(?:#[xX]([0-9a-fA-F]{1,6})|#([0-9]{1,7})|[a-zA-Z][a-zA-Z0-9]{1,31});")

internal data class CharacterStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strikeThrough: Boolean = false,
    val underline: Boolean = false,
    val highlight: Boolean = false,
    val highlightColorName: String? = null
) {
    val hasFormatting: Boolean
        get() = bold || italic || strikeThrough || underline || highlight

    fun combinedWith(other: CharacterStyle) = CharacterStyle(
        bold = bold || other.bold,
        italic = italic || other.italic,
        strikeThrough = strikeThrough || other.strikeThrough,
        underline = underline || other.underline,
        highlight = highlight || other.highlight,
        highlightColorName = highlightColorName ?: other.highlightColorName
    )
}

internal data class StyledText(val text: String, val spans: List<InlineSpan>, val tags: List<String> = emptyList()) {

    fun withoutFirstCharacters(count: Int): StyledText = StyledText(
        text = text.drop(count),
        spans = spans.mapNotNull { span ->
            val start = (span.start - count).coerceAtLeast(0)
            val end = span.end - count
            if (end <= start) null else span.copy(start = start, end = end)
        },
        tags = tags
    )

    fun withoutCharacterRanges(ranges: List<IntRange>): StyledText {
        val isRemoved = BooleanArray(text.length)
        ranges.forEach { range -> range.forEach { index -> isRemoved[index] = true } }
        val keptCountBefore = IntArray(text.length + 1)
        text.indices.forEach { index -> keptCountBefore[index + 1] = keptCountBefore[index] + if (isRemoved[index]) 0 else 1 }
        return StyledText(
            text = text.filterIndexed { index, _ -> !isRemoved[index] },
            spans = spans.mapNotNull { span ->
                val start = keptCountBefore[span.start]
                val end = keptCountBefore[span.end]
                if (end <= start) null else span.copy(start = start, end = end)
            },
            tags = tags
        )
    }

    companion object {
        val EMPTY = StyledText(text = "", spans = emptyList())
    }
}

private data class StyledCharacter(val character: Char, val style: CharacterStyle, val isLiteral: Boolean) {
    fun isMarker(expected: Char): Boolean = !isLiteral && character == expected
}

internal class StyledTextBuilder {

    private val characters = mutableListOf<StyledCharacter>()

    fun appendMarkdown(rawText: CharSequence, style: CharacterStyle) {
        rawText.forEach { character -> characters += StyledCharacter(character, style, isLiteral = false) }
    }

    fun appendLiteral(text: CharSequence, style: CharacterStyle) {
        text.forEach { character -> characters += StyledCharacter(character, style, isLiteral = true) }
    }

    fun build(): StyledText = finishedStyledTextOf(cleanedCharacters())

    fun buildAroundAttachmentEmbeds(attachmentPathFor: (String) -> String?): List<InlinePart> {
        val cleanedCharacters = cleanedCharacters()
        val parts = mutableListOf<InlinePart>()
        fun addWords(pieceCharacters: List<StyledCharacter>) {
            val styledText = finishedStyledTextOf(pieceCharacters)
            if (styledText.text.isNotEmpty()) parts += InlinePart.Words(styledText)
        }

        var pieceStart = 0
        var index = 0
        while (index < cleanedCharacters.size) {
            val wikiLink = if (cleanedCharacters[index].isMarker('!')) wikiLinkAt(cleanedCharacters, index + 1) else null
            val attachmentPath = wikiLink?.let { attachmentPathFor(WikiLink.fromInnerText(textOf(it.inside), isEmbed = true).target) }
            if (wikiLink != null && attachmentPath != null) {
                addWords(cleanedCharacters.subList(pieceStart, index))
                parts += InlinePart.Attachment(attachmentPath)
                index = wikiLink.endIndex
                pieceStart = index
            } else {
                index++
            }
        }
        addWords(cleanedCharacters.subList(pieceStart, cleanedCharacters.size))
        return parts
    }

    private fun cleanedCharacters(): List<StyledCharacter> = characters
        .let(::decodeEscapesAndEntities)
        .let(::removeSpacesAroundLineBreaks)
        .let(::removeInlineComments)
        .let(::applyDoubleEqualsHighlights)
}

private fun finishedStyledTextOf(cleanedCharacters: List<StyledCharacter>): StyledText {
    val finishedCharacters = cleanedCharacters
        .let(::replaceWikiLinksWithTheirText)
        .let(::withoutSurroundingWhitespace)
        .let(::withoutTrailingBlockId)
    return StyledText(text = textOf(finishedCharacters), spans = spansOf(finishedCharacters), tags = tagsIn(finishedCharacters))
}

internal fun decodeMarkdownEscapes(rawText: String): String {
    val characters = rawText.map { character -> StyledCharacter(character, CharacterStyle(), isLiteral = false) }
    return textOf(decodeEscapesAndEntities(characters))
}

private fun textOf(characters: List<StyledCharacter>): String =
    characters.map { it.character }.toCharArray().concatToString()

private fun decodeEscapesAndEntities(characters: List<StyledCharacter>): List<StyledCharacter> {
    val text = textOf(characters)
    val result = mutableListOf<StyledCharacter>()
    var index = 0
    while (index < characters.size) {
        val current = characters[index]
        val next = characters.getOrNull(index + 1)

        if (current.isMarker('\\') && next != null && !next.isLiteral && next.character in ESCAPABLE_CHARACTERS) {
            result += next.copy(isLiteral = true)
            index += 2
            continue
        }

        if (current.isMarker('&')) {
            val entity = entityPattern.matchAt(text, index)
            val decodedText = entity?.let(::decodeEntity)
            if (entity != null && decodedText != null && characters.subList(index, entity.range.last + 1).none { it.isLiteral }) {
                decodedText.forEach { character -> result += StyledCharacter(character, current.style, isLiteral = true) }
                index = entity.range.last + 1
                continue
            }
        }

        result += current
        index++
    }
    return result
}

private fun decodeEntity(entity: MatchResult): String? {
    val (hexDigits, decimalDigits) = entity.destructured
    val codePoint = when {
        hexDigits.isNotEmpty() -> hexDigits.toIntOrNull(16)
        decimalDigits.isNotEmpty() -> decimalDigits.toIntOrNull()
        else -> Entities.map[entity.value]
    } ?: return null
    return textOfCodePoint(codePoint)
}

private fun textOfCodePoint(codePoint: Int): String? = when {
    codePoint <= 0 || codePoint > 0x10FFFF -> null
    codePoint < 0x10000 -> codePoint.toChar().toString()
    else -> {
        val offset = codePoint - 0x10000
        charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
    }
}

private fun isSpaceOrTab(character: Char): Boolean = character == ' ' || character == '\t'

private fun removeSpacesAroundLineBreaks(characters: List<StyledCharacter>): List<StyledCharacter> {
    val result = mutableListOf<StyledCharacter>()
    for (styledCharacter in characters) {
        when {
            styledCharacter.character == '\n' -> {
                while (result.lastOrNull()?.let { isSpaceOrTab(it.character) } == true) result.removeAt(result.lastIndex)
                result += styledCharacter
            }
            isSpaceOrTab(styledCharacter.character) && result.lastOrNull()?.character == '\n' -> Unit
            else -> result += styledCharacter
        }
    }
    return result
}

private fun isDoubleMarkerAt(characters: List<StyledCharacter>, index: Int, marker: Char): Boolean =
    characters.getOrNull(index)?.isMarker(marker) == true && characters.getOrNull(index + 1)?.isMarker(marker) == true

private fun findDoubleMarker(characters: List<StyledCharacter>, startingAt: Int, marker: Char): Int? =
    (startingAt until characters.size - 1).firstOrNull { index -> isDoubleMarkerAt(characters, index, marker) }

private fun applyDoubleEqualsHighlights(characters: List<StyledCharacter>): List<StyledCharacter> {
    val result = mutableListOf<StyledCharacter>()
    var index = 0
    while (index < characters.size) {
        if (isDoubleMarkerAt(characters, index, '=')) {
            val closingIndex = findDoubleMarker(characters, startingAt = index + 2, marker = '=')
            val content = closingIndex?.let { characters.subList(index + 2, it) }
            if (closingIndex != null && content != null && content.isNotEmpty() &&
                !content.first().character.isWhitespace() && !content.last().character.isWhitespace()
            ) {
                content.forEach { styledCharacter ->
                    result += styledCharacter.copy(style = styledCharacter.style.copy(highlight = true))
                }
                index = closingIndex + 2
                continue
            }
        }
        result += characters[index]
        index++
    }
    return result
}

private class FoundWikiLink(val inside: List<StyledCharacter>, val endIndex: Int)

private fun wikiLinkAt(characters: List<StyledCharacter>, index: Int): FoundWikiLink? {
    if (!isDoubleMarkerAt(characters, index, '[')) return null
    val closingIndex = findDoubleMarker(characters, startingAt = index + 2, marker = ']') ?: return null
    val inside = characters.subList(index + 2, closingIndex)
    if (inside.isEmpty() || inside.any { it.character == '\n' || it.isMarker('[') }) return null
    return FoundWikiLink(inside = inside, endIndex = closingIndex + 2)
}

private fun replaceWikiLinksWithTheirText(characters: List<StyledCharacter>): List<StyledCharacter> {
    val result = mutableListOf<StyledCharacter>()
    var index = 0
    while (index < characters.size) {
        val wikiLink = wikiLinkAt(characters, index)
        if (wikiLink != null) {
            val isEmbed = result.lastOrNull()?.isMarker('!') == true
            if (isEmbed) result.removeAt(result.lastIndex)
            val shownText = WikiLink.fromInnerText(textOf(wikiLink.inside), isEmbed).shownText
            shownText.forEach { character -> result += StyledCharacter(character, wikiLink.inside.first().style, isLiteral = true) }
            index = wikiLink.endIndex
            continue
        }
        result += characters[index]
        index++
    }
    return result
}

private fun removeInlineComments(characters: List<StyledCharacter>): List<StyledCharacter> {
    val result = mutableListOf<StyledCharacter>()
    var index = 0
    while (index < characters.size) {
        if (isDoubleMarkerAt(characters, index, '%')) {
            val closingIndex = findDoubleMarker(characters, startingAt = index + 2, marker = '%')
            if (closingIndex != null) {
                index = closingIndex + 2
                if (result.lastOrNull()?.character == ' ' && characters.getOrNull(index)?.character == ' ') index++
                continue
            }
        }
        result += characters[index]
        index++
    }
    return result
}

private fun withoutSurroundingWhitespace(characters: List<StyledCharacter>): List<StyledCharacter> =
    characters.dropWhile { it.character.isWhitespace() }.dropLastWhile { it.character.isWhitespace() }

private fun isBlockIdCharacter(character: Char): Boolean = character.isLetterOrDigit() || character == '-'

private fun withoutTrailingBlockId(characters: List<StyledCharacter>): List<StyledCharacter> {
    var blockIdStart = characters.size
    while (blockIdStart > 0 && isBlockIdCharacter(characters[blockIdStart - 1].character)) blockIdStart--
    val caretIndex = blockIdStart - 1
    val hasBlockId = blockIdStart < characters.size &&
        caretIndex >= 0 &&
        characters[caretIndex].isMarker('^') &&
        (caretIndex == 0 || characters[caretIndex - 1].character.isWhitespace())
    return if (hasBlockId) withoutSurroundingWhitespace(characters.subList(0, caretIndex)) else characters
}

private fun isTagCharacter(character: Char): Boolean =
    character.isLetterOrDigit() || character == '_' || character == '-' || character == '/'

private fun tagsIn(characters: List<StyledCharacter>): List<String> {
    val tags = mutableListOf<String>()
    var index = 0
    while (index < characters.size) {
        val startsTag = characters[index].isMarker('#') && (index == 0 || characters[index - 1].character.isWhitespace())
        if (!startsTag) {
            index++
            continue
        }
        var tagEnd = index + 1
        while (tagEnd < characters.size && !characters[tagEnd].isLiteral && isTagCharacter(characters[tagEnd].character)) tagEnd++
        val tagName = textOf(characters.subList(index + 1, tagEnd)).trimEnd('/', '-')
        if (tagName.any { !it.isDigit() }) tags += tagName
        index = tagEnd
    }
    return tags
}

private fun spansOf(characters: List<StyledCharacter>): List<InlineSpan> {
    val spans = mutableListOf<InlineSpan>()
    var runStart = 0
    while (runStart < characters.size) {
        val style = characters[runStart].style
        var runEnd = runStart + 1
        while (runEnd < characters.size && characters[runEnd].style == style) runEnd++

        if (style.hasFormatting) {
            spans += InlineSpan(
                start = runStart,
                end = runEnd,
                bold = style.bold,
                italic = style.italic,
                strikeThrough = style.strikeThrough,
                underline = style.underline,
                highlight = style.highlight,
                highlightColorName = style.highlightColorName.takeIf { style.highlight }
            )
        }
        runStart = runEnd
    }
    return spans
}
