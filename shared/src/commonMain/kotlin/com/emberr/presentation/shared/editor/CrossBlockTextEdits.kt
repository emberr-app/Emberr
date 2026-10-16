package com.emberr.presentation.shared.editor

import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.editableTextOrNull
import com.emberr.domain.model.inlineSpansOrEmpty
import com.emberr.domain.model.markDeleted
import com.emberr.domain.model.withEditableText

data class BlockTextPosition(val blockId: String, val offset: Int)

data class CrossBlockTextRange(val start: BlockTextPosition, val end: BlockTextPosition)

data class TextCaret(val position: BlockTextPosition, val blockText: String)

data class BlockEditResult(val blocks: List<NoteBlock>, val caret: TextCaret)

fun orderedTextRange(
    anchor: BlockTextPosition,
    focus: BlockTextPosition,
    blockIdsInOrder: List<String>
): CrossBlockTextRange? {
    val anchorIndex = blockIdsInOrder.indexOf(anchor.blockId)
    val focusIndex = blockIdsInOrder.indexOf(focus.blockId)
    if (anchorIndex < 0 || focusIndex < 0) return null
    val anchorComesFirst = anchorIndex < focusIndex || (anchorIndex == focusIndex && anchor.offset <= focus.offset)
    return if (anchorComesFirst) CrossBlockTextRange(anchor, focus) else CrossBlockTextRange(focus, anchor)
}

fun blockIdsInRange(range: CrossBlockTextRange, blockIdsInOrder: List<String>): List<String> {
    val startIndex = blockIdsInOrder.indexOf(range.start.blockId)
    val endIndex = blockIdsInOrder.indexOf(range.end.blockId)
    if (startIndex < 0 || endIndex < startIndex) return emptyList()
    return blockIdsInOrder.subList(startIndex, endIndex + 1)
}

fun blocksCutToRange(blocksInRange: List<NoteBlock>, range: CrossBlockTextRange): List<NoteBlock> =
    blocksInRange.map { block ->
        val text = block.editableTextOrNull() ?: return@map block
        val from = if (block.id == range.start.blockId) range.start.offset.coerceIn(0, text.length) else 0
        val to = if (block.id == range.end.blockId) range.end.offset.coerceIn(from, text.length) else text.length
        val spans = spansMovedFrom(block.inlineSpansOrEmpty(), from, to, newStart = 0)
        block.withEditableText(text.substring(from, to), spans, block.updatedAt)
    }

fun replaceTextInRange(
    blocks: List<NoteBlock>,
    range: CrossBlockTextRange,
    blockIdsInRange: List<String>,
    replacement: String,
    newBlockId: String,
    now: Long
): BlockEditResult? {
    val firstBlock = blocks.firstOrNull { it.id == range.start.blockId } ?: return null
    val lastBlock = blocks.firstOrNull { it.id == range.end.blockId } ?: return null
    val firstText = firstBlock.editableTextOrNull()
    val lastText = lastBlock.editableTextOrNull()

    val headEnd = firstText?.let { range.start.offset.coerceIn(0, it.length) } ?: 0
    val head = firstText?.substring(0, headEnd).orEmpty()
    val headSpans = if (firstText != null) spansMovedFrom(firstBlock.inlineSpansOrEmpty(), 0, headEnd, newStart = 0) else emptyList()

    val tailStart = lastText?.let { range.end.offset.coerceIn(0, it.length) } ?: 0
    val tail = lastText?.substring(tailStart).orEmpty()
    val tailSpans = if (lastText != null) {
        spansMovedFrom(lastBlock.inlineSpansOrEmpty(), tailStart, lastText.length, newStart = head.length + replacement.length)
    } else {
        emptyList()
    }

    val joinedText = head + replacement + tail
    val keepsNewTextBlock = firstText == null && lastText == null
    val blockToKeep = when {
        firstText != null -> firstBlock.withEditableText(joinedText, headSpans + tailSpans, now)
        lastText != null -> lastBlock.withEditableText(joinedText, tailSpans, now)
        else -> TextBlock(
            id = newBlockId,
            text = replacement,
            indentationLevel = firstBlock.indentationLevel,
            isPinned = firstBlock.isPinned,
            updatedAt = now
        )
    }
    val idsToDelete = blockIdsInRange.toSet() - blockToKeep.id

    val updatedBlocks = blocks.flatMap { block ->
        when {
            block.id == blockToKeep.id -> listOf(blockToKeep)
            block.id == firstBlock.id && keepsNewTextBlock -> listOf(blockToKeep, block.markDeleted())
            block.id in idsToDelete -> listOf(block.markDeleted())
            else -> listOf(block)
        }
    }
    val caret = TextCaret(BlockTextPosition(blockToKeep.id, head.length + replacement.length), joinedText)
    return BlockEditResult(updatedBlocks, caret)
}

internal fun spansMovedFrom(spans: List<InlineSpan>, from: Int, to: Int, newStart: Int): List<InlineSpan> =
    spans.mapNotNull { span ->
        val start = maxOf(span.start, from)
        val end = minOf(span.end, to)
        if (start >= end) null else span.copy(start = start - from + newStart, end = end - from + newStart)
    }

fun textInsertedBetween(oldText: String, newText: String): String {
    val sharedStartLength = oldText.commonPrefixWith(newText).length
    val maxSharedEndLength = minOf(oldText.length, newText.length) - sharedStartLength
    val sharedEndLength = oldText.commonSuffixWith(newText).length.coerceAtMost(maxSharedEndLength)
    return newText.substring(sharedStartLength, newText.length - sharedEndLength)
}
