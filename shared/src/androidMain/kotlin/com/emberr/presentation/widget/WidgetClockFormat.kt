package com.emberr.presentation.widget

import android.content.Context
import android.text.format.DateFormat
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.util.time.formatTimeOfDay
import com.emberr.domain.util.time.timeFormatPreferenceNamed
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class WidgetClockFormat(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    fun clockTimeOf(timestamp: Long): String =
        clockTimeOf(Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault()))

    fun clockTimeOf(moment: LocalDateTime): String {
        val usesTwentyFourHourClock = timeFormatPreferenceNamed(settingsManager.getTimeFormatPreference())
            .usesTwentyFourHourClock(DateFormat.is24HourFormat(context))
        return formatTimeOfDay(moment.hour, moment.minute, usesTwentyFourHourClock).lowercase()
    }
}
