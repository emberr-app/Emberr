package com.emberr.presentation.shared.editor.text

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.emberr.domain.util.time.DateTextStyle
import com.emberr.domain.util.time.formatDate
import com.emberr.domain.util.time.formatTimeOfDay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

fun currentTimeText(usesTwentyFourHourClock: Boolean): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return formatTimeOfDay(now.hour, now.minute, usesTwentyFourHourClock)
}

fun currentDateText(): String =
    formatDate(Clock.System.todayIn(TimeZone.currentSystemDefault()), DateTextStyle.SHORT_MONTH_DAY_YEAR)

fun TextFieldValue.withTextInsertedAtCursor(text: String): TextFieldValue {
    val selectionStart = selection.min
    val selectionEnd = selection.max
    val spaceBefore = if (selectionStart > 0 && !this.text[selectionStart - 1].isWhitespace()) " " else ""
    val spaceAfter = if (selectionEnd < this.text.length && this.text[selectionEnd].isWhitespace()) "" else " "
    val insertedText = spaceBefore + text + spaceAfter
    val newText = this.text.substring(0, selectionStart) + insertedText + this.text.substring(selectionEnd)
    return copy(text = newText, selection = TextRange(selectionStart + insertedText.length), composition = null)
}
