package com.emberr.domain.util.time

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FirstDayOfWeekTest {

    @Test
    fun weekStartAndMonthBlanksFollowTheChosenFirstDay() {
        val wednesdayFirstOfOctober = LocalDate(2025, 10, 1)
        val sundayTheTwelfth = LocalDate(2025, 10, 12)

        assertEquals(LocalDate(2025, 10, 6), sundayTheTwelfth.startOfWeek(DayOfWeek.MONDAY))
        assertEquals(LocalDate(2025, 10, 12), sundayTheTwelfth.startOfWeek(DayOfWeek.SUNDAY))
        assertEquals(LocalDate(2025, 10, 11), sundayTheTwelfth.startOfWeek(DayOfWeek.SATURDAY))

        assertEquals(2, daysSinceStartOfWeek(wednesdayFirstOfOctober.dayOfWeek, DayOfWeek.MONDAY))
        assertEquals(3, daysSinceStartOfWeek(wednesdayFirstOfOctober.dayOfWeek, DayOfWeek.SUNDAY))
        assertEquals(4, daysSinceStartOfWeek(wednesdayFirstOfOctober.dayOfWeek, DayOfWeek.SATURDAY))

        assertEquals(
            listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
            daysOfWeekStartingFrom(DayOfWeek.SATURDAY).take(3)
        )
    }
}
