package com.emberr.presentation.calendar

import com.emberr.domain.util.time.formatTimeOfDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class CalendarSearchResults(
    val upcomingEvents: List<CalendarEvent>,
    val pastEvents: List<CalendarEvent>
) {
    val isEmpty: Boolean get() = upcomingEvents.isEmpty() && pastEvents.isEmpty()
}

fun searchCalendarEvents(
    events: List<CalendarEvent>,
    categories: List<CalendarCategory>,
    query: String,
    today: LocalDate
): CalendarSearchResults {
    val categoryNamesById = categories.associate { it.id to it.name }
    val matchingEvents = events.filter { event ->
        listOfNotNull(event.text, event.description, event.url, categoryNamesById[event.categoryId])
            .any { it.contains(query, ignoreCase = true) }
    }
    val (upcomingEvents, pastEvents) = matchingEvents.partition { it.dateString >= today.toString() }
    return CalendarSearchResults(
        upcomingEvents = upcomingEvents.sortedBy { it.reminderTimestamp },
        pastEvents = pastEvents.sortedByDescending { it.reminderTimestamp }
    )
}

data class CalendarEventSearchResult(
    val event: CalendarEvent,
    val category: CalendarCategory?
)

fun CalendarEvent.searchResultDateLabel(usesTwentyFourHourClock: Boolean): String {
    val startTime = Instant.fromEpochMilliseconds(reminderTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    val timeLabel = formatTimeOfDay(startTime.hour, startTime.minute, usesTwentyFourHourClock)
    return "${formatFullDate(LocalDate.parse(dateString))} · $timeLabel"
}

fun CalendarEvent.matchingDetail(query: String): String? =
    listOfNotNull(description, url).firstOrNull { it.contains(query, ignoreCase = true) }
