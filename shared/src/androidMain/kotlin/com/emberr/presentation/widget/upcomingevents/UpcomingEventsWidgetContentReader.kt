// Builds the ordered, date-labelled list of every upcoming, unchecked calendar event.
package com.emberr.presentation.widget.upcomingevents

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.dao.CalendarTaskDao
import com.emberr.data.local.room.dao.CategoryDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.domain.people.BIRTHDAYS_CATEGORY_ID
import com.emberr.domain.repository.NoteRepository
import com.emberr.presentation.calendar.CalendarCategoryVisibility
import com.emberr.presentation.calendar.CalendarEvent
import com.emberr.presentation.calendar.nextBirthdayEvents
import com.emberr.presentation.widget.WidgetBirthdays
import com.emberr.presentation.widget.WidgetClockFormat
import com.emberr.presentation.widget.WidgetLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.number
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.widgets_untitled_event
import emberr.shared.generated.resources.widgets_repeating
import emberr.shared.generated.resources.widgets_today
import emberr.shared.generated.resources.widgets_tomorrow
import org.jetbrains.compose.resources.getString

private const val maximumEventsShown = 60
private const val maximumCharactersPerTitle = 100
private const val daysSearchedForNextRepeat = 366
private const val repeatingGroupKey = "repeating"

class UpcomingEventsWidgetContentReader(
    private val calendarTaskDao: CalendarTaskDao,
    private val noteRepository: NoteRepository,
    private val categoryDao: CategoryDao,
    private val clockFormat: WidgetClockFormat,
    private val settingsManager: SettingsManager,
    private val widgetBirthdays: WidgetBirthdays,
) {
    suspend fun readContentOnce(): UpcomingEventsWidgetContent? = withContext(Dispatchers.IO) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val tasks = try {
            val oneTimeTasks = calendarTaskDao.getUpcomingTasks(today.toString())
                .filter { task -> task.recurrenceFrequency == null }
            oneTimeTasks + nextUnfinishedRepeats(today)
        } catch (cause: Exception) {
            WidgetLog.e("Could not read upcoming events", cause)
            return@withContext null
        }

        val colorsByCategoryId = try {
            categoryDao.getAllCategoriesOnce(ACCOUNT_SPACE_ID).associate { category -> category.categoryId to category.colorHex }
        } catch (cause: Exception) {
            WidgetLog.e("Could not read the event categories", cause)
            emptyMap()
        }

        val categoryVisibility = CalendarCategoryVisibility(
            hiddenVisibilityKeys = settingsManager.getHiddenCalendarCategories(),
            existingCategoryIds = colorsByCategoryId.keys
        )
        val shownTasks = tasks.filter { task -> categoryVisibility.isEventShown(task) }
        val birthdayEvents = if (categoryVisibility.areBirthdaysShown) {
            nextBirthdayEvents(widgetBirthdays.readPeople(), today)
        } else {
            emptyList()
        }

        UpcomingEventsWidgetContent(events = buildEvents(shownTasks, birthdayEvents, today, colorsByCategoryId))
    }

    private suspend fun nextUnfinishedRepeats(today: LocalDate): List<CalendarTaskEntity> {
        val lastSearchedDay = today.plus(DatePeriod(days = daysSearchedForNextRepeat))
        return noteRepository.getCalendarTasksInRange(today.toString(), lastSearchedDay.toString()).first()
            .filter { occurrence ->
                occurrence.recurrenceFrequency != null &&
                    !occurrence.isChecked &&
                    occurrence.targetDate != null &&
                    occurrence.targetDate >= today.toString()
            }
            .sortedBy { occurrence -> occurrence.targetDate }
            .distinctBy { occurrence -> occurrence.blockId }
    }

    private suspend fun buildEvents(
        tasks: List<CalendarTaskEntity>,
        birthdayEvents: List<CalendarEvent>,
        today: LocalDate,
        colorsByCategoryId: Map<String, String>
    ): List<UpcomingEvent> {
        val untitledEventTitle = getString(Res.string.widgets_untitled_event)
        val repeatingSectionTitle = getString(Res.string.widgets_repeating)
        val dayLabels = RelativeDayLabels(today = getString(Res.string.widgets_today), tomorrow = getString(Res.string.widgets_tomorrow))
        val taskEvents = tasks.mapNotNull { task ->
            val targetDate = task.targetDate?.let(LocalDate::parse) ?: return@mapNotNull null
            val isRepeating = task.recurrenceFrequency != null

            SortableUpcomingEvent(
                isRepeating = isRepeating,
                dateString = task.targetDate,
                timestamp = task.reminderTimestamp ?: Long.MAX_VALUE,
                event = UpcomingEvent(
                    blockId = task.blockId,
                    title = task.text.trim().take(maximumCharactersPerTitle).ifBlank { untitledEventTitle },
                    dateGroupKey = if (isRepeating) repeatingGroupKey else task.targetDate,
                    dateLabel = if (isRepeating) repeatingSectionTitle else formatDateLabel(targetDate, today, dayLabels),
                    dayLabel = if (isRepeating) formatDateLabel(targetDate, today, dayLabels) else null,
                    timeLabel = task.reminderTimestamp?.let { timestamp -> clockFormat.clockTimeOf(timestamp) },
                    isOverdue = targetDate == today &&
                        task.reminderTimestamp != null &&
                        task.reminderTimestamp < System.currentTimeMillis(),
                    accentColorHex = task.categoryId?.let { categoryId -> colorsByCategoryId[categoryId] },
                    weekdayShortLabel = weekdayShortLabelOf(targetDate),
                    dayOfMonthLabel = targetDate.day.toString()
                )
            )
        }
        val birthdays = birthdayEvents.map { birthday ->
            val birthdayDate = LocalDate.parse(birthday.dateString)
            SortableUpcomingEvent(
                isRepeating = false,
                dateString = birthday.dateString,
                timestamp = birthday.reminderTimestamp ?: Long.MAX_VALUE,
                event = UpcomingEvent(
                    blockId = birthday.blockId,
                    title = birthday.text.take(maximumCharactersPerTitle),
                    dateGroupKey = birthday.dateString,
                    dateLabel = formatDateLabel(birthdayDate, today, dayLabels),
                    timeLabel = null,
                    isOverdue = false,
                    accentColorHex = colorsByCategoryId[BIRTHDAYS_CATEGORY_ID],
                    birthdayPersonNoteId = birthday.birthdayPersonNoteId,
                    weekdayShortLabel = weekdayShortLabelOf(birthdayDate),
                    dayOfMonthLabel = birthdayDate.day.toString()
                )
            )
        }
        return (taskEvents + birthdays)
            .sortedWith(compareBy({ !it.isRepeating }, { it.dateString }, { it.timestamp }))
            .take(maximumEventsShown)
            .map { it.event }
    }

    private class SortableUpcomingEvent(
        val isRepeating: Boolean,
        val dateString: String,
        val timestamp: Long,
        val event: UpcomingEvent
    )
}

private fun weekdayShortLabelOf(date: LocalDate): String =
    date.dayOfWeek.name.take(3)

private val shortMonthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private class RelativeDayLabels(val today: String, val tomorrow: String)

private fun formatDateLabel(targetDate: LocalDate, today: LocalDate, dayLabels: RelativeDayLabels): String =
    when (targetDate) {
        today -> dayLabels.today
        today.plus(DatePeriod(days = 1)) -> dayLabels.tomorrow
        else -> {
            val month = shortMonthNames.getOrNull(targetDate.month.number - 1) ?: targetDate.month.name.take(3)
            "$month ${targetDate.day}"
        }
    }
