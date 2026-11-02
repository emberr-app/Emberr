package com.emberr.presentation.calendar

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.TaskSource
import com.emberr.domain.people.BIRTHDAYS_CATEGORY_ID

const val NO_CATEGORY_VISIBILITY_KEY = "no_category_$ACCOUNT_SPACE_ID"

fun spaceVisibilityKeyFor(spaceId: String): String = "space_$spaceId"

data class CalendarCategoryVisibility(
    val hiddenVisibilityKeys: Set<String>,
    val existingCategoryIds: Set<String>
) {
    fun isEventShown(task: CalendarTaskEntity): Boolean =
        categoryVisibilityKeyFor(task.categoryId) !in hiddenVisibilityKeys && !isHiddenBySpace(task)

    val areBirthdaysShown: Boolean
        get() = BIRTHDAYS_CATEGORY_ID !in hiddenVisibilityKeys

    private fun isHiddenBySpace(task: CalendarTaskEntity): Boolean =
        task.sourceType == TaskSource.NOTE && spaceVisibilityKeyFor(task.spaceId) in hiddenVisibilityKeys

    private fun categoryVisibilityKeyFor(categoryId: String?): String =
        if (categoryId != null && categoryId in existingCategoryIds) categoryId else NO_CATEGORY_VISIBILITY_KEY
}
