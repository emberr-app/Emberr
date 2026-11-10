package com.emberr.domain.util.time

import android.text.format.DateFormat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.time.format.DateTimeFormatter
import java.util.Locale

actual fun formatDate(date: LocalDate, style: DateTextStyle): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, style.skeleton)
    return DateTimeFormatter.ofPattern(pattern, locale)
        .format(java.time.LocalDate.of(date.year, date.month.number, date.day))
        .startingWithCapital()
}
