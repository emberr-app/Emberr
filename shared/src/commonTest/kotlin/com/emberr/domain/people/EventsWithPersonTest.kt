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
        recurrenceUntil: String? = null
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
        recurrenceUntil = recurrenceUntil,
        personNoteIds = "maya"
    )

    @Test
    fun eventsAreSplitIntoUpcomingSoonestFirstAndPastNewestFirst() {
        val events = listOf(
            event("next week", "2026-11-08", startsAt = LocalDateTime(2026, 11, 8, 9, 0)),
            event("ended this morning", "2026-11-01", startsAt = LocalDateTime(2026, 11, 1, 9, 0)),
            event("still going", "2026-11-01", startsAt = LocalDateTime(2026, 11, 1, 11, 45)),
            event("today, no time", "2026-11-01"),
            event("last month", "2026-10-01", startsAt = LocalDateTime(2026, 10, 1, 9, 0)),
            event("weekly, still repeating", "2026-01-05", startsAt = LocalDateTime(2026, 1, 5, 9, 0), recurrenceFrequency = RecurrenceFrequency.WEEKLY),
            event("weekly, finished", "2026-01-06", startsAt = LocalDateTime(2026, 1, 6, 9, 0), recurrenceFrequency = RecurrenceFrequency.WEEKLY, recurrenceUntil = "2026-03-01")
        )

        val split = splitEventsWithPerson(events, now, timeZone)

        assertEquals(listOf("weekly, still repeating", "today, no time", "still going", "next week"), split.upcoming.map { it.blockId })
        assertEquals(listOf("ended this morning", "last month", "weekly, finished"), split.past.map { it.blockId })
    }
}
