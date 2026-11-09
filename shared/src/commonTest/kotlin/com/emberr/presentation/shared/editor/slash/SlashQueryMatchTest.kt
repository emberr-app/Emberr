package com.emberr.presentation.shared.editor.slash

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlashQueryMatchTest {

    private val searchTexts = listOf("Heading 1", "Heading 2", "Checkbox")

    @Test
    fun aQueryThatIsPartOfAnItemNameKeepsTheSlashMenuOpen() {
        assertTrue(slashQueryCanStillMatch("head", searchTexts))
    }

    @Test
    fun aLongQueryThatMatchesNothingClosesTheSlashMenu() {
        assertFalse(slashQueryCanStillMatch("hezz", searchTexts))
    }

    @Test
    fun aQueryWithASpaceClosesTheSlashMenu() {
        assertFalse(slashQueryCanStillMatch("a b", searchTexts))
    }
}
