package com.emberr.presentation.shared.editor

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.editableTextOrNull
import com.emberr.domain.model.highlightColorNameOrNull
import com.emberr.domain.model.inlineSpansOrEmpty
import com.emberr.domain.model.markDeleted
import com.emberr.domain.model.withEditableText
import com.emberr.domain.model.withId
import com.emberr.domain.util.export.InlineMarkdownFormatter

class MarkdownClipboardActions {
    var copySelection: (() -> Boolean)? = null
    var cutSelection: (() -> Boolean)? = null
}

fun ClipboardManager.copyEmberrMarkdown(markdown: String) {
    GlobalEditorState.lastCopiedMarkdown = markdown
    setText(AnnotatedString(markdown))
}

data class PastedText(val replacedStart: Int, val replacedEnd: Int, val text: String)

fun pastedTextToReadAsMarkdown(oldText: String, newText: String, lastCopiedMarkdown: String?): PastedText? {
    val prefixLength = oldText.commonPrefixWith(newText).length
    val longestPossibleSuffix = minOf(oldText.length, newText.length) - prefixLength
    var suffixLength = 0
    while (suffixLength < longestPossibleSuffix && oldText[oldText.length - 1 - suffixLength] == newText[newText.length - 1 - suffixLength]) {
        suffixLength++
    }
    val insertedText = newText.substring(prefixLength, newText.length - suffixLength)
    if (!readsAsMarkdown(insertedText, lastCopiedMarkdown)) return null
    return PastedText(prefixLength, oldText.length - suffixLength, insertedText)
}

fun readsAsMarkdown(pastedText: String, lastCopiedMarkdown: String?): Boolean {
    val spansSeveralLines = '\n' in pastedText.trimEnd('\r', '\n')
    val isCopiedFromEmberr = pastedText.length > 1 && pastedText.replace("\r", "") == lastCopiedMarkdown?.replace("\r", "")
    return spansSeveralLines || isCopiedFromEmberr
}

fun startsOnNewLine(pastedText: String): Boolean = pastedText.trimStart(' ', '\t', '\r').startsWith('\n')

fun endsOnNewLine(pastedText: String): Boolean = pastedText.trimEnd(' ', '\t', '\r').endsWith('\n')

fun inlineMarkdownOf(block: NoteBlock, start: Int, end: Int): String {
    val text = block.editableTextOrNull() ?: return ""
    val from = start.coerceIn(0, text.length)
    val to = end.coerceIn(from, text.length)
    return InlineMarkdownFormatter.toMarkdown(
        text = text.substring(from, to),
        spans = spansMovedFrom(block.inlineSpansOrEmpty(), from, to, newStart = 0),
        isWholeBlockBold = block.isBold,
        isWholeBlockItalic = block.isItalic,
        isWholeBlockStrikeThrough = block.isStrikeThrough,
        isWholeBlockUnderlined = block.isUnderlined,
        isWholeBlockHighlighted = block.isHighlighted,
        wholeBlockHighlightColorName = block.highlightColorNameOrNull()
    )
}

fun pasteBlocksIntoBlock(
    blocks: List<NoteBlock>,
    blockId: String,
    replacedStart: Int,
    replacedEnd: Int,
    pastedBlocks: List<NoteBlock>,
    keepsHeadSeparate: Boolean,
    keepsTailSeparate: Boolean,
    newBlockId: () -> String,
    now: Long
): BlockEditResult? {
    val target = blocks.firstOrNull { it.id == blockId } ?: return null
    val text = target.editableTextOrNull() ?: return null
    val pasted = pastedBlocks.flatMap { it.splitAtLineBreaks(newBlockId, now) }.toMutableList()
    if (pasted.isEmpty()) return null

    val start = replacedStart.coerceIn(0, text.length)
    val end = replacedEnd.coerceIn(start, text.length)
    val targetSpans = target.inlineSpansOrEmpty()
    val head = text.substring(0, start)
    val headSpans = spansMovedFrom(targetSpans, 0, start, newStart = 0)
    val tail = text.substring(end)
    val tailSpans = spansMovedFrom(targetSpans, end, text.length, newStart = 0)

    val firstPasted = pasted.first()
    val headBlock = when {
        keepsHeadSeparate -> target.withEditableText(head, headSpans, now)
        firstPasted is TextBlock -> {
            pasted.removeAt(0)
            val firstPastedSpans = spansMovedFrom(firstPasted.inlineSpans, 0, firstPasted.text.length, newStart = head.length)
            target.withEditableText(head + firstPasted.text, headSpans + firstPastedSpans, now)
        }
        head.isEmpty() && target is TextBlock -> null
        else -> target.withEditableText(head, headSpans, now)
    }

    val pieces = (listOfNotNull(headBlock) + pasted).toMutableList()
    val lastPiece = pieces.last()
    val lastPieceText = lastPiece.editableTextOrNull()
    val caret = if (lastPieceText != null && lastPiece !is CodeBlock && !keepsTailSeparate) {
        val movedTailSpans = spansMovedFrom(tailSpans, 0, tail.length, newStart = lastPieceText.length)
        val joinedText = lastPieceText + tail
        pieces[pieces.lastIndex] = lastPiece.withEditableText(joinedText, lastPiece.inlineSpansOrEmpty() + movedTailSpans, now)
        TextCaret(BlockTextPosition(lastPiece.id, lastPieceText.length), joinedText)
    } else {
        val tailBlock = TextBlock(
            id = newBlockId(),
            text = tail,
            inlineSpans = tailSpans,
            indentationLevel = target.indentationLevel,
            updatedAt = now
        )
        pieces += tailBlock
        TextCaret(BlockTextPosition(tailBlock.id, 0), tail)
    }

    val replacementForTarget = if (headBlock == null) listOf(target.markDeleted()) + pieces else pieces
    val updatedBlocks = blocks.flatMap { block -> if (block.id == blockId) replacementForTarget else listOf(block) }
    return BlockEditResult(updatedBlocks, caret)
}

private fun NoteBlock.splitAtLineBreaks(newBlockId: () -> String, now: Long): List<NoteBlock> {
    val text = editableTextOrNull()
    if (text == null || this is CodeBlock || '\n' !in text) return listOf(this)
    var lineStart = 0
    return text.split('\n').mapIndexed { index, line ->
        val from = lineStart
        lineStart += line.length + 1
        val piece = withEditableText(line, spansMovedFrom(inlineSpansOrEmpty(), from, from + line.length, newStart = 0), now)
        if (index == 0) piece else piece.withId(newBlockId())
    }
}
