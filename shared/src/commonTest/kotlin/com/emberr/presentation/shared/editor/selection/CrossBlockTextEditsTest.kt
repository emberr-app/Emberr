package com.emberr.presentation.shared.editor.selection

import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.TextBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrossBlockTextEditsTest {

    @Test
    fun deletingAcrossBlocksJoinsTheTailOntoTheFirstBlockAndKeepsBothBlocksFormatting() {
        val blocks = listOf(
            TextBlock(id = "first", text = "Hello world", inlineSpans = listOf(InlineSpan(start = 0, end = 5, bold = true))),
            ImageBlock(id = "middle"),
            TextBlock(id = "last", text = "Goodbye moon", inlineSpans = listOf(InlineSpan(start = 8, end = 12, italic = true)))
        )
        val range = CrossBlockTextRange(BlockTextPosition("first", 6), BlockTextPosition("last", 8))

        val result = replaceTextInRange(blocks, range, listOf("first", "middle", "last"), "", newBlockId = "unused", now = 1L)!!

        val first = result.blocks.first { it.id == "first" } as TextBlock
        assertEquals("Hello moon", first.text)
        assertEquals(
            listOf(InlineSpan(start = 0, end = 5, bold = true), InlineSpan(start = 6, end = 10, italic = true)),
            first.inlineSpans
        )
        assertTrue(result.blocks.first { it.id == "middle" }.isDeleted)
        assertTrue(result.blocks.first { it.id == "last" }.isDeleted)
        assertEquals(TextCaret(BlockTextPosition("first", 6), "Hello moon"), result.caret)
    }

    @Test
    fun typingOverASelectionThatStartsOnAnImageKeepsTheRestOfTheLastBlock() {
        val blocks = listOf(
            ImageBlock(id = "image"),
            TextBlock(id = "text", text = "abcdef")
        )
        val range = CrossBlockTextRange(BlockTextPosition("image", 0), BlockTextPosition("text", 3))

        val result = replaceTextInRange(blocks, range, listOf("image", "text"), "X", newBlockId = "unused", now = 1L)!!

        assertTrue(result.blocks.first { it.id == "image" }.isDeleted)
        assertEquals("Xdef", (result.blocks.first { it.id == "text" } as TextBlock).text)
        assertEquals(TextCaret(BlockTextPosition("text", 1), "Xdef"), result.caret)
    }
}
