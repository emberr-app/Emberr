package com.emberr.presentation.shared.components

import kotlin.test.Test
import kotlin.test.assertEquals

class PersonInitialsTest {

    @Test
    fun aFullNameUsesTheFirstAndLastWord() {
        assertEquals("MC", personInitials("maya rose chen"))
    }

    @Test
    fun aOneWordNameUsesOneLetter() {
        assertEquals("M", personInitials("Maya"))
    }

    @Test
    fun anEmptyNameShowsAQuestionMark() {
        assertEquals("?", personInitials("   "))
    }
}
