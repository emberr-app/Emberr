package com.emberr.presentation.calendar

import com.emberr.data.local.room.entity.TaskSource
import com.emberr.domain.model.RecurrenceEditScope
import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class EventEditorSaveTest {

    private val movedWeeklyOccurrence = CalendarEvent(
        blockId = "block-1",
        noteId = "note-1",
        text = "Standup",
        isChecked = false,
        dateString = "2026-10-23",
        occurrenceDateString = "2026-10-22",
        reminderTimestamp = LocalDateTime(2026, 10, 23, 9, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
        categoryId = null,
        durationMinutes = 30,
        sourceType = TaskSource.DAILY,
        url = null,
        description = null,
        recurrenceRule = RecurrenceRule(frequency = RecurrenceFrequency.WEEKLY),
        allDay = null
    )

    @Test
    fun savingOneOccurrenceShowsTheNewValuesButKeepsItsOccurrenceDate() {
        val editedState = movedWeeklyOccurrence.toEditorState().copy(
            name = "Team standup",
            isEditing = true,
            editScope = RecurrenceEditScope.THIS_EVENT
        )

        val viewState = editedState.viewAfterSaving()!!

        assertFalse(viewState.isEditing)
        assertEquals("Team standup", viewState.name)
        assertEquals(LocalDate(2026, 10, 23), viewState.date)
        assertEquals("2026-10-22", viewState.original?.occurrenceDateString)
    }

    @Test
    fun savingAllFutureOccurrencesClosesTheSheetBecauseTheSeriesSplits() {
        val editedState = movedWeeklyOccurrence.toEditorState().copy(
            name = "Team standup",
            isEditing = true,
            editScope = RecurrenceEditScope.ALL_FUTURE_EVENTS
        )

        assertNull(editedState.viewAfterSaving())
    }
}
