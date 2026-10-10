package com.emberr.domain.importer.markdown

import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.QuoteBlock
import com.emberr.domain.model.SolidDividerBlock
import com.emberr.domain.model.TableBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ThreeDotDividerBlock
import com.emberr.domain.model.ToggleBlock
import com.emberr.domain.model.withId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val NOW = 1_700_000_000_000L

class ImportedNoteConverterTest {

    private class FakeLinkResolver(
        private val noteIdsByTarget: Map<String, String> = emptyMap(),
        private val attachmentPathsByTarget: Map<String, String> = emptyMap()
    ) : ImportLinkResolver {
        override fun noteIdFor(linkTarget: String): String? = noteIdsByTarget[linkTarget]
        override fun attachmentPathFor(linkTarget: String): String? = attachmentPathsByTarget[linkTarget]
    }

    private fun convert(
        markdown: String,
        relativePath: String = "Notes/Example.md",
        linkResolver: ImportLinkResolver = FakeLinkResolver()
    ): ConvertedNote {
        var nextBlockNumber = 0
        return ImportedNoteConverter.convert(relativePath, markdown, linkResolver, NOW) { "block-${++nextBlockNumber}" }
    }

    private fun blocksOf(markdown: String, linkResolver: ImportLinkResolver = FakeLinkResolver()): List<NoteBlock> =
        convert(markdown, linkResolver = linkResolver).blocks.map { block -> block.withId("") }

    @Test
    fun theFileNameBecomesTheTitle() {
        assertEquals("Example", convert("Body").title)
    }

    @Test
    fun aFirstHeadingThatRepeatsTheTitleIsDropped() {
        assertEquals(
            listOf(TextBlock(id = "", text = "Body", updatedAt = NOW)),
            convert("# Example\n\nBody").blocks.map { it.withId("") }
        )
    }

    @Test
    fun aFirstHeadingWithADifferentTextIsKept() {
        assertEquals(
            HeadingBlock(id = "", text = "Something else", level = 1, updatedAt = NOW),
            convert("# Something else\n\nBody").blocks.first().withId("")
        )
    }

    @Test
    fun frontMatterIsLeftOut() {
        assertEquals(
            listOf(TextBlock(id = "", text = "Body", updatedAt = NOW)),
            blocksOf("---\ntitle: Example\ntags: [work]\n---\nBody")
        )
    }

    @Test
    fun theFirstHeadingLevelStaysAndDeeperLevelsBecomeTheSecond() {
        assertEquals(
            listOf(
                HeadingBlock(id = "", text = "One", level = 1, updatedAt = NOW),
                HeadingBlock(id = "", text = "Two", level = 2, updatedAt = NOW),
                HeadingBlock(id = "", text = "Three", level = 2, updatedAt = NOW)
            ),
            blocksOf("# One\n\n## Two\n\n### Three")
        )
    }

    @Test
    fun linesOfOneParagraphStayInOneBlock() {
        assertEquals(
            listOf(TextBlock(id = "", text = "line one\nline two", updatedAt = NOW)),
            blocksOf("line one\nline two")
        )
    }

    @Test
    fun inlineFormattingBecomesSpans() {
        val block = blocksOf("Plain **bold** *italic* ~~gone~~ ==marked== <u>under</u> `code`").single() as TextBlock

        assertEquals("Plain bold italic gone marked under code", block.text)
        assertEquals(
            listOf(
                InlineSpan(start = 6, end = 10, bold = true),
                InlineSpan(start = 11, end = 17, italic = true),
                InlineSpan(start = 18, end = 22, strikeThrough = true),
                InlineSpan(start = 23, end = 29, highlight = true),
                InlineSpan(start = 30, end = 35, underline = true)
            ),
            block.inlineSpans
        )
    }

    @Test
    fun aColoredHighlightKeepsItsColor() {
        val block = blocksOf("<mark class=\"red\">warm</mark>").single() as TextBlock

        assertEquals(listOf(InlineSpan(start = 0, end = 4, highlight = true, highlightColorName = "red")), block.inlineSpans)
    }

    @Test
    fun escapedMarkersStayAsPlainCharacters() {
        val block = blocksOf("\\*not italic\\* and \\=\\=not marked\\=\\=").single() as TextBlock

        assertEquals("*not italic* and ==not marked==", block.text)
        assertEquals(emptyList(), block.inlineSpans)
    }

    @Test
    fun listsKeepTheirTypeAndNesting() {
        assertEquals(
            listOf(
                BulletedListBlock(id = "", text = "one", updatedAt = NOW),
                BulletedListBlock(id = "", text = "nested", indentationLevel = 1, updatedAt = NOW),
                NumberedListBlock(id = "", text = "first", number = 1, updatedAt = NOW),
                NumberedListBlock(id = "", text = "second", number = 2, updatedAt = NOW)
            ),
            blocksOf("- one\n  - nested\n\n1. first\n2. second")
        )
    }

