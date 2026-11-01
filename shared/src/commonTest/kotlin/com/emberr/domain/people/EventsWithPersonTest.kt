package com.emberr.domain.people

import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.TaskSource
import com.emberr.domain.model.RecurrenceFrequency
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class EventsWithPersonTest {

    private val timeZone = TimeZone.UTC
    private val now = LocalDateTime(2026, 11, 1, 12, 0).toInstant(timeZone)

    private fun event(
        id: String,
        targetDate: String,
        startsAt: LocalDateTime? = null,
        durationMinutes: Int = 30,
        recurrenceFrequency: RecurrenceFrequency? = null,
        personNoteIds: String = "maya"
    ) = CalendarTaskEntity(
        blockId = id,
        noteId = "note",
        text = id,
        isChecked = false,
        targetDate = targetDate,
        reminderTimestamp = startsAt?.toInstant(timeZone)?.toEpochMilliseconds(),
        sourceType = TaskSource.NOTE,
        durationMinutes = durationMinutes,
        recurrenceFrequency = recurrenceFrequency,
        personNoteIds = personNoteIds
    )

    @Test
    fun eventsAreSplitIntoUpcomingSoonestFirstAndPastNewestFirstWithOneRowPerRepeatingEventAhead() {
        val events = listOf(
            event("next week", "2026-11-08", startsAt = LocalDateTime(2026, 11, 8, 9, 0)),
            event("ended this morning", "2026-11-01", startsAt = LocalDateTime(2026, 11, 1, 9, 0)),
            event("still going", "2026-11-01", startsAt = LocalDateTime(2026, 11, 1, 11, 45)),
            event("today, no time", "2026-11-01"),
            event("last month", "2026-10-01", startsAt = LocalDateTime(2026, 10, 1, 9, 0)),
            event("weekly", "2026-11-03", startsAt = LocalDateTime(2026, 11, 3, 9, 0), recurrenceFrequency = RecurrenceFrequency.WEEKLY),
            event("weekly", "2026-11-10", startsAt = LocalDateTime(2026, 11, 10, 9, 0), recurrenceFrequency = RecurrenceFrequency.WEEKLY),
            event("weekly", "2026-10-27", startsAt = LocalDateTime(2026, 10, 27, 9, 0), recurrenceFrequency = RecurrenceFrequency.WEEKLY),
            event("someone else", "2026-11-02", startsAt = LocalDateTime(2026, 11, 2, 9, 0), personNoteIds = "sam")
        )

        val split = eventsWithPerson("maya", events, now, timeZone)

        assertEquals(listOf("today, no time", "still going", "weekly", "next week"), split.upcoming.map { it.blockId })
        assertEquals("2026-11-03", split.upcoming.single { it.blockId == "weekly" }.targetDate)
        assertEquals(listOf("ended this morning", "weekly", "last month"), split.past.map { it.blockId })
    }
}
