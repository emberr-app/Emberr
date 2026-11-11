package com.emberr.presentation.calendar

import androidx.compose.runtime.Composable
import com.emberr.domain.util.search.containsForSearch
import kotlinx.datetime.LocalDate

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
    today: LocalDate,
    birthdaysWord: String
): CalendarSearchResults {
    val categoryNamesById = categories.associate { it.id to it.name }
    val matchingEvents = events.filter { event ->
        listOfNotNull(
            event.text,
            event.description,
            event.url,
            categoryNamesById[event.categoryId],
            birthdaysWord.takeIf { event.isBirthday }
        )
            .any { it.containsForSearch(query) }
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

@Composable
fun CalendarEvent.searchResultDateLabel(usesTwentyFourHourClock: Boolean): String =
    "${formatFullDate(LocalDate.parse(dateString))} · ${timeOfDayLabel(usesTwentyFourHourClock)}"

fun CalendarEvent.matchingDetail(query: String): String? =
    listOfNotNull(description, url).firstOrNull { it.containsForSearch(query) }
