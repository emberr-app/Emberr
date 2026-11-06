// Reads and writes each widget's shown month and content, and pushes a freshly drawn month to the home screen.
package com.emberr.presentation.widget.calendaragenda

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

fun decodeCachedAgenda(rawCache: String?): CalendarAgendaWidgetContent? {
    if (rawCache.isNullOrBlank()) return null
    return try {
        cacheJson.decodeFromString<CalendarAgendaWidgetContent>(rawCache)
    } catch (cause: Exception) {
        WidgetLog.e("Dropped an unreadable agenda cache", cause)
        null
    }
}

suspend fun readAgendaShownMonth(context: Context, glanceId: GlanceId): String? =
    try {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[agendaShownMonthKey]
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the shown month", cause)
        null
    }

suspend fun readCachedAgenda(context: Context, glanceId: GlanceId): CalendarAgendaWidgetContent? =
    try {
        decodeCachedAgenda(
            getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cachedAgendaKey]
        )
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the agenda cache", cause)
        null
    }

suspend fun writeAgendaShownMonth(context: Context, glanceId: GlanceId, shownMonth: String) {
    try {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[agendaShownMonthKey] = shownMonth }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not save the shown month", cause)
    }
}

suspend fun writeCachedAgenda(
    context: Context,
    glanceId: GlanceId,
    content: CalendarAgendaWidgetContent
) {
    try {
        val encoded = cacheJson.encodeToString(CalendarAgendaWidgetContent.serializer(), content)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[cachedAgendaKey] = encoded }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not write the agenda cache", cause)
    }
}

suspend fun refreshCalendarAgendaWidget(context: Context, glanceId: GlanceId) {
    try {
        val contentReader = GlobalContext.get().get<CalendarAgendaWidgetContentReader>()
        val shownMonth = readAgendaShownMonth(context, glanceId)
        val freshContent = contentReader.readContentOnce(shownMonth) ?: return

        if (freshContent == readCachedAgenda(context, glanceId)) return

        writeCachedAgenda(context, glanceId, freshContent)
        CalendarAgendaWidget().update(context, glanceId)
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh an agenda widget", cause)
    }
}

suspend fun refreshCalendarAgendaWidgets(context: Context) {
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(CalendarAgendaWidget::class.java)
            .forEach { glanceId -> refreshCalendarAgendaWidget(context, glanceId) }
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh the agenda widgets", cause)
    }
}
