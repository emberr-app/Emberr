package com.emberr.presentation.shared.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardToolbarItemTest {

    @Test
    fun readingASavedToolbarSkipsUnknownNamesAndRepeatedButtonsButKeepsEveryDivider() {
        val savedValue = "BOLD,DIVIDER,REMOVED_BUTTON,BOLD,DIVIDER,ITALIC"

        val items = keyboardToolbarItemsFrom(savedValue)

        assertEquals(
            listOf(
                KeyboardToolbarItem.BOLD,
                KeyboardToolbarItem.DIVIDER,
                KeyboardToolbarItem.DIVIDER,
                KeyboardToolbarItem.ITALIC
            ),
            items
        )
    }
}
