package com.emberr.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurrenceSeriesShiftTest {

    @Test
    fun draggingOneWeekdayIntoTheNextWeekOnlyChangesThatWeekday() {
        val mondaysAndWednesdays = RecurrenceRule(RecurrenceFrequency.WEEKLY, 1, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val seriesStart = LocalDate(2026, 10, 19)

        val shift = shiftRecurringSeries(
            rule = mondaysAndWednesdays,
            anchor = seriesStart,
            draggedOccurrence = LocalDate(2026, 10, 26),
            targetDate = LocalDate(2026, 11, 3)
        )

        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY), shift.rule.daysOfWeek)
        assertEquals(seriesStart, shift.anchor)
        assertEquals(LocalDate(2026, 10, 27), shift.shiftedOccurrenceDate(LocalDate(2026, 10, 26)))
        assertEquals(LocalDate(2026, 10, 28), shift.shiftedOccurrenceDate(LocalDate(2026, 10, 28)))
        assertTrue(RecurrenceEngine.occursOn(shift.rule, shift.anchor, LocalDate(2026, 10, 27)))
    }

    @Test
    fun draggingAMonthlyEventMovesItsStartAndEndByTheSameDays() {
        val fifteenthUntilDecember = RecurrenceRule(RecurrenceFrequency.MONTHLY, 1, emptySet(), "2026-12-15")

        val shift = shiftRecurringSeries(
            rule = fifteenthUntilDecember,
            anchor = LocalDate(2026, 10, 15),
            draggedOccurrence = LocalDate(2026, 11, 15),
            targetDate = LocalDate(2026, 11, 16)
        )

        assertEquals(LocalDate(2026, 10, 16), shift.anchor)
        assertEquals("2026-12-16", shift.rule.untilDateString)
        assertTrue(RecurrenceEngine.occursOn(shift.rule, shift.anchor, LocalDate(2026, 12, 16)))
    }
}
