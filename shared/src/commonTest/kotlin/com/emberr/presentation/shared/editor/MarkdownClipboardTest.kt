package com.emberr.presentation.shared.editor

import com.emberr.domain.importer.ImportLinkResolver
import com.emberr.domain.importer.markdown.MarkdownToBlocks
import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.TextBlock
import com.emberr.domain.util.export.NoteMarkdownWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkdownClipboardTest {

    @Test
    fun pastingThreeCopiedBlocksGivesThreeBlocksAndKeepsNoteMentions() {
        val copied = NoteMarkdownWriter.writeMarkdown(
            listOf(
                TextBlock(id = "a", text = "First"),
                TextBlock(id = "b", text = "See [Plan](emberr://note/plan-id)"),
                TextBlock(id = "c", text = "Third")
            )
        )
        val noLinks = object : ImportLinkResolver {
            override fun noteIdFor(linkTarget: String): String? = null
            override fun attachmentPathFor(linkTarget: String): String? = null
        }
        var lastId = 0
        val pasted = MarkdownToBlocks.convert(copied, noLinks, now = 1L, newBlockId = { "pasted-${++lastId}" }, keepsEmberrNoteLinks = true).blocks

        val result = pasteBlocksIntoBlock(
            blocks = listOf(TextBlock(id = "target", text = "")),
            blockId = "target",
            replacedStart = 0,
            replacedEnd = 0,
            pastedBlocks = pasted,
            keepsHeadSeparate = false,
            keepsTailSeparate = false,
            newBlockId = { "new-${++lastId}" },
            now = 1L
        )!!

        assertEquals(
            listOf("First", "See [Plan](emberr://note/plan-id)", "Third"),
            result.blocks.filterNot { it.isDeleted }.map { (it as TextBlock).text }
        )
    }

    @Test
    fun aSingleLineCopiedFromEmberrIsReadAsMarkdown() {
        val copied = inlineMarkdownOf(TextBlock(id = "a", text = "very bold", inlineSpans = listOf(InlineSpan(start = 5, end = 9, bold = true))), 0, 9)

        assertEquals(PastedText(3, 3, copied), pastedTextToReadAsMarkdown("abc", "abc$copied", lastCopiedMarkdown = copied))
        assertEquals(null, pastedTextToReadAsMarkdown("abc", "abc plain words", lastCopiedMarkdown = copied))
    }

    @Test
    fun aSingleLineFromAnotherAppIsReadAsMarkdownOnlyWhenItsFormattingIsClear() {
        assertTrue(readsAsMarkdown("The **butler** did it.", lastCopiedMarkdown = null))
        assertTrue(readsAsMarkdown("an *odd* clue", lastCopiedMarkdown = null))
        assertFalse(readsAsMarkdown("2*3*4", lastCopiedMarkdown = null))
        assertFalse(readsAsMarkdown("rename my_var_name", lastCopiedMarkdown = null))
    }
}
