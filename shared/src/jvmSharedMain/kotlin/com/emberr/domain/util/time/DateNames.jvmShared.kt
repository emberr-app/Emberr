package com.emberr.domain.util.time

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import java.text.DateFormatSymbols
import java.time.format.TextStyle
import java.util.Locale

private val languagesWithDayPeriodFirstAndSpace = setOf("ko")
private val languagesWithDayPeriodFirstAndNoSpace = setOf("ja", "zh")

actual fun monthName(month: Month): String =
    javaMonth(month).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).startingWithCapital()

actual fun shortMonthName(month: Month): String =
    javaMonth(month).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()).startingWithCapital()

actual fun weekdayName(day: DayOfWeek): String =
    javaDayOfWeek(day).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).startingWithCapital()

actual fun shortWeekdayName(day: DayOfWeek): String =
    javaDayOfWeek(day).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()).startingWithCapital()

actual fun weekdayLetter(day: DayOfWeek): String =
    javaDayOfWeek(day).getDisplayName(TextStyle.NARROW_STANDALONE, Locale.getDefault()).startingWithCapital()

actual fun dayPeriodName(isAfternoon: Boolean): String =
    DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings[if (isAfternoon) 1 else 0]

actual fun joinTimeWithDayPeriod(time: String, isAfternoon: Boolean): String {
    val dayPeriod = dayPeriodName(isAfternoon)
    return when (Locale.getDefault().language) {
        in languagesWithDayPeriodFirstAndSpace -> "$dayPeriod $time"
        in languagesWithDayPeriodFirstAndNoSpace -> "$dayPeriod$time"
        else -> "$time $dayPeriod"
    }
}

internal fun String.startingWithCapital(): String =
    replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun javaMonth(month: Month): java.time.Month = java.time.Month.of(month.number)

private fun javaDayOfWeek(day: DayOfWeek): java.time.DayOfWeek = java.time.DayOfWeek.of(day.isoDayNumber)
