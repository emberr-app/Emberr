package com.emberr.domain.people

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonSearchTest {

    private fun person(name: String, vararg details: PersonDetailBlock, trashedAt: Long? = null) = PersonWithDetails(
        person = NoteMetadataEntity(
            noteId = name,
            title = name,
            folderId = null,
            isDaily = false,
            dateString = null,
            createdAt = 0L,
            updatedAt = 0L,
            filePath = "",
            spaceId = ACCOUNT_SPACE_ID,
            kind = NoteKind.PERSON,
            trashedAt = trashedAt
        ),
        details = details.toList()
    )

    private fun detail(id: String, kind: PersonDetailKind, value: String, label: String? = null, isDeleted: Boolean = false) =
        PersonDetailBlock(id = id, kind = kind, label = label, value = value, isDeleted = isDeleted)

    private fun List<PersonSearchResult>.namesAndShownDetails() = map { it.person.title to it.shownDetail?.id }

    @Test
    fun matchesNamesAndLiveDetailsIncludingPhoneDigitsWithoutPunctuation() {
        val people = listOf(
            person("Maya", detail("maya-phone", PersonDetailKind.PHONE, "+91 (98765) 43-210", label = "Mobile")),
            person("Ravi", detail("ravi-old-phone", PersonDetailKind.PHONE, "98765 43210", isDeleted = true)),
            person("Sam 98765", trashedAt = 1L),
            person("Lena", detail("lena-company", PersonDetailKind.COMPANY, "Acme"))
        )

        assertEquals(listOf("Maya" to "maya-phone"), searchPeople(people, "98765").namesAndShownDetails())
        assertEquals(listOf("Maya" to "maya-phone"), searchPeople(people, "9876543210").namesAndShownDetails())
        assertEquals(listOf("Maya" to "maya-phone"), searchPeople(people, "  mobile ").namesAndShownDetails())
        assertEquals(listOf("Lena" to "lena-company"), searchPeople(people, "ACME").namesAndShownDetails())
        assertEquals(listOf("Lena" to null), searchPeople(people, "len").namesAndShownDetails())
    }

    @Test
    fun nameMatchesComeBeforeDetailMatchesAndEachGroupIsSortedByName() {
        val people = listOf(
            person("Zoe", detail("zoe-email", PersonDetailKind.EMAIL, "zoe@ann.dev")),
            person("Joanna", detail("joanna-email", PersonDetailKind.EMAIL, "jo@mail.com")),
            person("Bob", detail("bob-relationship", PersonDetailKind.RELATIONSHIP, "Anna's brother")),
            person("Ann")
        )

        assertEquals(
            listOf("Ann" to null, "Joanna" to "joanna-email", "Bob" to "bob-relationship", "Zoe" to "zoe-email"),
            searchPeople(people, "ann").namesAndShownDetails()
        )
    }
}
