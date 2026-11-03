package com.emberr.presentation.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.emberr.domain.util.time.formatTimeOfDay
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

const val EventResizeSnapMinutes = 5
const val MinimumEventDurationMinutes = 15
private const val MinutesInDay = 24 * 60

enum class EventResizeEdge { TOP, BOTTOM }

data class EventTimeRange(val startMinuteOfDay: Int, val durationMinutes: Int) {
    val endMinuteOfDay: Int get() = startMinuteOfDay + durationMinutes
}

data class EventResizePreview(val event: CalendarEvent, val timeRange: EventTimeRange)

class EventResizeState {
    var eventInResizeMode by mutableStateOf<CalendarEvent?>(null)
    var preview by mutableStateOf<EventResizePreview?>(null)
}

fun CalendarEvent.isSameOccurrenceAs(other: CalendarEvent?): Boolean =
    other != null && blockId == other.blockId && occurrenceDateString == other.occurrenceDateString

fun resizeEventTimeRange(original: EventTimeRange, edge: EventResizeEdge, draggedMinutes: Float): EventTimeRange =
    when (edge) {
        EventResizeEdge.TOP -> {
            val latestStart = (original.endMinuteOfDay - MinimumEventDurationMinutes)
                .coerceIn(0, MinutesInDay - MinimumEventDurationMinutes)
            val newStart = snapToResizeStep(original.startMinuteOfDay + draggedMinutes).coerceIn(0, latestStart)
            EventTimeRange(startMinuteOfDay = newStart, durationMinutes = original.endMinuteOfDay - newStart)
        }
        EventResizeEdge.BOTTOM -> {
            val earliestEnd = original.startMinuteOfDay + MinimumEventDurationMinutes
            val latestEnd = maxOf(MinutesInDay, earliestEnd)
            val newEnd = snapToResizeStep(original.endMinuteOfDay + draggedMinutes).coerceIn(earliestEnd, latestEnd)
            original.copy(durationMinutes = newEnd - original.startMinuteOfDay)
        }
    }

fun moveEventTimeRange(original: EventTimeRange, draggedMinutes: Float): EventTimeRange {
    val latestStart = maxOf(MinutesInDay - original.durationMinutes, original.startMinuteOfDay).coerceAtLeast(0)
    val newStart = snapToResizeStep(original.startMinuteOfDay + draggedMinutes).coerceIn(0, latestStart)
    return original.copy(startMinuteOfDay = newStart)
}

private fun snapToResizeStep(minuteOfDay: Float): Int =
    (minuteOfDay / EventResizeSnapMinutes).roundToInt() * EventResizeSnapMinutes

fun CalendarEvent.timeRange(): EventTimeRange {
    val start = Instant.fromEpochMilliseconds(reminderTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    return EventTimeRange(startMinuteOfDay = start.hour * 60 + start.minute, durationMinutes = durationMinutes)
}

val EventTimeRange.crossesMidnight: Boolean get() = endMinuteOfDay > MinutesInDay

fun EventTimeRange.partOnDay(daysAfterStart: Int): EventTimeRange = when {
    daysAfterStart > 0 -> EventTimeRange(startMinuteOfDay = 0, durationMinutes = (endMinuteOfDay - MinutesInDay).coerceAtLeast(0))
    crossesMidnight -> copy(durationMinutes = MinutesInDay - startMinuteOfDay)
    else -> this
}

fun CalendarEvent.crossesMidnight(): Boolean = allDay == null && timeRange().crossesMidnight

fun CalendarEvent.continuesInto(date: LocalDate): Boolean =
    crossesMidnight() && LocalDate.parse(dateString).plus(1, DateTimeUnit.DAY) == date

fun CalendarEvent.daysAfterStart(date: LocalDate): Int = if (continuesInto(date)) 1 else 0

fun resizeOvernightEventEnd(original: EventTimeRange, draggedMinutes: Float): EventTimeRange {
    val endOnNextDay = original.endMinuteOfDay - MinutesInDay
    val latestEndOnNextDay = maxOf(MinimumEventDurationMinutes, original.startMinuteOfDay)
    val newEndOnNextDay = snapToResizeStep(endOnNextDay + draggedMinutes).coerceIn(MinimumEventDurationMinutes, latestEndOnNextDay)
    return original.copy(durationMinutes = MinutesInDay + newEndOnNextDay - original.startMinuteOfDay)
}

data class MovedOvernightEvent(val daysLater: Int, val timeRange: EventTimeRange)

fun moveOvernightEvent(original: EventTimeRange, draggedMinutes: Float): MovedOvernightEvent {
    val newStart = snapToResizeStep(original.startMinuteOfDay + draggedMinutes)
        .coerceIn(0, MinutesInDay * 2 - EventResizeSnapMinutes)
    return MovedOvernightEvent(
        daysLater = newStart / MinutesInDay,
        timeRange = original.copy(startMinuteOfDay = newStart % MinutesInDay)
    )
}

fun timestampOnDate(dateString: String, minuteOfDay: Int): Long =
    LocalDateTime(LocalDate.parse(dateString), LocalTime(minuteOfDay / 60, minuteOfDay % 60))
        .toInstant(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()

fun formatEventTimeRange(timeRange: EventTimeRange, usesTwentyFourHourClock: Boolean): String {
    val start = timeRange.startMinuteOfDay
    val end = timeRange.endMinuteOfDay % MinutesInDay
    val startText = formatTimeOfDay(start / 60, start % 60, usesTwentyFourHourClock)
    val endText = formatTimeOfDay(end / 60, end % 60, usesTwentyFourHourClock)
    return "$startText - $endText".lowercase()
}
