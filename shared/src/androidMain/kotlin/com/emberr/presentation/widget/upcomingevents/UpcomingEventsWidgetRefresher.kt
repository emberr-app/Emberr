// Reads and writes each widget's cached content, and pushes freshly drawn events to the home screen.
package com.emberr.presentation.widget.upcomingevents

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

fun decodeCachedUpcomingEvents(rawCache: String?): UpcomingEventsWidgetContent? {
    if (rawCache.isNullOrBlank()) return null
    return try {
        cacheJson.decodeFromString<UpcomingEventsWidgetContent>(rawCache)
    } catch (cause: Exception) {
        WidgetLog.e("Dropped an unreadable upcoming events cache", cause)
        null
    }
}

suspend fun readCachedUpcomingEvents(context: Context, glanceId: GlanceId): UpcomingEventsWidgetContent? =
    try {
        decodeCachedUpcomingEvents(
            getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cachedUpcomingEventsKey]
        )
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the upcoming events cache", cause)
        null
    }

suspend fun writeCachedUpcomingEvents(
    context: Context,
    glanceId: GlanceId,
    content: UpcomingEventsWidgetContent
) {
    try {
        val encoded = cacheJson.encodeToString(UpcomingEventsWidgetContent.serializer(), content)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[cachedUpcomingEventsKey] = encoded }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not write the upcoming events cache", cause)
    }
}

suspend fun refreshUpcomingEventsWidget(context: Context, glanceId: GlanceId) {
    try {
        val contentReader = GlobalContext.get().get<UpcomingEventsWidgetContentReader>()
        val freshContent = contentReader.readContentOnce() ?: return

        if (freshContent == readCachedUpcomingEvents(context, glanceId)) return

        writeCachedUpcomingEvents(context, glanceId, freshContent)
        UpcomingEventsWidget().update(context, glanceId)
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh an upcoming events widget", cause)
    }
}

suspend fun refreshUpcomingEventsWidgets(context: Context) {
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(UpcomingEventsWidget::class.java)
            .forEach { glanceId -> refreshUpcomingEventsWidget(context, glanceId) }
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh the upcoming events widgets", cause)
    }
}
