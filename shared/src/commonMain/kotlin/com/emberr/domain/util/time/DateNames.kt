package com.emberr.domain.util.time

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month

expect fun monthName(month: Month): String

expect fun shortMonthName(month: Month): String

expect fun weekdayName(day: DayOfWeek): String

expect fun shortWeekdayName(day: DayOfWeek): String

expect fun weekdayLetter(day: DayOfWeek): String

expect fun dayPeriodName(isAfternoon: Boolean): String

expect fun joinTimeWithDayPeriod(time: String, isAfternoon: Boolean): String
