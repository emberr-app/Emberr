@file:OptIn(ExperimentalMaterial3Api::class)

package com.emberr.presentation.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.DEFAULT_ALL_DAY_REMINDER_HOUR
import com.emberr.domain.model.lastDate
import com.emberr.domain.model.RecurrenceEditScope
import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.editor.components.DesktopCursor
import com.emberr.presentation.shared.editor.components.desktopPointerCursor
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.EmberrBlur
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrDesktopSubmenu
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.DesktopDatePickerMenuWidth
import com.emberr.presentation.shared.components.DesktopTimePickerMenuWidth
import com.emberr.presentation.shared.components.MenuAtTap
import com.emberr.presentation.shared.components.MenuTapAnchor
import com.emberr.presentation.shared.components.MinimalDatePickerDialog
import com.emberr.presentation.shared.components.MinimalTimePickerDialog
import com.emberr.presentation.shared.components.NoRippleIndicationNodeFactory
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.menuTapAnchor
import com.emberr.presentation.shared.components.rememberMenuTapAnchor
import dev.chrisbanes.haze.HazeState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.bell
import emberr.shared.generated.resources.bell_off2
import emberr.shared.generated.resources.calendar
import emberr.shared.generated.resources.calendar_day
import emberr.shared.generated.resources.chevron_down
import emberr.shared.generated.resources.clock_circle
import emberr.shared.generated.resources.doc_text
import emberr.shared.generated.resources.link
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.trash
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.datetime.number
import org.jetbrains.compose.resources.painterResource
import com.emberr.domain.util.time.formatTimeOfDay
import com.emberr.domain.util.time.daysOfWeekStartingFrom
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.ui.theme.LocalFirstDayOfWeek
import com.emberr.ui.theme.LocalAppIsDark

internal val EventChipTextColor = Color(0xFF1A1A1A)
private val InteractiveShape = RoundedCornerShape(12.dp)
private val EventChipShape = RoundedCornerShape(8.dp)
private val EventChipHeightForTwoLines = 52.dp
private val DesktopResizeHandleHeight = 6.dp
internal const val MouseMoveStartSlopRatio = 0.1f
private val TouchResizeHandleHeight = 12.dp
private val ResizeDashShape = RoundedCornerShape(percent = 50)
private val ResizeDashDarkModeColor = Color(0xFFDDDDDD)
internal const val CheckedEventTitleAlpha = 0.45f
private val FieldPadding = 14.dp
private val EditorSpacing = 24.dp
private val EventTitleStyle: TextStyle
    @Composable get() = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
private val EventSheetDesktopMaxWidth = 560.dp
private val ChipShape = RoundedCornerShape(8.dp)
private val ChipSpacing = 8.dp
private val ChipHorizontalPadding = 10.dp
private val ChipVerticalPadding = 5.dp
private val PillMenuGap = 6.dp
private val PillMenuMinWidth = 160.dp
private val PillMenuMaxWidth = 280.dp
private val DurationPresetsInMinutes = listOf(15, 30, 45, 60, 90, 120)

