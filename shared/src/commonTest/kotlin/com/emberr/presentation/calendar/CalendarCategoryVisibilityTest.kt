package com.emberr.presentation.calendar

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarCategoryVisibilityTest {

    @Test
    fun eventsWhoseCategoryWasDeletedFollowTheNoCategoryToggle() {
        val visibility = CalendarCategoryVisibility(
            hiddenVisibilityKeys = setOf(noCategoryVisibilityKeyFor("space-a")),
            existingCategoryIds = setOf("work"),
            spaceId = "space-a"
        )

        assertTrue(visibility.isEventShown("work"))
        assertFalse(visibility.isEventShown(null))
        assertFalse(visibility.isEventShown("deleted-category"))
    }

    @Test
    fun hidingNoCategoryInOneSpaceKeepsItShownInAnother() {
        val visibility = CalendarCategoryVisibility(
            hiddenVisibilityKeys = setOf(noCategoryVisibilityKeyFor("space-a")),
            existingCategoryIds = emptySet(),
            spaceId = "space-b"
        )

        assertTrue(visibility.isEventShown(null))
    }
}
