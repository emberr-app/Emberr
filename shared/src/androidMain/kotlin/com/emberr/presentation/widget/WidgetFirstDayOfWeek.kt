package com.emberr.presentation.widget

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.util.time.androidSystemFirstDayOfWeek
import com.emberr.domain.util.time.firstDayOfWeekPreferenceNamed
import kotlinx.datetime.DayOfWeek

class WidgetFirstDayOfWeek(private val settingsManager: SettingsManager) {
    fun firstDayOfWeek(): DayOfWeek =
        firstDayOfWeekPreferenceNamed(settingsManager.getFirstDayOfWeekPreference())
            .firstDayOfWeek(androidSystemFirstDayOfWeek())
}
