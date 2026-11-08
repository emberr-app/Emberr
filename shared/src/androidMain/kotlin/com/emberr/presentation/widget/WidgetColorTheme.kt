package com.emberr.presentation.widget

import android.content.Context
import androidx.compose.material3.ColorScheme
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
import kotlinx.coroutines.flow.flowOf
import org.koin.core.context.GlobalContext

internal class WidgetColorSchemes(val light: ColorScheme, val dark: ColorScheme)

internal val LocalWidgetColorSchemes = staticCompositionLocalOf {
    WidgetColorSchemes(
        light = ColorThemePreference.EMBERR.lightColorScheme,
        dark = ColorThemePreference.EMBERR.darkColorScheme
    )
}

@Composable
internal fun WidgetColorTheme(content: @Composable () -> Unit) {
    val settingsManager = remember { GlobalContext.getOrNull()?.getOrNull<SettingsManager>() }
    val colorThemePreferenceFlow = remember(settingsManager) {
        settingsManager?.colorThemePreferenceFlow ?: flowOf(SyncConstants.DEFAULT_COLOR_THEME_PREFERENCE)
    }
    val colorThemePreferenceName by colorThemePreferenceFlow.collectAsState(
        initial = settingsManager?.getColorThemePreference() ?: SyncConstants.DEFAULT_COLOR_THEME_PREFERENCE
    )
    val themedTextAndIconsFlow = remember(settingsManager) {
        settingsManager?.themedTextAndIconsFlow ?: flowOf(SyncConstants.DEFAULT_THEMED_TEXT_AND_ICONS)
    }
    val themedTextAndIcons by themedTextAndIconsFlow.collectAsState(
        initial = settingsManager?.isThemedTextAndIconsEnabled() ?: SyncConstants.DEFAULT_THEMED_TEXT_AND_ICONS
    )
    val colorThemeStrengthFlow = remember(settingsManager) {
        settingsManager?.colorThemeStrengthFlow ?: flowOf(SyncConstants.DEFAULT_COLOR_THEME_STRENGTH)
    }
    val colorThemeStrength by colorThemeStrengthFlow.collectAsState(
        initial = settingsManager?.getColorThemeStrength() ?: SyncConstants.DEFAULT_COLOR_THEME_STRENGTH
    )
    val colorTheme = colorThemePreferenceNamed(colorThemePreferenceName)
    val widgetContext = LocalContext.current
    val sourceColorSchemes = remember(colorTheme, widgetContext) {
        val wallpaperColorSchemes = if (colorTheme == ColorThemePreference.WALLPAPER) wallpaperColorSchemesFor(widgetContext) else null
        wallpaperColorSchemes ?: ThemeColorSchemes(colorTheme.lightColorScheme, colorTheme.darkColorScheme)
    }

    CompositionLocalProvider(
        LocalWidgetColorSchemes provides WidgetColorSchemes(
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
