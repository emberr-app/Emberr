package com.emberr.presentation.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

suspend fun readDeletedFlag(context: Context, glanceId: GlanceId, deletedKey: Preferences.Key<Boolean>): Boolean =
    try {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[deletedKey] == true
    } catch (cause: Exception) {
        WidgetLog.e("Could not read whether the widget's ${deletedKey.name} flag is set", cause)
        false
    }

suspend fun writeDeletedFlag(context: Context, glanceId: GlanceId, deletedKey: Preferences.Key<Boolean>) {
    try {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply { this[deletedKey] = true }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not set the widget's ${deletedKey.name} flag", cause)
    }
}

suspend fun markDeletedOnce(
    context: Context,
    glanceId: GlanceId,
    deletedKey: Preferences.Key<Boolean>,
    widget: GlanceAppWidget
) {
    if (readDeletedFlag(context, glanceId, deletedKey)) return

    writeDeletedFlag(context, glanceId, deletedKey)
    widget.update(context, glanceId)
}
