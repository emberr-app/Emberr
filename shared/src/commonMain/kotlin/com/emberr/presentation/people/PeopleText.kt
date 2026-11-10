package com.emberr.presentation.people

import androidx.compose.runtime.Composable
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.shownOnDateString
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.people.ContactImportProgress
import com.emberr.domain.people.ContactImportResult
import com.emberr.domain.people.PersonBirthday
import com.emberr.domain.util.time.formatTimeOfDay
import com.emberr.presentation.calendar.formatDuration
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.people_all_already_in_people
import emberr.shared.generated.resources.people_all_day
import emberr.shared.generated.resources.people_already_in_people
import emberr.shared.generated.resources.people_birthday_with_age
import emberr.shared.generated.resources.people_could_not_add_contacts
import emberr.shared.generated.resources.people_could_not_read_file
import emberr.shared.generated.resources.people_count
import emberr.shared.generated.resources.people_event_count
import emberr.shared.generated.resources.people_event_with_names
import emberr.shared.generated.resources.people_import_added
import emberr.shared.generated.resources.people_import_could_not_add
import emberr.shared.generated.resources.people_import_skipped_already_in_people
import emberr.shared.generated.resources.people_importing_contacts
import emberr.shared.generated.resources.people_importing_contacts_progress
import emberr.shared.generated.resources.people_meta_last_seen
import emberr.shared.generated.resources.people_meta_next
import emberr.shared.generated.resources.people_meta_next_and_last_seen
import emberr.shared.generated.resources.people_meta_no_events
import emberr.shared.generated.resources.people_no_contacts_found
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant
import com.emberr.domain.util.time.formatDate
import com.emberr.domain.util.time.DateTextStyle

private const val LEAP_YEAR_FOR_BIRTHDAY_TEXT = 2000

@Composable
fun personDetailSearchLine(detail: PersonDetailBlock): String {
    val heading = detail.label?.trim()?.takeIf { it.isNotEmpty() } ?: detail.kind.displayName()
    return "$heading · ${detail.value}"
}

fun personFirstNameOrNull(title: String): String? = title.trim().split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotEmpty() }

fun personGroupLetter(title: String): String {
    val firstCharacter = title.trim().firstOrNull() ?: return "#"
    return if (firstCharacter.isLetter()) firstCharacter.uppercase() else "#"
}

suspend fun contactImportMessage(result: ContactImportResult): String {
    val added = result.importedPersonNoteIds.size
    val skipped = result.copiesSkipped
    if (result.couldNotReadFile) return getString(Res.string.people_could_not_read_file)
    if (result.foundNoContacts) return getString(Res.string.people_no_contacts_found)
    if (added == 0 && result.failures > 0) return getString(Res.string.people_could_not_add_contacts)
    if (added == 0) {
        return if (skipped == 1) getString(Res.string.people_already_in_people) else getPluralString(Res.plurals.people_all_already_in_people, skipped, skipped)
    }
    val parts = mutableListOf(getPluralString(Res.plurals.people_import_added, added, added))
    if (skipped > 0) parts += getPluralString(Res.plurals.people_import_skipped_already_in_people, skipped, skipped)
    if (result.failures > 0) parts += getPluralString(Res.plurals.people_import_could_not_add, result.failures, result.failures)
    return parts.joinToString(", ")
}

@Composable
fun importingContactsLabel(progress: ContactImportProgress.Importing): String =
    if (progress.totalCount == 0) {
        stringResource(Res.string.people_importing_contacts)
    } else {
        stringResource(Res.string.people_importing_contacts_progress, progress.handledCount, progress.totalCount)
    }

@Composable
fun peopleCountLabel(count: Int): String = pluralStringResource(Res.plurals.people_count, count, count)

@Composable
fun eventCountLabel(count: Int): String = pluralStringResource(Res.plurals.people_event_count, count, count)

internal fun birthdayMonthDay(month: Int, day: Int): String =
    formatDate(LocalDate(LEAP_YEAR_FOR_BIRTHDAY_TEXT, month, day), DateTextStyle.FULL_MONTH_DAY)

fun weekdayMonthDay(date: LocalDate, today: LocalDate): String {
    val style = if (date.year == today.year) {
        DateTextStyle.SHORT_WEEKDAY_SHORT_MONTH_DAY
    } else {
        DateTextStyle.SHORT_WEEKDAY_SHORT_MONTH_DAY_YEAR
    }
    return formatDate(date, style)
}

fun monthDay(date: LocalDate, today: LocalDate): String {
    val style = if (date.year == today.year) DateTextStyle.SHORT_MONTH_DAY else DateTextStyle.SHORT_MONTH_DAY_YEAR
    return formatDate(date, style)
}

fun eventDate(event: CalendarTaskEntity): LocalDate? =
    event.shownOnDateString()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

@Composable
fun personMetaLine(nextEvent: CalendarTaskEntity?, lastEvent: CalendarTaskEntity?, today: LocalDate): String {
    val nextDate = nextEvent?.let(::eventDate)
    val lastDate = lastEvent?.let(::eventDate)
    return when {
        nextDate != null && lastDate != null -> stringResource(
            Res.string.people_meta_next_and_last_seen,
            weekdayMonthDay(nextDate, today),
            monthDay(lastDate, today)
        )
        nextDate != null -> stringResource(Res.string.people_meta_next, weekdayMonthDay(nextDate, today))
        lastDate != null -> stringResource(Res.string.people_meta_last_seen, monthDay(lastDate, today))
        else -> stringResource(Res.string.people_meta_no_events)
    }
}

@Composable
fun birthdayText(birthday: PersonBirthday, today: LocalDate): String {
    val monthAndDay = birthdayMonthDay(birthday.month, birthday.day)
    val birthYear = birthday.year ?: return monthAndDay
    val hasBirthdayPassedThisYear = birthday.month < today.month.number ||
        (birthday.month == today.month.number && birthday.day < today.day)
    val yearOfNextBirthday = if (hasBirthdayPassedThisYear) today.year + 1 else today.year
    return stringResource(Res.string.people_birthday_with_age, monthAndDay, birthYear, yearOfNextBirthday - birthYear)
}

@Composable
fun eventTimeLine(event: CalendarTaskEntity, otherPeopleNames: List<String>, usesTwentyFourHourClock: Boolean): String {
    val timeParts = when {
        event.isAllDay -> listOf(stringResource(Res.string.people_all_day))
        event.reminderTimestamp != null -> {
            val start = Instant.fromEpochMilliseconds(event.reminderTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            listOf(formatTimeOfDay(start.hour, start.minute, usesTwentyFourHourClock), formatDuration(event.durationMinutes))
        }
        else -> emptyList()
    }
    val withPart = if (otherPeopleNames.isEmpty()) null else stringResource(Res.string.people_event_with_names, otherPeopleNames.joinToString(", "))
    return (timeParts + listOfNotNull(withPart)).joinToString(" · ")
}
