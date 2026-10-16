package com.emberr.presentation.shared.editor.markdown

sealed interface MarkdownShortcut {
    val convertedText: String
    val cursorAfter: Int

    data class TurnInto(val blockType: String, override val convertedText: String) : MarkdownShortcut {
        override val cursorAfter: Int get() = 0
    }

    data object Divider : MarkdownShortcut {
        override val convertedText: String get() = ""
        override val cursorAfter: Int get() = 0
    }

    data class InlineFormat(
        val format: String,
        val formattedStart: Int,
        val formattedEnd: Int,
        override val convertedText: String
    ) : MarkdownShortcut {
        override val cursorAfter: Int get() = formattedEnd
    }
}

private const val DIVIDER_MARKER = "---"
private const val NOTE_LINK_ADDRESS = "emberr://note/"

private val blockTypesByMarker = mapOf(
    "# " to "h1",
    "## " to "h2",
    "- " to "bullet",
    "* " to "bullet",
    "[] " to "checkbox",
    "[ ] " to "checkbox",
    "> " to "quote",
    "```" to "code"
)
private val numberedListMarker = Regex("""\d+\. """)

private val inlineFormatPatterns = listOf(
    "bold" to Regex("""(?<![*\\])\*\*(?=\S)([^*]+?)(?<=\S)\*\*$"""),
    "italic" to Regex("""(?<![*\\])\*(?=[^\s*])([^*]+?)(?<=[^\s*])\*$"""),
    "strike" to Regex("""(?<![~\\])~~(?=\S)([^~]+?)(?<=\S)~~$"""),
    "highlight" to Regex("""(?<![=\\])==(?=\S)([^=]+?)(?<=\S)==$""")
)

fun markdownShortcutAfterTyping(oldText: String, newText: String, cursor: Int): MarkdownShortcut? {
    val typedOneCharacter = newText.length == oldText.length + 1 &&
        cursor in 1..newText.length &&
        newText.removeRange(cursor - 1, cursor) == oldText
    if (!typedOneCharacter) return null

    if (newText == DIVIDER_MARKER) return MarkdownShortcut.Divider

    val textUpToCursor = newText.substring(0, cursor)
    val blockType = blockTypesByMarker[textUpToCursor] ?: "number".takeIf { numberedListMarker.matches(textUpToCursor) }
    if (blockType != null) return MarkdownShortcut.TurnInto(blockType, newText.substring(cursor))

    for ((format, pattern) in inlineFormatPatterns) {
        val match = pattern.find(textUpToCursor) ?: continue
        val formattedText = match.groupValues[1]
        if (NOTE_LINK_ADDRESS in formattedText) continue
        val openingStart = match.range.first
        return MarkdownShortcut.InlineFormat(
            format = format,
            formattedStart = openingStart,
            formattedEnd = openingStart + formattedText.length,
            convertedText = newText.substring(0, openingStart) + formattedText + newText.substring(cursor)
        )
    }
    return null
}
