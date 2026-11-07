// Reads and writes each widget's saved note name, and pushes a freshly drawn shortcut to the home screen.
package com.emberr.presentation.widget.noteshortcut

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
import com.emberr.presentation.widget.markDeletedOnce
import com.emberr.presentation.widget.readDeletedFlag

private val cacheJson = Json { ignoreUnknownKeys = true }

fun decodeCachedShortcut(rawCache: String?): NoteShortcutWidgetContent? {
    if (rawCache.isNullOrBlank()) return null
    return try {
        cacheJson.decodeFromString<NoteShortcutWidgetContent>(rawCache)
    } catch (cause: Exception) {
        WidgetLog.e("Dropped an unreadable shortcut cache", cause)
        null
    }
}

suspend fun readShortcutNoteId(context: Context, glanceId: GlanceId): String? =
    try {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[shortcutNoteIdKey]
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the shortcut's note id", cause)
        null
    }

suspend fun readCachedShortcut(context: Context, glanceId: GlanceId): NoteShortcutWidgetContent? =
    try {
        decodeCachedShortcut(
            getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[cachedShortcutKey]
        )
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the shortcut cache", cause)
        null
    }

suspend fun writeCachedShortcut(context: Context, glanceId: GlanceId, content: NoteShortcutWidgetContent) {
    try {
        val encoded = cacheJson.encodeToString(NoteShortcutWidgetContent.serializer(), content)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply {
                this[cachedShortcutKey] = encoded
                remove(shortcutNoteDeletedKey)
            }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not write the shortcut cache", cause)
    }
}

suspend fun refreshNoteShortcutWidget(context: Context, glanceId: GlanceId) {
    try {
        val noteId = readShortcutNoteId(context, glanceId)?.takeIf { it.isNotBlank() } ?: return
        val contentReader = GlobalContext.get().get<NoteShortcutWidgetContentReader>()
        val freshContent = contentReader.readContentOnce(noteId)
        if (freshContent == null) {
            if (GlobalContext.get().get<WidgetNoteSource>().isNoteDeleted(noteId)) {
                markDeletedOnce(context, glanceId, shortcutNoteDeletedKey, NoteShortcutWidget())
            }
            return
        }

        val isUnchanged = freshContent == readCachedShortcut(context, glanceId) &&
            !readDeletedFlag(context, glanceId, shortcutNoteDeletedKey)
        if (isUnchanged) return

        writeCachedShortcut(context, glanceId, freshContent)
        NoteShortcutWidget().update(context, glanceId)
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh a shortcut widget", cause)
    }
}

suspend fun refreshNoteShortcutWidgets(context: Context) {
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(NoteShortcutWidget::class.java)
            .forEach { glanceId -> refreshNoteShortcutWidget(context, glanceId) }
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh the shortcut widgets", cause)
    }
}
