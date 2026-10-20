package com.emberr.domain.reminders

import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AllDayRemindersTest {

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int): Long =
        LocalDateTime(year, month, day, hour, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()

    private fun nextReminder(allDay: AllDaySpan, recurrenceRule: RecurrenceRule?, start: Long, now: Long): Long? =
        nextAllDayReminderTimestamp(start, allDay, recurrenceRule, now, TimeZone.UTC)

    @Test
    fun threeDayEventRemindsOnEachDayItCoversAndThenStops() {
        val start = utcMillis(2026, 10, 30, 9)
        val threeDays = AllDaySpan(lengthInDays = 3)

        assertEquals(utcMillis(2026, 10, 30, 9), nextReminder(threeDays, null, start, now = utcMillis(2026, 10, 30, 8)))
        assertEquals(utcMillis(2026, 10, 31, 9), nextReminder(threeDays, null, start, now = utcMillis(2026, 10, 30, 10)))
        assertNull(nextReminder(threeDays, null, start, now = utcMillis(2026, 11, 1, 10)))
        assertNull(nextReminder(threeDays.copy(isReminderOn = false), null, start, now = utcMillis(2026, 10, 30, 8)))
    }

    @Test
    fun weeklyTwoDayEventRemindsOnTheSecondDayAndThenOnTheNextWeek() {
        val mondayStart = utcMillis(2026, 10, 19, 9)
        val twoDays = AllDaySpan(lengthInDays = 2)
        val weekly = RecurrenceRule(RecurrenceFrequency.WEEKLY)

        assertEquals(utcMillis(2026, 10, 27, 9), nextReminder(twoDays, weekly, mondayStart, now = utcMillis(2026, 10, 27, 8)))
        assertEquals(utcMillis(2026, 11, 2, 9), nextReminder(twoDays, weekly, mondayStart, now = utcMillis(2026, 10, 27, 10)))
    }
}
