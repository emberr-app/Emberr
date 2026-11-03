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
    fun onlyAContactWithTheSameNameAndDetailsIsACopy() {
        val savedDetails = listOf(ContactDetailToImport(PersonDetailKind.PHONE, "Mobile", "+1 (555) 010-0100"))
        val sameContact = ContactToImport("maya", listOf(ContactDetailToImport(PersonDetailKind.PHONE, "Mobile", "+15550100100")), "", null)
        val contactWithAnEmail = ContactToImport("Maya", savedDetails + ContactDetailToImport(PersonDetailKind.EMAIL, null, "maya@example.com"), "", null)

        assertTrue(isExactCopyOfPerson(sameContact, "Maya", savedDetails))
        assertFalse(isExactCopyOfPerson(contactWithAnEmail, "Maya", savedDetails))
    }

    @Test
    fun contactDatesBecomeBirthdayText() {
        assertEquals("1996-03-14", contactDateToBirthdayText("19960314"))
        assertEquals("--03-14", contactDateToBirthdayText("--0314"))
    }
}
