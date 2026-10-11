package com.emberr.domain.importer.markdown

import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TasksPluginFieldsTest {

    private fun read(text: String): TaskFields = TasksPluginFields.read(StyledText(text = text, spans = emptyList()))

    private fun nineInTheMorningOf(year: Int, month: Int, day: Int): Long =
        LocalDateTime(year, month, day, 9, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    private fun recurrenceRuleOf(writtenRule: String): RecurrenceRule? = read("Task 🔁 $writtenRule 📅 2026-11-02").recurrenceRule

    @Test
    fun theDueDateBecomesAReminderAtNineAndOtherDatesStayInTheText() {
        val task = read("Gym 🛫 2026-10-01 ⏳ 2026-10-10 📅 2026-10-13")

        assertEquals(nineInTheMorningOf(2026, 10, 13), task.reminderTimestamp)
        assertEquals("Gym 🛫 2026-10-01 ⏳ 2026-10-10", task.styledText.text)
    }

    @Test
    fun withoutADueDateTheScheduledDateAndThenTheStartDateAreUsed() {
        assertEquals(nineInTheMorningOf(2026, 10, 10), read("Gym 🛫 2026-10-01 ⏳ 2026-10-10").reminderTimestamp)
        assertEquals(nineInTheMorningOf(2026, 10, 1), read("Gym 🛫 2026-10-01").reminderTimestamp)
    }

    @Test
    fun theDoneDateBecomesTheCompletionDay() {
        val task = read("File taxes ✅ 2026-04-10 📅 2026-04-15")

        assertEquals(LocalDate(2026, 4, 10).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(), task.completedAt)
        assertEquals("File taxes", task.styledText.text)
    }

    @Test
    fun commonRepeatRulesAreUnderstood() {
        assertEquals(RecurrenceRule(RecurrenceFrequency.DAILY), recurrenceRuleOf("every day"))
        assertEquals(RecurrenceRule(RecurrenceFrequency.WEEKLY, interval = 2), recurrenceRuleOf("every 2 weeks"))
        assertEquals(RecurrenceRule(RecurrenceFrequency.YEARLY), recurrenceRuleOf("every year when done"))
        assertEquals(
            RecurrenceRule(RecurrenceFrequency.WEEKLY, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
            recurrenceRuleOf("every week on Monday, Thursday")
        )
        assertEquals(
            RecurrenceRule(RecurrenceFrequency.WEEKLY, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
            recurrenceRuleOf("every Monday and Friday")
        )
        assertEquals(
            RecurrenceRule(
                RecurrenceFrequency.WEEKLY,
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            ),
            recurrenceRuleOf("every weekday")
        )
    }

    @Test
    fun aRepeatRuleThatCannotBeReadStaysInTheTextButTheDateIsStillUsed() {
        val task = read("Odd one 🔁 every month on the 1st 📅 2026-10-20")

        assertNull(task.recurrenceRule)
        assertEquals(nineInTheMorningOf(2026, 10, 20), task.reminderTimestamp)
        assertEquals("Odd one 🔁 every month on the 1st", task.styledText.text)
    }

    @Test
    fun aRepeatRuleWithoutADateStaysInTheText() {
        val task = read("Water plants 🔁 every week")

        assertNull(task.recurrenceRule)
        assertNull(task.reminderTimestamp)
        assertEquals("Water plants 🔁 every week", task.styledText.text)
    }

    @Test
    fun anImpossibleDateStaysInTheText() {
        val task = read("Typo 📅 2026-13-40")

        assertNull(task.reminderTimestamp)
        assertEquals("Typo 📅 2026-13-40", task.styledText.text)
    }

    @Test
    fun formattingMovesWithTheTextWhenFieldsAreRemoved() {
        val task = TasksPluginFields.read(
            StyledText(text = "📅 2026-11-01 Pay rent", spans = listOf(InlineSpan(start = 14, end = 17, bold = true)))
        )

        assertEquals("Pay rent", task.styledText.text)
        assertEquals(listOf(InlineSpan(start = 0, end = 3, bold = true)), task.styledText.spans)
    }
}