@Composable
fun EventChip(
    text: String,
    timeText: String,
    color: Color,
    hasCategory: Boolean,
    isChecked: Boolean,
    height: Dp,
    isInResizeMode: Boolean,
    onResizeStart: (edge: EventResizeEdge, pointerInWindow: Offset) -> Unit,
    onResizeMove: (pointerInWindow: Offset) -> Unit,
    onResizeEnd: () -> Unit,
    onResizeCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveStart: ((pointerInWindow: Offset, pointerId: PointerId, cardTopLeftInWindow: Offset, cardSize: IntSize) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val textColor = if (hasCategory) EventChipTextColor else MaterialTheme.colorScheme.onSurface
    val resizeDashColor = if (LocalAppIsDark.current && !hasCategory) ResizeDashDarkModeColor else textColor
    val currentOnMoveStart by rememberUpdatedState(onMoveStart)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentIsInResizeMode by rememberUpdatedState(isInResizeMode)
    var chipCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Surface(
        shape = EventChipShape,
        color = color,
        border = if (isInResizeMode) BorderStroke(1.5.dp, textColor) else null,
        modifier = modifier
            .height(height)
            .clip(EventChipShape)
            .clickable(onClick = onClick)
            .onPlaced { chipCoordinates = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (isDesktopPlatform && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                    val coordinates = chipCoordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture
                    val downInWindow = coordinates.localToWindow(down.position)
                    val moveStart = when {
                        isDesktopPlatform -> awaitEventMoveStart(down, viewConfiguration.touchSlop * MouseMoveStartSlopRatio)
                        currentIsInResizeMode -> awaitEventMoveStart(down, viewConfiguration.touchSlop)
                        else -> awaitEventMoveStartAfterLongPress(down) { currentOnLongClick?.invoke() }
                    } ?: return@awaitEachGesture
                    val onStart = currentOnMoveStart ?: return@awaitEachGesture
                    onStart(downInWindow, moveStart.id, coordinates.positionInWindow(), coordinates.size)
                    drag(moveStart.id) { change -> change.consume() }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            EventChipLabels(text = text, timeText = timeText, textColor = textColor, isChecked = isChecked, height = height)

            if (isDesktopPlatform || isInResizeMode) {
                EventResizeHandle(
                    edge = EventResizeEdge.TOP,
                    showDash = isInResizeMode,
                    dashColor = resizeDashColor,
                    onResizeStart = onResizeStart,
                    onResizeMove = onResizeMove,
                    onResizeEnd = onResizeEnd,
                    onResizeCancel = onResizeCancel,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                EventResizeHandle(
                    edge = EventResizeEdge.BOTTOM,
                    showDash = isInResizeMode,
                    dashColor = resizeDashColor,
                    onResizeStart = onResizeStart,
                    onResizeMove = onResizeMove,
                    onResizeEnd = onResizeEnd,
                    onResizeCancel = onResizeCancel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

internal suspend fun AwaitPointerEventScope.awaitEventMoveStart(
    down: PointerInputChange,
    moveStartDistance: Float
): PointerInputChange? {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return null
        if (!change.pressed || change.isConsumed) return null
        if ((change.position - down.position).getDistance() >= moveStartDistance) {
            change.consume()
            return change
        }
    }
}

internal suspend fun AwaitPointerEventScope.awaitEventMoveStartAfterLongPress(
    down: PointerInputChange,
    onLongPress: () -> Unit
): PointerInputChange? {
    val longPress = awaitLongPressOrCancellation(down.id) ?: return null
    onLongPress()
    longPress.consume()
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == longPress.id }
            ?: return null
        change.consume()
        if (!change.pressed) return null
        if ((change.position - longPress.position).getDistance() >= viewConfiguration.touchSlop) return change
    }
}

@Composable
fun EventMovePreviewChip(
    text: String,
    timeText: String,
    color: Color,
    hasCategory: Boolean,
    isChecked: Boolean,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val textColor = if (hasCategory) EventChipTextColor else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = EventChipShape,
        color = color,
        shadowElevation = 6.dp,
        modifier = modifier.height(height)
    ) {
        EventChipLabels(text = text, timeText = timeText, textColor = textColor, isChecked = isChecked, height = height)
    }
}

@Composable
private fun EventChipLabels(text: String, timeText: String, textColor: Color, isChecked: Boolean, height: Dp) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text.ifBlank { "Untitled event" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = if (isChecked) textColor.copy(alpha = CheckedEventTitleAlpha) else textColor,
            textDecoration = if (isChecked) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (height >= EventChipHeightForTwoLines) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EventResizeHandle(
    edge: EventResizeEdge,
    showDash: Boolean,
    dashColor: Color,
    onResizeStart: (edge: EventResizeEdge, pointerInWindow: Offset) -> Unit,
    onResizeMove: (pointerInWindow: Offset) -> Unit,
    onResizeEnd: () -> Unit,
    onResizeCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnResizeStart by rememberUpdatedState(onResizeStart)
    val currentOnResizeMove by rememberUpdatedState(onResizeMove)
    val currentOnResizeEnd by rememberUpdatedState(onResizeEnd)
    val currentOnResizeCancel by rememberUpdatedState(onResizeCancel)
    var handleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        contentAlignment = if (edge == EventResizeEdge.TOP) Alignment.TopCenter else Alignment.BottomCenter,
        modifier = modifier
            .fillMaxWidth()
            .height(if (isDesktopPlatform) DesktopResizeHandleHeight else TouchResizeHandleHeight)
            .onPlaced { handleCoordinates = it }
            .desktopPointerCursor(DesktopCursor.RESIZE_VERTICAL)
            .pointerInput(edge) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val coordinates = handleCoordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture
                    down.consume()
                    currentOnResizeStart(edge, coordinates.localToWindow(down.position))
                    val released = drag(down.id) { change ->
                        change.consume()
                        currentOnResizeMove(coordinates.localToWindow(change.position))
                    }
                    if (released) currentOnResizeEnd() else currentOnResizeCancel()
                }
            }
    ) {
        if (showDash) {
            Box(
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .size(width = 18.dp, height = 3.dp)
                    .background(dashColor, ResizeDashShape)
            )
        }
    }
}

data class EventEditorState(
    val original: CalendarEvent?,
    val name: String,
    val date: LocalDate,
    val hour: Int,
    val minute: Int,
    val categoryId: String?,
    val durationMinutes: Int = 30,
    val url: String = "",
    val description: String = "",
    val recurrenceRule: RecurrenceRule? = null,
    val allDay: AllDaySpan? = null,
    val editScope: RecurrenceEditScope = RecurrenceEditScope.ALL_EVENTS,
    val isEditing: Boolean = original == null
)

fun CalendarEvent.toEditorState(): EventEditorState {
    val startedAt = Instant.fromEpochMilliseconds(reminderTimestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    return EventEditorState(
        original = this,
        name = text,
        date = startedAt.date,
        hour = startedAt.hour,
        minute = startedAt.minute,
        categoryId = categoryId,
        durationMinutes = durationMinutes,
        url = url.orEmpty(),
        description = description.orEmpty(),
        recurrenceRule = recurrenceRule,
        allDay = allDay
    )
}


fun EventEditorState.toEpochMillis(): Long {
    val localDateTime = LocalDateTime(date.year, date.month.number, date.day, hour, minute)
    return localDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
fun EventEditorState.toEndEpochMillis(): Long = toEpochMillis() + durationMinutes * 60_000L

fun EventEditorState.endHour(): Int {
    val startTotal = hour * 60 + minute
    return ((startTotal + durationMinutes) / 60) % 24
}

fun EventEditorState.endMinute(): Int = (minute + durationMinutes) % 60

fun EventEditorState.allDayLastDate(): LocalDate = allDay?.lastDate(date) ?: date

fun EventEditorState.keepsSheetOpenAfterSave(): Boolean {
    val original = original ?: return false
    val splitsSeries = original.recurrenceRule != null &&
        (editScope == RecurrenceEditScope.ALL_FUTURE_EVENTS || editScope == RecurrenceEditScope.ALL_PAST_EVENTS)
    return !splitsSeries
}

fun EventEditorState.viewAfterSaving(): EventEditorState? {
    val original = original
    if (original == null || !keepsSheetOpenAfterSave()) return null
    val savedEvent = original.copy(
        text = name,
        dateString = date.toString(),
        occurrenceDateString = if (editScope == RecurrenceEditScope.THIS_EVENT) original.occurrenceDateString else date.toString(),
        reminderTimestamp = toEpochMillis(),
        categoryId = categoryId,
        durationMinutes = durationMinutes,
        url = url.trim().takeIf { it.isNotEmpty() }?.let(::normalizeEventUrl),
        description = description.trim().takeIf { it.isNotEmpty() },
        recurrenceRule = recurrenceRule,
        allDay = allDay
    )
    return savedEvent.toEditorState()
}

private fun LocalDate.atTimeInEpochMillis(hour: Int, minute: Int): Long =
    LocalDateTime(year, month.number, day, hour, minute).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

fun formatRecurrenceSummary(rule: RecurrenceRule): String {
    val base = when (rule.frequency) {
        RecurrenceFrequency.DAILY -> if (rule.interval == 1) "Daily" else "Every ${rule.interval} days"
        RecurrenceFrequency.WEEKLY -> {
            val dayLabel = rule.daysOfWeek.takeIf { it.isNotEmpty() }
                ?.sortedBy { it.isoDayNumber }
                ?.joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
                ?.let { " on $it" }
                ?: ""
            (if (rule.interval == 1) "Weekly" else "Every ${rule.interval} weeks") + dayLabel
        }
        RecurrenceFrequency.MONTHLY -> if (rule.interval == 1) "Monthly" else "Every ${rule.interval} months"
        RecurrenceFrequency.YEARLY -> if (rule.interval == 1) "Yearly" else "Every ${rule.interval} years"
    }
    val until = rule.untilDateString?.let { " until ${formatFullDate(LocalDate.parse(it))}" } ?: ""
    return base + until
}

fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours == 0 -> "$mins min"
        mins == 0 -> "$hours hr"
        else -> "$hours hr $mins min"
    }
}

private fun formatEventTimeSuffix(state: EventEditorState, usesTwentyFourHourClock: Boolean): String {
    val allDay = state.allDay
    return when {
        allDay == null -> ", ${formatTimeOfDay(state.hour, state.minute, usesTwentyFourHourClock)} – " +
            formatTimeOfDay(state.endHour(), state.endMinute(), usesTwentyFourHourClock)
        allDay.lengthInDays > 1 -> " – ${formatEditorDate(state.allDayLastDate())}"
        else -> ", All day"
    }
}

private fun formatEditorDate(date: LocalDate): String {
    val weekday = date.dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase)
    val month = date.month.name.take(3).lowercase().replaceFirstChar(Char::uppercase)
    val isThisYear = date.year == Clock.System.todayIn(TimeZone.currentSystemDefault()).year
    return if (isThisYear) "$weekday, $month ${date.day}" else "$weekday, $month ${date.day}, ${date.year}"
}

@Composable
fun EventEditorSheet(
    state: EventEditorState?,
    categories: List<CalendarCategory>,
    onNameChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onDurationChange: (minutes: Int) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onUrlChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRecurrenceChange: (RecurrenceRule?) -> Unit,
    onAllDayChange: (AllDaySpan?) -> Unit,
    onEditClick: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    EmberrBottomSheet(
        expanded = state != null,
        onDismiss = onDismiss,
        desktopMaxWidth = EventSheetDesktopMaxWidth
    ) { closeAnd ->
      CompositionLocalProvider(
        LocalIndication provides NoRippleIndicationNodeFactory,
        LocalRippleConfiguration provides null
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (isDesktopPlatform) 20.dp else 6.dp)) {
            if (state != null) {
                EventEditorFields(
                    state = state,
                    categories = categories,
                    onNameChange = onNameChange,
                    onDateChange = onDateChange,
                    onTimeChange = onTimeChange,
                    onDurationChange = onDurationChange,
                    onCategoryChange = onCategoryChange,
                    onUrlChange = onUrlChange,
                    onDescriptionChange = onDescriptionChange,
                    onRecurrenceChange = onRecurrenceChange,
                    onAllDayChange = onAllDayChange,
                    onEditClick = onEditClick,
                    onCancel = { if (state.isEditing && state.original != null) onCancelEdit() else closeAnd(onDismiss) },
                    onSave = { if (state.keepsSheetOpenAfterSave()) onSave() else closeAnd(onSave) },
                    onDelete = onDelete?.let { delete -> { closeAnd(delete) } }
                )
            }
        }
      }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventEditorFields(
    state: EventEditorState,
    categories: List<CalendarCategory>,
    onNameChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onDurationChange: (minutes: Int) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onUrlChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRecurrenceChange: (RecurrenceRule?) -> Unit,
    onAllDayChange: (AllDaySpan?) -> Unit,
    onEditClick: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?
) {
    if (!state.isEditing) {
        EventViewFields(
            state = state,
            categories = categories,
            onEditClick = onEditClick,
            onDelete = onDelete,
            onClose = onCancel
        )
        return
    }

    var showsDetails by remember {
        mutableStateOf(state.allDay != null || state.recurrenceRule != null || state.url.isNotBlank() || state.description.isNotBlank())
    }
    val canEditDateAndRepeat = state.editScope == RecurrenceEditScope.ALL_EVENTS
    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (isDesktopPlatform) titleFocusRequester.requestFocus()
    }
    val allDay = state.allDay

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 32.dp,
                bottom = if (isDesktopPlatform) 0.dp else 20.dp
            ),
        verticalArrangement = Arrangement.spacedBy(EditorSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmberrTextField(
                value = state.name,
                onValueChange = onNameChange,
                placeholder = "What's happening?",
                textStyle = EventTitleStyle,
                hasBackground = false,
                modifier = Modifier.weight(1f).focusRequester(titleFocusRequester),
                onSubmit = onSave
            )
            if (onDelete != null) DeleteEventButton(alignedTo = EventTitleStyle, onDelete = onDelete)
        }

        EventEditorHeader(
            state = state,
            canEditDate = canEditDateAndRepeat,
            onDateChange = onDateChange,
            onTimeChange = onTimeChange
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
            verticalAlignment = Alignment.Top
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
                verticalArrangement = Arrangement.spacedBy(ChipSpacing)
            ) {
                CategoryChoiceChip(categoryId = state.categoryId, categories = categories, onCategoryChange = onCategoryChange)
                if (allDay == null) {
                    DurationChip(state = state, onDurationChange = onDurationChange)
                } else {
                    AllDayLengthChip(state = state, allDay = allDay, onAllDayChange = onAllDayChange)
                    AllDayReminderChip(state = state, allDay = allDay, onAllDayChange = onAllDayChange, onTimeChange = onTimeChange)
                }
            }
            DetailsTogglePill(showsDetails = showsDetails, onClick = { showsDetails = !showsDetails })
        }

        if (showsDetails) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
                verticalArrangement = Arrangement.spacedBy(ChipSpacing)
            ) {
                EventEditorChip(
                    label = "All day",
                    icon = painterResource(Res.drawable.calendar_day),
                    isSelected = allDay != null,
                    opensMenu = false,
                    onClick = {
                        if (allDay == null) {
                            onAllDayChange(AllDaySpan())
                            onTimeChange(DEFAULT_ALL_DAY_REMINDER_HOUR, 0)
                        } else {
                            onAllDayChange(null)
                        }
                    }
                )
                RepeatChip(state = state, isEnabled = canEditDateAndRepeat, onRecurrenceChange = onRecurrenceChange)
            }

            EventDetailField(
                icon = painterResource(Res.drawable.link),
                value = state.url,
                onValueChange = onUrlChange,
                placeholder = "Add a link",
                onSubmit = onSave
            )

            EventDetailField(
                icon = painterResource(Res.drawable.doc_text),
                value = state.description,
                onValueChange = onDescriptionChange,
                placeholder = "Add a description",
                singleLine = false
            )
        }

        val saveLabel = if (state.original == null) "Add" else "Save"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmberrButtonSecondary(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
            EmberrButtonPrimary(text = saveLabel, onClick = onSave, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventEditorHeader(
    state: EventEditorState,
    canEditDate: Boolean,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    val dateTapAnchor = rememberMenuTapAnchor()
    val timeTapAnchor = rememberMenuTapAnchor()
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val allDay = state.allDay

    FlowRow(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.menuTapAnchor(dateTapAnchor)) {
            Text(
                text = formatEditorDate(state.date),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = if (canEditDate) Modifier.clickable { showDatePicker = true } else Modifier.alpha(0.5f)
            )
            if (showDatePicker) {
                PickerAtTap(dateTapAnchor, DesktopDatePickerMenuWidth) {
                    MinimalDatePickerDialog(
                        initialTimestamp = state.toEpochMillis(),
                        onDismiss = { showDatePicker = false },
                        onConfirm = { millis ->
                            val instant = Instant.fromEpochMilliseconds(millis)
                            onDateChange(instant.toLocalDateTime(TimeZone.currentSystemDefault()).date)
                        }
                    )
                }
            }
        }
        val timeSuffix = formatEventTimeSuffix(state, usesTwentyFourHourClock)
        when {
            allDay == null -> Box(modifier = Modifier.menuTapAnchor(timeTapAnchor)) {
                Text(
                    text = timeSuffix,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { showStartTimePicker = true }
                )
                if (showStartTimePicker) {
                    PickerAtTap(timeTapAnchor, DesktopTimePickerMenuWidth) {
                        MinimalTimePickerDialog(
                            initialTimestamp = state.toEpochMillis(),
                            onDismiss = { showStartTimePicker = false },
                            onConfirm = { hour, minute -> onTimeChange(hour, minute) }
                        )
                    }
                }
            }
            else -> Text(
                text = timeSuffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DeleteEventButton(alignedTo: TextStyle, onDelete: () -> Unit) {
    val lineHeight = with(LocalDensity.current) { alignedTo.lineHeight.toDp() }
    Box(modifier = Modifier.height(lineHeight).wrapContentHeight(unbounded = true)) {
        TopBarIconButton(
            icon = painterResource(Res.drawable.trash),
            contentDescription = "Delete",
            bgColor = Color.Transparent,
            tint = MaterialTheme.colorScheme.error,
            hazeState = remember { HazeState() },
            hazeStyle = EmberrBlur.Regular,
            shadowElevation = EmberrShadowElevation.None,
            tooltip = "Delete event",
            onClick = onDelete
        )
    }
}

@Composable
private fun DurationChip(state: EventEditorState, onDurationChange: (minutes: Int) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        EventEditorChip(
            label = formatDuration(state.durationMinutes),
            icon = painterResource(Res.drawable.clock_circle),
            onClick = { showMenu = true }
        )
        if (showMenu) BelowPill {
            EventChoiceMenu(
                title = "Duration",
                onDismiss = { showMenu = false },
                choices = DurationPresetsInMinutes.map { minutes ->
                    EventChoice(
                        label = formatDuration(minutes),
                        isSelected = minutes == state.durationMinutes,
                        onClick = { onDurationChange(minutes) }
                    )
                },
                customTime = EventCustomTimeChoice(
                    label = "Custom end time…",
                    initialTimestamp = state.toEndEpochMillis(),
                    onTimeChosen = { hour, minute ->
                        val startTotal = state.hour * 60 + state.minute
                        var endTotal = hour * 60 + minute
                        if (endTotal <= startTotal) endTotal += 24 * 60
                        onDurationChange((endTotal - startTotal).coerceAtLeast(5))
                    }
                )
            )
        }
    }
}

@Composable
private fun AllDayLengthChip(state: EventEditorState, allDay: AllDaySpan, onAllDayChange: (AllDaySpan?) -> Unit) {
    var showEndDatePicker by remember { mutableStateOf(false) }
    Box {
        EventEditorChip(
            label = if (allDay.lengthInDays == 1) "1 day" else "${allDay.lengthInDays} days",
            icon = painterResource(Res.drawable.calendar),
            onClick = { showEndDatePicker = true }
        )
        if (showEndDatePicker) BelowPill {
            MinimalDatePickerDialog(
                initialTimestamp = state.allDayLastDate().atTimeInEpochMillis(state.hour, state.minute),
                onDismiss = { showEndDatePicker = false },
                onConfirm = { millis ->
                    val endDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    onAllDayChange(allDay.copy(lengthInDays = (state.date.daysUntil(endDate) + 1).coerceAtLeast(1)))
                }
            )
        }
    }
}

@Composable
private fun AllDayReminderChip(
    state: EventEditorState,
    allDay: AllDaySpan,
    onAllDayChange: (AllDaySpan?) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val reminderTime = formatTimeOfDay(state.hour, state.minute, LocalUsesTwentyFourHourClock.current)

    Box {
        EventEditorChip(
            label = if (allDay.isReminderOn) reminderTime else "No reminder",
            icon = painterResource(if (allDay.isReminderOn) Res.drawable.bell else Res.drawable.bell_off2),
            onClick = { showMenu = true }
        )
        if (showMenu) BelowPill {
            EventChoiceMenu(
                title = "Reminder",
                onDismiss = { showMenu = false },
                choices = listOf(
                    EventChoice(
                        label = "No reminder",
                        isSelected = !allDay.isReminderOn,
                        onClick = { onAllDayChange(allDay.copy(isReminderOn = false)) }
                    ),
                    EventChoice(
                        label = "At $reminderTime",
                        subtitle = if (allDay.lengthInDays > 1) "On each day" else "On this day",
                        isSelected = allDay.isReminderOn,
                        onClick = { onAllDayChange(allDay.copy(isReminderOn = true)) }
                    )
                ),
                customTime = EventCustomTimeChoice(
                    label = "Pick a time…",
                    initialTimestamp = state.toEpochMillis(),
                    onTimeChosen = { hour, minute ->
                        onTimeChange(hour, minute)
                        onAllDayChange(allDay.copy(isReminderOn = true))
                    }
                )
            )
        }
    }
}

@Composable
private fun CategoryChoiceChip(
    categoryId: String?,
    categories: List<CalendarCategory>,
    onCategoryChange: (String?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val category = categories.firstOrNull { it.id == categoryId }
    val noCategoryColor = MaterialTheme.colorScheme.outline

    Box {
        EventEditorChip(
            label = category?.name ?: "No category",
            dotColor = category?.colorHex?.toCategoryColor() ?: noCategoryColor,
            onClick = { showMenu = true }
        )
        if (showMenu) BelowPill {
            EventChoiceMenu(
                title = "Category",
                onDismiss = { showMenu = false },
                choices = listOf(
                    EventChoice(
                        label = "No category",
                        isSelected = category == null,
                        dotColor = noCategoryColor,
                        onClick = { onCategoryChange(null) }
                    )
                ) + categories.map { choice ->
                    EventChoice(
                        label = choice.name,
                        isSelected = choice.id == category?.id,
                        dotColor = choice.colorHex.toCategoryColor(),
                        onClick = { onCategoryChange(choice.id) }
                    )
                }
            )
        }
    }
}

@Composable
private fun RepeatChip(state: EventEditorState, isEnabled: Boolean, onRecurrenceChange: (RecurrenceRule?) -> Unit) {
    var showRepeatDialog by remember { mutableStateOf(false) }
    Box {
        EventEditorChip(
            label = state.recurrenceRule?.let(::formatRecurrenceSummary) ?: "Does not repeat",
            icon = painterResource(Res.drawable.refresh_cw),
            isEnabled = isEnabled,
            onClick = { showRepeatDialog = true }
        )
        if (showRepeatDialog) BelowPill {
            RepeatOptionsDialog(
                initialRule = state.recurrenceRule,
                anchorDate = state.date,
                onDismiss = { showRepeatDialog = false },
                onConfirm = { rule ->
                    onRecurrenceChange(rule)
                    showRepeatDialog = false
                }
            )
        }
    }
}

@Composable
private fun EventEditorChip(
    label: String,
    onClick: (() -> Unit)?,
    icon: Painter? = null,
    dotColor: Color? = null,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    opensMenu: Boolean = true
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val containerColor = chipContainerColor(isSelected)

    Row(
        modifier = Modifier
            .alpha(if (isEnabled) 1f else 0.5f)
            .clip(ChipShape)
            .background(containerColor)
            .then(if (onClick != null) Modifier.clickable(enabled = isEnabled, onClick = onClick) else Modifier)
            .padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = if (isSelected) 1f else 0.7f),
                modifier = Modifier.size(14.dp)
            )
        }
        if (dotColor != null) {
            ChoiceDot(dotColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (opensMenu) {
            Icon(
                painter = painterResource(Res.drawable.chevron_down),
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 2.dp).size(14.dp)
            )
        }
    }
}

@Composable
private fun chipContainerColor(isSelected: Boolean): Color = when {
    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    !isDesktopPlatform || LocalAppIsDark.current -> MaterialTheme.colorScheme.surfaceVariant
    else -> Color(0xFFD8D8D8)
}

@Composable
private fun DetailsTogglePill(showsDetails: Boolean, onClick: () -> Unit) {
    val labelLineHeight = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() }
    Box(
        modifier = Modifier
            .height(labelLineHeight + ChipVerticalPadding * 2)
            .clip(ChipShape)
            .background(chipContainerColor(isSelected = false))
            .clickable(onClick = onClick)
            .padding(horizontal = ChipHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (showsDetails) Icons.Default.Remove else Icons.Default.Add,
            contentDescription = if (showsDetails) "Fewer details" else "Add details",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun BoxScope.BelowPill(menu: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(y = PillMenuGap)
            .size(0.dp)
    ) {
        menu()
    }
}

@Composable
private fun ChoiceDot(color: Color) {
    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
}

@Composable
private fun EventDetailField(
    icon: Painter,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    onSubmit: (() -> Unit)? = null
) {
    EventDetailRow(icon = icon) {
        EmberrTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            singleLine = singleLine,
            hasBackground = false,
            modifier = Modifier.weight(1f),
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun EventDetailRow(icon: Painter, content: @Composable RowScope.() -> Unit) {
    val firstLineHeight = with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.lineHeight.toDp() }
    val iconSize = 16.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .padding(top = (firstLineHeight - iconSize) / 2)
                .size(iconSize)
        )
        Spacer(modifier = Modifier.width(12.dp))
        content()
    }
}

private class EventChoice(
    val label: String,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val isSelected: Boolean = false,
    val dotColor: Color? = null
)

private class EventCustomTimeChoice(
    val label: String,
    val initialTimestamp: Long,
    val onTimeChosen: (hour: Int, minute: Int) -> Unit
)

@Composable
private fun EventChoiceMenu(
    title: String,
    choices: List<EventChoice>,
    onDismiss: () -> Unit,
    customTime: EventCustomTimeChoice? = null
) {
    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            modifier = Modifier.widthIn(min = PillMenuMinWidth, max = PillMenuMaxWidth)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                choices.forEach { choice ->
                    EmberrDesktopMenuOption(
                        label = choice.label,
                        subtitle = choice.subtitle,
                        isSelected = choice.isSelected,
                        icon = if (choice.dotColor != null) { { ChoiceDot(choice.dotColor) } } else null,
                        onClick = {
                            choice.onClick()
                            onDismiss()
                        }
                    )
                }
                if (customTime != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
                    EmberrDesktopSubmenu(
                        menuWidthEstimate = DesktopTimePickerMenuWidth,
                        anchor = { openPicker, _ ->
                            EmberrDesktopMenuOption(label = customTime.label, onClick = openPicker)
                        }
                    ) { isPickerOpen, closePicker ->
                        MinimalTimePickerDialog(
                            expanded = isPickerOpen,
                            initialTimestamp = customTime.initialTimestamp,
                            onDismiss = closePicker,
                            onConfirm = { hour, minute ->
                                customTime.onTimeChosen(hour, minute)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
        return
    }

    var showsCustomTimePicker by remember { mutableStateOf(false) }

    if (customTime != null && showsCustomTimePicker) {
        MinimalTimePickerDialog(
            initialTimestamp = customTime.initialTimestamp,
            onDismiss = onDismiss,
            onConfirm = { hour, minute -> customTime.onTimeChosen(hour, minute) }
        )
        return
    }

    EmberrBottomSheet(
        expanded = true,
        onDismiss = onDismiss,
        title = title,
        contentHorizontalPadding = 0.dp
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            choices.forEach { choice ->
                EmberrBottomSheetOption(
                    label = choice.label,
                    subtitle = choice.subtitle,
                    isSelected = choice.isSelected,
                    icon = if (choice.dotColor != null) { { ChoiceDot(choice.dotColor) } } else null,
                    onClick = { closeAnd(choice.onClick) }
                )
            }
            if (customTime != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                EmberrBottomSheetOption(label = customTime.label, onClick = { showsCustomTimePicker = true })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventViewFields(
    state: EventEditorState,
    categories: List<CalendarCategory>,
    onEditClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    val category = categories.firstOrNull { it.id == state.categoryId }
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val allDay = state.allDay

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 32.dp,
                bottom = if (isDesktopPlatform) 0.dp else 20.dp
            ),
        verticalArrangement = Arrangement.spacedBy(EditorSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.name.ifBlank { "Untitled event" },
                style = EventTitleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onDelete != null) DeleteEventButton(alignedTo = EventTitleStyle, onDelete = onDelete)
        }

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formatEditorDate(state.date)) }
                append(formatEventTimeSuffix(state, usesTwentyFourHourClock))
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
            verticalArrangement = Arrangement.spacedBy(ChipSpacing)
        ) {
            if (category != null) {
                EventEditorChip(
                    label = category.name,
                    dotColor = category.colorHex.toCategoryColor(),
                    opensMenu = false,
                    onClick = null
                )
            }
            if (allDay == null) {
                EventEditorChip(
                    label = formatDuration(state.durationMinutes),
                    icon = painterResource(Res.drawable.clock_circle),
                    opensMenu = false,
                    onClick = null
                )
            } else {
                EventEditorChip(
                    label = if (allDay.lengthInDays == 1) "1 day" else "${allDay.lengthInDays} days",
                    icon = painterResource(Res.drawable.calendar),
                    opensMenu = false,
                    onClick = null
                )
                EventEditorChip(
                    label = if (allDay.isReminderOn) formatTimeOfDay(state.hour, state.minute, usesTwentyFourHourClock) else "No reminder",
                    icon = painterResource(if (allDay.isReminderOn) Res.drawable.bell else Res.drawable.bell_off2),
                    opensMenu = false,
                    onClick = null
                )
            }
            state.recurrenceRule?.let { rule ->
                EventEditorChip(
                    label = formatRecurrenceSummary(rule),
                    icon = painterResource(Res.drawable.refresh_cw),
                    opensMenu = false,
                    onClick = null
                )
            }
        }

        if (state.url.isNotBlank()) {
            val uriHandler = LocalUriHandler.current
            EventDetailRow(icon = painterResource(Res.drawable.link)) {
                Text(
                    text = state.url,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { try { uriHandler.openUri(state.url) } catch (_: Exception) {} }
                )
            }
        }
        if (state.description.isNotBlank()) {
            EventDetailRow(icon = painterResource(Res.drawable.doc_text)) {
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmberrButtonSecondary(text = "Close", onClick = onClose, modifier = Modifier.weight(1f))
            EmberrButtonPrimary(text = "Edit", onClick = onEditClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EventFieldRow(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = InteractiveShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clip(InteractiveShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FieldPadding, vertical = FieldPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun PickerAtTap(tapAnchor: MenuTapAnchor, desktopMenuWidth: Dp, picker: @Composable () -> Unit) {
    if (isDesktopPlatform) MenuAtTap(tapAnchor, menuWidth = desktopMenuWidth, menu = picker) else picker()
}

@Composable
private fun CategoryChip(
    label: String,
    color: Color,
    hasCategory: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (hasCategory) EventChipTextColor else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = InteractiveShape,
        color = color,
        modifier = Modifier
            .clip(InteractiveShape)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else Color.Transparent,
                shape = InteractiveShape
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = FieldPadding, vertical = 8.dp)
        )
    }
}

@Composable
private fun SheetOptionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(InteractiveShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private val CustomRepeatMenuWidth = 340.dp

@Composable
private fun RepeatDesktopMenu(
    initialRule: RecurrenceRule?,
    anchorDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceRule?) -> Unit
) {
    EmberrDesktopMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = PillMenuMinWidth, max = PillMenuMaxWidth)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            EmberrDesktopMenuOption(label = "Never", onClick = { onConfirm(null) })
            EmberrDesktopMenuOption(label = "Daily", onClick = { onConfirm(RecurrenceRule(frequency = RecurrenceFrequency.DAILY)) })
            EmberrDesktopMenuOption(
                label = "Weekly",
                onClick = { onConfirm(RecurrenceRule(frequency = RecurrenceFrequency.WEEKLY, daysOfWeek = setOf(anchorDate.dayOfWeek))) }
            )
            EmberrDesktopMenuOption(label = "Monthly", onClick = { onConfirm(RecurrenceRule(frequency = RecurrenceFrequency.MONTHLY)) })
            EmberrDesktopMenuOption(label = "Yearly", onClick = { onConfirm(RecurrenceRule(frequency = RecurrenceFrequency.YEARLY)) })
            EmberrDesktopSubmenu(
                menuWidthEstimate = CustomRepeatMenuWidth,
                anchor = { openCustom, _ -> EmberrDesktopMenuOption(label = "Custom…", onClick = openCustom) }
            ) { isCustomOpen, closeCustom ->
                EmberrDesktopMenu(
                    expanded = isCustomOpen,
                    onDismissRequest = closeCustom,
                    modifier = Modifier.width(CustomRepeatMenuWidth)
                ) {
                    CustomRepeatForm(
                        initialRule = initialRule,
                        anchorDate = anchorDate,
                        onCancel = closeCustom,
                        onDone = onConfirm,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RepeatOptionsDialog(
    initialRule: RecurrenceRule?,
    anchorDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceRule?) -> Unit
) {
    if (isDesktopPlatform) {
        RepeatDesktopMenu(
            initialRule = initialRule,
            anchorDate = anchorDate,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
        return
    }

    var showCustom by remember { mutableStateOf(false) }

    if (showCustom) {
        CustomRepeatDialog(
            initialRule = initialRule,
            anchorDate = anchorDate,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
        return
    }

    EmberrBottomSheet(expanded = true, onDismiss = onDismiss, title = "Repeat") { closeAnd ->
        CompositionLocalProvider(
            LocalIndication provides NoRippleIndicationNodeFactory,
            LocalRippleConfiguration provides null
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                SheetOptionRow("Never") { closeAnd { onConfirm(null) } }
                SheetOptionRow("Daily") { closeAnd { onConfirm(RecurrenceRule(frequency = RecurrenceFrequency.DAILY)) } }
                SheetOptionRow("Weekly") {
                    closeAnd { onConfirm(RecurrenceRule(frequency = RecurrenceFrequency.WEEKLY, daysOfWeek = setOf(anchorDate.dayOfWeek))) }
                }
                SheetOptionRow("Monthly") { closeAnd { onConfirm(RecurrenceRule(frequency = RecurrenceFrequency.MONTHLY)) } }
                SheetOptionRow("Yearly") { closeAnd { onConfirm(RecurrenceRule(frequency = RecurrenceFrequency.YEARLY)) } }
                SheetOptionRow("Custom…") { showCustom = true }

                EmberrButtonPrimary(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

private val EmberrCorner = RoundedCornerShape(12.dp)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconTooltip(text = contentDescription) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(EmberrCorner)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun DayOfWeek.shortLabel(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

private fun buildRepeatSummary(
    frequency: RecurrenceFrequency,
    interval: Int,
    daysOfWeek: Set<DayOfWeek>,
    weekdayOrder: List<DayOfWeek>,
    anchorDate: LocalDate,
    untilDateString: String?
): String {
    val unit = when (frequency) {
        RecurrenceFrequency.DAILY -> "day"
        RecurrenceFrequency.WEEKLY -> "week"
        RecurrenceFrequency.MONTHLY -> "month"
        RecurrenceFrequency.YEARLY -> "year"
    }
    val base = if (interval == 1) "Every $unit" else "Every $interval ${unit}s"
    val detail = when (frequency) {
        RecurrenceFrequency.WEEKLY -> daysOfWeek
            .sortedBy { weekdayOrder.indexOf(it) }
            .joinToString(", ") { it.shortLabel() }
            .let { if (it.isBlank()) "" else " on $it" }
        RecurrenceFrequency.MONTHLY -> " on day ${anchorDate.day}"
        else -> ""
    }
    val ending = untilDateString
        ?.let { " · until ${formatFullDate(LocalDate.parse(it))}" }
        .orEmpty()
    return base + detail + ending
}

@Composable
private fun CustomRepeatDialog(
    initialRule: RecurrenceRule?,
    anchorDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceRule?) -> Unit
) {
    EmberrBottomSheet(expanded = true, onDismiss = onDismiss, title = "Custom Repeat") { closeAnd ->
        CustomRepeatForm(
            initialRule = initialRule,
            anchorDate = anchorDate,
            onCancel = { closeAnd(onDismiss) },
            onDone = { rule -> closeAnd { onConfirm(rule) } },
            modifier = Modifier.padding(bottom = 30.dp)
        )
    }
}

@Composable
private fun CustomRepeatForm(
    initialRule: RecurrenceRule?,
    anchorDate: LocalDate,
    onCancel: () -> Unit,
    onDone: (RecurrenceRule) -> Unit,
    modifier: Modifier = Modifier
) {
    var frequency by remember { mutableStateOf(initialRule?.frequency ?: RecurrenceFrequency.WEEKLY) }
    var interval by remember { mutableIntStateOf(initialRule?.interval ?: 1) }
    var daysOfWeek by remember {
        mutableStateOf(initialRule?.daysOfWeek?.takeIf { it.isNotEmpty() } ?: setOf(anchorDate.dayOfWeek))
    }
    var hasEndDate by remember { mutableStateOf(initialRule?.untilDateString != null) }
    var untilDateString by remember { mutableStateOf(initialRule?.untilDateString) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val weekdayOrder = daysOfWeekStartingFrom(LocalFirstDayOfWeek.current)

    val summary = remember(frequency, interval, daysOfWeek, weekdayOrder, hasEndDate, untilDateString) {
        buildRepeatSummary(
            frequency = frequency,
            interval = interval,
            daysOfWeek = daysOfWeek,
            weekdayOrder = weekdayOrder,
            anchorDate = anchorDate,
            untilDateString = if (hasEndDate) untilDateString else null
        )
    }

    val divider = @Composable {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }

    CompositionLocalProvider(
        LocalIndication provides NoRippleIndicationNodeFactory,
        LocalRippleConfiguration provides null
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            divider()

            // Interval
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Repeat every",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StepperButton(
                        icon = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        enabled = interval > 1
                    ) { interval = (interval - 1).coerceAtLeast(1) }
                    Text(
                        text = interval.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 36.dp)
                    )
                    StepperButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Increase",
                        enabled = interval < 99
                    ) { interval = (interval + 1).coerceAtMost(99) }
                }
            }

            // Frequency
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecurrenceFrequency.entries.forEach { option ->
                    CategoryChip(
                        label = when (option) {
                            RecurrenceFrequency.DAILY -> "Days"
                            RecurrenceFrequency.WEEKLY -> "Weeks"
                            RecurrenceFrequency.MONTHLY -> "Months"
                            RecurrenceFrequency.YEARLY -> "Years"
                        },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        hasCategory = false,
                        isSelected = frequency == option,
                        onClick = { frequency = option }
                    )
                }
            }

            // Weekdays
            if (frequency == RecurrenceFrequency.WEEKLY) {
                divider()

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("On these days")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekdayOrder.forEach { day ->
                            val isSelected = day in daysOfWeek
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(EmberrCorner)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        daysOfWeek = if (isSelected) {
                                            (daysOfWeek - day).ifEmpty { setOf(day) }
                                        } else {
                                            daysOfWeek + day
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.name.take(1),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            divider()

            // End condition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ends",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip(
                        label = "Never",
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        hasCategory = false,
                        isSelected = !hasEndDate,
                        onClick = { hasEndDate = false }
                    )
                    CategoryChip(
                        label = "On date",
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        hasCategory = false,
                        isSelected = hasEndDate,
                        onClick = {
                            hasEndDate = true
                            if (untilDateString == null) showEndDatePicker = true
                        }
                    )
                }
            }

            if (hasEndDate) {
                EventFieldRow(
                    icon = painterResource(Res.drawable.calendar),
                    label = untilDateString?.let { formatFullDate(LocalDate.parse(it)) }
                        ?: "Choose end date",
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showEndDatePicker) {
                val initialMillis = untilDateString?.let { LocalDate.parse(it) }
                    ?.atTimeInEpochMillis(0, 0)
                    ?: System.currentTimeMillis()
                MinimalDatePickerDialog(
                    initialTimestamp = initialMillis,
                    onDismiss = { showEndDatePicker = false },
                    onConfirm = { millis ->
                        untilDateString = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                        showEndDatePicker = false
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EmberrButtonSecondary(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
                EmberrButtonPrimary(
                    text = "Done",
                    onClick = {
                        onDone(
                            RecurrenceRule(
                                frequency = frequency,
                                interval = interval,
                                daysOfWeek = if (frequency == RecurrenceFrequency.WEEKLY) daysOfWeek else emptySet(),
                                untilDateString = if (hasEndDate) untilDateString else null
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RecurrenceScopeChooser(
    onDismiss: () -> Unit,
    onScopeSelected: (RecurrenceEditScope) -> Unit
) {
    EmberrBottomSheet(expanded = true, onDismiss = onDismiss, title = "Which events?") { closeAnd ->
        CompositionLocalProvider(
            LocalIndication provides NoRippleIndicationNodeFactory,
            LocalRippleConfiguration provides null
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                SheetOptionRow("This event") { closeAnd { onScopeSelected(RecurrenceEditScope.THIS_EVENT) } }
                SheetOptionRow("All future events") { closeAnd { onScopeSelected(RecurrenceEditScope.ALL_FUTURE_EVENTS) } }
                SheetOptionRow("All past events") { closeAnd { onScopeSelected(RecurrenceEditScope.ALL_PAST_EVENTS) } }
                SheetOptionRow("All events") { closeAnd { onScopeSelected(RecurrenceEditScope.ALL_EVENTS) } }

                EmberrButtonPrimary(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

data class PositionedEvent(
    val event: CalendarEvent,
    val columnIndex: Int,
    val columnCount: Int
)

private fun CalendarEvent.endTimestamp(): Long = reminderTimestamp + durationMinutes * 60_000L
fun layoutEventsForColumn(events: List<CalendarEvent>): List<PositionedEvent> {
    if (events.isEmpty()) return emptyList()
    val sorted = events.sortedBy { it.reminderTimestamp }

    val result = mutableListOf<PositionedEvent>()
    var clusterColumns = mutableListOf<MutableList<CalendarEvent>>()
    var clusterEnd = Long.MIN_VALUE

    fun flushCluster() {
        val columnCount = clusterColumns.size
        if (columnCount == 0) return
        clusterColumns.forEachIndexed { columnIndex, column ->
            column.forEach { event -> result.add(PositionedEvent(event, columnIndex, columnCount)) }
        }
        clusterColumns = mutableListOf()
        clusterEnd = Long.MIN_VALUE
    }

    for (event in sorted) {
        if (clusterColumns.isNotEmpty() && event.reminderTimestamp >= clusterEnd) {
            flushCluster()
        }
        val targetColumn = clusterColumns.firstOrNull { column -> column.last().endTimestamp() <= event.reminderTimestamp }
        if (targetColumn != null) {
            targetColumn.add(event)
        } else {
            clusterColumns.add(mutableListOf(event))
        }
        clusterEnd = maxOf(clusterEnd, event.endTimestamp())
    }
    flushCluster()

    return result
}