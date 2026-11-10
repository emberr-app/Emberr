package com.emberr.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.emberr.presentation.widget.calendar.CalendarWidget
import com.emberr.presentation.widget.calendar.refreshCalendarWidgets
import com.emberr.presentation.widget.calendaragenda.CalendarAgendaWidget
import com.emberr.presentation.widget.calendaragenda.refreshCalendarAgendaWidgets
import com.emberr.presentation.widget.dailyjot.DailyJotWidget
import com.emberr.presentation.widget.dailyquote.DailyQuoteWidget
import com.emberr.presentation.widget.dailyquote.refreshDailyQuoteWidgets
import com.emberr.presentation.widget.note.NoteWidget
import com.emberr.presentation.widget.note.refreshNoteWidgets
import com.emberr.presentation.widget.notecard.NoteCardWidget
import com.emberr.presentation.widget.notecard.refreshNoteCardWidgets
import com.emberr.presentation.widget.notelist.NoteListWidget
import com.emberr.presentation.widget.notelist.refreshNoteListWidgets
import com.emberr.presentation.widget.noteshortcut.NoteShortcutWidget
import com.emberr.presentation.widget.noteshortcut.refreshNoteShortcutWidgets
import com.emberr.presentation.widget.tasks.TasksWidget
import com.emberr.presentation.widget.tasks.refreshTaskWidgets
import com.emberr.presentation.widget.todaytasks.TodayTasksWidget
import com.emberr.presentation.widget.todaytasks.refreshTodayTasksWidgets
import com.emberr.presentation.widget.upcomingevents.UpcomingEventsWidget
import com.emberr.presentation.widget.upcomingevents.refreshUpcomingEventsWidgets

suspend fun refreshWidgetsForNewLanguage(context: Context) {
    refreshNoteWidgets(context)
    refreshNoteCardWidgets(context)
    refreshNoteShortcutWidgets(context)
    refreshNoteListWidgets(context)
    refreshTaskWidgets(context)
    refreshTodayTasksWidgets(context)
    refreshCalendarWidgets(context)
    refreshCalendarAgendaWidgets(context)
    refreshUpcomingEventsWidgets(context)
    refreshDailyQuoteWidgets(context)

    val everyWidget = listOf(
        NoteWidget(),
        NoteCardWidget(),
        NoteShortcutWidget(),
        NoteListWidget(),
        TasksWidget(),
        TodayTasksWidget(),
        CalendarWidget(),
        CalendarAgendaWidget(),
        UpcomingEventsWidget(),
        DailyJotWidget(),
        DailyQuoteWidget()
    )
    everyWidget.forEach { widget ->
        try {
            widget.updateAll(context)
        } catch (cause: Exception) {
            WidgetLog.e("Could not redraw ${widget::class.simpleName} in the new language", cause)
        }
    }

    publishWidgetPreviewsAfterInstallOrUpdate(context)
}
