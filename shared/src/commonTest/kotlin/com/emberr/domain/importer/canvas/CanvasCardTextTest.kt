package com.emberr.domain.importer.canvas

import kotlin.test.Test
import kotlin.test.assertEquals

private const val NOW = 1_700_000_000_000L

class CanvasCardTextTest {

    private fun cardTextOf(markdown: String): String = CanvasCardText.fromMarkdown(markdown, NOW)

    @Test
    fun formattingMarksAreRemoved() {
        assertEquals("Bold and italic and code", cardTextOf("**Bold** and _italic_ and `code`"))
    }

    @Test
    fun headingsParagraphsAndListsBecomeLines() {
        assertEquals(
            "Plan\nIntro\n• one\n  • nested\n1. first\n2. second\n☐ todo\n☑ done",
            cardTextOf("# Plan\n\nIntro\n\n- one\n  - nested\n\n1. first\n2. second\n\n- [ ] todo\n- [x] done")
        )
    }

    @Test
    fun wikiLinksShowTheirTextOrAlias() {
        assertEquals("See the plan and Ideas", cardTextOf("See [[Project plan|the plan]] and [[Ideas]]"))
    }

    @Test
    fun webLinksKeepTheirAddress() {
        assertEquals("Read Docs (https://example.com) now", cardTextOf("Read [Docs](https://example.com) now"))
        assertEquals("Docs (https://example.com)", cardTextOf("[Docs](https://example.com)"))
    }

    @Test
    fun imagesAndFilesShowTheirFileName() {
        assertEquals("diagram.png\nchart.png\nreport.pdf", cardTextOf("![[diagram.png]]\n\n![chart](img/chart.png)\n\n![[report.pdf]]"))
    }

    @Test
    fun aCalloutWithoutATitleShowsItsTypeAndItsBodyIsIndented() {
        assertEquals("Tip\n  Stay hydrated.", cardTextOf("> [!tip]\n> Stay hydrated."))
    }

    @Test
    fun emptyTextStaysEmpty() {
        assertEquals("", cardTextOf(""))
    }
}
