package com.emberr.presentation.shared.editor.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarkdownShortcutsTest {

    @Test
    fun typingTheSpaceAfterAMarkerAtTheStartTurnsTheBlockIntoThatType() {
        assertEquals(MarkdownShortcut.TurnInto("h1", "Plan"), markdownShortcutAfterTyping("#Plan", "# Plan", cursor = 2))
        assertEquals(MarkdownShortcut.TurnInto("number", ""), markdownShortcutAfterTyping("12.", "12. ", cursor = 4))
        assertNull(markdownShortcutAfterTyping("Plan #", "Plan # ", cursor = 7))
    }

    @Test
    fun boldOnlyAppliesOnTheLastClosingStarAndNeverTurnsItalicHalfWay() {
        assertNull(markdownShortcutAfterTyping("say **hi", "say **hi*", cursor = 9))
        assertEquals(
            MarkdownShortcut.InlineFormat("bold", formattedStart = 4, formattedEnd = 6, convertedText = "say hi"),
            markdownShortcutAfterTyping("say **hi*", "say **hi**", cursor = 10)
        )
    }
}
