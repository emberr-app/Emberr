package com.emberr.domain.people

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BirthdaysInRangeTest {

    private fun person(name: String, birthday: String, trashedAt: Long? = null) = PersonWithDetails(
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
        details = listOf(PersonDetailBlock(id = "$name-birthday", kind = PersonDetailKind.BIRTHDAY, value = birthday))
    )

    private fun List<BirthdayOnDate>.namesDatesAndAges() = map { Triple(it.personName, it.date.toString(), it.turningAge) }

    @Test
    fun aRangeAcrossNewYearGivesOneItemPerYearWithAgeOnlyAfterTheBirthYear() {
        val people = listOf(
            person("Maya", "1996-12-31"),
            person("Ravi", "--01-02"),
            person("Baby", "2026-01-01"),
            person("Later", "2027-01-01"),
            person("Gone", "1990-01-01", trashedAt = 1L)
        )

        assertEquals(
            listOf(
                Triple("Maya", "2025-12-31", 29),
                Triple("Ravi", "2026-01-02", null),
                Triple("Baby", "2026-01-01", null)
            ),
            birthdaysInRange(people, LocalDate(2025, 12, 20), LocalDate(2026, 1, 10)).namesDatesAndAges()
        )
    }

    @Test
    fun aLeapDayBirthdayFallsOnFebruary28InYearsWithoutOne() {
        val people = listOf(person("Leap", "2000-02-29"))

        assertEquals(
            listOf(Triple("Leap", "2027-02-28", 27), Triple("Leap", "2028-02-29", 28)),
            birthdaysInRange(people, LocalDate(2027, 1, 1), LocalDate(2028, 12, 31)).namesDatesAndAges()
        )
    }
}
