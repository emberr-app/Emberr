package com.emberr.presentation.widget

import androidx.compose.runtime.Composable
import com.emberr.R
import com.emberr.domain.util.time.daysOfWeekStartingFrom
import com.emberr.presentation.widget.calendar.CalendarDayCell
import com.emberr.presentation.widget.calendar.CalendarWidgetContent
import com.emberr.presentation.widget.calendaragenda.AgendaDayCell
import com.emberr.presentation.widget.calendaragenda.AgendaEvent
import com.emberr.presentation.widget.calendaragenda.CalendarAgendaWidgetContent
import com.emberr.presentation.widget.note.WidgetElement
import com.emberr.presentation.widget.note.WidgetLineMarker
import com.emberr.presentation.widget.note.WidgetNoteContent
import com.emberr.presentation.widget.note.WidgetTextStyleName
import com.emberr.presentation.widget.notecard.NoteCardWidgetContent
import com.emberr.presentation.widget.notelist.NoteListWidgetContent
import com.emberr.presentation.widget.notelist.NoteListWidgetRow
import com.emberr.presentation.widget.noteshortcut.NoteShortcutWidgetContent
import com.emberr.presentation.widget.tasks.TasksWidgetContent
import com.emberr.presentation.widget.tasks.TasksWidgetRow
import com.emberr.presentation.widget.todaytasks.TodayTaskRow
import com.emberr.presentation.widget.todaytasks.TodayTasksWidgetContent
import com.emberr.presentation.widget.upcomingevents.UpcomingEvent
import com.emberr.presentation.widget.upcomingevents.UpcomingEventsWidgetContent
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

internal const val previewNoteId = "widget-preview"
@Composable
internal fun previewQuoteText(): String = widgetText(R.string.widget_preview_quote)
internal const val previewQuoteAuthor = "Mark Twain"

private const val blueHex = "#64B5F6"
private const val greenHex = "#81C784"
private const val orangeHex = "#FFB74D"
private const val pinkHex = "#FFC6FF"
private const val daysPerWeek = 7
private const val weeksShown = 6
private val previewWeekdays = daysOfWeekStartingFrom(DayOfWeek.SUNDAY)

@Composable
internal fun previewNoteContent() = WidgetNoteContent(
    title = widgetText(R.string.widget_preview_weekend_trip),
    elements = listOf(
        previewTextLine("packing", widgetText(R.string.widget_preview_packing), WidgetTextStyleName.SUBHEADING),
        previewTextLine("passport", widgetText(R.string.widget_preview_passport), marker = WidgetLineMarker.UNCHECKED_BOX),
        previewTextLine("tickets", widgetText(R.string.widget_preview_train_tickets), marker = WidgetLineMarker.CHECKED_BOX, isStruckThrough = true),
        previewTextLine("charger", widgetText(R.string.widget_preview_camera_charger), marker = WidgetLineMarker.UNCHECKED_BOX),
        WidgetElement.DividerLine(key = "divider"),
        previewTextLine("plan", widgetText(R.string.widget_preview_leave_early)),
        previewTextLine("lunch", widgetText(R.string.widget_preview_lunch_at_the_harbour), marker = WidgetLineMarker.BULLET),
        previewTextLine("museum", widgetText(R.string.widget_preview_museum), marker = WidgetLineMarker.BULLET)
    )
)

@Composable
internal fun previewNoteCardContent() = NoteCardWidgetContent(
    noteId = previewNoteId,
    title = widgetText(R.string.widget_preview_reading_list),
    icon = "📚",
    isFavorite = true,
    isCanvas = false,
    coverImageAbsolutePath = null
)

@Composable
internal fun previewNoteShortcutContent() = NoteShortcutWidgetContent(
    noteId = previewNoteId,
    title = widgetText(R.string.widget_preview_ideas),
    icon = "💡"
)

