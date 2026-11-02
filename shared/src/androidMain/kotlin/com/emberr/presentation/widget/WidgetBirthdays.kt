package com.emberr.presentation.widget

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.dao.CategoryDao
import com.emberr.domain.people.BIRTHDAYS_CATEGORY_ID
import com.emberr.domain.people.PeopleRepository
import com.emberr.domain.people.PersonWithDetails
import com.emberr.domain.people.birthdaySummaryOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WidgetBirthdays(
    private val peopleRepository: PeopleRepository,
    private val categoryDao: CategoryDao,
    private val settingsManager: SettingsManager
) {
    fun changes(): Flow<Unit> =
        combine(
            peopleRepository.observePeopleWithDetails().map(::birthdaySummaryOf),
            categoryDao.observeCategoryById(BIRTHDAYS_CATEGORY_ID),
            settingsManager.hiddenCalendarCategoriesFlow.map { hiddenKeys -> BIRTHDAYS_CATEGORY_ID in hiddenKeys }
        ) { birthdays, birthdaysCategory, areBirthdaysHidden -> Triple(birthdays, birthdaysCategory, areBirthdaysHidden) }
            .distinctUntilChanged()
            .map { }

    suspend fun readPeople(): List<PersonWithDetails> =
        try {
            peopleRepository.observePeopleWithDetails().first()
        } catch (cause: Exception) {
            WidgetLog.e("Could not read people for birthdays", cause)
            emptyList()
        }

    fun areBirthdaysHidden(): Boolean = BIRTHDAYS_CATEGORY_ID in settingsManager.getHiddenCalendarCategories()
}
