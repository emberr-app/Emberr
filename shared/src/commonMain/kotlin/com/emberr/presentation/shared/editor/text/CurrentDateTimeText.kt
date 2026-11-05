package com.emberr.presentation.shared.editor.text

import com.emberr.domain.model.shortDateText
import com.emberr.domain.util.time.formatTimeOfDay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

fun currentTimeText(usesTwentyFourHourClock: Boolean): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return formatTimeOfDay(now.hour, now.minute, usesTwentyFourHourClock)
}

fun currentDateText(): String = shortDateText(Clock.System.todayIn(TimeZone.currentSystemDefault()))
