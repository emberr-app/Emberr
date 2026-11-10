package com.emberr.presentation.shared.editor.blockViews.checkbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.DEFAULT_ALL_DAY_REMINDER_HOUR
import com.emberr.domain.model.lastDate
import com.emberr.domain.util.time.formatTimeOfDay
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.MinimalDatePickerDialog
import com.emberr.presentation.shared.components.MinimalTimePickerDialog
import com.emberr.presentation.shared.components.ReminderPresetMenu
import com.emberr.presentation.shared.components.TimePresetMenu
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.calendar_add
import emberr.shared.generated.resources.clock_circle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import com.emberr.presentation.shared.editor.EditorActions
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Stable
import emberr.shared.generated.resources.editor_reminder_all_day
import emberr.shared.generated.resources.editor_reminder_all_day_range
import emberr.shared.generated.resources.editor_reminder_date_and_time
import org.jetbrains.compose.resources.stringResource
import emberr.shared.generated.resources.editor_date
import emberr.shared.generated.resources.editor_event_options
import emberr.shared.generated.resources.editor_more_details
import emberr.shared.generated.resources.editor_time

private fun shortDateLabel(date: LocalDate): String =
    "${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.day}"

private fun CheckboxBlock.reminderTimestampOnNewDate(newTimestamp: Long): Long {
    val currentTimestamp = reminderTimestamp
    if (allDay == null || currentTimestamp == null) return newTimestamp
    val currentTime = Instant.fromEpochMilliseconds(currentTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    return reminderTimestampAtTime(newTimestamp, currentTime.hour, currentTime.minute)
}

private fun reminderTimestampAtTime(currentTimestamp: Long?, hour: Int, minute: Int): Long {
    val timeZone = TimeZone.currentSystemDefault()
    val currentInstant = currentTimestamp?.let { Instant.fromEpochMilliseconds(it) } ?: Clock.System.now()
    val currentDateTime = currentInstant.toLocalDateTime(timeZone)
    val newDateTime = LocalDateTime(
        currentDateTime.year, currentDateTime.month.number, currentDateTime.day,
        hour, minute, 0, 0
    )
    return newDateTime.toInstant(timeZone).toEpochMilliseconds()
}

@Stable
internal class ReminderPickerState {
    var showPresetMenu by mutableStateOf(false)
    var showTimePresetMenu by mutableStateOf(false)
    var showDatePicker by mutableStateOf(false)
    var showTimePicker by mutableStateOf(false)
    var isReminderPickerOpening by mutableStateOf(false)
}

@Composable
internal fun ColumnScope.CheckboxReminderRow(
    block: CheckboxBlock,
    actions: EditorActions,
    isActiveBlock: Boolean,
    pickerState: ReminderPickerState,
    isKeyboardOpen: Boolean,
    afterKeyboardCloses: (action: () -> Unit) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val hasReminder = block.reminderTimestamp != null
    val isPickerOpen = pickerState.showPresetMenu || pickerState.showTimePresetMenu || pickerState.showDatePicker ||
        pickerState.showTimePicker || pickerState.isReminderPickerOpening
    AnimatedVisibility(
        visible = isActiveBlock || hasReminder || isPickerOpen,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasReminder) {
                IconTooltip(text = stringResource(Res.string.editor_more_details)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            val occurrenceDate = block.reminderTimestamp.let {
                                Instant.fromEpochMilliseconds(it)
                                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                            }
                            afterKeyboardCloses {
                                actions.onOpenEventOptions(block.id, occurrenceDate)
                            }
                        }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert, stringResource(Res.string.editor_event_options),
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            Box {
                IconTooltip(text = stringResource(Res.string.editor_date)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                pickerState.isReminderPickerOpening = true
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                scope.launch {
                                    if (isKeyboardOpen) delay(500L.milliseconds) else delay(
                                        50L.milliseconds
                                    )
                                    pickerState.showPresetMenu = true
                                    pickerState.isReminderPickerOpening = false
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(Res.drawable.calendar_add), stringResource(Res.string.editor_date),
                            modifier = Modifier.size(15.dp),
                            tint = if (hasReminder) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                ReminderPresetMenu(
                    expanded = pickerState.showPresetMenu,
                    onDismiss = { pickerState.showPresetMenu = false },
                    onPresetSelected = {
                        actions.onUpdateReminder(
                            block.id,
                            block.reminderTimestampOnNewDate(it),
                            block.allDay
                        )
                    },
                    onCustomSelected = { pickerState.showDatePicker = true },
                    customDateInitialTimestamp = block.reminderTimestamp,
                    onCustomDateChosen = { timestamp ->
                        actions.onUpdateReminder(block.id, block.reminderTimestampOnNewDate(timestamp), block.allDay)
                    },
                    onRemove = if (hasReminder) {
                        { actions.onUpdateReminder(block.id, null, null) }
                    } else null
                )
            }
            Box {
                IconTooltip(text = stringResource(Res.string.editor_time)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                pickerState.isReminderPickerOpening = true
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                scope.launch {
                                    if (isKeyboardOpen) delay(500L.milliseconds) else delay(
                                        50L.milliseconds
                                    )
                                    pickerState.showTimePresetMenu = true
                                    pickerState.isReminderPickerOpening = false
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(Res.drawable.clock_circle), stringResource(Res.string.editor_time),
                            modifier = Modifier.size(16.dp),
                            tint = if (hasReminder) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                TimePresetMenu(
                    expanded = pickerState.showTimePresetMenu,
                    onDismiss = { pickerState.showTimePresetMenu = false },
                    onPresetSelected = {
                        actions.onUpdateReminder(
                            block.id,
                            it,
                            null
                        )
                    },
                    onCustomSelected = { pickerState.showTimePicker = true },
                    customTimeInitialTimestamp = block.reminderTimestamp,
                    onCustomTimeChosen = { hour, minute ->
                        actions.onUpdateReminder(block.id, reminderTimestampAtTime(block.reminderTimestamp, hour, minute), null)
                    },
                    isAllDay = block.allDay != null,
                    onAllDaySelected = {
                        if (block.allDay == null) {
                            actions.onUpdateReminder(
                                block.id,
                                reminderTimestampAtTime(block.reminderTimestamp, DEFAULT_ALL_DAY_REMINDER_HOUR, 0),
                                AllDaySpan()
                            )
                        }
                    }
                )
            }
            if (hasReminder) {
                val dt = remember(block.reminderTimestamp) {
                    Instant.fromEpochMilliseconds(block.reminderTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
                }
                val allDay = block.allDay
                val timeText = when {
                    allDay == null -> stringResource(Res.string.editor_reminder_date_and_time, shortDateLabel(dt.date), formatTimeOfDay(dt.hour, dt.minute, usesTwentyFourHourClock))
                    allDay.lengthInDays > 1 -> stringResource(Res.string.editor_reminder_all_day_range, shortDateLabel(dt.date), shortDateLabel(allDay.lastDate(dt.date)))
                    else -> stringResource(Res.string.editor_reminder_all_day, shortDateLabel(dt.date))
                }
                val timeZone = TimeZone.currentSystemDefault()
                val reminderDate = Instant.fromEpochMilliseconds(block.reminderTimestamp)
                    .toLocalDateTime(timeZone).date
                val lastReminderDate = block.allDay?.lastDate(reminderDate) ?: reminderDate
                val isOverdue = !block.isChecked && lastReminderDate < Clock.System.todayIn(timeZone)
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
internal fun CheckboxReminderPickerDialogs(block: CheckboxBlock, actions: EditorActions, pickerState: ReminderPickerState) {
    if (pickerState.showDatePicker) {
        MinimalDatePickerDialog(
            initialTimestamp = block.reminderTimestamp,
            onDismiss = { pickerState.showDatePicker = false },
            onConfirm = { timestamp ->
                actions.onUpdateReminder(block.id, block.reminderTimestampOnNewDate(timestamp), block.allDay)
                pickerState.showDatePicker = false
            }
        )
    }
    if (pickerState.showTimePicker) {
        MinimalTimePickerDialog(
            initialTimestamp = block.reminderTimestamp,
            onDismiss = { pickerState.showTimePicker = false },
            onConfirm = { hour, minute ->
                actions.onUpdateReminder(block.id, reminderTimestampAtTime(block.reminderTimestamp, hour, minute), null)
                pickerState.showTimePicker = false
            }
        )
    }
}
