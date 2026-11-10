package com.emberr.presentation.widget

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.emberr.presentation.widget.calendar.CalendarWidgetReceiver
import com.emberr.presentation.widget.calendaragenda.CalendarAgendaWidgetReceiver
import com.emberr.presentation.widget.dailyjot.DailyJotWidgetReceiver
import com.emberr.presentation.widget.dailyquote.DailyQuoteWidgetReceiver
import com.emberr.presentation.widget.note.NoteWidgetReceiver
import com.emberr.presentation.widget.notecard.NoteCardWidgetReceiver
import com.emberr.presentation.widget.notelist.NoteListWidgetReceiver
import com.emberr.presentation.widget.noteshortcut.NoteShortcutWidgetReceiver
import com.emberr.presentation.widget.tasks.TasksWidgetReceiver
import com.emberr.presentation.widget.todaytasks.TodayTasksWidgetReceiver
import com.emberr.presentation.widget.upcomingevents.UpcomingEventsWidgetReceiver
import java.util.Locale
import kotlin.reflect.KClass

private const val widgetPreviewPreferencesName = "widget_previews"
private const val publishedForInstallTimeKey = "published_for_install_time"
private const val publishedForLanguageKey = "published_for_language"

private val receiversWithPreviews: List<KClass<out GlanceAppWidgetReceiver>> = listOf(
    NoteWidgetReceiver::class,
    NoteCardWidgetReceiver::class,
    NoteShortcutWidgetReceiver::class,
    NoteListWidgetReceiver::class,
    TasksWidgetReceiver::class,
    TodayTasksWidgetReceiver::class,
    CalendarWidgetReceiver::class,
    CalendarAgendaWidgetReceiver::class,
    UpcomingEventsWidgetReceiver::class,
    DailyJotWidgetReceiver::class,
    DailyQuoteWidgetReceiver::class
)

suspend fun publishWidgetPreviewsAfterInstallOrUpdate(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

    val preferences = context.getSharedPreferences(widgetPreviewPreferencesName, Context.MODE_PRIVATE)
    val installTime = try {
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the app install time for widget previews", cause)
        return
    }
    val languageTag = Locale.getDefault().toLanguageTag()
    val isAlreadyPublished = preferences.getLong(publishedForInstallTimeKey, 0L) == installTime &&
        preferences.getString(publishedForLanguageKey, null) == languageTag
    if (isAlreadyPublished) return

    val widgetManager = GlanceAppWidgetManager(context)
    val publishResults = receiversWithPreviews.map { receiver ->
        try {
            widgetManager.setWidgetPreviews(receiver) == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS
        } catch (cause: Exception) {
            WidgetLog.e("Could not publish the preview for ${receiver.simpleName}", cause)
            false
        }
    }

    if (publishResults.all { isPublished -> isPublished }) {
        preferences.edit {
            putLong(publishedForInstallTimeKey, installTime)
            putString(publishedForLanguageKey, languageTag)
        }
    }
}
