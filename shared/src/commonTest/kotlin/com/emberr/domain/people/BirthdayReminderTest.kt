package com.emberr.domain.people

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class BirthdayReminderTest {

    private val nineInTheMorning = 9 * 60

    private fun person(noteId: String, name: String, birthday: String) = PersonWithDetails(
        person = NoteMetadataEntity(
            noteId = noteId,
            title = name,
            folderId = null,
            isDaily = false,
            dateString = null,
            createdAt = 0L,
            updatedAt = 0L,
            filePath = "",
            spaceId = ACCOUNT_SPACE_ID,
            kind = NoteKind.PERSON
        ),
        details = listOf(PersonDetailBlock(id = "$noteId-birthday", kind = PersonDetailKind.BIRTHDAY, value = birthday))
    )

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(TimeZone.UTC).toEpochMilliseconds()

    private fun nextReminder(person: PersonWithDetails, minuteOfDay: Int, now: Long) =
        nextBirthdayReminder(person, minuteOfDay, now, TimeZone.UTC)

    @Test
    fun onTheBirthdayTheReminderIsTodayUntilItsTimeThenNextYear() {
        val maya = person("maya-id", "Maya", "1995-11-02")

        assertEquals(
            BirthdayReminder("birthday_maya-id", "Maya", 31, millisAt(2026, 11, 2, 9, 0)),
            nextReminder(maya, nineInTheMorning, now = millisAt(2026, 11, 2, 8, 59))
        )
        assertEquals(
            BirthdayReminder("birthday_maya-id", "Maya", 32, millisAt(2027, 11, 2, 9, 0)),
            nextReminder(maya, nineInTheMorning, now = millisAt(2026, 11, 2, 9, 0))
        )
        assertEquals(
            millisAt(2026, 11, 2, 18, 30),
            nextReminder(maya, 18 * 60 + 30, now = millisAt(2026, 11, 2, 12, 0))?.timestamp
        )
        assertEquals("maya-id", personNoteIdOfBirthdayReminder("birthday_maya-id"))
    }

    @Test
    fun aLeapDayBirthdayIsRemindedOnFebruary28AndAMissingYearOrNameStillGivesAReminder() {
        val leap = person("leap-id", "Leap", "2000-02-29")
        val noYearNoName = person("blank-id", " ", "--03-14")

        assertEquals(
            BirthdayReminder("birthday_leap-id", "Leap", 27, millisAt(2027, 2, 28, 9, 0)),
            nextReminder(leap, nineInTheMorning, now = millisAt(2027, 1, 10, 12, 0))
        )
        assertEquals(
            millisAt(2028, 2, 29, 9, 0),
            nextReminder(leap, nineInTheMorning, now = millisAt(2027, 2, 28, 10, 0))?.timestamp
        )
        assertEquals(
            BirthdayReminder("birthday_blank-id", "", null, millisAt(2027, 3, 14, 9, 0)),
            nextReminder(noYearNoName, nineInTheMorning, now = millisAt(2026, 11, 2, 12, 0))
        )
        assertEquals(
            "Maya",
            nextReminder(person("spaced-id", "  Maya  ", "--03-14"), nineInTheMorning, now = millisAt(2026, 11, 2, 12, 0))?.personName
        )
    }
}
