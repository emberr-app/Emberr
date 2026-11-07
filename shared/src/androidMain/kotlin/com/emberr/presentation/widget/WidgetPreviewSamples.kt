package com.emberr.presentation.widget

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
internal const val previewQuoteText = "The secret of getting ahead is getting started."
internal const val previewQuoteAuthor = "Mark Twain"

private const val blueHex = "#64B5F6"
private const val greenHex = "#81C784"
private const val orangeHex = "#FFB74D"
private const val pinkHex = "#FFC6FF"
private const val daysPerWeek = 7
private const val weeksShown = 6
private val previewWeekdays = daysOfWeekStartingFrom(DayOfWeek.SUNDAY)

internal val previewNoteContent = WidgetNoteContent(
    title = "Weekend trip",
    elements = listOf(
        previewTextLine("packing", "Packing", WidgetTextStyleName.SUBHEADING),
        previewTextLine("passport", "Passport", marker = WidgetLineMarker.UNCHECKED_BOX),
        previewTextLine("tickets", "Train tickets", marker = WidgetLineMarker.CHECKED_BOX, isStruckThrough = true),
        previewTextLine("charger", "Camera charger", marker = WidgetLineMarker.UNCHECKED_BOX),
        WidgetElement.DividerLine(key = "divider"),
        previewTextLine("plan", "Leave by 8am to beat the traffic."),
        previewTextLine("lunch", "Lunch at the harbour", marker = WidgetLineMarker.BULLET),
        previewTextLine("museum", "Museum before it closes at 5", marker = WidgetLineMarker.BULLET)
    )
)

internal val previewNoteCardContent = NoteCardWidgetContent(
    noteId = previewNoteId,
    title = "Reading list",
    icon = "📚",
    snippet = "The Overstory, Piranesi, Project Hail Mary and A Psalm for the Wild-Built.",
    isFavorite = true,
    isCanvas = false,
    coverImageAbsolutePath = null
)

internal val previewNoteShortcutContent = NoteShortcutWidgetContent(
    noteId = previewNoteId,
    title = "Ideas",
    icon = "💡"
)

internal val previewNoteListContent = NoteListWidgetContent(
    notes = listOf(
        NoteListWidgetRow("trip", "Weekend trip", "Passport, train tickets, camera charger", "🧳"),
        NoteListWidgetRow("reading", "Reading list", "The Overstory, Piranesi, Project Hail Mary", "📚"),
        NoteListWidgetRow("meeting", "Meeting notes", "Agree on the launch date", "🗒️"),
        NoteListWidgetRow("recipes", "Recipes", "Lemon pasta, miso soup", "🍋")
    )
)

internal val previewTasksContent = TasksWidgetContent(
    isShowingCompleted = false,
    rows = listOf(
        previewTask("dentist", "Call the dentist", timeLabel = "10:30 AM"),
        previewTask("slides", "Finish the slides"),
        TasksWidgetRow.SectionHeading(key = "groceries", label = "Groceries"),
        previewTask("milk", "Oat milk"),
        previewTask("bread", "Bread"),
        TasksWidgetRow.SectionHeading(key = "repeating", label = "Repeating"),
        previewTask("plants", "Water the plants", timeLabel = "9:00 AM", dayLabel = "Today")
    )
)

internal fun previewTodayTasksContent(): TodayTasksWidgetContent {
    val today = previewToday()
    return TodayTasksWidgetContent(
        dateLabel = "${monthNameOf(today)} ${today.day}",
        weekdayLabel = capitalized(today.dayOfWeek.name),
        rows = listOf(
            TodayTaskRow("plants", "Water the plants", isChecked = true, scheduleLabel = "9:00 AM", isOverdue = false),
            TodayTaskRow("dentist", "Call the dentist", isChecked = false, scheduleLabel = "10:30 AM", isOverdue = false),
            TodayTaskRow("slides", "Finish the slides", isChecked = false, scheduleLabel = null, isOverdue = false)
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
            AgendaEvent("standup", "Team sync", "$todayLabel · 10:00 AM", isDone = false, accentColorHex = blueHex),
            AgendaEvent("gym", "Gym", "$todayLabel · 6:00 PM", isDone = false, accentColorHex = greenHex),
            AgendaEvent("dinner", "Dinner with Sam", "$todayLabel · 8:00 PM", isDone = false, accentColorHex = orangeHex)
        )
    )
}

internal fun previewUpcomingEventsContent(): UpcomingEventsWidgetContent {
    val today = previewToday()
    val tomorrow = today.plus(DatePeriod(days = 1))
    return UpcomingEventsWidgetContent(
        events = listOf(
            previewUpcomingEvent("standup", "Team sync", today, "Today", "10:00 AM", blueHex),
            previewUpcomingEvent("gym", "Gym", today, "Today", "6:00 PM", greenHex),
            previewUpcomingEvent("dentist", "Dentist", tomorrow, "Tomorrow", "2:30 PM", orangeHex),
            previewUpcomingEvent("birthday", "Alex's birthday", tomorrow, "Tomorrow", null, pinkHex)
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
