package com.emberr.presentation.calendar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.editor.components.DesktopCursor
import com.emberr.presentation.shared.editor.components.desktopPointerCursor
import kotlin.math.roundToInt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

const val MAX_ALL_DAY_ROWS_SHOWN = 2

private val AllDayBarHeight = 22.dp
private val AllDayBarGap = 2.dp
private val AllDayBarInset = 2.dp
private val AllDayBarCorner = 6.dp
private val AllDayStripVerticalPadding = 4.dp
private val AllDayMoreLineHeight = 18.dp
private val EmptyAllDayStripHeight = 28.dp
private val ColumnDividerThickness = 1.dp
private val DesktopEdgeHandleWidth = 6.dp
private val TouchEdgeHandleWidth = 14.dp

data class AllDayBar(
    val event: CalendarEvent,
    val firstColumn: Int,
    val lastColumn: Int,
    val row: Int,
    val startsBeforeWindow: Boolean,
    val endsAfterWindow: Boolean
)

data class AllDayStripLayout(
    val shownBars: List<AllDayBar>,
    val shownRowCount: Int,
    val hiddenCountByColumn: List<Int>
) {
    val hasHiddenBars: Boolean get() = hiddenCountByColumn.any { it > 0 }
}

enum class AllDayDragKind { MOVE, START_EDGE, END_EDGE }

data class AllDaySpanPreview(
    val event: CalendarEvent,
    val startDate: LocalDate,
    val lengthInDays: Int,
    val dropsIntoTimeline: Boolean = false
) {
    val isUnchanged: Boolean
        get() = startDate.toString() == event.dateString && lengthInDays == event.allDay?.lengthInDays
}

class AllDayDragState {
    var selectedEvent by mutableStateOf<CalendarEvent?>(null)
    var preview by mutableStateOf<AllDaySpanPreview?>(null)
    var isDragging = false
}

fun draggedAllDaySpan(event: CalendarEvent, kind: AllDayDragKind, draggedDays: Int): AllDaySpanPreview {
    val startDate = LocalDate.parse(event.dateString)
    val lengthInDays = (event.allDay?.lengthInDays ?: 1).coerceAtLeast(1)
    return when (kind) {
        AllDayDragKind.MOVE -> AllDaySpanPreview(event, startDate.plus(draggedDays, DateTimeUnit.DAY), lengthInDays)
        AllDayDragKind.START_EDGE -> {
            val shiftedDays = minOf(draggedDays, lengthInDays - 1)
            AllDaySpanPreview(event, startDate.plus(shiftedDays, DateTimeUnit.DAY), lengthInDays - shiftedDays)
        }
        AllDayDragKind.END_EDGE -> AllDaySpanPreview(event, startDate, (lengthInDays + draggedDays).coerceAtLeast(1))
    }
}

private data class AllDayEventInWindow(val event: CalendarEvent, val startDate: LocalDate, val lastDate: LocalDate)

fun layoutAllDayStrip(events: List<CalendarEvent>, windowDates: List<LocalDate>): AllDayStripLayout {
    val windowStart = windowDates.first()
    val windowEnd = windowDates.last()
    val eventsInWindow = events
        .filter { it.allDay != null }
        .map { event -> AllDayEventInWindow(event, LocalDate.parse(event.dateString), event.lastShownDate()) }
        .filter { it.startDate <= windowEnd && it.lastDate >= windowStart }
        .sortedWith(
            compareBy<AllDayEventInWindow> { it.startDate }
                .thenByDescending { it.startDate.daysUntil(it.lastDate) }
                .thenBy { it.event.text }
        )

    val occupiedColumnsByRow = mutableListOf<BooleanArray>()
    val bars = eventsInWindow.map { (event, startDate, lastDate) ->
        val firstColumn = windowStart.daysUntil(maxOf(startDate, windowStart))
        val lastColumn = windowStart.daysUntil(minOf(lastDate, windowEnd))
        val columns = firstColumn..lastColumn
        var row = occupiedColumnsByRow.indexOfFirst { occupied -> columns.none { occupied[it] } }
        if (row == -1) {
            occupiedColumnsByRow.add(BooleanArray(windowDates.size))
            row = occupiedColumnsByRow.lastIndex
        }
        columns.forEach { occupiedColumnsByRow[row][it] = true }
        AllDayBar(
            event = event,
            firstColumn = firstColumn,
            lastColumn = lastColumn,
            row = row,
            startsBeforeWindow = startDate < windowStart,
            endsAfterWindow = lastDate > windowEnd
        )
    }

    val (shownBars, hiddenBars) = bars.partition { it.row < MAX_ALL_DAY_ROWS_SHOWN }
    return AllDayStripLayout(
        shownBars = shownBars,
        shownRowCount = minOf(occupiedColumnsByRow.size, MAX_ALL_DAY_ROWS_SHOWN),
        hiddenCountByColumn = windowDates.indices.map { column ->
            hiddenBars.count { column in it.firstColumn..it.lastColumn }
        }
    )
}

