package com.emberr.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

suspend fun readWidgetState(context: Context, glanceId: GlanceId): Preferences? =
    try {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
    } catch (cause: Exception) {
        WidgetLog.e("Could not read the widget's saved state", cause)
        null
    }

@Composable
fun GlanceAppWidget.RedrawIfStateChangedAtStart(context: Context, glanceId: GlanceId, stateChangedAtStart: Boolean) {
    LaunchedEffect(stateChangedAtStart) {
        if (stateChangedAtStart) update(context, glanceId)
    }
}
