package com.emberr.presentation.calendar

fun noCategoryVisibilityKeyFor(spaceId: String): String = "no_category_$spaceId"

data class CalendarCategoryVisibility(
    val hiddenVisibilityKeys: Set<String>,
    val existingCategoryIds: Set<String>,
    val spaceId: String
) {
    fun isEventShown(categoryId: String?): Boolean = visibilityKeyFor(categoryId) !in hiddenVisibilityKeys

    private fun visibilityKeyFor(categoryId: String?): String =
        if (categoryId != null && categoryId in existingCategoryIds) categoryId else noCategoryVisibilityKeyFor(spaceId)
}