    @Test
    fun taskListItemsBecomeCheckboxes() {
        assertEquals(
            listOf(
                CheckboxBlock(id = "", text = "todo", isChecked = false, updatedAt = NOW),
                CheckboxBlock(id = "", text = "done", isChecked = true, updatedAt = NOW)
            ),
            blocksOf("- [ ] todo\n- [x] done")
        )
    }

    @Test
    fun emberrToggleItemsComeBackAsToggles() {
        assertEquals(
            listOf(
                ToggleBlock(id = "", text = "Details", updatedAt = NOW),
                BulletedListBlock(id = "", text = "hidden", indentationLevel = 1, updatedAt = NOW)
            ),
            blocksOf("- ▸ Details\n  - hidden")
        )
    }

    @Test
    fun quotedLinesBecomeOneQuote() {
        assertEquals(listOf(QuoteBlock(id = "", text = "quoted\nmore", updatedAt = NOW)), blocksOf("> quoted\n> more"))
    }

    @Test
    fun fencedCodeKeepsItsLanguageAndIndentation() {
        assertEquals(
            listOf(CodeBlock(id = "", code = "val x = 1\n  indented", language = "kotlin", updatedAt = NOW)),
            blocksOf("```kotlin\nval x = 1\n  indented\n```")
        )
    }

    @Test
    fun fencedCodeWithoutALanguageIsPlainText() {
        assertEquals(
            listOf(CodeBlock(id = "", code = "echo hi", language = "plaintext", updatedAt = NOW)),
            blocksOf("```\necho hi\n```")
        )
    }

    @Test
    fun dividersKeepTheirStyle() {
        assertEquals(
            listOf(
                TextBlock(id = "", text = "Above", updatedAt = NOW),
                SolidDividerBlock(id = "", updatedAt = NOW),
                TextBlock(id = "", text = "Middle", updatedAt = NOW),
                ThreeDotDividerBlock(id = "", updatedAt = NOW),
                TextBlock(id = "", text = "Below", updatedAt = NOW)
            ),
            blocksOf("Above\n\n---\n\nMiddle\n\n* * *\n\nBelow")
        )
    }

    @Test
    fun tablesKeepTheirCellsAndCellFormatting() {
        assertEquals(
            listOf(
                TableBlock(
                    id = "",
                    rows = listOf(listOf("Name", "Notes"), listOf("Ann", "a|b")),
                    cellSpans = mapOf("1:0" to listOf(InlineSpan(start = 0, end = 3, bold = true))),
                    updatedAt = NOW
                )
            ),
            blocksOf("| Name | Notes |\n| --- | --- |\n| **Ann** | a\\|b |")
        )
    }

    @Test
    fun aLineThatIsOnlyAWebLinkBecomesABookmark() {
        assertEquals(
            listOf(
                BookmarkBlock(id = "", url = "https://example.com", title = "Example site", updatedAt = NOW),
                BookmarkBlock(id = "", url = "https://example.org", updatedAt = NOW)
            ),
            blocksOf("[Example site](https://example.com)\n\nhttps://example.org")
        )
    }

    @Test
    fun aWebLinkInsideASentenceKeepsItsAddressInBrackets() {
        assertEquals(
            listOf(TextBlock(id = "", text = "Read the docs (https://example.com/docs) today", updatedAt = NOW)),
            blocksOf("Read [the docs](https://example.com/docs) today")
        )
    }

    @Test
    fun aLineThatIsOnlyALinkToAnImportedNoteBecomesALinkedNote() {
        val linkResolver = FakeLinkResolver(noteIdsByTarget = mapOf("Other note.md" to "note-42", "Other note" to "note-42"))

        assertEquals(
            listOf(
                LinkedNoteBlock(id = "", linkedNoteId = "note-42", updatedAt = NOW),
                LinkedNoteBlock(id = "", linkedNoteId = "note-42", updatedAt = NOW)
            ),
            blocksOf("[Other note](Other%20note.md)\n\n[[Other note]]", linkResolver)
        )
    }

    @Test
    fun percentEncodedLinkPathsAreDecodedBeforeTheyAreLookedUp() {
        val linkResolver = FakeLinkResolver(noteIdsByTarget = mapOf("Café menu.md" to "note-7"))

        assertEquals(
            listOf(LinkedNoteBlock(id = "", linkedNoteId = "note-7", updatedAt = NOW)),
            blocksOf("[Menu](Caf%C3%A9%20menu.md)", linkResolver)
        )
    }

