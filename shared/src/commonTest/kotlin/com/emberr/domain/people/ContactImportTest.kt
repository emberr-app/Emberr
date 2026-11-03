package com.emberr.domain.people

import kotlin.test.Test
import kotlin.test.assertEquals

class ContactImportTest {

    @Test
    fun aTakenNameGetsTheNextFreeNumber() {
        assertEquals("Maya", nameNotTakenYet("Maya", listOf("Sam")))
        assertEquals("Maya (2)", nameNotTakenYet("Maya", listOf("maya", "Maya (1)")))
    }

    @Test
    fun contactDatesBecomeBirthdayText() {
        assertEquals("1996-03-14", contactDateToBirthdayText("19960314"))
        assertEquals("--03-14", contactDateToBirthdayText("--0314"))
    }
}
