package com.emberr.domain.util.time

import kotlinx.datetime.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

private val systemFirstDayOfWeekOncePerProcess: DayOfWeek by lazy {
    val locale = timeLocaleFromEnvironment() ?: Locale.getDefault(Locale.Category.FORMAT)
    val mondayBasedDayNumber = WeekFields.of(locale).firstDayOfWeek.value
    DayOfWeek.entries[mondayBasedDayNumber - 1]
}

fun desktopSystemFirstDayOfWeek(): DayOfWeek = systemFirstDayOfWeekOncePerProcess
