package com.emberr.domain.people

import com.emberr.domain.model.PersonDetailKind
import kotlin.test.Test
import kotlin.test.assertEquals

class VCardReaderTest {

    @Test
    fun aVersion3CardKeepsFoldedNotesLabelsAndYearlessBirthdays() {
        val text = listOf(
            "BEGIN:VCARD",
            "VERSION:3.0",
            "FN:Maya Chen",
            "item1.TEL;TYPE=CELL:+1 555 0100",
            "item1.X-ABLabel:_\$!<Pager>!\$_",
            "EMAIL;TYPE=INTERNET,WORK:maya@example.com",
            "BDAY:--0314",
            "ADR;TYPE=HOME:;;12 Oak St;Springfield;IL;62701;USA",
            "NOTE:Likes tea\\, not coffee.\\nMet at ",
            " the book club.",
            "END:VCARD",
            "BEGIN:VCARD",
            "N:Lee;Sam;;;",
            "END:VCARD"
        ).joinToString("\r\n")

        val contacts = readContactsFromVCardText(text)

        assertEquals(listOf("Maya Chen", "Sam Lee"), contacts.map { it.name })
        assertEquals(
            listOf(
                ContactDetailToImport(PersonDetailKind.PHONE, "Pager", "+1 555 0100"),
                ContactDetailToImport(PersonDetailKind.EMAIL, "Work", "maya@example.com"),
                ContactDetailToImport(PersonDetailKind.BIRTHDAY, null, "--03-14"),
                ContactDetailToImport(PersonDetailKind.ADDRESS, "Home", "12 Oak St, Springfield, IL 62701, USA")
            ),
            contacts.first().details
        )
        assertEquals("Likes tea, not coffee.\nMet at the book club.", contacts.first().note)
    }

    @Test
    fun aVersion21CardDecodesQuotedPrintableText() {
        val text = listOf(
            "BEGIN:VCARD",
            "VERSION:2.1",
            "FN;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:Jos=C3=A9 Mart=",
            "=C3=ADnez",
            "TEL;HOME;VOICE:555-0199",
            "END:VCARD"
        ).joinToString("\n")

        val contact = readContactsFromVCardText(text).single()

        assertEquals("José Martínez", contact.name)
        assertEquals(listOf(ContactDetailToImport(PersonDetailKind.PHONE, "Home", "555-0199")), contact.details)
    }
}
