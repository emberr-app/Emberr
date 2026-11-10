package com.emberr.domain.util.time

enum class TimeFormatPreference {
    SYSTEM,
    TWELVE_HOUR,
    TWENTY_FOUR_HOUR;

    fun usesTwentyFourHourClock(systemUsesTwentyFourHourClock: Boolean): Boolean = when (this) {
        SYSTEM -> systemUsesTwentyFourHourClock
        TWELVE_HOUR -> false
        TWENTY_FOUR_HOUR -> true
    }
}

fun timeFormatPreferenceNamed(name: String): TimeFormatPreference =
    TimeFormatPreference.entries.firstOrNull { it.name == name } ?: TimeFormatPreference.SYSTEM

fun formatTimeOfDay(hour: Int, minute: Int, usesTwentyFourHourClock: Boolean): String {
    val paddedMinute = minute.toString().padStart(2, '0')
    if (usesTwentyFourHourClock) return "${hour.toString().padStart(2, '0')}:$paddedMinute"
    return joinTimeWithDayPeriod("${twelveHourClockHour(hour)}:$paddedMinute", isAfternoon = hour >= 12)
}

fun formatHourLabel(hour: Int, usesTwentyFourHourClock: Boolean): String {
    if (usesTwentyFourHourClock) return "${hour.toString().padStart(2, '0')}:00"
    return joinTimeWithDayPeriod("${twelveHourClockHour(hour)}", isAfternoon = hour >= 12)
}

private fun twelveHourClockHour(hour: Int): Int = when {
    hour == 0 -> 12
    hour > 12 -> hour - 12
    else -> hour
}
