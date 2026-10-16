package com.emberr.domain.util.time

enum class TimeFormatPreference(val displayName: String) {
    SYSTEM("System default"),
    TWELVE_HOUR("12-hour"),
    TWENTY_FOUR_HOUR("24-hour");

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
    return "${twelveHourClockHour(hour)}:$paddedMinute ${dayPeriod(hour)}"
}

fun formatHourLabel(hour: Int, usesTwentyFourHourClock: Boolean): String {
    if (usesTwentyFourHourClock) return "${hour.toString().padStart(2, '0')}:00"
    return "${twelveHourClockHour(hour)} ${dayPeriod(hour)}"
}

private fun twelveHourClockHour(hour: Int): Int = when {
    hour == 0 -> 12
    hour > 12 -> hour - 12
    else -> hour
}

private fun dayPeriod(hour: Int): String = if (hour < 12) "AM" else "PM"
