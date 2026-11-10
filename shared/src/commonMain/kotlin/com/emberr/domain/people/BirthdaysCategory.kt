package com.emberr.domain.people

import com.emberr.data.local.room.dao.CategoryDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CategoryEntity
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.sync.AutoSyncTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock

const val BIRTHDAYS_CATEGORY_ID = "birthdays"
const val BIRTHDAYS_CATEGORY_DEFAULT_NAME = "Birthdays"
private const val BIRTHDAYS_CATEGORY_DEFAULT_COLOR_HEX = "#FFC6FF"

fun birthdaysCategoryRowToSave(existingRow: CategoryEntity?, nowMillis: Long): CategoryEntity? = when {
    existingRow == null -> CategoryEntity(
        categoryId = BIRTHDAYS_CATEGORY_ID,
        name = BIRTHDAYS_CATEGORY_DEFAULT_NAME,
        colorHex = BIRTHDAYS_CATEGORY_DEFAULT_COLOR_HEX,
        createdAt = nowMillis,
        updatedAt = 0L,
        isDeleted = false,
        spaceId = ACCOUNT_SPACE_ID
    )
    existingRow.isDeleted -> existingRow.copy(isDeleted = false, updatedAt = nowMillis)
    else -> null
}

fun PersonWithDetails.hasBirthday(): Boolean =
    details.any { detail -> detail.kind == PersonDetailKind.BIRTHDAY && readPersonBirthday(detail.value) != null }

class BirthdaysCategoryKeeper(
    private val peopleRepository: PeopleRepository,
    private val categoryDao: CategoryDao,
    private val appScope: CoroutineScope
) {
    private var keepingJob: Job? = null

    fun start() {
        if (keepingJob != null) return
        keepingJob = appScope.launch {
            try {
                val someoneHasABirthday = peopleRepository.observePeopleWithDetails()
                    .map { people -> people.any { person -> person.hasBirthday() } }
                    .distinctUntilChanged()
                combine(someoneHasABirthday, categoryDao.observeCategoryById(BIRTHDAYS_CATEGORY_ID)) { needsCategory, existingRow ->
                    if (needsCategory) birthdaysCategoryRowToSave(existingRow, Clock.System.now().toEpochMilliseconds()) else null
                }.collect { rowToSave ->
                    if (rowToSave != null) saveBirthdaysCategory(rowToSave)
                }
            } catch (cause: Exception) {
                cause.printStackTrace()
            }
        }
    }

    private suspend fun saveBirthdaysCategory(row: CategoryEntity) {
        try {
            categoryDao.insertOrUpdateCategory(row)
            AutoSyncTrigger.requestSync()
        } catch (cause: Exception) {
            cause.printStackTrace()
        }
    }
}
