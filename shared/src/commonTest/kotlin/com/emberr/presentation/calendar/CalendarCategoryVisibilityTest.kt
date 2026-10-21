package com.emberr.presentation.calendar

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.TaskSource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarCategoryVisibilityTest {

    private fun taskIn(spaceId: String, sourceType: TaskSource, categoryId: String?) = CalendarTaskEntity(
        blockId = "$spaceId-$categoryId",
        noteId = "note",
        text = "Event",
        isChecked = false,
        targetDate = "2026-10-21",
        reminderTimestamp = 0L,
        sourceType = sourceType,
        categoryId = categoryId,
        spaceId = spaceId
    )

    @Test
    fun eventsWhoseCategoryWasDeletedFollowTheNoCategoryToggle() {
        val visibility = CalendarCategoryVisibility(
            hiddenVisibilityKeys = setOf(noCategoryVisibilityKeyFor(ACCOUNT_SPACE_ID)),
            existingCategoryIds = setOf("work")
        )

        assertTrue(visibility.isEventShown(taskIn("space-a", TaskSource.NOTE, "work")))
        assertFalse(visibility.isEventShown(taskIn("space-a", TaskSource.NOTE, null)))
        assertFalse(visibility.isEventShown(taskIn("space-a", TaskSource.NOTE, "deleted-category")))
    }

    @Test
    fun hidingASpaceHidesOnlyThatSpacesNoteEvents() {
        val visibility = CalendarCategoryVisibility(
            hiddenVisibilityKeys = setOf(spaceVisibilityKeyFor("space-w")),
            existingCategoryIds = setOf("work")
        )

        assertFalse(visibility.isEventShown(taskIn("space-w", TaskSource.NOTE, "work")))
        assertTrue(visibility.isEventShown(taskIn("space-p", TaskSource.NOTE, "work")))
        assertTrue(visibility.isEventShown(taskIn(ACCOUNT_SPACE_ID, TaskSource.DAILY, "work")))
    }
}
