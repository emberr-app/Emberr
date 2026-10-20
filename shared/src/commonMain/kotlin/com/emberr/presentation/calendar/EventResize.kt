package com.emberr.presentation.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.emberr.domain.util.time.formatTimeOfDay
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
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
    other != null && blockId == other.blockId && dateString == other.dateString

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

private fun snapToResizeStep(minuteOfDay: Float): Int =
    (minuteOfDay / EventResizeSnapMinutes).roundToInt() * EventResizeSnapMinutes

fun CalendarEvent.timeRange(): EventTimeRange {
    val start = Instant.fromEpochMilliseconds(reminderTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    return EventTimeRange(startMinuteOfDay = start.hour * 60 + start.minute, durationMinutes = durationMinutes)
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
