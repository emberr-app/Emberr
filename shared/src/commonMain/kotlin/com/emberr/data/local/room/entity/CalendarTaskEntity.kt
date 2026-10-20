package com.emberr.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.lastDate
import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import com.emberr.domain.model.isoDayNumberToDayOfWeek
import com.emberr.domain.model.toIsoDayNumberCsv
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "calendar_tasks",
    indices = [
        Index(value = ["spaceId", "targetDate"]),
        Index("noteId")
    ]
)
data class CalendarTaskEntity(
    @PrimaryKey val blockId: String,
    val noteId: String,
    val text: String,
    val isChecked: Boolean,
    val targetDate: String?,
    val reminderTimestamp: Long?,
    val sourceType: TaskSource,
    val categoryId: String? = null,
    @ColumnInfo(defaultValue = "30") val durationMinutes: Int = 30,
    val url: String? = null,
    val description: String? = null,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    @ColumnInfo(defaultValue = "1") val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: String? = null,
    val recurrenceUntil: String? = null,
    val spaceId: String = DEFAULT_SPACE_ID,
    @ColumnInfo(defaultValue = "0") val isAllDay: Boolean = false,
    @ColumnInfo(defaultValue = "1") val allDayLengthInDays: Int = 1,
    @ColumnInfo(defaultValue = "1") val isAllDayReminderOn: Boolean = true
)

fun CalendarTaskEntity.toAllDaySpan(): AllDaySpan? =
    if (isAllDay) AllDaySpan(lengthInDays = allDayLengthInDays, isReminderOn = isAllDayReminderOn) else null

fun CalendarTaskEntity.datesShownOn(): List<LocalDate> {
    val firstDate = shownOnDateString()?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return emptyList()
    val lastDate = toAllDaySpan()?.lastDate(firstDate) ?: firstDate
    return generateSequence(firstDate) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= lastDate }.toList()
}

fun CalendarTaskEntity.toRecurrenceRule(): RecurrenceRule? {
    val frequency = recurrenceFrequency ?: return null
    return RecurrenceRule(
        frequency = frequency,
        interval = recurrenceInterval,
        daysOfWeek = recurrenceDaysOfWeek
            ?.split(",")
            ?.mapNotNull { isoDayNumberToDayOfWeek(it.trim().toIntOrNull()) }
            ?.toSet()
            ?: emptySet(),
        untilDateString = recurrenceUntil
    )
}

fun CalendarTaskEntity.shownOnDateString(): String? {
    val timestamp = reminderTimestamp
    if (recurrenceFrequency == null || timestamp == null) return targetDate
    return Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
}

fun RecurrenceRule.toEntityColumns(): Triple<RecurrenceFrequency, Int, String?> =
    Triple(frequency, interval, daysOfWeek.takeIf { it.isNotEmpty() }?.toIsoDayNumberCsv())