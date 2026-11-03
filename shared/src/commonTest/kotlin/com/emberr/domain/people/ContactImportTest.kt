package com.emberr.domain.people

import com.emberr.domain.model.PersonDetailKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactImportTest {

    @Test
    fun aTakenNameGetsTheNextFreeNumber() {
        assertEquals("Maya", nameNotTakenYet("Maya", listOf("Sam")))
        assertEquals("Maya (2)", nameNotTakenYet("Maya", listOf("maya", "Maya (1)")))
    }

    @Test
    fun aContactWithNothingNewIsSkippedButOneWithMoreIsNot() {
        val savedDetails = listOf(
            ContactDetailToImport(PersonDetailKind.PHONE, "Mobile", "+1 (555) 010-0100"),
            ContactDetailToImport(PersonDetailKind.EMAIL, "Work", "maya@example.com")
        )
        val shorterCard = ContactToImport("maya  ", listOf(ContactDetailToImport(PersonDetailKind.PHONE, null, "555-010-0100")), "", null)
        val cardWithANewNumber = ContactToImport("Maya", listOf(ContactDetailToImport(PersonDetailKind.PHONE, null, "555-010-0199")), "", null)

        assertTrue(contactAddsNothingNewTo(shorterCard, "Maya", savedDetails))
        assertFalse(contactAddsNothingNewTo(cardWithANewNumber, "Maya", savedDetails))
    }

    @Test
    fun contactDatesBecomeBirthdayText() {
        assertEquals("1996-03-14", contactDateToBirthdayText("19960314"))
        assertEquals("--03-14", contactDateToBirthdayText("--0314"))
    }
}
