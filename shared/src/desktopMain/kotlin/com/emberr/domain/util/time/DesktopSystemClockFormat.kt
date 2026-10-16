package com.emberr.domain.util.time

import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

private val systemUsesTwentyFourHourClockOncePerProcess: Boolean by lazy {
    val shortTimePattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        null,
        FormatStyle.SHORT,
        IsoChronology.INSTANCE,
        timeLocaleFromEnvironment() ?: Locale.getDefault(Locale.Category.FORMAT)
    )
    'a' !in shortTimePattern && 'B' !in shortTimePattern
}

fun desktopSystemUsesTwentyFourHourClock(): Boolean = systemUsesTwentyFourHourClockOncePerProcess

private fun timeLocaleFromEnvironment(): Locale? {
    val localeName = listOf("LC_ALL", "LC_TIME")
        .firstNotNullOfOrNull { variable -> System.getenv(variable)?.takeIf { it.isNotBlank() } }
        ?: return null
    val languageAndRegion = localeName.substringBefore('.').substringBefore('@')
    if (languageAndRegion == "C" || languageAndRegion == "POSIX") return null
    return Locale.forLanguageTag(languageAndRegion.replace('_', '-'))
}