    @Test
    fun htmlEntitiesBecomeTheirCharacters() {
        assertEquals(listOf(TextBlock(id = "", text = "Fish & chips ©", updatedAt = NOW)), blocksOf("Fish &amp; chips &#169;"))
    }

    @Test
    fun aWikiLinkInsideASentenceShowsItsText() {
        assertEquals(
            listOf(TextBlock(id = "", text = "See the other one and Plain target now", updatedAt = NOW)),
            blocksOf("See [[Other note|the other one]] and [[Plain target]] now")
        )
    }

    @Test
    fun anImportedImageBecomesAnImageBlockWaitingForItsFile() {
        val linkResolver = FakeLinkResolver(attachmentPathsByTarget = mapOf("images/chart.png" to "Notes/images/chart.png"))
        val converted = convert("Before\n\n![chart](images/chart.png)\n\nAfter", linkResolver = linkResolver)

        val image = converted.blocks[1]
        assertTrue(image is ImageBlock)
        assertEquals(listOf(PendingAttachment(blockId = image.id, relativePath = "Notes/images/chart.png")), converted.pendingAttachments)
        assertEquals(listOf("Before", "After"), converted.blocks.filterIsInstance<TextBlock>().map { it.text })
    }

    @Test
    fun anImageInsideASentenceSplitsTheSentence() {
        val linkResolver = FakeLinkResolver(attachmentPathsByTarget = mapOf("a.png" to "Notes/a.png"))

        assertEquals(
            listOf(
                TextBlock(id = "", text = "Look", updatedAt = NOW),
                ImageBlock(id = "", updatedAt = NOW),
                TextBlock(id = "", text = "here", updatedAt = NOW)
            ),
            blocksOf("Look ![](a.png) here", linkResolver)
        )
    }

    @Test
    fun aLinkToAnImportedFileBecomesADocumentBlock() {
        val linkResolver = FakeLinkResolver(attachmentPathsByTarget = mapOf("files/report.pdf" to "Notes/files/report.pdf"))
        val converted = convert("[Report](files/report.pdf)", linkResolver = linkResolver)

        val document = converted.blocks.single() as DocumentBlock
        assertEquals("report.pdf", document.fileName)
        assertEquals(listOf(PendingAttachment(blockId = document.id, relativePath = "Notes/files/report.pdf")), converted.pendingAttachments)
    }

    @Test
    fun anImageFromTheWebBecomesABookmark() {
        assertEquals(
            listOf(BookmarkBlock(id = "", url = "https://example.com/logo.png", title = "logo", updatedAt = NOW)),
            blocksOf("![logo](https://example.com/logo.png)")
        )
    }

    @Test
    fun anImageThatIsNotInTheImportKeepsItsOriginalText() {
        assertEquals(listOf(TextBlock(id = "", text = "![](missing.png)", updatedAt = NOW)), blocksOf("![](missing.png)"))
    }

    @Test
    fun htmlBlocksKeepOnlyTheirText() {
        assertEquals(
            listOf(
                TextBlock(id = "", text = "More", updatedAt = NOW),
                TextBlock(id = "", text = "Hidden text", updatedAt = NOW)
            ),
            blocksOf("<details>\n<summary>More</summary>\nHidden text\n</details>")
        )
    }

    @Test
    fun aTextFileBecomesOneTextBlockPerLine() {
        val converted = convert("\nfirst line\n\nsecond line\n\n", relativePath = "journal.txt")

        assertEquals("journal", converted.title)
        assertEquals(
            listOf(
                TextBlock(id = "", text = "first line", updatedAt = NOW),
                TextBlock(id = "", text = "", updatedAt = NOW),
                TextBlock(id = "", text = "second line", updatedAt = NOW)
            ),
            converted.blocks.map { it.withId("") }
        )
    }

    @Test
    fun aFrontMatterTitleIsUsedAndTheFrontMatterIsHandedOn() {
        val converted = convert("---\ntitle: Real title\ntags: [work]\n---\n# Real title\n\nBody")

        assertEquals("Real title", converted.title)
        assertEquals(listOf(TextBlock(id = "", text = "Body", updatedAt = NOW)), converted.blocks.map { it.withId("") })
        assertEquals(
            listOf(
                FrontMatterField("title", FrontMatterValue.Single("Real title")),
                FrontMatterField("tags", FrontMatterValue.Many(listOf("work")))
            ),
            converted.frontMatter
        )
    }

    @Test
    fun tagsWrittenInTheTextAreCollectedAndStayInTheText() {
        val converted = convert("Working on the #importer and #ideas/2026 today, not #42 or `#code` or \\#escaped")

        assertEquals(listOf("importer", "ideas/2026"), converted.inlineTags)
        assertEquals(
            "Working on the #importer and #ideas/2026 today, not #42 or #code or #escaped",
            (converted.blocks.single() as TextBlock).text
        )
    }

