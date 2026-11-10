package com.emberr.presentation.calendar

import com.emberr.data.local.room.entity.TaskSource
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.people.BIRTHDAYS_CATEGORY_ID
import com.emberr.domain.people.BirthdayOnDate
import com.emberr.domain.people.PersonWithDetails
import com.emberr.domain.people.birthdaysInRange
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.calendar_birthday_title
import emberr.shared.generated.resources.calendar_birthday_title_turning
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.getString
import emberr.shared.generated.resources.common_untitled

suspend fun nextBirthdayEvents(people: List<PersonWithDetails>, today: LocalDate): List<CalendarEvent> =
    birthdaysInRange(people, today, today.plus(1, DateTimeUnit.YEAR))
        .distinctBy { birthday -> birthday.personNoteId }
        .map { birthday -> birthday.toCalendarEvent() }

suspend fun BirthdayOnDate.toCalendarEvent(): CalendarEvent {
    val name = personName.trim().ifEmpty { getString(Res.string.common_untitled) }
    val title = if (turningAge == null) {
        getString(Res.string.calendar_birthday_title, name)
    } else {
        getString(Res.string.calendar_birthday_title_turning, name, turningAge)
    }
    return CalendarEvent(
        blockId = "birthday_${personNoteId}_${date.year}",
        noteId = personNoteId,
        text = title,
        isChecked = false,
        dateString = date.toString(),
        occurrenceDateString = date.toString(),
        reminderTimestamp = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
        categoryId = BIRTHDAYS_CATEGORY_ID,
        durationMinutes = 0,
        sourceType = TaskSource.DAILY,
        url = null,
        description = null,
        recurrenceRule = null,
        allDay = AllDaySpan(lengthInDays = 1, isReminderOn = false),
        birthdayPersonNoteId = personNoteId
    )
}
