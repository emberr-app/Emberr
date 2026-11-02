package com.emberr.presentation.calendar

import com.emberr.data.local.room.entity.TaskSource
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.people.BIRTHDAYS_CATEGORY_ID
import com.emberr.domain.people.BirthdayOnDate
import com.emberr.domain.people.PersonWithDetails
import com.emberr.domain.people.birthdaysInRange
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

fun nextBirthdayEvents(people: List<PersonWithDetails>, today: LocalDate): List<CalendarEvent> =
    birthdaysInRange(people, today, today.plus(1, DateTimeUnit.YEAR))
        .distinctBy { birthday -> birthday.personNoteId }
        .map { birthday -> birthday.toCalendarEvent() }

fun BirthdayOnDate.toCalendarEvent(): CalendarEvent {
    val name = personName.trim().ifBlank { "Untitled" }
    val title = if (turningAge == null) "$name's birthday" else "$name's birthday (turns $turningAge)"
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
