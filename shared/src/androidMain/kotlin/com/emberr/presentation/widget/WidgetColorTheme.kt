package com.emberr.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.presentation.widget.calendar.CalendarWidget
import com.emberr.presentation.widget.calendaragenda.CalendarAgendaWidget
import com.emberr.presentation.widget.dailyjot.DailyJotWidget
import com.emberr.presentation.widget.dailyquote.DailyQuoteWidget
import com.emberr.presentation.widget.note.NoteWidget
import com.emberr.presentation.widget.notecard.NoteCardWidget
import com.emberr.presentation.widget.notelist.NoteListWidget
import com.emberr.presentation.widget.noteshortcut.NoteShortcutWidget
import com.emberr.presentation.widget.tasks.TasksWidget
import com.emberr.presentation.widget.todaytasks.TodayTasksWidget
import com.emberr.presentation.widget.upcomingevents.UpcomingEventsWidget
import com.emberr.ui.theme.ColorThemePreference
import com.emberr.ui.theme.ThemeColorSchemes
import com.emberr.ui.theme.wallpaperColorSchemesFor
import com.emberr.ui.theme.colorThemePreferenceNamed
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.context.GlobalContext

internal val LocalWidgetColorSchemes = staticCompositionLocalOf {
    ColorThemePreference.EMBERR.builtInColorSchemes
}

@Composable
internal fun WidgetColorTheme(followsSettingChanges: Boolean = true, content: @Composable () -> Unit) {
    val settingsManager = remember { GlobalContext.getOrNull()?.getOrNull<SettingsManager>() }
    val liveSettingsManager = settingsManager?.takeIf { followsSettingChanges }
    val colorThemePreferenceFlow = remember(liveSettingsManager) {
        liveSettingsManager?.colorThemePreferenceFlow ?: emptyFlow()
    }
    val colorThemePreferenceName by colorThemePreferenceFlow.collectAsState(
        initial = settingsManager?.getColorThemePreference() ?: SyncConstants.DEFAULT_COLOR_THEME_PREFERENCE
    )
    val themedTextAndIconsFlow = remember(liveSettingsManager) {
        liveSettingsManager?.themedTextAndIconsFlow ?: emptyFlow()
    }
    val themedTextAndIcons by themedTextAndIconsFlow.collectAsState(
        initial = settingsManager?.isThemedTextAndIconsEnabled() ?: SyncConstants.DEFAULT_THEMED_TEXT_AND_ICONS
    )
    val colorThemeStrengthFlow = remember(liveSettingsManager) {
        liveSettingsManager?.colorThemeStrengthFlow ?: emptyFlow()
    }
    val colorThemeStrength by colorThemeStrengthFlow.collectAsState(
        initial = settingsManager?.getColorThemeStrength() ?: SyncConstants.DEFAULT_COLOR_THEME_STRENGTH
    )
    val colorTheme = colorThemePreferenceNamed(colorThemePreferenceName)
    val widgetContext = LocalContext.current
    val sourceColorSchemes = remember(colorTheme, widgetContext) {
        val wallpaperColorSchemes = if (colorTheme == ColorThemePreference.WALLPAPER) wallpaperColorSchemesFor(widgetContext) else null
        wallpaperColorSchemes ?: colorTheme.builtInColorSchemes
    }

    CompositionLocalProvider(
        LocalWidgetColorSchemes provides ThemeColorSchemes(
            light = colorTheme.colorSchemeFor(
                darkTheme = false,
                themedTextAndIcons = themedTextAndIcons,
                strengthPercent = colorThemeStrength,
                sourceColorSchemes = sourceColorSchemes
            ),
            dark = colorTheme.colorSchemeFor(
                darkTheme = true,
                themedTextAndIcons = themedTextAndIcons,
                strengthPercent = colorThemeStrength,
                sourceColorSchemes = sourceColorSchemes
            )
        ),
        content = content
    )
}

private fun allWidgets(): List<GlanceAppWidget> = listOf(
    CalendarWidget(),
    CalendarAgendaWidget(),
    DailyJotWidget(),
    DailyQuoteWidget(),
    NoteWidget(),
    NoteCardWidget(),
    NoteListWidget(),
    NoteShortcutWidget(),
    TasksWidget(),
    TodayTasksWidget(),
    UpcomingEventsWidget()
)

suspend fun redrawAllWidgets(context: Context) {
    allWidgets().forEach { widget ->
        try {
            widget.updateAll(context)
        } catch (cause: Exception) {
            WidgetLog.e("Could not redraw ${widget::class.simpleName} for the new color theme", cause)
        }
    }
}
