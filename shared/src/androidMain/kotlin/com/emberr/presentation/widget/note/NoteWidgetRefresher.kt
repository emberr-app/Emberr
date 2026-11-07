// Reads and writes each widget's saved content, and pushes freshly drawn content to the home screen.
package com.emberr.presentation.widget.note

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.WidgetNoteSource
import com.emberr.presentation.widget.markDeletedOnce
import com.emberr.presentation.widget.readDeletedFlag

private val cacheJson = Json { ignoreUnknownKeys = true }

fun decodeCachedContent(rawCache: String?): WidgetNoteContent? {
    if (rawCache.isNullOrBlank()) return null
    return try {
        cacheJson.decodeFromString<WidgetNoteContent>(rawCache)
    } catch (cause: Exception) {
        WidgetLog.e("Dropped an unreadable content cache", cause)
        null
    }
}

suspend fun readSelectedNoteId(context: Context, glanceId: GlanceId): String? =
    try {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[selectedNoteIdKey]
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the chosen note id", cause)
        null
    }

suspend fun readCachedContent(context: Context, glanceId: GlanceId): WidgetNoteContent? =
    try {
        decodeCachedContent(
            getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cachedContentKey]
        )
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the content cache", cause)
        null
    }

suspend fun writeCachedContent(context: Context, glanceId: GlanceId, content: WidgetNoteContent) {
    try {
        val encoded = cacheJson.encodeToString(WidgetNoteContent.serializer(), content)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply {
                this[cachedContentKey] = encoded
                remove(noteDeletedKey)
            }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not write the content cache", cause)
    }
}

suspend fun findWidgetsShowingNote(context: Context, noteId: String): List<GlanceId> =
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(NoteWidget::class.java)
            .filter { glanceId -> readSelectedNoteId(context, glanceId) == noteId }
    } catch (cause: Exception) {
        WidgetLog.e("Could not list widgets showing note $noteId", cause)
        emptyList()
    }

suspend fun readAllSelectedNoteIds(context: Context): Set<String> =
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(NoteWidget::class.java)
            .mapNotNull { glanceId -> readSelectedNoteId(context, glanceId)?.takeIf { it.isNotBlank() } }
            .toSet()
    } catch (cause: Exception) {
        WidgetLog.e("Could not list the chosen notes", cause)
        emptySet()
    }

suspend fun pushContentToWidgets(context: Context, noteId: String, content: WidgetNoteContent) {
    val targets = findWidgetsShowingNote(context, noteId)
    if (targets.isEmpty()) return

    for (glanceId in targets) {
        writeCachedContent(context, glanceId, content)
        NoteWidget().update(context, glanceId)
    }
}

suspend fun refreshNoteWidgets(context: Context) {
    try {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(NoteWidget::class.java)
        if (glanceIds.isEmpty()) return

        val contentReader = GlobalContext.get().get<WidgetContentReader>()
        val noteSource = GlobalContext.get().get<WidgetNoteSource>()

        for (glanceId in glanceIds) {
            val noteId = readSelectedNoteId(context, glanceId)?.takeIf { it.isNotBlank() } ?: continue
            val freshContent = contentReader.readNoteContentOnce(noteId)
            if (freshContent == null) {
                if (noteSource.isNoteDeleted(noteId)) markDeletedOnce(context, glanceId, noteDeletedKey, NoteWidget())
                continue
            }
            val isUnchanged = freshContent == readCachedContent(context, glanceId) &&
                !readDeletedFlag(context, glanceId, noteDeletedKey)
            if (isUnchanged) continue

            writeCachedContent(context, glanceId, freshContent)
            NoteWidget().update(context, glanceId)
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh the note widgets", cause)
    }
}
