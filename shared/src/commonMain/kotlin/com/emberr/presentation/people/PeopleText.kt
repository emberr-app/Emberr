package com.emberr.presentation.people

import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.shownOnDateString
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.people.PersonBirthday
import com.emberr.domain.util.time.formatTimeOfDay
import com.emberr.presentation.calendar.formatDuration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val SHORT_NAME_LENGTH = 3

fun personDetailSearchLine(detail: PersonDetailBlock): String {
    val heading = detail.label?.trim()?.takeIf { it.isNotEmpty() } ?: detail.kind.label
    return "$heading · ${detail.value}"
}

fun personFirstNameOrThem(title: String): String = title.trim().split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotEmpty() } ?: "them"

fun personGroupLetter(title: String): String {
    val firstCharacter = title.trim().firstOrNull() ?: return "#"
    return if (firstCharacter.isLetter()) firstCharacter.uppercase() else "#"
}

fun peopleCountLabel(count: Int): String = if (count == 1) "1 person" else "$count people"

fun eventCountLabel(count: Int): String = if (count == 1) "1 event" else "$count events"

fun shortWeekdayName(date: LocalDate): String =
    date.dayOfWeek.name.take(SHORT_NAME_LENGTH).lowercase().replaceFirstChar(Char::uppercase)

fun shortMonthName(date: LocalDate): String =
    date.month.name.take(SHORT_NAME_LENGTH).lowercase().replaceFirstChar(Char::uppercase)

internal fun fullMonthName(monthNumber: Int): String =
    Month(monthNumber).name.lowercase().replaceFirstChar(Char::uppercase)

fun weekdayMonthDay(date: LocalDate, today: LocalDate): String {
    val text = "${shortWeekdayName(date)}, ${shortMonthName(date)} ${date.day}"
    return if (date.year == today.year) text else "$text, ${date.year}"
}

fun monthDay(date: LocalDate, today: LocalDate): String {
    val text = "${shortMonthName(date)} ${date.day}"
    return if (date.year == today.year) text else "$text, ${date.year}"
}

fun eventDate(event: CalendarTaskEntity): LocalDate? =
    event.shownOnDateString()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun personMetaLine(nextEvent: CalendarTaskEntity?, lastEvent: CalendarTaskEntity?, today: LocalDate): String {
    val nextDate = nextEvent?.let(::eventDate)
    val lastDate = lastEvent?.let(::eventDate)
    return when {
        nextDate != null && lastDate != null -> "Next on ${weekdayMonthDay(nextDate, today)} · last seen ${monthDay(lastDate, today)}"
        nextDate != null -> "Next on ${weekdayMonthDay(nextDate, today)}"
        lastDate != null -> "Last seen ${monthDay(lastDate, today)}"
        else -> "No events yet"
    }
}

fun birthdayText(birthday: PersonBirthday, today: LocalDate): String {
    val monthAndDay = "${fullMonthName(birthday.month)} ${birthday.day}"
    val birthYear = birthday.year ?: return monthAndDay
    val hasBirthdayPassedThisYear = birthday.month < today.month.number ||
        (birthday.month == today.month.number && birthday.day < today.day)
    val yearOfNextBirthday = if (hasBirthdayPassedThisYear) today.year + 1 else today.year
    return "$monthAndDay, $birthYear · turns ${yearOfNextBirthday - birthYear}"
}

fun eventTimeLine(event: CalendarTaskEntity, otherPeopleNames: List<String>, usesTwentyFourHourClock: Boolean): String {
    val timeParts = when {
        event.isAllDay -> listOf("All day")
        event.reminderTimestamp != null -> {
            val start = Instant.fromEpochMilliseconds(event.reminderTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            listOf(formatTimeOfDay(start.hour, start.minute, usesTwentyFourHourClock), formatDuration(event.durationMinutes))
        }
        else -> emptyList()
    }
    val withPart = otherPeopleNames.takeIf { it.isNotEmpty() }?.let { names -> "with ${names.joinToString(", ")}" }
    return (timeParts + listOfNotNull(withPart)).joinToString(" · ")
}
