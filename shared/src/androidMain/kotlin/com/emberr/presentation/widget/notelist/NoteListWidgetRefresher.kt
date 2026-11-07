// Reads and writes each widget's saved note list, and pushes freshly drawn rows to the home screen.
package com.emberr.presentation.widget.notelist

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.isPinnedSpaceDeleted
import com.emberr.presentation.widget.readWidgetSpaceId
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext

private val cacheJson = Json { ignoreUnknownKeys = true }

val deletedSpaceNoteList = NoteListWidgetContent(notes = emptyList(), isSpaceDeleted = true)

fun decodeCachedNoteList(rawCache: String?): NoteListWidgetContent? {
    if (rawCache.isNullOrBlank()) return null
    return try {
        cacheJson.decodeFromString<NoteListWidgetContent>(rawCache)
    } catch (cause: Exception) {
        WidgetLog.e("Dropped an unreadable note list cache", cause)
        null
    }
}

suspend fun readCachedNoteList(context: Context, glanceId: GlanceId): NoteListWidgetContent? =
    try {
        decodeCachedNoteList(
            getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cachedNoteListKey]
        )
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the note list cache", cause)
        null
    }

suspend fun writeCachedNoteList(context: Context, glanceId: GlanceId, content: NoteListWidgetContent) {
    try {
        val encoded = cacheJson.encodeToString(NoteListWidgetContent.serializer(), content)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[cachedNoteListKey] = encoded }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not write the note list cache", cause)
    }
}

suspend fun pushNoteListToWidgets(context: Context, notes: List<NoteMetadataEntity>) {
    try {
        val contentReader = GlobalContext.get().get<NoteListWidgetContentReader>()

        GlanceAppWidgetManager(context)
            .getGlanceIds(NoteListWidget::class.java)
            .forEach { glanceId ->
                val content = if (isPinnedSpaceDeleted(context, glanceId)) {
                    deletedSpaceNoteList
                } else {
                    contentReader.buildContent(readWidgetSpaceId(context, glanceId), notes)
                }
                if (content != readCachedNoteList(context, glanceId)) {
                    writeCachedNoteList(context, glanceId, content)
                    NoteListWidget().update(context, glanceId)
                }
            }
    } catch (cause: Exception) {
        WidgetLog.e("Could not push the note list to the widgets", cause)
    }
}

suspend fun refreshNoteListWidget(context: Context, glanceId: GlanceId) {
    try {
        val contentReader = GlobalContext.get().get<NoteListWidgetContentReader>()
        val freshContent = if (isPinnedSpaceDeleted(context, glanceId)) {
            deletedSpaceNoteList
        } else {
            contentReader.readContentOnce(readWidgetSpaceId(context, glanceId)) ?: return
        }

        if (freshContent == readCachedNoteList(context, glanceId)) return

        writeCachedNoteList(context, glanceId, freshContent)
        NoteListWidget().update(context, glanceId)
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh a note list widget", cause)
    }
}

suspend fun refreshNoteListWidgets(context: Context) {
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(NoteListWidget::class.java)
            .forEach { glanceId -> refreshNoteListWidget(context, glanceId) }
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh the note list widgets", cause)
    }
}
