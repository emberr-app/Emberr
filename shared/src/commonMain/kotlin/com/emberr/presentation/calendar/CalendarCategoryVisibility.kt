package com.emberr.presentation.calendar

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.TaskSource

fun noCategoryVisibilityKeyFor(spaceId: String): String = "no_category_$spaceId"

fun spaceVisibilityKeyFor(spaceId: String): String = "space_$spaceId"

data class CalendarCategoryVisibility(
    val hiddenVisibilityKeys: Set<String>,
    val existingCategoryIds: Set<String>
) {
    fun isEventShown(task: CalendarTaskEntity): Boolean =
        categoryVisibilityKeyFor(task.categoryId) !in hiddenVisibilityKeys && !isHiddenBySpace(task)

    private fun isHiddenBySpace(task: CalendarTaskEntity): Boolean =
        task.sourceType == TaskSource.NOTE && spaceVisibilityKeyFor(task.spaceId) in hiddenVisibilityKeys

    private fun categoryVisibilityKeyFor(categoryId: String?): String =
        if (categoryId != null && categoryId in existingCategoryIds) categoryId else noCategoryVisibilityKeyFor(ACCOUNT_SPACE_ID)
}
