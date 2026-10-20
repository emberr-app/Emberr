package com.emberr.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

const val DEFAULT_ALL_DAY_REMINDER_HOUR = 9

@Serializable
data class AllDaySpan(
    val lengthInDays: Int = 1,
    val isReminderOn: Boolean = true
)

fun AllDaySpan.lastDate(startDate: LocalDate): LocalDate =
    startDate.plus(lengthInDays.coerceAtLeast(1) - 1, DateTimeUnit.DAY)
