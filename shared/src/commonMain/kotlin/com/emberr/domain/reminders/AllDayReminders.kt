package com.emberr.domain.reminders

import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.RecurrenceEngine
import com.emberr.domain.model.RecurrenceRule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

fun nextAllDayReminderTimestamp(
    startTimestamp: Long,
    allDay: AllDaySpan,
    recurrenceRule: RecurrenceRule?,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Long? {
    if (!allDay.isReminderOn) return null
    val lengthInDays = allDay.lengthInDays.coerceAtLeast(1)
    val startDate = Instant.fromEpochMilliseconds(startTimestamp).toLocalDateTime(timeZone).date
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
    val occurrenceStartDates = if (recurrenceRule == null) {
        listOf(startDate)
    } else {
        RecurrenceEngine.occurrenceDatesInRange(
            rule = recurrenceRule,
            anchor = startDate,
            rangeStart = today.minus(lengthInDays - 1, DateTimeUnit.DAY),
            rangeEnd = today.plus(RESCHEDULE_HORIZON_DAYS, DateTimeUnit.DAY)
        )
    }

    for (occurrenceStartDate in occurrenceStartDates) {
        for (dayOffset in 0 until lengthInDays) {
            val reminderDate = occurrenceStartDate.plus(dayOffset, DateTimeUnit.DAY)
            val reminderTimestamp = RecurrenceEngine.retargetTimestampToDate(startTimestamp, reminderDate, timeZone)
            if (reminderTimestamp > nowMillis) return reminderTimestamp
        }
    }
    return null
}

fun ReminderScheduler.scheduleEventReminder(
    blockId: String,
    noteTitle: String,
    text: String,
    timestamp: Long,
    allDay: AllDaySpan?,
    recurrenceRule: RecurrenceRule?
) {
    if (allDay == null) {
        schedule(blockId, noteTitle, text, timestamp)
        return
    }
    val reminderTimestamp = nextAllDayReminderTimestamp(
        startTimestamp = timestamp,
        allDay = allDay,
        recurrenceRule = recurrenceRule,
        nowMillis = Clock.System.now().toEpochMilliseconds()
    )
    if (reminderTimestamp == null) cancel(blockId) else schedule(blockId, noteTitle, text, reminderTimestamp)
}
