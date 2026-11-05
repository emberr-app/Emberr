package com.emberr.presentation.shared.editor.text

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.emberr.domain.model.InlineSpan
import com.emberr.presentation.shared.editor.links.HoveredLink
import com.emberr.presentation.shared.editor.links.NOTE_LINK_TAG
import com.emberr.presentation.shared.editor.links.withInteractiveLinksHighlighted
import com.emberr.ui.theme.highlightBackgroundFor
import kotlinx.coroutines.flow.first

private const val NOTE_LINK_MARKER = "](emberr://note/"
internal val NoteLinkRegex = """\[([^\]]+)\]\(emberr://note/([^)]+)\)""".toRegex()

data class RichTextVisualTransformation(
    private val linkColor: Color,
    private val fadedColor: Color,
    private val isDarkTheme: Boolean,
    private val validNoteIds: Set<String>,
    private val inlineSpans: List<InlineSpan> = emptyList(),
    private val hoveredLink: HoveredLink? = null,
    private val noteTitlesById: Map<String, String> = emptyMap()
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text

        if (inlineSpans.isEmpty() && !originalText.contains(NOTE_LINK_MARKER)) {
            return TransformedText(text.withInteractiveLinksHighlighted(hoveredLink), OffsetMapping.Identity)
        }

        val matches = NoteLinkRegex.findAll(originalText).toList()
        if (matches.isEmpty() && inlineSpans.isEmpty()) {
            return TransformedText(text.withInteractiveLinksHighlighted(hoveredLink), OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder()
        val mapping = IntArray(originalText.length * 2 + 50)
        val inverse = IntArray(originalText.length + 1)

        var originalIndex = 0
        var transformedIndex = 0
        data class PendingLink(val noteId: String, val start: Int, val end: Int, val isMissing: Boolean)
        val pendingLinks = mutableListOf<PendingLink>()

        for (match in matches) {
            while (originalIndex < match.range.first) {
                mapping[transformedIndex] = originalIndex
                inverse[originalIndex] = transformedIndex
                builder.append(originalText[originalIndex])
                originalIndex++
                transformedIndex++
            }

            val noteId = match.groupValues[2]
            val title = noteTitlesById[noteId]?.ifEmpty { "Untitled" } ?: match.groupValues[1]
            val linkText = "@$title"
            val isMissing = !validNoteIds.contains(noteId)

            val linkStartTransformed = transformedIndex
            builder.append(linkText)
            pendingLinks.add(PendingLink(noteId, linkStartTransformed, transformedIndex + linkText.length, isMissing))

            for (i in linkText.indices) {
                mapping[transformedIndex] = match.range.last + 1
                transformedIndex++
            }

            for (i in match.range) {
                inverse[i] = linkStartTransformed
            }
            originalIndex = match.range.last + 1
        }

        while (originalIndex < originalText.length) {
            mapping[transformedIndex] = originalIndex
            inverse[originalIndex] = transformedIndex
            builder.append(originalText[originalIndex])
            originalIndex++
            transformedIndex++
        }

        val finalTransformedLength = transformedIndex
        val finalOriginalLength = originalText.length

        mapping[finalTransformedLength] = finalOriginalLength
        inverse[finalOriginalLength] = finalTransformedLength

        pendingLinks.forEach { link ->
            val finalColor = if (link.isMissing) fadedColor else linkColor
            val decoration = if (link.isMissing) TextDecoration.LineThrough else TextDecoration.None
            builder.addStyle(SpanStyle(color = finalColor, fontWeight = FontWeight.SemiBold, textDecoration = decoration), link.start, link.end)
            builder.addStringAnnotation(tag = NOTE_LINK_TAG, annotation = link.noteId, start = link.start, end = link.end)
        }

        inlineSpans.forEach { span ->
            val start = span.start.coerceIn(0, finalOriginalLength)
            val end = span.end.coerceIn(0, finalOriginalLength)
            if (start >= end) return@forEach
            val transformedStart = inverse[start]
            val transformedEnd = inverse[end]
            if (transformedStart >= transformedEnd) return@forEach
            val decoration = when {
                span.strikeThrough && span.underline -> TextDecoration.LineThrough + TextDecoration.Underline
                span.strikeThrough -> TextDecoration.LineThrough
                span.underline -> TextDecoration.Underline
                else -> null
            }
            builder.addStyle(
                SpanStyle(
                    fontWeight = if (span.bold) FontWeight.Bold else null,
                    fontStyle = if (span.italic) FontStyle.Italic else null,
                    textDecoration = decoration,
                    background = if (span.highlight) {
                        highlightBackgroundFor(span.highlightColorName, isDarkTheme)
                    } else Color.Unspecified
                ),
                transformedStart,
                transformedEnd
            )
        }

        return TransformedText(
            builder.toAnnotatedString().withInteractiveLinksHighlighted(hoveredLink),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0
                    if (offset >= finalOriginalLength) return finalTransformedLength
                    return inverse[offset]
                }
                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 0) return 0
                    if (offset >= finalTransformedLength) return finalOriginalLength
                    return mapping[offset]
                }
            }
        )
    }
}
