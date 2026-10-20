package com.emberr.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

class RecurrenceSeriesShift(
    val rule: RecurrenceRule,
    val anchor: LocalDate,
    private val shiftDays: Int,
    private val onlyShiftedWeekday: DayOfWeek?
) {
    fun shiftedOccurrenceDate(date: LocalDate): LocalDate =
        if (onlyShiftedWeekday != null && date.dayOfWeek != onlyShiftedWeekday) date
        else date.plus(shiftDays, DateTimeUnit.DAY)
}

fun shiftRecurringSeries(
    rule: RecurrenceRule,
    anchor: LocalDate,
    draggedOccurrence: LocalDate,
    targetDate: LocalDate
): RecurrenceSeriesShift {
    val draggedDays = draggedOccurrence.daysUntil(targetDate)
    if (rule.frequency != RecurrenceFrequency.WEEKLY) {
        return RecurrenceSeriesShift(
            rule = rule.copy(untilDateString = rule.untilDateString?.let { shiftedDateString(it, draggedDays) }),
            anchor = anchor.plus(draggedDays, DateTimeUnit.DAY),
            shiftDays = draggedDays,
            onlyShiftedWeekday = null
        )
    }

    val weekdayDays = draggedDays % 7
    val draggedWeekday = draggedOccurrence.dayOfWeek
    val newWeekday = DayOfWeek((draggedWeekday.isoDayNumber - 1 + weekdayDays).mod(7) + 1)

    if (rule.daysOfWeek.size <= 1) {
        return RecurrenceSeriesShift(
            rule = rule.copy(
                daysOfWeek = if (rule.daysOfWeek.isEmpty()) emptySet() else setOf(newWeekday),
                untilDateString = rule.untilDateString?.let { shiftedDateString(it, weekdayDays) }
            ),
            anchor = anchor.plus(weekdayDays, DateTimeUnit.DAY),
            shiftDays = weekdayDays,
            onlyShiftedWeekday = null
        )
    }

    return RecurrenceSeriesShift(
        rule = rule.copy(
            daysOfWeek = rule.daysOfWeek - draggedWeekday + newWeekday,
            untilDateString = rule.untilDateString?.let { if (weekdayDays > 0) shiftedDateString(it, weekdayDays) else it }
        ),
        anchor = if (weekdayDays < 0) anchor.plus(weekdayDays, DateTimeUnit.DAY) else anchor,
        shiftDays = weekdayDays,
        onlyShiftedWeekday = draggedWeekday
    )
}

private fun shiftedDateString(dateString: String, days: Int): String =
    LocalDate.parse(dateString).plus(days, DateTimeUnit.DAY).toString()
