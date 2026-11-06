// Reads and writes each card widget's saved note, and redraws cards when their note changes or is deleted.
package com.emberr.presentation.widget.notecard

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.WidgetNoteSource
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext

private val cacheJson = Json { ignoreUnknownKeys = true }

fun decodeCachedCard(rawCache: String?): NoteCardWidgetContent? {
    if (rawCache.isNullOrBlank()) return null
    return try {
        cacheJson.decodeFromString<NoteCardWidgetContent>(rawCache)
    } catch (cause: Exception) {
        WidgetLog.e("Dropped an unreadable note card cache", cause)
        null
    }
}

suspend fun readCardNoteId(context: Context, glanceId: GlanceId): String? =
    try {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cardNoteIdKey]
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the card's note id", cause)
        null
    }

suspend fun readCachedCard(context: Context, glanceId: GlanceId): NoteCardWidgetContent? =
    try {
        decodeCachedCard(getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cachedCardKey])
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the note card cache", cause)
        null
    }

suspend fun writeCachedCard(context: Context, glanceId: GlanceId, content: NoteCardWidgetContent) {
    try {
        val encoded = cacheJson.encodeToString(NoteCardWidgetContent.serializer(), content)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply {
                this[cachedCardKey] = encoded
                remove(cardNoteDeletedKey)
            }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not write the note card cache", cause)
    }
}

suspend fun readCardNoteDeleted(context: Context, glanceId: GlanceId): Boolean =
    try {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cardNoteDeletedKey] == true
    } catch (cause: Exception) {
        WidgetLog.e("Could not read whether the card's note was deleted", cause)
        false
    }

suspend fun writeCardNoteDeleted(context: Context, glanceId: GlanceId) {
    try {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[cardNoteDeletedKey] = true }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not mark the card's note as deleted", cause)
    }
}

suspend fun refreshNoteCardWidget(context: Context, glanceId: GlanceId) {
    try {
        val noteId = readCardNoteId(context, glanceId)?.takeIf { it.isNotBlank() } ?: return
        val freshContent = GlobalContext.get().get<NoteCardWidgetContentReader>().readContentOnce(noteId)

        if (freshContent == null) {
            val isDeleted = GlobalContext.get().get<WidgetNoteSource>().isNoteDeleted(noteId)
            if (isDeleted && !readCardNoteDeleted(context, glanceId)) {
                writeCardNoteDeleted(context, glanceId)
                NoteCardWidget().update(context, glanceId)
            }
            return
        }

        val isUnchanged = freshContent == readCachedCard(context, glanceId) &&
            !readCardNoteDeleted(context, glanceId)
        if (isUnchanged) return

        writeCachedCard(context, glanceId, freshContent)
        NoteCardWidget().update(context, glanceId)
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh a note card widget", cause)
    }
}

suspend fun refreshNoteCardWidgets(context: Context) {
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(NoteCardWidget::class.java)
            .forEach { glanceId -> refreshNoteCardWidget(context, glanceId) }
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh the note card widgets", cause)
    }
}
