// Loads today's tasks and turns them into the date heading and rows the widget draws.
package com.emberr.presentation.widget.todaytasks

import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.domain.repository.NoteRepository
import com.emberr.presentation.widget.WidgetClockFormat
import com.emberr.presentation.widget.WidgetLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.number
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.widgets_untitled_task
import emberr.shared.generated.resources.widgets_minutes_ago
import emberr.shared.generated.resources.widgets_hours_ago
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

private const val maximumTasksShown = 12
private const val maximumCharactersPerTitle = 120

class TodayTasksWidgetContentReader(
    private val noteRepository: NoteRepository,
    private val clockFormat: WidgetClockFormat
) {

    suspend fun readContentOnce(): TodayTasksWidgetContent? = withContext(Dispatchers.IO) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val tasks = try {
            noteRepository.getCalendarTasksForDate(today.toString()).first()
        } catch (cause: Exception) {
            WidgetLog.e("Could not read today's tasks", cause)
            return@withContext null
        }

        TodayTasksWidgetContent(
            dateLabel = formatMonthAndDay(today),
            rows = buildRows(tasks, System.currentTimeMillis()),
            weekdayLabel = dayNames.getOrNull(today.dayOfWeek.ordinal) ?: today.dayOfWeek.name,
            doneCount = tasks.count { task -> task.isChecked },
            totalCount = tasks.size
        )
    }

    private suspend fun buildRows(tasks: List<CalendarTaskEntity>, now: Long): List<TodayTaskRow> {
        val untitledTaskTitle = getString(Res.string.widgets_untitled_task)
        return tasks
            .sortedWith(
                compareBy(
                    { task -> if (task.isChecked) 1 else 0 },
                    { task -> task.reminderTimestamp ?: Long.MAX_VALUE },
                    { task -> task.text.lowercase() }
                )
            )
            .take(maximumTasksShown)
            .map { task ->
                val scheduledAt = task.reminderTimestamp
                val isOverdue = !task.isChecked && scheduledAt != null && scheduledAt < now

                TodayTaskRow(
                    blockId = task.blockId,
                    occurrenceDate = task.targetDate,
                    title = task.text.trim().take(maximumCharactersPerTitle).ifBlank { untitledTaskTitle },
                    isChecked = task.isChecked,
                    scheduleLabel = when {
                        scheduledAt == null -> null
                        isOverdue -> formatOverdueLabel(now - scheduledAt)
                        else -> clockFormat.clockTimeOf(scheduledAt)
                    },
                    isOverdue = isOverdue
                )
            }
    }
}

private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private val dayNames = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)

private fun formatMonthAndDay(date: LocalDate): String {
    val month = monthNames.getOrNull(date.month.number - 1) ?: date.month.name
    return "$month ${date.day}"
}

private suspend fun formatOverdueLabel(elapsedMillis: Long): String {
    val elapsedMinutes = (elapsedMillis / 60_000L).coerceAtLeast(1L)

    if (elapsedMinutes < 60) {
        return getPluralString(Res.plurals.widgets_minutes_ago, elapsedMinutes.toInt(), elapsedMinutes)
    }

    val hours = elapsedMinutes / 60
    return getPluralString(Res.plurals.widgets_hours_ago, hours.toInt(), hours)
}
