package com.emberr.presentation.calendar

import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.TaskSource
import com.emberr.data.local.room.entity.shownOnDateString
import com.emberr.data.local.room.entity.toAllDaySpan
import com.emberr.data.local.room.entity.toRecurrenceRule
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.RecurrenceRule
import com.emberr.domain.model.lastDate
import com.emberr.domain.util.time.formatTimeOfDay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class CalendarEvent(
    val blockId: String,
    val noteId: String,
    val text: String,
    val isChecked: Boolean,
    val dateString: String,
    val occurrenceDateString: String,
    val reminderTimestamp: Long,
    val categoryId: String?,
    val durationMinutes: Int,
    val sourceType: TaskSource,
    val url: String?,
    val description: String?,
    val recurrenceRule: RecurrenceRule?,
    val allDay: AllDaySpan?
)

fun CalendarEvent.lastShownDate(): LocalDate {
    val startDate = LocalDate.parse(dateString)
    return allDay?.lastDate(startDate) ?: startDate
}

fun CalendarEvent.datesShownOn(): List<LocalDate> {
    val lastDate = lastShownDate()
    return generateSequence(LocalDate.parse(dateString)) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= lastDate }.toList()
}

fun CalendarEvent.timeOfDayLabel(usesTwentyFourHourClock: Boolean): String {
    if (allDay != null) return "All day"
    val startTime = Instant.fromEpochMilliseconds(reminderTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    return formatTimeOfDay(startTime.hour, startTime.minute, usesTwentyFourHourClock)
}

fun CalendarTaskEntity.toCalendarEvent(): CalendarEvent? {
    val timestamp = reminderTimestamp ?: return null
    val occurrenceDate = targetDate ?: return null
    val shownOnDate = shownOnDateString() ?: return null
    return CalendarEvent(
        blockId = blockId,
        noteId = noteId,
        text = text,
        isChecked = isChecked,
        dateString = shownOnDate,
        occurrenceDateString = occurrenceDate,
        reminderTimestamp = timestamp,
        categoryId = categoryId,
        durationMinutes = durationMinutes,
        sourceType = sourceType,
        url = url,
        description = description,
        recurrenceRule = toRecurrenceRule(),
        allDay = toAllDaySpan()
    )
}