@Composable
fun AllDayStrip(
    windowDates: List<LocalDate>,
    events: List<CalendarEvent>,
    categories: List<CalendarCategory>,
    dragState: AllDayDragState,
    moveTracker: EventMoveTracker,
    onEventClick: (CalendarEvent) -> Unit,
    onEmptyDateClick: (LocalDate) -> Unit,
    onMoreClick: (LocalDate) -> Unit,
    onDropIntoTimeline: (EventMovePreview) -> Unit,
    onAllDaySpanChange: (AllDaySpanPreview) -> Unit,
    modifier: Modifier = Modifier
) {
    val layout = remember(events, windowDates) { layoutAllDayStrip(events, windowDates) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    LaunchedEffect(events) {
        if (!dragState.isDragging) dragState.preview = null
    }
    val rowsHeight = AllDayBarHeight * layout.shownRowCount + AllDayBarGap * (layout.shownRowCount - 1).coerceAtLeast(0)
    val moreLineHeight = if (layout.hasHiddenBars) AllDayBarGap + AllDayMoreLineHeight else 0.dp
    val targetHeight = maxOf(EmptyAllDayStripHeight, AllDayStripVerticalPadding * 2 + rowsHeight + moreLineHeight)
    val stripHeight by animateDpAsState(targetHeight, label = "AllDayStripHeight")
    val emptyAreaInteractionSource = remember { MutableInteractionSource() }
    var stripCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Row(modifier = modifier.fillMaxWidth().height(stripHeight)) {
        Box(modifier = Modifier.width(56.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds()
                .onPlaced { stripCoordinates = it }
        ) {
            val columnWidth = (maxWidth - ColumnDividerThickness * (windowDates.size - 1)) / windowDates.size
            val columnStep = columnWidth + ColumnDividerThickness
            val columnStepPx = with(density) { columnStep.toPx() }
            val isNarrow = windowDates.size >= 7

            fun isBelowStrip(pointerInWindow: Offset): Boolean {
                val strip = stripCoordinates?.takeIf { it.isAttached } ?: return false
                return pointerInWindow.y > strip.positionInWindow().y + strip.size.height
            }

            fun showDragPreview(event: CalendarEvent, kind: AllDayDragKind, dragX: Float, pointerInWindow: Offset, pointerId: PointerId?) {
                dragState.isDragging = true
                if (kind == AllDayDragKind.MOVE && pointerId != null && isBelowStrip(pointerInWindow)) {
                    if (moveTracker.activeMove == null) {
                        moveTracker.startFromAllDayStrip(event, pointerId, pointerInWindow)
                    } else {
                        moveTracker.moveTo(pointerInWindow)
                    }
                } else if (moveTracker.activeMove?.startedInAllDayStrip == true) {
                    moveTracker.cancelMove()
                }
                dragState.preview = draggedAllDaySpan(event, kind, (dragX / columnStepPx).roundToInt())
                    .copy(dropsIntoTimeline = moveTracker.activeMove?.startedInAllDayStrip == true)
            }

            fun finishDrag(released: Boolean) {
                dragState.isDragging = false
                val preview = dragState.preview
                if (preview?.dropsIntoTimeline == true) {
                    val timedPreview = if (released) {
                        moveTracker.finish()
                    } else {
                        moveTracker.cancelMove()
                        null
                    }
                    if (timedPreview == null) dragState.preview = null else onDropIntoTimeline(timedPreview)
                    return
                }
                when {
                    !released || preview == null || preview.isUnchanged -> dragState.preview = null
                    else -> onAllDaySpanChange(preview)
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                windowDates.forEachIndexed { index, date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(interactionSource = emptyAreaInteractionSource, indication = null) {
                                if (dragState.selectedEvent != null) dragState.selectedEvent = null else onEmptyDateClick(date)
                            }
                    )
                    if (index != windowDates.lastIndex) {
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = ColumnDividerThickness,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }

            layout.shownBars.forEach { bar ->
                key(bar.event.blockId, bar.event.occurrenceDateString) {
                    val columnsCovered = bar.lastColumn - bar.firstColumn + 1
                    val isDraggedAway = bar.event.isSameOccurrenceAs(dragState.preview?.event)
                    AllDayBarChip(
                        event = bar.event,
                        category = categories.firstOrNull { it.id == bar.event.categoryId },
                        startsBeforeWindow = bar.startsBeforeWindow,
                        endsAfterWindow = bar.endsAfterWindow,
                        isSelected = bar.event.isSameOccurrenceAs(dragState.selectedEvent),
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragState.selectedEvent = bar.event
                        },
                        onDrag = { kind, dragX, pointerInWindow, pointerId ->
                            showDragPreview(bar.event, kind, dragX, pointerInWindow, pointerId)
                        },
                        onDragEnd = ::finishDrag,
                        onClick = {
                            dragState.selectedEvent = null
                            onEventClick(bar.event)
                        },
                        modifier = Modifier
                            .offset(
                                x = columnStep * bar.firstColumn + AllDayBarInset,
                                y = AllDayStripVerticalPadding + (AllDayBarHeight + AllDayBarGap) * bar.row
                            )
                            .width(columnStep * columnsCovered - ColumnDividerThickness - AllDayBarInset * 2)
                            .height(AllDayBarHeight)
                            .alpha(if (isDraggedAway) MovedEventOriginalAlpha else 1f)
                    )
                }
            }

            val incomingTimedEvent = moveTracker.preview?.takeIf { it.becomesAllDay }
            val stripPreview = dragState.preview?.takeIf { !it.dropsIntoTimeline }
                ?: incomingTimedEvent?.let { AllDaySpanPreview(it.event, it.targetDate, lengthInDays = 1) }
            stripPreview?.let { preview ->
                val windowStart = windowDates.first()
                val windowEnd = windowDates.last()
                val previewLastDate = preview.startDate.plus(preview.lengthInDays - 1, DateTimeUnit.DAY)
                if (preview.startDate <= windowEnd && previewLastDate >= windowStart) {
                    val firstColumn = windowStart.daysUntil(maxOf(preview.startDate, windowStart))
                    val lastColumn = windowStart.daysUntil(minOf(previewLastDate, windowEnd))
                    val row = layout.shownBars.firstOrNull { it.event.isSameOccurrenceAs(preview.event) }?.row
                        ?: (0 until layout.shownRowCount).firstOrNull { candidateRow ->
                            layout.shownBars.none { it.row == candidateRow && firstColumn in it.firstColumn..it.lastColumn }
                        }
                        ?: 0
                    AllDayBarLook(
                        event = preview.event,
                        category = categories.firstOrNull { it.id == preview.event.categoryId },
                        startsBeforeWindow = preview.startDate < windowStart,
                        endsAfterWindow = previewLastDate > windowEnd,
                        isLifted = true,
                        modifier = Modifier
                            .offset(
                                x = columnStep * firstColumn + AllDayBarInset,
                                y = AllDayStripVerticalPadding + (AllDayBarHeight + AllDayBarGap) * row
                            )
                            .width(columnStep * (lastColumn - firstColumn + 1) - ColumnDividerThickness - AllDayBarInset * 2)
                            .height(AllDayBarHeight)
                    )
                }
            }

            layout.hiddenCountByColumn.forEachIndexed { column, hiddenCount ->
                if (hiddenCount > 0) {
                    Text(
                        text = if (isNarrow) "+$hiddenCount" else "+$hiddenCount more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .offset(
                                x = columnStep * column,
                                y = AllDayStripVerticalPadding + (AllDayBarHeight + AllDayBarGap) * MAX_ALL_DAY_ROWS_SHOWN
                            )
                            .width(columnWidth)
                            .height(AllDayMoreLineHeight)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onMoreClick(windowDates[column]) }
                            .wrapContentHeight(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@Composable
private fun AllDayBarChip(
    event: CalendarEvent,
    category: CalendarCategory?,
    startsBeforeWindow: Boolean,
    endsAfterWindow: Boolean,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onDrag: (kind: AllDayDragKind, dragX: Float, pointerInWindow: Offset, pointerId: PointerId?) -> Unit,
    onDragEnd: (released: Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var barCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentIsSelected by rememberUpdatedState(isSelected)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val showsEdgeHandles = isDesktopPlatform || isSelected

    AllDayBarLook(
        event = event,
        category = category,
        startsBeforeWindow = startsBeforeWindow,
        endsAfterWindow = endsAfterWindow,
        isSelected = isSelected,
        modifier = modifier,
        gestureModifier = Modifier
            .clickable(onClick = onClick)
            .onPlaced { barCoordinates = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (isDesktopPlatform && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                    val coordinates = barCoordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture
                    val moveStart = when {
                        isDesktopPlatform -> awaitEventMoveStart(down, viewConfiguration.touchSlop * MouseMoveStartSlopRatio)
                        currentIsSelected -> awaitEventMoveStart(down, viewConfiguration.touchSlop)
                        else -> awaitEventMoveStartAfterLongPress(down) { currentOnLongPress() }
                    } ?: return@awaitEachGesture
                    currentOnDrag(AllDayDragKind.MOVE, moveStart.position.x - down.position.x, coordinates.localToWindow(moveStart.position), moveStart.id)
                    val released = drag(moveStart.id) { change ->
                        change.consume()
                        currentOnDrag(AllDayDragKind.MOVE, change.position.x - down.position.x, coordinates.localToWindow(change.position), change.id)
                    }
                    currentOnDragEnd(released)
                }
            }
    ) {
        if (showsEdgeHandles && !startsBeforeWindow) {
            AllDayEdgeHandle(
                kind = AllDayDragKind.START_EDGE,
                showsDash = isSelected,
                onDrag = { dragX -> currentOnDrag(AllDayDragKind.START_EDGE, dragX, Offset.Zero, null) },
                onDragEnd = { released -> currentOnDragEnd(released) },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        if (showsEdgeHandles && !endsAfterWindow) {
            AllDayEdgeHandle(
                kind = AllDayDragKind.END_EDGE,
                showsDash = isSelected,
                onDrag = { dragX -> currentOnDrag(AllDayDragKind.END_EDGE, dragX, Offset.Zero, null) },
                onDragEnd = { released -> currentOnDragEnd(released) },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun AllDayEdgeHandle(
    kind: AllDayDragKind,
    showsDash: Boolean,
    onDrag: (dragX: Float) -> Unit,
    onDragEnd: (released: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(if (isDesktopPlatform) DesktopEdgeHandleWidth else TouchEdgeHandleWidth)
            .fillMaxHeight()
            .desktopPointerCursor(DesktopCursor.RESIZE_HORIZONTAL)
            .pointerInput(kind) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val released = drag(down.id) { change ->
                        change.consume()
                        currentOnDrag(change.position.x - down.position.x)
                    }
                    currentOnDragEnd(released)
                }
            }
    ) {
        if (showsDash) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 12.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), RoundedCornerShape(percent = 50))
            )
        }
    }
}

@Composable
private fun AllDayBarLook(
    event: CalendarEvent,
    category: CalendarCategory?,
    startsBeforeWindow: Boolean,
    endsAfterWindow: Boolean,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isLifted: Boolean = false,
    gestureModifier: Modifier = Modifier,
    handles: @Composable BoxScope.() -> Unit = {}
) {
    val startCorner = if (startsBeforeWindow) 0.dp else AllDayBarCorner
    val endCorner = if (endsAfterWindow) 0.dp else AllDayBarCorner
    val shape = RoundedCornerShape(topStart = startCorner, bottomStart = startCorner, topEnd = endCorner, bottomEnd = endCorner)
    val textColor = if (category != null) EventChipTextColor else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .then(if (isLifted) Modifier.shadow(6.dp, shape) else Modifier)
            .clip(shape)
            .background(category?.colorHex?.toCategoryColor() ?: MaterialTheme.colorScheme.surface)
            .then(if (isSelected) Modifier.border(1.5.dp, textColor, shape) else Modifier)
            .then(gestureModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = event.text.ifBlank { "Untitled event" },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (event.isChecked) textColor.copy(alpha = CheckedEventTitleAlpha) else textColor,
            textDecoration = if (event.isChecked) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        handles()
    }
}

@Composable
fun AllDayEventsSheet(
    date: LocalDate?,
    events: List<CalendarEvent>,
    categories: List<CalendarCategory>,
    onEventClick: (CalendarEvent) -> Unit,
    onDismiss: () -> Unit
) {
    EmberrBottomSheet(
        expanded = date != null,
        onDismiss = onDismiss,
        title = date?.let { "All day · ${formatFullDate(it)}" }
    ) { closeAnd ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            events.forEach { event ->
                MonthEventRow(
                    event = event,
                    category = categories.firstOrNull { it.id == event.categoryId },
                    onClick = { closeAnd { onEventClick(event) } }
                )
            }
        }
    }
}