    @Test
    fun aCalloutBecomesACalloutBlockWithItsBodyIndentedUnderIt() {
        assertEquals(
            listOf(
                CalloutBlock(id = "", text = "Remember", calloutTypeName = "INFO", updatedAt = NOW),
                TextBlock(id = "", text = "Back up your vault.", indentationLevel = 1, updatedAt = NOW),
                BulletedListBlock(id = "", text = "Twice a week", indentationLevel = 1, updatedAt = NOW)
            ),
            blocksOf("> [!info] Remember\n> Back up your vault.\n>\n> - Twice a week")
        )
    }

    @Test
    fun aCalloutWithoutATitleLetsItsTypeNameShowAndOtherTypesKeepTheirName() {
        assertEquals(
            listOf(
                CalloutBlock(id = "", text = "", calloutTypeName = "TIP", updatedAt = NOW),
                TextBlock(id = "", text = "Stay hydrated.", indentationLevel = 1, updatedAt = NOW),
                CalloutBlock(id = "", text = "Idea", calloutTypeName = "NOTE", updatedAt = NOW),
                TextBlock(id = "", text = "Write it down.", indentationLevel = 1, updatedAt = NOW)
            ),
            blocksOf("> [!TIP]\n> Stay hydrated.\n\n> [!idea]\n> Write it down.")
        )
    }

    @Test
    fun calloutAliasesUseTheMatchingEmberrType() {
        assertEquals(
            listOf("ABSTRACT", "WARNING", "DANGER"),
            blocksOf("> [!summary] A\n\n> [!caution] B\n\n> [!error] C").filterIsInstance<CalloutBlock>().map { it.calloutTypeName }
        )
    }

    @Test
    fun foldableCalloutsKeepWhetherTheyStartOpenOrClosed() {
        assertEquals(
            listOf(
                CalloutBlock(id = "", text = "Why?", calloutTypeName = "QUESTION", isFoldable = true, isExpanded = false, updatedAt = NOW),
                TextBlock(id = "", text = "Because.", indentationLevel = 1, updatedAt = NOW),
                CalloutBlock(id = "", text = "Open one", calloutTypeName = "QUESTION", isFoldable = true, isExpanded = true, updatedAt = NOW),
                TextBlock(id = "", text = "Visible.", indentationLevel = 1, updatedAt = NOW)
            ),
            blocksOf("> [!faq]- Why?\n> Because.\n\n> [!faq]+ Open one\n> Visible.")
        )
    }

    @Test
    fun aPlainQuoteStaysAQuote() {
        assertEquals(listOf(QuoteBlock(id = "", text = "Just a quote", updatedAt = NOW)), blocksOf("> Just a quote"))
    }

    @Test
    fun inlineCommentsAreRemovedAndBlockCommentsBecomeAClosedToggle() {
        assertEquals(
            listOf(
                TextBlock(id = "", text = "Call Ann tomorrow", updatedAt = NOW),
                ToggleBlock(id = "", text = "Comment", isExpanded = false, updatedAt = NOW),
                TextBlock(id = "", text = "Private draft", indentationLevel = 1, updatedAt = NOW)
            ),
            blocksOf("Call Ann %%she prefers mornings%% tomorrow\n\n%%\nPrivate draft\n%%")
        )
    }

    @Test
    fun blockIdsAtTheEndOfABlockAreRemoved() {
        assertEquals(
            listOf(
                TextBlock(id = "", text = "Buy milk", updatedAt = NOW),
                BulletedListBlock(id = "", text = "item", updatedAt = NOW)
            ),
            blocksOf("Buy milk ^groceries\n\n- item ^abc-123")
        )
    }

    @Test
    fun inlineMathKeepsItsSourceExactlyAsWritten() {
        assertEquals(
            listOf(TextBlock(id = "", text = "Area \$x\\_1 + y\\{2\\}\$ done", updatedAt = NOW)),
            blocksOf("Area \$x\\_1 + y\\{2\\}\$ done")
        )
    }

    @Test
    fun blockMathOnItsOwnBecomesACodeBlock() {
        assertEquals(
            listOf(CodeBlock(id = "", code = "x^2 + y^2", language = "latex", updatedAt = NOW)),
            blocksOf("\$\$\nx^2 + y^2\n\$\$")
        )
    }

    @Test
    fun everyBlockGetsItsOwnId() {
        val blocks = convert("# One\n\nTwo\n\n- three").blocks

        assertEquals(listOf("block-1", "block-2", "block-3"), blocks.map { it.id })
    }
}
