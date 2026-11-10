package com.emberr.domain.util.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.time.DateTimeException
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

actual fun formatDate(date: LocalDate, style: DateTextStyle): String {
    val locale = Locale.getDefault()
    val javaDate = java.time.LocalDate.of(date.year, date.month.number, date.day)
    val text = try {
        DateTimeFormatter.ofLocalizedPattern(style.skeleton).withLocale(locale).format(javaDate)
    } catch (_: DateTimeException) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(javaDate)
    }
    return text.startingWithCapital()
}
