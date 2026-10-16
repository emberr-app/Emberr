package com.emberr.domain.util.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

enum class FirstDayOfWeekPreference(val displayName: String) {
    SYSTEM("System default"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday"),
    MONDAY("Monday");

    fun firstDayOfWeek(systemFirstDayOfWeek: DayOfWeek): DayOfWeek = when (this) {
        SYSTEM -> systemFirstDayOfWeek
        SATURDAY -> DayOfWeek.SATURDAY
        SUNDAY -> DayOfWeek.SUNDAY
        MONDAY -> DayOfWeek.MONDAY
    }
}

fun firstDayOfWeekPreferenceNamed(name: String): FirstDayOfWeekPreference =
    FirstDayOfWeekPreference.entries.firstOrNull { it.name == name } ?: FirstDayOfWeekPreference.SYSTEM

fun daysSinceStartOfWeek(dayOfWeek: DayOfWeek, firstDayOfWeek: DayOfWeek): Int =
    (dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + 7) % 7

fun LocalDate.startOfWeek(firstDayOfWeek: DayOfWeek): LocalDate =
    minus(daysSinceStartOfWeek(dayOfWeek, firstDayOfWeek), DateTimeUnit.DAY)

fun daysOfWeekStartingFrom(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
    (0 until 7).map { offset -> DayOfWeek.entries[(firstDayOfWeek.ordinal + offset) % 7] }
