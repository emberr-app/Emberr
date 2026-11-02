// Builds the ordered, date-labelled list of every upcoming, unchecked calendar event.
package com.emberr.presentation.widget.upcomingevents

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.dao.CalendarTaskDao
import com.emberr.data.local.room.dao.CategoryDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.domain.people.BIRTHDAYS_CATEGORY_ID
import com.emberr.presentation.calendar.CalendarCategoryVisibility
import com.emberr.presentation.calendar.CalendarEvent
import com.emberr.presentation.calendar.nextBirthdayEvents
import com.emberr.presentation.widget.WidgetBirthdays
import com.emberr.presentation.widget.WidgetClockFormat
import com.emberr.presentation.widget.WidgetLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.number

private const val maximumEventsShown = 60
private const val maximumCharactersPerTitle = 100

class UpcomingEventsWidgetContentReader(
    private val calendarTaskDao: CalendarTaskDao,
    private val categoryDao: CategoryDao,
    private val clockFormat: WidgetClockFormat,
    private val settingsManager: SettingsManager,
    private val widgetBirthdays: WidgetBirthdays,
) {
    suspend fun readContentOnce(): UpcomingEventsWidgetContent? = withContext(Dispatchers.IO) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val tasks = try {
            calendarTaskDao.getUpcomingTasks(today.toString())
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

    private fun buildEvents(
        tasks: List<CalendarTaskEntity>,
        birthdayEvents: List<CalendarEvent>,
        today: LocalDate,
        colorsByCategoryId: Map<String, String>
    ): List<UpcomingEvent> {
        val taskEvents = tasks.mapNotNull { task ->
            val targetDate = task.targetDate?.let(LocalDate::parse) ?: return@mapNotNull null

            SortableUpcomingEvent(
                timestamp = task.reminderTimestamp ?: Long.MAX_VALUE,
                event = UpcomingEvent(
                    blockId = task.blockId,
                    title = task.text.trim().take(maximumCharactersPerTitle).ifBlank { "Untitled event" },
                    dateGroupKey = task.targetDate,
                    dateLabel = formatDateLabel(targetDate, today),
                    timeLabel = task.reminderTimestamp?.let { timestamp -> clockFormat.clockTimeOf(timestamp) },
                    isOverdue = targetDate == today &&
                        task.reminderTimestamp != null &&
                        task.reminderTimestamp < System.currentTimeMillis(),
                    accentColorHex = task.categoryId?.let { categoryId -> colorsByCategoryId[categoryId] }
                )
            )
        }
        val birthdays = birthdayEvents.map { birthday ->
            SortableUpcomingEvent(
                timestamp = birthday.reminderTimestamp ?: Long.MAX_VALUE,
                event = UpcomingEvent(
                    blockId = birthday.blockId,
                    title = birthday.text.take(maximumCharactersPerTitle),
                    dateGroupKey = birthday.dateString,
                    dateLabel = formatDateLabel(LocalDate.parse(birthday.dateString), today),
                    timeLabel = null,
                    isOverdue = false,
                    accentColorHex = colorsByCategoryId[BIRTHDAYS_CATEGORY_ID],
                    birthdayPersonNoteId = birthday.birthdayPersonNoteId
                )
            )
        }
        return (taskEvents + birthdays)
            .sortedWith(compareBy({ it.event.dateGroupKey }, { it.timestamp }))
            .take(maximumEventsShown)
            .map { it.event }
    }

    private class SortableUpcomingEvent(
        val timestamp: Long,
        val event: UpcomingEvent
    )
}

private val shortMonthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private fun formatDateLabel(targetDate: LocalDate, today: LocalDate): String =
    when (targetDate) {
        today -> "Today"
        today.plus(DatePeriod(days = 1)) -> "Tomorrow"
        else -> {
            val month = shortMonthNames.getOrNull(targetDate.month.number - 1) ?: targetDate.month.name.take(3)
            "$month ${targetDate.day}"
        }
    }
