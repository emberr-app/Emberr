package com.emberr.domain.util.time

import kotlinx.datetime.LocalDate

enum class DateTextStyle(val skeleton: String) {
    SHORT_MONTH_DAY("MMMd"),
    FULL_MONTH_DAY("MMMMd"),
    SHORT_WEEKDAY_SHORT_MONTH_DAY("MMMEd"),
    FULL_WEEKDAY_SHORT_MONTH_DAY("MMMEEEEd"),
    FULL_WEEKDAY_FULL_MONTH_DAY("MMMMEEEEd"),
    MONTH_YEAR("yMMMM"),
    SHORT_MONTH_DAY_YEAR("yMMMd"),
    FULL_MONTH_DAY_YEAR("yMMMMd"),
    SHORT_WEEKDAY_SHORT_MONTH_DAY_YEAR("yMMMEd"),
    FULL_WEEKDAY_SHORT_MONTH_DAY_YEAR("yMMMEEEEd")
}

expect fun formatDate(date: LocalDate, style: DateTextStyle): String
