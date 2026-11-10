// Loads the task list from storage and groups it into rows the widget can draw.
package com.emberr.presentation.widget.tasks

import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.dao.SpaceDao
import com.emberr.data.local.room.entity.ACCOUNT_TASKS_INBOX_NOTE_ID
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.TaskSource
import com.emberr.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import com.emberr.presentation.widget.WidgetClockFormat
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.home.overview.tasks.taskHeadingsByNoteId
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.widgets_repeating
import emberr.shared.generated.resources.widgets_note
import emberr.shared.generated.resources.widgets_untitled_task
import emberr.shared.generated.resources.widgets_today
import emberr.shared.generated.resources.widgets_tomorrow
import emberr.shared.generated.resources.widgets_yesterday
import org.jetbrains.compose.resources.getString
import com.emberr.domain.util.time.formatDate
import com.emberr.domain.util.time.DateTextStyle

private const val maximumTasksShown = 40
private const val maximumCharactersPerTask = 160
private const val inboxNoteTitle = "Inbox"

class TasksWidgetContentReader(
    private val noteRepository: NoteRepository,
    private val noteDao: NoteDao,
    private val spaceDao: SpaceDao,
    private val clockFormat: WidgetClockFormat
) {
    fun observeTasks(): Flow<List<CalendarTaskEntity>> =
        noteRepository.getTasksWithCurrentRepeatDays(Clock.System.todayIn(TimeZone.currentSystemDefault()))
            .flowOn(Dispatchers.IO)
            .catch { cause -> WidgetLog.e("Stopped observing the task list", cause) }

    suspend fun readContentOnce(isShowingCompleted: Boolean): TasksWidgetContent? =
        withContext(Dispatchers.IO) {
            val tasks = try {
                noteRepository.getTasksWithCurrentRepeatDays(Clock.System.todayIn(TimeZone.currentSystemDefault())).first()
            } catch (cause: Exception) {
                WidgetLog.e("Could not read the task list", cause)
                return@withContext null
            }
            buildContent(tasks, isShowingCompleted)
        }

    suspend fun buildContent(
        tasks: List<CalendarTaskEntity>,
        isShowingCompleted: Boolean
    ): TasksWidgetContent = withContext(Dispatchers.Default) {
        val matchingTasks = tasks.filter { task -> task.isChecked == isShowingCompleted }
        TasksWidgetContent(
            isShowingCompleted = isShowingCompleted,
            rows = buildRows(matchingTasks)
        )
    }

    private suspend fun buildRows(tasks: List<CalendarTaskEntity>): List<TasksWidgetRow> {
        if (tasks.isEmpty()) return emptyList()

        val notesHoldingTasks = resolveNotesHoldingTasks(tasks)
        val noteTitlesById = notesHoldingTasks.associate { note -> note.noteId to note.title.trim() }
        val labels = TaskRowLabels(
            repeating = getString(Res.string.widgets_repeating),
            untitledNote = getString(Res.string.widgets_note),
            untitledTask = getString(Res.string.widgets_untitled_task),
            today = getString(Res.string.widgets_today),
            tomorrow = getString(Res.string.widgets_tomorrow),
            yesterday = getString(Res.string.widgets_yesterday)
        )
        val noteHeadingsById = taskHeadingsByNoteId(notesHoldingTasks, resolveSpaceNames(), labels.untitledNote)
        val inboxNoteIds = findInboxNoteIds(noteTitlesById)

        val (unlabelled, labelled) = tasks.partition { task ->
            headingFor(task, noteHeadingsById, inboxNoteIds, labels) == null
        }

        val (repeatingTasks, nonRepeatingTasks) = labelled.partition { task -> task.recurrenceFrequency != null }
        val sortedRepeatingTasks = repeatingTasks.sortedWith(
            compareBy(
                { task -> task.targetDate },
                { task -> task.reminderTimestamp ?: Long.MAX_VALUE }
            )
        )
        val dailyTasks = nonRepeatingTasks
            .filter { task -> task.sourceType == TaskSource.DAILY }
            .sortedBy { task -> task.noteId }
        val noteTasks = nonRepeatingTasks
            .filter { task -> task.sourceType != TaskSource.DAILY }
            .sortedWith(compareBy({ task -> noteHeadingsById[task.noteId].orEmpty() }, { task -> task.noteId }))

        val orderedTasks = (sortedWithinGroup(unlabelled) + sortedRepeatingTasks + dailyTasks + noteTasks)
            .take(maximumTasksShown)

        val rows = mutableListOf<TasksWidgetRow>()
        var lastHeading: String? = null

        orderedTasks.forEach { task ->
            val heading = headingFor(task, noteHeadingsById, inboxNoteIds, labels)
            if (heading != null && heading != lastHeading) {
                rows += TasksWidgetRow.SectionHeading(key = "heading:${task.blockId}", label = heading)
            }
            lastHeading = heading

            rows += TasksWidgetRow.Task(
                key = "task:${task.blockId}",
                blockId = task.blockId,
                text = task.text.trim().take(maximumCharactersPerTask).ifBlank { labels.untitledTask },
                isChecked = task.isChecked,
                timeLabel = task.reminderTimestamp?.let { timestamp -> clockFormat.clockTimeOf(timestamp) },
                occurrenceDate = task.targetDate,
                dayLabel = if (task.recurrenceFrequency != null) task.targetDate?.let { date -> dailyDateLabel(date, labels) } else null
            )
        }

        return rows
    }

    private fun sortedWithinGroup(tasks: List<CalendarTaskEntity>): List<CalendarTaskEntity> =
        tasks.sortedWith(
            compareBy(
                { task -> task.reminderTimestamp ?: Long.MAX_VALUE },
                { task -> task.text.lowercase() }
            )
        )

    private suspend fun resolveNotesHoldingTasks(tasks: List<CalendarTaskEntity>): List<NoteMetadataEntity> {
        val noteIds = tasks
            .filter { task -> task.sourceType != TaskSource.DAILY }
            .map { task -> task.noteId }
            .distinct()
        if (noteIds.isEmpty()) return emptyList()

        return try {
            noteDao.getNotesByIds(noteIds)
        } catch (cause: Exception) {
            WidgetLog.e("Could not resolve note titles for the task list", cause)
            emptyList()
        }
    }

    private suspend fun resolveSpaceNames(): Map<String, String> =
        try {
            spaceDao.getAllSpacesOnce().associate { space -> space.spaceId to space.displayName }
        } catch (cause: Exception) {
            WidgetLog.e("Could not resolve space names for the task list", cause)
            emptyMap()
        }

    private fun findInboxNoteIds(noteTitlesById: Map<String, String>): Set<String> =
        noteTitlesById.filterValues { title -> title.equals(inboxNoteTitle, ignoreCase = true) }.keys + ACCOUNT_TASKS_INBOX_NOTE_ID

    private fun headingFor(
        task: CalendarTaskEntity,
        noteHeadingsById: Map<String, String>,
        inboxNoteIds: Set<String>,
        labels: TaskRowLabels
    ): String? = when {
        task.recurrenceFrequency != null -> labels.repeating
        task.sourceType == TaskSource.DAILY -> dailyDateLabel(task.noteId, labels)
        task.noteId in inboxNoteIds -> null
        else -> noteHeadingsById[task.noteId] ?: labels.untitledNote
    }
}

private class TaskRowLabels(
    val repeating: String,
    val untitledNote: String,
    val untitledTask: String,
    val today: String,
    val tomorrow: String,
    val yesterday: String
)

private fun dailyDateLabel(dateString: String, labels: TaskRowLabels): String {
    val date = try {
        LocalDate.parse(dateString)
    } catch (cause: IllegalArgumentException) {
        return dateString
    }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (date) {
        today -> labels.today
        today.plus(DatePeriod(days = 1)) -> labels.tomorrow
        today.minus(DatePeriod(days = 1)) -> labels.yesterday
        else -> formatDate(date, DateTextStyle.SHORT_WEEKDAY_SHORT_MONTH_DAY)
    }
}
