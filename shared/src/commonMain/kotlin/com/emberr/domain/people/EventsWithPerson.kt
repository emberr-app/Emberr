package com.emberr.domain.people

import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.datesShownOn
import com.emberr.data.local.room.entity.personNoteIdList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val MILLIS_PER_MINUTE = 60_000L

data class EventsWithPerson(
    val upcoming: List<CalendarTaskEntity>,
    val past: List<CalendarTaskEntity>
)

fun eventsWithPerson(
    personNoteId: String,
    eventsAndOccurrences: List<CalendarTaskEntity>,
    now: Instant,
    timeZone: TimeZone
): EventsWithPerson {
    val today = now.toLocalDateTime(timeZone).date
    val eventsWithThisPerson = eventsAndOccurrences.filter { event -> personNoteId in event.personNoteIdList() }
    val (upcoming, past) = eventsWithThisPerson.partition { event -> isStillAhead(event, now, today) }
    return EventsWithPerson(
        upcoming = upcoming
            .sortedWith(compareBy(nullsLast<Long>()) { event -> startMillisOf(event, timeZone) })
            .distinctBy { event -> event.blockId },
        past = past.sortedByDescending { event -> startMillisOf(event, timeZone) }
    )
}

private fun isStillAhead(event: CalendarTaskEntity, now: Instant, today: LocalDate): Boolean {
    if (event.isAllDay) {
        val lastDate = event.datesShownOn().lastOrNull() ?: return true
        return lastDate >= today
    }
    val reminderTimestamp = event.reminderTimestamp
    if (reminderTimestamp != null) {
        return reminderTimestamp + event.durationMinutes * MILLIS_PER_MINUTE > now.toEpochMilliseconds()
    }
    val targetDate = event.targetDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return true
    return targetDate >= today
}

private fun startMillisOf(event: CalendarTaskEntity, timeZone: TimeZone): Long? =
    event.reminderTimestamp
        ?: event.targetDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.atStartOfDayIn(timeZone)?.toEpochMilliseconds()