@Composable
internal fun previewNoteListContent() = NoteListWidgetContent(
    notes = listOf(
        NoteListWidgetRow("trip", widgetText(R.string.widget_preview_weekend_trip), widgetText(R.string.widget_preview_weekend_trip_snippet), "🧳"),
        NoteListWidgetRow("reading", widgetText(R.string.widget_preview_reading_list), "The Overstory, Piranesi, Project Hail Mary", "📚"),
        NoteListWidgetRow("meeting", widgetText(R.string.widget_preview_meeting_notes), widgetText(R.string.widget_preview_meeting_notes_snippet), "🗒️"),
        NoteListWidgetRow("recipes", widgetText(R.string.widget_preview_recipes), widgetText(R.string.widget_preview_recipes_snippet), "🍋")
    )
)

@Composable
internal fun previewTasksContent() = TasksWidgetContent(
    isShowingCompleted = false,
    rows = listOf(
        previewTask("dentist", widgetText(R.string.widget_preview_call_the_dentist), timeLabel = "10:30 AM"),
        previewTask("slides", widgetText(R.string.widget_preview_finish_the_slides)),
        TasksWidgetRow.SectionHeading(key = "groceries", label = widgetText(R.string.widget_preview_groceries)),
        previewTask("milk", widgetText(R.string.widget_preview_oat_milk)),
        previewTask("bread", widgetText(R.string.widget_preview_bread)),
        TasksWidgetRow.SectionHeading(key = "repeating", label = widgetText(R.string.widget_repeating)),
        previewTask("plants", widgetText(R.string.widget_preview_water_the_plants), timeLabel = "9:00 AM", dayLabel = widgetText(R.string.widget_preview_today))
    )
)

@Composable
internal fun previewTodayTasksContent(): TodayTasksWidgetContent {
    val today = previewToday()
    return TodayTasksWidgetContent(
        dateLabel = "${monthNameOf(today)} ${today.day}",
        weekdayLabel = capitalized(today.dayOfWeek.name),
        rows = listOf(
            TodayTaskRow("plants", widgetText(R.string.widget_preview_water_the_plants), isChecked = true, scheduleLabel = "9:00 AM", isOverdue = false),
            TodayTaskRow("dentist", widgetText(R.string.widget_preview_call_the_dentist), isChecked = false, scheduleLabel = "10:30 AM", isOverdue = false),
            TodayTaskRow("slides", widgetText(R.string.widget_preview_finish_the_slides), isChecked = false, scheduleLabel = null, isOverdue = false)
        ),
        doneCount = 1,
        totalCount = 3
    )
}

internal fun previewCalendarContent(): CalendarWidgetContent {
    val today = previewToday()
    val dotsByDay = previewDotsByDay(today)
    return CalendarWidgetContent(
        shownMonth = monthKeyOf(today),
        monthLabel = monthNameOf(today),
        weekdayLabels = previewWeekdayLabels(),
        weeks = previewMonthGrid(today).map { week ->
            week.map { date ->
                CalendarDayCell(
                    dateString = date?.toString(),
                    dayNumber = date?.day,
                    isToday = date == today,
                    dotColorHexes = date?.let { dotsByDay[it.day] }.orEmpty()
                )
            }
        },
        yearLabel = today.year.toString()
    )
}

@Composable
internal fun previewCalendarAgendaContent(): CalendarAgendaWidgetContent {
    val today = previewToday()
    val dotsByDay = previewDotsByDay(today)
    val todayLabel = "${shortMonthNameOf(today)} ${today.day}"
    return CalendarAgendaWidgetContent(
        shownMonth = monthKeyOf(today),
        monthLabel = monthNameOf(today),
        weekdayLabels = previewWeekdayLabels(),
        weeks = previewMonthGrid(today).map { week ->
            week.map { date ->
                AgendaDayCell(
                    dayNumber = date?.day,
                    isToday = date == today,
                    hasEvents = date != null && date.day in dotsByDay
                )
            }
        },
        events = listOf(
            AgendaEvent("standup", widgetText(R.string.widget_preview_team_sync), "$todayLabel · 10:00 AM", isDone = false, accentColorHex = blueHex),
            AgendaEvent("gym", widgetText(R.string.widget_preview_gym), "$todayLabel · 6:00 PM", isDone = false, accentColorHex = greenHex),
            AgendaEvent("dinner", widgetText(R.string.widget_preview_dinner_with_sam), "$todayLabel · 8:00 PM", isDone = false, accentColorHex = orangeHex)
        )
    )
}

