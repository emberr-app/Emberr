package com.emberr.domain.util.time

import kotlin.test.Test
import kotlin.test.assertEquals

class ClockTimeFormatTest {

    @Test
    fun midnightAndNoonReadCorrectlyOnBothClocks() {
        assertEquals("12:05 AM", formatTimeOfDay(0, 5, usesTwentyFourHourClock = false))
        assertEquals("12:30 PM", formatTimeOfDay(12, 30, usesTwentyFourHourClock = false))
        assertEquals("00:05", formatTimeOfDay(0, 5, usesTwentyFourHourClock = true))
        assertEquals("18:30", formatTimeOfDay(18, 30, usesTwentyFourHourClock = true))
        assertEquals("12 AM", formatHourLabel(0, usesTwentyFourHourClock = false))
        assertEquals("09:00", formatHourLabel(9, usesTwentyFourHourClock = true))
    }
}
