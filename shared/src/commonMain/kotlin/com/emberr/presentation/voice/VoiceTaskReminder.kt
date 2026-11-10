package com.emberr.presentation.voice

import com.emberr.domain.util.time.formatTimeOfDay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val DEFAULT_REMINDER_TIME = LocalTime(9, 0)

internal fun voiceTaskDayLabel(
    reminder: Long?,
    today: LocalDate,
    timeZone: TimeZone,
    todayLabel: String,
    tomorrowLabel: String
): String {
    val date = reminder?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).date } ?: today
    return when (date) {
        today -> todayLabel
        today.plus(DatePeriod(days = 1)) -> tomorrowLabel
        else -> {
            val shortDay = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val shortMonth = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            "$shortDay, $shortMonth ${date.day}"
        }
    }
}

internal fun voiceTaskTimeLabel(
    reminder: Long?,
    timeZone: TimeZone,
    usesTwentyFourHourClock: Boolean,
    addTimeLabel: String
): String {
    if (reminder == null) return addTimeLabel
    val dateTime = Instant.fromEpochMilliseconds(reminder).toLocalDateTime(timeZone)
    return formatTimeOfDay(dateTime.hour, dateTime.minute, usesTwentyFourHourClock)
}

internal fun reminderMovedToDate(currentReminder: Long?, pickedDate: Long, timeZone: TimeZone): Long {
    val date = Instant.fromEpochMilliseconds(pickedDate).toLocalDateTime(timeZone).date
    val time = currentReminder?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).time }
        ?: DEFAULT_REMINDER_TIME
    return LocalDateTime(date, time).toInstant(timeZone).toEpochMilliseconds()
}

internal fun reminderMovedToTime(
    currentReminder: Long?,
    hour: Int,
    minute: Int,
    today: LocalDate,
    timeZone: TimeZone
): Long {
    val date = currentReminder?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).date } ?: today
    return LocalDateTime(date, LocalTime(hour, minute)).toInstant(timeZone).toEpochMilliseconds()
}
