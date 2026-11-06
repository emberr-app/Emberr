package com.emberr.presentation.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.emberr.presentation.widget.dailyquote.refreshDailyQuoteWidgets
import com.emberr.presentation.widget.calendar.refreshCalendarWidgets
import com.emberr.presentation.widget.calendaragenda.refreshCalendarAgendaWidgets
import com.emberr.presentation.widget.tasks.refreshTaskWidgets
import com.emberr.presentation.widget.todaytasks.refreshTodayTasksWidgets
import com.emberr.presentation.widget.upcomingevents.refreshUpcomingEventsWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

private const val refreshWidgetsForNewDayAction = "com.emberr.widget.REFRESH_FOR_NEW_DAY"
private const val minutesAfterMidnightToRefresh = 1L

fun scheduleWidgetRefreshForNextDay(context: Context) {
    val timeZone = TimeZone.currentSystemDefault()
    val tomorrow = Clock.System.todayIn(timeZone).plus(DatePeriod(days = 1))
    val refreshAtMillis = tomorrow.atStartOfDayIn(timeZone).toEpochMilliseconds() + minutesAfterMidnightToRefresh * 60_000L

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val refreshIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, WidgetDayChangeReceiver::class.java).setAction(refreshWidgetsForNewDayAction),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    try {
        alarmManager.set(AlarmManager.RTC, refreshAtMillis, refreshIntent)
    } catch (cause: SecurityException) {
        WidgetLog.e("Could not schedule the widget refresh for the next day", cause)
    }
}

class WidgetDayChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val applicationContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                refreshTodayTasksWidgets(applicationContext)
                refreshTaskWidgets(applicationContext)
                refreshUpcomingEventsWidgets(applicationContext)
                refreshCalendarWidgets(applicationContext)
                refreshCalendarAgendaWidgets(applicationContext)
                refreshDailyQuoteWidgets(applicationContext)
            } catch (cause: Exception) {
                WidgetLog.e("Could not refresh the widgets for the new day", cause)
            } finally {
                scheduleWidgetRefreshForNextDay(applicationContext)
                pendingResult.finish()
            }
        }
    }
}
