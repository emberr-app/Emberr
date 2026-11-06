// Saves today's quote into each quote widget and redraws the widgets when the day changes.
package com.emberr.presentation.widget.dailyquote

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.emberr.domain.quote.DailyQuoteLibrary
import com.emberr.presentation.widget.WidgetLog
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

val quoteTextKey = stringPreferencesKey("daily_quote_text")
val quoteAuthorKey = stringPreferencesKey("daily_quote_author")
private val quoteDateKey = stringPreferencesKey("daily_quote_date")

suspend fun saveTodaysQuoteIfNeeded(context: Context, glanceId: GlanceId): Boolean =
    try {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val savedDate = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[quoteDateKey]

        if (savedDate == today.toString()) {
            false
        } else {
            val quote = DailyQuoteLibrary.quoteForDate(today)
            if (quote == null) {
                false
            } else {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
                    preferences.toMutablePreferences().apply {
                        this[quoteTextKey] = quote.text
                        this[quoteAuthorKey] = quote.author
                        this[quoteDateKey] = today.toString()
                    }
                }
                true
            }
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not save today's quote into a widget", cause)
        false
    }

suspend fun refreshDailyQuoteWidgets(context: Context) {
    try {
        GlanceAppWidgetManager(context)
            .getGlanceIds(DailyQuoteWidget::class.java)
            .forEach { glanceId ->
                if (saveTodaysQuoteIfNeeded(context, glanceId)) {
                    DailyQuoteWidget().update(context, glanceId)
                }
            }
    } catch (cause: Exception) {
        WidgetLog.e("Could not refresh the daily quote widgets", cause)
    }
}
