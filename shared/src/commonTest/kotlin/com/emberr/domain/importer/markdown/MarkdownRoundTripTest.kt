package com.emberr.domain.importer.markdown

import com.emberr.domain.importer.ImportLinkResolver
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.QuoteBlock
import com.emberr.domain.model.SolidDividerBlock
import com.emberr.domain.model.TableBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ThreeDotDividerBlock
import com.emberr.domain.model.ToggleBlock
import com.emberr.domain.util.export.NoteMarkdownWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownRoundTripTest {

    private object NoImportedLinks : ImportLinkResolver {
        override fun noteIdFor(linkTarget: String): String? = null
        override fun attachmentPathFor(linkTarget: String): String? = null
    }

    @Test
    fun markdownExportedByEmberrComesBackUnchanged() {
        val blocks = listOf(
            HeadingBlock(id = "heading", text = "Plan", level = 2),
            TextBlock(id = "mixed", text = "Mixed bold text", inlineSpans = listOf(InlineSpan(start = 6, end = 10, bold = true))),
            TextBlock(id = "underlined", text = "Whole line underlined", isUnderlined = true),
            TextBlock(id = "colored", text = "warm", inlineSpans = listOf(InlineSpan(start = 0, end = 4, highlight = true, highlightColorName = "red"))),
            TextBlock(id = "symbols", text = "Stars * and == stay plain"),
            BulletedListBlock(id = "bullet", text = "Bullet"),
            BulletedListBlock(id = "nested", text = "Nested", indentationLevel = 1),
            NumberedListBlock(id = "first", text = "First", number = 1),
            NumberedListBlock(id = "second", text = "Second", number = 2),
            CheckboxBlock(id = "task", text = "Done", isChecked = true),
            ToggleBlock(id = "toggle", text = "Toggle"),
            QuoteBlock(id = "quote", text = "Quoted\nlines"),
            CodeBlock(id = "code", code = "fun main() {}\n    return", language = "kotlin"),
            SolidDividerBlock(id = "solid"),
            ThreeDotDividerBlock(id = "dots"),
            TableBlock(id = "table", rows = listOf(listOf("Name", "Note"), listOf("Ann", "a|b"))),
            CalloutBlock(id = "callout", text = "Remember", calloutTypeName = "TIP", isFoldable = true, isExpanded = false),
            TextBlock(id = "inside", text = "Inside the callout", indentationLevel = 1)
        )

        val exportedMarkdown = NoteMarkdownWriter.writeMarkdown(blocks)
        val importedBlocks = ImportedNoteConverter.convert("Round trip.md", exportedMarkdown, NoImportedLinks, now = 1L).blocks

        assertEquals(exportedMarkdown, NoteMarkdownWriter.writeMarkdown(importedBlocks))
    }
}
