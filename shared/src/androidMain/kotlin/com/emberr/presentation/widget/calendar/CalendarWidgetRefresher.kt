// Reads and writes each widget's shown month and grid, and pushes a freshly drawn month to the home screen.
package com.emberr.presentation.widget.calendar

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.emberr.presentation.widget.WidgetLog
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext

private val cacheJson = Json { ignoreUnknownKeys = true }

fun decodeCachedCalendar(rawCache: String?): CalendarWidgetContent? {
    if (rawCache.isNullOrBlank()) return null
    return try {
        cacheJson.decodeFromString<CalendarWidgetContent>(rawCache)
    } catch (cause: Exception) {
        WidgetLog.e("Dropped an unreadable calendar cache", cause)
        null
    }
}

suspend fun readShownMonth(context: Context, glanceId: GlanceId): String? =
    try {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[shownMonthKey]
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the shown month", cause)
        null
    }

suspend fun readCachedCalendar(context: Context, glanceId: GlanceId): CalendarWidgetContent? =
    try {
        decodeCachedCalendar(
            getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cachedCalendarKey]
        )
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the calendar cache", cause)
        null
    }

suspend fun writeShownMonth(context: Context, glanceId: GlanceId, shownMonth: String) {
    try {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[shownMonthKey] = shownMonth }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not save the shown month", cause)
    }
}

suspend fun writeCachedCalendar(context: Context, glanceId: GlanceId, content: CalendarWidgetContent) {
    try {
        val encoded = cacheJson.encodeToString(CalendarWidgetContent.serializer(), content)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[cachedCalendarKey] = encoded }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not write the calendar cache", cause)
    }
}

suspend fun refreshCalendarWidget(context: Context, glanceId: GlanceId) {
    try {
        val contentReader = GlobalContext.get().get<CalendarWidgetContentReader>()
        val shownMonth = readShownMonth(context, glanceId)
        val freshContent = contentReader.readContentOnce(shownMonth) ?: return

        if (freshContent == readCachedCalendar(context, glanceId)) return

        writeCachedCalendar(context, glanceId, freshContent)
        CalendarWidget().update(context, glanceId)
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh a calendar widget", cause)
    }
}

suspend fun refreshCalendarWidgets(context: Context) {
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(CalendarWidget::class.java)
            .forEach { glanceId -> refreshCalendarWidget(context, glanceId) }
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh the calendar widgets", cause)
    }
}
