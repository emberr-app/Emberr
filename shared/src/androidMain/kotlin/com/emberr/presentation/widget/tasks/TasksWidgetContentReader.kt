// Loads the task list from storage and groups it into rows the widget can draw.
package com.emberr.presentation.widget.tasks

import com.emberr.data.local.room.dao.CalendarTaskDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.dao.SpaceDao
import com.emberr.data.local.room.entity.ACCOUNT_TASKS_INBOX_NOTE_ID
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.TaskSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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

private const val maximumTasksShown = 40
private const val maximumCharactersPerTask = 160
private const val inboxNoteTitle = "Inbox"

class TasksWidgetContentReader(
    private val calendarTaskDao: CalendarTaskDao,
    private val noteDao: NoteDao,
    private val spaceDao: SpaceDao,
    private val clockFormat: WidgetClockFormat
) {
    fun observeTasks(): Flow<List<CalendarTaskEntity>> =
        calendarTaskDao.getTasksOutsideTrashAcrossSpacesFlow()
            .flowOn(Dispatchers.IO)
            .catch { cause -> WidgetLog.e("Stopped observing the task list", cause) }

    suspend fun readContentOnce(isShowingCompleted: Boolean): TasksWidgetContent? =
        withContext(Dispatchers.IO) {
            val tasks = try {
                calendarTaskDao.getTasksOutsideTrashAcrossSpaces()
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
        val noteHeadingsById = taskHeadingsByNoteId(notesHoldingTasks, resolveSpaceNames())
        val inboxNoteIds = findInboxNoteIds(noteTitlesById)

        val (unlabelled, labelled) = tasks.partition { task ->
            headingFor(task, noteHeadingsById, inboxNoteIds) == null
        }

        val dailyTasks = labelled
            .filter { task -> task.sourceType == TaskSource.DAILY }
            .sortedBy { task -> task.noteId }
        val noteTasks = labelled
            .filter { task -> task.sourceType != TaskSource.DAILY }
            .sortedWith(compareBy({ task -> noteHeadingsById[task.noteId].orEmpty() }, { task -> task.noteId }))

        val orderedTasks = (sortedWithinGroup(unlabelled) + dailyTasks + noteTasks)
            .take(maximumTasksShown)

        val rows = mutableListOf<TasksWidgetRow>()
        var lastHeading: String? = null

        orderedTasks.forEach { task ->
            val heading = headingFor(task, noteHeadingsById, inboxNoteIds)
            if (heading != null && heading != lastHeading) {
                rows += TasksWidgetRow.SectionHeading(key = "heading:${task.blockId}", label = heading)
            }
            lastHeading = heading

            rows += TasksWidgetRow.Task(
                key = "task:${task.blockId}",
                blockId = task.blockId,
                text = task.text.trim().take(maximumCharactersPerTask).ifBlank { "Untitled task" },
                isChecked = task.isChecked,
                timeLabel = task.reminderTimestamp?.let { timestamp -> clockFormat.clockTimeOf(timestamp) }
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
        inboxNoteIds: Set<String>
    ): String? = when {
        task.sourceType == TaskSource.DAILY -> dailyDateLabel(task.noteId)
        task.noteId in inboxNoteIds -> null
        else -> noteHeadingsById[task.noteId] ?: "Note"
    }
}

private fun dailyDateLabel(dateString: String): String {
    val date = try {
        LocalDate.parse(dateString)
    } catch (cause: IllegalArgumentException) {
        return dateString
    }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (date) {
        today -> "Today"
        today.plus(DatePeriod(days = 1)) -> "Tomorrow"
        today.minus(DatePeriod(days = 1)) -> "Yesterday"
        else -> {
            val shortDay = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val shortMonth = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            "$shortDay, $shortMonth ${date.day}"
        }
    }
}