@Composable
internal fun previewUpcomingEventsContent(): UpcomingEventsWidgetContent {
    val today = previewToday()
    val tomorrow = today.plus(DatePeriod(days = 1))
    val todayLabel = widgetText(R.string.widget_preview_today)
    val tomorrowLabel = widgetText(R.string.widget_preview_tomorrow)
    return UpcomingEventsWidgetContent(
        events = listOf(
            previewUpcomingEvent("standup", widgetText(R.string.widget_preview_team_sync), today, todayLabel, "10:00 AM", blueHex),
            previewUpcomingEvent("gym", widgetText(R.string.widget_preview_gym), today, todayLabel, "6:00 PM", greenHex),
            previewUpcomingEvent("dentist", widgetText(R.string.widget_preview_dentist), tomorrow, tomorrowLabel, "2:30 PM", orangeHex),
            previewUpcomingEvent("birthday", widgetText(R.string.widget_preview_alexs_birthday), tomorrow, tomorrowLabel, null, pinkHex)
        )
    )
}

private fun previewTextLine(
    key: String,
    text: String,
    style: WidgetTextStyleName = WidgetTextStyleName.BODY,
    marker: WidgetLineMarker? = null,
    isStruckThrough: Boolean = false
) = WidgetElement.TextLine(
    key = key,
    text = text,
    style = style,
    indentationLevel = 0,
    isStruckThrough = isStruckThrough,
    marker = marker
)

private fun previewTask(
    key: String,
    text: String,
    timeLabel: String? = null,
    dayLabel: String? = null
) = TasksWidgetRow.Task(
    key = key,
    blockId = key,
    text = text,
    isChecked = false,
    timeLabel = timeLabel,
    dayLabel = dayLabel
)

private fun previewUpcomingEvent(
    key: String,
    title: String,
    date: LocalDate,
    dateLabel: String,
    timeLabel: String?,
    accentColorHex: String
) = UpcomingEvent(
    blockId = key,
    title = title,
    dateGroupKey = date.toString(),
    dateLabel = dateLabel,
    timeLabel = timeLabel,
    isOverdue = false,
    accentColorHex = accentColorHex,
    weekdayShortLabel = date.dayOfWeek.name.take(3),
    dayOfMonthLabel = date.day.toString()
)

private fun previewToday(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

private fun firstOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month, 1)

private fun monthKeyOf(date: LocalDate): String = "${date.year}-${date.month.number.toString().padStart(2, '0')}"

private fun previewDotsByDay(today: LocalDate): Map<Int, List<String>> =
    mapOf(
        3 to listOf(orangeHex),
        12 to listOf(pinkHex),
        21 to listOf(blueHex),
        today.day to listOf(blueHex, greenHex)
    )

private fun previewWeekdayLabels(): List<String> = previewWeekdays.map { day -> day.name.take(1) }

private fun previewMonthGrid(today: LocalDate): List<List<LocalDate?>> {
    val monthStart = firstOfMonth(today)
    val gridStart = monthStart.minus(DatePeriod(days = previewWeekdays.indexOf(monthStart.dayOfWeek)))
    return (0 until weeksShown)
        .map { weekIndex ->
            (0 until daysPerWeek).map { dayIndex ->
                gridStart.plus(DatePeriod(days = weekIndex * daysPerWeek + dayIndex))
                    .takeIf { date -> date.month == monthStart.month }
            }
        }
        .dropLastWhile { week -> week.all { date -> date == null } }
}

private fun monthNameOf(date: LocalDate): String = capitalized(date.month.name)

private fun shortMonthNameOf(date: LocalDate): String = monthNameOf(date).take(3)

private fun capitalized(upperCaseName: String): String =
    upperCaseName.lowercase().replaceFirstChar { letter -> letter.uppercase() }
