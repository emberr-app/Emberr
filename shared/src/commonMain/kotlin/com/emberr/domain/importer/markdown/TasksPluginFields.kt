package com.emberr.domain.importer.markdown

import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant

private const val REMINDER_HOUR = 9
private const val WHEN_DONE_ENDING = " when done"
private val dueDateField = Regex("""\s*(?:📅|📆|🗓)️?\s*(\d{4}-\d{2}-\d{2})""")
private val scheduledDateField = Regex("""\s*(?:⏳|⌛)️?\s*(\d{4}-\d{2}-\d{2})""")
private val startDateField = Regex("""\s*🛫️?\s*(\d{4}-\d{2}-\d{2})""")
private val doneDateField = Regex("""\s*✅️?\s*(\d{4}-\d{2}-\d{2})""")
private val recurrenceField = Regex("""\s*🔁️?\s*([A-Za-z0-9,]+(?:\s+[A-Za-z0-9,]+)*)""")
private val everyIntervalPattern = Regex("""^every (?:(\d+) )?(day|week|month|year)s?(?: on (.+))?$""")
private val dayNameSeparators = Regex("""[,\s]+""")
private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

internal class TaskFields(
    val styledText: StyledText,
    val reminderTimestamp: Long?,
    val completedAt: Long?,
    val recurrenceRule: RecurrenceRule?
)

internal object TasksPluginFields {

    fun read(styledText: StyledText): TaskFields {
        val text = styledText.text
        val timeZone = TimeZone.currentSystemDefault()

        val reminderField = listOf(dueDateField, scheduledDateField, startDateField)
            .firstNotNullOfOrNull { field -> field.find(text)?.takeIf { dateOf(it) != null } }
        val reminderDate = reminderField?.let(::dateOf)
        val doneField = doneDateField.find(text)?.takeIf { dateOf(it) != null }
        val recurrenceMatch = if (reminderDate != null) recurrenceField.find(text) else null
        val recurrenceRule = recurrenceMatch?.let { recurrenceRuleOf(it.groupValues[1]) }
        val readFields = listOfNotNull(reminderField, doneField, recurrenceMatch.takeIf { recurrenceRule != null })

        return TaskFields(
            styledText = styledText.withoutCharacterRanges(readFields.map { it.range }).trimmed(),
            reminderTimestamp = reminderDate?.let { date ->
                LocalDateTime(date, LocalTime(REMINDER_HOUR, 0)).toInstant(timeZone).toEpochMilliseconds()
            },
            completedAt = doneField?.let(::dateOf)?.atStartOfDayIn(timeZone)?.toEpochMilliseconds(),
            recurrenceRule = recurrenceRule
        )
    }

    private fun dateOf(field: MatchResult): LocalDate? =
        try {
            LocalDate.parse(field.groupValues[1])
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun recurrenceRuleOf(writtenRule: String): RecurrenceRule? {
        val rule = writtenRule.trim().lowercase().removeSuffix(WHEN_DONE_ENDING)
        if (rule == "every weekday") return RecurrenceRule(RecurrenceFrequency.WEEKLY, daysOfWeek = weekdays)

        val intervalMatch = everyIntervalPattern.matchEntire(rule)
        if (intervalMatch != null) {
            val (writtenCount, unit, writtenDays) = intervalMatch.destructured
            val interval = writtenCount.toIntOrNull() ?: 1
            val frequency = when (unit) {
                "day" -> RecurrenceFrequency.DAILY
                "week" -> RecurrenceFrequency.WEEKLY
                "month" -> RecurrenceFrequency.MONTHLY
                else -> RecurrenceFrequency.YEARLY
            }
            val daysOfWeek = if (writtenDays.isEmpty()) emptySet() else daysOfWeekIn(writtenDays) ?: return null
            if (interval < 1 || (daysOfWeek.isNotEmpty() && frequency != RecurrenceFrequency.WEEKLY)) return null
            return RecurrenceRule(frequency, interval, daysOfWeek)
        }

        if (!rule.startsWith("every ")) return null
        val daysOfWeek = daysOfWeekIn(rule.removePrefix("every ")) ?: return null
        return RecurrenceRule(RecurrenceFrequency.WEEKLY, daysOfWeek = daysOfWeek)
    }

    private fun daysOfWeekIn(writtenDays: String): Set<DayOfWeek>? {
        val dayNames = writtenDays.split(dayNameSeparators).filter { it.isNotEmpty() && it != "and" }
        if (dayNames.isEmpty()) return null
        return dayNames.map { dayName -> DayOfWeek.entries.firstOrNull { it.name.lowercase() == dayName } ?: return null }.toSet()
    }

    private fun StyledText.trimmed(): StyledText {
        val leadingSpaceCount = text.takeWhile { it.isWhitespace() }.length
        val trailingSpaceCount = text.takeLastWhile { it.isWhitespace() }.length
        return withoutCharacterRanges(listOf(0 until leadingSpaceCount, text.length - trailingSpaceCount until text.length))
    }
}
